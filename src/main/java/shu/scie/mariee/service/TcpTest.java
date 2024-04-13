package shu.scie.mariee.service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpTest {
    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket s;
        try {
            s = new Socket();

            s.connect(new InetSocketAddress("172.16.104.2",4196));

            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataInputStream ips = new DataInputStream(bis);

            byte[] b = getCommandByDegrees();
            out.write(b);
            out.flush();


            byte[] bytes = new byte[1]; // 一次读取一个byte
            String ret = "";
            while (ips.read(bytes) != -1) {
                ret += bytesToHexString(bytes) + " ";
                if (ips.available() == 0) { //一个请求
                    System.out.println(s.getRemoteSocketAddress() + ":" + ret);
                    ret = "";
                }
            }

            out.close();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static byte[] getCommandByDegrees(){
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x03;
        b[4] = (byte) 0x00;//0x17
        b[5] = (byte) 0x01;//0xDB
        b[6] = (byte) 0x05;


        //q前面值相加对256取余数，校驗
        int sum=0;
        for(int i=1;i<6;i++){
            sum=sum+b[i];
        }
        int y=sum%256;

        b[6] = (byte) y;
        System.out.println(sum);
        return b;
    }
    public static String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

}