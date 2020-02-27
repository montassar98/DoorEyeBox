package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    
    private Button btnMakeCall;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference callingRef,ringingRef,boxUsersRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         mAuth = FirebaseAuth.getInstance();
        callingRef = database.getReference("BoxList/"+generateId(mAuth.getUid()));
        boxUsersRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/users");
        findViewById(R.id.btnMakeCall).setOnClickListener(callListener);
    }
    
    private View.OnClickListener callListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
           // Toast.makeText(MainActivity.this, "Calling...", Toast.LENGTH_SHORT).show();
            makeCall();
            //startActivity(new Intent(MainActivity.this,CallingActivity.class));
            //finish();
        }
    };

    private void makeCall()
    {
        //callingRef.child("calling").child.(setValue(mAuth.getUid());
        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    Toast.makeText(MainActivity.this, ""+dp.getKey(), Toast.LENGTH_LONG).show();
                    Log.d(TAG, "onDataChange: "+dp.getKey());
                    callingRef.child("Calling").child(dp.getKey()).setValue("calling...");
                    boxUsersRef.child(dp.getKey()).child("Ringing").child(generateId(mAuth.getUid())).setValue("ringing...");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    public String generateId(String uid) {

        if (uid != null) {
            StringBuilder id = new StringBuilder();
            for (int i = uid.length() - 6; i < uid.length(); i++) {
                id.append(uid.charAt(i));
            }
            return id.toString();
        }else return null;
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        System.exit(0);
    }
}
