package au.wright.ambient;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** A current/incoming scene owns at most two fitted photos and one tiny static colour wash. */
final class PhotoSceneView extends View {
    static final class Scene {
        final Bitmap[] photos;
        final Bitmap background;
        Scene(Bitmap[] photos) {
            this.photos=photos;
            Bitmap wash=null;
            try {
                wash=Bitmap.createBitmap(48,27,Bitmap.Config.ARGB_8888);
                Canvas canvas=new Canvas(wash);Paint paint=new Paint(Paint.FILTER_BITMAP_FLAG);
                for(int i=0;i<photos.length;i++)
                    canvas.drawBitmap(photos[i],null,new RectF(i*48f/photos.length,0,(i+1)*48f/photos.length,27),paint);
            } catch(OutOfMemoryError ignored) { if(wash!=null)wash.recycle();wash=null; }
            background=wash;
        }
        void recycle() {
            for(Bitmap photo:photos)if(photo!=null&&!photo.isRecycled())photo.recycle();
            if(background!=null&&!background.isRecycled())background.recycle();
        }
    }
    private Scene scene;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF rect=new RectF();
    PhotoSceneView(Context context) { super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    void setScene(Scene value) { clear();scene=value;invalidate(); }
    void clear() { if(scene!=null){scene.recycle();scene=null;}invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(scene==null)return;
        PhotoPresentation.Layout layout=new PhotoPresentation.Layout(getWidth(),getHeight());
        int save=canvas.save();canvas.scale(getWidth()/layout.width,getHeight()/layout.height);
        if(scene.background!=null) {
            paint.setColor(Color.WHITE);rect.set(0,0,layout.width,layout.height);
            canvas.drawBitmap(scene.background,null,rect,paint);
        }
        // A cream veil keeps even dark source photos light; foreground pixels are unchanged.
        canvas.drawColor(0xe0f5f1e7);
        for(int i=0;i<scene.photos.length;i++) {
            Bitmap photo=scene.photos[i];
            PhotoPresentation.Box box=PhotoPresentation.fit(photo.getWidth(),photo.getHeight(),layout.slot(i,scene.photos.length));
            rect.set(box.x-6,box.y-6,box.x+box.width+6,box.y+box.height+6);
            paint.setColor(0xfffaf8f1);canvas.drawRoundRect(rect,12,12,paint);
            rect.set(box.x,box.y,box.x+box.width,box.y+box.height);
            paint.setColor(Color.WHITE);canvas.drawBitmap(photo,null,rect,paint);
        }
        canvas.restoreToCount(save);
    }
}
