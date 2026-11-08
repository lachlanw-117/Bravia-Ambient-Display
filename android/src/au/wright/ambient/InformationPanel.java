package au.wright.ambient;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/** Quiet clock/weather sidebar. No notices, provider-credit line or device controls. */
final class InformationPanel extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect=new RectF();
    private String clock="",period="",weekday="",date="",temperature="—",condition="Weather unavailable",forecast="",status="",code="";
    private final Typeface light=Typeface.create("sans-serif-light",Typeface.NORMAL);
    private final Typeface normal=Typeface.create("sans-serif",Typeface.NORMAL);
    private String lastContent="";
    private JSONObject lastWeather;
    private long lastMinute=Long.MIN_VALUE;
    InformationPanel(Context context) { super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    void update(JSONObject weather,Date now) {
        long minute=now.getTime()/60000;
        if(weather==lastWeather&&minute==lastMinute)return;
        lastWeather=weather;lastMinute=minute;
        TimeZone zone=TimeZone.getTimeZone(weather.optString("timeZone","Australia/Sydney"));
        clock=format("h:mm",now,zone);period=format("a",now,zone);
        weekday=format("EEEE",now,zone).toUpperCase(Locale.getDefault());date=format("d MMMM yyyy",now,zone);
        boolean available=weather.has("temperature")&&!weather.isNull("temperature")&&Double.isFinite(weather.optDouble("temperature"));
        temperature=available?String.format(Locale.getDefault(),"%.0f%s",weather.optDouble("temperature"),weather.optString("unit","°C")):"—";
        code=available?weather.optString("condition",""):"";condition=conditionLabel(code);
        forecast="";
        if(available&&format("yyyy-MM-dd",now,zone).equals(weather.optString("forecastDay"))) {
            String high=value(weather,"high"),low=value(weather,"low");
            if(!high.isEmpty()||!low.isEmpty())forecast="High "+(high.isEmpty()?"—":high)+"   Low "+(low.isEmpty()?"—":low);
        }
        boolean stale=weather.optBoolean("stale",true);
        try { stale|=now.getTime()-Instant.parse(weather.getString("fetchedAt")).toEpochMilli()>1800000; }
        catch(Exception ignored){stale=true;}
        status=!available?"Waiting for weather":stale?"Cached weather":
            !forecast.isEmpty()&&weather.optBoolean("forecastStale",true)?"Cached forecast":"";
        String content=weekday+" "+clock+" "+period+" "+date+" "+temperature+" "+condition+" "+forecast+" "+status;
        if(!content.equals(lastContent)){lastContent=content;setContentDescription(content);invalidate();}
    }
    private static String value(JSONObject weather,String key) {
        return weather.has(key)&&!weather.isNull(key)&&Double.isFinite(weather.optDouble(key))
            ?String.format(Locale.getDefault(),"%.0f°",weather.optDouble(key)):"";
    }
    private static String format(String pattern,Date now,TimeZone zone) {
        SimpleDateFormat formatter=new SimpleDateFormat(pattern,Locale.getDefault());formatter.setTimeZone(zone);return formatter.format(now);
    }
    private static String conditionLabel(String value) {
        switch(value) {
            case "partlycloudy":return "Partly cloudy";
            case "clear-night":return "Clear night";
            case "lightning-rainy":return "Thunderstorms and rain";
            case "lightning":return "Thunderstorms";
            case "snowy-rainy":return "Sleet";
            case "windy-variant":return "Windy and cloudy";
            case "pouring":return "Heavy rain";
            case "exceptional":return "Exceptional weather";
            case "":case "unknown":case "unavailable":return "Weather unavailable";
            default:String label=value.replace('-',' ').replace('_',' ');return label.substring(0,1).toUpperCase(Locale.ROOT)+label.substring(1);
        }
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(getWidth()==0||getHeight()==0)return;
        PhotoPresentation.Layout layout=new PhotoPresentation.Layout(getWidth(),getHeight());
        int save=canvas.save();canvas.scale(getWidth()/layout.width,getHeight()/layout.height);
        float h=layout.height,x=layout.panelLeft+h*0.045f,right=layout.width-h*0.045f,w=right-x;
        paint.setColor(0xce10171a);canvas.drawRect(layout.panelLeft,0,layout.width,h,paint);
        text(canvas,weekday,x,h*0.15f,h*0.018f,w,0xffb8b9af,normal);
        paint.setTypeface(light);paint.setTextSize(h*0.078f);paint.setColor(0xfff6f1e6);
        float clockWidth=paint.measureText(clock);
        text(canvas,clock,x,h*0.255f,h*0.078f,w,0xfff6f1e6,light);
        text(canvas,period,x+clockWidth+12,h*0.255f,h*0.023f,Math.max(1,w-clockWidth-12),0xffd4d5cd,normal);
        text(canvas,date,x,h*0.305f,h*0.023f,w,0xffd4d5cd,normal);
        glyph(canvas,x,h*0.405f,h*0.09f);
        text(canvas,temperature,x+h*0.115f,h*0.48f,h*0.07f,w-h*0.115f,0xfff6f1e6,light);
        text(canvas,condition,x,h*0.55f,h*0.027f,w,0xffeeece2,normal);
        text(canvas,forecast,x,h*0.60f,h*0.022f,w,0xffc9cbc1,normal);
        paint.setColor(0x506d7c77);paint.setStrokeWidth(1);canvas.drawLine(x,h*0.66f,right,h*0.66f,paint);
        text(canvas,status,x,h*0.705f,h*0.018f,w,0xffcbb895,normal);
        canvas.restoreToCount(save);
    }
    private void text(Canvas canvas,String text,float x,float baseline,float size,float width,int colour,Typeface face) {
        if(text.isEmpty())return;paint.setTypeface(face);paint.setTextSize(size);
        float measured=paint.measureText(text);if(measured>width)paint.setTextSize(size*width/measured);
        paint.setColor(colour);paint.setStyle(Paint.Style.FILL);canvas.drawText(text,x,baseline,paint);
    }
    private void glyph(Canvas canvas,float x,float y,float size) {
        int save=canvas.save();canvas.translate(x,y);canvas.scale(size/100,size/100);
        paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);
        if(code.isEmpty()||code.equals("unknown")||code.equals("unavailable")) {
            paint.setColor(0xffa3b0ac);canvas.drawLine(20,50,80,50,paint);canvas.restoreToCount(save);return;
        }
        if(code.equals("sunny")||code.equals("partlycloudy")) {
            paint.setColor(0xffe7b76a);paint.setStyle(Paint.Style.FILL);canvas.drawCircle(64,32,18,paint);
            for(int i=0;i<8;i++) { double angle=i*Math.PI/4;canvas.drawLine(64+(float)Math.cos(angle)*25,32+(float)Math.sin(angle)*25,64+(float)Math.cos(angle)*32,32+(float)Math.sin(angle)*32,paint); }
        }
        if(code.equals("clear-night")) {
            paint.setColor(0xffe9dfbd);paint.setStyle(Paint.Style.STROKE);rect.set(20,10,80,70);canvas.drawArc(rect,35,285,false,paint);
        } else if(!code.equals("sunny")) {
            paint.setColor(0xffdce5df);paint.setStyle(Paint.Style.STROKE);
            Path cloud=new Path();cloud.moveTo(24,72);cloud.cubicTo(-2,72,0,39,25,40);
            cloud.cubicTo(25,12,67,10,73,42);cloud.cubicTo(101,36,108,72,80,72);cloud.close();canvas.drawPath(cloud,paint);
            if(code.contains("rain")||code.equals("pouring")) {
                paint.setColor(0xff97b9c2);for(int i=0;i<3;i++)canvas.drawLine(27+i*23,83,21+i*23,96,paint);
            }
            if(code.contains("lightning")) {
                paint.setColor(0xffe7b76a);Path bolt=new Path();bolt.moveTo(60,72);bolt.lineTo(49,87);bolt.lineTo(61,87);bolt.lineTo(51,100);canvas.drawPath(bolt,paint);
            }
        }
        paint.setStyle(Paint.Style.FILL);canvas.restoreToCount(save);
    }
}
