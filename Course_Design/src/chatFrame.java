package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.io.*;
import java.net.*;
import java.util.List;

public class chatFrame {

    User selectedUser;
    volatile String statusMessage = "";

    void init(Socket socket, String clientName) {
        System.out.println("这是用户：" + clientName);
        final Object lock = new Object();    //保证文件传输完成后监听线程再继续

        JFrame frame = new JFrame("chatFrameTest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 800);

        // 左侧用户列表数据
        DefaultListModel<User> userModel = new DefaultListModel<>();
        JList<User> userList = new JList<>(userModel);
        userList.setCellRenderer(new UserRenderer());
        userList.setVisibleRowCount(8);

        // 初始化用户列表
        Set<String> friends = Util.getFriends(clientName);
        for (String friend : friends) {
            userModel.addElement(new User(friend, "", Util.getAvatarPath(friend)));
        }

        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(200, 0));

        //消息列表部分没做长期存储，空白初始化

        // 聊天部分
        DefaultListModel<Message> messageModel = new DefaultListModel<>();
        JList<Message> messageList = new JList<>(messageModel);
        messageList.setCellRenderer(new MessageRenderer());
        JScrollPane chatScrollPane = new JScrollPane(messageList);


        // 输入区域
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout());
        JTextArea jTextArea = new JTextArea(10, 45);
        jTextArea.setBorder(BorderFactory.createLineBorder(Color.black));
        JButton sendButton = new JButton("发送");
        JButton selectFile = new JButton("发送文件");
        jTextArea.setRows(3);

        jTextArea.setLineWrap(true);
        jTextArea.setWrapStyleWord(true);

        JScrollPane textScroll = new JScrollPane(jTextArea);

        inputPanel.add(textScroll);
        inputPanel.add(Box.createHorizontalStrut(20));
        inputPanel.add(sendButton);
        inputPanel.add(Box.createHorizontalStrut(20));
        inputPanel.add(selectFile);

