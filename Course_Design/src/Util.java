package src;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.List;

// 用户类
class User {
    private String name;
    private String lastMessage;
    private String avatarPath;

    public User(String name, String lastMessage, String avatarPath) {
        this.name = name;
        this.lastMessage = lastMessage;
        this.avatarPath = avatarPath;
    }

    public String getName() {
        return name;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
}

// 消息类
class Message {
    private String text;
    private boolean isSentByThisUser;

    public Message(String text, boolean isSentByThisUser) {
        this.text = text;
        this.isSentByThisUser = isSentByThisUser;
    }

    public String getText() {
        return text;
    }

    public boolean isSentByThisUser() {
        return isSentByThisUser;
    }
}

// 用户渲染器
class UserRenderer extends JPanel implements ListCellRenderer<User> {
    private JLabel avatarLabel;
    private JLabel nameLabel;
    private JLabel lastMessageLabel;
    JPanel textPanel = new JPanel(new BorderLayout());

    public UserRenderer() {
        setLayout(new BorderLayout(3, 5));
        avatarLabel = new JLabel();
        nameLabel = new JLabel();
        nameLabel.setFont(new Font("stsong", Font.BOLD, 18));
        lastMessageLabel = new JLabel();
        lastMessageLabel.setFont(new Font("stsong", Font.ITALIC, 14));
        lastMessageLabel.setForeground(Color.GRAY);

        textPanel.add(nameLabel, BorderLayout.NORTH);
        textPanel.add(lastMessageLabel, BorderLayout.CENTER);

        add(avatarLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends User> list, User user, int index, boolean isSelected, boolean cellHasFocus) {

        // 设置头像
        ImageIcon avatarIcon = new ImageIcon(user.getAvatarPath());
        avatarIcon = new ImageIcon(avatarIcon.getImage().getScaledInstance(50, 50, Image.SCALE_SMOOTH));
        avatarLabel.setIcon(avatarIcon);

        // 设置用户名和最后消息
        nameLabel.setText(user.getName());
        String lastMessage = user.getLastMessage();
        lastMessageLabel.setText(lastMessage.length() > 20 ? lastMessage.substring(0, 20) + "..." : lastMessage);

        // 设置选中状态样式
        if (isSelected) {
            textPanel.setBackground(Color.decode("#b3cccc"));
        } else {
            textPanel.setBackground(Color.decode("#f0f5f5"));
        }

        return this;
    }
}

// 消息渲染器
class MessageRenderer extends JPanel implements ListCellRenderer<Message> {
    private JLabel messageLabel;

    public MessageRenderer() {
        setLayout(new BorderLayout());
        messageLabel = new JLabel();
        messageLabel.setOpaque(true);
        add(messageLabel, BorderLayout.CENTER);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends Message> list, Message message, int index, boolean isSelected, boolean cellHasFocus) {

        messageLabel.setText(message.getText());
        messageLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        if (message.isSentByThisUser()) {
            messageLabel.setBackground(new Color(173, 216, 230));
            setLayout(new FlowLayout(FlowLayout.RIGHT));
        } else {
            messageLabel.setBackground(new Color(240, 240, 240));
            setLayout(new FlowLayout(FlowLayout.LEFT));
        }

        return this;
    }
}

public class Util {
    //根据用户名获得好友列表Set<String>
    public static Set<String> getFriends(String user) {
        int curuser_id = 0;
        int friends_id;
        Set<String> friends = new HashSet<>();
        //连接数据库获得当前用户的好友列表
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ChatRoomDB?serverTimezone=GMT%2B8&useSSL=false",
                "root", "123456")) {
            PreparedStatement ps = conn.prepareStatement("select id from users where name = ?");
            ps.setString(1, user);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                curuser_id = rs.getInt("id");
            } else {
                System.out.println("No user found");
            }
            //根据用户id获取friendsid集(添加好友时双向添加）
            ps = conn.prepareStatement("select friends_id from friends where users_id = ?");
            ps.setString(1, String.valueOf(curuser_id));
            rs = ps.executeQuery();

            ArrayList<Integer> fids = new ArrayList<>();
            while (rs.next()) {
                fids.add(rs.getInt("friends_id"));
            }
            ps = conn.prepareStatement("select name from users where id = ?");
            for (int fid : fids) {
                ps.setString(1, String.valueOf(fid));
                rs = ps.executeQuery();
                if (rs.next()) {
                    friends.add(rs.getString("name"));
                } else {
                    System.out.println("id 为 " + fid + "的用户没找到");
                }
            }
        } catch (SQLException ex) {
            System.out.println("sql异常：" + ex.getMessage());
        }

        return friends;
    }


    //根据名字获得头像路径
    public static String getAvatarPath(String name) {
        String avatarPath = "resources/avatarTest.png";   //默认头像路径以防万一
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ChatRoomDB?serverTimezone=GMT%2B8&useSSL=false",
                "root", "123456")) {
            PreparedStatement ps = conn.prepareStatement("select avatarpath from users where name = ?");
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                avatarPath = rs.getString("avatarpath");
            }
        } catch (SQLException e) {
            System.out.println("sql异常：" + e.getMessage());
        }
        return avatarPath;
    }

    //添加好友功能
    public static void addNewFriend(String client, String friendName) {
        int clientId = 0;
        int friendId = 0;
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/ChatRoomDB?serverTimezone=GMT%2B8&useSSL=false",
                "root", "123456")) {
            PreparedStatement ps = conn.prepareStatement("select id from users where name = ?");
            ps.setString(1, client);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                clientId = rs.getInt("id");
            }
            ps.setString(1, friendName);
            rs = ps.executeQuery();
            if (rs.next()) {
                friendId = rs.getInt("id");
            } else {
                JOptionPane.showMessageDialog(null, "没有该用户", "用户添加异常", JOptionPane.ERROR_MESSAGE);
            }

            //修改friends表
            ps = conn.prepareStatement("insert into friends values(null,?,?),(null,?,?)");
            ps.setString(1, String.valueOf(clientId));
            ps.setString(2, String.valueOf(friendId));
            ps.setString(3, String.valueOf(friendId));
            ps.setString(4, String.valueOf(clientId));
            ps.executeUpdate();
            JOptionPane.showMessageDialog(null, "添加成功啦", "添加好友", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException e) {
            System.out.println("sql异常：" + e.getMessage());
        }
    }
}