package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.felhr.utils.Utils;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.montassarselmi.dooreyebox.Model.Motion;
import com.montassarselmi.dooreyebox.Model.Ring;
import com.pd.chocobar.ChocoBar;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
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
    private Motion motion;

    public final String ACTION_USB_PERMISSION = "com.hariharan.arduinousb.USB_PERMISSION";
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    private  IntentFilter filter;
    private Button btnStart, btnStop;
    private TextView textView;
    private MutableLiveData<Boolean> ringData = new MutableLiveData<>();
    private MutableLiveData<Boolean> motionData = new MutableLiveData<>();

    private final int LEVEL_SERIAL_CONNECTION_CLOSED = 0;
    private final int LEVEL_PORT_NOT_OPEN = 1;
    private final int LEVEL_PORT_IS_NULL = 2;
    private final int LEVEL_PERMISSION_NOT_GRANTED = 3;
    private String motionOn = "O",motionOff = "F",ringYes = "Y", ringNo = "N";
    private int count =0;



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
        textView = (TextView) findViewById(R.id.textView);
        tvAppend(textView,"onCreate");
        btnCall.setEnabled(true);
        btnCall.setOnClickListener(callListener);

        checkFrontDoor();



        ringData.observe(this, aBoolean -> {
            Toast.makeText(MainActivity.this, "observed value = "+aBoolean, Toast.LENGTH_SHORT).show();
            if (aBoolean) {
                Toast.makeText(MainActivity.this, "call received", Toast.LENGTH_SHORT).show();
                initiateCameraFragment();
                makeCall();
            }

        });

        motionData.observe(this, aBoolean -> {
            if (aBoolean){
                count++;
                Toast.makeText(MainActivity.this, "Motion Detected "+count, Toast.LENGTH_SHORT).show();
                serialPort.write(motionOff.getBytes());

                new Handler().postDelayed(() -> {
                    serialPort.write(motionOn.getBytes());

                    closeCameraFragment();
                },30000);
                new Handler().postDelayed(this::createMotionHistory, 5000);

                initiateCameraFragment();

            }
            motionData.postValue(false);
        });

    }

    private void createMotionHistory() {
        Random random = new Random();
        int id = random.nextInt(99999-10000)+10000;
        date = new Date();
        motion = new Motion();
        motion.setId(id);
        motion.setEventTime(date);
        motion.setStatus("Motion");
        instantImagePathRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                Log.d(TAG, "instant image path \n"+ dataSnapshot1.getValue());
                MainActivity.this.motion.setVisitorImage(dataSnapshot1.getValue().toString());
                boxHistoryRef.child("motions").child(String.valueOf(MainActivity.this.motion.getEventTime()))
                        .setValue(MainActivity.this.motion);
                instantImagePathRef.removeValue();
                //startActivity(new Intent(MainActivity.this,MainActivity.class));
                //finish();
                           }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                startActivity(new Intent(MainActivity.this,MainActivity.class));
                finish();

            }
        });
        //Ring ring = new Ring(id, currentTime.toString(), imagePath);

    }



    private void closeCameraFragment() {
        Fragment frm = getSupportFragmentManager().findFragmentByTag("camera_fragment");
        if(frm!=null){
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(frm);
            fragmentTransaction.commit();
        }
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
                        //TODO finish();
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
                //data1 = new String(arg0,"UTF-8" );
                data = new String(arg0,"UTF-8");
                String byteArrayStr= new String(Base64.encodeToString(arg0, Base64.DEFAULT));




                String ring= "r";
            final String motion= "m";

            if (Arrays.equals(arg0,ring.getBytes())) {
                ringData.postValue(true);
                tvAppend(textView,"ring");

                serialPort.write(ringNo.getBytes());

            }
            if (Arrays.equals(arg0,motion.getBytes())){
                motionData.postValue(true);
                tvAppend(textView,"motion");
            }

                //tvAppend(textView, data);

            } catch (UnsupportedEncodingException  e) {
                e.printStackTrace();
            }


        }
    };


    private void initiateCameraFragment() {
        Log.d(TAG, "initiateCameraFragment: ");
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container_camera, CameraFragment.newInstance(),"camera_fragment")
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
                new Handler().postDelayed(() -> boxUsersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                        boolean hasResponder = false;
                        for (DataSnapshot dp : dataSnapshot1.getChildren())
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
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {
                                    Log.d(TAG, "instant image path \n"+ dataSnapshot1.getValue());
                                    MainActivity.this.ring.setVisitorImage(dataSnapshot1.getValue().toString());
                                    boxHistoryRef.child("rings").child(String.valueOf(MainActivity.this.ring.getEventTime()))
                                            .setValue(MainActivity.this.ring);
                                    instantImagePathRef.removeValue();
                                    //startActivity(new Intent(MainActivity.this,MainActivity.class));
                                    //finish();
                                    closeCameraFragment();
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
                }),30000);

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
                                new Handler().postDelayed(() -> {
                                    Log.d(TAG, "onChildChanged: get a response and move to the chat activity");
                                    startActivity(new Intent(MainActivity.this,VideoChatActivity.class));
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
                            showGreenSnackbar("Serial Connection Opened!");
                            tvAppend(textView,"Serial Connection Opened!\n");

                            new Handler().postDelayed(() -> {
                                serialPort.write(motionOn.getBytes());
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                serialPort.write(ringYes.getBytes());

                            },2000);

                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                            showRedSnackbar("Port Not Open!",LEVEL_PORT_NOT_OPEN);
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                        showRedSnackbar("Port is Null!",LEVEL_PORT_IS_NULL);

                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                    showRedSnackbar("Permission Not Granted!",LEVEL_PERMISSION_NOT_GRANTED);

                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                startUsbConnection();

            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                stopUsbConnection();
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

    public void stopUsbConnection() {
        //setUiEnabled(false);
        if ( serialPort != null ){
         if (serialPort.isOpen()){
            //String s = "0";
            //serialPort.write(s.getBytes());
            //serialPort.close();
         }
        }
        tvAppend(textView,"\nSerial Connection Closed! \n");
        showRedSnackbar("Serial Connection Closed!", LEVEL_SERIAL_CONNECTION_CLOSED);

    }

    @Override
    protected void onStart() {
        super.onStart();
        tvAppend(textView,"onStart");
        ringData.setValue(false);
        motionData.setValue(false);
        initUsbSerial();
        startUsbConnection();
        if ( serialPort != null ){
            if (serialPort.isOpen()){
                Toast.makeText(this, "open", Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(this, "Closed", Toast.LENGTH_SHORT).show();
            }
        }


    }



    @Override
    protected void onStop() {
        super.onStop();
        //if (broadcastReceiver.)
        unregisterReceiver(broadcastReceiver);
        if ( serialPort != null )
            if (serialPort.isOpen()){
                serialPort.write(ringNo.getBytes());
                serialPort.write(motionOff.getBytes());
                serialPort.close();
            }


    }

    public void onClickSend(View view) {
        //serialPort.write(r1.getBytes());
        serialPort.write(motionOn.getBytes());
        tvAppend(textView, "\nData Sent \n");

    }


        private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }
    private void showRedSnackbar(String warningMessage, final int level){
        ChocoBar.builder().setActivity(MainActivity.this)
                .red()
                .setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE)
                .setAction(R.string.retry, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        switch (level){
                            case LEVEL_SERIAL_CONNECTION_CLOSED :
                                startUsbConnection();
                                break;
                            case LEVEL_PORT_NOT_OPEN:
                                break;
                            case LEVEL_PORT_IS_NULL:
                                break;
                            case LEVEL_PERMISSION_NOT_GRANTED:
                                break;


                        }
                    }
                })
                .setText(warningMessage)
                .show();
    }
    private void showGreenSnackbar(String message){
        ChocoBar.builder().setActivity(MainActivity.this)
                .green()
                .setDuration(BaseTransientBottomBar.LENGTH_SHORT)
                .setText(message)
                .show();
    }
}