package au.wright.ambient;

import android.app.Activity;
import android.os.Bundle;
import android.view.*;

/** Explicit user preview only. Never launched by timers, HA or a boot receiver. */
public final class PreviewActivity extends Activity {
    private SlideshowView slideshow;
    @Override public void onCreate(Bundle state){super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        slideshow=new SlideshowView(this);setContentView(slideshow);}
    @Override public void onResume(){super.onResume();slideshow.start();}
    @Override public void onPause(){slideshow.stop();super.onPause();}
    @Override public boolean dispatchKeyEvent(KeyEvent event){if(event.getAction()==KeyEvent.ACTION_DOWN)finish();return true;}
}
