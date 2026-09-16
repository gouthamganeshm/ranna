package in.nxprototype.app;
public class PaymentCoreTest {
 static int checks=0;
 static void eq(Object a,Object b){checks++;if(!a.equals(b))throw new AssertionError(a+" != "+b);}
 static void bad(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException expected){return;}throw new AssertionError("Expected rejection");}
 public static void main(String[] args){
  PaymentCore.Request q=PaymentCore.parseQr("upi://pay?pa=test@bank&pn=Test%20Merchant&tr=ORDER123&mc=5411&tn=A%26B");
  eq(q.name,"Test Merchant");eq(q.params.get("tn"),"A&B");
  eq(q.paymentUri("1"),"upi://pay?pa=test@bank&pn=Test%20Merchant&tr=ORDER123&mc=5411&tn=A%26B&am=1.00&cu=INR");
  PaymentCore.Request fixed=PaymentCore.parseQr("upi://pay?pa=t@bank&am=2.50&cu=INR");
  eq(fixed.paymentUri("2.5"),fixed.original);bad(()->fixed.paymentUri("1"));
  for(String raw:new String[]{"https://pay.example/?pa=test@bank","upi://collect?pa=test@bank","upi://pay?pa=a@bank&PA=b@bank","upi://pay?pa=bad","upi://pay?pa=a@bank&am=-1","upi://pay?pa=a@bank&am=1e2","upi://pay?pa=a@bank&cu=USD","upi://pay?pa=a@bank#x","upi://pay?pa=a@bank&pn=%0aInject","upi://pay?pa=a@bank&sign=x","upi://pay?pa=a@bank&am=50000.01"})bad(()->PaymentCore.parseQr(raw));
  eq(PaymentCore.money("50000"),"50000.00");eq(PaymentCore.money("0.01"),"0.01");
  for(String amount:new String[]{"0","-1","1.001","NaN","1e2","50001","2000000"})bad(()->PaymentCore.money(amount));
  eq(PaymentCore.reportedStatus("txnId=x&Status=SUCCESS&responseCode=00"),"REPORTED_SUCCESS");
  eq(PaymentCore.reportedStatus("status=success"),"REPORTED_SUCCESS");
  eq(PaymentCore.reportedStatus("Status=FAILURE&ApprovalRefNo=123456789012"),"REPORTED_FAILURE");
  eq(PaymentCore.reportedStatus("Status=SUBMITTED&ApprovalRefNo=123456789012"),"PENDING");
  eq(PaymentCore.reportedStatus("Status=SUCCESS&status=FAILURE"),"UNKNOWN");
  eq(PaymentCore.reportedStatus(null),"UNKNOWN");eq(PaymentCore.reportedStatus(""),"UNKNOWN");
  eq(PaymentCore.reportedStatus("Status=%zz"),"UNKNOWN");eq(PaymentCore.reportedStatus("Status=FUTURE_NEW_STATUS"),"UNKNOWN");
  eq(PaymentCore.responseCode("Status=FAILED&responseCode=Y1"),"Y1");
  eq(PaymentCore.responseCode("responseCode=private%20message%20with%20PIN"),"not_available");
  System.out.println("PASS: "+checks+" payment parsing, amount, metadata and callback checks");
 }
}
