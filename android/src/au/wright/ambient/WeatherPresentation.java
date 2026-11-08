package au.wright.ambient;

import java.time.*;
import java.util.*;

/** Calendar and provenance rules independent of Android for synthetic verification. */
public final class WeatherPresentation {
    public static final class Forecast {
        public final String date,condition,checkedAt;
        public final Double high,low;
        public final boolean cached;
        public Forecast(String date,String condition,Double high,Double low,String checkedAt,boolean cached) {
            this.date=date;this.condition=condition;this.high=high;this.low=low;this.checkedAt=checkedAt;this.cached=cached;
        }
    }
    public static LocalDate day(Instant now,String zone) {
        return now.atZone(ZoneId.of(zone)).toLocalDate();
    }
    public static Forecast find(List<Forecast> rows,LocalDate date) {
        for(Forecast row:rows)if(date.toString().equals(row.date))return row;
        return null;
    }
    public static long age(String stamp,Instant now) {
        try { long seconds=Duration.between(Instant.parse(stamp),now).getSeconds();return seconds<0?-1:seconds; }
        catch(Exception ignored){return -1;}
    }
    public static String ageLabel(long seconds) {
        if(seconds<0)return "Check time unavailable";
        if(seconds<60)return "Checked just now";
        if(seconds<3600)return "Checked "+seconds/60+" min ago";
        if(seconds<86400)return "Checked "+seconds/3600+" hr ago";
        return "Checked "+seconds/86400+" days ago";
    }
    public static String freshness(String currentAt,boolean currentStale,Forecast today,Forecast tomorrow,Instant now) {
        long oldest=age(currentAt,now);
        boolean cached=currentStale,forecastCached=false;
        for(Forecast row:new Forecast[]{today,tomorrow})if(row!=null) {
            long value=age(row.checkedAt,now);
            if(value<0)oldest=-1;else if(oldest>=0)oldest=Math.max(oldest,value);
            forecastCached|=row.cached||value<0||value>21600;
        }
        if(cached)return ageLabel(oldest)+" · Weather check delayed";
        if(forecastCached)return ageLabel(oldest)+" · Forecast check delayed";
        return ageLabel(oldest);
    }
    public static String condition(String code) {
        if(code==null)return "Forecast unavailable";
        switch(code) {
            case "partlycloudy":return "Partly cloudy";
            case "clear-night":return "Clear night";
            case "lightning-rainy":case "lightning":return "Thunderstorms";
            case "snowy-rainy":return "Sleet";
            case "windy-variant":return "Windy and cloudy";
            case "pouring":return "Heavy rain";
            case "exceptional":return "Exceptional weather";
            case "cloudy":return "Cloudy";
            case "fog":return "Fog";
            case "hail":return "Hail";
            case "rainy":return "Rainy";
            case "snowy":return "Snowy";
            case "sunny":return "Sunny";
            case "windy":return "Windy";
            default:return "Forecast unavailable";
        }
    }
    public static String range(Forecast row) {
        if(row==null)return "";
        return "High "+degrees(row.high)+"   Low "+degrees(row.low);
    }
    private static String degrees(Double value) {
        return value==null||!Double.isFinite(value)?"—":String.format(Locale.getDefault(),"%.0f°",value);
    }
    private WeatherPresentation() {}
}
