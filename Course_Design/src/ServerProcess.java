package src;

import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

public class ServerProcess {
    // 存储所有活跃客户端（线程安全）
    //名字和输出流
    private static ConcurrentHashMap<String, PrintWriter> clientMap = new ConcurrentHashMap<>();
    //文件输出流
    private static ConcurrentHashMap<String, OutputStream> clientFileMap = new ConcurrentHashMap<>();
    //用户个数计数
    private static int clientNumber = 0;

    public static void main(String[] args) {
        new serverFrame().init();

        try {
            ServerSocket serverSocket = new ServerSocket(39393);
            System.out.println("服务器已启动，端口为 39393");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientSocket.setSoTimeout(7200000);  //超时为两小时
                System.out.println("新用户已连接： : " + clientSocket);

                // 给每个客户端创建独立线程
                new chatProcess(clientSocket).start();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    // 会话进程
    private static class chatProcess extends Thread {
        private Socket socket;
        private String clientName;
        private String password;
        private PrintWriter out;
        private BufferedReader in;

        public chatProcess(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                //输出输入流初始化为当前连接client
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                //sql
                PreparedStatement ps = null;
                ResultSet rs = null;

                // 客户端首次连接,发送连接成功，获取用户名和密码
                //防止消息丢失，加载预留5秒
                for (int i = 0; i < 5; i++) {
                    Thread.currentThread().sleep(1000);
                    out.println("连接成功");
                }

                //比对,等待登录
                while (true) {
                    clientName = in.readLine();
                    if (clientName.startsWith("#")) {
                        clientName = clientName.substring(1); //delete #
                        password = in.readLine();
                    } else {
                        System.out.println("用户" + clientName + "有异常登录操作");
                    }

                    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ChatRoomDB?serverTimezone=GMT%2B8&useSSL=false", "root", "123456")) {
                        ps = conn.prepareStatement("select * from Users where name = ?");
                        ps.setString(1, clientName);
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            if (rs.getString("password").equals(password)) {
                                out.println("登陆成功");
                                //界面更新
                                clientNumber++;
                                serverFrame.updateAddUsers(new User(clientName, "", rs.getString("avatarpath")));
                                serverFrame.updateInfoPanel(clientNumber, "");
                                break;
                            } else {
                                out.println("密码错误");
                            }
                        } else {
                            out.println("没有此账号");
                        }
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }

                }
                // 将新客户端添加到管理列表
                clientFileMap.put(clientName, socket.getOutputStream());
                clientMap.put(clientName, out);

                // 登陆成功后，持续监听，处理客户端消息
                String message;
                while (true) {
                    message = in.readLine();
                    System.out.println("接收到来自客户端" + clientName + ":" + message);
                    serverFrame.updateInfoPanel(clientNumber, "接到客户端" + clientName + "信息：" + message);
                    if (message != null) {
                        if (message.startsWith("@")) {    //私聊消息
                            // 消息格式：@目标名:内容
                            int colonIndex = message.indexOf(":");
                            if (colonIndex != -1) {
                                String targetName = message.substring(1, colonIndex);
                                String privateMessage = message.substring(colonIndex + 1);
                                sendPrivateMessage(clientName, targetName, privateMessage);
                            }
                        } else if (message.startsWith("[File]")) {         //接受处理文件
                            //格式  [File]目标名:文件名
                            String targetName;
                            String fileName;
                            long fileSize;
                            int colonIndex = message.indexOf(":");
                            int colonIndex2 = message.indexOf("|");
                            if (colonIndex != -1) {
                                targetName = message.substring(6, colonIndex);
                                fileName = message.substring(colonIndex + 1, colonIndex2);
                                fileSize = Long.parseLong(message.substring(colonIndex2 + 1));  //大小转换
                                sendFile(clientName, targetName, fileName, fileSize);
                            }
                        } else if (message.startsWith("[Friend]")) {
                            sendPrivateMessage(clientName, message.substring(8), "!!#fd");
                        } else {
                            System.out.println("来自客户端的消息返回了null");
                        }
//                        else {
//                            // 群聊消息(没开发)
//                            broadcastMessage(clientName, message);
//                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("用户 " + clientName + " 已下线.(服务器发完消息异常)" + e.getMessage());
                serverFrame.updateRemoveUsers(clientName);
                clientNumber--;
                serverFrame.updateInfoPanel(clientNumber, "");
                clientMap.remove(clientName);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        //私发消息
        void sendPrivateMessage(String sender, String targetName, String message) {
            PrintWriter target = clientMap.get(targetName);
            if (target != null) {
                target.println("[Message]" + sender + ":" + message);
                System.out.println("[Message]" + sender + ":" + message + "|" + targetName);
                serverFrame.updateInfoPanel(clientNumber, "服务器对" + targetName + "反馈：[Message]" + sender + ":" + message);
            } else {
                out.println("[Message][server]:" + targetName + " 不在线上");
                serverFrame.updateInfoPanel(clientNumber, "服务器对" + clientName + "反馈：[Message][server]:" + targetName + " 不在线上");
            }
        }

        //全体广播
        void broadcastMessage(String sender, String message) {
            for (PrintWriter writer : clientMap.values()) {
                writer.println("[Message]" + sender + ": " + message);
            }
        }

        //发送文件
        void sendFile(String sender, String targetName, String fileName, long fileSize) {
            long sendSize = 0;
            try {
                InputStream fin = socket.getInputStream();
                OutputStream fout = clientFileMap.get(targetName);
                PrintWriter writer = clientMap.get(targetName);
                if (fout != null && writer != null) {
                    //先发送确认信息和文件信息
                    out.println("用户在线");
                    out.flush();

                    writer.println("[File]" + sender + ":" + fileName + "|" + fileSize);
                    writer.flush();

                    //传文件
                    int flag;
                    byte[] buffer = new byte[4096000];
                    while ((flag = fin.read(buffer)) != -1) {
                        fout.write(buffer, 0, flag);
                        fout.flush();
                        sendSize += flag;
                        if (sendSize >= fileSize) {
                            System.out.println("文件发送成功");
                            //清空数据流，防止混淆
                            while (fin.available() > 0) {
                                in.read();
                            }
                            break;
                        }
                    }
                } else {
                    out.println("用户" + targetName + "不在线上");
                }
            } catch (IOException e) {
                System.out.println("服务器发送文件异常：" + e.getMessage());
            }
        }

        private void cleanup() {
            try {
                if (clientName != null) {
                    clientMap.remove(clientName);
                    broadcastMessage("[Message][server]:", clientName + "-已离开.");
                }
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
