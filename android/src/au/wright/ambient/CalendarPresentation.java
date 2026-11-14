package au.wright.ambient;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/** Sydney civil dates, immutable validated data and remaining-space policy. */
public final class CalendarPresentation {
    public static final ZoneId ZONE=ZoneId.of("Australia/Sydney");
    public static final class Item {
        public final String id,title,kind;
        public final LocalDate date;
        public final Integer age;
        private Item(Map<?,?> row) {
            id=text(row.get("occurrenceId"),64);title=text(row.get("title"),160);
            if(!id.matches("[a-f0-9]{64}")||title.matches("(?s).*(@|\\b\\d{4}\\b|\\b\\d{1,2}[/.\\-]\\d{1,2}\\b).*"))throw new IllegalArgumentException();
            kind=text(row.get("kind"),16);
            if(!kind.equals("birthday")&&!kind.equals("occasion"))throw new IllegalArgumentException();
            String day=text(row.get("localDate"),10);date=LocalDate.parse(day);
            if(!date.toString().equals(day))throw new IllegalArgumentException();
            Object value=row.get("displayAge");
            if(value!=null&&(!(value instanceof Number)||((Number)value).doubleValue()!=((Number)value).intValue()||
                ((Number)value).intValue()<0||((Number)value).intValue()>130||!kind.equals("birthday")))throw new IllegalArgumentException();
            age=value==null?null:((Number)value).intValue();
        }
    }
    public static final class Snapshot {
        public final Instant fetched;
        public final boolean stale;
        public final List<Item> items;
        private Snapshot(Instant fetched,boolean stale,List<Item> items) {
            this.fetched=fetched;this.stale=stale;this.items=Collections.unmodifiableList(items);
        }
    }
    public static Snapshot parse(Map<String,Object> root) {
        if(!(root.get("schemaVersion") instanceof Number)||((Number)root.get("schemaVersion")).doubleValue()!=1||
            !"Australia/Sydney".equals(root.get("timezone"))||!(root.get("stale") instanceof Boolean)||
            !(root.get("occurrences") instanceof List))throw new IllegalArgumentException();
        Object stamp=root.get("fetchedAt");Instant fetched=stamp==null?null:Instant.parse(text(stamp,64));
        List<?> rows=(List<?>)root.get("occurrences");
        if(rows.size()>256||(fetched==null&&!rows.isEmpty()))throw new IllegalArgumentException();
        Set<String> ids=new HashSet<>();List<Item> items=new ArrayList<>();
        for(Object row:rows) {
            if(!(row instanceof Map))throw new IllegalArgumentException();
            Item item=new Item((Map<?,?>)row);if(!ids.add(item.id))throw new IllegalArgumentException();items.add(item);
        }
        items.sort(Comparator.comparing((Item item)->item.date).thenComparing(item->item.id));
        return new Snapshot(fetched,(Boolean)root.get("stale"),items);
    }
    public static List<Item> visible(Snapshot snapshot,Instant now) {
        LocalDate today=now.atZone(ZONE).toLocalDate();List<Item> result=new ArrayList<>();
        if(snapshot!=null)for(Item item:snapshot.items)
            if(!item.date.isBefore(today)&&!item.date.isAfter(today.plusYears(1)))result.add(item);
        return result;
    }
    public static String countdown(Item item,Instant now) {
        long days=ChronoUnit.DAYS.between(now.atZone(ZONE).toLocalDate(),item.date);
        return days==0?"Today":days==1?"Tomorrow":"in "+days+" days";
    }
    public static String detail(Item item,Instant now) {
        return item.date.format(DateTimeFormatter.ofPattern("d MMM",Locale.ENGLISH))+" · "+countdown(item,now)+
            (item.age==null?"":" · turns "+item.age);
    }
    public static String status(Snapshot snapshot,Instant now) {
        if(snapshot==null||snapshot.fetched==null)return "Calendar unavailable";
        long age=Duration.between(snapshot.fetched,now).toMillis();
        return snapshot.stale||age<0||age>=86400000?"Calendar may be out of date":"";
    }
    // Caller first allocates the existing notice count, overflow and status unchanged.
    public static int capacity(float top,float bottom,float h,boolean status) {
        return Math.max(0,Math.min(3,(int)Math.floor((bottom-top-h*.045f-(status?h*.05f:0))/(h*.08f))));
    }
    public static boolean statusFits(float top,float bottom,float h) { return bottom-top>=h*.095f; }
    private static String text(Object value,int max) {
        if(!(value instanceof String))throw new IllegalArgumentException();String text=(String)value;
        if(text.trim().isEmpty()||text.length()>max||text.matches("(?s).*[\\x00-\\x1f\\x7f].*"))throw new IllegalArgumentException();
        return text.trim();
    }
}
