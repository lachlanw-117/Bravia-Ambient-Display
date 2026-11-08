package au.wright.ambient;

/** Pure layout and sequence rules, shared with the synthetic JVM acceptance tests. */
public final class PhotoPresentation {
    public static final class Box {
        public final float x,y,width,height;
        public Box(float x,float y,float width,float height) {
            this.x=x;this.y=y;this.width=width;this.height=height;
        }
    }
    public static final class Layout {
        public final float width,height,panelLeft,gap;
        public final Box photo;
        public Layout(int screenWidth,int screenHeight) {
            if(screenWidth<=0||screenHeight<=0)throw new IllegalArgumentException("Invalid surface");
            float scale=Math.min(1f,Math.min(1920f/screenWidth,1080f/screenHeight));
            width=screenWidth*scale;height=screenHeight*scale;panelLeft=width*0.72f;gap=width*0.014f;
            photo=new Box(width*0.035f,height*0.06f,width*0.65f,height*0.88f);
        }
        public Box slot(int index,int count) {
            if((count!=1&&count!=2)||index<0||index>=count)throw new IllegalArgumentException("Invalid group");
            float w=(photo.width-gap*(count-1))/count;
            return new Box(photo.x+index*(w+gap),photo.y,w,photo.height);
        }
    }
    public static boolean portrait(int width,int height) { return width>0&&height>width; }
    public static int groupSize(boolean pairs,int index,int total,int w,int h,int nextW,int nextH,Layout layout) {
        // Never pair the last item with the first item of the next album cycle.
        return pairs&&index+1<total&&portrait(w,h)&&portrait(nextW,nextH)
            &&layout.photo.width>layout.photo.height?2:1;
    }
    public static int nextIndex(int index,int taken,int total) {
        if(total<=0||index<0||index>=total||taken<1||index+taken>total)
            throw new IllegalArgumentException("Invalid sequence");
        return (index+taken)%total;
    }
    public static Box fit(int width,int height,Box box) {
        if(width<=0||height<=0||box.width<=0||box.height<=0)throw new IllegalArgumentException("Invalid image bounds");
        float scale=Math.min(box.width/width,box.height/height);
        float w=width*scale,h=height*scale;
        return new Box(box.x+(box.width-w)/2,box.y+(box.height-h)/2,w,h);
    }
    public static int[] decodeSize(int width,int height,Box box) {
        Box fitted=fit(width,height,box);
        // Never upscale a source while decoding; Canvas handles final screen scaling.
        return new int[]{Math.max(1,Math.min(width,(int)Math.ceil(fitted.width))),
            Math.max(1,Math.min(height,(int)Math.ceil(fitted.height)))};
    }
    private PhotoPresentation() {}
}
