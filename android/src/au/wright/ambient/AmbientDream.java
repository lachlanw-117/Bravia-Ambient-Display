package au.wright.ambient;

import android.service.dreams.DreamService;

/** System-owned idle lifecycle. No launch timer, boot receiver, wake lock or device control. */
public final class AmbientDream extends DreamService {
    private SlideshowView slideshow;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false); // Any remote interaction dismisses the MVP immediately.
        setFullscreen(true);
        slideshow=new SlideshowView(this);setContentView(slideshow);
    }
    @Override public void onDreamingStarted(){super.onDreamingStarted();slideshow.start();}
    @Override public void onDreamingStopped(){if(slideshow!=null)slideshow.stop();super.onDreamingStopped();}
    @Override public void onDetachedFromWindow(){if(slideshow!=null)slideshow.stop();super.onDetachedFromWindow();}
    @Override public void onWakeUp(){finish();}
}
