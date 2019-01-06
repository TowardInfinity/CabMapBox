package com.map.to_in.cabmapbox;

import android.content.Intent;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class DriverLoginActivity extends AppCompatActivity {

    private EditText email, password;
    private Button login, registration;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_login);

        firebaseAuth = FirebaseAuth.getInstance();

        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user != null){
                    Intent intent = new Intent(DriverLoginActivity.this, Main2Activity.class);
                    startActivity(intent);
                    return;
                }
            }
        };

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);

        login = findViewById(R.id.login);
        registration = findViewById(R.id.registration);

        registration.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String emailID = email.getText().toString();
                final String pass = password.getText().toString();
                firebaseAuth.createUserWithEmailAndPassword(emailID, pass).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "Sign Up Error", Toast.LENGTH_SHORT).show();
                        }else{
                            String user_id = firebaseAuth.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("User").child("Driver").child(user_id);
                            current_user_db.setValue(true);
                            Toast.makeText(DriverLoginActivity.this, "User Id Created.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String emailID = email.getText().toString();
                final String pass = password.getText().toString();
                firebaseAuth.signInWithEmailAndPassword(emailID, pass).addOnCompleteListener(DriverLoginActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(Task<AuthResult> task) {
                        if(!task.isSuccessful()) {
                            Toast.makeText(DriverLoginActivity.this, "Sign In Error", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(DriverLoginActivity.this, "Logging In", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }@Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }
}
