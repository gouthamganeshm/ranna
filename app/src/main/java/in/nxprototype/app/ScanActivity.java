package in.nxprototype.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import java.util.*;
import java.util.concurrent.*;

/** Small on-device QR scanner. Camera frames are never saved or logged. */
@SuppressWarnings("deprecation")
public class ScanActivity extends Activity implements SurfaceHolder.Callback {
 private DebugLog log;private TextView hint;private long frames,tries,lastReport;private boolean torch=false;
 private SurfaceView preview;private Camera camera;private boolean surfaceReady,finished=false;
 private volatile boolean busy=false;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 @Override public void onCreate(Bundle b){super.onCreate(b);log=new DebugLog(this);log.event("SCAN_OPEN","camera_requested");if(!UiFlags.ALLOW_SCREEN_RECORDING)getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(16,50,16,30);root.setBackgroundColor(Brand.CONSOLE_BG);
  TextView label=new TextView(this);label.setText("Scan merchant UPI QR\nKeep the whole QR inside the camera view.");label.setTextColor(-1);label.setTextSize(20);label.setPadding(8,20,8,12);root.addView(label);
  root.addView(Brand.tagline(this,true));
  hint=new TextView(this);hint.setText("Waiting for camera…");hint.setTextColor(Brand.CONSOLE_INK);root.addView(hint);
  preview=new SurfaceView(this);root.addView(preview,new LinearLayout.LayoutParams(-1,0,1));preview.getHolder().addCallback(this);
  Button focus=new Button(this);focus.setText("Refocus / tap if blurry");focus.setOnClickListener(v->refocus());Brand.primary(focus,Brand.SUN);root.addView(focus);
  Button flash=new Button(this);flash.setText("Toggle torch");flash.setOnClickListener(v->toggleTorch());Brand.primary(flash,Brand.SKY);root.addView(flash);
  Button cancel=new Button(this);cancel.setText("Cancel scan");cancel.setOnClickListener(v->finish());Brand.primary(cancel,Brand.PRIMARY);root.addView(cancel);
  root.addView(Brand.educational(this,true));setContentView(root);
  root.setOnApplyWindowInsetsListener((v,i)->{root.setPadding(16,i.getSystemWindowInsetTop()+16,16,i.getSystemWindowInsetBottom()+16);return i;});
  if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.CAMERA},1);
 }
 public void surfaceCreated(SurfaceHolder h){surfaceReady=true;open();}
 public void surfaceChanged(SurfaceHolder h,int f,int w,int height){}
 public void surfaceDestroyed(SurfaceHolder h){surfaceReady=false;close();}
 @Override protected void onResume(){super.onResume();open();}
 private void open(){if(camera!=null || !surfaceReady || finished || checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)return;
  try{
   int id=0;Camera.CameraInfo info=new Camera.CameraInfo();for(int n=0;n<Camera.getNumberOfCameras();n++){Camera.getCameraInfo(n,info);if(info.facing==Camera.CameraInfo.CAMERA_FACING_BACK){id=n;break;}}
   Camera.CameraInfo selected=new Camera.CameraInfo();Camera.getCameraInfo(id,selected);camera=Camera.open(id);
   Camera.Parameters p=camera.getParameters();List<Camera.Size> sizes=p.getSupportedPreviewSizes();Camera.Size best=sizes.get(0);
   for(Camera.Size s:sizes)if(Math.abs(s.width-1280)<Math.abs(best.width-1280))best=s;
   p.setPreviewSize(best.width,best.height);p.setPreviewFormat(android.graphics.ImageFormat.NV21);
   if(p.getSupportedFocusModes()!=null && p.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
   try{camera.setParameters(p);}catch(RuntimeException e){log.event("CAMERA_CONFIG","preferred_config_rejected");}
   final Camera.Parameters actual=camera.getParameters();
   final Camera.Size frameSize=actual.getPreviewSize();
   log.event("CAMERA_READY","width="+frameSize.width+" height="+frameSize.height+" format="+actual.getPreviewFormat()+" focus="+actual.getFocusMode());
   camera.setErrorCallback((code,c)->log.event("CAMERA_ERROR","code="+code));
   int rotation=getWindowManager().getDefaultDisplay().getRotation()*90;int orientation=(selected.orientation-rotation+360)%360;
   if(selected.facing==Camera.CameraInfo.CAMERA_FACING_FRONT)orientation=(360-(selected.orientation+rotation)%360)%360;
   camera.setDisplayOrientation(orientation);camera.setPreviewDisplay(preview.getHolder());
   camera.setPreviewCallback((bytes,c)->{frames++;
    long now=SystemClock.elapsedRealtime();if(now-lastReport>5000){lastReport=now;log.event("SCAN_PROGRESS","frames="+frames+" passes="+tries);hint.setText("Camera frames: "+frames+" · scans: "+tries+"\nMove closer, avoid glare, or tap Refocus.");}
    if(busy || finished || worker.isShutdown())return;busy=true;tries++;Camera.Size s=frameSize;byte[] copy=bytes.clone();
    worker.execute(()->{String raw=null;try{PlanarYUVLuminanceSource src=new PlanarYUVLuminanceSource(copy,s.width,s.height,0,0,s.width,s.height,false);raw=QrDecoder.decode(src,false);}catch(Exception ignored){}
     final String found=raw;runOnUiThread(()->{busy=false;if(found!=null && !finished && !isFinishing()){log.event("SCAN_DECODED","frames="+frames+" passes="+tries);finished=true;setResult(RESULT_OK,new Intent().putExtra("qr",found));finish();}});
    });});camera.startPreview();hint.setText("Camera running. Hold the QR steady.");
  }catch(Exception e){log.event("CAMERA_OPEN_ERROR","type="+e.getClass().getSimpleName());close();finished=true;setResult(RESULT_CANCELED,new Intent().putExtra("scan_error","camera_unavailable"));Toast.makeText(this,"Camera unavailable. Try importing a QR image.",Toast.LENGTH_LONG).show();finish();}
 }
 private void refocus(){if(camera==null)return;try{camera.cancelAutoFocus();camera.autoFocus((ok,c)->log.event("CAMERA_FOCUS","success="+ok));}catch(RuntimeException e){log.event("CAMERA_FOCUS","manual_focus_unavailable");}}
 private void toggleTorch(){if(camera==null)return;try{Camera.Parameters p=camera.getParameters();if(p.getSupportedFlashModes()==null || !p.getSupportedFlashModes().contains(Camera.Parameters.FLASH_MODE_TORCH)){hint.setText("Torch unavailable on this camera.");return;}torch=!torch;p.setFlashMode(torch?Camera.Parameters.FLASH_MODE_TORCH:Camera.Parameters.FLASH_MODE_OFF);camera.setParameters(p);}catch(RuntimeException e){log.event("CAMERA_TORCH","unavailable");}}
 private void close(){if(camera!=null){camera.setPreviewCallback(null);try{camera.stopPreview();}catch(Exception ignored){}camera.release();camera=null;}}
 @Override protected void onPause(){close();super.onPause();}
 @Override protected void onDestroy(){log.event("SCAN_CLOSED","frames="+frames+" passes="+tries+" completed="+finished);worker.shutdownNow();super.onDestroy();}
 @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==1){if(g.length>0 && g[0]==PackageManager.PERMISSION_GRANTED)open();else{setResult(RESULT_CANCELED,new Intent().putExtra("scan_error","permission_denied"));finish();}}}
}
