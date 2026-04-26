import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class CreateLogo {
    public static void main(String[] args) {
        try {
            // Create logo for xxxhdpi (192x192)
            BufferedImage logo = createLogoImage(192);
            ImageIO.write(logo, "png", new File("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"));
            System.out.println("Created xxxhdpi logo");

            // Create logo for xxhdpi (144x144)
            logo = createLogoImage(144);
            ImageIO.write(logo, "png", new File("app/src/main/res/mipmap-xxhdpi/ic_launcher.png"));
            System.out.println("Created xxhdpi logo");

            // Create logo for xhdpi (96x96)
            logo = createLogoImage(96);
            ImageIO.write(logo, "png", new File("app/src/main/res/mipmap-xhdpi/ic_launcher.png"));
            System.out.println("Created xhdpi logo");

            // Create logo for hdpi (72x72)
            logo = createLogoImage(72);
            ImageIO.write(logo, "png", new File("app/src/main/res/mipmap-hdpi/ic_launcher.png"));
            System.out.println("Created hdpi logo");

            // Create logo for mdpi (48x48)
            logo = createLogoImage(48);
            ImageIO.write(logo, "png", new File("app/src/main/res/mipmap-mdpi/ic_launcher.png"));
            System.out.println("Created mdpi logo");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static BufferedImage createLogoImage(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics g = image.getGraphics();

        // Draw background gradient
        g.setColor(new Color(66, 133, 244)); // Blue
        g.fillRoundRect(0, 0, size, size, size/4, size/4);

        // Draw chat bubble
        int bubbleSize = size * 3 / 4;
        int bubbleX = size / 8;
        int bubbleY = size / 8;
        g.setColor(Color.WHITE);
        g.fillRoundRect(bubbleX, bubbleY, bubbleSize, bubbleSize, size/8, size/8);

        // Draw AI brain
        int brainSize = bubbleSize * 3 / 4;
        int brainX = bubbleX + bubbleSize / 8;
        int brainY = bubbleY + bubbleSize / 8;
        g.setColor(new Color(66, 133, 244)); // Blue
        g.fillOval(brainX, brainY, brainSize, brainSize);

        // Draw brain lines
        g.setColor(Color.WHITE);
        int lineWidth = brainSize / 6;
        ((Graphics2D)g).setStroke(new BasicStroke(lineWidth));
        g.drawLine(brainX + brainSize/4, brainY + brainSize/2, brainX + brainSize*3/4, brainY + brainSize/2);
        g.drawLine(brainX + brainSize/2, brainY + brainSize/4, brainX + brainSize/2, brainY + brainSize*3/4);
        g.drawLine(brainX + brainSize/3, brainY + brainSize/3, brainX + brainSize*2/3, brainY + brainSize*2/3);
        g.drawLine(brainX + brainSize*2/3, brainY + brainSize/3, brainX + brainSize/3, brainY + brainSize*2/3);

        g.dispose();
        return image;
    }
}
