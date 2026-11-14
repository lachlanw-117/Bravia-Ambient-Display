package au.wright.ambient;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.*;

/** Quiet clock/weather sidebar with display-only household notices. */
final class InformationPanel extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private WeatherPresentation.Forecast today,tomorrow;
    private String clock="",period="",weekday="",date="",temperature="\u2014",status="";
    private final Typeface light=Typeface.create("sans-serif-light",Typeface.NORMAL);
    private final Typeface normal=Typeface.create("sans-serif",Typeface.NORMAL);
    private String lastContent="";
    private JSONObject lastWeather;
    private long lastMinute=Long.MIN_VALUE;
    private List<NoticePresentation.Item> notices=Collections.emptyList();
    private String noticeStatus="Notices unavailable",noticeContent="";
    private List<CalendarPresentation.Item> occasions=Collections.emptyList();
    private String calendarStatus="Calendar unavailable",calendarContent="";
    private java.time.Instant calendarNow=java.time.Instant.EPOCH;
    void updateCalendar(CalendarPresentation.Snapshot snapshot,Date now) {
        java.time.Instant instant=now.toInstant();
        List<CalendarPresentation.Item> next=CalendarPresentation.visible(snapshot,instant);
        String status=CalendarPresentation.status(snapshot,instant);
        StringBuilder signature=new StringBuilder(status);
        for(CalendarPresentation.Item item:next)signature.append('|').append(item.id).append('|').append(item.title)
            .append('|').append(CalendarPresentation.detail(item,instant));
        if(signature.toString().equals(calendarContent))return;
        occasions=next;calendarStatus=status;calendarContent=signature.toString();calendarNow=instant;
        accessibility();invalidate();
    }
    void updateNotices(NoticePresentation.Snapshot snapshot,Date now) {
        List<NoticePresentation.Item> next=NoticePresentation.visible(snapshot,now.toInstant());
        String status=NoticePresentation.status(snapshot,now.toInstant());
        StringBuilder signature=new StringBuilder(status);
        for(NoticePresentation.Item item:next)signature.append('|').append(item.id).append('|').append(item.title)
            .append('|').append(item.severity).append('|').append(item.quality);
        if(signature.toString().equals(noticeContent))return;
        notices=next;noticeStatus=status;noticeContent=signature.toString();
        accessibility();invalidate();
    }
    private void accessibility() {
        StringBuilder description=new StringBuilder(lastContent);
        description.append(NoticePresentation.showNotices(notices.size())?". Home Assistant":". Coming up");
        for(NoticePresentation.Item item:notices)description.append(". ").append(item.title);
        if(!noticeStatus.isEmpty())description.append(". ").append(noticeStatus);
        if(!NoticePresentation.showNotices(notices.size()))for(CalendarPresentation.Item item:occasions)description.append(". ").append(item.title).append(" ")
            .append(CalendarPresentation.detail(item,calendarNow));
        if(!NoticePresentation.showNotices(notices.size())&&!calendarStatus.isEmpty())description.append(". ").append(calendarStatus);
        setContentDescription(description.toString());
    }
    InformationPanel(Context context) { super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    void update(JSONObject weather,Date now) {
        long minute=now.getTime()/60000;
        if(weather==lastWeather&&minute==lastMinute)return;
        boolean newWeather=weather!=lastWeather;
        lastWeather=weather;lastMinute=minute;
        TimeZone zone=TimeZone.getTimeZone(weather.optString("timeZone","Australia/Sydney"));
        clock=format("h:mm",now,zone);period=format("a",now,zone);
        weekday=format("EEEE",now,zone);date=format("d MMMM yyyy",now,zone);
        boolean available=weather.has("temperature")&&!weather.isNull("temperature")&&Double.isFinite(weather.optDouble("temperature"));
        temperature=available?String.format(Locale.getDefault(),"%.0f%s",weather.optDouble("temperature"),weather.optString("unit","°C")):"—";
        java.time.Instant instant=now.toInstant();
        java.time.LocalDate local=WeatherPresentation.day(instant,zone.getID());
        List<WeatherPresentation.Forecast> rows=new ArrayList<>();
        org.json.JSONArray daily=weather.optJSONArray("dailyForecasts");
        if(daily!=null)for(int i=0;i<Math.min(daily.length(),2);i++) {
            JSONObject row=daily.optJSONObject(i);if(row==null)continue;
            rows.add(new WeatherPresentation.Forecast(row.optString("date"),row.optString("condition",null),
                number(row,"high"),number(row,"low"),row.optString("checkedAt",null),
                row.optBoolean("cached",false)||row.optBoolean("stale",false)));
        }
        // Legacy host compatibility supplies today's range, never a guessed condition.
        if(daily==null&&local.toString().equals(weather.optString("forecastDay")))
            rows.add(new WeatherPresentation.Forecast(local.toString(),null,number(weather,"high"),number(weather,"low"),
                weather.optString("forecastAt",null),weather.optBoolean("forecastStale",true)));
        today=WeatherPresentation.find(rows,local);tomorrow=WeatherPresentation.find(rows,local.plusDays(1));
        long fetchedAge=WeatherPresentation.age(weather.optString("fetchedAt",null),instant);
        long reportAge=WeatherPresentation.age(weather.optString("haReportedAt",weather.optString("observedAt",null)),instant);
        boolean stale=weather.optBoolean("stale",true)||fetchedAge<0||fetchedAge>1800||reportAge<0||reportAge>10800;
        if(stale&&newWeather)android.util.Log.w("AmbientWeather","Freshness: checkAgeSeconds="+fetchedAge+
            " reportAgeSeconds="+reportAge+" feedStale="+weather.optBoolean("stale",true));
        status=!available?"Waiting for weather":WeatherPresentation.freshness(weather.optString("fetchedAt",null),stale,today,tomorrow,instant);
        String content=weekday+" "+date+" "+clock+" "+period+" Now "+temperature+
            " Today "+forecastContent(today)+" Tomorrow "+forecastContent(tomorrow)+" "+status;
        if(!content.equals(lastContent)){lastContent=content;accessibility();invalidate();}
    }
    private static Double number(JSONObject row,String key) {
        return row.has(key)&&!row.isNull(key)&&Double.isFinite(row.optDouble(key))?row.optDouble(key):null;
    }
    private static String forecastContent(WeatherPresentation.Forecast row) {
        return WeatherPresentation.condition(row==null?null:row.condition)+" "+WeatherPresentation.range(row);
    }
    private static String format(String pattern,Date now,TimeZone zone) {
        SimpleDateFormat formatter=new SimpleDateFormat(pattern,Locale.getDefault());formatter.setTimeZone(zone);return formatter.format(now);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(getWidth()==0||getHeight()==0)return;
        PhotoPresentation.Layout layout=new PhotoPresentation.Layout(getWidth(),getHeight());
        int save=canvas.save();canvas.scale(getWidth()/layout.width,getHeight()/layout.height);
        float h=layout.height,x=layout.panelLeft+h*0.045f,right=layout.width-h*0.045f,w=right-x;
        paint.setColor(0xfff5f1e7);canvas.drawRect(layout.panelLeft,0,layout.width,h,paint);
        paint.setTypeface(normal);paint.setTextSize(h*0.023f);
        if(paint.measureText(weekday+" "+date)<=w)
            text(canvas,weekday+" "+date,x,h*0.07f,h*0.023f,w,0xff60665c,normal);
        else {
            text(canvas,weekday,x,h*0.045f,h*0.023f,w,0xff60665c,normal);
            text(canvas,date,x,h*0.075f,h*0.023f,w,0xff60665c,normal);
        }
        paint.setTypeface(light);paint.setTextSize(h*0.078f);paint.setColor(0xff30342f);
        float clockWidth=paint.measureText(clock);
        text(canvas,clock,x,h*0.16f,h*0.078f,w,0xff30342f,light);
        text(canvas,period,x+clockWidth+12,h*0.16f,h*0.023f,Math.max(1,w-clockWidth-12),0xff60665c,normal);
        text(canvas,"NOW",x,h*0.212f,h*0.018f,w,0xff60665c,normal);
        text(canvas,temperature,x,h*0.284f,h*0.07f,w,0xff30342f,light);
        float todayEnd=forecast(canvas,"TODAY",today,x,h*0.344f,h,w);
        float tomorrowEnd=forecast(canvas,"TOMORROW",tomorrow,x,Math.max(h*0.444f,todayEnd+h*0.04f),h,w);
        float checkedEnd=paragraph(canvas,status,x,tomorrowEnd+h*0.036f,h*0.018f,w,0xff60665c,true);
        float divider=checkedEnd+h*0.022f;
        paint.setColor(0xffd5d6ca);paint.setStrokeWidth(1);canvas.drawLine(x,divider,right,divider,paint);
        drawNotices(canvas,x,divider+h*0.015f,h,w);
        canvas.restoreToCount(save);
    }
    private void drawNotices(Canvas canvas,float x,float top,float h,float w) {
        float bottom=h*.955f;
        if(!noticeStatus.isEmpty())bottom-=h*.052f;
        if(!NoticePresentation.showNotices(notices.size())) {
            drawCalendar(canvas,x,top,bottom,h,w);
            if(!noticeStatus.isEmpty())paragraph(canvas,"Home Assistant: "+noticeStatus,x,h*.935f,h*.018f,w,0xff60665c,false);
            return;
        }
        text(canvas,"HOME ASSISTANT",x,top+h*.03f,h*.018f,w,0xff60665c,normal);
        float bodyTop=top+h*.045f;
        int measured=Math.min(5,notices.size());
        StaticLayout[] blocks=new StaticLayout[measured];float[] heights=new float[measured];
        for(int i=0;i<measured;i++) {
            blocks[i]=textBlock(notices.get(i).title,h*.022f,w-h*.021f,0xff30342f,false);
            heights[i]=blocks[i].getHeight()+h*.018f;
        }
        int count=NoticePresentation.fittedCount(notices.size(),heights,bottom-bodyTop,h*.035f);
        float rowTop=bodyTop;
        for(int i=0;i<count;i++) {
            NoticePresentation.Item item=notices.get(i);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(item.severity.equals("important")?0xff9d4e32:item.severity.equals("warning")?0xff976621:0xff52645c);
            canvas.drawCircle(x+h*.005f,rowTop+blocks[i].getLineBaseline(0)-h*.007f,h*.0045f,paint);
            int save=canvas.save();canvas.translate(x+h*.021f,rowTop);blocks[i].draw(canvas);canvas.restoreToCount(save);
            rowTop+=heights[i];
        }
        int overflow=NoticePresentation.overflow(notices.size(),count);
        if(overflow>0)text(canvas,"+"+overflow+" more · View in HA",x,rowTop+h*.025f,h*.018f,w,0xff60665c,normal);
        if(!noticeStatus.isEmpty())paragraph(canvas,noticeStatus,x,h*.935f,h*.018f,w,0xff60665c,false);
    }
    private void drawCalendar(Canvas canvas,float x,float top,float bottom,float h,float w) {
        boolean status=!calendarStatus.isEmpty();
        int count=Math.min(occasions.size(),CalendarPresentation.capacity(top,bottom,h,status));
        if(count==0&&(!occasions.isEmpty()||!CalendarPresentation.statusFits(top,bottom,h)))return;
        text(canvas,"COMING UP",x,top+h*.03f,h*.018f,w,0xff60665c,normal);
        for(int i=0;i<count;i++) {
            CalendarPresentation.Item item=occasions.get(i);float baseline=top+h*.065f+i*h*.08f;
            String countdown=CalendarPresentation.countdown(item,calendarNow);
            int colour=countdown.equals("Today")||countdown.equals("Tomorrow")?0xff9d4e32:0xff60665c;
            text(canvas,item.title,x,baseline,h*.022f,w,0xff30342f,normal);
            text(canvas,CalendarPresentation.detail(item,calendarNow),x,baseline+h*.026f,h*.018f,w,colour,normal);
        }
        if(status)paragraph(canvas,calendarStatus,x,top+h*.065f+count*h*.08f,h*.018f,w,0xff60665c,false);
        else if(occasions.isEmpty())text(canvas,"No upcoming occasions",x,top+h*.065f,h*.018f,w,0xff60665c,normal);
    }
    private float forecast(Canvas canvas,String label,WeatherPresentation.Forecast row,float x,float baseline,float h,float w) {
        text(canvas,label,x,baseline,h*0.018f,w,0xff60665c,normal);
        String forecastCode=row==null?null:row.condition;
        boolean icon=!WeatherPresentation.condition(forecastCode).equals("Forecast unavailable");
        if(icon)glyph(canvas,x,baseline+h*0.005f,h*0.028f,forecastCode);
        float inset=icon?h*0.043f:0;
        float end=paragraph(canvas,WeatherPresentation.condition(forecastCode),x+inset,baseline+h*0.03f,h*0.022f,w-inset,0xff30342f,false);
        String high=WeatherPresentation.highLabel(row),low=WeatherPresentation.lowLabel(row);
        if(high.isEmpty()&&low.isEmpty())return end;
        float rangeY=Math.max(baseline+h*0.057f,end+h*0.027f);
        text(canvas,high,x,rangeY,h*0.022f,w,0xff9d4e32,normal);
        paint.setTypeface(normal);paint.setTextSize(h*0.022f);
        float lowX=x+(high.isEmpty()?0:paint.measureText(high+"   "));
        text(canvas,low,lowX,rangeY,h*0.022f,w-(lowX-x),0xff356e85,normal);
        return rangeY;
    }
    private float paragraph(Canvas canvas,String value,float x,float baseline,float size,float width,int colour,boolean right) {
        TextPaint style=new TextPaint(Paint.ANTI_ALIAS_FLAG);style.setTypeface(normal);style.setTextSize(size);style.setColor(colour);
        StaticLayout block=StaticLayout.Builder.obtain(value,0,value.length(),style,Math.max(1,(int)width))
            .setAlignment(right?Layout.Alignment.ALIGN_OPPOSITE:Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build();
        int save=canvas.save();canvas.translate(x,baseline-block.getLineBaseline(0));block.draw(canvas);canvas.restoreToCount(save);
        return baseline+block.getLineBaseline(block.getLineCount()-1)-block.getLineBaseline(0);
    }
    private StaticLayout textBlock(String value,float size,float width,int colour,boolean right) {
        TextPaint style=new TextPaint(Paint.ANTI_ALIAS_FLAG);style.setTypeface(normal);style.setTextSize(size);style.setColor(colour);
        return StaticLayout.Builder.obtain(value,0,value.length(),style,Math.max(1,(int)width))
            .setAlignment(right?Layout.Alignment.ALIGN_OPPOSITE:Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false).setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).build();
    }
    private void text(Canvas canvas,String text,float x,float baseline,float size,float width,int colour,Typeface face) {
        if(text.isEmpty())return;paint.setTypeface(face);paint.setTextSize(size);
        text=TextUtils.ellipsize(text,new TextPaint(paint),Math.max(1,width),TextUtils.TruncateAt.END).toString();
        paint.setColor(colour);paint.setStyle(Paint.Style.FILL);canvas.drawText(text,x,baseline,paint);
    }
    private void glyph(Canvas canvas,float x,float y,float size,String code) {
        int save=canvas.save();canvas.translate(x,y);canvas.scale(size/100,size/100);
        paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);
        if(code.isEmpty()||code.equals("unknown")||code.equals("unavailable")) {
            paint.setColor(0xff60665c);canvas.drawLine(20,50,80,50,paint);canvas.restoreToCount(save);return;
        }
        if(code.equals("sunny")||code.equals("partlycloudy")) {
            paint.setColor(0xffdca229);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(64,32,18,paint);
            for(int i=0;i<8;i++) { double angle=i*Math.PI/4;canvas.drawLine(64+(float)Math.cos(angle)*25,32+(float)Math.sin(angle)*25,64+(float)Math.cos(angle)*32,32+(float)Math.sin(angle)*32,paint); }
        }
        if(code.equals("clear-night")) {
            paint.setColor(0xff686047);paint.setStyle(Paint.Style.STROKE);rect.set(20,10,80,70);canvas.drawArc(rect,35,285,false,paint);
        } else if(!code.equals("sunny")) {
            paint.setColor(0xff4d7988);paint.setStyle(Paint.Style.FILL);
            Path cloud=new Path();cloud.moveTo(24,72);cloud.cubicTo(-2,72,0,39,25,40);
            cloud.cubicTo(25,12,67,10,73,42);cloud.cubicTo(101,36,108,72,80,72);cloud.close();
            paint.setColor(0xffb4c7cd);canvas.drawPath(cloud,paint);
            paint.setColor(0xff4d7988);paint.setStyle(Paint.Style.STROKE);canvas.drawPath(cloud,paint);
            if(code.contains("rain")||code.equals("pouring")) {
                paint.setColor(0xff356e85);for(int i=0;i<3;i++)canvas.drawLine(27+i*23,83,21+i*23,96,paint);
            }
            if(code.contains("lightning")) {
                paint.setColor(0xffdca229);Path bolt=new Path();bolt.moveTo(60,72);bolt.lineTo(49,87);bolt.lineTo(61,87);bolt.lineTo(51,100);canvas.drawPath(bolt,paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
}
