package com.detect.bait;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class HomeActivity extends Activity {

    public final static int REQUEST_CODE = 10101;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference mDatabase;

    @BindView(R.id.btn_start_track)
    Button btnStartTrack;
    @BindView(R.id.btn_stop_track)
    Button btnStopTrack;
    @BindView(R.id.view_dialog)
    View viewDialog;
    @BindView(R.id.txt_activityName)
    AutoCompleteTextView txtActivityName;

    private void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(getApplicationContext())) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 200);

            }
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);

        //getting FireBase auth object
        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() == null){
            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }

        checkStatus();

        requestPermission();

        if (checkDrawOverlayPermission()) {
            setInitialInterval();

            final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                displayLocationSettingsRequest(this);
            } else {
                startTrackService();
            }
        }
    }

    private void checkStatus() {
        String userId = firebaseAuth.getCurrentUser().getUid();

        final String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        if (mDatabase == null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            try {
                database.setPersistenceEnabled(true);
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            mDatabase = database.getReference();
        }

        final DatabaseReference userDatabase = mDatabase.child("users").child(userId);

        userDatabase.child(device_id).child("isTracking").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isTracking = false;
                try {
                    isTracking = (boolean) dataSnapshot.getValue();
                } catch (NullPointerException e) {
                    userDatabase.child(device_id).child("isTracking").setValue(isTracking);
                }

                if (isTracking) {
                    btnStartTrack.setVisibility(View.GONE);
                    btnStopTrack.setVisibility(View.VISIBLE);
                } else {
                    btnStartTrack.setVisibility(View.VISIBLE);
                    btnStopTrack.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        userDatabase.child(device_id).child("isBaitMode").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    boolean isBaitMode = (boolean) dataSnapshot.getValue();
                    Log.d("======", "home activity");
                    if (isBaitMode) {
//                        Constant.isBaitMode = true;
                        finish();
                    }
                } catch (NullPointerException e) {
                    Constant.isBaitMode = false;
                    userDatabase.child(device_id).child("isBaitMode").setValue(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @OnClick(R.id.btn_start_track)
    public void onStartTrack(View view) {
        this.viewDialog.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btn_stop_track)
    public void onStopTrack(View view) {
        String userId = firebaseAuth.getCurrentUser().getUid();
        String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        mDatabase.child("users").child(userId).child(device_id).child("isTracking").setValue(false);
    }

    @OnClick(R.id.btn_ok)
    public void onOk(View view) {

        String strActivityName = txtActivityName.getText().toString().trim();

        if (TextUtils.isEmpty(strActivityName)) {
            Toast.makeText(getApplicationContext(), "Enter Activity Name!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPreferences = getSharedPreferences(Constant.SHARED_PR.SHARE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("activityName", strActivityName);
        editor.apply();

        if (checkDrawOverlayPermission()) {
            setInitialInterval();

            final LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                displayLocationSettingsRequest(this);
            } else {
                String userId = firebaseAuth.getCurrentUser().getUid();
                String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                mDatabase.child("users").child(userId).child(device_id).child("isTracking").setValue(true);
            }

            this.viewDialog.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.btn_cancel)
    public void onCancel(View view) {
        this.viewDialog.setVisibility(View.GONE);
    }


    private void startTrackService() {
        Intent intent = new Intent(HomeActivity.this, PowerButtonService.class);
        startService(intent);
    }

    @OnClick(R.id.btn_logout)
    public void onLogout(View view) {
        firebaseAuth.signOut();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (!Settings.canDrawOverlays(this)) {
            /** if not construct intent to request permission */
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            /** request permission via start activity for result */
            startActivityForResult(intent, REQUEST_CODE);
            return false;
        } else {
            return true;
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                startService(new Intent(this, PowerButtonService.class));
            }
        }

        if (requestCode == LoginActivity.REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                startTrackService();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "You can not track your location. Please enable your location on Settings", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (! hasFocus) {
            Intent closeDialog = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            sendBroadcast(closeDialog);
        }
    }

    private void setInitialInterval() {
        String email = firebaseAuth.getCurrentUser().getEmail();
        String userId = firebaseAuth.getCurrentUser().getUid();

        final String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                Settings.Secure.ANDROID_ID);

        String deviceName = Settings.System.getString(getContentResolver(), "device_name");

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = telephonyManager.getDeviceId();
        String phoneNumber = telephonyManager.getLine1Number();

        if (mDatabase == null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            try {
                database.setPersistenceEnabled(true);
            } catch (DatabaseException e) {
                e.printStackTrace();
            }
            mDatabase = database.getReference();
        }

        final DatabaseReference userDatabase = mDatabase.child("users").child(userId);

        userDatabase.child("profile").child("email").setValue(email);

        userDatabase.child(device_id).child("deviceName").setValue(deviceName);
        userDatabase.child(device_id).child("deviceMake").setValue(Build.MANUFACTURER);
        userDatabase.child(device_id).child("deviceModel").setValue(Build.MODEL);
        userDatabase.child(device_id).child("phoneNumber").setValue(phoneNumber);
        userDatabase.child(device_id).child("deviceImei").setValue(imei);

        userDatabase.child(device_id).child("interval").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long initialInterval = 300;
                try {
                    initialInterval = (long) dataSnapshot.getValue();
                } catch (NullPointerException e) {
                    userDatabase.child(device_id).child("interval").setValue(initialInterval);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void displayLocationSettingsRequest(Context context) {
        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(LocationServices.API).build();
        googleApiClient.connect();

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000 / 2);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result = LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        Log.i("MyDeviceLocation", "All location settings are satisfied.");
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        Log.i("MyDeviceLocation", "Location settings are not satisfied. Show the user a dialog to upgrade location settings ");

                        try {
                            // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                            status.startResolutionForResult(HomeActivity.this, LoginActivity.REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.i("MyDeviceLocation", "PendingIntent unable to execute request.");
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        Log.i("MyDeviceLocation", "Location settings are inadequate, and cannot be fixed here. Dialog not created.");
                        break;
                }
            }
        });
    }

    @Override
    public void onDestroy()
    {
        Log.e("======", "onDestroy");
        try {
            String userId = firebaseAuth.getCurrentUser().getUid();

            final String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                    Settings.Secure.ANDROID_ID);

            if (mDatabase == null) {
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                try {
                    database.setPersistenceEnabled(true);
                } catch (DatabaseException e) {
                    e.printStackTrace();
                }
                mDatabase = database.getReference();
            }

            final DatabaseReference userDatabase = mDatabase.child("users").child(userId);
            userDatabase.child(device_id).child("isTracking").setValue(false);

        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }
}
