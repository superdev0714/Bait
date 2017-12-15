package com.detect.bait;

import android.animation.Animator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.VideoView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;


public class TurnOffScreenActivity extends Activity {

    public static boolean isPowerOff = false;
    private int mBackLight = 0;

    private HomeKeyLocker mHomeKeyLocker;
    TurnOnBroadcastReceiver receiver;

    @BindView(R.id.rlPowerButtons)
    View rlPowerButtons;
    @BindView(R.id.rlBlackOverView)
    View rlBlackOverView;
    @BindView(R.id.videoView)
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_turnoff);
        ButterKnife.bind(this);

        mHomeKeyLocker = new HomeKeyLocker();

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }

        receiver = new TurnOnBroadcastReceiver();


        // Disable Lock screen
        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();

    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter("TurnOn");
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        ActivityManager activityManager = (ActivityManager) getApplicationContext()
                .getSystemService(Context.ACTIVITY_SERVICE);

        activityManager.moveTaskToFront(getTaskId(), 0);
    }

    @Override
    public void onStop() {

        if (receiver != null) {
            unregisterReceiver(receiver);
        }

        super.onStop();
    }



    @OnClick(R.id.poweroff_view)
    public void onPowerOff(View view) {
        // turn off key light
        try {
            mBackLight = Settings.System.getInt(getContentResolver(), "button_key_light");
            Settings.System.putInt(getApplicationContext().getContentResolver(), "button_key_light", 0);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            Log.e("ButtonKeyLight", e.toString());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(getApplicationContext())) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, 200);
                }
            }
        }

        turnOff();
    }

    @OnClick(R.id.resume_view)
    public void onResume(View view) {
        isPowerOff = false;

        IntentFilter filter = new IntentFilter("TurnOn");
        TurnOnBroadcastReceiver receiver = new TurnOnBroadcastReceiver();
        registerReceiver(receiver, filter);

        Settings.System.putInt(getApplicationContext().getContentResolver(), "button_key_light", mBackLight);

        mHomeKeyLocker.unlock();

        finish();
    }

    @Override
    public void onAttachedToWindow() {
        this.getWindow().setFlags(FLAG_NOT_FOCUSABLE, 0xffffff);
        super.onAttachedToWindow();
    }

    private void turnOff() {
        isPowerOff = true;

        mHomeKeyLocker.lock(this);

        rlPowerButtons.setVisibility(View.GONE);

        String uriPath = "android.resource://"+getPackageName()+"/"+R.raw.video;
        videoView.setVideoURI(Uri.parse(uriPath));
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                videoView.setVisibility(View.GONE);
            }
        });

        videoView.start();
    }

    class TurnOnBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Implement code here to be performed when
            // broadcast is detected

            rlBlackOverView.animate().alpha(0.0f).setDuration(1000)
                    .setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {

                            rlBlackOverView.setVisibility(View.GONE);
                            try {
                                Settings.System.putInt(getApplicationContext().getContentResolver(), "button_key_light", mBackLight);
                            } catch (SecurityException e) {
                                Log.e("ButtonKeyLight", e.toString());
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    if (!Settings.System.canWrite(getApplicationContext())) {
                                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                                        startActivityForResult(intent, 200);
                                    }
                                }
                            }

                            if (receiver != null) {
                                unregisterReceiver(receiver);
                                receiver = null;
                            }
                            mHomeKeyLocker.unlock();
                            finish();
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }
                    });

        }
    }


}
