package com.detect.bait;

import android.animation.Animator;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class TurnOffScreenActivity extends Activity {

    public static boolean isPowerOff = false;

    private HomeKeyLocker mHomeKeyLocker;

    @BindView(R.id.rlBlackOverView)
    View rlBlackOverView;
    @BindView(R.id.rlPowerButtons)
    View rlPowerButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_turnoff);
        ButterKnife.bind(this);

        mHomeKeyLocker = new HomeKeyLocker();
        mHomeKeyLocker.lock(this);

        IntentFilter filter = new IntentFilter("TurnOn");
        TurnOnBroadcastReceiver receiver = new TurnOnBroadcastReceiver();
        registerReceiver(receiver, filter);
    }

    @OnClick(R.id.poweroff_view)
    public void onPowerOff(View view) {
        turnOff();
    }

    @OnClick(R.id.restart_view)
    public void onRestart(View view) {
        turnOff();
    }

    private void turnOff() {
        isPowerOff = true;

        rlPowerButtons.setVisibility(View.GONE);
        rlBlackOverView.setVisibility(View.VISIBLE);
        rlBlackOverView.setAlpha(0);

        rlBlackOverView.animate().alpha(1.0f).setDuration(1000);

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
                            mHomeKeyLocker.unlock();
                            finish();
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
