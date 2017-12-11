package com.detect.bait;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;


/**
 * Created by lucas on 12/11/17.
 */

public class TurnOffScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turnoff);

        IntentFilter filter = new IntentFilter("turnOn");
        this.registerReceiver(new TurnOffScreenActivity.Receiver(), filter);




    }


    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context arg0, Intent arg1) {
            boolean turnoff = arg1.getExtras().getBoolean("poweroff");

            Log.e("Test", Boolean.toString(turnoff));

            if (!turnoff) {
                finish();
            }
        }
    }
}
