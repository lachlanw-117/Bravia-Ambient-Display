import au.wright.ambient.WeatherPresentation;
import au.wright.ambient.WeatherPresentation.Forecast;
import java.time.*;
import java.util.*;

public final class WeatherPresentationTest {
    private static int assertions;
    private static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    public static void main(String[] args) {
        Instant now=Instant.parse("2026-11-08T02:00:00Z");
        Forecast today=new Forecast("2026-11-08","rainy",19d,11d,now.toString(),false);
        Forecast tomorrow=new Forecast("2026-11-09","partlycloudy",21d,null,now.toString(),false);
        List<Forecast> rows=Arrays.asList(today,tomorrow);
        check(WeatherPresentation.find(rows,LocalDate.parse("2026-11-08"))==today,"today by explicit date");
        check(WeatherPresentation.find(rows,LocalDate.parse("2026-11-10"))==null,"never use wrong day");
        check(WeatherPresentation.find(rows,LocalDate.parse("2026-11-09"))==tomorrow,"promote correct day at midnight");
        check(WeatherPresentation.day(Instant.parse("2026-11-08T13:00:00Z"),"Australia/Sydney").toString().equals("2026-11-09"),"Sydney midnight");
        check(WeatherPresentation.day(Instant.parse("2026-10-03T14:00:00Z"),"Australia/Sydney").plusDays(1).toString().equals("2026-10-05"),"spring DST calendar day");
        check(WeatherPresentation.day(Instant.parse("2026-04-04T14:00:00Z"),"Australia/Sydney").plusDays(1).toString().equals("2026-04-06"),"autumn DST calendar day");
        check(WeatherPresentation.age(null,now)==-1,"unknown age");
        check(WeatherPresentation.age(now.plusSeconds(60).toString(),now)==-1,"future age");
        check(WeatherPresentation.ageLabel(120).equals("Checked 2 min ago"),"check wording");
        check(WeatherPresentation.freshness(now.toString(),false,today,tomorrow,now).equals("Checked just now"),"fresh checks");
        Forecast retained=new Forecast(tomorrow.date,tomorrow.condition,21d,14d,now.minusSeconds(900).toString(),true);
        check(WeatherPresentation.freshness(now.toString(),false,today,retained,now).equals("Forecast cached · checked 15 min ago"),"fresh temperature cannot conceal cached forecast");
        check(WeatherPresentation.freshness(now.toString(),true,today,tomorrow,now).startsWith("Cached weather"),"stale report retained");
        check(WeatherPresentation.condition("unknown").equals("Forecast unavailable"),"unknown is explicit");
        check(WeatherPresentation.condition("lightning-rainy").equals("Thunderstorms"),"bounded long condition");
        check(WeatherPresentation.range(tomorrow).contains("Low —"),"missing low never fabricated");
        check(WeatherPresentation.range(null).isEmpty(),"missing day no range");
        check(WeatherPresentation.freshness("bad",false,today,tomorrow,now).equals("Check time unavailable"),"unknown check time");
        System.out.println("PASS "+assertions+" weather date, provenance and freshness assertions");
    }
}
