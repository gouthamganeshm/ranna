package in.nxprototype.app;
public final class ObserverSessionTest {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Observer check "+n);}
 public static void main(String[] args){
  ObserverSession.stop();check(!ObserverSession.armed(0));check(!ObserverSession.begin("test",1));
  ObserverSession.arm(100);check(ObserverSession.armed(100));check(ObserverSession.attempt(200)==null);
  check(ObserverSession.begin("test",200));check("test".equals(ObserverSession.attempt(201)));
  check(ObserverSession.next()==1);check(ObserverSession.next()==2);
  check(ObserverSession.attempt(600100)==null);check(!ObserverSession.armed(99));
  check(ObserverSession.stop()==2);check(ObserverSession.attempt(201)==null);
  check(ObserverSession.safeClass("com.dreamplug.androidapp.MainActivity").equals("com.dreamplug.androidapp.MainActivity"));
  check(ObserverSession.safeClass("android.app.Dialog").equals("android.app.Dialog"));
  for(String input:new String[]{null,"123456","person@bank","com.dreamplug.example\nsecret","private.Name","com.dreamplug."})check(ObserverSession.safeClass(input).equals("other"));
  System.out.println("PASS: "+n+" observer checks (consent, handoff, expiry, stop and metadata filtering)");
 }
}
