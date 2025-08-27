package shu.scie.mariee.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shu.scie.mariee.model.HkIpc;
import shu.scie.mariee.service.HkIpcService;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import static shu.scie.mariee.service.TcpClient.bytesToHexString;

@Component
public class MyTask {
    private final HkIpcService hkIpcService;
    private Socket s;

    public MyTask(HkIpcService hkIpcService) {
        this.hkIpcService = hkIpcService;
    }

    public void getSocket(HkIpc ipc) {
        if (ipc == null) {
            System.out.println("no robot select!");
        } else {
            String ip = ipc.slide_ip;
            Long port = ipc.slide_port;
            try {
                s = new Socket();
                s.connect(new InetSocketAddress(ip, port.intValue()), 10000);
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

    @Scheduled(cron = "0 * * * * ?")
    public void heartBeat() {
        System.out.println(new Date() + "发送心跳包");
        HkIpc ipc = this.hkIpcService.getById(1L);
        if (s == null || s.isClosed() || !s.isConnected()) {
            getSocket(ipc);
        }
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0xeb;
        b[4] = (byte) 0x00;
        b[5] = (byte) 0x00;
        b[6] = (byte) 0xec;

        //q前面值相加对256取余数，校验
        int sum = 0;
        for (int i = 1; i < 6; i++) {
            sum = sum + b[i];
        }
        int y = sum % 256;

        b[6] = (byte) y;

/*        byte[] c = new byte[7];
        c[0] = (byte) 0xff;
        c[1] = (byte) 0x1a;
        c[2] = (byte) 0x00;
        c[3] = (byte) 0x07;
        c[4] = (byte) 0x00;
        c[5] = (byte) id;
        //q前面值相加对256取余数，校驗
        sum = 0;
        for (int i = 2; i < 6; i++) {
            sum = sum + c[i];
        }
        y = (sum % 256) + 1;
        c[6] = (byte) y;*/
        //String cString = "ff0100dc";

        try {
            DataOutputStream out = new DataOutputStream(s.getOutputStream());
            BufferedInputStream bis = new BufferedInputStream(s.getInputStream());
            DataInputStream ips = new DataInputStream(bis);
            // 发送指令前
            //System.out.println("发送指令: " + bytesToHexString(b));
            // 发送指令后
            out.write(b);
            out.flush();
            System.out.println("指令" + bytesToHexString(b) + "已发送，等待设备响应...");

            byte[] bytes = new byte[1]; // 一次读取一个byte
            String ret = "";
            StringBuilder ensure = new StringBuilder();

            long startTime = System.currentTimeMillis();
            //System.out.println("开始时间：" + startTime);
            // 读取数据时
            while (true) {
                if (ips.read(bytes) != -1) {
                    ret = bytesToHexString(bytes);
                    ensure.append(ret);
                    if (ensure.length() == 8) {
                        System.out.println("设备已响应：" + ensure.toString());
                        ensure.delete(0, 8);
                        break;
                    }
                }
                long currentTime = System.currentTimeMillis(); // 获取当前时间
                if ((currentTime - startTime) / 1000 >= 600) { // 检查是否超过10分钟
                    System.out.println("超时告警");
                    break;
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
