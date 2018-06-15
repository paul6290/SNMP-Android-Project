package com.example.jang_yongsu.snmp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.example.jang_yongsu.snmp.asn1_java.BER;
import com.example.jang_yongsu.snmp.asn1_java.BEROutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public class MainActivity extends AppCompatActivity {

    public static final int UDP_RECEIVE = 0;


    public static final int GET = 0;
    public static final int SET = 1;
    public static final int WALK = 2;

    private SNMPManager snmp;
    private UDPManager udp;

    private int workState = SET;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        snmp = new SNMPManager();
        udp = new UDPManager(UDPHandler,"kuwiden.iptime.org", 11161);
        //snmpWALK("1.3.6.1.2.1");
        //snmpSET("1.3.6.1.6.3.16.1.5.2.1.3.5.95.97.108.108.95.1.1", BER.OCTETSTRING, "hello");
    }

    private void snmpGET(String oid){
        udp.sendMessage(snmp.buildRequestPacket(oid, SNMPManager.GETREQUEST, (byte)0x00, 0));
    }

    private void snmpSET(String oid, byte type, Object value){
        udp.sendMessage(snmp.buildRequestPacket(oid, SNMPManager.SETREQUEST, type, value));
    }

    private void snmpWALK(String oid){
        udp.sendMessage(snmp.buildRequestPacket(oid, SNMPManager.GETNEXTREQUEST, (byte)0x00, 0));
    }


    private final Handler UDPHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == UDP_RECEIVE){
                byte[] receive = (byte[])msg.obj;
                int size = msg.arg1;

                byte[] message = new byte[size];
                for(int i=0; i<size; i++){
                    message[i] = receive[i];
                }

                StringBuilder sb = new StringBuilder();
                for(int i=0; i<msg.arg1; i++){
                    sb.append(String.format("%02x ", ((byte[])msg.obj)[i]&0xff));
                }
                Log.i("INFO", sb.toString());

                String[] results = snmp.parseReceiveMessage(message);

                Log.i("INFO", results[0] + " " + results[2] + "  " + results[1]);

                if(workState == WALK){
                    snmpWALK(results[0]);
                }
            }
        }
    };
}
