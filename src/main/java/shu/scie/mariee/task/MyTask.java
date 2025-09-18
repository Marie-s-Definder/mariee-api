package shu.scie.mariee.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import shu.scie.mariee.model.HkIpc;
import shu.scie.mariee.service.HkIpcService;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import static shu.scie.mariee.service.TcpClient.bytesToHexString;

@Component
public class MyTask {
    private final HkIpcService hkIpcService;

    /** 复用同一条长连接 */
    private volatile Socket s;
    /** 复用流，避免每次 new 包装器导致泄露（可空） */
    private volatile DataOutputStream out;
    private volatile DataInputStream in;

    /** 防止 @Scheduled 并发/重入写同一 Socket */
    private final AtomicBoolean running = new AtomicBoolean(false);

    // 连接/读超时配置
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 3000;

    public MyTask(HkIpcService hkIpcService) {
        this.hkIpcService = hkIpcService;
    }

    /** 关闭工具 */
    private static void closeQuietly(Closeable c) {
        if (c != null) try { c.close(); } catch (IOException ignored) {}
    }
    private static void closeSocket(Socket s) {
        if (s != null) try { s.close(); } catch (IOException ignored) {}
    }

    /** 断线自愈：若当前连接不可用则重建 */
    private void ensureConnected(HkIpc ipc) throws IOException {
        if (ipc == null) throw new IOException("no robot select!");

        if (s != null && s.isConnected() && !s.isClosed()) return;

        // 清理旧资源
        closeQuietly(in);
        closeQuietly(out);
        closeSocket(s);
        in = null; out = null; s = null;

        String ip = ipc.slide_ip;
        int port = ipc.slide_port.intValue();

        Socket ns = new Socket();
        ns.setKeepAlive(true);      // TCP keepalive（内核级）
        ns.setTcpNoDelay(true);     // 减少 Nagle 影响（小包心跳更及时）
        ns.connect(new InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS);
        ns.setSoTimeout(READ_TIMEOUT_MS); // read() 超时，避免卡死

        // 仅创建一次，后续复用
        DataOutputStream o = new DataOutputStream(ns.getOutputStream());
        DataInputStream i = new DataInputStream(new BufferedInputStream(ns.getInputStream()));

        this.s = ns;
        this.out = o;
        this.in = i;

        System.out.println("connected to " + ip);
    }

    /** 组帧 & 发送一次心跳，并做最小应答探测 */
    private void sendHeartbeatOnce() throws IOException {
        // 原有 7 字节心跳帧
        byte[] b = new byte[7];
        b[0] = (byte) 0xff;
        b[1] = (byte) 0x01;
        b[2] = (byte) 0x00;
        b[3] = (byte) 0xeb;
        b[4] = (byte) 0x00;
        b[5] = (byte) 0x00;
        // 校验
        int sum = 0;
        for (int i = 1; i < 6; i++) sum += (b[i] & 0xFF);
        b[6] = (byte) (sum & 0xFF);

        // --- 发送 ---
        out.write(b);
        out.flush();
        System.out.println("指令 " + bytesToHexString(b) + " 已发送，等待设备响应...");

        // --- 应答探测 ---
        byte[] buf = new byte[16]; // 可以多读几个字节
        int n = in.read(buf); // READ_TIMEOUT_MS 生效
        if (n == -1) {
            System.out.println("未收到应答，告警提示！");
            throw new IOException("Peer closed (EOF)");
        } else {
            // 打印收到的应答
            byte[] resp = new byte[n];
            System.arraycopy(buf, 0, resp, 0, n);
            System.out.println("收到应答: " + bytesToHexString(resp));
        }
    }

    /**
     * 每分钟的第 0 秒触发；用 running 防止重入
     * 也可改为 fixedDelayString="60000" 来从上次结束再延迟 60s 执行
     */
    @Scheduled(cron = "0 * * * * ?")
    public void heartBeat() {
        if (!running.compareAndSet(false, true)) {
            // 上一次还没跑完，避免重入并发写
            return;
        }
        try {
            System.out.println(new Date() + " 发送心跳包");

            HkIpc ipc = this.hkIpcService.getById(1L);
            ensureConnected(ipc);         // 不可用就重连
            sendHeartbeatOnce();          // 发送 + 探测

        } catch (IOException e) {
            // 任何 I/O 异常：打印简要日志并清理，留待下次自动重连
            System.out.println("Heartbeat failed: " + e);
            closeQuietly(in);
            closeQuietly(out);
            closeSocket(s);
            in = null; out = null; s = null;
        } finally {
            running.set(false);
        }
    }
}
