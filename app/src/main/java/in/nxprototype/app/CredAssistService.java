package in.nxprototype.app;
import android.accessibilityservice.*;
import android.view.accessibility.*;
import android.os.*;
import android.graphics.*;
import java.util.*;

/** Guarded navigation only: opens CRED's authentication prompt for a verified payment, then
 *  dismisses CRED's own reward banner and result screen afterwards. It never authorises a debit -
 *  the fingerprint or UPI PIN is the approval, it stays with the human, and it is enforced by CRED,
 *  not by this app. Claim now is never pressed. */
public final class CredAssistService extends AccessibilityService {
 public static volatile boolean connected;
 private static final String CRED="com.dreamplug.androidapp";
 private final Handler handler=new Handler();
 private String lastShape="",lastResult="";
 private final Runnable poll=new Runnable(){public void run(){inspect();handler.postDelayed(this,400);}};
 @Override protected void onServiceConnected(){connected=true;new DebugLog(this).event("ASSIST_SERVICE","connected=true");handler.post(poll);}
 @Override public void onAccessibilityEvent(AccessibilityEvent e){}

 private void inspect(){
  long now=SystemClock.elapsedRealtime();
  boolean pre=AssistSession.active,post=AssistSession.resultValid(now);
  if(!pre && !post)return;
  if(pre && !AssistSession.valid(now)){
   AssistSession.stop();pre=false;
   new DebugLog(this).event("ASSIST_PAUSED","reason=control_not_verified_timeout manual_navigation_required=true");
  }
  if(!pre && !post)return;
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return;
  AccessibilityNodeInfo closeNode=null,payNode=null;
  try{
   if(!CRED.contentEquals(root.getPackageName()==null?"":root.getPackageName()))return;
   ArrayDeque<AccessibilityNodeInfo> todo=new ArrayDeque<>();todo.add(AccessibilityNodeInfo.obtain(root));
   boolean vpaMatch=false,nameMatch=false,amount=false,sensitive=false,currencyConflict=false,success=false;
   Rect slider=null,banner=null;int controls=0,payControls=0,visited=0;
   try{
    while(!todo.isEmpty() && visited++<800){AccessibilityNodeInfo n=todo.remove();
     try{
      if(n.isPassword()){sensitive=true;continue;}
      if(!n.isVisibleToUser())continue;
      String t=n.getText()==null?"":n.getText().toString().trim();
      String d=n.getContentDescription()==null?"":n.getContentDescription().toString().trim();
      String lower=(t+" "+d).toLowerCase(Locale.ROOT);
      // A visible authentication marker suspends all assistance before any gesture.
      if(SplitCore.authMarker(lower))sensitive=true;
      if(SplitCore.successMarker(lower))success=true;
      if(t.equalsIgnoreCase(AssistSession.vpa) || d.equalsIgnoreCase(AssistSession.vpa))vpaMatch=true;
      // The intent-flow payment screen shows the bank-resolved name and no address, so the
      // recipient is matched on either signal. Both are compared against this part's own plan.
      if(SplitCore.nameMatches(t,AssistSession.name) || SplitCore.nameMatches(d,AssistSession.name))nameMatch=true;
      for(String value:new String[]{t,d}){
       if(SplitCore.looksLikeAmount(value)){
        if(SplitCore.amountMatches(value,AssistSession.amount))amount=true;else currencyConflict=true;
       }
      }
      if("swipe to pay".equalsIgnoreCase(t) || "swipe to pay".equalsIgnoreCase(d)){
       controls++;if(n.isEnabled())slider=sliderBounds(n);
      }
      if(pre && (SplitCore.payControl(t) || SplitCore.payControl(d))){
       payControls++;if(payNode==null){AccessibilityNodeInfo c=clickable(n);if(c!=null)payNode=c;}
      }
      if(post){
       if(banner==null && SplitCore.rewardBanner(lower)){Rect r=new Rect();n.getBoundsInScreen(r);banner=r;}
       // An offer control is never a dismissal target, whatever else it is labelled.
       if(closeNode==null && !SplitCore.offerControl(t) && !SplitCore.offerControl(d)
          && SplitCore.closeControl(t,d,n.getViewIdResourceName())){
        AccessibilityNodeInfo c=clickable(n);if(c!=null)closeNode=c;
       }
      }
      for(int i=0;i<n.getChildCount();i++){AccessibilityNodeInfo child=n.getChild(i);if(child!=null)todo.add(child);}
     }finally{n.recycle();}
    }
    // A screen we could not finish reading is a screen we have not verified: say so, never act.
    if(!todo.isEmpty()){new DebugLog(this).event("ASSIST_PAUSED","reason=screen_not_fully_read visited="+visited+" manual_navigation_required=true");return;}
   }finally{while(!todo.isEmpty())todo.remove().recycle();}

   if(pre){
    boolean payee=vpaMatch || nameMatch;
    String shape="attempt="+AssistSession.attempt+" payee_vpa_match="+vpaMatch+" payee_name_match="+nameMatch+" amount_match="+amount+" currency_conflict="+currencyConflict+" swipe_controls="+controls+" pay_controls="+payControls+" usable_bounds="+(slider!=null)+" pay_node="+(payNode!=null)+" auth_marker="+sensitive;
    if(!shape.equals(lastShape)){lastShape=shape;new DebugLog(this).event("ASSIST_CHECK",shape);}
    if(sensitive){AssistSession.stop();new DebugLog(this).event("ASSIST_PAUSED","reason=authentication_visible human_action_required=true");return;}
    if(payee && amount && !currencyConflict){
     if(controls==1 && slider!=null && swipe(slider))return;
     // Intent flow: one Pay now control opens the authentication prompt. Authority is revoked first.
     if(payControls==1 && payNode!=null){
      final String id=AssistSession.attempt;AssistSession.stop();
      boolean done=payNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
      new DebugLog(this).event("ASSIST_TAP","attempt="+id+" target=pay_now accepted="+done+" authentication=manual");
      return;
     }
    }
   }

   if(post){
    String shape="attempt="+AssistSession.resultAttempt+" success_marker="+success+" banner="+(banner!=null)+" close_control="+(closeNode!=null)+" auth_marker="+sensitive+" actions="+AssistSession.actions;
    if(!shape.equals(lastResult)){lastResult=shape;new DebugLog(this).event("ASSIST_RESULT",shape);}
    // The human is authenticating, or this is not a post-payment screen. Do nothing either way.
    if(sensitive || !success || !AssistSession.cooled(now))return;
    if(banner!=null && !AssistSession.bannerDone){
     if(dismissBanner(banner)){AssistSession.bannerDone=true;AssistSession.acted(now);
      new DebugLog(this).event("ASSIST_DISMISS","attempt="+AssistSession.resultAttempt+" target=reward_banner action=swipe");}
     return;
    }
    if(closeNode!=null && closeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)){
     AssistSession.acted(now);
     new DebugLog(this).event("ASSIST_DISMISS","attempt="+AssistSession.resultAttempt+" target=result_screen action=close_click");
    }
   }
  }catch(RuntimeException e){
   AssistSession.stop();AssistSession.endResult();
   new DebugLog(this).event("ASSIST_PAUSED","reason=exception type="+e.getClass().getSimpleName());
  }finally{if(closeNode!=null)closeNode.recycle();if(payNode!=null)payNode.recycle();root.recycle();}
 }

 /** One pre-authentication swipe. Authority is revoked before dispatch; there is no retry. */
 private boolean swipe(Rect slider){
  android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
  // Only the actual accessible slider's bounds, never absolute video coordinates.
  if(slider.width()<160*dm.density || slider.height()<24*dm.density || slider.height()>120*dm.density
     || slider.left<0 || slider.top<0 || slider.right>dm.widthPixels || slider.bottom>dm.heightPixels)return false;
  Path path=new Path();path.moveTo(slider.left+slider.height()/2f,slider.centerY());path.lineTo(slider.right-slider.height()/2f,slider.centerY());
  final String id=AssistSession.attempt;AssistSession.stop(); // revoke before dispatch: never inspect or act on the resulting auth screen
  boolean accepted=dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,550)).build(),new GestureResultCallback(){
   @Override public void onCompleted(GestureDescription g){new DebugLog(CredAssistService.this).event("ASSIST_SWIPE","attempt="+id+" completed=true authentication=manual");}
   @Override public void onCancelled(GestureDescription g){new DebugLog(CredAssistService.this).event("ASSIST_PAUSED","reason=gesture_cancelled retry=false");}
  },null);
  new DebugLog(this).event("ASSIST_DISPATCH","attempt="+id+" accepted="+accepted);
  return true;
 }

 /** Flicks CRED's reward banner up and off screen, using the banner's own bounds. */
 private boolean dismissBanner(Rect banner){
  android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
  if(banner.width()<120*dm.density || banner.height()<16*dm.density || banner.height()>240*dm.density
     || banner.left<0 || banner.right>dm.widthPixels || banner.top<0 || banner.bottom>dm.heightPixels)return false;
  float x=banner.exactCenterX(),y=banner.exactCenterY();
  Path path=new Path();path.moveTo(x,y);path.lineTo(x,Math.max(1f,y-banner.height()*2f));
  return dispatchGesture(new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,220)).build(),null,null);
 }

 /** The node itself or its nearest clickable ancestor, within two levels. Caller recycles. */
 private AccessibilityNodeInfo clickable(AccessibilityNodeInfo node){
  AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(node);
  for(int level=0;level<3 && n!=null;level++){
   if(n.isClickable() && n.isEnabled() && n.isVisibleToUser())return n;
   AccessibilityNodeInfo parent=n.getParent();n.recycle();n=parent;
  }
  if(n!=null)n.recycle();
  return null;
 }

 private Rect sliderBounds(AccessibilityNodeInfo label){
  android.util.DisplayMetrics dm=getResources().getDisplayMetrics();
  AccessibilityNodeInfo n=AccessibilityNodeInfo.obtain(label);
  try{
   for(int level=0;level<3 && n!=null;level++){
    Rect r=new Rect();n.getBoundsInScreen(r);
    if(n.isVisibleToUser() && n.isEnabled() && r.width()>=160*dm.density && r.height()>=24*dm.density && r.height()<=120*dm.density && r.left>=0 && r.right<=dm.widthPixels && r.top>=dm.heightPixels/2 && r.bottom<=dm.heightPixels)return r;
    AccessibilityNodeInfo parent=n.getParent();n.recycle();n=parent;
   }
   return null;
  }finally{if(n!=null)n.recycle();}
 }
 @Override public void onInterrupt(){AssistSession.stop();AssistSession.endResult();}
 @Override public void onDestroy(){handler.removeCallbacks(poll);connected=false;AssistSession.stop();AssistSession.endResult();super.onDestroy();}
}
