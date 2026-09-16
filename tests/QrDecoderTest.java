package in.nxprototype.app;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.*;
public class QrDecoderTest {
 static int count;
 static void check(String expected,int w,int h,int[] pixels){String result=QrDecoder.decode(QrDecoder.fromArgb(w,h,pixels),true);if(!Objects.equals(expected,result))throw new AssertionError("Decode mismatch case "+count);count++;}
 public static void main(String[] args)throws Exception{
  String payload="nxlab://scanner-test";
  BitMatrix qr=new QRCodeWriter().encode(payload,BarcodeFormat.QR_CODE,320,320);
  java.awt.image.BufferedImage fixture=new java.awt.image.BufferedImage(320,320,java.awt.image.BufferedImage.TYPE_INT_RGB);
  for(int y=0;y<320;y++)for(int x=0;x<320;x++)fixture.setRGB(x,y,qr.get(x,y)?0xff000000:0xffffffff);
  java.io.File output=new java.io.File("dist/nx-scanner-test.png");output.getParentFile().mkdirs();javax.imageio.ImageIO.write(fixture,"png",output);
  java.awt.image.BufferedImage read=javax.imageio.ImageIO.read(output);check(payload,320,320,read.getRGB(0,0,320,320,null,0,320));
  for(int rotate=0;rotate<4;rotate++)for(boolean invert:new boolean[]{false,true}){
   int[] pix=new int[320*320];for(int y=0;y<320;y++)for(int x=0;x<320;x++){
    int a=x,b=y;for(int r=0;r<rotate;r++){int old=a;a=319-b;b=old;}
    pix[y*320+x]=(qr.get(a,b)^invert)?0xff000000:0xffffffff;
   }check(payload,320,320,pix);
  }
  int[] screen=new int[900*1400];Arrays.fill(screen,0xffe5e5e5);
  for(int y=0;y<320;y++)for(int x=0;x<320;x++)screen[(y+900)*900+x+450]=qr.get(x,y)?0xff303030:0xffdddddd;
  check(payload,900,1400,screen);
  int[] transparent=new int[320*320];for(int y=0;y<320;y++)for(int x=0;x<320;x++)transparent[y*320+x]=qr.get(x,y)?0xff000000:0x00000000;check(payload,320,320,transparent);
  int[] blank=new int[320*320];Arrays.fill(blank,0xffffffff);check(null,320,320,blank);
  System.out.println("PASS: "+count+" QR checks (rotations, inverted colours, off-centre screenshot, blank)");
 }
}
