package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public class registFrame {

    // 基本组件
    JFrame frame = new JFrame("regist");

    JLabel avatarLabel = new JLabel("");
    String avatarPath = null;
    JLabel title = new JLabel("注册");
    JLabel account = new JLabel("用户名： ");
    JLabel password = new JLabel("密码：     ");
    JLabel confirmpassword = new JLabel("确认密码:");

    JTextField username = new JTextField(20);
    JTextField password1 = new JTextField(20);
    JTextField conTF = new JTextField(20);

    JButton confirm = new JButton("确认");
    JButton selectAvatarButton = new JButton("选择头像");

    void init() throws IOException {
        title.setFont(new Font("stsong", Font.BOLD, 40));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setForeground(Color.decode("#c44dff"));

        DiyPanel backgroud = new DiyPanel("resources/registback.jpg", 0.35f);

        title.setPreferredSize(new Dimension(300, 50));

        JPanel accountj = new JPanel(new FlowLayout(FlowLayout.CENTER));

        //绑定listener
        confirm.addActionListener(new ActionListener() {
            PreparedStatement ps = null;
            ResultSet rs = null;

            //在数据库里添加账号信息,不会向服务器发信息，保证登录窗口不会异常
            //同时本地更新数据库省略
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!password1.getText().equals(conTF.getText())) {
                    JOptionPane.showMessageDialog(frame, "密码不一致");
                } else if (username.getText().equals("") || password1.getText().equals("") || conTF.getText().equals("")) {
                    JOptionPane.showMessageDialog(frame, "信息不完全");
                } else {
                    try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ChatRoomDB?serverTimezone=GMT%2B8&useSSL=false",
                            "root", "123456")) {
                        ps = conn.prepareStatement("select * from Users where name = ?");
                        ps.setString(1, username.getText());
                        rs = ps.executeQuery();
                        if (rs.next()) {
                            JOptionPane.showMessageDialog(frame, "用户名已存在");
                        } else {
                            if (Files.exists(Path.of(avatarPath))) {
                                ps = conn.prepareStatement("insert into Users values(?,?,?,?)");
                                ps.setString(1, null);
                                ps.setString(2, username.getText());
                                ps.setString(3, password1.getText());
                                ps.setString(4, avatarPath);
                                ps.executeUpdate();
                                JOptionPane.showMessageDialog(frame, "注册成功啦");
                            } else {
                                JOptionPane.showMessageDialog(frame, "头像路径异常");
                            }
                        }
                    } catch (SQLException ex) {
                        System.out.println("sql异常111：" + ex.getMessage());
                    }
                }
            }
        });

        //绘制头像
        selectAvatarButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.showOpenDialog(frame);
                File file = chooser.getSelectedFile();
                if (Files.exists(file.toPath())) {
                    avatarPath = file.getAbsolutePath().replace("\\", "/");
                    System.out.println(avatarPath);
                } else {
                    JOptionPane.showMessageDialog(frame, "文件不存在");
                }
                setAvatar();
            }
        });

        //组装组件
        JPanel password1j = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel password2j = new JPanel(new FlowLayout(FlowLayout.CENTER));
        account.setFont(new Font("stsong", Font.BOLD, 25));
        accountj.add(account);
        accountj.add(username);
        password.setFont(new Font("stsong", Font.BOLD, 25));
        password1j.add(password);
        password1j.add(password1);
        confirmpassword.setFont(new Font("stsong", Font.BOLD, 25));
        password2j.add(confirmpassword);
        password2j.add(conTF);

        Box box = Box.createVerticalBox();
        box.add(Box.createVerticalStrut(30));
        box.add(title);
        box.add(Box.createVerticalStrut(30));
        box.add(avatarLabel);
        box.add(Box.createVerticalStrut(80));
        box.add(accountj);
        box.add(Box.createVerticalStrut(50));
        box.add(password1j);
        box.add(Box.createVerticalStrut(50));
        box.add(password2j);
        box.add(Box.createVerticalStrut(75));
        box.add(confirm);
        box.add(Box.createVerticalStrut(40));
        box.add(selectAvatarButton);


        backgroud.add(box);
        frame.setPreferredSize(new Dimension(650, 840));
        frame.add(backgroud);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    //设置头像方法,默认按照150,150像素缩放
    public void setAvatar() {
        ImageIcon avatarIcon = new ImageIcon(avatarPath);
        avatarIcon = new ImageIcon(avatarIcon.getImage().getScaledInstance(150, 150, Image.SCALE_SMOOTH));
        avatarLabel.setIcon(avatarIcon);
    }

}
