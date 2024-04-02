package shu.scie.mariee.service;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient {
    public static void main(String[] args) throws UnknownHostException, IOException {
        Socket s;
        try {
            s = new Socket();

            s.connect(new InetSocketAddress("172.16.104.2",4196));

            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataInputStream ips = new DataInputStream(bis);
//            InputStreamReader ipsr = new InputStreamReader(ips);
//            BufferedReader br = new BufferedReader(ipsr);

            byte[] b = getCommandByDegrees(100);
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
//            String a = "";
//            while((a = br.readLine()) != null)
//                System.out.println(bytesToHexString(a));


            out.close();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static byte[] getCommandByDegrees(int du){
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0x0B;
        b[4] = (byte) 0x00;//0x17
        b[5] = (byte) 0x02;//0xDB
        b[6] = (byte) 0x0e;
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
