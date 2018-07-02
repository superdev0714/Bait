package com.detect.bait;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
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
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.Timer;
import java.util.TimerTask;


public class PowerButtonService extends Service {

    View mView;
    TextView tvContent;

    private float mBatteryLevel = 0.0f;

    private static final String Location_TAG = "MyLocationService";
    private static final String Key_TAG = "Keyguard";

    private LocationManager mLocationManager = null;
    private long location_interval = 5 * 60 * 1000; // 5 mins

    //FireBase auth object
    private FirebaseAuth firebaseAuth;

    private DatabaseReference mDatabase;

    private String activityName = "Bait Mode";

    Location mLastLocation;
    Timer mTimer = new Timer();
    TimerTask mTimerTask;

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

        mDatabase.child("isTracking").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    boolean isTracking = (boolean) dataSnapshot.getValue();
                    if (isTracking) {
                        Constant.isTracking = true;
                        startTrack();
                    } else {
                        Constant.isTracking = false;
                        stopTrack();
                    }
                } catch (NullPointerException e) {
                    Constant.isTracking = false;
                    mDatabase.child("isTracking").setValue(false);
                    stopTrack();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        mDatabase.child("isBaitMode").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {

                    boolean isBaitMode = (boolean) dataSnapshot.getValue();
                    if (isBaitMode != Constant.isBaitMode) {

                        if (isBaitMode) {
                            Constant.isBaitMode = true;
                            activityName = "Bait Mode";

                            SharedPreferences write_data = getApplicationContext().getSharedPreferences(Constant.SHARED_PR.SHARE_PREF, MODE_PRIVATE);
                            SharedPreferences.Editor editor = write_data.edit();
                            editor.putString("activityName", activityName);
                            editor.apply();

                            mDatabase.child("isTracking").setValue(true);
                            detectPowerKeys();
                        } else {
                            Constant.isBaitMode = false;
                            mDatabase.child("isTracking").setValue(false);
                            showHomeActivity();
                        }
                    }

                } catch (NullPointerException e) {
                    if (Constant.isBaitMode) {
                        showHomeActivity();

                        Constant.isBaitMode = false;
                        mDatabase.child("isBaitMode").setValue(false);
                        mDatabase.child("isTracking").setValue(false);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        mDatabase.child("deviceName").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {

                    String newName= (String) dataSnapshot.getValue();
                    String oldName = Settings.System.getString(getContentResolver(), "device_name");
                    if (oldName != newName) {
                        Settings.System.putString(getContentResolver(), "device_name", newName);
                    }

                } catch (NullPointerException e) {
                    String oldName = Settings.System.getString(getContentResolver(), "device_name");
                    mDatabase.child("deviceName").setValue(oldName);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void showHomeActivity() {

        Intent intent = new Intent(getApplicationContext(), HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void startTrack() {

        mDatabase.child("interval").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    location_interval = (long) dataSnapshot.getValue() * 1000;
                } catch (NullPointerException e) {
                    mDatabase.child("interval").setValue(300); // 5 mins.
                    location_interval = 300 * 1000;
                }

                // start location track with time interval
                if (Constant.isTracking) {

                    initializeLocationManager();

                    startLocationTrack();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void stopTrack() {
        stopSchedule();
        removeLocationListeners();
    }

    public void detectPowerKeys() {

        final LinearLayout mLinear = new LinearLayout(getApplicationContext()) {

            //home or recent button
            public void onCloseSystemDialogs(String reason) {

                Log.i(Key_TAG, reason);

                if (!Constant.isBaitMode) {
                    return;
                }

                tvContent = (TextView)mView.findViewById(R.id.tvContent);


                if ("globalactions".equals(reason)) {   // long press on power button
                    Log.i(Key_TAG, "Long press on power button");

                    Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                    sendBroadcast(closeDialog);

                    if (!Constant.isPowerOff) {

                        // disable button sound, vibration
                        try {
                            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 0);
                            Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 0);
                        } catch (Exception e) {
                            Log.e("soundOff", e.toString());
                            e.printStackTrace();
                        }

                        // show power off dialog
                        Intent intent = new Intent(getContext(), TurnOffScreenActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);

                    } else {

                        try {
                            Settings.System.putInt(getContentResolver(), Settings.System.SOUND_EFFECTS_ENABLED, 1);
                            Settings.System.putInt(getContentResolver(), Settings.System.HAPTIC_FEEDBACK_ENABLED, 1);
                        } catch (Exception e) {
                            Log.e("soundOff", e.toString());
                            e.printStackTrace();
                        }

                        Constant.isPowerOff = false;
                        Intent intent = new Intent();
                        intent.setAction("TurnOn");
                        sendBroadcast(intent);
                    }


                } else if ("homekey".equals(reason)) {
                    Log.i("Key", "home key pressed");
                } else if ("recentapps".equals(reason)) {
                    Log.i("Key", "recent apps button clicked");

                    if (Constant.isPowerOff) {
                        Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                        sendBroadcast(closeDialog);
                    }
                }
            }

        };

        mLinear.setFocusable(true);

        mView = LayoutInflater.from(this).inflate(R.layout.service_layout, mLinear);
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        tvContent = (TextView)mView.findViewById(R.id.tvContent);

        //params
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
//                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        wm.addView(mView, params);
    }


    ////// location service
    private void stopSchedule() {

        if (mTimerTask != null) {
            Log.d("====", "timer canceled");
            mTimerTask.cancel();
        }
    }
    private void startLocationTrack() {
        stopSchedule();

        mTimerTask = new TimerTask() {
            @Override
            public void run() {
                uploadLocationData();
            }
        };

        mTimer.scheduleAtFixedRate(mTimerTask, 0, location_interval);

        removeLocationListeners();

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0,
                    mLocationListeners[0]);

        } catch (java.lang.SecurityException ex) {
            Log.i(Location_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(Location_TAG, "gps provider does not exist " + ex.getMessage());
        }

        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 0,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(Location_TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(Location_TAG, "network provider does not exist, " + ex.getMessage());
        }
    }

    // upload gps, time, battery info to Firebase.
    private void uploadLocationData() {

        if (mLastLocation == null || (mLastLocation.getLongitude() == 0 && mLastLocation.getLatitude() == 0 )) {
            return;
        }

        Date current = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = dateFormat.format(current);

        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(Constant.SHARED_PR.SHARE_PREF, MODE_PRIVATE);
        this.activityName = sharedPreferences.getString("activityName", "");

        if (activityName == "") {

            return;
        }

        DatabaseReference databaseReference = mDatabase.child("locations").child(strDate).push();

        dateFormat = new SimpleDateFormat("HH:mm:ss");

        String strTime = dateFormat.format(current);

        Map<String, Object> locationMap = new HashMap<>();
        locationMap.put("latitude", mLastLocation.getLatitude());
        locationMap.put("longitude", mLastLocation.getLongitude());

        locationMap.put("time", strTime);
        mBatteryLevel = getBatteryLevel();
        locationMap.put("battery", mBatteryLevel);
        locationMap.put("activityName", activityName);
        locationMap.put("speed", mLastLocation.getSpeed());
        locationMap.put("provider", mLastLocation.getProvider());

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

        public LocationListener(String provider) {
            Log.e(Location_TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(Location_TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(Location_TAG, "onProviderDisabled: " + provider);
            Toast.makeText(getApplicationContext(), "Location is disabled. Please enable your location on Settings.", Toast.LENGTH_SHORT).show();
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
        Log.e(Location_TAG, "initializeLocationManager - LOCATION_INTERVAL: "+ location_interval);

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
        stopTrack();
    }

}
