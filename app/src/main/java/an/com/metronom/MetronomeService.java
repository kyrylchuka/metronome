package an.com.metronom;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

public class MetronomeService extends Service {
    final String LOG_TAG = "...Metronome - Service";
    private static final int NOTIFY_ID = 99;
    final long VIBRATE_TIME = 100;
    final long FLASH_TIME = 100;
    private boolean isVibrate = false;
    private boolean isFlash = false;
    private boolean isSound = true;
    private int soundIdK;
    private int soundIdL;
    private boolean lOrK = true;
    private NotificationManager notificationManager;


    private void log(String message) {
        if (MainActivity.debug){
        Log.d(LOG_TAG, message);}
    }

    private static MetronomeService instance = null;

    public static boolean isInstanceCreated() {
        return instance != null;
    }

    private MetronomeBinder binder = new MetronomeBinder();

    private Timer timer;
    private TimerTask tTask;
    private Vibrator v;
    private Camera mCamera;
    private Camera.Parameters mParams;
    private Intent intent;
    private SoundPool sound;
    private int step = 0;


    @Override
    public void onCreate() {
        instance = this;
        v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        /*check flash*/
        if (checkCameraHardware()) {
            mCamera = getCameraInstance();
//            mParams = mCamera.getParameters();
        checkFlash();

        } else {
            stopFlashMode();
        }

        createSoundPool();
        soundIdK = sound.load(this, R.raw.blip1, 1);
        soundIdL = sound.load(this, R.raw.blip2, 1);

        intent = new Intent(MainActivity.BROADCAST_ACTION);

        log("...onCreate");
        timer = new Timer();

        schedule();

    }


    private long interval(int step) {
        if (step >= MainActivity.MIN_BPM && step <= MainActivity.MAX_BPM) {
            return MainActivity.STEP_BPM * step;
        }
        log("interval = 0");
        return 0;
    }

    private void schedule() {
        if (tTask != null) tTask.cancel();
        if (step >= MainActivity.MIN_BPM && step <= MainActivity.MAX_BPM) {
            tTask = new TimerTask() {
                public void run() {
                    vibrate();
                    flash();
                    sound();

                    intent.putExtra(MainActivity.PARAM_STATUS, MainActivity.STATUS_REUSE);
                    sendBroadcast(intent);

                    log("...run");
                }
            };
            timer.schedule(tTask, 1000, interval(step));
        }
    }

    protected void setVibrate(Boolean v) {
        isVibrate = v;
        schedule();//??????
    }

    protected void setFlash(Boolean f) {
        if (checkCameraHardware()) {
            isFlash = f;
            if (!f){ mCamera.stopPreview();}
        }
        schedule();//??????
    }

    protected void setSound(Boolean s) {
        isSound = s;
        schedule();//??????
    }

    private void vibrate() {
        if (isVibrate) {
            v.vibrate(VIBRATE_TIME);
            log("...vibrate");
        }
    }

    private void flash() {
        if (isFlash) {
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            mCamera.setParameters(mParams);
            mCamera.startPreview();
            try {
                Thread.sleep(FLASH_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            mParams.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(mParams);

            log("...flash");
        }
    }

    private void sound() {
        if (isSound) {
            if (lOrK) {
                sound.play(soundIdK, 1, 1, 0, 0, 1);
                lOrK = false;
            } else {
                sound.play(soundIdL, 1, 1, 0, 0, 1);
                lOrK = true;
            }
            log("...sound");
        }
    }
   protected void toGo(int gap) {
        step = gap;
        schedule();
    }

    protected void toGo(int gap, boolean v, boolean f, boolean s) {
        isVibrate = v;
        if (checkCameraHardware()) {
            isFlash = f;
            if (!f){ mCamera.stopPreview();}
        }
        isSound = s;
        step = gap;
        schedule();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        log("Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }


    class MetronomeBinder extends Binder {
        MetronomeService getService() {
            return MetronomeService.this;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("...onDestroy");
        instance = null;
        timer.cancel();
        exemptCamRes();
        exemptSoundRes();
            if (notificationManager != null) {
                notificationManager.cancel(NOTIFY_ID);
            }
    }

    private void exemptCamRes() {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();}
        mCamera = null;
        log("...camera is exempted");
    }

    private boolean checkCameraHardware() {
        boolean result = false;
        PackageManager pm = getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                && pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            // this device has a camera and flash
            result = true;
        } else {
            stopFlashMode();
        }

        return result;
    }

    private void stopFlashMode() {
        exemptCamRes();
        log("...Error! camera is absent");
        intent = new Intent(MainActivity.BROADCAST_ACTION);
        intent.putExtra(MainActivity.PARAM_BUTTON, MainActivity.BUTTON_FLASH);
        sendBroadcast(intent);
    }

    private Camera getCameraInstance() {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera c = null;

        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    c = Camera.open(i); // attempt to get a Camera instance
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return c; // returns null if camera is unavailable
    }

    private void checkFlash() {
        if (mCamera != null) {
            mParams = mCamera.getParameters();
        } else
            stopFlashMode();
    }


    private void createSoundPool() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            createNewSoundPool();
        } else {
            createOldSoundPool();
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void createNewSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        sound = new SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(2).build();
    }

    @SuppressWarnings("deprecation")
    private void createOldSoundPool() {
        sound = new SoundPool(5, AudioManager.STREAM_MUSIC, 0);
    }

    private void exemptSoundRes() {
        if (sound != null)
            sound.release();
        sound = null;
    }

    protected void sendNotify() {
        Context context = getApplicationContext();
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context,
                0, resultIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        builder.setContentIntent(contentIntent)
                .setSmallIcon(R.drawable.ic_metronome)
                .setContentTitle("Metronome")
                .setContentText("Return into app");

        Notification notification = builder.build();

        notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFY_ID, notification);
    }
}
