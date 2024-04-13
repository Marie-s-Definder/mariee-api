package shu.scie.mariee.service;

import shu.scie.mariee.model.HkIpc;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient {

    private Socket s;
    public TcpClient(HkIpc ipc) {
        if (ipc == null) {
            System.out.println("no robot select!");
        } else {
            String ip = ipc.slide_ip;
            Long port = ipc.slide_port;

            try {
                s = new Socket();
                s.connect(new InetSocketAddress(ip,port.intValue()));
                System.out.println("connected to " + ip);

            } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
            // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }


    public void gotoPresetPoint(int id){
        System.out.println("预置点：" + id);
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x07;
        b[4] = (byte) 0x00;
        b[5] = (byte) id;
        if (id == 2) {
            b[6] = (byte) 0x0a;
        }else if (id == 3){
            b[6] = (byte) 0x0b;
        } else if(id == 1) {
            b[6] = (byte) 0x09;
        } else {
            b[6] = (byte) 0x0c;
        }
        //q前面值相加对256取余数，校驗
        int sum=0;
        for(int i=1;i<6;i++){
            sum=sum+b[i];
        }
        int y=sum%256;

        b[6] = (byte) y;
        try {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataInputStream ips = new DataInputStream(bis);

            out.write(b);
            out.flush();

            byte[] bytes = new byte[1]; // 一次读取一个byte
            String ret = "";
            while (ips.read(bytes) != -1) {
                ret = bytesToHexString(bytes) + " ";
                if (ret.startsWith("1a")) { //一个请求
                    System.out.println(s.getRemoteSocketAddress() + ":" + ret);
                    break;
                }
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void left() {
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x09;
        b[4] = (byte) 0x00;
        b[5] = (byte) 0x01;
        b[6] = (byte) 0x0b;
        request(b);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("Thread is interrupted while sleeping! Exiting.");
        }
        // stop
        b[3] = (byte) 0x0b;
        b[6] = (byte) 0x0d;
        request(b);
    }

    public void right() {
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x09;
        b[4] = (byte) 0x00;
        b[5] = (byte) 0x02;
        b[6] = (byte) 0x0c;
        request(b);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            System.out.println("Thread is interrupted while sleeping! Exiting.");
        }
        // stop
        b[3] = (byte) 0x0b;
        b[6] = (byte) 0x0e;
        request(b);
    }

    private void request(byte[] b) {
        int sum=0;
        for(int i=1;i<6;i++){
            sum=sum+b[i];
        }
        int y=sum%256;
        b[6] = (byte) y;
        try {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            out.write(b);
            out.flush();
        }catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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