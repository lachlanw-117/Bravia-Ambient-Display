import au.wright.ambient.CalendarPresentation;
import au.wright.ambient.NoticePresentation;
import java.time.Instant;
import java.util.*;

public final class CalendarPresentationTest {
    private static int assertions;
    private static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    private static final Instant NOW=Instant.parse("2032-12-30T13:00:00Z");
    private static Map<String,Object> row(int id,String date) {
        Map<String,Object> row=new HashMap<>();row.put("occurrenceId",String.format("%064x",id));
        row.put("title","Example birthday");row.put("kind","birthday");row.put("localDate",date);return row;
    }
    private static Map<String,Object> feed(List<Map<String,Object>> rows) {
        Map<String,Object> root=new HashMap<>();root.put("schemaVersion",1);root.put("timezone","Australia/Sydney");
        root.put("fetchedAt",NOW.toString());root.put("stale",false);root.put("occurrences",rows);return root;
    }
    private static void rejects(Map<String,Object> root) {
        boolean rejected=false;try{CalendarPresentation.parse(root);}catch(RuntimeException e){rejected=true;}
        check(rejected,"invalid feed rejected");
    }
    public static void main(String[] args) {
        Map<String,Object> today=row(1,"2032-12-31"),tomorrow=row(2,"2033-01-01");
        Map<String,Object> root=feed(Arrays.asList(tomorrow,today,row(3,"2032-12-30"),row(4,"2034-02-01")));
        CalendarPresentation.Snapshot snapshot=CalendarPresentation.parse(root);
        List<CalendarPresentation.Item> visible=CalendarPresentation.visible(snapshot,NOW);
        check(visible.size()==2&&visible.get(0).id.endsWith("1"),"date sort and twelve-month range");
        check(CalendarPresentation.countdown(visible.get(0),NOW).equals("Today"),"Sydney Today");
        check(CalendarPresentation.countdown(visible.get(1),NOW).equals("Tomorrow"),"Sydney Tomorrow");
        check(CalendarPresentation.countdown(visible.get(1),NOW.minusSeconds(86400)).equals("in 2 days"),"year countdown");
        check(CalendarPresentation.visible(snapshot,Instant.parse("2032-12-31T13:00:00Z")).size()==1,"midnight offline expiry");
        check(CalendarPresentation.status(snapshot,NOW.plusMillis(86399999)).isEmpty(),"fresh threshold");
        check(CalendarPresentation.status(snapshot,NOW.plusSeconds(86400)).equals("Calendar may be out of date"),"stale 24h");
        check(!CalendarPresentation.status(snapshot,NOW.minusSeconds(1)).isEmpty(),"future fetch stale");
        check(CalendarPresentation.status(null,NOW).equals("Calendar unavailable"),"missing cache");
        root.put("stale",true);check(!CalendarPresentation.status(CalendarPresentation.parse(root),NOW).isEmpty(),"upstream stale");
        root=feed(Collections.emptyList());check(CalendarPresentation.visible(CalendarPresentation.parse(root),NOW).isEmpty(),"successful empty");
        check(CalendarPresentation.status(CalendarPresentation.parse(root),NOW).isEmpty(),"empty is fresh");
        root.put("fetchedAt",null);check(CalendarPresentation.status(CalendarPresentation.parse(root),NOW).equals("Calendar unavailable"),"unavailable restart");
        tomorrow.put("displayAge",33);snapshot=CalendarPresentation.parse(feed(Arrays.asList(tomorrow)));
        check(CalendarPresentation.detail(snapshot.items.get(0),NOW).equals("1 Jan · Tomorrow · turns 33"),"optional display age only");
        Map<String,Object> leap=row(5,"2032-02-29");snapshot=CalendarPresentation.parse(feed(Arrays.asList(leap)));
        check(CalendarPresentation.countdown(snapshot.items.get(0),Instant.parse("2032-02-27T13:00:00Z")).equals("Tomorrow"),"leap day supplied by provider");
        Map<String,Object> dst=row(6,"2033-10-03");snapshot=CalendarPresentation.parse(feed(Arrays.asList(dst)));
        check(CalendarPresentation.countdown(snapshot.items.get(0),Instant.parse("2033-10-01T14:00:00Z")).equals("Tomorrow"),"DST uses civil day");
        for(String field:Arrays.asList("occurrenceId","title","kind","localDate")) {
            Map<String,Object> bad=row(1,"2033-01-01");bad.put(field,"invalid\nvalue");rejects(feed(Arrays.asList(bad)));
        }
        for(Object age:Arrays.asList(-1,131,1.5,"33")) {Map<String,Object> bad=row(1,"2033-01-01");bad.put("displayAge",age);rejects(feed(Arrays.asList(bad)));}
        for(String label:Arrays.asList("Example 2000","person@example.invalid","Born 1/2")) {
            Map<String,Object> bad=row(1,"2033-01-01");bad.put("title",label);rejects(feed(Arrays.asList(bad)));
        }
        rejects(feed(Arrays.asList(today,today)));rejects(feed(Arrays.asList(row(1,"2033-02-29"))));
        root=feed(Arrays.asList(today));root.put("fetchedAt",null);rejects(root);
        root=feed(Arrays.asList(today));root.put("schemaVersion",1.5);rejects(root);
        root=feed(Arrays.asList(today));root.put("timezone","UTC");rejects(root);
        root=feed(Arrays.asList(today));root.put("fetchedAt","bad");rejects(root);
        // Exhaustively preserve original notice capacity and allocate calendar only afterward.
        for(float top:new float[]{580,660,740,840})for(int total=0;total<=8;total++)for(boolean stale:new boolean[]{false,true}) {
            float h=1080,rowHeight=h*.082f,bottom=h*.955f-(stale?h*.052f:0);
            int notices=NoticePresentation.capacity(top,bottom,rowHeight);
            if(total>notices)notices=NoticePresentation.capacity(top,bottom-h*.035f,rowHeight);
            notices=Math.min(notices,total);int overflow=NoticePresentation.overflow(total,notices);
            float remainingTop=top+notices*rowHeight+(overflow>0?h*.045f:0);
            int count=CalendarPresentation.capacity(remainingTop,bottom,h,stale);
            check(count>=0&&count<=3,"bounded calendar rows");
            if(count>0)check(remainingTop+h*.045f+count*h*.08f+(stale?h*.05f:0)<=bottom+.01f,"calendar within remaining space");
        }
        check(CalendarPresentation.capacity(600,1031,1080,false)==3,"three rows with spare space");
        check(CalendarPresentation.capacity(950,1031,1080,false)==0,"hide when no safe row");
        check(!CalendarPresentation.statusFits(1000,1031,1080),"no clipped status");
        System.out.println("Calendar presentation: "+assertions+" assertions passed");
    }
}
