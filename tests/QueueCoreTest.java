package in.nxprototype.app;
public final class QueueCoreTest {
 static int checks=0;
 static void ok(boolean b){checks++;if(!b)throw new AssertionError("Queue check "+checks);}
 static void reject(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException|IllegalStateException e){return;}throw new AssertionError("Expected rejection "+checks);}
 public static void main(String[] args){
  PaymentCore.Request qr=PaymentCore.parseQr("upi://pay?pa=test@bank&pn=Test&cu=INR&mc=1234");
  QueueCore.validate(qr,2);QueueCore.validate(qr,3);
  ok(QueueCore.total("0.01",3).equals("0.03"));ok(QueueCore.total("2000",3).equals("6000.00"));ok(QueueCore.total("16000",3).equals("48000.00"));
  reject(()->QueueCore.validate(qr,1));reject(()->QueueCore.validate(qr,4));
  for(String extra:new String[]{"am=1","tr=order1","tid=tx1","mode=02","tn=order","url=https%3A%2F%2Fexample.com"})
   reject(()->QueueCore.validate(PaymentCore.parseQr("upi://pay?pa=test@bank&"+extra),2));
  // A callback/return without receipt confirmation cannot progress a payment.
  reject(()->QueueCore.received(0,2,false));
  int done=QueueCore.received(0,2,true);ok(done==1);
  reject(()->QueueCore.received(1,2,false));
  done=QueueCore.received(done,2,true);ok(done==2);
  reject(()->QueueCore.received(2,2,true));reject(()->QueueCore.received(-1,2,true));
  reject(()->QueueCore.received(0,4,true));
  System.out.println("PASS: "+checks+" queue checks (decimal totals, metadata rejection, receipt gating, completion)");
 }
}
