package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    
    private FirebaseAuth mAuth;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference callingRef,boxUsersRef;
    private Button btnCall;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getBaseContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();

         mAuth = FirebaseAuth.getInstance();
        callingRef = database.getReference("BoxList/"+generateId(mAuth.getUid()));
        boxUsersRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/users");
        btnCall = (Button) findViewById(R.id.btnMakeCall);
        btnCall.setEnabled(true);
        btnCall.setOnClickListener(callListener);

        checkFrontDoor();

    }

    private void checkFrontDoor()
    {
        boxUsersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    if (dp.hasChild("checking")) {
                        //Log.d(TAG, "WhoPickedUp \n" + dp.child("Ringing").getValue().toString());
                        //if (boxUsersRef.child(dp.getKey()).child("Ringing").child("pickup").getKey())
                        startActivity(new Intent(MainActivity.this, VideoChatActivity.class));
                        //finish();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: "+databaseError.toString());
            }
        });
    }

    private View.OnClickListener callListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            btnCall.setEnabled(false);
            Toast.makeText(MainActivity.this, "Calling...", Toast.LENGTH_SHORT).show();
            makeCall();

            //startActivity(new Intent(MainActivity.this,CallingActivity.class));
            //finish();
        }
    };

    private void makeCall()
    {

        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                for (DataSnapshot dp : dataSnapshot.getChildren())
                {
                    //generate both references for calling and ringing
                    //create a calling reference and define the numbers that this box is calling
                    //create a Ringing reference below all the user that this box is calling.
                    Log.d(TAG, "onDataChange: "+dp.getKey());
                    //send roomId to firebase(opentok roomId)
                    if (!dp.child("status").getValue().equals("waiting")) {
                        callingRef.child("Calling").child(dp.getKey()).setValue("Calling...");
                        boxUsersRef.child(dp.getKey()).child("Ringing").setValue("Ringing...");
                        boxUsersRef.child(dp.getKey()).child("pickup").setValue(false);
                    }
                    //startActivity(new Intent(MainActivity.this, VideoChatActivity.class));
                    //finish();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                for (DataSnapshot dp : dataSnapshot.getChildren())
                                {
                                    if (dp.hasChild("Ringing")&& dp.hasChild("pickup"))
                                    {
                                        boxUsersRef.child(dp.getKey()).child("Ringing").removeValue();
                                        if(dp.child("pickup").getValue().equals(false)) {
                                            boxUsersRef.child(dp.getKey()).child("pickup").removeValue();
                                        }
                                    }


                                }
                                callingRef.child("Calling").removeValue();
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                },30000);

                boxUsersRef.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        Log.d(TAG, "onChildChanged: pickup \n"+dataSnapshot.toString()+"\n"+s);
                        if (dataSnapshot.hasChild("pickup"))
                        {
                            if (dataSnapshot.child("pickup").getValue().equals(true))
                            {
                                // remove all other references
                                boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        for (DataSnapshot dp : dataSnapshot.getChildren())
                                        {
                                            if (dp.hasChild("Ringing")&& dp.hasChild("pickup"))
                                            {
                                                boxUsersRef.child(dp.getKey()).child("Ringing").removeValue();
                                                if(dp.child("pickup").getValue().equals(false)) {
                                                    boxUsersRef.child(dp.getKey()).child("pickup").removeValue();
                                                }
                                            }


                                        }
                                        callingRef.child("Calling").removeValue();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                                //---------------------------
                                Log.d(TAG, "onChildChanged: get a response and move to the chat activity");
                                startActivity(new Intent(MainActivity.this,VideoChatActivity.class));
                                finish();
                            }
                        }
                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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
        //destroy the app when back button pressed.
        finish();
        System.exit(0);
    }
}
