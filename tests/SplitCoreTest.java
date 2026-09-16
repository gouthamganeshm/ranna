package in.nxprototype.app;
import java.math.BigDecimal;
public final class SplitCoreTest {
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("Split check "+n);}
 static void rejects(Runnable r){n++;try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("Expected rejection "+n);}
 public static void main(String[] args){
  check(java.util.Arrays.equals(SplitCore.parts("2",2),new String[]{"1.00","1.00"}));
  check(java.util.Arrays.equals(SplitCore.parts("10",3),new String[]{"3.34","3.33","3.33"}));
  check(java.util.Arrays.equals(SplitCore.parts("0.03",2),new String[]{"0.02","0.01"}));
  for(int c=2;c<=3;c++)for(String v:new String[]{"0.03","1.01","1999.99","4000.00","49999.99","50000.00"}){
   BigDecimal sum=BigDecimal.ZERO;for(String p:SplitCore.parts(v,c)){sum=sum.add(new BigDecimal(p));check(new BigDecimal(p).signum()>0);check(new BigDecimal(p).compareTo(new BigDecimal("50000"))<=0);}check(sum.compareTo(new BigDecimal(v))==0);
  }
  check(java.util.Arrays.equals(SplitCore.parts("50000",2),new String[]{"25000.00","25000.00"}));
  rejects(()->SplitCore.parts("50000.01",3));rejects(()->SplitCore.parts("99999",2));rejects(()->SplitCore.parts("0.01",2));rejects(()->SplitCore.parts("1.001",2));rejects(()->SplitCore.parts("-2",2));rejects(()->SplitCore.parts("2",4));
  check(SplitCore.canAdvance("REPORTED_SUCCESS",-1));
  for(String s:new String[]{"UNKNOWN","REPORTED_FAILURE","PENDING","USER_CONFIRMED_RECEIVED"})check(!SplitCore.canAdvance(s,-1));
  check(!SplitCore.canAdvance("REPORTED_SUCCESS",0));
  check(SplitCore.amountMatches("₹25000.00","25000.00"));check(SplitCore.amountMatches("₹2.00","2.00"));check(!SplitCore.amountMatches("₹20.00","2.00"));check(!SplitCore.amountMatches("balance ₹2.00","2.00"));
  AssistSession.begin("test","test@bank","Test Payee","1.00",100);check(AssistSession.name.equals("Test Payee"));check(AssistSession.valid(100));check(!AssistSession.valid(99));check(!AssistSession.valid(20100));AssistSession.stop();check(!AssistSession.valid(101));check(AssistSession.vpa==null);check(AssistSession.name==null);
  // Authentication markers: assistance must never act while any of these are visible.
  for(String v:new String[]{"place your thumb on fingerprint scanner","use biometric","enter upi pin","enter pin","passcode"})check(SplitCore.authMarker(v));
  for(String v:new String[]{"pay now","claim your reward","paid successfully to","good news: you have earned a reward",""})check(!SplitCore.authMarker(v));
  // Success markers: only post-payment screens, never the payment screen itself.
  for(String v:new String[]{"paid successfully to","good news: you have earned a reward!","claim your reward"})check(SplitCore.successMarker(v));
  for(String v:new String[]{"pay now","recommended methods","get assured rewards with cred upi","10m+ members pay via cred & earn rewards",""})check(!SplitCore.successMarker(v));
  check(SplitCore.rewardBanner("good news: you have earned a reward!"));check(!SplitCore.rewardBanner("pay now"));
  // Offer controls can never be treated as dismissal targets.
  for(String v:new String[]{"Claim now","claim","Pay now","PAY NOW","Swipe to pay","pay ₹1"})check(SplitCore.offerControl(v));
  for(String v:new String[]{"close","Dismiss","",null})check(!SplitCore.offerControl(v));
  // Dismissal controls are matched on explicit close semantics only.
  check(SplitCore.closeControl("close",null,null));check(SplitCore.closeControl(null,"Close",null));
  check(SplitCore.closeControl("✕",null,null));check(SplitCore.closeControl(null,null,"com.dreamplug.androidapp:id/iv_close"));
  check(!SplitCore.closeControl("Claim now",null,null));check(!SplitCore.closeControl(null,"Pay now",null));
  check(!SplitCore.closeControl("x",null,null));check(!SplitCore.closeControl(null,null,"com.dreamplug.androidapp:id/closest_bank"));
  check(!SplitCore.closeControl(null,null,null));
  // Dismissal phase: armed with the part, bounded in time and count, cooled between actions,
  // and independent of the pre-authentication swipe authority being revoked.
  AssistSession.begin("a1","x@bank","Payee One","1.00",1000);
  check(AssistSession.resultValid(1000));check(AssistSession.cooled(1000));
  AssistSession.stop();                       // swipe authority revoked before dispatch
  check(!AssistSession.valid(1000));          // pre-auth gone
  check(AssistSession.resultValid(1000));     // dismissal survives it
  AssistSession.acted(1000);
  check(!AssistSession.cooled(1500));check(AssistSession.cooled(2400));
  check(!AssistSession.resultValid(999));check(!AssistSession.resultValid(181001));
  for(int i=0;i<3;i++)AssistSession.acted(1000+i);
  check(!AssistSession.resultValid(1000));    // action cap reached
  AssistSession.begin("a2","x@bank","Payee One","1.00",1000);AssistSession.endResult();
  check(!AssistSession.resultValid(1000));check(AssistSession.resultAttempt==null);
  // Indian digit grouping: CRED renders any amount over a thousand with commas. Matching these
  // is what a 5000-in-3 split depends on; before this was handled, every part above 999 paused.
  String[] plan5000=SplitCore.parts("5000",3);
  check(java.util.Arrays.equals(plan5000,new String[]{"1666.67","1666.67","1666.66"}));
  check(SplitCore.amountMatches("₹1,666.67","1666.67"));
  check(SplitCore.amountMatches("₹ 1,666.66","1666.66"));
  check(SplitCore.amountMatches("INR 1,666.67","1666.67"));
  check(SplitCore.amountMatches("₹50,000.00","50000.00"));
  check(SplitCore.amountMatches("₹1,666.67","1666.670"));
  check(!SplitCore.amountMatches("₹1,666.67","1666.66"));
  check(!SplitCore.amountMatches("₹16,666.70","1666.67"));
  check(!SplitCore.amountMatches(null,"1.00"));check(!SplitCore.amountMatches("₹1.00",null));
  // A grouped amount must still read as an amount, or it is never compared at all.
  for(String v:new String[]{"₹1,666.67","₹ 1,666.67","INR 1,666.67","₹50,000","₹1","₹2.00"})check(SplitCore.looksLikeAmount(v));
  for(String v:new String[]{"a/c xxxx 1234","check balance","Pay now","","1666.67",null})check(!SplitCore.looksLikeAmount(v));
  System.out.println("PASS: "+n+" split and navigation-session checks");
 }
}
