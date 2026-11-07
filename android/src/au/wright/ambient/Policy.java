package au.wright.ambient;

/** Platform-independent invariants shared by the Android components and JVM tests. */
public final class Policy {
    public static final int MAX_PHOTOS = 250;
    public static final long MAX_PHOTO_BYTES = 8L * 1024 * 1024;
    public static final long MAX_CACHE_BYTES = 256L * 1024 * 1024;
    public static boolean validId(String id) { return id != null && id.matches("[a-f0-9]{64}"); }
    public static int slideSeconds(int value) { return Math.max(10, Math.min(300, value)); }
    public static int sampleSize(int width, int height) {
        int sample = 1;
        while (width / sample > 1920 || height / sample > 1080) sample *= 2;
        return sample;
    }
    public static long acceptPhoto(String id, long bytes, java.util.Collection<String> seen, long total) {
        if (!validId(id) || seen.contains(id) || bytes <= 0 || bytes > MAX_PHOTO_BYTES ||
                total < 0 || total + bytes > MAX_CACHE_BYTES) throw new IllegalArgumentException("Invalid photo budget or ID");
        return total + bytes;
    }
    private Policy() {}
}
