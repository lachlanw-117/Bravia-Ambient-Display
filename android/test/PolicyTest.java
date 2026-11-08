import au.wright.ambient.Policy;

public final class PolicyTest {
    public static void main(String[] args) {
        check(Policy.slideSeconds(30)==30,"30 second default");
        check(Policy.slideSeconds(0)==10,"lower bound");check(Policy.slideSeconds(999)==300,"upper bound");
        check(Policy.validId(new String(new char[64]).replace('\0','a')),"content ID");
        check(!Policy.validId("../../private"),"traversal");check(!Policy.validId(null),"missing ID");
        check(Policy.sampleSize(8000,6000)==8,"bounded portrait bitmap");
        check(Policy.sampleSize(3840,2160)==2,"4k decode budget");
        check(Policy.sampleSize(1920,1080)==1,"native 1080p");
        String id=new String(new char[64]).replace('\0','a');
        check(Policy.acceptPhoto(id,1024,java.util.Collections.emptyList(),0)==1024,"accepted byte count");
        reject(()->Policy.acceptPhoto(id,0,java.util.Collections.emptyList(),0));
        reject(()->Policy.acceptPhoto(id,Policy.MAX_PHOTO_BYTES+1,java.util.Collections.emptyList(),0));
        reject(()->Policy.acceptPhoto(id,1024,java.util.Collections.singletonList(id),0));
        reject(()->Policy.acceptPhoto(id,1024,java.util.Collections.emptyList(),Policy.MAX_CACHE_BYTES));
        reject(()->Policy.acceptPhoto("../private",1024,java.util.Collections.emptyList(),0));
        System.out.println("PASS 15 JVM cache and display policy assertions");
    }
    private static void check(boolean result,String message){if(!result)throw new AssertionError(message);}
    private static void reject(Runnable operation){try{operation.run();throw new AssertionError("Must reject invalid feed");}catch(IllegalArgumentException expected){}}
}
