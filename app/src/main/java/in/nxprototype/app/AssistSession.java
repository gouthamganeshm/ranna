package in.nxprototype.app;
/** Only non-secret routing data. Process death revokes navigation permission. */
public final class AssistSession {
 // Pre-authentication phase: verifies the payment screen, may swipe once, revoked before dispatch.
 static String attempt,vpa,name,amount;
 static long started;
 static boolean active;
 // Post-result phase: dismisses CRED's own reward banner and result screen AFTER a payment.
 // Deliberately separate so that revoking swipe authority never grants, and never withdraws,
 // dismissal authority. It can approve nothing: no swipe, no Pay now, no Claim now.
 static String resultAttempt;
 static long resultArmed=-1,lastAction=-1;
 static int actions;
 static boolean bannerDone;
 static void begin(String id,String payee,String payeeName,String value,long now){
  attempt=id;vpa=payee;name=payeeName;amount=value;started=now;active=true;
  resultAttempt=id;resultArmed=now;lastAction=-1;actions=0;bannerDone=false;
 }
 static void stop(){active=false;attempt=null;vpa=null;name=null;amount=null;}
 static boolean valid(long now){return active && now>=started && now-started<20000;}
 /** Ends dismissal authority: the part resolved, the run stopped, or the service died. */
 static void endResult(){resultAttempt=null;resultArmed=-1;lastAction=-1;actions=0;bannerDone=false;}
 /** Dismissal stays available across the authentication screen, but is bounded in time and count. */
 static boolean resultValid(long now){return resultArmed>=0 && now>=resultArmed && now-resultArmed<180000 && actions<4;}
 static boolean cooled(long now){return lastAction<0 || now-lastAction>=1200;}
 static void acted(long now){lastAction=now;actions++;}
}
