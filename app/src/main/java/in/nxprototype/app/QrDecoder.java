package in.nxprototype.app;
import com.google.zxing.*;
import com.google.zxing.common.*;
import java.util.*;

/** Bounded, on-device passes. No payload or image enters diagnostic logs. */
public final class QrDecoder {
 public static LuminanceSource fromArgb(int w,int h,int[] pixels) {
  for(int i=0;i<pixels.length;i++){int p=pixels[i],a=(p>>>24)&255;
   if(a!=255){int r=((p>>>16)&255)*a/255+255-a,g=((p>>>8)&255)*a/255+255-a,b=(p&255)*a/255+255-a;pixels[i]=0xff000000|(r<<16)|(g<<8)|b;}
  }
  return new RGBLuminanceSource(w,h,pixels);
 }
 public static String decode(LuminanceSource source, boolean image) {
  String result=passes(source);if(result!=null)return result;
  if(source.isCropSupported()) {
   int w=source.getWidth(),h=source.getHeight(),side=Math.min(w,h);
   // Whole-screen imports often contain a small QR amid unrelated interface text.
   int[] lefts={0,(w-side)/2,w-side}, tops={0,(h-side)/2,h-side};
   Set<String> visited=new HashSet<>();
   for(int x:lefts)for(int y:tops){if(!visited.add(x+":"+y))continue;result=passes(source.crop(x,y,side,side));if(result!=null)return result;}
   if(image && w>200 && h>200){int cw=w*2/3,ch=h*2/3;
    for(int x:new int[]{0,w-cw})for(int y:new int[]{0,h-ch}){result=passes(source.crop(x,y,cw,ch));if(result!=null)return result;}
   }
  }
  return null;
 }
 private static String passes(LuminanceSource src) {
  Map<DecodeHintType,Object> hints=new EnumMap<>(DecodeHintType.class);
  hints.put(DecodeHintType.POSSIBLE_FORMATS,Collections.singletonList(BarcodeFormat.QR_CODE));
  hints.put(DecodeHintType.TRY_HARDER,Boolean.TRUE);
  for(int inverted=0;inverted<2;inverted++){
   LuminanceSource current=inverted==0?src:src.invert();
   for(int mode=0;mode<2;mode++)try{
    BinaryBitmap bitmap=new BinaryBitmap(mode==0?new HybridBinarizer(current):new GlobalHistogramBinarizer(current));
    return new MultiFormatReader().decode(bitmap,hints).getText();
   }catch(ReaderException ignored){}
  }
  return null;
 }
}
