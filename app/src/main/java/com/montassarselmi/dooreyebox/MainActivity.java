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
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.montassarselmi.dooreyebox.Model.Ring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private FirebaseAuth mAuth =  FirebaseAuth.getInstance();
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference callingRef,boxUsersRef, boxHistoryRef;
    private Button btnCall;
    private SharedPreferences mSharedPreferences;
    private SharedPreferences.Editor editor;
    private DateFormat dateFormat;
    private Date date;
    private String time;
    private DatabaseReference instantImagePathRef = database.getReference().child("BoxList")
            .child(generateId(mAuth.getCurrentUser().getUid())).child("history").child("instantImagePath");
    private Ring ring;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSharedPreferences = getBaseContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        editor = mSharedPreferences.edit();
        callingRef = database.getReference("BoxList/"+generateId(mAuth.getUid()));
        boxUsersRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/users");
        boxHistoryRef = database.getReference("BoxList/"+generateId(mAuth.getUid())+"/history/");
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
                        editor.putBoolean("CHECKING",true);
                        editor.apply();
                        startActivity(new Intent(MainActivity.this, VideoChatActivity.class));
                        finish();
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
            initiateCameraFragment();
            makeCall();

        }
    };

    private void initiateCameraFragment()
    {
        Log.d(TAG, "initiateCameraFragment: ");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_camera, CameraFragment.newInstance())
                .commit();
    }

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
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                boolean hasResponder = false;
                                for (DataSnapshot dp : dataSnapshot.getChildren())
                                {
                                    if (dp.hasChild("Ringing")&& dp.hasChild("pickup"))
                                    {
                                        //Removing all ringing references after 30sc
                                        boxUsersRef.child(dp.getKey()).child("Ringing").removeValue();
                                        if(dp.child("pickup").getValue().equals(false)) {
                                            //Removing all the pickup references if they equal to false
                                            boxUsersRef.child(dp.getKey()).child("pickup").removeValue();
                                        }else hasResponder = true;
                                    }else Log.d(TAG, "No one");
                                    if (dp.hasChild("pickup"))
                                    {
                                        if(dp.child("pickup").getValue().equals(true))
                                            hasResponder = true;
                                    }

                                }
                                callingRef.child("Calling").removeValue();
                                Log.d(TAG, "has responder: "+hasResponder);
                                if (!hasResponder)
                                {
                                    Log.d(TAG, "adding to the history...");
                                    // send to the history
                                    Random random = new Random();
                                    int id = random.nextInt(99999-10000)+10000;
                                    //Date currentTime = Calendar.getInstance().getTime();
                                    dateFormat = new SimpleDateFormat(getResources().getString(R.string.date_format));
                                    //get current date time with Date()
                                    date = new Date();
                                    time = dateFormat.format(date);
                                    ring = new Ring();
                                    ring.setId(id);
                                    ring.setEventTime(time);
                                    ring.setStatus("Ring");
                                    ring.setResponder("No one");
                                    instantImagePathRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            Log.d(TAG, "instant image path \n"+dataSnapshot.getValue());
                                            MainActivity.this.ring.setVisitorImage(dataSnapshot.getValue().toString());
                                            boxHistoryRef.child("rings").child(String.valueOf(MainActivity.this.ring.getEventTime()))
                                                    .setValue(MainActivity.this.ring);
                                            instantImagePathRef.removeValue();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                                    //Ring ring = new Ring(id, currentTime.toString(), imagePath);

                                }
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
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "onChildChanged: get a response and move to the chat activity");
                                        startActivity(new Intent(MainActivity.this,VideoChatActivity.class));
                                        finish();
                                    }
                                    },3000);

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



    public static String generateId(String uid) {

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