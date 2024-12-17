package src;

import java.io.*;
import java.net.*;

public class Main {
    private static Socket socket;

    // 获取全局 Socket 实例
    public static Socket getSocket() throws IOException {
        // 如果socket为空或已经关闭，则重新连接
        if (socket == null || socket.isClosed()) {
            socket = new Socket("localhost", 39393);
        }
        socket.setSoTimeout(3600000);  //超时为1小时
        return socket;
    }

    // 主动关闭 Socket 的方法
    public static void closeSocket() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            // 获取全局 socket
            socket = getSocket();   // 获取全局socket，这里链接不成功就处理了，后面全throw就好

            InetAddress hostname = InetAddress.getLocalHost();
            String ip = hostname.getHostName();
            System.out.println("客户端： " + ip + " 连接成功");

            // 开启登录，尝试与服务器连接
            new LoginFrame().init(socket);  // 传递全局的 socket 实例
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("客户端连接失败： " + e.getMessage());
        }
    }
}
