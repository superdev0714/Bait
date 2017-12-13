package com.detect.bait;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import butterknife.ButterKnife;
import butterknife.OnClick;


public class TurnOffScreenActivity extends Activity {

    DevicePolicyManager devicePolicyManager;
    ComponentName componentName;

    public static final int RESULT_ENABLE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_turnoff);
        ButterKnife.bind(this);

        devicePolicyManager= (DevicePolicyManager)getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        componentName = new ComponentName(TurnOffScreenActivity.this, Controller.class);

        boolean active = devicePolicyManager.isAdminActive(componentName);

        if (active) {
            Log.e("turnoff", "Disable");
        } else {
            Log.e("turnoff", "Enable");
        }

        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "You should enable the app!");
        startActivityForResult(intent, RESULT_ENABLE);
    }

    @OnClick(R.id.poweroff_view)
    public void onPowerOff(View view) {
        finish();
        devicePolicyManager.lockNow();
    }

    @OnClick(R.id.restart_view)
    public void onRestart(View view) {
        finish();
        devicePolicyManager.lockNow();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.e("poweroff", "enabled!");
                } else {
                    Toast.makeText(this, "Failed!", Toast.LENGTH_SHORT).show();
                }
                return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}
