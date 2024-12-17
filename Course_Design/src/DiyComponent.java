package src;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

//自定义按钮，根据图像创建
//path + actionlistener
class DiyButton extends JButton {
    private ImageIcon icon;
    String imagepath;
    int width, height;


    public DiyButton(String imagepath, ActionListener al) {
        super("");

        this.imagepath = imagepath;
        this.icon = new ImageIcon(imagepath);
        this.addActionListener(al);
        this.width = getPreferredSize().width;
        this.height = getPreferredSize().height;

        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    public DiyButton(String imagepath, int width, int height, ActionListener al) {
        this.imagepath = imagepath;
        this.icon = new ImageIcon(imagepath);
        this.addActionListener(al);
        this.width = width;
        this.height = height;

        setBorderPainted(false);
        setContentAreaFilled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int x = (getWidth() - icon.getIconWidth()) / 2;
        int y = (getHeight() - icon.getIconHeight()) / 2;
        if (Files.exists(Paths.get(imagepath))) {
            g.drawImage(icon.getImage(), x, y, width, height, null);
        }
    }
}

//path + 不透明度
//自定义panel
class DiyPanel extends JPanel {
    private Image image;
    private double rate; // 图片的宽高比
    float alpha;


    public DiyPanel(String imagepath, float alpha) {
        try {
            this.image = ImageIO.read(new File(imagepath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // 计算原始图片的宽高比
        rate = (double) image.getWidth(null) / image.getHeight(null);
        this.alpha = alpha;

        // 设置背景颜色
        setBackground(Color.decode("#e6e6e6"));
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        // 设置透明度
        Graphics2D g2d = (Graphics2D) g;
        AlphaComposite ac = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha);
        g2d.setComposite(ac);

        // 获取面板的当前大小
        int panelWidth = getWidth();
        int panelHeight = getHeight();

        // 根据面板的宽高比调整图片大小
        int newWidth = panelWidth;
        int newHeight = (int) (panelWidth / rate);

        // 如果根据宽度调整后的高度超过面板的高度，则根据高度来调整宽度
        if (newHeight > panelHeight) {
            newHeight = panelHeight;
            newWidth = (int) (panelHeight * rate);
        }

        // 在面板中心绘制图片
        int x = (panelWidth - newWidth) / 2;
        int y = (panelHeight - newHeight) / 2;

        // 绘制调整后的图片
        g.drawImage(image, x, y, newWidth, newHeight, null);
    }
}


//调试
public class DiyComponent {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Custom Button with Image");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);

        DiyButton button = new DiyButton("resources/loginbutton.png", new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showConfirmDialog(frame, "success", null, JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);
            }
        }); // 使用自定义的图片路径

        JPanel panel = new JPanel();
        panel.add(button);
        button.setPreferredSize(new Dimension(287, 61));
        frame.add(panel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }
}
