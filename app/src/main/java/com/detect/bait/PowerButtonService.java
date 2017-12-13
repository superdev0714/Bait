package com.detect.bait;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class PowerButtonService extends Service {

    View mView;

    private float mBatteryLevel = 0.0f;

    private static final String Location_TAG = "MyLocationService";
    private static final String Key_TAG = "Keyguard";

    private LocationManager mLocationManager = null;
    private long location_interval = 30 * 60 * 1000; // 30 mins
    private static final float LOCATION_DISTANCE = 0.0f;

    //firebase auth object
    private FirebaseAuth firebaseAuth;

    private DatabaseReference mDatabase;


    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(Location_TAG, "onCreate");

        mBatteryLevel = getBatteryLevel();

        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        String userId = user.getUid();

        String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        mDatabase = databaseReference.child("users").child(userId).child(device_id);

        mDatabase.child("interval").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    location_interval = (long) dataSnapshot.getValue() * 1000;
                } catch (NullPointerException e) {
                    location_interval = 600;
                    mDatabase.child("interval").setValue(location_interval);
                }
                // start location track with time interval
                initializeLocationManager();
                removeLocationListeners();
                startLocationTrack();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        detectPowerKeys();
    }


    public void detectPowerKeys() {

        final LinearLayout mLinear = new LinearLayout(getApplicationContext()) {

            //home or recent button
            public void onCloseSystemDialogs(String reason) {

                Log.i(Key_TAG, reason);

                if ("globalactions".equals(reason)) {
                    Log.i(Key_TAG, "Long press on power button");

                    Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    sendBroadcast(closeDialog);

                    Intent intent = new Intent(getContext(), TurnOffScreenActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);

                } else if ("homekey".equals(reason)) {
                    Log.i("Key", "home key pressed");
                } else if ("recentapps".equals(reason)) {
                    Log.i("Key", "recent apps button clicked");
                }
            }

        };

        mLinear.setFocusable(true);

        mView = LayoutInflater.from(this).inflate(R.layout.service_layout, mLinear);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

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

        wm.addView(mView, params);

    }


    ////// location service

    private void startLocationTrack() {

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, location_interval, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(Location_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(Location_TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, location_interval, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(Location_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(Location_TAG, "gps provider does not exist " + ex.getMessage());
        }
    }

    private void uploadLocationData(Location location) {

        DatabaseReference databaseReference = mDatabase.child("locations").push();

        Date time = new Date(location.getTime());
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddkkmmss");
        String strTime = dateFormat.format(time);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("latitude", location.getLatitude());
        locationMap.put("longitude", location.getLongitude());
        locationMap.put("time", strTime);
        locationMap.put("battery", mBatteryLevel);

        databaseReference.updateChildren(locationMap);


        float battery = getBatteryLevel();
        Log.e("BatteryLevel", Float.toString(battery));

    }


    private float getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        // Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return 50.0f;
        }

        return ((float)level / (float)scale) * 100.0f;
    }

    private class LocationListener implements android.location.LocationListener {
        Location mLastLocation;

        public LocationListener(String provider) {
            Log.e(Location_TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);

        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(Location_TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);

            mBatteryLevel = getBatteryLevel();
            uploadLocationData(mLastLocation);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(Location_TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(Location_TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(Location_TAG, "onStatusChanged: " + provider);

        }
    }



    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

    @Override
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(Location_TAG, "onStartCommand");
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    private void initializeLocationManager() {
        Log.e(Location_TAG, "initializeLocationManager - LOCATION_INTERVAL: "+ location_interval + " LOCATION_DISTANCE: " + LOCATION_DISTANCE);
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void removeLocationListeners() {
        if (mLocationManager != null) {
            for (int i = 0; i < mLocationListeners.length; i++) {
                try {
                    mLocationManager.removeUpdates(mLocationListeners[i]);
                } catch (Exception ex) {
                    Log.i(Location_TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
    }
    @Override
    public void onDestroy()
    {
        Log.e(Location_TAG, "onDestroy");
        super.onDestroy();
        removeLocationListeners();
    }

}
