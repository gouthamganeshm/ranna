package in.nxprototype.app;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.os.SystemClock;

/** Metadata only. Never requests window content, reads event text or performs actions. */
public final class CredObserverService extends AccessibilityService {
 public static volatile boolean connected=false;
 @Override protected void onServiceConnected(){connected=true;new DebugLog(this).event("OBSERVER_SERVICE","connected=true");}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  long now=SystemClock.elapsedRealtime();String attempt=ObserverSession.attempt(now);
  if(attempt==null || event==null || event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.isPassword())return;
  if(!"com.dreamplug.androidapp".contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
  // Event text/contentDescription/source are deliberately never accessed.
  String clazz=ObserverSession.safeClass(event.getClassName()==null?null:event.getClassName().toString());
  int count=ObserverSession.next();
  if(count>200){ObserverSession.stop();new DebugLog(this).event("OBSERVER_STOP","reason=event_limit");return;}
  new DebugLog(this).event("CRED_WINDOW","attempt="+attempt+" sequence="+count+" class="+clazz);
 }
 @Override public void onInterrupt(){ObserverSession.stop();new DebugLog(this).event("OBSERVER_STOP","reason=interrupted");}
 @Override public void onDestroy(){connected=false;ObserverSession.stop();new DebugLog(this).event("OBSERVER_SERVICE","connected=false");super.onDestroy();}
}
