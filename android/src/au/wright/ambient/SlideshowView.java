package au.wright.ambient;

import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Two bounded photo scenes; the independent information panel never crossfades. */
public final class SlideshowView extends FrameLayout {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final PhotoStore store;
    private final PhotoSceneView[] scenes=new PhotoSceneView[2];
    private final InformationPanel panel;
    private final TextView empty;
    private ScheduledExecutorService photoWorker,weatherWorker;
    private ExecutorService decoder;
    private volatile boolean running;
    private volatile int generation;
    private volatile String photoStatus="Waiting for photos";
    private int front;
    private boolean photoQueued;
    private Sequence sequence;
    private static final class Sequence {
        List<String> album=Collections.emptyList();int index;String nextId,lastId;
        Sequence(String nextId,String lastId){this.nextId=nextId;this.lastId=lastId;}
    }
    private static final class DecodedScene {
        final PhotoSceneView.Scene scene;final String nextId,lastId;
        DecodedScene(PhotoSceneView.Scene scene,String nextId,String lastId){this.scene=scene;this.nextId=nextId;this.lastId=lastId;}
    }
    private static final class Source {
        final File file;
        final int width,height,orientation;
        Source(File file) throws IOException {
            this.file=file;
            BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(file.getPath(),options);
            if(options.outWidth<=0||options.outHeight<=0)throw new IOException("Photo unavailable");
            int exif=ExifInterface.ORIENTATION_NORMAL;
            try { exif=new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL); }
            catch(IOException ignored){}
            orientation=exif;
            boolean swap=exif>=5&&exif<=8;
            width=swap?options.outHeight:options.outWidth;height=swap?options.outWidth:options.outHeight;
        }
    }
    public SlideshowView(Context context) {
        super(context);setBackgroundColor(0xfff5f1e7);store=new PhotoStore(context);
        for(int i=0;i<2;i++) {
            scenes[i]=new PhotoSceneView(context);scenes[i].setAlpha(i==0?1f:0f);addView(scenes[i],new LayoutParams(-1,-1));
        }
        empty=new TextView(context);empty.setTextColor(0xff60665c);empty.setGravity(Gravity.CENTER);
        empty.setText("Your photos will appear here\nConnect the photo host in Wright Ambient setup.");
        addView(empty,new LayoutParams(-1,-1));
        panel=new InformationPanel(context);addView(panel,new LayoutParams(-1,-1));
    }
    @Override protected void onSizeChanged(int w,int h,int oldW,int oldH) {
        super.onSizeChanged(w,h,oldW,oldH);
        LayoutParams layout=(LayoutParams)empty.getLayoutParams();layout.width=(int)(w*0.72f);empty.setLayoutParams(layout);
        empty.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,h*0.025f);
        if(running&&!photoQueued){handler.removeCallbacks(advance);handler.post(advance);}
    }
    public void start() {
        if(running)return;running=true;final int ticket=++generation;
        android.content.SharedPreferences playback=getContext().getSharedPreferences("playback",Context.MODE_PRIVATE);
        sequence=new Sequence(playback.getString("next_photo",null),playback.getString("last_photo",null));
        front=0;photoQueued=false;photoStatus="Waiting for photos";
        photoWorker=Executors.newSingleThreadScheduledExecutor();weatherWorker=Executors.newSingleThreadScheduledExecutor();
        decoder=Executors.newSingleThreadExecutor();handler.post(tick);handler.post(advance);
        photoWorker.scheduleWithFixedDelay(()->{
            if(!running||ticket!=generation)return;
            try {store.sync();if(ticket==generation)photoStatus="Photos ready";}
            catch(Exception ignored){if(ticket==generation)photoStatus="Waiting for the photo host";}
        },0,300,TimeUnit.SECONDS);
        weatherWorker.scheduleWithFixedDelay(()->{
            if(!running||ticket!=generation)return;
            try {store.syncWeather();}catch(Exception ignored){}
        },0,300,TimeUnit.SECONDS);
    }
    public void stop() {
        if(!running)return;running=false;generation++;handler.removeCallbacksAndMessages(null);photoQueued=false;
        if(photoWorker!=null)photoWorker.shutdownNow();if(weatherWorker!=null)weatherWorker.shutdownNow();
        if(decoder!=null)decoder.shutdownNow();store.cancelRequests();
        for(PhotoSceneView scene:scenes){scene.animate().cancel();scene.clear();}
    }
    private final Runnable tick=new Runnable(){public void run(){if(running){
        panel.update(store.weather(),new Date());
        if(empty.getVisibility()==VISIBLE)empty.setText("Your photos will appear here\n"+photoStatus+"\nOpen Wright Ambient setup to check the connection.");
        handler.postDelayed(this,1000);
    }}};
    private final Runnable advance=()->queuePhoto();
    private void queuePhoto() {
        if(!running||decoder==null||photoQueued||getWidth()==0||getHeight()==0)return;
        photoQueued=true;handler.removeCallbacks(advance);final int ticket=generation;
        final Sequence cursor=sequence;
        final PhotoPresentation.Layout layout=new PhotoPresentation.Layout(getWidth(),getHeight());
        final boolean pairs=getContext().getSharedPreferences("ambient",Context.MODE_PRIVATE).getBoolean("portrait_pairs",true);
        decoder.execute(()->{
            DecodedScene decoded=loadGroup(cursor,layout,pairs);
            handler.post(()->{
                if(!running||ticket!=generation){if(decoded!=null)decoded.scene.recycle();return;}
                photoQueued=false;
                if(decoded!=null) {
                    show(decoded.scene,ticket);
                    // Persist only a scene that starts displaying, never a decoded/stale job.
                    getContext().getSharedPreferences("playback",Context.MODE_PRIVATE).edit()
                        .putString("next_photo",decoded.nextId).putString("last_photo",decoded.lastId).apply();
                }
                int seconds=Policy.slideSeconds(getContext().getSharedPreferences("ambient",Context.MODE_PRIVATE).getInt("seconds",30));
                handler.postDelayed(advance,decoded==null?5000:seconds*1000L);
            });
        });
    }
    private DecodedScene loadGroup(Sequence cursor,PhotoPresentation.Layout layout,boolean pairs) {
        List<String> photos=store.photos();
        if(!photos.equals(cursor.album)) {
            cursor.album=photos;cursor.index=PhotoPresentation.resumeIndex(photos,cursor.nextId,cursor.lastId);
        }
        for(int attempt=0;attempt<photos.size()&&!Thread.currentThread().isInterrupted();attempt++) {
            int index=cursor.index;
            cursor.index=PhotoPresentation.nextIndex(index,1,photos.size());
            Bitmap first=null,second=null;
            try {
                Source source=new Source(store.file(photos.get(index))),next=null;
                if(pairs&&PhotoPresentation.portrait(source.width,source.height)&&index+1<photos.size()) {
                    try {next=new Source(store.file(photos.get(index+1)));}catch(IOException ignored){}
                }
                int count=PhotoPresentation.groupSize(pairs,index,photos.size(),source.width,source.height,
                    next==null?0:next.width,next==null?0:next.height,layout);
                first=decode(source,layout.slot(0,count));
                if(first==null)continue;
                if(count==2) {
                    // A failed second image is skipped without losing or reordering the first.
                    cursor.index=PhotoPresentation.nextIndex(index,2,photos.size());
                    try {second=decode(next,layout.slot(1,2));}catch(IOException|OutOfMemoryError ignored){}
                }
                if(Thread.currentThread().isInterrupted()){first.recycle();if(second!=null)second.recycle();return null;}
                cursor.nextId=photos.get(cursor.index);cursor.lastId=photos.get(second==null?index:index+1);
                return new DecodedScene(new PhotoSceneView.Scene(second==null?new Bitmap[]{first}:new Bitmap[]{first,second}),cursor.nextId,cursor.lastId);
            } catch(IOException|RuntimeException|OutOfMemoryError ignored) {
                if(first!=null)first.recycle();if(second!=null)second.recycle();
            }
        }
        return null;
    }
    private Bitmap decode(Source source,PhotoPresentation.Box slot) throws IOException {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        int[] size=PhotoPresentation.decodeSize(source.width,source.height,slot);
        if(Build.VERSION.SDK_INT>=28)return ImageDecoder.decodeBitmap(ImageDecoder.createSource(source.file),(decoder,info,input)->{
            decoder.setTargetSize(size[0],size[1]);decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
        });
        // Native EXIF support keeps complete images correctly oriented on API 26-27 too.
        BitmapFactory.Options options=new BitmapFactory.Options();int sample=1;
        options.inPreferredConfig=Bitmap.Config.ARGB_8888;options.inPreferredColorSpace=ColorSpace.get(ColorSpace.Named.SRGB);
        while(source.width/sample>size[0]||source.height/sample>size[1])sample*=2;
        options.inSampleSize=sample;
        Bitmap bitmap=BitmapFactory.decodeFile(source.file.getPath(),options);if(bitmap==null)return null;
        Matrix matrix=new Matrix();
        switch(source.orientation) {
            case 2:matrix.setScale(-1,1);break;
            case 3:matrix.setRotate(180);break;
            case 4:matrix.setRotate(180);matrix.postScale(-1,1);break;
            case 5:matrix.setRotate(90);matrix.postScale(-1,1);break;
            case 6:matrix.setRotate(90);break;
            case 7:matrix.setRotate(-90);matrix.postScale(-1,1);break;
            case 8:matrix.setRotate(-90);break;
            default:return bitmap;
        }
        try {Bitmap rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);if(rotated!=bitmap)bitmap.recycle();return rotated;}
        catch(RuntimeException|OutOfMemoryError error){bitmap.recycle();throw error;}
    }
    private void show(PhotoSceneView.Scene group,int ticket) {
        empty.setVisibility(GONE);
        final int next=1-front,old=front;
        scenes[next].animate().cancel();scenes[old].animate().cancel();
        scenes[next].setScene(group);scenes[next].setAlpha(0f);
        scenes[next].animate().alpha(1f).setDuration(1500).start();
        scenes[old].animate().alpha(0f).setDuration(1500).withEndAction(()->{
            if(ticket==generation&&front!=old)scenes[old].clear();
        }).start();front=next;
    }
}
