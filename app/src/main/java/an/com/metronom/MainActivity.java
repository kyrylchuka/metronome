package an.com.metronom;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {
    static boolean debug = false;

    private final String SAVED_BMP = "saved_bmp";
    private static final String SAVED_MODE_VIBRATION = "saved_vibration";
    private static final String SAVED_MODE_FLASH = "saved_flash";
    private static final String SAVED_MODE_SOUND = "saved_sound";

    final String LOG_TAG = "...Metronome - Main";
    final static int MIN_BPM = 1;
    final static int MAX_BPM = 200;
    final static int STEP_BPM = 250;
    final static int START_BPM = 1;
    /*BroadcastReceiver*/
    final static int STATUS_REUSE = 100;
    final static int BUTTON_FLASH = 101;
    final static String PARAM_STATUS = "status";
    final static String PARAM_BUTTON = "button_flash";
    final static String BROADCAST_ACTION = "com.an.metronome";



    private void log(String message) {
        if(debug){
        Log.d(LOG_TAG, message);}
        //    Log.d(LOG_TAG, this.getClass().getSimpleName() + " . " + Thread.currentThread().getStackTrace()[1].getMethodName());
    }
//    public void setDebug(boolean d){
//        debug = d;
//    }

    private SharedPreferences sPref;


    private boolean bound = false;
    private boolean ifStart = true;
    private boolean ifVibration = false;
    private boolean ifFlash = false;
    private boolean ifSound = true;
    private ServiceConnection sConn;
    private Intent intent;
    private MetronomeService mService;
    private BroadcastReceiver br;
    private EditText etStep;
    private SeekBar mSeekBar;
    private Button start;
    private Button vibration;
    private Button flash;
    private Button sound;
    private ImageView indicator;


    private int step;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intent = new Intent(this, MetronomeService.class);


        indicator = (ImageView) findViewById(R.id.imageViewIndicator);

        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                int status = intent.getIntExtra(PARAM_STATUS, 0);

//                log("...broadcastReceiver = " + PARAM_STATUS + "." + status + " = " + STATUS_REUSE);

                if (status == STATUS_REUSE) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        indicator.setImageDrawable(getDrawable(R.drawable.indicator0));
                                    } else {

                                        indicator.setImageDrawable(getResources().getDrawable(R.drawable.indicator0));
//                                        log("...green");
                                    }

                                }

                            });
                            try {
                                Thread.sleep(STEP_BPM / 2);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        indicator.setImageDrawable(getDrawable(R.drawable.indicator0b));
                                    } else {

                                        indicator.setImageDrawable(getResources().getDrawable(R.drawable.indicator0b));
//                                        log("...black");
                                    }

                                }
                            });

                        }
                    }).start();
                }
                int buttonF = intent.getIntExtra(PARAM_BUTTON, 0);
                if (buttonF == BUTTON_FLASH) {
                    flashMode(false);
                    flash.setClickable(false);
                }
            }
        };
        IntentFilter iFilter = new IntentFilter(BROADCAST_ACTION);
        registerReceiver(br, iFilter);

        sConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder binder) {
                log("...onServiceConnected");
                mService = ((MetronomeService.MetronomeBinder) binder).getService();
                bound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                log("...onServiceDisconnected");
                bound = false;
            }
        };

        start = (Button) findViewById(R.id.buttonStart);
        vibration = (Button) findViewById(R.id.buttonVibration);
        flash = (Button) findViewById(R.id.buttonFlash);
        sound = (Button) findViewById(R.id.buttonSound);


        mSeekBar = (SeekBar) findViewById(R.id.seekBarBmp);
        etStep = (EditText) findViewById(R.id.editTextBmp);

        loadMode();
        vibrationMode(ifVibration);
        flashMode(ifFlash);
        soundMode(ifSound);

        int lbmp = loadBmp();
        int startValue;
        if (lbmp != 0) {
            startValue = lbmp;
        } else {
            startValue = START_BPM;
        }
        mSeekBar.setProgress(startValue);
        etStep.setText(String.valueOf(startValue));


        if (MetronomeService.isInstanceCreated()) {
            start.setText(R.string.stop);
            start.setBackgroundResource(R.color.colorButtonStop);
//            if (!bound) {
//                bindService(intent, sConn, 0);
//            }
            ifStart = false;
        }
        View.OnClickListener onClickManualPanel = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.buttonStart:
                        startMetronome(ifStart);
                        break;
                    case R.id.buttonVibration:
                        if (ifVibration) {
                            ifVibration = false;
                            vibrationMode(ifVibration);
                        } else {
                            ifVibration = true;
                            vibrationMode(ifVibration);
                        }
                        log("...onClick - vibration " + ifVibration);
                        if (bound) {
                            mService.setVibrate(ifVibration);
                        }
                        break;
                    case R.id.buttonFlash:
                        if (ifFlash) {
                            ifFlash = false;
                            flashMode(ifFlash);
                        } else {
                            ifFlash = true;
                            flashMode(ifFlash);
                        }
                        log("...onClick - flash " + ifFlash);
                        if (bound) {
                            mService.setFlash(ifFlash);
                        }
                        break;
                    case R.id.buttonSound:
                        if (ifSound) {
                            ifSound = false;
                            soundMode(ifSound);

                        } else {
                            ifSound = true;
                            soundMode(ifSound);
                        }
                        log("...onClick - sound " + ifSound);
                        if (bound) {
                            mService.setSound(ifSound);
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        start.setOnClickListener(onClickManualPanel);
        vibration.setOnClickListener(onClickManualPanel);
        flash.setOnClickListener(onClickManualPanel);
        sound.setOnClickListener(onClickManualPanel);



        etStep.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                int pos = etStep.getText().length();
                etStep.setSelection(pos);

                String value = s.toString();
                String regex = "(^1?[0-9]?[0-9])|(^200)";
                if (value.matches(regex) && Integer.parseInt(value) != 0) {
                    etStep.setTextColor(Color.BLACK);

                    mSeekBar.setProgress(Integer.valueOf(String.valueOf(etStep.getText())));
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                            if (bound) {
                                mService.toGo(mSeekBar.getProgress());
                            }
                        }
                    }).start();
                } else {
                    etStep.setTextColor(Color.RED);
                }
            }
        });

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress < MIN_BPM) {
                    progress = MIN_BPM;
                }
                etStep.setText(String.valueOf(progress));
                getDelayBmp();

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!ifStart){
        /*has leaked ServiceConnection*/
        bindService(intent, sConn, 0);}
        log("...onStart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        log("...onStop");
        if (bound) {
            mService.sendNotify();
            unbindService(sConn);
            bound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        log("...onResume");
    }

    public void onClickUp(View v) {
        step = mSeekBar.getProgress() + 1;
        if (step >= MAX_BPM) {
            step = MAX_BPM;
        }
        etStep.setText(String.valueOf(step));
        if (bound) {
            mService.toGo(step);

            log("...onClickUp");
        }
    }

    public void onClickDown(View v) {
        step = mSeekBar.getProgress() - 1;
        if (step <= MIN_BPM) {
            step = MIN_BPM;
        }
        etStep.setText(String.valueOf(step));
        if (bound) {
            mService.toGo(step);
            log("...onClickDown");
        }
    }

    @Override
    protected void onDestroy() {
        log("...onDestroy");
        saved();
        unregisterReceiver(br);
        super.onDestroy();

    }

    protected void startMetronome(boolean isStart) {
        if (isStart) {
            ifStart = false;
            start.setText(R.string.stop);
            start.setBackgroundResource(R.color.colorButtonStop);

            log("...onClick - start");
            startService(intent);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    bindService(intent, sConn, 0);
                }
            }).start();
            log("...onClick - bindService/create");
            getDelayBmp();
        } else {
            ifStart = true;
            start.setText(R.string.start);
            start.setBackgroundResource(R.color.colorButtonStart);

            log("...onClick - stop");

            stopService(intent);
            unbindService(sConn);
            bound = false;
            log("...onClick - stopService");

        }
    }

    protected void vibrationMode(boolean isVibration) {
        if (isVibration) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                vibration.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.v3b), null, null);
            } else {
                vibration.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.v3b), null, null);
            }

        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                vibration.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.v3w), null, null);
            } else {
                vibration.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.v3w), null, null);
            }
        }
    }

    protected void flashMode(boolean isFlash) {
        if (isFlash) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flash.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.f1b), null, null);
            } else {
                flash.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.f1b), null, null);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                flash.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.f1w), null, null);
            } else {
                flash.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.f1w), null, null);
            }
        }
    }

    protected void soundMode(boolean isSound) {
        if (isSound) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sound.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.s1w), null, null);
            } else {
                sound.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.s1w), null, null);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                sound.setCompoundDrawablesWithIntrinsicBounds(null, getDrawable(R.drawable.s1b), null, null);
            } else {
                sound.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.s1b), null, null);
            }
        }
    }

    protected void getDelayBmp() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (bound) {
                            mService.toGo(mSeekBar.getProgress(), ifVibration, ifFlash, ifSound);
                        }
                    }
                });
            }
        }).start();
    }

    protected void saved() {
        sPref = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putInt(SAVED_BMP, mSeekBar.getProgress());
        ed.putBoolean(SAVED_MODE_VIBRATION, ifVibration);
        ed.putBoolean(SAVED_MODE_FLASH, ifFlash);
        ed.putBoolean(SAVED_MODE_SOUND, ifSound);
        ed.commit();
        log("...saved");
    }

    protected void loadMode() {
        sPref = getPreferences(MODE_PRIVATE);
        ifVibration = sPref.getBoolean(SAVED_MODE_VIBRATION, false);
        ifFlash = sPref.getBoolean(SAVED_MODE_FLASH, false);
        ifSound = sPref.getBoolean(SAVED_MODE_SOUND, true);
        log("...loaded mode");
    }

    protected int loadBmp() {
        sPref = getPreferences(MODE_PRIVATE);
        int savedValue = sPref.getInt(SAVED_BMP, 0);
        log("...loaded BMP");
        return savedValue;

    }
/*editText for disable focus*/
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof EditText) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }
}

