package au.wright.ambient;

import android.content.Context;
import android.util.AtomicFile;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** One notice writer, separate from album/weather IO. Never stores HA credentials. */
final class NoticeStore {
    private final Context context;
    private final PhotoStore transport;
    private final AtomicFile file;
    private volatile NoticePresentation.Snapshot snapshot;
    private String source;
    NoticeStore(Context context,PhotoStore transport) {
        this.context=context;this.transport=transport;
        file=new AtomicFile(new File(context.getFilesDir(),"ambient/notices.json"));source=base();
        try {
            if(file.getBaseFile().length()>65536)throw new IOException("Oversized notice cache");
            JSONObject saved=new JSONObject(new String(file.readFully(),StandardCharsets.UTF_8));
            if(source.equals(saved.getString("source")))snapshot=parse(saved.getJSONObject("feed"));
        } catch(Exception ignored){}
    }
    private String base(){return context.getSharedPreferences("ambient",Context.MODE_PRIVATE).getString("base","");}
    NoticePresentation.Snapshot snapshot(){return snapshot;}
    synchronized void sync() throws Exception {
        String address=base();if(!address.equals(source)){snapshot=null;source=address;}
        JSONObject feed=new JSONObject(new String(transport.get("/notices",65536),StandardCharsets.UTF_8));
        NoticePresentation.Snapshot next=parse(feed);
        // An unavailable freshly restarted host cannot erase the TV's last-known alerts.
        if(next.fetchedAt==null)return;
        byte[] bytes=new JSONObject().put("source",address).put("feed",feed).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>65536)throw new IOException("Notice cache too large");
        if(Thread.currentThread().isInterrupted()||!address.equals(base()))throw new InterruptedIOException();
        FileOutputStream output=null;
        try {output=file.startWrite();output.write(bytes);file.finishWrite(output);snapshot=next;}
        catch(Exception error){if(output!=null)file.failWrite(output);throw error;}
    }
    static NoticePresentation.Snapshot parse(JSONObject object) throws JSONException {
        Map<String,Object> root=new HashMap<>();
        for(String key:new String[]{"version","fetchedAt","stale"})root.put(key,scalar(object,key));
        JSONArray array=object.getJSONArray("items");
        if(array.length()>64)throw new JSONException("Too many notices");
        List<Map<String,Object>> rows=new ArrayList<>();
        for(int i=0;i<array.length();i++) {
            JSONObject item=array.getJSONObject(i);Map<String,Object> row=new HashMap<>();
            for(String key:new String[]{"occurrenceId","title","category","kind","severity","quality","active",
                "firstDetected","lastValidObservation","resolvedAt","expiresAt"})row.put(key,scalar(item,key));
            rows.add(row);
        }
        root.put("items",rows);return NoticePresentation.parse(root);
    }
    private static Object scalar(JSONObject object,String key)throws JSONException {
        Object value=object.get(key);return value==JSONObject.NULL?null:value;
    }
}
