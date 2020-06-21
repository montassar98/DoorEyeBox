package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.montassarselmi.dooreyebox.Model.Ring;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    private  IntentFilter filter;
    private Button btnStart, btnStop;
    private TextView textView;
    private MutableLiveData<Boolean> ringData = new MutableLiveData<>();


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
        btnStop = (Button) findViewById(R.id.btn_stop);
        textView = (TextView) findViewById(R.id.textView);
        btnCall.setEnabled(true);
        btnCall.setOnClickListener(callListener);

        checkFrontDoor();
        initUsbSerial();
        ringData.setValue(false);
        startUsbConnection();

        ringData.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                Toast.makeText(MainActivity.this, "observed value = "+aBoolean, Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void initUsbSerial() {
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
    }

    private void checkFrontDoor() {
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

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data = null;
            try {
                data = new String(arg0, "UTF-8");
                data.concat("/n");
                tvAppend(textView, data);
                ringData.postValue(true);
                //Toast.makeText(getApplicationContext(), "data received", Toast.LENGTH_SHORT).show();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }


        }
    };

    private void initiateCameraFragment()
    {
        Log.d(TAG, "initiateCameraFragment: ");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_camera, CameraFragment.newInstance())
                .commit();
    }

    private void makeCall() {
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
                                    ring = new Ring();
                                    ring.setId(id);
                                    ring.setEventTime(date);
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
                                            startActivity(new Intent(MainActivity.this,MainActivity.class));
                                            finish();
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            startActivity(new Intent(MainActivity.this,MainActivity.class));
                                            finish();
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

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            //setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");
                            Toast.makeText(context, "Serial Connection Opened!", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                startUsbConnection();

            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(btnStop);
                Toast.makeText(context, "Serial Connection Closed.", Toast.LENGTH_LONG).show();
            }
        }
    };

    private void startUsbConnection() {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                {
                    PendingIntent pi = PendingIntent.getBroadcast(
                            this,
                            0,
                            new Intent(ACTION_USB_PERMISSION),
                            0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop(View view) {
        //setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");
        Toast.makeText(this, "Serial Connection Closed!", Toast.LENGTH_LONG).show();

    }

    @Override
    protected void onStart() {
        super.onStart();
        //registerReceiver(broadcastReceiver, filter);


    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(broadcastReceiver);
    }

    public void onClickSend(View view) {
        String string = "data from android";
        serialPort.write(string.getBytes());
        tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        //Toast.makeText(this, ""+String.valueOf(text), Toast.LENGTH_SHORT).show();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }

    public void onClickCheck(View view) {
        Toast.makeText(this, "", Toast.LENGTH_SHORT).show();
    }
}