package com.example.jang_yongsu.snmp;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;

public class UDPManager{

    private String TargetAddress;
    private int TargetPort;
    private Handler handler;


    public UDPManager(Handler handler, String address, int port){
        this.handler = handler;
        this.TargetAddress = address;
        this.TargetPort = port;
    }

    public void sendMessage(final byte[] message){

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    InetAddress targetAddr = InetAddress.getByName(TargetAddress);
                    DatagramSocket socket = new DatagramSocket();
                    DatagramPacket packet = new DatagramPacket(message, message.length, targetAddr, TargetPort);
                    socket.send(packet);
                    Log.i("INFO", "Send Success");
                    int receivePort = socket.getPort();
                    byte[] buf = new byte[1024];
                    DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
                    socket.receive(receivePacket);
                    Log.i("INFO", "Receive Success");
                    handler.obtainMessage(MainActivity.UDP_RECEIVE, receivePacket.getLength(), -1, buf).sendToTarget();
                    socket.close();
                }catch(Exception ex){
                    ex.printStackTrace();
                }
            }
        }).start();
    }


}
