import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GenerateLogo {
    public static void main(String[] args) {
        // 定义不同密度的尺寸
        int[] sizes = {48, 72, 96, 144, 192}; // mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
        String[] directories = {
            "app/src/main/res/mipmap-mdpi",
            "app/src/main/res/mipmap-hdpi",
            "app/src/main/res/mipmap-xhdpi",
            "app/src/main/res/mipmap-xxhdpi",
            "app/src/main/res/mipmap-xxxhdpi"
        };

        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            String directory = directories[i];
            generateLogo(size, directory + File.separator + "ic_launcher.png");
        }
    }

    private static void generateLogo(int size, String outputPath) {
        // 创建缓冲图像
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制背景渐变
        GradientPaint gradient = new GradientPaint(
            0, 0, new Color(66, 133, 244), // 蓝色
            size, size, new Color(156, 39, 176)  // 紫色
        );
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, size, size, size/4, size/4);

        // 绘制聊天气泡
        int bubbleSize = size * 3 / 4;
        int bubbleX = size / 8;
        int bubbleY = size / 8;
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(bubbleX, bubbleY, bubbleSize, bubbleSize, size/8, size/8);

        // 绘制AI大脑符号
        int brainSize = bubbleSize * 3 / 4;
        int brainX = bubbleX + bubbleSize / 8;
        int brainY = bubbleY + bubbleSize / 8;
        g2d.setColor(new Color(66, 133, 244));
        g2d.fillOval(brainX, brainY, brainSize, brainSize);

        // 绘制大脑内部结构
        g2d.setColor(Color.WHITE);
        int lineWidth = brainSize / 6;
        g2d.setStroke(new BasicStroke(lineWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.drawLine(brainX + brainSize/4, brainY + brainSize/2, brainX + brainSize*3/4, brainY + brainSize/2);
        g2d.drawLine(brainX + brainSize/2, brainY + brainSize/4, brainX + brainSize/2, brainY + brainSize*3/4);
        g2d.drawLine(brainX + brainSize/3, brainY + brainSize/3, brainX + brainSize*2/3, brainY + brainSize*2/3);
        g2d.drawLine(brainX + brainSize*2/3, brainY + brainSize/3, brainX + brainSize/3, brainY + brainSize*2/3);

        // 释放资源
        g2d.dispose();

        // 保存图像
        try {
            File outputFile = new File(outputPath);
            ImageIO.write(image, "png", outputFile);
            System.out.println("Generated logo at: " + outputPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
