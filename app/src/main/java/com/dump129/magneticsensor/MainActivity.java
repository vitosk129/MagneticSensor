package com.dump129.magneticsensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;

import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    @Bind(R.id.tvHeading)
    TextView tvHeading;
    @Bind(R.id.btnSend)
    Button btnSend;
    @Bind(R.id.btnDisconnect)
    Button btnDisconnect;
    @Bind(R.id.btnConnect)
    Button btnConnect;

    private AbstractXMPPConnection connection;

    private static final String hostGeny = "10.0.3.2";
    private static final String host = "192.168.10.21";

    // Sensor
    private SensorManager sensorManager;
    private Sensor magnetometer;

    private boolean bBtnSendClick = false;

    float degree;

    Message message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        sensorInitialize();
    }

    private void sensorInitialize() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener((SensorEventListener) this,
                magnetometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        sensorManager.unregisterListener((SensorEventListener) this, magnetometer);
    }

    @OnClick(R.id.btnConnect)
    public void btnConnectClick() {
        new XmppTask().execute();
        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.btnSend)
    public void btnSendClick() {

        if (connection != null) {
            if (connection.isConnected()) {
                bBtnSendClick = true;
                message = new Message();
                message.setType(Message.Type.chat);
                message.setTo("alice@pakgon");
                message.setBody(Float.toString(degree));
                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                Chat chat = chatManager.createChat("bob@pakgon");
                try {
                    chat.sendMessage(message);
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }

                Log.d("Message", "" + message.getBody().toString());
            }
        } else {
            Toast.makeText(getApplicationContext(), "Please Press Connect Button", Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.btnDisconnect)
    public void btnDisconnectClick() {
        if (connection != null) {
            if (connection.isConnected()) {
                connection.disconnect();
                if (bBtnSendClick) {
                    bBtnSendClick = false;
                    Log.d("XMPPDisConnect", "Disconnected");
                    Toast.makeText(getApplicationContext(), "Disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        degree = Math.round(event.values[0]);

        if (connection != null) {
            if (connection.isConnected()) {
                if (bBtnSendClick) {
                    btnSendClick();
                }
            }
        }

        // Magnetic
        tvHeading.setText("Heading: " + Float.toString(degree) + " degress");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes
    }

    private class XmppTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
                    .setUsernameAndPassword("alice", "password")
                    .setHost(host)
                    .setServiceName("pakgon")
                    .setPort(5222)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setDebuggerEnabled(true)
                    .build();

            connection = new XMPPTCPConnection(config);
            try {
                connection.connect();
                connection.login();

                Presence presence = new Presence(Presence.Type.available);
                presence.setStatus("Ready");
                connection.sendStanza(presence);

                Log.d("XMPPConnect", "Connected");
            } catch (XMPPException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SmackException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}