package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class SplashActivity extends AppCompatActivity {


    private final String TAG="SplashActivity";
    private FirebaseAuth mAuth;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference boxsRef = database.getReference("BoxList");




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();


    }

    private void logAsGuest()
    {
        mAuth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful())
                {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInAnonymously:success");
                    FirebaseUser user = mAuth.getCurrentUser();
                    buildBox();
                    updateUI(user);
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInAnonymously:failure", task.getException());
                    Toast.makeText(SplashActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
                    updateUI(null);
                }
            }


        });

    }
    private  void buildBox()
    {
        DatabaseReference mRef = database.getReference("BoxList/"+generateId(mAuth.getUid()));
        mRef.child("boxID").setValue(generateId(mAuth.getUid()));
    }

    public String generateId(String uid) {

        // Generate a Shortcut from the box id with 6 letters.
        if (uid != null) {
            StringBuilder id = new StringBuilder();
            for (int i = uid.length() - 6; i < uid.length(); i++) {
                id.append(uid.charAt(i));
            }
            return id.toString();
        }else return null;
    }


    private void updateUI(FirebaseUser currentUser)
    {
        if (currentUser != null) {
            checkBoxAvailability();
            Intent intent = new Intent(SplashActivity.this,MainActivity.class);
            startActivity(intent);
            finish();
            Log.d(TAG, "updateUI: userUid= "+currentUser.getUid());
        }
        else
            logAsGuest();


    }

    private void checkBoxAvailability() {

        boxsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                FirebaseUser currentUser = mAuth.getCurrentUser();
                Log.d(TAG, "onDataChange: currentUser = "+currentUser.getUid());
                //check for the box availability.
                if (dataSnapshot.hasChild(generateId(currentUser.getUid())))
                {

                    Toast.makeText(SplashActivity.this, "find it", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(SplashActivity.this, "nope", Toast.LENGTH_SHORT).show();
                    //if the box not available or deleted than recreate an other box.
                    buildBox();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        updateUI(mAuth.getCurrentUser());
    }
}
