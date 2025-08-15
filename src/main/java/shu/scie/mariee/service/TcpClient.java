package shu.scie.mariee.service;

import shu.scie.mariee.model.HkIpc;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class TcpClient {

    private Socket s;
    private HkIpc gloIpc;
    public TcpClient(HkIpc ipc) {
        gloIpc = ipc;
    }
    public void getSocket(HkIpc ipc){
        // Socket s = null;
        if (ipc == null) {
            System.out.println("no robot select!");
        }else {
            String ip = ipc.slide_ip;
            Long port = ipc.slide_port;

            try {
                s = new Socket();
                s.connect(new InetSocketAddress(ip,port.intValue()), 10000);
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

//    public static void main(String[] args) throws UnknownHostException, IOException {
//        for(byte i:gotoPresetPoint(2)){
//            byte[] c = new byte[1];
//            c[0] = i;
//            System.out.println(i);
//            System.out.println(bytesToHexString(c));
//        }
//        Socket s;
//        try {
//            s = new Socket();
//
//            s.connect(new InetSocketAddress("172.16.104.2",4196));
//
//            DataOutputStream out = new DataOutputStream(s.getOutputStream());
//
//            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
//            DataInputStream ips = new DataInputStream(bis);
//
//
//            byte[] b = gotoPresetPoint(100);
//            out.write(b);
//            out.flush();
//
//
//            byte[] bytes = new byte[1]; // 一次读取一个byte
//            String ret = "";
//            while (ips.read(bytes) != -1) {
//                ret += bytesToHexString(bytes) + " ";
//                if (ips.available() == 0) { //一个请求
//                    System.out.println(s.getRemoteSocketAddress() + ":" + ret);
//                    ret = "";
//                }
//            }
//
//
//
//            out.close();
//        } catch (UnknownHostException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//    }


    public void gotoPresetPoint(int id){
//        System.out.println("gotoPresetPoint的id"+id);
//        TempPreset.need_on.put(String.valueOf(id), "ON");
//        new Thread(){
//            @Override
//            public void run(){
//                try {
//                    Thread.sleep(10000);//延迟10秒
//                }catch (InterruptedException e){
//                    e.printStackTrace();
//                }finally {
//                    TempPreset.need_on.put(String.valueOf(id), "OFF");
//                }
//
//            }
//        }.start();
        if( s == null || s.isClosed() || !s.isConnected() || !isSocketAlive(s)){
            getSocket(gloIpc);
        }
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

        byte[] c = new byte[7];
        c[0] = (byte) 0xff;
        c[1] = (byte) 0x1a;
        c[2] = (byte) 0x00;
        c[3] = (byte) 0x07;
        c[4] = (byte) 0x00;
        c[5] = (byte) id;
        //q前面值相加对256取余数，校驗
        sum=0;
        for(int i=2;i<6;i++){
            sum=sum+c[i];
        }
        y=(sum%256)+1;
        c[6] = (byte) y;
        String cString= bytesToHexString(c);
        DataOutputStream out;
        try {
            out = new DataOutputStream(s.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataInputStream ips = new DataInputStream(bis);

            out.write(b);
            out.flush();
            System.out.println("命令已发送");

            byte[] bytes = new byte[1]; // 一次读取一个byte
            String ret = "";
            StringBuilder ensure = new StringBuilder();

            long startTime = System.currentTimeMillis();
            while (true) {
                if (ips.read(bytes) != -1) {
                    ret = bytesToHexString(bytes);
                    ensure.append(ret);
                    if (ensure.length() == 14) {
                        if (ensure.toString().equals(cString)) {
                            System.out.println(ensure.toString());
                            break;
                        }
                        ensure.delete(0, 14);
                    }
                }
                long currentTime = System.currentTimeMillis(); // 获取当前时间
                if ((currentTime - startTime) / 1000 >= 1200) { // 检查是否超过20分钟
                    System.out.println("超时退出");
                    break;
                }
            }
        }catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            System.out.println("命令发送失败，重连中...");
            getSocket(gloIpc);
            try {
                out = new DataOutputStream(s.getOutputStream());
                out.write(b);
                out.flush();
            } catch (IOException e1) {
                // 记录错误日志
                System.err.println("发送数据失败: " + e.getMessage());
                e1.printStackTrace();

                // 这里可以选择关闭连接或重试
                try {
                    if (s != null && !s.isClosed()) {
                        s.close();
                    }
                } catch (IOException closeEx) {
                    System.err.println("关闭Socket失败: " + closeEx.getMessage());
                }
            }
        }finally {

            new Thread(){
                @Override
                public void run(){
                    try {
                        Thread.sleep(5000);
                        TempPreset.need_on.put(String.valueOf(id), "ON");
                        Thread.sleep(30000);//延迟30秒
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }finally {
//                        System.out.println(TempPreset.need_on);
                        TempPreset.need_on.put(String.valueOf(id), "OFF");
//                        System.out.println(TempPreset.need_on);
                    }

                }
            }.start();
//            System.out.println(TempPreset.need_on);
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
        if( s == null || s.isClosed() || !s.isConnected() || !isSocketAlive(s)){
            getSocket(gloIpc);
        }
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

    private boolean isSocketAlive(Socket s) {
        try {
            s.sendUrgentData(0xFF); // 发送一个 OOB 字节测试
            return true;
        } catch (IOException e) {
            return false;
        }
    }

}
