package com.detect.bait;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;


public class PowerButtonService extends Service {

    View mView;
    WindowManager wm;

    public PowerButtonService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final LinearLayout mLinear = new LinearLayout(getApplicationContext()) {

            //home or recent button
            public void onCloseSystemDialogs(String reason) {

                TextView tvContent = (TextView)mView.findViewById(R.id.tvContent);

                if ("globalactions".equals(reason)) {
                    Log.i("Key", "Long press on power button");

                    Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    sendBroadcast(closeDialog);

                    if (mView.getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) {
//                        tvContent.setText("Long press on power button");
                        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                                PixelFormat.TRANSLUCENT);
                        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;
                        wm.updateViewLayout(mView, params);

                        restoreScreenOffTimeOut();
                    } else {
                        tvContent.setText("");

                        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                                PixelFormat.TRANSLUCENT);

                        wm.updateViewLayout(mView, params);

                        setScreenOffTimeOut();

                    }

                } else if ("homekey".equals(reason)) {
                    Log.i("Key", "home key pressed");
                } else if ("recentapps".equals(reason)) {
                    Log.i("Key", "recent apps button clicked");
                }
            }
            

            @Override
            public boolean dispatchKeyEvent(KeyEvent event) {

                TextView tvContent = (TextView)mView.findViewById(R.id.tvContent);


                Log.i("Key", Integer.toString(event.getKeyCode()));


                if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
                    Log.i("Key", "Back Key pressed");
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) {
                    Log.i("Key", "Volume Up Key pressed");
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    Log.i("Key", "Volume Down Key pressed");
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_CAMERA) {
                    Log.i("Key", "Camera Key pressed");
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_POWER) {
                    Log.i("Key", "POWER Key pressed");
                    return true;
                }

                return super.dispatchKeyEvent(event);
            }



        };

        mLinear.setFocusable(true);

        mView = LayoutInflater.from(this).inflate(R.layout.service_layout, mLinear);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        //params
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.LEFT | Gravity.CENTER_VERTICAL;

        wm.addView(mView, params);


    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static final int SCREEN_OFF_TIME_OUT = 500;
    private int mSystemScreenOffTimeOut;
    private void setScreenOffTimeOut() {
        try {
            mSystemScreenOffTimeOut = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, SCREEN_OFF_TIME_OUT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void restoreScreenOffTimeOut() {
        if (mSystemScreenOffTimeOut == 0) return;
        try {
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, mSystemScreenOffTimeOut);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
