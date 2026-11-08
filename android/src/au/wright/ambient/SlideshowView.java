package au.wright.ambient;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

/** Two image layers and a permanent overlay; network and decoding stay off the UI thread. */
public final class SlideshowView extends FrameLayout {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final PhotoStore store;
    private final ImageView[] images=new ImageView[2];
    private final TextView overlay;
    private final TextView empty;
    private ScheduledExecutorService worker;
    private ExecutorService decoder;
    private volatile boolean running;
    private volatile String photoStatus="Not checked", weatherStatus="Not checked";
    private int generation=0,front=0,index=0;
    private Bitmap[] bitmaps=new Bitmap[2];
    public SlideshowView(Context context) {
        super(context);setBackgroundColor(Color.BLACK);store=new PhotoStore(context);
        for(int i=0;i<2;i++) {
            images[i]=new ImageView(context);images[i].setScaleType(ImageView.ScaleType.FIT_CENTER);
            images[i].setAlpha(i==0?1f:0f);addView(images[i],new LayoutParams(-1,-1));
        }
        empty=new TextView(context);empty.setText("Waiting for family photos\nOpen Wright Ambient setup to check the photo host.");
        empty.setTextColor(0xffaaaaaa);empty.setTextSize(20);empty.setGravity(Gravity.CENTER);addView(empty,new LayoutParams(-1,-1));
        overlay=new TextView(context);overlay.setTextColor(Color.argb(220,255,255,255));
        overlay.setTextSize(20);overlay.setGravity(Gravity.RIGHT);overlay.setShadowLayer(3,0,1,Color.BLACK);
        overlay.setPadding(dp(28),dp(16),dp(28),dp(16));
        overlay.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{Color.TRANSPARENT,0x90000000}));
        LayoutParams p=new LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.RIGHT);p.setMargins(dp(24),dp(24),dp(24),dp(24));addView(overlay,p);
    }
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
    public void start() {
        if(running)return;running=true;generation++;index=0;
        worker=Executors.newSingleThreadScheduledExecutor();
        decoder=Executors.newSingleThreadExecutor();
        handler.post(tick);queuePhoto();
        worker.scheduleWithFixedDelay(()->{
            if(!running)return;
            try{store.sync();photoStatus="Album cached";}catch(Exception ignored){photoStatus="No album cached (empty feed or connection failure)";}
            if(!running)return;
            try{store.syncWeather();weatherStatus="Feed received";}catch(Exception ignored){weatherStatus="Feed unavailable";}
        },0,300,TimeUnit.SECONDS);
    }
    public void stop() {
        if(!running)return;running=false;generation++;handler.removeCallbacksAndMessages(null);
        if(worker!=null)worker.shutdownNow();
        if(decoder!=null)decoder.shutdownNow();
        for(int i=0;i<2;i++){images[i].animate().cancel();images[i].setImageDrawable(null);if(bitmaps[i]!=null){bitmaps[i].recycle();bitmaps[i]=null;}}
    }
    private final Runnable tick=new Runnable(){public void run(){if(running){updateOverlay();handler.postDelayed(this,1000);}}};
    private final Runnable advance=()->queuePhoto();
    private void queuePhoto() {
        if(!running||decoder==null)return;final int ticket=generation;
        decoder.execute(()->{
            List<String> photos=store.photos();Bitmap bitmap=null;
            for(int attempt=0;attempt<photos.size();attempt++) {
                File photo=store.file(photos.get((index++)%photos.size()));
                try {
                    if(Build.VERSION.SDK_INT>=28) bitmap=ImageDecoder.decodeBitmap(ImageDecoder.createSource(photo),(decoder,info,source)->{
                        int sample=Policy.sampleSize(info.getSize().getWidth(),info.getSize().getHeight());
                        decoder.setTargetSampleSize(sample);decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    });
                    else {
                        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(photo.getPath(),o);
                        o.inSampleSize=Policy.sampleSize(o.outWidth,o.outHeight);o.inJustDecodeBounds=false;bitmap=BitmapFactory.decodeFile(photo.getPath(),o);
                    }
                    if(bitmap!=null)break;
                } catch(Exception|OutOfMemoryError ignored){}
            }
            final Bitmap decoded=bitmap;
            handler.post(()->{
                if(!running||ticket!=generation){if(decoded!=null)decoded.recycle();return;}
                if(decoded!=null)show(decoded);
                int seconds=Policy.slideSeconds(getContext().getSharedPreferences("ambient",Context.MODE_PRIVATE).getInt("seconds",30));
                handler.postDelayed(advance,decoded==null?5000:seconds*1000L);
            });
        });
    }
    private void show(Bitmap bitmap) {
        empty.setVisibility(GONE);
        final int next=1-front,old=front;
        images[next].animate().cancel();images[old].animate().cancel();
        images[next].setImageDrawable(null);if(bitmaps[next]!=null)bitmaps[next].recycle();bitmaps[next]=bitmap;
        images[next].setImageBitmap(bitmap);images[next].setAlpha(0f);images[next].animate().alpha(1f).setDuration(1500).start();
        images[old].animate().alpha(0f).setDuration(1500).start();front=next;
    }
    private static String conditionLabel(String condition) {
        switch(condition) {
            case "partlycloudy": return "Partly cloudy";
            case "clear-night": return "Clear night";
            case "lightning-rainy": return "Thunderstorms and rain";
            case "lightning": return "Thunderstorms";
            case "snowy-rainy": return "Sleet";
            case "windy-variant": return "Windy and cloudy";
            case "pouring": return "Heavy rain";
            case "exceptional": return "Exceptional weather";
            case "": case "unknown": case "unavailable": return "Weather unavailable";
            default:
                String label=condition.replace('-',' ').replace('_',' ');
                return label.substring(0,1).toUpperCase(Locale.ROOT)+label.substring(1);
        }
    }
    private void updateOverlay() {
        if(empty.getVisibility()==VISIBLE)empty.setText("Wright Ambient - infrastructure trial\nNo photos required\n\nPhotos: "+photoStatus+"\nWeather: "+weatherStatus+"\nRefresh runs at entry and every five minutes.\nA received weather feed may still contain no weather readings.");
        JSONObject w=store.weather();TimeZone zone=TimeZone.getTimeZone(w.optString("timeZone","Australia/Sydney"));
        Date now=new Date();SimpleDateFormat time=new SimpleDateFormat("h:mm a",Locale.getDefault());time.setTimeZone(zone);
        SimpleDateFormat day=new SimpleDateFormat("EEE, d MMM yyyy",Locale.getDefault());day.setTimeZone(zone);
        SimpleDateFormat key=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);key.setTimeZone(zone);
        String weather="Weather unavailable";
        if(!w.isNull("temperature")&&w.has("temperature")) {
            weather=String.format(Locale.getDefault(),"%.0f%s  %s",w.optDouble("temperature"),w.optString("unit","°C"),conditionLabel(w.optString("condition","")));
            if(key.format(now).equals(w.optString("forecastDay"))&&!w.isNull("high")&&!w.isNull("low"))
                weather+=String.format(Locale.getDefault(),"\nHigh %.0f°  Low %.0f°",w.optDouble("high"),w.optDouble("low"));
            boolean stale=w.optBoolean("stale",true)||w.optBoolean("forecastStale",false);
            try{stale|=now.getTime()-Instant.parse(w.getString("fetchedAt")).toEpochMilli()>1800000;}catch(Exception e){stale=true;}
            if(stale)weather+="  · cached";
            String attribution=w.optString("attribution","").trim();
            if(!attribution.isEmpty())weather+="\n"+attribution;
        }
        overlay.setText(time.format(now)+"\n"+day.format(now)+"\n"+weather);
    }
}
