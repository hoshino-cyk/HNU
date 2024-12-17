package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.sql.*;

public class LoginFrame {
    JFrame frame = new JFrame("Login");
    //右侧
    JSplitPane right;

    //title
    JLabel title = new JLabel("欢迎  Welcome");

    //用户名密码及对应输入框
    JLabel account = new JLabel("用户名：");
    JLabel password = new JLabel("密码：    ");
    JTextField username = new JTextField(20);
    JTextField password1 = new JTextField(20);

    //连接服务器
    Socket socket;
    PrintWriter out = null;
    BufferedReader in = null;

    //登录以及注册按钮
    DiyButton login = new DiyButton("resources/loginbutton.png", 250, 63, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            //比对成功后提交用户名和密码 格式：#+name 密码无格式
            if (!username.getText().isEmpty() && !password1.getText().isEmpty()) {
                out.println("#" + username.getText());
                out.println(password1.getText());

            } else {
                JOptionPane.showMessageDialog(frame, "登录信息不完全");
            }
        }
    });

    DiyButton regist = new DiyButton("resources/registerbutton.png", 250, 60, new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            //唤起registFrame窗口
            try {
                new registFrame().init();
            } catch (IOException ex) {
                System.out.println(ex.getMessage());
            }
        }
    });

    //数据库组件
    Statement st = null;
    Connection con = null;

    public void init(Socket s) throws Exception {
        this.socket = s;
        in = new BufferedReader(new InputStreamReader(s.getInputStream()));
        out = new PrintWriter(s.getOutputStream(), true);
        //连接成功
        String connectStatus = in.readLine();
        if (connectStatus.equals("连接成功")) {
            JOptionPane.showMessageDialog(frame, "连接服务器成功!", "连接提示", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(frame, "连接服务器失败，请重试", "连接提示", JOptionPane.INFORMATION_MESSAGE);
        }


        //组装文本文字框
        JPanel usernamePanel = new JPanel();
        usernamePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 20));
        usernamePanel.add(account);
        usernamePanel.add(username);
        account.setPreferredSize(new Dimension(60, 33));
        username.setPreferredSize(new Dimension(20, 33));
        account.setFont(new Font("stsong", Font.BOLD, 12));

        JPanel passwordPanel = new JPanel();
        passwordPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 20));
        passwordPanel.add(password);
        passwordPanel.add(password1);
        password.setPreferredSize(new Dimension(60, 33));
        password1.setPreferredSize(new Dimension(20, 33));
        password.setFont(new Font("stsong", Font.BOLD, 12));


        //右侧总体组装
        title.setFont(new Font("stsong", Font.BOLD, 40));
        title.setForeground(Color.decode("#00aaff"));
        title.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel right = new JPanel(new GridLayout(8, 1, 5, 0));
        right.add(new Panel());
        right.add(title);
        right.add(new Panel());
        right.add(usernamePanel);
        right.add(passwordPanel);
        right.add(new Panel());
        login.setPreferredSize(new Dimension(287, 61));
        regist.setPreferredSize(new Dimension(287, 61));
        right.add(login);
        right.add(regist);


        //总体组装
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new DiyPanel("resources/loginbackground.jpg", 0.9f), right);
        sp.resetToPreferredSizes();
        sp.setDividerSize(1);
        sp.setDividerLocation(573);
        sp.setEnabled(false);

        frame.setPreferredSize(new Dimension(1050, 849));
        frame.add(sp);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //登录确认
        while (true) {
            String temp = in.readLine();
            switch (temp) {
                case "登陆成功":
                    JOptionPane.showMessageDialog(frame, "登陆成功", "登录提示", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "密码错误":
                    JOptionPane.showMessageDialog(frame, "密码错误", "登录提示", JOptionPane.INFORMATION_MESSAGE);
                    break;
                case "没有此账号":
                    JOptionPane.showMessageDialog(frame, "没有此账号", "登录提示", JOptionPane.INFORMATION_MESSAGE);
                    break;
                default:
                    break;
            }
            if (temp.equals("登陆成功")) {
                break;
            }
        }

        //登录成功后，开启chatFrame，传入socket对象
        new chatFrame().init(socket, username.getText().trim());
        //关闭窗口
        frame.setVisible(false);
    }
}
