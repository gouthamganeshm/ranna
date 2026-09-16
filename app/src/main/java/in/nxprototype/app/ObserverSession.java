package in.nxprototype.app;
/** In-memory, one-handoff consent. Process death drops all observation authority. */
public final class ObserverSession {
 private static long armedAt=-1;
 private static String attempt=null;
 private static int events=0;
 public static synchronized void arm(long now){armedAt=now;attempt=null;events=0;}
 public static synchronized boolean armed(long now){return armedAt>=0 && now>=armedAt && now-armedAt<600000;}
 public static synchronized boolean begin(String id,long now){if(!armed(now))return false;attempt=id;events=0;return true;}
 public static synchronized String attempt(long now){return armed(now)?attempt:null;}
 public static synchronized int next(){return ++events;}
 public static synchronized int stop(){int n=events;armedAt=-1;attempt=null;events=0;return n;}
 public static String safeClass(String value){
  if(value!=null && value.length()<=180 && (value.startsWith("com.dreamplug.") || value.startsWith("android.")) && value.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)+"))return value;
  return "other";
 }
}
