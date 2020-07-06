package org.onyx.sps;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.appcompat.*;
import android.support.v7.appcompat.BuildConfig;
import android.util.FloatProperty;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MQTT";
    //fields
    private Button outlets[];
    private Button syncbtn;
    private Button alloff;
    private TextView outputStream;
    private int mem[];
    private MqttAndroidClient client;



    //methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup
        setContentView(R.layout.activity_sync);
        mem = new int[8];
        outlets = new Button[8];
        final String stopic = "topic/sps_out";
        syncbtn = (Button) findViewById(R.id.syncbtn);


        //client mqtt
        String clientId = MqttClient.generateClientId();
        client =
                new MqttAndroidClient(this.getApplicationContext(), "tcp://192.168.42.1",
                        clientId);



        //check connection and subscribe
        try {
            IMqttToken token = client.connect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess");
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "onFailure");
                }
            });
            Log.d(TAG, "con...done");
        } catch (MqttException e) {
            e.printStackTrace();
        }




        //sync
        syncbtn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //move to next activity
                setContentView(R.layout.activity_main);
                outputStream = (TextView) findViewById(R.id.outputStream);
                //subscribe
                subscribe(stopic);
                //publish
                if(0!=requestInfo()){
                    outputStream.setText("connection!");
                }else{
                    outputStream.setText("not connected");
                }
                Log.d(TAG, "sync cent");
                //thread sleep?
                outlets[0] = (Button) findViewById(R.id.one);
                outlets[1] = (Button) findViewById(R.id.two);
                outlets[2] = (Button) findViewById(R.id.three);
                outlets[3] = (Button) findViewById(R.id.four);
                outlets[4] = (Button) findViewById(R.id.five);
                outlets[5] = (Button) findViewById(R.id.six);
                outlets[6] = (Button) findViewById(R.id.seven);
                outlets[7] = (Button) findViewById(R.id.eight);
                //update
                updateBtnz();
                //set listeners for btnz
                btnset();
                alloff = (Button) findViewById(R.id.alloff);
                alloff.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setalloff();
                    }
                });
            }
        });
    }
    //end on create




    //other methodds
    private int requestInfo(){
        String payload = "sync";
        if(client.isConnected()){
            try {
                byte[] encodedPayload;
                encodedPayload = payload.getBytes("UTF-8");
                MqttMessage message = new MqttMessage(encodedPayload);
                client.publish("topic/sps_in", message);
            } catch (UnsupportedEncodingException | MqttException e) {
                e.printStackTrace();
            }
            return 1;
        }
        return 0;
    }

    //get memory values
    private String getflagtext(int i) {
        return (mem[i]==1)?"ON":"OFF";
    }

    //toggle mem values and publish sps toggle cmd
    private String btext(int num){
        if(-1<num&&8>num) {
            mem[num]=(0==mem[num])?1:0;
            return (1==mem[num])?"ON":"OFF";
        }else{
            return "ERR";
        }
    }


    //publish msg to sps
    private void publish_mqtt(int num) {
        String topic = "topic/sps_in";
        String payload="err";
        payload = ""+num;
        byte[] encodedPayload;
        try {
            encodedPayload = payload.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }

    //for sync...triggered on message from sps
    private void parseMqttMessage(String s) {
        int i;
        char msg[] = new char[s.length()];
        if (s.length()!=8){
            //sensor info
            outputStream.setText(s);
        }else{
            for (i=0;i<s.length();i++){
                mem[i]=(s.charAt(i)=='1')?1:0;
            }
        }
    }


    //subscribe to topic
    private void subscribe(String tpc){
        try {
            if (client.isConnected()) {
                client.subscribe(tpc, 0);
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                    }
                    //GET MESSAGE HERE
                    @Override
                    public void messageArrived(String topic, MqttMessage message) throws Exception {
                        outputStream.setText(new String(message.getPayload()));
                        parseMqttMessage(new String(message.getPayload()));
                        updateBtnz();
                    }
                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {

                    }
                });
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }



    //handle btn press
    public void btnhandle(int i){
            outlets[i].setText((i+1)+": " + btext(i));
            publish_mqtt(i);
    }
    //main activity btn setup
    public void btnset(){
        //set listeners
        outlets[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(0);
            }
        });
        outlets[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(1);
            }
        });
        outlets[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(2);
            }
        });
        outlets[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(3);
            }
        });
        outlets[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(4);
            }
        });
        outlets[5].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(5);
            }
        });
        outlets[6].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(6);
            }
        });
        outlets[7].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnhandle(7);
            }
        });
    }
    //set btnz to mem values
    public void updateBtnz(){
        outlets[0].setText("1: "+getflagtext(0));
        outlets[1].setText("2: "+getflagtext(1));
        outlets[2].setText("3: "+getflagtext(2));
        outlets[3].setText("4: "+getflagtext(3));
        outlets[4].setText("5: "+getflagtext(4));
        outlets[5].setText("6: "+getflagtext(5));
        outlets[6].setText("7: "+getflagtext(6));
        outlets[7].setText("8: "+getflagtext(7));
    }
    //set all outlets off
    public void setalloff(){
        int i;
        for(i=0;i<8;i++){
            if(mem[i]==1) {
                publish_mqtt(i);
                mem[i]=0;
            }
        }
        updateBtnz();
    }
    //test methods
    public void memset(){
        int i;
        for (i=0;i<mem.length;i++){
            mem[i]=1;
            if (3==i){
                mem[i]=0;
            }
        }
    }
}