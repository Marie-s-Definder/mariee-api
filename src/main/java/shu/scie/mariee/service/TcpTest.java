package shu.scie.mariee.service;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class TcpTest {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("172.16.104.2", 4196);
            DataInputStream is = new DataInputStream(socket.getInputStream());
            DataOutputStream os = new DataOutputStream(socket.getOutputStream());
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    // 发送信息
                    try {
                        byte[] b = getCommandByDegrees(100);
                        os.write(b);
                        os.flush();
                    } catch (Exception e) {
                    }

                }
            };

            Runnable r2 = new Runnable() {
                @Override
                public void run() {
                    // 接受发送的信息
                    while (true) {
                        String str;
                        try {
                            str = br.readLine();
                            System.out.println("接受者receiver:" + str);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            Thread t1 = new Thread(r);
            Thread t2 = new Thread(r2);
            t1.start();
            t2.start();

        } catch (UnknownHostException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    public static byte[] getCommandByDegrees(int du){
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0xEB;
        b[4] = (byte) 0x00;//0x17
        b[5] = (byte) 0x00;//0xDB
        b[6] = (byte) 0xec;
        //byte[] b = BitConverter.GetBytes(
        //   0xba5eba11 );

//        String str=Integer.toHexString(du*100);
//        System.out.println(du);
//        System.out.println(Integer.toHexString(du));
//        int  s4=Integer.valueOf(str.substring(0, 2));
//        int  s5=Integer.valueOf(str.substring(2, 4));
//        System.out.println();
//        b[4] = (byte)s4;
//        b[5] = (byte)s5;

        //q前面值相加对256取余数，校驗
        int sum=0;
        for(int i=1;i<6;i++){
            sum=sum+b[i];
        }
        int y=sum%256;
        //System.out.println(y+"--"+Integer.valueOf("3E",16));
//        int s6= Integer.valueOf(Integer.toHexString(y));
        b[6] = (byte) y;
        System.out.println(sum);
        return b;
    }

}
