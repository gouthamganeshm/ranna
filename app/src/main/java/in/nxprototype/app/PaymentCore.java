package in.nxprototype.app;

import java.net.URI;
import java.net.URLDecoder;
import java.math.BigDecimal;
import java.util.*;

/** Pure logic, independently tested without Android. Never handles a PIN. */
public final class PaymentCore {
    public static final String VERSION = "0.1.12";
    public static final class Request {
        public final String original, vpa, name, fixedAmount;
        public final Map<String,String> params;
        Request(String raw, Map<String,String> p) {
            original=raw; params=Collections.unmodifiableMap(p);
            vpa=p.get("pa"); name=p.containsKey("pn")?p.get("pn"):"Name not supplied";
            fixedAmount=p.get("am");
        }
        public String paymentUri(String amount) {
            String normal = money(amount);
            if (fixedAmount != null && !money(fixedAmount).equals(normal))
                throw new IllegalArgumentException("This QR has a fixed amount. Scan a different QR to change it.");
            String result=original;
            if (fixedAmount == null) result += "&am="+normal;
            if (!params.containsKey("cu")) result += "&cu=INR";
            return result;
        }
    }
    public static Request parseQr(String raw) {
        try {
            if(raw==null || raw.length()>8192) throw new IllegalArgumentException();
            raw=raw.trim();
            URI u=new URI(raw);
            if(!"upi".equalsIgnoreCase(u.getScheme()) || !"pay".equalsIgnoreCase(u.getRawAuthority())
                || u.getRawFragment()!=null || (u.getPath()!=null && !u.getPath().isEmpty())) throw new IllegalArgumentException();
            Map<String,String> p=parseQuery(u.getRawQuery(),true);
            String pa=p.get("pa");
            if(pa==null || !pa.matches("[A-Za-z0-9._-]{1,256}@[A-Za-z0-9._-]{2,64}")) throw new IllegalArgumentException();
            for(String v:p.values()) if(v.length()>2048 || v.matches("(?s).*[\\p{Cntrl}].*")) throw new IllegalArgumentException();
            if(p.containsKey("cu") && !"INR".equals(p.get("cu"))) throw new IllegalArgumentException();
            // Signed QRs require provider-specific validation; do not mutate or claim to support them.
            if(p.containsKey("sign")) throw new IllegalArgumentException("Signed QR is not supported in this prototype. Use a static UPI QR.");
            if(p.containsKey("am")) money(p.get("am"));
            return new Request(raw,p);
        } catch(IllegalArgumentException e) {
            if(e.getMessage()!=null && (e.getMessage().startsWith("Signed") || e.getMessage().startsWith("Amount"))) throw e;
            throw new IllegalArgumentException("Use a valid INR UPI payment QR (upi://pay). Duplicate fields are not accepted.");
        } catch(Exception e) { throw new IllegalArgumentException("This is not a supported UPI payment QR."); }
    }
    public static String money(String raw) {
        if(raw==null || !raw.matches("[0-9]{1,7}(\\.[0-9]{1,2})?")) throw new IllegalArgumentException("Amount must have at most two decimal places.");
        BigDecimal n=new BigDecimal(raw);
        if(n.signum()<=0 || n.compareTo(new BigDecimal("50000"))>0) throw new IllegalArgumentException("Amount must be between ₹0.01 and ₹50,000.");
        return n.setScale(2).toPlainString();
    }
    static Map<String,String> parseQuery(String raw,boolean strict) throws Exception {
        Map<String,String> m=new LinkedHashMap<>();
        if(raw==null) return m;
        for(String part:raw.split("&")) {
            String[] kv=part.split("=",2);
            if(kv.length!=2) { if(strict) throw new IllegalArgumentException(); else continue; }
            String k=URLDecoder.decode(kv[0],"UTF-8").toLowerCase(Locale.ROOT);
            String v=URLDecoder.decode(kv[1],"UTF-8");
            if(m.containsKey(k)) throw new IllegalArgumentException();
            m.put(k,v);
        }
        return m;
    }
    public static String reportedStatus(String response) {
        if(response==null || response.length()>16384) return "UNKNOWN";
        try {
            String s=parseQuery(response,false).get("status");
            if(s==null) return "UNKNOWN";
            s=s.trim().toUpperCase(Locale.ROOT);
            if("SUCCESS".equals(s)) return "REPORTED_SUCCESS";
            if("FAILURE".equals(s) || "FAILED".equals(s)) return "REPORTED_FAILURE";
            if("SUBMITTED".equals(s) || "PENDING".equals(s)) return "PENDING";
        } catch(Exception ignored) {}
        return "UNKNOWN";
    }
    public static String responseShape(String raw) {
        try {Map<String,String> p=parseQuery(raw,false);
            return "fields="+p.size()+" status="+p.containsKey("status")+" responsecode="+p.containsKey("responsecode")+" txnid="+p.containsKey("txnid")+" approvalref="+p.containsKey("approvalrefno");
        }catch(Exception e){return "fields=unparseable";}
    }
    public static String responseCode(String response) {
        try { String c=parseQuery(response,false).get("responsecode");
            return c!=null && c.matches("[A-Za-z0-9]{2}")?c:"not_available";
        } catch(Exception ignored) { return "not_available"; }
    }
}
