package in.nxprototype.app;
import java.math.BigDecimal;
/** Sequential, user-confirmed queue. No callback can advance this counter. */
public final class QueueCore {
 public static void validate(PaymentCore.Request qr,int count){
  if(count<2 || count>3)throw new IllegalArgumentException("Choose 2 or 3 payments for this prototype.");
  // Only a basic static QR: never replay order references or dynamic payment metadata.
  for(String key:qr.params.keySet())
   if(!java.util.Arrays.asList("pa","pn","cu","mc").contains(key))
    throw new IllegalArgumentException("Repeat mode needs a basic static QR without an amount, order reference or other payment metadata.");
 }
 public static String total(String amount,int count){return new BigDecimal(PaymentCore.money(amount)).multiply(BigDecimal.valueOf(count)).setScale(2).toPlainString();}
 public static int received(int done,int count,boolean unresolved){
  if(!unresolved || count<2 || count>3 || done<0 || done>=count)throw new IllegalStateException("No pending queue payment to confirm.");
  return done+1;
 }
}
