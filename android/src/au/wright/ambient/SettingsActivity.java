package au.wright.ambient;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.*;

public final class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(48,24,48,24);
        TextView title=new TextView(this);title.setText("Wright Ambient · Setup");title.setTextSize(28);form.addView(title);
        TextView info=new TextView(this);info.setText("Photos stay on this TV for offline playback. Any remote button exits the screensaver.\nIdle timing is controlled by the TV, not this app.");form.addView(info);
        final SharedPreferences prefs=getSharedPreferences("ambient",MODE_PRIVATE);
        final EditText base=new EditText(this);base.setHint("Photo host address, e.g. http://192.168.1.20:8099");base.setSingleLine(true);
        base.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);base.setText(prefs.getString("base",""));form.addView(base);
        final EditText key=new EditText(this);key.setHint("Photo host access key (not your HA token)");key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);key.setText(prefs.getString("key",""));form.addView(key);
        final EditText seconds=new EditText(this);seconds.setHint("Seconds per photo (10–300)");seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        seconds.setText(String.valueOf(prefs.getInt("seconds",30)));form.addView(seconds);
        Button save=new Button(this);save.setText("Save settings");form.addView(save);
        save.setOnClickListener(v->{
            String address=base.getText().toString().trim();String access=key.getText().toString().trim();int delay;
            try{delay=Integer.parseInt(seconds.getText().toString());}catch(Exception e){delay=30;}
            if(!PhotoStore.allowedBase(address)||access.length()<24){Toast.makeText(this,"Enter a LAN HTTP or HTTPS address and a key of at least 24 characters",Toast.LENGTH_LONG).show();return;}
            prefs.edit().putString("base",address).putString("key",access).putInt("seconds",Policy.slideSeconds(delay)).apply();
            Toast.makeText(this,"Saved",Toast.LENGTH_SHORT).show();
        });
        Button preview=new Button(this);preview.setText("Preview photos");form.addView(preview);
        preview.setOnClickListener(v->startActivity(new Intent(this,PreviewActivity.class)));
        Button system=new Button(this);system.setText("Open TV screensaver settings");form.addView(system);
        system.setOnClickListener(v->{try{startActivity(new Intent(Settings.ACTION_DREAM_SETTINGS));}
            catch(ActivityNotFoundException e){Toast.makeText(this,"This TV hides the screensaver picker. See installation notes.",Toast.LENGTH_LONG).show();}});
        ScrollView scroll=new ScrollView(this);scroll.addView(form);setContentView(scroll);
    }
}
