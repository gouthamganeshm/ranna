package in.nxprototype.app;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {
 private static final int SCAN=10, IMAGE=11, PAY=12, SAVE=13;
 private static boolean crashInstalled=false;
 private static final String[][] KNOWN_APPS={
  {"CRED","com.dreamplug.androidapp"},{"BHIM","in.org.npci.upiapp"},
  {"Google Pay","com.google.android.apps.nbu.paisa.user"},{"PhonePe","com.phonepe.app"},
  {"Paytm","net.one97.paytm"},{"Amazon","in.amazon.mShop.android.shopping"}};
 private DebugLog log;
 private android.content.SharedPreferences prefs;
 private PaymentCore.Request request;
 private TextView merchant,status,console,observerStatus;
 private EditText amount;
 private Button pay,queueStart,queueNext;
 private String attempt="none";
 private final Handler handler=new Handler();
 private final Runnable refresh=new Runnable(){public void run(){if(console!=null)console.setText(log.read());if(observerStatus!=null)observerStatus.setText("Service: "+(CredObserverService.connected?"connected":"off")+" · Observation: "+(ObserverSession.armed(android.os.SystemClock.elapsedRealtime())?"armed":"off"));handler.postDelayed(this,1000);}};
 @Override public void onCreate(Bundle saved){super.onCreate(saved);
  if(!UiFlags.ALLOW_SCREEN_RECORDING)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  prefs=getSharedPreferences("session",MODE_PRIVATE);log=new DebugLog(this);
  attempt=prefs.getString("attempt","none");
  if(!crashInstalled){crashInstalled=true;
  final DebugLog crashLog=log;
  Thread.UncaughtExceptionHandler previous=Thread.getDefaultUncaughtExceptionHandler();
  Thread.setDefaultUncaughtExceptionHandler((thread,error)->{
   crashLog.event("UNCAUGHT_ERROR","type="+error.getClass().getSimpleName());
   for(StackTraceElement frame:error.getStackTrace())if(frame.getClassName().startsWith("in.nxprototype.app."))crashLog.event("APP_FRAME",frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber());
   if(previous!=null)previous.uncaughtException(thread,error);
  });
  }
  buildUi();
  if(saved!=null && !locked() && !queueActive()){try{String raw=saved.getString("qr");if(raw!=null)loadQr(raw);amount.setText(saved.getString("amount",""));}catch(Exception ignored){}}
  log.event("APP_OPEN","version="+PaymentCore.VERSION+" api="+Build.VERSION.SDK_INT);
  if(queueActive())log.event("QUEUE_RESTORED","confirmed="+prefs.getInt("queue_done",0)+" count="+prefs.getInt("queue_count",0)+" unresolved="+locked());
  updateStatus();
 }
 private int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int colour){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(colour);t.setPadding(0,dp(6),0,dp(6));return t;}
 private Button button(String label,View.OnClickListener action){Button b=new Button(this);b.setText(label);b.setOnClickListener(action);Brand.quiet(b);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(5));b.setLayoutParams(lp);return b;}
 private void buildUi(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Brand.BG);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(22),dp(18),dp(22),dp(24));scroll.addView(root);
  root.setOnApplyWindowInsetsListener((v,insets)->{root.setPadding(dp(22),dp(18)+insets.getSystemWindowInsetTop(),dp(22),dp(24)+insets.getSystemWindowInsetBottom());return insets;});
  root.addView(text(Brand.NAME.toUpperCase(java.util.Locale.ROOT)+"   /   01",14,Brand.PRIMARY));
  root.addView(text("One QR.\nStep by step.",32,Brand.INK));
  root.addView(text(Brand.SUBTITLE+" · prototype "+PaymentCore.VERSION,14,Brand.MUTED));
  root.addView(Brand.tagline(this,false));
  root.addView(Brand.educational(this,false));
  root.addView(text("Start with a ₹1 test at a merchant who can confirm receipt. Each payment needs approval inside your UPI app.",15,Brand.INK));
  LinearLayout buttons=new LinearLayout(this);
  buttons.addView(button("Scan QR",v->startActivityForResult(new Intent(this,ScanActivity.class),SCAN)),new LinearLayout.LayoutParams(0,dp(54),1));
  buttons.addView(button("Import QR",v->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,IMAGE);}),new LinearLayout.LayoutParams(0,dp(54),1));root.addView(buttons);
  root.addView(button("Paste UPI QR text",v->{EditText input=new EditText(this);input.setHint("upi://pay?pa=...");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);new AlertDialog.Builder(this).setTitle("Paste QR contents").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Read QR",(d,w)->loadQr(input.getText().toString())).show();}));
  merchant=text("Scan a merchant’s UPI QR to begin.",17,Brand.INK);merchant.setTextIsSelectable(true);root.addView(merchant);
  amount=new EditText(this);amount.setHint("Amount in ₹");amount.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);amount.setSaveEnabled(false);root.addView(amount);
  root.addView(text("₹0.01–₹50,000 per payment. Repeat mode uses a basic static QR.",12,Brand.MUTED));
  pay=button("Review payment",v->review());Brand.primary(pay,Brand.PRIMARY);root.addView(pay);
  Button splitBtn=button("Split a TOTAL · you authenticate each part",v->{
   if(locked() || queueActive() || request==null){message("Resolve existing payments and load a basic static QR first.");return;}
   startActivity(new Intent(this,AssistedSplitActivity.class).putExtra("qr",request.original));
  });Brand.primary(splitBtn,Brand.LEAF);root.addView(splitBtn);
  queueStart=button("Set up CRED repeat payments",v->setupQueue());root.addView(queueStart);
  queueNext=button("Review next CRED payment",v->reviewQueue());root.addView(queueNext);
  root.addView(button("Stop repeat queue",v->stopQueue()));
  root.addView(button("Check installed UPI apps",v->appAvailability()));
  status=text("Ready",15,Brand.LEAF);root.addView(status);
  root.addView(button("I checked the payment outcome",v->resolve()));
  root.addView(text("OPTIONAL CRED OBSERVATION",13,Brand.SKY));
  root.addView(text("Records window timing and class names during one CRED payment. No text, screenshots, PIN capture or automatic taps. Protected or in-app transitions may be invisible.",12,Brand.MUTED));
  observerStatus=text("Observer off",13,Brand.SKY);root.addView(observerStatus);
  root.addView(button("Observer settings",v->new AlertDialog.Builder(this).setMessage("Enable Ranna CRED observer in Android Accessibility settings only if you want this diagnostic capture. Then return here and arm one session. If Android or CRED blocks it, stop and report that message.").setNegativeButton("Cancel",null).setPositiveButton("Open settings",(d,w)->{try{startActivity(new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS));}catch(RuntimeException e){message("Accessibility settings unavailable on this device.");}}).show()));
  root.addView(button("Arm one CRED observation",v->{
   if(locked()){message("Resolve the existing payment before arming another observation.");return;}
   if(!CredObserverService.connected){message("Enable Ranna CRED observer in Observer settings first, then return here.");return;}
   new AlertDialog.Builder(this).setMessage("Record CRED window class names and transition times for your next CRED payment? Consent expires in 10 minutes and ends when that payment returns. Logs stay on this device unless you export them.").setNegativeButton("Cancel",null).setPositiveButton("Arm",(d,w)->{ObserverSession.arm(android.os.SystemClock.elapsedRealtime());log.event("OBSERVER_ARMED","ttl_seconds=600");updateStatus();}).show();
  }));
  root.addView(button("Stop observation",v->{int n=ObserverSession.stop();log.event("OBSERVER_STOP","reason=user_stop events="+n);updateStatus();}));
  root.addView(text("LIVE CONSOLE",13,Brand.SKY));
  root.addView(text("Captures app events and optional CRED window metadata. No PINs, screen text, QR contents, bank references or raw payment responses are recorded.",12,Brand.MUTED));
  console=text("",12,Brand.CONSOLE_INK);console.setTypeface(Typeface.MONOSPACE);console.setTextIsSelectable(true);console.setPadding(dp(12),dp(12),dp(12),dp(12));
  GradientDrawable bg=new GradientDrawable();bg.setColor(Brand.CONSOLE_BG);bg.setCornerRadius(dp(14));console.setBackground(bg);
  ScrollView logs=new ScrollView(this);logs.addView(console);root.addView(logs,new LinearLayout.LayoutParams(-1,dp(240)));
  root.addView(button("Share debug log",v->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,Brand.NAME+" "+PaymentCore.VERSION+" debug log");i.putExtra(Intent.EXTRA_TEXT,log.export());startActivity(Intent.createChooser(i,"Share sanitised log"));}));
  root.addView(button("Save debug log (.txt)",v->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.setType("text/plain");i.addCategory(Intent.CATEGORY_OPENABLE);i.putExtra(Intent.EXTRA_TITLE,"nx-lab-"+System.currentTimeMillis()+".txt");startActivityForResult(i,SAVE);}));
  root.addView(button("Clear console",v->new AlertDialog.Builder(this).setMessage("Delete captured debug events? Payment lock stays unchanged.").setNegativeButton("Keep",null).setPositiveButton("Clear",(d,w)->{log.clear();console.setText("");}).show()));
  root.addView(Brand.footer(this,false));
  setContentView(scroll);
 }
 private boolean locked(){return prefs.getBoolean("unresolved",false);}
 private void loadQr(String raw){
  if("nxlab://scanner-test".equals(raw)){log.event("SCANNER_SELF_TEST","decoded_ok");message("Scanner test passed. This QR cannot initiate a payment.");return;}
  if(locked() || queueActive()){message("Resolve the previous payment and finish or stop the repeat queue before loading another QR.");return;}
  try {request=PaymentCore.parseQr(raw);merchant.setText(request.name+"\n"+request.vpa+"\nVerify the bank-resolved recipient in your UPI app.");amount.setText(request.fixedAmount==null?"":request.fixedAmount);amount.setEnabled(request.fixedAmount==null);log.event("QR_ACCEPTED","fixed_amount="+(request.fixedAmount!=null)+" pn="+request.params.containsKey("pn")+" tr="+request.params.containsKey("tr")+" mc="+request.params.containsKey("mc")+" cu="+request.params.containsKey("cu"));updateStatus();}
  catch(IllegalArgumentException e){request=null;merchant.setText("QR not accepted");amount.setText("");amount.setEnabled(true);log.event("QR_REJECTED","validation_failed");message(e.getMessage());updateStatus();}
 }
 private void review(){
  if(locked() || queueActive() || request==null)return;
  try {String value=PaymentCore.money(amount.getText().toString().trim());String uri=request.paymentUri(value);
   new AlertDialog.Builder(this).setTitle("Review ₹"+value).setMessage("Recipient from QR:\n"+request.name+"\n"+request.vpa+"\n\nConfirm the recipient again in your UPI app. This opens one real payment.").setNegativeButton("Cancel",null).setPositiveButton("Choose UPI app",(d,w)->choose(uri)).show();
  }catch(IllegalArgumentException e){message(e.getMessage());}
 }
 private void recordRequestShape(String raw){
  try{PaymentCore.Request q=PaymentCore.parseQr(raw);StringBuilder shape=new StringBuilder("route=package_intent");
   for(String key:new String[]{"pa","pn","am","cu","tr","tid","mc","tn","mode","orgid","sign"}){
    String value=q.params.get(key);shape.append(" ").append(key).append("=").append(value==null?"absent":value.isEmpty()?"empty":"present");
   }log.event("REQUEST_SHAPE",shape.toString());
  }catch(Exception e){log.event("REQUEST_SHAPE","validation_failed");}
 }
 private String readyUri(){
  if(request==null)throw new IllegalArgumentException("Scan a QR and enter its amount first. App availability will be checked against that exact payment link.");
  return request.paymentUri(amount.getText().toString().trim());
 }
 private AppChoices.Candidate candidate(ResolveInfo r,String expected,String label){
  if(r==null || r.activityInfo==null)return null;
  android.content.pm.ActivityInfo a=r.activityInfo;
  boolean match=expected==null || expected.equals(a.packageName);
  boolean allowed=a.permission==null || checkSelfPermission(a.permission)==android.content.pm.PackageManager.PERMISSION_GRANTED;
  if(!match)return null;
  int appState=-1,componentState=-1;
  try{
   appState=getPackageManager().getApplicationEnabledSetting(a.packageName);
   componentState=getPackageManager().getComponentEnabledSetting(new ComponentName(a.packageName,a.name));
  }catch(RuntimeException e){log.event("APP_STATE_ERROR","package="+a.packageName+" type="+e.getClass().getSimpleName());}
  boolean enabled=AppChoices.effectivelyEnabled(true,appState,componentState);
  log.event("APP_HANDLER","package="+a.packageName+" exported="+a.exported+" raw_activity_enabled="+a.enabled+" raw_application_enabled="+(a.applicationInfo!=null && a.applicationInfo.enabled)+" app_state="+appState+" component_state="+componentState+" enabled="+enabled+" permitted="+allowed+" target_match="+match);
  return new AppChoices.Candidate(a.packageName,label==null?r.loadLabel(getPackageManager()).toString():label,a.exported,enabled,allowed);
 }
 private List<AppChoices.Candidate> discover(String uri){
  List<AppChoices.Candidate> targeted=new ArrayList<>(),generic=new ArrayList<>();
  for(String[] app:KNOWN_APPS){
   Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(uri)).setPackage(app[1]);
   try{ResolveInfo r=getPackageManager().resolveActivity(i,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
    log.event("APP_TARGET_PROBE","package="+app[1]+" resolves="+(r!=null));
    targeted.add(candidate(r,app[1],app[0]));
   }catch(RuntimeException e){log.event("APP_TARGET_PROBE","package="+app[1]+" error="+e.getClass().getSimpleName());}
  }
  Intent base=new Intent(Intent.ACTION_VIEW,Uri.parse(uri));
  for(ResolveInfo r:getPackageManager().queryIntentActivities(base,android.content.pm.PackageManager.MATCH_DEFAULT_ONLY))generic.add(candidate(r,null,null));
  List<AppChoices.Candidate> all=AppChoices.merge(targeted,generic,getPackageName());
  log.event("DISCOVERY_MERGE","targeted_probes="+targeted.size()+" generic_matches="+generic.size()+" eligible="+all.size());
  return all;
 }
 private void appAvailability(){
  try{String uri=readyUri();recordRequestShape(uri);List<AppChoices.Candidate> apps=discover(uri);
   StringBuilder report=new StringBuilder("Available for this exact payment link:\n\n");
   for(AppChoices.Candidate a:apps)report.append(a.label).append("\n");
   if(apps.isEmpty())report.append("No accessible UPI handlers found.\n");
   message(report.toString()+"\nThe send list uses this same check. Installed apps without an accessible handler cannot be offered.");
  }catch(IllegalArgumentException e){message(e.getMessage());}
 }
 private void choose(String uri){
  if(locked())return;
  recordRequestShape(uri);
  List<AppChoices.Candidate> apps=discover(uri);
  log.event("APPS_FOUND","count="+apps.size());
  if(apps.isEmpty()){message("No installed UPI app accepts this payment link. Install or enable a UPI app and try again.");return;}
  String[] labels=new String[apps.size()];for(int n=0;n<apps.size();n++){AppChoices.Candidate a=apps.get(n);labels[n]=a.label+"\n"+a.pkg;log.event("APP_OPTION","package="+a.pkg);}
  new AlertDialog.Builder(this).setTitle("Choose a UPI app (no default)").setItems(labels,(d,n)->launch(uri,apps.get(n))).setNegativeButton("Cancel",null).show();
 }
 private void launch(String uri,AppChoices.Candidate target){
  if(locked() || queueActive())return;
  launchConfirmed(uri,target);
 }
 private void launchConfirmed(String uri,AppChoices.Candidate target){
  if(locked())return;
  attempt=UUID.randomUUID().toString().substring(0,8);
  // Commit BEFORE external handoff. A killed process must not enable a duplicate send.
  if(!prefs.edit().putBoolean("unresolved",true).putString("attempt",attempt).putString("status","AWAITING_RESULT").putLong("launch_elapsed",android.os.SystemClock.elapsedRealtime()).commit()){message("Could not save payment state. Payment was not started.");return;}
  if("com.dreamplug.androidapp".equals(target.pkg) && CredObserverService.connected && ObserverSession.begin(attempt,android.os.SystemClock.elapsedRealtime()))log.event("OBSERVER_BEGIN","attempt="+attempt);
  if(queueActive())log.event("QUEUE_PAYMENT_LAUNCH","index="+(prefs.getInt("queue_done",0)+1)+" count="+prefs.getInt("queue_count",0)+" attempt="+attempt);
  log.event("PAYMENT_LAUNCH","attempt="+attempt+" route=package_intent app="+target.pkg);
  updateStatus();
  try{Intent i=new Intent(Intent.ACTION_VIEW,Uri.parse(uri));i.setPackage(target.pkg);startActivityForResult(i,PAY);}
  catch(Exception e){ObserverSession.stop();prefs.edit().putString("status","LAUNCH_ERROR").commit();log.event("LAUNCH_ERROR","attempt="+attempt+" type="+e.getClass().getSimpleName());updateStatus();message("Could not open that UPI app. Check payment history before clearing the attempt.");}
 }
 private void updateStatus(){
  if(observerStatus!=null)observerStatus.setText("Service: "+(CredObserverService.connected?"connected":"off")+" · Observation: "+(ObserverSession.armed(android.os.SystemClock.elapsedRealtime())?"armed (one handoff, expires after 10 min)":"off"));
  pay.setEnabled(request!=null && !locked() && !queueActive());
  queueStart.setEnabled(request!=null && !locked() && !queueActive());
  queueNext.setEnabled(queueActive() && !locked());
  amount.setEnabled(!locked() && !queueActive() && (request==null || request.fixedAmount==null));
  if(!locked()){status.setText(queueActive()?"CRED queue: "+prefs.getInt("queue_done",0)+" / "+prefs.getInt("queue_count",0)+" receipts confirmed. Review the next payment when ready.":"Ready · Each payment requires your approval in the UPI app.");return;}
  String s=prefs.getString("status","UNKNOWN");
  String detail="Payment outcome is unknown. Check your UPI app and merchant before another payment.";
  if("REPORTED_SUCCESS".equals(s))detail="UPI app reported success. Merchant receipt has not been independently verified.";
  if("REPORTED_FAILURE".equals(s))detail="UPI app reported failure. Check payment history before retrying.";
  if("PENDING".equals(s))detail="UPI app reported pending. Do not send a replacement until resolved.";
  if("AWAITING_RESULT".equals(s))detail="Payment opened. If you have returned without a result, check your UPI app and merchant.";
  status.setText((queueActive()?"CRED queue: "+prefs.getInt("queue_done",0)+" / "+prefs.getInt("queue_count",0)+" confirmed\n":"")+"Attempt "+attempt+" · "+s+"\n"+detail);
 }
 private void resolve(){
  if(!locked()){message("There is no unresolved attempt.");return;}
  new AlertDialog.Builder(this).setTitle("Record your check").setMessage("Only clear this attempt after checking your UPI payment history and, for success, the merchant’s receipt. This records your observation; it does not verify settlement.")
   .setPositiveButton("Received",(d,w)->finishCheck("USER_CONFIRMED_RECEIVED"))
   .setNeutralButton("Confirmed not paid",(d,w)->finishCheck("USER_CONFIRMED_NOT_PAID"))
   .setNegativeButton("Still unsure",(d,w)->{log.event("USER_CHECK","attempt="+attempt+" unresolved");}).show();
 }
 private void finishCheck(String observation){
  if(!locked())return;
  ObserverSession.stop();
  boolean queued=queueActive(),received="USER_CONFIRMED_RECEIVED".equals(observation);
  int done=prefs.getInt("queue_done",0),count=prefs.getInt("queue_count",0);
  if(queued && received)done=QueueCore.received(done,count,true);
  android.content.SharedPreferences.Editor edit=prefs.edit().putBoolean("unresolved",false).putString("status",observation);
  boolean keep=queued && received && done<count;
  if(keep)edit.putInt("queue_done",done);
  else edit.remove("queue_uri").remove("queue_count").remove("queue_done");
  if(!edit.commit()){message("Could not save your check. Attempt remains locked.");return;}
  log.event(observation,"attempt="+attempt);
  if(queued)log.event(keep?"QUEUE_READY":received?"QUEUE_COMPLETE":"QUEUE_STOPPED","confirmed="+done+" count="+count+" reason="+(received?"receipt_confirmed":"not_paid"));
  request=null;merchant.setText(keep?"CRED queue retained. Review the next payment below.":"Scan a QR to start a new payment.");amount.setText("");updateStatus();
 }
 private boolean queueActive(){return prefs.contains("queue_uri");}
 private void setupQueue(){
  if(locked() || queueActive() || request==null)return;
  new AlertDialog.Builder(this).setTitle("Number of equal CRED payments").setItems(new String[]{"2 payments","3 payments"},(d,index)->{
   if(locked() || queueActive() || request==null)return;
   try{
    final int count=index+2;QueueCore.validate(request,count);
    final String value=PaymentCore.money(amount.getText().toString().trim()),uri=request.paymentUri(value);
    new AlertDialog.Builder(this).setTitle("Review repeat queue").setMessage(request.name+"\n"+request.vpa+"\n\n"+count+" × ₹"+value+" = ₹"+QueueCore.total(value,count)+" total.\n\nThese are separate real payments to the same recipient. You approve each in CRED and confirm receipt before the next. Payment details are held in private app storage until the queue ends. No PIN is stored.")
     .setNegativeButton("Cancel",null).setPositiveButton("Create queue",(dd,w)->{
      if(locked() || queueActive())return;
      if(!prefs.edit().putString("queue_uri",uri).putInt("queue_count",count).putInt("queue_done",0).commit()){message("Could not save queue. Nothing was sent.");return;}
      log.event("QUEUE_CREATED","count="+count+" app=com.dreamplug.androidapp");updateStatus();reviewQueue();
     }).show();
   }catch(IllegalArgumentException e){message(e.getMessage());}
  }).setNegativeButton("Cancel",null).show();
 }
 private void reviewQueue(){
  if(locked() || !queueActive())return;
  final String uri=prefs.getString("queue_uri","");
  final int done=prefs.getInt("queue_done",0),count=prefs.getInt("queue_count",0);
  try{
   PaymentCore.Request q=PaymentCore.parseQr(uri);
   if(done<0 || done>=count || count<2 || count>3 || q.fixedAmount==null)throw new IllegalArgumentException("Queue state is invalid. Stop this queue.");
   new AlertDialog.Builder(this).setTitle("CRED payment "+(done+1)+" of "+count).setMessage(q.name+"\n"+q.vpa+"\n\nSend ₹"+q.fixedAmount+" now.\nConfirmed receipts: "+done+". Verify the recipient inside CRED.")
    .setNegativeButton("Later",null).setPositiveButton("Open CRED",(d,w)->{
     if(locked() || !uri.equals(prefs.getString("queue_uri",null)) || done!=prefs.getInt("queue_done",-1))return;
     AppChoices.Candidate cred=null;
     for(AppChoices.Candidate app:discover(uri))if("com.dreamplug.androidapp".equals(app.pkg))cred=app;
     if(cred==null){log.event("QUEUE_PAUSED","reason=cred_unavailable");message("CRED is unavailable for this payment. Queue paused; nothing sent.");return;}
     recordRequestShape(uri);launchConfirmed(uri,cred);
    }).show();
  }catch(IllegalArgumentException e){message(e.getMessage());}
 }
 private void stopQueue(){
  if(!queueActive()){message("No active repeat queue.");return;}
  new AlertDialog.Builder(this).setMessage("Discard remaining queued payments? Any launched payment still needs its outcome checked; stopping cannot cancel a payment in CRED.")
   .setNegativeButton("Keep queue",null).setPositiveButton("Stop queue",(d,w)->{
    if(!prefs.edit().remove("queue_uri").remove("queue_count").remove("queue_done").commit()){message("Could not stop queue.");return;}
    log.event("QUEUE_STOPPED","reason=user_stop unresolved="+locked());request=null;merchant.setText("Queue stopped.");amount.setText("");updateStatus();
   }).show();
 }

 @Override protected void onActivityResult(int code,int result,Intent data){super.onActivityResult(code,result,data);
  if(code==PAY){
   if(ObserverSession.armed(android.os.SystemClock.elapsedRealtime())){int n=ObserverSession.stop();log.event("OBSERVER_STOP","reason=payment_return events="+n);}
   String raw=null;try{if(data!=null)raw=data.getStringExtra("response");}catch(RuntimeException ignored){}
   String s=PaymentCore.reportedStatus(raw);
   long started=prefs.getLong("launch_elapsed",0),now=android.os.SystemClock.elapsedRealtime();
   log.event("HANDOFF_RETURN","attempt="+attempt+" elapsed_ms="+(started>0 && now>=started?now-started:-1));
   log.event("CALLBACK_SHAPE","has_intent="+(data!=null)+" has_response="+(raw!=null)+" response_length="+(raw==null?0:raw.length())+" "+PaymentCore.responseShape(raw));
   // An Android RESULT_CANCELED or absent callback is not proof that money was not sent.
   if(locked()){prefs.edit().putString("status",s).commit();log.event("PAYMENT_RETURN","attempt="+attempt+" android_result="+result+" status="+s+" code="+PaymentCore.responseCode(raw));}
   else log.event("STALE_CALLBACK","ignored");
   updateStatus();
  }else if(code==SCAN){if(result==RESULT_OK && data!=null)loadQr(data.getStringExtra("qr"));else{String reason=data==null?"user_cancelled":data.getStringExtra("scan_error");log.event("SCAN_ENDED",Arrays.asList("permission_denied","camera_unavailable").contains(reason)?reason:"user_cancelled");}}
  else if(code==IMAGE && result==RESULT_OK && data!=null){readImage(data.getData());}
  else if(code==SAVE && result==RESULT_OK && data!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(log.export().getBytes("UTF-8"));message("Debug log saved.");}catch(Exception e){message("Could not save log. Try Share debug log.");}}
 }
 private void readImage(Uri uri){
  log.event("QR_IMAGE","decode_started");
  new Thread(()->{String decoded=null;try{
   BitmapFactory.Options opts=new BitmapFactory.Options();opts.inJustDecodeBounds=true;
   try(InputStream in=getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,opts);}
   if(opts.outWidth<=0 || opts.outHeight<=0)throw new IOException();
   opts.inSampleSize=1;while(Math.max(opts.outWidth,opts.outHeight)/opts.inSampleSize>3072)opts.inSampleSize*=2;opts.inJustDecodeBounds=false;log.event("IMAGE_INFO","width="+opts.outWidth+" height="+opts.outHeight+" sample="+opts.inSampleSize);
   Bitmap b;try(InputStream in=getContentResolver().openInputStream(uri)){b=BitmapFactory.decodeStream(in,null,opts);}if(b==null)throw new IOException();
   int w=b.getWidth(),h=b.getHeight();int[] pixels=new int[w*h];b.getPixels(pixels,0,w,0,0,w,h);b.recycle();
   decoded=QrDecoder.decode(QrDecoder.fromArgb(w,h,pixels),true);
   if(decoded==null)log.event("IMAGE_RESULT","no_qr_detected");
  }catch(Exception e){log.event("IMAGE_ERROR","type="+e.getClass().getSimpleName());}
  final String value=decoded;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;if(value!=null)loadQr(value);else{log.event("QR_IMAGE","decode_failed");message("No readable QR found. Try a clear, cropped image or scan with the camera.");}});
  }).start();
 }
 private void message(String s){new AlertDialog.Builder(this).setMessage(s).setPositiveButton("OK",null).show();}
 @Override protected void onSaveInstanceState(Bundle out){super.onSaveInstanceState(out);if(request!=null)out.putString("qr",request.original);out.putString("amount",amount.getText().toString());}
 @Override protected void onResume(){super.onResume();if(log!=null){attempt=prefs.getString("attempt","none");log.event("APP_RESUME","unresolved="+locked());updateStatus();handler.post(refresh);}}
 @Override protected void onPause(){handler.removeCallbacks(refresh);if(log!=null)log.event("APP_PAUSE","unresolved="+locked());super.onPause();}
}
