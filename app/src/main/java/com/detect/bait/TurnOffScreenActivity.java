package com.detect.bait;

import android.animation.Animator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
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


public class TurnOffScreenActivity extends Activity {

    public static boolean isPowerOff = false;
    private int mBackLight = 0;

//    private HomeKeyLocker mHomeKeyLocker;
    TurnOnBroadcastReceiver receiver;

    @BindView(R.id.rlPowerButtons)
    View rlPowerButtons;
    @BindView(R.id.rlBlackOverView)
    View rlBlackOverView;
    @BindView(R.id.videoView)
    VideoView videoView;
    @BindView(R.id.mainView)
    View mainView;

    AudioManager mAudioManager;
    int mPrevRingerMode = AudioManager.RINGER_MODE_SILENT;
    int mPrevRingVolume = 0;
    int mPrevMusicVolume = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_turnoff);
        ButterKnife.bind(this);

//        mHomeKeyLocker = new HomeKeyLocker();

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mPrevRingerMode = mAudioManager.getRingerMode();
        mPrevMusicVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mPrevRingVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_RING);

        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,0,0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,0,0);

        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        receiver = new TurnOnBroadcastReceiver();


        // Disable Lock screen
        KeyguardManager keyguardManager = (KeyguardManager)getSystemService(Activity.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = keyguardManager.newKeyguardLock(KEYGUARD_SERVICE);
        lock.disableKeyguard();



        final int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE;

        mainView.setSystemUiVisibility(uiOptions);

        mainView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                mainView.setSystemUiVisibility(uiOptions);
            }
        });

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

//        mHomeKeyLocker.unlock();

        mAudioManager.setRingerMode(mPrevRingerMode);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,mPrevMusicVolume,0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,mPrevRingVolume,0);

        finish();
    }

    private void turnOff() {
        isPowerOff = true;

        mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),0);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,mAudioManager.getStreamMaxVolume(AudioManager.STREAM_RING),0);


//        mHomeKeyLocker.lock(this);

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
//                            mHomeKeyLocker.unlock();
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
