package au.wright.ambient;

import java.time.Instant;
import java.util.*;

/** Validated immutable feed and display policy, independent of Android. Times are epoch seconds. */
public final class NoticePresentation {
    public static final class Item {
        public final String id,title,category,kind,severity,quality;
        public final boolean active;
        public final double firstDetected;
        public final Double expiresAt;
        private Item(Map<String,Object> row) {
            id=string(row.get("occurrenceId"),256);title=string(row.get("title"),300);
            category=string(row.get("category"),64);kind=choice(row.get("kind"),"fault","event","attention");
            severity=choice(row.get("severity"),"important","warning","info");
            quality=choice(row.get("quality"),"valid","unknown","stale");
            if(!(row.get("active") instanceof Boolean))throw new IllegalArgumentException("Invalid active flag");
            active=(Boolean)row.get("active");firstDetected=timestamp(row.get("firstDetected"));
            optionalTime(row.get("lastValidObservation"));optionalTime(row.get("resolvedAt"));
            expiresAt=kind.equals("event")?timestamp(row.get("expiresAt")):null;
        }
    }
    public static final class Snapshot {
        public final String fetchedAt;
        public final boolean upstreamStale;
        public final List<Item> items;
        private Snapshot(String fetchedAt,boolean stale,List<Item> items) {
            this.fetchedAt=fetchedAt;upstreamStale=stale;this.items=Collections.unmodifiableList(items);
        }
    }
    public static Snapshot parse(Map<String,Object> root) {
        if(!(root.get("version") instanceof Number)||((Number)root.get("version")).doubleValue()!=1 ||
            !(root.get("stale") instanceof Boolean)||!(root.get("items") instanceof List))
            throw new IllegalArgumentException("Unsupported notice feed");
        Object time=root.get("fetchedAt");String fetched=null;
        if(time!=null){fetched=string(time,64);Instant.parse(fetched);}
        List<?> rows=(List<?>)root.get("items");
        if(rows.size()>64||(fetched==null&&!rows.isEmpty()))throw new IllegalArgumentException("Invalid notice feed");
        List<Item> items=new ArrayList<>();Set<String> ids=new HashSet<>();
        for(Object value:rows) {
            if(!(value instanceof Map))throw new IllegalArgumentException("Invalid notice row");
            @SuppressWarnings("unchecked") Map<String,Object> row=(Map<String,Object>)value;
            Item item=new Item(row);
            if(!ids.add(item.id))throw new IllegalArgumentException("Duplicate notice");items.add(item);
        }
        return new Snapshot(fetched,(Boolean)root.get("stale"),items);
    }
    public static List<Item> visible(Snapshot snapshot,Instant now) {
        List<Item> items=new ArrayList<>();
        if(snapshot!=null)for(Item item:snapshot.items)
            if(item.kind.equals("event")?item.expiresAt>now.toEpochMilli()/1000d:item.active)items.add(item);
        items.sort(Comparator.comparingInt((Item item)->rank(item.severity))
            .thenComparingDouble(item->item.firstDetected).thenComparing(item->item.id));
        return items;
    }
    public static String status(Snapshot snapshot,Instant now) {
        if(snapshot==null||snapshot.fetchedAt==null)return "Notices unavailable";
        long age=now.toEpochMilli()-Instant.parse(snapshot.fetchedAt).toEpochMilli();
        return snapshot.upstreamStale||age<0||age>=120000?"Notices may be out of date":"";
    }
    public static int capacity(float top,float bottom,float rowHeight) {
        return Math.max(0,Math.min(3,(int)Math.floor((bottom-top)/rowHeight)));
    }
    public static int overflow(int total,int capacity){return Math.max(0,total-capacity);}
    private static int rank(String value){return value.equals("important")?0:value.equals("warning")?1:2;}
    private static String string(Object value,int max) {
        if(!(value instanceof String))throw new IllegalArgumentException("Invalid notice text");
        String text=((String)value).trim();
        if(text.isEmpty()||text.length()>max||text.matches("(?s).*[\\x00-\\x1f\\x7f].*"))
            throw new IllegalArgumentException("Invalid notice text");
        return text;
    }
    private static String choice(Object value,String... choices) {
        if(!Arrays.asList(choices).contains(value))throw new IllegalArgumentException("Unsupported notice value");
        return (String)value;
    }
    private static double timestamp(Object value) {
        if(!(value instanceof Number))throw new IllegalArgumentException("Invalid notice time");
        double n=((Number)value).doubleValue();
        if(!Double.isFinite(n)||n<=0||n>8640000000000d)throw new IllegalArgumentException("Invalid notice time");
        return n;
    }
    private static void optionalTime(Object value){if(value!=null)timestamp(value);}
}
