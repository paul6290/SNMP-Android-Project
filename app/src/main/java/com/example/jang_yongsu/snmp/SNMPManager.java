package com.example.jang_yongsu.snmp;

import android.Manifest;
import android.util.Log;

import com.example.jang_yongsu.snmp.asn1_java.BER;
import com.example.jang_yongsu.snmp.asn1_java.BERInputStream;
import com.example.jang_yongsu.snmp.asn1_java.BEROutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public class SNMPManager {

    //REQUEST NUMBER
    private int request_num = 0;

    //PDU TYPE
    public static final byte GETREQUEST = (byte)0xA0;
    public static final byte GETNEXTREQUEST = (byte)0xA1;
    public static final byte SETREQUEST = (byte)0xA3;

    //FIEXD OPTION
    private byte[] versionBER;
    private byte[] community_BER_public;
    private byte[] community_BER_write;
    private byte[] errorStatus_BER;
    private byte[] errorIndex_BER;

    //생성자
    public SNMPManager(){
        BEROutputStream os_ver = new BEROutputStream(ByteBuffer.allocate(30));
        BEROutputStream os_com_public = new BEROutputStream(ByteBuffer.allocate(30));
        BEROutputStream os_com_write = new BEROutputStream(ByteBuffer.allocate(30));
        BEROutputStream os_errStatus = new BEROutputStream(ByteBuffer.allocate(30));
        BEROutputStream os_errIndex = new BEROutputStream(ByteBuffer.allocate(30));
        try{
            BER.encodeInteger(os_ver, BER.INTEGER, 1);
            BER.encodeString(os_com_public, BER.OCTETSTRING, "public".getBytes());
            BER.encodeString(os_com_write, BER.OCTETSTRING, "write".getBytes());
            BER.encodeInteger(os_errStatus, BER.INTEGER, 0);
            BER.encodeInteger(os_errIndex, BER.INTEGER, 0);
        }catch(IOException ex) {
            ex.printStackTrace();
        }

        versionBER = bufferTobytes(os_ver.getBuffer());
        community_BER_public = bufferTobytes(os_com_public.getBuffer());
        community_BER_write = bufferTobytes(os_com_write.getBuffer());
        errorStatus_BER = bufferTobytes(os_errStatus.getBuffer());
        errorIndex_BER = bufferTobytes(os_errIndex.getBuffer());
    }

    //Request ID BER 생성
    private byte[] get_RequestIdBER(){
        BEROutputStream os = new BEROutputStream(ByteBuffer.allocate(20));
        try{
            BER.encodeInteger(os, BER.INTEGER, request_num);
        }catch(IOException ex){
            ex.printStackTrace();
        }
        byte[] reqIdBER = bufferTobytes(os.getBuffer());
        return reqIdBER;
    }

    //요청 Item BER
    private byte[] get_RequestItemBER(int[] oid, byte work, byte type, Object value){
        BEROutputStream os_oid = new BEROutputStream(ByteBuffer.allocate(99));
        BEROutputStream os_value = new BEROutputStream(ByteBuffer.allocate(99));
        BEROutputStream os_item = new BEROutputStream(ByteBuffer.allocate(150));
        try{
            BER.encodeOID(os_oid, BER.OID, oid);
            if(work == SETREQUEST){
                //BER.encodeInteger(os_value, BER.INTEGER, value);
                switch (type){
                    case BER.INTEGER:
                        BER.encodeInteger(os_value, BER.INTEGER, Integer.valueOf((String)value));
                        break;
                    case BER.OCTETSTRING:
//                        Log.i("INFO","write new String");
                        BER.encodeString(os_value, BER.OCTETSTRING, ((String)value).getBytes());
                        break;
                    case BER.OID:
                        String[] strs = ((String)value).split("//.");
                        int[] values = new int[strs.length];
                        for(int i=0; i<strs.length; i++){
                            values[i] = Integer.valueOf(strs[i]);
                        }
                        BER.encodeOID(os_value, BER.OID, values);
                        break;
                    case BER.GAUGE:
                        BER.encodeUnsignedInteger(os_value, BER.INTEGER, Integer.valueOf((String)value));
                        break;
                    case BER.COUNTER:
                        BER.encodeUnsignedInteger(os_value, BER.INTEGER, Integer.valueOf((String)value));
                        break;
                    case BER.TIMETICKS:
                        BER.encodeUnsignedInteger(os_value, BER.INTEGER, Integer.valueOf((String)value));
                        break;
                    default:
                        break;
                }
            }else{
                os_value.write(BER.NULL);
                os_value.write(0x00);
            }

        }catch(IOException ex){
            ex.printStackTrace();
        }

        byte[] bytes_oid = bufferTobytes(os_oid.getBuffer());
        byte[] bytes_value = bufferTobytes(os_value.getBuffer());

        try{
            //1개짜리 item 만 보낼것이기 때문에 미리 감싸는 것이다
            BER.encodeSequence(os_item, BER.SEQUENCE, os_oid.getBuffer().position()+os_value.getBuffer().position()+2);
            BER.encodeSequence(os_item, BER.SEQUENCE, os_oid.getBuffer().position()+os_value.getBuffer().position());

            os_item.write(bytes_oid);
            os_item.write(bytes_value);

        }catch (IOException ex){
            ex.printStackTrace();
        }

        byte[] bytes_item = bufferTobytes(os_item.getBuffer());
        return bytes_item;
    }

    private byte[] get_RequestBERDATA(int[] oid, byte work, byte type, Object value){
        byte[] reqID = get_RequestIdBER();
        byte[] item = get_RequestItemBER(oid, work, type, value);
        BEROutputStream os = new BEROutputStream(ByteBuffer.allocate(999));

        try{
            os.write(work);
            os.write(reqID.length+errorStatus_BER.length+errorIndex_BER.length+item.length);
            os.write(reqID);
            os.write(errorStatus_BER);
            os.write(errorIndex_BER);
            os.write(item);
        }catch (IOException ex){
            ex.printStackTrace();
        }

        byte[] result = bufferTobytes(os.getBuffer());
        return result;
    }


    public byte[] buildRequestPacket(String oidStr, byte work, byte type, Object value){
        String[] oidStrs = oidStr.split("\\.");
        int[] oid = new int[oidStrs.length];

        for(int i=0; i<oidStrs.length; i++){
            oid[i] = Integer.valueOf(oidStrs[i]);
        }

        byte[] requestData = get_RequestBERDATA(oid, work, type, value);

        BEROutputStream os = new BEROutputStream(ByteBuffer.allocate(999));

        try{
            if(work == SETREQUEST){
                BER.encodeSequence(os, BER.SEQUENCE, versionBER.length+community_BER_write.length+requestData.length);
                os.write(versionBER);
                os.write(community_BER_write);
            }else{
                BER.encodeSequence(os, BER.SEQUENCE, versionBER.length+community_BER_public.length+requestData.length);
                os.write(versionBER);
                os.write(community_BER_public);
            }
            os.write(requestData);
        }catch(IOException ex){
            ex.printStackTrace();
        }

        byte[] result = bufferTobytes(os.getBuffer());

        addRequestNumber();

        Log.i("INFO", byteArrayToHex(result));
        return result;
    }




    //decode part
    public String[] parseReceiveMessage(byte[] message){

        String[] results = new String[3]; // 0 -> oid 1 -> value 2 -> type
        BERInputStream is = new BERInputStream(ByteBuffer.wrap(message));
        BER.MutableByte mutableByte = new BER.MutableByte();

        try{
            BER.decodeHeader(is, mutableByte);
            BER.decodeInteger(is, mutableByte);
            BER.decodeString(is, mutableByte);
            BER.decodeHeader(is, mutableByte);
            BER.decodeInteger(is, mutableByte);
            //error-status
            int error = BER.decodeInteger(is, mutableByte);
            if(error != 0x00){
                results[1] = "";
                results[2] = "ERROR";
                return results;
            }
            //error-index
            BER.decodeInteger(is, mutableByte);
            BER.decodeHeader(is, mutableByte);
            BER.decodeHeader(is, mutableByte);
            int[] key = BER.decodeOID(is, mutableByte);
            results[0] = arrayToString(key, key.length); // oid save
            int dataType = (int)is.getBuffer().get(is.getBuffer().position());

            switch (dataType){
                case 0x02: //INTEGER
                    int int_value = BER.decodeInteger(is, mutableByte);
                    results[1] = String.valueOf(int_value);
                    results[2] = "INTEGER";
                    break;
                case 0x04: //OCTET STRING
                    byte[] strBytes = BER.decodeString(is, mutableByte);
                    if(isPrintable(strBytes)){
                        results[1] = String.valueOf(new String(strBytes));
                    }else{
                        results[1] = byteArrayToHex(strBytes);
                    }
                    results[2] = "OCTET STRING";
                    break;
                case 0x06: //OBJECT IDENTIFIER
                    int[] objInts = BER.decodeOID(is, mutableByte);
                    results[1] = arrayToString(objInts, objInts.length);
                    results[2] = "OBJECT IDENTIFIER";
                    break;
                case 0x05: //NULL
                    results[1] = "";
                    results[2] = "NULL";
                    break;
                case 0x30: //SEQUENCE, SEQUENCE OF
                    break;
                case 0x40: //IPAddress
                    results[1] = "";
                    results[2] = "IPAddress";
                    break;
                case 0x41: //Counter
                    results[1] = String.valueOf(BER.decodeUnsignedInteger(is, mutableByte));
                    results[2] = "Counter";
                    break;
                case 0x42: //Gauge
                    results[1] = String.valueOf(BER.decodeUnsignedInteger(is, mutableByte));
                    results[2] = "Gauge";
                    break;
                case 0x43: //TimeTicks
                    results[1] = String.valueOf(BER.decodeUnsignedInteger(is, mutableByte));
                    results[2] = "TimeTicks";
                    break;
                case 0x44: //Opaque
                    results[1] = "";
                    results[2] = "Opaque";
                    break;
                case (byte)BER.ENDOFMIBVIEW:
                    Log.i("info", "0x82");
                    results[1] = "";
                    results[2] = "END";
                    break;
                default:
                    Log.i("info", "default");
                    break;
            }
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return results;
    }



    private void addRequestNumber(){
        this.request_num += 1;
    }

    //보조 함수
    private byte[] bufferTobytes(ByteBuffer byteBuf){
        byte[] bytes = new byte[byteBuf.position()];
        for(int i=0; i<byteBuf.position(); i++){
            bytes[i] = byteBuf.get(i);
        }
        return bytes;
    }

    private String arrayToString(int[] arr, int size){
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<size-1; i++){
            sb.append(String.format("%d.", arr[i]));
        }
        sb.append(String.format("%d", arr[size-1]));
        return sb.toString();
    }

    private boolean isPrintable(byte[] data){
        for(int i=0; i<data.length; i++){
            char c = (char)data[i];
            if((Character.isISOControl(c) || ((c & 0xFF) >= 0X80)) && ((!Character.isWhitespace(c)) ||
                    (((c&0xFF) >= 0x1C) && ((c & 0xFF) <= 0x1F)))){
                return false;
            }
        }
        return true;
    }

    private String byteArrayToHex(byte[] a) {
        StringBuilder sb = new StringBuilder();
        for(final byte b: a)
            sb.append(String.format("%02x ", b&0xff));
        return sb.toString();
    }
}
