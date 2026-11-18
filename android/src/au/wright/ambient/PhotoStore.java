package au.wright.ambient;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Single-writer transactional cache. A failed refresh never commits a partial album. */
public final class PhotoStore {
    private final File root;
    private final Context context;
    private volatile List<String> ids = Collections.emptyList();
    private volatile JSONObject weather = new JSONObject();
    private final Object weatherLock=new Object();
    private final Set<HttpURLConnection> requests=java.util.concurrent.ConcurrentHashMap.newKeySet();
    public PhotoStore(Context context) {
        this.context = context;
        root = new File(context.getFilesDir(), "ambient"); root.mkdirs();
        try {
            JSONObject manifest = new JSONObject(read(new AtomicFile(new File(root,"manifest.json"))));
            List<String> found = new ArrayList<>();
            JSONArray photos = manifest.getJSONArray("photos");
            for (int i=0;i<photos.length();i++) {
                String id = photos.getJSONObject(i).getString("id");
                if (Policy.validId(id) && new File(root,id).isFile()) found.add(id);
            }
            ids = Collections.unmodifiableList(found);
        } catch (Exception ignored) {}
        try { weather = new JSONObject(read(new AtomicFile(new File(root,"weather.json")))); } catch(Exception ignored) {}
    }
    public List<String> photos() { return ids; }
    public JSONObject weather() { return weather; }
    public File file(String id) { if (!Policy.validId(id)) throw new IllegalArgumentException(); return new File(root,id); }
    public static boolean allowedBase(String base) {
        try {
            URI uri = new URI(base);
            if(uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null) return false;
            if("https".equals(uri.getScheme())) return true;
            if(!"http".equals(uri.getScheme())) return false;
            String h=uri.getHost().toLowerCase(Locale.ROOT);
            if(h.equals("localhost") || h.endsWith(".local") || !h.contains(".")) return true;
            if(!h.matches("[0-9.]+")) return false;
            String[] parts=h.split("\\."); if(parts.length!=4)return false;
            int a=Integer.parseInt(parts[0]),b=Integer.parseInt(parts[1]);
            for(String p:parts)if(Integer.parseInt(p)>255)return false;
            return a==10 || a==127 || (a==192 && b==168) || (a==172 && b>=16 && b<=31);
        } catch(Exception e) { return false; }
    }
    private HttpURLConnection connect(String route) throws Exception {
        if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
        android.content.SharedPreferences prefs = context.getSharedPreferences("ambient",Context.MODE_PRIVATE);
        String base=prefs.getString("base","").replaceAll("/+$","");
        if(!allowedBase(base))throw new IOException("Configure a LAN HTTP or HTTPS address");
        HttpURLConnection c=(HttpURLConnection)new URL(base+route).openConnection();
        c.setInstanceFollowRedirects(false); c.setConnectTimeout(5000); c.setReadTimeout(10000);
        c.setRequestProperty("Authorization","Bearer "+prefs.getString("key",""));
        requests.add(c);
        try {
            if(Thread.currentThread().isInterrupted()||c.getResponseCode()!=200)throw new IOException("Feed unavailable");
            return c;
        } catch(Exception error){requests.remove(c);c.disconnect();throw error;}
    }
    byte[] get(String route,int limit) throws Exception {
        HttpURLConnection c=connect(route);
        try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int n,total=0;long deadline=System.nanoTime()+30000000000L;
            while((n=in.read(buffer))!=-1) {
                if(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline)throw new IOException("Download interrupted or timed out");
                total+=n;if(total>limit)throw new IOException("Response too large");out.write(buffer,0,n);
            }
            return out.toByteArray();
        } finally { requests.remove(c);c.disconnect(); }
    }
    public void cancelRequests() { for(HttpURLConnection request:requests)request.disconnect(); }
    public synchronized void sync() throws Exception {
        prune(ids); // Discard remnants of an interrupted previous transaction.
        JSONObject manifest = new JSONObject(new String(get("/manifest",65536),StandardCharsets.UTF_8));
        if(manifest.getInt("version")!=1)throw new IOException("Unsupported feed version");
        JSONArray photos=manifest.getJSONArray("photos");
        if(photos.length()==0 || photos.length()>Policy.MAX_PHOTOS)throw new IOException("Empty or oversized album");
        List<String> next=new ArrayList<>();long total=0;
        for(int i=0;i<photos.length();i++) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            JSONObject photo=photos.getJSONObject(i);String id=photo.getString("id");long size=photo.getLong("bytes");
            total=Policy.acceptPhoto(id,size,next,total);
            File target=new File(root,id);
            boolean intact=target.isFile()&&target.length()==size;
            if(intact) {
                try(InputStream input=new FileInputStream(target)) {
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] block=new byte[8192];int n;
                    while((n=input.read(block))!=-1)digest.update(block,0,n);
                    intact=hex(digest.digest()).equals(id);
                }
            }
            if(!intact) {
                byte[] bytes=get("/photos/"+id,(int)Policy.MAX_PHOTO_BYTES);
                if(bytes.length!=size || !hex(MessageDigest.getInstance("SHA-256").digest(bytes)).equals(id))throw new IOException("Photo checksum mismatch");
                BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;
                BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                if(bounds.outWidth<=0||bounds.outHeight<=0||bounds.outWidth>20000||bounds.outHeight>20000)throw new IOException("Invalid image dimensions");
                bounds.inJustDecodeBounds=false;bounds.inSampleSize=Policy.sampleSize(bounds.outWidth,bounds.outHeight);
                android.graphics.Bitmap check=BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
                if(check==null)throw new IOException("Undecodable image");check.recycle();
                write(new AtomicFile(target),bytes);
            }
            next.add(id);
        }
        write(new AtomicFile(new File(root,"manifest.json")),manifest.toString().getBytes(StandardCharsets.UTF_8));
        ids=Collections.unmodifiableList(next); prune(next);
    }
    public void syncWeather() throws Exception {
        synchronized(weatherLock) {
            byte[] bytes=get("/weather",32768);JSONObject next=new JSONObject(new String(bytes,StandardCharsets.UTF_8));
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();
            write(new AtomicFile(new File(root,"weather.json")),bytes);weather=next;
        }
    }
    private void prune(List<String> keep) {
        File[] files=root.listFiles();if(files==null)return;
        for(File f:files)if(Policy.prunePhotoFile(f.getName(),keep))f.delete();
    }
    private static String read(AtomicFile file)throws Exception {return new String(file.readFully(),StandardCharsets.UTF_8);}
    private static void write(AtomicFile file,byte[] bytes)throws Exception {
        FileOutputStream out=null;
        try {out=file.startWrite();out.write(bytes);file.finishWrite(out);}
        catch(Exception e){if(out!=null)file.failWrite(out);throw e;}
    }
    private static String hex(byte[] bytes) {StringBuilder out=new StringBuilder();for(byte b:bytes)out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
}
