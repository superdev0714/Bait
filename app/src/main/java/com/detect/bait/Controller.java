package com.detect.bait;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;


/**
 * Created by lucas on 12/11/17.
 */

public class Controller extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "Enabled!", Toast.LENGTH_SHORT);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "Disabled!", Toast.LENGTH_SHORT);
    }
}
