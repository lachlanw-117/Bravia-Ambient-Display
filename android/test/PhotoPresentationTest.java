import au.wright.ambient.PhotoPresentation;
import au.wright.ambient.Policy;
import java.util.*;

public final class PhotoPresentationTest {
    private static int assertions;
    public static void main(String[] args) {
        int[][] sizes={{1080,1920},{4000,3000},{8000,800},{32,16},{1000,1000}};
        for(int[] screen:new int[][]{{1920,1080},{3840,2160},{1280,720}}) {
            PhotoPresentation.Layout layout=new PhotoPresentation.Layout(screen[0],screen[1]);
            check(layout.width<=1920&&layout.height<=1080,"bounded decoding surface");
            for(int count:new int[]{1,2})for(int i=0;i<count;i++)for(int[] source:sizes) {
                PhotoPresentation.Box slot=layout.slot(i,count),fit=PhotoPresentation.fit(source[0],source[1],slot);
                check(fit.x>=slot.x-0.01&&fit.y>=slot.y-0.01&&fit.x+fit.width<=slot.x+slot.width+0.01
                    &&fit.y+fit.height<=slot.y+slot.height+0.01,"complete image fits its slot");
                check(Math.abs(fit.width/fit.height-(float)source[0]/source[1])<0.001,"source aspect ratio preserved");
                int[] decode=PhotoPresentation.decodeSize(source[0],source[1],slot);
                check(decode[0]<=source[0]&&decode[1]<=source[1],"never upscale during decode");
                check((long)decode[0]*decode[1]*4*count*2<16L*1024*1024,"current and incoming foreground bitmap budget");
            }
            PhotoPresentation.Box a=layout.slot(0,2),b=layout.slot(1,2);
            check(a.x+a.width<b.x,"portrait frames have a gutter");
            check(b.x+b.width<layout.panelLeft,"photos do not overlap the information panel");
        }
        PhotoPresentation.Layout layout=new PhotoPresentation.Layout(1920,1080);
        int[][] album={{600,900},{600,900},{1200,800},{600,900},{600,900},{600,900}};
        List<String> groups=new ArrayList<>();int index=0;
        do {
            int[] first=album[index],next=index+1<album.length?album[index+1]:new int[]{0,0};
            int count=PhotoPresentation.groupSize(true,index,album.length,first[0],first[1],next[0],next[1],layout);
            groups.add(count==2?index+"+"+(index+1):String.valueOf(index));
            index=PhotoPresentation.nextIndex(index,count,album.length);
        } while(index!=0);
        check(groups.equals(Arrays.asList("0+1","2","3+4","5")),"mixed album preserves order and unmatched last portrait");
        check(PhotoPresentation.groupSize(false,0,2,600,900,600,900,layout)==1,"single-photo option");
        check(PhotoPresentation.groupSize(true,0,2,1000,1000,600,900,layout)==1,"square is not paired");
        check(PhotoPresentation.groupSize(true,0,2,600,900,0,0,layout)==1,"missing next image is not paired");
        check(PhotoPresentation.groupSize(true,0,2,600,900,600,900,new PhotoPresentation.Layout(720,1280))==1,"narrow surface avoids tiny pairs");
        int[] small=PhotoPresentation.decodeSize(32,16,layout.slot(0,1));
        check(small[0]==32&&small[1]==16,"small source keeps native resolution");
        boolean rejected=false;
        try{PhotoPresentation.fit(0,1,layout.slot(0,1));}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"bad dimensions rejected");
        String id=String.join("",Collections.nCopies(64,"a"));
        check(!Policy.prunePhotoFile("weather.json.new",Collections.emptyList()),"photo refresh must not delete concurrent weather transaction");
        check(!Policy.prunePhotoFile(id,Collections.singleton(id)),"keep a committed photo");
        check(Policy.prunePhotoFile(id+".new",Collections.emptyList()),"retire abandoned photo transaction");
        check(!Policy.prunePhotoFile("unrelated.new",Collections.emptyList()),"leave unrelated files alone");
        System.out.println("PASS "+assertions+" photo layout, sequence, bitmap budget and cache-isolation assertions");
    }
    private static void check(boolean result,String reason){assertions++;if(!result)throw new AssertionError(reason);}
}
