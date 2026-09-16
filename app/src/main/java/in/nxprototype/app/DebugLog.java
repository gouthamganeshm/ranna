package in.nxprototype.app;
import android.content.Context;
import android.os.Build;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Event allowlist: never accepts QR payloads, callbacks, PINs, names or VPAs. */
final class DebugLog {
 private final File file;
 private static final Object LOCK=new Object();
 DebugLog(Context c){file=new File(c.getFilesDir(),"debug-console.txt");}
 void event(String code,String detail){synchronized(LOCK){
  String line=new SimpleDateFormat("HH:mm:ss.SSS",Locale.ROOT).format(new Date())+" "+code+" "+detail+"\n";
  try {String old=read(); if(old.length()>48000) old=old.substring(old.indexOf('\n',old.length()-36000)+1);
   try(FileOutputStream out=new FileOutputStream(file)){out.write((old+line).getBytes("UTF-8"));}
  }catch(IOException ignored){}
 }}
 String read(){synchronized(LOCK){try{ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(file)){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}return out.toString("UTF-8");}catch(IOException e){return "";}}}
 void clear(){synchronized(LOCK){if(file.exists())file.delete();}}
 String export(){return Brand.NAME.toUpperCase(java.util.Locale.ROOT)+" "+PaymentCore.VERSION+"\nAndroid API: "+Build.VERSION.SDK_INT+"\nDevice: "+Build.MANUFACTURER+" / "+Build.MODEL+"\nEvents only. No raw QR, payee, amount, PIN, bank reference or raw callback.\n\n"+read();}
}
