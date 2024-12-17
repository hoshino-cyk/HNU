package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

public class serverFrame {
    JFrame frame = new JFrame("Server");

    // 主面板显示消息文件收发。
    JPanel infoPanel = new JPanel(new BorderLayout());
    JPanel memberPanel = new JPanel(new BorderLayout());
    JPanel aboutPanel = new JPanel(new GridLayout(2, 1, 10, 10));

    // 右侧记录列表
    static JPanel rightPanel = new JPanel(new BorderLayout(10, 10));

    // 消息与文件记录列表
    static DefaultListModel<String> recordListModel = new DefaultListModel<>();
    static JList<String> recordList = new JList<>(recordListModel);
    static JScrollPane recordScrollPane = new JScrollPane(recordList);

    static JLabel currentOnline = new JLabel("0");

    //用户列表
    static DefaultListModel<User> userModel = new DefaultListModel<>();
    static JList<User> userList = new JList<>(userModel);
    JScrollPane memberScrollPane = new JScrollPane(userList);

    public void init() {
        frame.setTitle("服务器");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);

        // 设置菜单栏
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("菜单");

        JMenuItem infoMenuItem = new JMenuItem("信息面板");
        JMenuItem memberMenuItem = new JMenuItem("成员列表");
        JMenuItem aboutMenuItem = new JMenuItem("关于本软件");

        menu.add(infoMenuItem);
        menu.add(memberMenuItem);
        menu.add(aboutMenuItem);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        // 设置主要面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        frame.setContentPane(mainPanel);

        //绑定用户renderer
        userList.setCellRenderer(new UserRenderer());
        memberPanel.add(memberScrollPane, BorderLayout.CENTER);

        // 默认显示信息面板
        mainPanel.add(infoPanel, BorderLayout.CENTER);

        // 左侧信息面板
        JPanel leftPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        leftPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        //初始化文本
        leftPanel.add(new JLabel("在线人数:"));
        leftPanel.add(currentOnline);
        leftPanel.add(new JLabel("服务器IP:"));
        JLabel IP = new JLabel("127.0.0.1");
        leftPanel.add(IP);
        leftPanel.add(new JLabel("端口:"));
        JLabel Port = new JLabel("39393");
        leftPanel.add(Port);


        rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        recordList.setBorder(BorderFactory.createTitledBorder("消息与文件记录"));


        // 底部清空按钮
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton clearButton = new JButton("清空记录");
        bottomPanel.add(clearButton);

        // 清空记录功能
        clearButton.addActionListener(e -> recordListModel.clear());

        rightPanel.add(recordScrollPane, BorderLayout.CENTER);
        rightPanel.add(bottomPanel, BorderLayout.SOUTH);

        // SplitPane分隔
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(300);
        infoPanel.add(splitPane, BorderLayout.CENTER);


        // 菜单功能绑定
        infoMenuItem.addActionListener(e -> {
            mainPanel.removeAll();
            mainPanel.add(infoPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        memberMenuItem.addActionListener(e -> {
            mainPanel.removeAll();
            mainPanel.add(memberPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        aboutMenuItem.addActionListener(e -> {
            mainPanel.removeAll();
            aboutPanel = createAboutPanel();
            mainPanel.add(aboutPanel, BorderLayout.CENTER);
            mainPanel.revalidate();
            mainPanel.repaint();
        });

        frame.setVisible(true);
    }

    // 关于面板
    private JPanel createAboutPanel() {
        JPanel aboutPanel = this.aboutPanel;
        aboutPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel authorLabel = new JLabel("作者: Hoshino_cyk");
        authorLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel versionLabel = new JLabel("版本: 0.1");
        versionLabel.setHorizontalAlignment(SwingConstants.CENTER);

        aboutPanel.add(authorLabel);
        aboutPanel.add(versionLabel);

        return aboutPanel;
    }

    //更新主面板
    public static void updateInfoPanel(int userNum, String message) {
        currentOnline.setText(String.valueOf(userNum));
        recordListModel.addElement(message);
        recordList.ensureIndexIsVisible(recordListModel.getSize() - 1);
        recordList.repaint();
    }

    //更新添加用户列表
    public static void updateAddUsers(User newUser) {
        userModel.addElement(newUser);
        userList.ensureIndexIsVisible(userModel.getSize() - 1);
        userList.repaint();
    }

    //更新减少用户列表
    public static void updateRemoveUsers(String name) {
        for (int i = 0; i < userModel.getSize(); i++) {
            if (userModel.getElementAt(i).getName().equals(name)) {
                userModel.removeElementAt(i);
            }
        }
        userList.ensureIndexIsVisible(userModel.getSize() - 1);
        userList.repaint();
    }

}
