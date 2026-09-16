package in.nxprototype.app;
import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.graphics.Color;
import android.text.InputType;
import android.widget.*;
import java.util.*;

/** Foreground, consented split sequence. Every authentication remains inside CRED. */
public final class AssistedSplitActivity extends Activity {
 private final Handler handler=new Handler();
 private android.content.SharedPreferences prefs;
 private DebugLog log;
 private PaymentCore.Request qr;
 private EditText total;
 private Spinner count;
 private CheckBox consent;
 private TextView state;
 private Button start,resume,stop,receipts;
 private String[] parts;
 private int index;
 private String current;
 private boolean running,awaiting,foreground,advanceAllowed;
 private final Runnable next=()->{if(running && foreground && advanceAllowed)send();};
 @Override public void onCreate(Bundle saved){super.onCreate(saved);if(!UiFlags.ALLOW_SCREEN_RECORDING)getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
  prefs=getSharedPreferences("session",MODE_PRIVATE);log=new DebugLog(this);
  try{qr=PaymentCore.parseQr(getIntent().getStringExtra("qr"));QueueCore.validate(qr,2);}catch(Exception e){new AlertDialog.Builder(this).setMessage("Use a basic static QR without a fixed amount or order reference.").setPositiveButton("Back",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
  ScrollView scroll=new ScrollView(this);LinearLayout root=new LinearLayout(this);root.setOrientation(1);int pad=(int)(24*getResources().getDisplayMetrics().density);root.setPadding(pad,pad*2,pad,pad*2);scroll.addView(root);scroll.setBackgroundColor(Brand.BG);
  add(root,"Split total through CRED",26).setTextColor(Brand.INK);
  add(root,Brand.NAME+" · prototype "+PaymentCore.VERSION,13).setTextColor(Brand.PRIMARY);
  root.addView(Brand.tagline(this,false));
  root.addView(Brand.educational(this,false));
  add(root,qr.name+"\n"+qr.vpa,17);
  add(root,"2 or 3 parts · Up to ₹50,000 per part. You authenticate every part in CRED. Check the recipient and amount there.",15);
  total=new EditText(this);total.setHint("TOTAL amount in ₹ (not per payment)");total.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);total.setSaveEnabled(false);root.addView(total);
  count=new Spinner(this);count.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"2 parts","3 parts"}));root.addView(count);
  consent=new CheckBox(this);consent.setText("Allow guarded CRED navigation for every part: check the recipient and amount, press Pay now to reach CRED authentication, dismiss CRED reward banners and result screens afterwards, and open the next part automatically once CRED reports success. I authenticate every part myself with my fingerprint or UPI PIN, and no debit can happen without that. Declining or cancelling authentication stops the whole sequence. Claim now is never pressed. Reported success is not independent receipt verification.");root.addView(consent);
  add(root,"Navigation permission lets this service inspect CRED UI locally to match this part’s recipient and currency amount, press Pay now to open authentication, and dismiss CRED reward banners and result screens afterwards. Screen text is never written to logs. It stops the moment any authentication screen is visible, and never presses Claim now. A recipient or amount that does not match the plan pauses it for manual navigation.",14);
  button(root,"Enable CRED navigation service",()->startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  start=button(root,"Review split total",()->review());Brand.primary(start,Brand.PRIMARY);
  resume=button(root,"Continue to next part",()->{if(running && advanceAllowed && !awaiting)send();});Brand.primary(resume,Brand.LEAF);resume.setEnabled(false);
  stop=button(root,"Stop remaining parts",()->stopRun("user_stop"));
  receipts=button(root,"I checked all receipts — close",()->confirmReceipts());receipts.setEnabled(false);
  state=add(root,"Ready. Existing payments must be resolved first.",16);state.setTextColor(Brand.INK);
  add(root,"If authentication is cancelled, CRED fails, or no clear callback arrives, this sequence stops. Stop cannot cancel a payment already launched. After an app restart, check payment history; this sequence never restarts itself.",14);
  root.addView(Brand.footer(this,false));
  setContentView(scroll);
  if(prefs.getBoolean("unresolved",false)){start.setEnabled(false);state.setText("An earlier payment is unresolved. Go back to Ranna and check its outcome first.");}
 }
 private TextView add(LinearLayout root,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(size>=17?Brand.INK:Brand.MUTED);t.setPadding(0,12,0,12);root.addView(t);return t;}
 private Button button(LinearLayout root,String text,Runnable r){Button b=new Button(this);b.setText(text);b.setOnClickListener(v->r.run());Brand.quiet(b);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,Brand.dp(this,5),0,Brand.dp(this,5));b.setLayoutParams(lp);root.addView(b);return b;}
 private void review(){
  if(running || prefs.getBoolean("unresolved",false))return;
  if(!consent.isChecked()){state.setText("Read and select the navigation consent checkbox first.");return;}
  if(!CredAssistService.connected){state.setText("Enable Ranna CRED navigation in Accessibility settings, then return. The observer service alone cannot navigate.");return;}
  try{
   final String[] plan=SplitCore.parts(total.getText().toString().trim(),count.getSelectedItemPosition()+2);
   StringBuilder summary=new StringBuilder(qr.name+"\n"+qr.vpa+"\n\n");java.math.BigDecimal sum=java.math.BigDecimal.ZERO;
   for(int i=0;i<plan.length;i++){summary.append("Part ").append(i+1).append(": ₹").append(plan[i]).append("\n");sum=sum.add(new java.math.BigDecimal(plan[i]));}
   summary.append("TOTAL ₹").append(sum.toPlainString()).append("\n\nEach part opens automatically and lands on CRED authentication. You authenticate every part yourself; declining stops the whole sequence. No retry on failure or uncertainty.");
   new AlertDialog.Builder(this).setTitle("Approve this split plan").setMessage(summary).setNegativeButton("Cancel",null).setPositiveButton("Start split",(d,w)->{
    if(running || prefs.getBoolean("unresolved",false))return;
    parts=plan;index=0;running=true;advanceAllowed=true;start.setEnabled(false);total.setEnabled(false);count.setEnabled(false);consent.setEnabled(false);
    log.event("SPLIT_STARTED","count="+parts.length+" authentication=manual");send();
   }).show();
  }catch(IllegalArgumentException e){state.setText(e.getMessage());}
 }
 private void send(){
  handler.removeCallbacks(next);if(!running || !foreground || awaiting || !advanceAllowed)return;
  if(prefs.getBoolean("unresolved",false) && (current==null || !current.equals(prefs.getString("attempt","")) || !"REPORTED_SUCCESS".equals(prefs.getString("status","")))){stopRun("pending_state_mismatch");return;}
  if(!CredAssistService.connected){stopRun("service_unavailable");return;}
  String uri=qr.paymentUri(parts[index]);Intent intent=new Intent(Intent.ACTION_VIEW,Uri.parse(uri)).setPackage("com.dreamplug.androidapp");
  if(getPackageManager().resolveActivity(intent,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)==null){stopRun("cred_unavailable");return;}
  current=UUID.randomUUID().toString().substring(0,8);
  if(!prefs.edit().putBoolean("unresolved",true).putString("attempt",current).putString("status","AWAITING_RESULT").putLong("launch_elapsed",SystemClock.elapsedRealtime()).commit()){stopRun("storage_failure");return;}
  awaiting=true;advanceAllowed=false;resume.setEnabled(false);
  AssistSession.begin(current,qr.vpa,qr.name,parts[index],SystemClock.elapsedRealtime());
  log.event("SPLIT_LAUNCH","attempt="+current+" index="+(index+1)+" count="+parts.length);
  state.setText("Part "+(index+1)+" / "+parts.length+" opened. Authenticate in CRED with your fingerprint or UPI PIN. If navigation pauses, complete the visible steps yourself.");
  try{startActivityForResult(intent,71);}catch(RuntimeException e){awaiting=false;prefs.edit().putString("status","LAUNCH_ERROR").commit();stopRun("launch_error");}
 }
 @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);if(code!=71)return;
  AssistSession.stop();AssistSession.endResult();
  if(!awaiting || current==null || !current.equals(prefs.getString("attempt",""))){log.event("SPLIT_STALE_CALLBACK","ignored");return;}
  awaiting=false;String raw=null;try{if(data!=null)raw=data.getStringExtra("response");}catch(RuntimeException ignored){}
  String reported=PaymentCore.reportedStatus(raw);
  if(!prefs.edit().putString("status",reported).commit()){stopRun("storage_failure");return;}
  log.event("SPLIT_RETURN","attempt="+current+" index="+(index+1)+" status="+reported+" android_result="+result);
  log.event("CALLBACK_SHAPE",PaymentCore.responseShape(raw));
  if(!running)return;
  if(!SplitCore.canAdvance(reported,result)){stopRun("result_requires_check");return;}
  index++;
  if(index==parts.length){running=false;advanceAllowed=false;state.setText("CRED reported success for all "+parts.length+" parts. Check every receipt before closing. Settlement is not independently verified.");receipts.setEnabled(true);log.event("SPLIT_REPORTED_COMPLETE","count="+parts.length+" receipt_verified=false");return;}
  advanceAllowed=true;resume.setEnabled(true);state.setText("CRED reported success for part "+index+". Next opens in 4 seconds while this screen stays visible. Tap Stop remaining parts to stop.");handler.postDelayed(next,4000);
 }
 private void stopRun(String reason){handler.removeCallbacks(next);AssistSession.stop();AssistSession.endResult();running=false;advanceAllowed=false;if(resume!=null)resume.setEnabled(false);if(state!=null)state.setText("Split stopped. Check completed parts and any pending payment in CRED. Return to Ranna to record the pending outcome. Nothing is retried automatically.");log.event("SPLIT_STOPPED","reason="+reason+" reported_completed="+index+" unresolved="+prefs.getBoolean("unresolved",false));}
 private void confirmReceipts(){new AlertDialog.Builder(this).setMessage("Have you checked receipt of ALL parts? This records your observation, not independent bank verification.").setNegativeButton("Not yet",null).setPositiveButton("All received",(d,w)->{
  if(running || awaiting || parts==null || index!=parts.length || !current.equals(prefs.getString("attempt","")))return;
  if(!prefs.edit().putBoolean("unresolved",false).putString("status","USER_CONFIRMED_RECEIVED").commit())return;
  log.event("SPLIT_RECEIPTS_CONFIRMED","count="+parts.length);finish();
 }).show();}
 @Override protected void onResume(){super.onResume();foreground=true;
  // onActivityResult always precedes onResume, so a still-awaiting part means CRED returned
  // no result: the human declined, backed out, or the screen was dismissed. Never continue.
  if(running && awaiting){awaiting=false;AssistSession.stop();AssistSession.endResult();
   prefs.edit().putString("status","NO_CALLBACK").commit();
   log.event("SPLIT_RETURN","attempt="+current+" index="+(index+1)+" status=NO_CALLBACK android_result=none");
   stopRun("returned_without_result");}
 }
 @Override protected void onPause(){foreground=false;handler.removeCallbacks(next);super.onPause();}
 @Override protected void onDestroy(){handler.removeCallbacks(next);AssistSession.stop();AssistSession.endResult();super.onDestroy();}
 @Override public void onBackPressed(){if(running || awaiting){new AlertDialog.Builder(this).setMessage("Stop this split sequence and return? Any launched payment remains unresolved.").setNegativeButton("Stay",null).setPositiveButton("Stop and return",(d,w)->{stopRun("leave_screen");finish();}).show();}else super.onBackPressed();}
}