        //jsplit组装两边
        JPanel right = new JPanel();
        right.setLayout(new BorderLayout());
        right.add(chatScrollPane, BorderLayout.CENTER);
        right.add(inputPanel, BorderLayout.SOUTH);
        JSplitPane jsp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, userScrollPane, right);
        jsp.setDividerSize(5);
        jsp.setDividerLocation(300);
        frame.add(jsp);

        // 初始化实时消息存储,存储接发消息
        Map<String, List<Message>> userMessagesMap = new HashMap<>();
        //更新全体好友消息列表
        for (int i = 0; i < userModel.size(); i++) {
            //初始化下，免得抛exception
            List<Message> list = new ArrayList<>();
            list.add(new Message("已添加好友，一起聊天吧", false));
            userMessagesMap.put(userModel.getElementAt(i).getName(), list);
        }
        userList.setSelectedIndex(0);
        selectedUser = userList.getSelectedValue();//初始化选中对象

        // 选中用户切换聊天
        userList.addListSelectionListener(e -> {
            //防止点击过于灵敏
            if (!e.getValueIsAdjusting()) {
                selectedUser = userList.getSelectedValue();
                if (selectedUser != null) {
                    // 切换到选中用户的消息记录
                    messageModel.clear();
                    List<Message> messages = userMessagesMap.get(selectedUser.getName());
                    for (Message msg : messages) {
                        messageModel.addElement(msg);
                    }
                }
            }
        });


        // 发送消息逻辑
        //":" 作为分隔符
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String messageText = jTextArea.getText().trim();
                if (!messageText.isEmpty() && userList.getSelectedValue() != null) {
                    User selectedUser = userList.getSelectedValue();
                    Message message = new Message(messageText, true);
                    userMessagesMap.get(selectedUser.getName()).add(message);
                    messageModel.addElement(message);
                    //滚动到最新消息
                    messageList.ensureIndexIsVisible(messageModel.size() - 1);
                    List<Message> update = userMessagesMap.get(selectedUser.getName());
                    update.add(message);

                    //上传到服务器
                    try {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("@" + selectedUser.getName() + ":" + messageText);
                        out.flush();
                        System.out.println("已将消息发送至服务器：" + "@" + selectedUser.getName() + ":" + messageText);
                    } catch (IOException IOe) {
                        System.out.println("消息发送异常： " + IOe.getMessage());
                    }

                    // 更新用户列表的最后一条消息
                    selectedUser.setLastMessage(messageText);
                    userList.repaint();
                    jTextArea.setText("");
                }
                //空不做提示
            }
        });

        //发送文件
        selectFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setMultiSelectionEnabled(false);
                fileChooser.showOpenDialog(frame);
                fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);//仅单选，限定文件
                File file = fileChooser.getSelectedFile();

                long fileSize = file.length();  // 给出文件大小，划分流。

                messageModel.addElement(new Message("我发送了一个文件: " + file.getName(), true));

                // 更新用户列表的最后一条消息
                selectedUser.setLastMessage("[发送文件]" + file.getName());
                userList.repaint();
                messageList.repaint();
                List<Message> update = userMessagesMap.get(selectedUser.getName());
                if (update != null) {
                    update.add(new Message("我发送了一个文件: " + file.getName(), true));
                }

                //传服务器
                try {
                    FileInputStream fin = new FileInputStream(file);
                    OutputStream fout = socket.getOutputStream();
                    PrintWriter pwout = new PrintWriter(fout);

                    byte[] buffer = new byte[4096000];
                    int flag;

                    // 发送文件名,格式[File]目标:文件名|文件大小
                    pwout.println("[File]" + selectedUser.getName() + ":" + file.getName() + "|" + fileSize);
                    pwout.flush();
                    //这里如果用户不在线则停止发送文件
                    while (true) {
                        if (!statusMessage.isEmpty()) {
                            if (statusMessage.equals("用户在线")) {
                                synchronized (lock) {
                                    while ((flag = fin.read(buffer)) != -1) {
                                        fout.write(buffer, 0, flag);
                                        fout.flush();
                                    }
                                    System.out.println("文件发送完成");  //可做进度条，算了
                                    statusMessage = "";
                                    lock.notify();
                                }
                            } else {
                                messageModel.addElement(new Message("[server]" + statusMessage, false));
                                messageList.repaint();
                                statusMessage = "";
                            }
                            break;
                        }
                    }
                } catch (IOException ioex) {
                    System.out.println("文件发送异常：" + ioex.getMessage());
                }
            }
        });
        frame.setVisible(true);

        //右键菜单，添加好友功能
        JPopupMenu popup = new JPopupMenu();
        JMenuItem addFriend = new JMenuItem("添加好友");
        addFriend.addActionListener(e -> {
            String friend;
            friend = JOptionPane.showInputDialog(userList, "请输入添加好友的名字", "输入对话框", JOptionPane.INFORMATION_MESSAGE);
            Set<String> friendList = Util.getFriends(clientName);

            if (!friend.isEmpty()) {
                if (friend.equals(clientName)) {
                    JOptionPane.showMessageDialog(frame, "不能添加自己为好友", "添加异常", JOptionPane.WARNING_MESSAGE);
                } else if (friendList.contains(friend)) {
                    JOptionPane.showMessageDialog(frame, "已经添加该好友", "添加异常", JOptionPane.WARNING_MESSAGE);
                } else {
                    Util.addNewFriend(clientName, friend);
                    userMessagesMap.put(friend, new ArrayList<>());
                    userModel.addElement(new User(friend, "新好友哦！", Util.getAvatarPath(friend)));
                    userList.repaint();
                    userList.revalidate();
                    //当对方在线时，要发送让对方刷新列表的请求。
                    try {
                        //好友添加格式：[Friend]+目标对象
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println("[Friend]" + friend);
                        out.flush();
                    } catch (IOException ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        });
        popup.add(addFriend);

        userList.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                showPopupMenu(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopupMenu(e);
            }

            void showPopupMenu(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });


        //开启一个监听服务器线程，监听其他用户发送信息，并且自动保存文件
        //文件格式[File]sender:fileName
        //消息格式[Message]sender:message
        new Thread(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String message;
                while (true) {
                    synchronized (lock) {         //等待文件传输完毕lock
                        System.out.println("监听线程获得锁");
                        message = reader.readLine();
                        System.out.println("收到来自服务器的信息：" + message);
                        if (message.startsWith("[Message]")) {
                            String sender = message.substring(9, message.indexOf(":"));
                            String content = message.substring(message.indexOf(":") + 1);
                            if (!sender.equals("[server]")) {
                                if (!content.equals("!!#fd")) {
                                    List<Message> update = userMessagesMap.get(sender);
                                    if (update != null) {
                                        update.add(new Message(content, false));
                                    }
                                    final User[] senderUser = {null};
                                    for (int i = 0; i < userModel.size(); i++) {
                                        if (userModel.get(i).getName().equals(sender)) {
                                            senderUser[0] = userModel.get(i);
                                        }
                                    }
                                    if (senderUser[0] != null) {
                                        senderUser[0].setLastMessage(content);
                                    }
                                    // UI 更新放入事件调度线程
                                    SwingUtilities.invokeLater(() -> {
                                        if (selectedUser != null && selectedUser.getName().equals(sender)) {
                                            messageModel.addElement(new Message(content, false));
                                            messageList.ensureIndexIsVisible(messageModel.size() - 1);
                                        }
                                        userList.repaint();
                                        messageList.revalidate();
                                        messageList.repaint();
                                    });
                                } else {
                                    userMessagesMap.put(sender, new ArrayList<>());   //新friend
                                    SwingUtilities.invokeLater(() -> {
                                        userModel.addElement(new User(sender, "对方刚刚添加你为好友！", Util.getAvatarPath(sender)));
                                        userList.repaint();
                                    });

                                }
                            } else {
                                // 处理服务器消息
                                SwingUtilities.invokeLater(() -> {
                                    messageModel.addElement(new Message("[server]" + content, false));
                                    messageList.repaint();
                                });
                            }
                        } else if (message.startsWith("[File]")) {   //接收方一定在线
                            String fileMessage = message.substring(6); // 去掉 [File]
                            String sender = message.substring(6, message.indexOf(":"));
                            String fileName = message.substring(message.indexOf(":") + 1, message.indexOf("|"));
                            long fileSize = Long.parseLong(message.substring(message.indexOf("|") + 1));
                            System.out.println("fileSize:" + fileSize);
                            long receivedSize = 0;

                            List<Message> update = userMessagesMap.get(sender);
                            if (update != null) {
                                update.add(new Message("对方向你发送了一个文件：" + fileMessage.substring(fileMessage.indexOf(":") + 1), false));
                            }
                            final User[] senderUser = {null};
                            for (int i = 0; i < userModel.size(); i++) {
                                if (userModel.get(i).getName().equals(sender)) {
                                    senderUser[0] = userModel.get(i);
                                }
                            }
                            if (senderUser[0] != null) {
                                senderUser[0].setLastMessage("有新文件！");
                            }
                            // UI 更新放入事件调度线程
                            SwingUtilities.invokeLater(() -> {
//                                System.out.println("当前选中：" + selectedUser.getName());
                                if (selectedUser != null && selectedUser.getName().equals(sender)) {
                                    messageModel.addElement(new Message("对方向你发送了一个文件：" + fileMessage.substring(fileMessage.indexOf(":") + 1), false));
                                    messageList.ensureIndexIsVisible(messageModel.size() - 1);
                                }
                                userList.repaint();
                                messageList.revalidate();
                                messageList.repaint();
                            });

                            //处理文件，默认保存在file文件夹
                            File file = new File("./file", fileName);
                            try {
                                InputStream in = socket.getInputStream();
                                FileOutputStream fout = new FileOutputStream(file);
                                byte[] buffer = new byte[4096000];
                                int flag;
                                while ((flag = in.read(buffer)) != -1) {
                                    fout.write(buffer, 0, flag);
                                    fout.flush();
                                    receivedSize += flag;
                                    System.out.println("进度：" + receivedSize + "读取量：" + flag);
                                    if (receivedSize >= fileSize) {
                                        break;
                                    }
                                }
                                while (in.available() > 0) {
                                    in.read();
                                }
                                System.out.println("inputstream清空完毕");
                                System.out.println("文件接受完毕，保存至项目文件夹file下，文件名：" + fileName);
                            } catch (IOException e) {
                                System.out.println("文件接受错误" + e.getMessage());
                            }

                        } else if (message.equals("用户在线")) {
                            statusMessage = message;
                            lock.wait();
                        } else {
                            statusMessage = "用户不在线";
                            Thread.sleep(1500);     //等待1.5s界面完成更新。
                        }
                    }
                }
            } catch (IOException e) {
                if (socket.isClosed()) {
                    System.out.println(e.getMessage());
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }).start();


    }


}

