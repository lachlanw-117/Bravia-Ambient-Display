import au.wright.ambient.NoticePresentation;
import java.time.Instant;
import java.util.*;

public final class NoticePresentationTest {
    private static int assertions;
    private static void check(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
    private static final Instant NOW=Instant.parse("2026-11-14T06:00:00Z");
    private static Map<String,Object> item(String id,String severity,double first) {
        Map<String,Object> row=new HashMap<>();row.put("occurrenceId",id);row.put("severity",severity);
        row.put("title","Synthetic notice");row.put("category","devices");row.put("kind","fault");
        row.put("active",true);row.put("quality","valid");row.put("firstDetected",first);
        row.put("lastValidObservation",null);row.put("resolvedAt",null);row.put("expiresAt",null);return row;
    }
    private static Map<String,Object> feed(List<Map<String,Object>> rows) {
        Map<String,Object> root=new HashMap<>();root.put("version",1);root.put("fetchedAt",NOW.toString());
        root.put("stale",false);root.put("items",rows);return root;
    }
    private static void rejects(Map<String,Object> root,String why) {
        boolean rejected=false;try{NoticePresentation.parse(root);}catch(RuntimeException error){rejected=true;}check(rejected,why);
    }
    public static void main(String[] args) {
        double at=NOW.getEpochSecond();
        List<Map<String,Object>> rows=Arrays.asList(item("z","info",at-100),item("b","important",at-20),
            item("a","important",at-20),item("c","warning",at-200));
        Map<String,Object> root=feed(rows);
        NoticePresentation.Snapshot snapshot=NoticePresentation.parse(root);
        List<NoticePresentation.Item> sorted=NoticePresentation.visible(snapshot,NOW);
        check(sorted.get(0).id.equals("a")&&sorted.get(1).id.equals("b")&&sorted.get(2).id.equals("c"),"severity then oldest then identity");
        check(NoticePresentation.status(snapshot,NOW.plusMillis(119999)).isEmpty(),"fresh until threshold");
        check(NoticePresentation.status(snapshot,NOW.plusSeconds(120)).equals("Notices may be out of date"),"stale threshold");
        check(!NoticePresentation.status(snapshot,NOW.minusSeconds(1)).isEmpty(),"future timestamp is uncertain");
        check(NoticePresentation.status(null,NOW).equals("Notices unavailable"),"no snapshot");
        Map<String,Object> restored=new HashMap<>(root);
        check(NoticePresentation.visible(NoticePresentation.parse(restored),NOW.plusSeconds(600)).size()==4,"restored stale faults remain");
        check(NoticePresentation.visible(NoticePresentation.parse(feed(Collections.emptyList())),NOW).isEmpty(),"fresh empty removes previous notices");
        Map<String,Object> event=item("event","info",at-10);event.put("kind","event");event.put("active",false);event.put("expiresAt",at+60);
        NoticePresentation.Snapshot events=NoticePresentation.parse(feed(Arrays.asList(event)));
        check(NoticePresentation.visible(events,NOW).size()==1,"resolved one-shot displays until expiry");
        check(NoticePresentation.visible(events,NOW.plusSeconds(60)).isEmpty(),"event expires at deadline offline");
        Map<String,Object> cleared=item("cleared","warning",at);cleared.put("active",false);
        check(NoticePresentation.visible(NoticePresentation.parse(feed(Arrays.asList(cleared))),NOW).isEmpty(),"inactive fault absent");
        check(NoticePresentation.capacity(0,350,100)==3,"maximum three rows");
        check(NoticePresentation.capacity(0,250,100)==2,"fit fewer without shrinking");
        check(NoticePresentation.capacity(350,300,100)==0,"no negative capacity");
        check(NoticePresentation.overflow(5,3)==2&&NoticePresentation.overflow(1,3)==0,"overflow count");
        root=feed(rows);root.put("version",2);rejects(root,"schema version");
        root=feed(rows);root.put("version",1.5);rejects(root,"fractional version");
        root=feed(rows);root.put("fetchedAt","invalid");rejects(root,"invalid fetch date");
        root=feed(rows);root.put("fetchedAt",null);rejects(root,"items need provenance");
        root=feed(rows);root.put("items","invalid");rejects(root,"items must be array");
        rejects(feed(Arrays.asList(rows.get(0),rows.get(0))),"duplicate occurrences");
        for(String field:Arrays.asList("occurrenceId","severity","kind","quality","title","firstDetected","active")) {
            Map<String,Object> bad=item("test","info",at);bad.put(field,"invalid\nvalue");rejects(feed(Arrays.asList(bad)),"reject invalid "+field);
        }
        event.put("expiresAt",null);rejects(feed(Arrays.asList(event)),"event needs deadline");
        root=feed(Collections.emptyList());root.put("fetchedAt",null);root.put("stale",true);
        check(NoticePresentation.status(NoticePresentation.parse(root),NOW).equals("Notices unavailable"),"host has no snapshot");
        root=feed(rows);root.put("stale",true);
        check(!NoticePresentation.status(NoticePresentation.parse(root),NOW).isEmpty(),"host uncertainty preserved");
        System.out.println("Notice presentation: "+assertions+" assertions passed");
    }
}
