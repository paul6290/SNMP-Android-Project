package com.example.jang_yongsu.snmp;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.jang_yongsu.snmp.asn1_java.BER;


public class MainActivity extends AppCompatActivity {

    public static final int UDP_RECEIVE = 0;

    public static final int GET = 0;
    public static final int SET = 1;
    public static final int WALK = 2;

    private SNMPManager snmp;
    private UDPManager udp;

    private int workState = WALK;

    EditText ipAddrEDT;
    EditText portEDT;

    EditText oidEDT;
    EditText valueEDT;

    Button setBTN;
    Button getBTN;
    Button walkBTN;

    TextView getsetResultTXV;
    TextView walkResultTXV;

    ScrollView scrollView;
    Spinner typeSpinner;

    String[] spinner_types = new String[]{
            "Integer", "String", "OID", "IPAddress", "Counter", "Gauge", "TimeTicks", "Opaque"
    };

    byte[] types = new byte[]{
            BER.INTEGER, BER.OCTETSTRING, BER.OID, BER.IPADDRESS, BER.COUNTER, BER.GAUGE, BER.TIMETICKS, BER.OPAQUE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ipAddrEDT = (EditText)findViewById(R.id.ipAddrEDT);
        portEDT = (EditText)findViewById(R.id.portEDT);
        oidEDT = (EditText)findViewById(R.id.oidEDT);
        valueEDT = (EditText)findViewById(R.id.valueEDT);
        setBTN = (Button)findViewById(R.id.setBTN);
        getBTN = (Button)findViewById(R.id.getBTN);
        walkBTN = (Button)findViewById(R.id.walkBTN);
        getsetResultTXV = (TextView)findViewById(R.id.resultTXV);
        walkResultTXV = (TextView)findViewById(R.id.walkResultTXV);
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        scrollView.fullScroll(View.FOCUS_DOWN);
        typeSpinner = (Spinner)findViewById(R.id.typeSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.spinner_item, spinner_types);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        typeSpinner.setAdapter(adapter);
        typeSpinner.setSelection(0);


        snmp = new SNMPManager();
        udp = new UDPManager(UDPHandler,"kuwiden.iptime.org", 11161);

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
                    walkResultTXV.append(results[0]+ " ["+results[2]+"] " + results[1]+'\n');
                }else{
                    String resultSTR = "[OID] " + results[0] + "\n" + "[VALUE] " + results[1] + " ("+results[2]+")";
                    getsetResultTXV.setText("");
                    getsetResultTXV.setText(resultSTR);
                }
            }
        }
    };

    public void setClick(View v){
        if(oidEDT.getText().toString().equals("") || valueEDT.getText().toString().equals("")){
            return;
        }
        workState = SET;
        snmpSET(oidEDT.getText().toString(), types[typeSpinner.getSelectedItemPosition()], valueEDT.getText().toString());
    }

    public void getClick(View v){
        if(oidEDT.getText().toString().equals("")){
            return;
        }
        workState = GET;
        snmpGET(oidEDT.getText().toString());
    }

    public void walkClick(View v){
        workState = WALK;
        walkResultTXV.setText("");
        snmpWALK("1.3.6.1.2.1");
    }

    public void udpSetClick(View v){
        udp = new UDPManager(UDPHandler,ipAddrEDT.getText().toString(), Integer.valueOf(portEDT.getText().toString()));
    }
}
