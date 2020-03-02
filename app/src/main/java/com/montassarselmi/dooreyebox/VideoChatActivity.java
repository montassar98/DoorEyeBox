package com.montassarselmi.dooreyebox;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.opentok.android.OpentokError;
import com.opentok.android.Publisher;
import com.opentok.android.PublisherKit;
import com.opentok.android.Session;

import android.util.Log;
import android.widget.FrameLayout;

import com.opentok.android.Stream;
import com.opentok.android.Subscriber;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class VideoChatActivity extends AppCompatActivity implements Session.SessionListener, PublisherKit.PublisherListener {

    private static String API_KEY = "46525032";
    private static String SESSION_ID = "2_MX40NjUyNTAzMn5-MTU4Mjk4MjgzODk1M35SdEVoQ2VSdnNraHZRRzdyUEZURkhvVW5-fg²";
    private static String TOKEN = "T1==cGFydG5lcl9pZD00NjUyNTAzMiZzaWc9YzdmYjUzZGY0MzM3YzYxYmJmYTg0NDFhYzEwMjBkYTk0NzkzM2I1NDpzZXNzaW9uX2lkPTJfTVg0ME5qVXlOVEF6TW41LU1UVTRNams0TWpnek9EazFNMzVTZEVWb1EyVlNkbk5yYUhaUlJ6ZHlVRVpVUmtodlZXNS1mZyZjcmVhdGVfdGltZT0xNTgyOTgyOTI3Jm5vbmNlPTAuMjU2MTQ0NjA2MTczODAxNSZyb2xlPXB1Ymxpc2hlciZleHBpcmVfdGltZT0xNTg1NTcxMzI3JmluaXRpYWxfbGF5b3V0X2NsYXNzX2xpc3Q9";
    private static final String TAG = VideoChatActivity.class.getSimpleName();
    private static final int RC_SETTINGS_SCREEN_PERM = 123;
    private static final int RC_VIDEO_APP_PERM = 124;
    private FrameLayout mSubscriberViewController;
    private FrameLayout mPublisherViewController;
    private Session session;
    private Publisher publisher;
    private Subscriber subscriber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_chat);

        requestPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults,VideoChatActivity.this);
    }


    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private void requestPermissions()
    {
        String[] perms = {Manifest.permission.INTERNET,Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        if (EasyPermissions.hasPermissions(this, perms)) {
            // initialize view objects from your layout
            mSubscriberViewController = (FrameLayout) findViewById(R.id.subscriber_container);
            mPublisherViewController = (FrameLayout) findViewById(R.id.publisher_container);

            // initialize and connect to the session
            session = new Session.Builder(this, API_KEY, SESSION_ID).build();
            session.setSessionListener(VideoChatActivity.this);
            session.connect(TOKEN);


        } else {
            EasyPermissions.requestPermissions(this, getResources().getString(R.string.permissions_not_granted),
                    RC_VIDEO_APP_PERM, perms);
        }
    }

    @Override
    public void onConnected(Session session) {
        Log.d(TAG, "onConnected: ");
        publisher = new Publisher.Builder(this).build();
        publisher.setPublisherListener(VideoChatActivity.this);
        mPublisherViewController.addView(publisher.getView());
        if (publisher.getView() instanceof GLSurfaceView)
        {
            ((GLSurfaceView) publisher.getView()).setZOrderOnTop(true);
        }
        session.publish(publisher);
    }

    @Override
    public void onDisconnected(Session session) {

    }

    @Override
    public void onStreamReceived(Session session, Stream stream) {
        if (subscriber == null)
        {
            subscriber = new Subscriber.Builder(this,stream).build();
            session.subscribe(subscriber);
            mSubscriberViewController.addView(subscriber.getView());
        }
    }

    @Override
    public void onStreamDropped(Session session, Stream stream) {
        Log.d(TAG, "onStreamDropped: ");
        if (subscriber != null)
        {
            subscriber = null;
            mSubscriberViewController.removeAllViews();
        }
    }

    @Override
    public void onError(Session session, OpentokError opentokError) {

    }

    @Override
    public void onStreamCreated(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onStreamDestroyed(PublisherKit publisherKit, Stream stream) {

    }

    @Override
    public void onError(PublisherKit publisherKit, OpentokError opentokError) {

    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
