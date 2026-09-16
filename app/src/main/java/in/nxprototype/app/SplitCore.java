package in.nxprototype.app;
import java.math.BigDecimal;
public final class SplitCore {
 public static String[] parts(String total,int count){
  if(count<2 || count>3 || total==null || !total.matches("[0-9]{1,5}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException("Use a total up to ₹50,000 and 2 or 3 parts.");
  int paise=new BigDecimal(total).movePointRight(2).intValueExact();
  if(paise<count || paise>5000000)throw new IllegalArgumentException("Total must allow at least ₹0.01 per part and be at most ₹50,000.");
  String[] out=new String[count];for(int i=0;i<count;i++)out[i]=PaymentCore.money(BigDecimal.valueOf(paise/count+(i<paise%count?1:0),2).toPlainString());
  return out;
 }
 public static boolean canAdvance(String status,int result){return result==-1 && "REPORTED_SUCCESS".equals(status);}
 /** Compares a visible label against this part's planned amount. CRED renders amounts with Indian
  *  digit grouping once they pass a thousand ("₹1,666.67"), so grouping separators are removed
  *  before the numeric comparison. The comparison itself stays exact. */
 public static boolean amountMatches(String text,String expected){
  if(text==null || expected==null)return false;
  String s=text.trim().replace("₹","").replaceFirst("(?i)^INR\\s*","").trim();
  s=s.replace(",","").replace(" ","").replace(" ","").replace(" ","");
  if(!s.matches("[0-9]{1,7}(\\.[0-9]{1,2})?"))return false;
  try{return new BigDecimal(s).compareTo(new BigDecimal(expected))==0;}catch(Exception e){return false;}
 }
 /** True when a visible label reads as a rupee amount, grouped or not. Used to decide whether a
  *  label is an amount at all; a label that is one but does not match the plan is a conflict. */
 public static boolean looksLikeAmount(String value){
  if(value==null)return false;
  return value.trim().matches("(?i)(₹|INR)\\s*[0-9][0-9,   ]{0,14}(\\.[0-9]{1,2})?");
 }

 /** Any visible sign that CRED is asking the human to authenticate. Assistance never acts then. */
 public static boolean authMarker(String lower){
  if(lower==null)return false;
  return lower.contains("fingerprint")||lower.contains("biometric")||lower.contains("thumb")
   ||lower.contains("upi pin")||lower.contains("enter pin")||lower.contains("passcode");
 }
 /** Only CRED's post-payment screens. Never matches the payment screen, so dismissal cannot close it. */
 public static boolean successMarker(String lower){
  if(lower==null)return false;
  return lower.contains("paid successfully")||lower.contains("earned a reward")||lower.contains("claim your reward");
 }
 /** CRED's transient "good news ..." reward banner. */
 public static boolean rewardBanner(String lower){
  if(lower==null)return false;
  return lower.contains("good news");
 }
 /** Controls that accept an offer or move money. Never actioned automatically. */
 public static boolean offerControl(String label){
  if(label==null)return false;
  String s=label.trim().toLowerCase(java.util.Locale.ROOT);
  return s.equals("claim now")||s.equals("claim")||s.equals("pay now")||s.equals("swipe to pay")||s.startsWith("pay ");
 }
 /** A dismissal control, matched on explicit close semantics only. */
 public static boolean closeControl(String text,String desc,String viewId){
  for(String v:new String[]{text,desc}){
   if(v==null)continue;
   String s=v.trim().toLowerCase(java.util.Locale.ROOT);
   if(s.equals("close")||s.equals("dismiss")||s.equals("✕")||s.equals("×")||s.equals("✖"))return true;
  }
  if(viewId!=null){
   String s=viewId.toLowerCase(java.util.Locale.ROOT);
   if(s.endsWith("close")||s.endsWith("close_button")||s.endsWith("btn_close")||s.endsWith("iv_close"))return true;
  }
  return false;
 }

 /** CRED's intent-flow confirm control. Pressing it only opens authentication; it authorises nothing. */
 public static boolean payControl(String label){
  if(label==null)return false;
  String s=label.trim().toLowerCase(java.util.Locale.ROOT);
  int nl=s.indexOf(10);if(nl>=0)s=s.substring(0,nl).trim();
  return s.equals("pay now");
 }
 /** Recipient name comparison, whitespace and case insensitive. Blank never matches. */
 public static boolean nameMatches(String shown,String expected){
  if(shown==null || expected==null)return false;
  String a=shown.trim().replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);
  String b=expected.trim().replaceAll("\\s+"," ").toLowerCase(java.util.Locale.ROOT);
  return a.length()>=3 && a.equals(b);
 }
}
