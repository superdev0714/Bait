package com.detect.bait;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class RegisterActivity extends Activity {

    //FireBase auth object
    private FirebaseAuth firebaseAuth;

    @BindView(R.id.etEmail)
    EditText editTextEmail;
    @BindView(R.id.etPassword)
    EditText editTextPassword;
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        //getting firebase auth object
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @OnClick(R.id.sign_in_button)
    public void onSignIn(View view) {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @OnClick(R.id.sign_up_button)
    public void onSignUp(View view) {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(getApplicationContext(), "Enter email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            Toast.makeText(getApplicationContext(), "Enter password!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(getApplicationContext(), "Password too short, enter minimum 6 characters!", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        //create user
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {

                    progressBar.setVisibility(View.GONE);

                    if (!task.isSuccessful()) {
                        Log.e("Authentication failed", task.getException().toString());
                        Toast.makeText(RegisterActivity.this, "Authentication failed." + task.getException(),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, "You are registered successfully", Toast.LENGTH_SHORT).show();

                        String email = firebaseAuth.getCurrentUser().getEmail();
                        String userId = firebaseAuth.getCurrentUser().getUid();
                        int initialInterval = 10 * 60;  // 10 mins
                        String device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(),
                                Settings.Secure.ANDROID_ID);

                        try {
                            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
                        } catch (DatabaseException e) {
                            e.printStackTrace();
                        }
                        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                        DatabaseReference userDatabase = mDatabase.child("users").child(userId);

                        userDatabase.child("profile").child("email").setValue(email);
                        userDatabase.child(device_id).child("interval").setValue(initialInterval);

                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });
    }

    @OnClick(R.id.reset_password_button)
    public void onReset(View view) {
        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

}
