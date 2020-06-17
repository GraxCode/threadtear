package me.nov.threadtear.util;

import me.nov.threadtear.CoreUtils;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public final class Images {
  private Images() {
  }

  public static ImageIcon combine(ImageIcon icon1, ImageIcon icon2) {
    Image img1 = icon1.getImage();
    Image img2 = icon2.getImage();

    int w = icon1.getIconWidth();
    int h = icon1.getIconHeight();
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    g2.drawImage(img1, 0, 0, null);
    g2.drawImage(img2, 0, 0, null);
    g2.dispose();

    return new ImageIcon(image);
  }

  public static BufferedImage watermark(BufferedImage old) {
    BufferedImage copy = broadenImage(old);
    Graphics2D g2d = copy.createGraphics();
    g2d.setPaint(Color.black);
    g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    FontMetrics fm = g2d.getFontMetrics();
    String watermark = "threadtear " + CoreUtils.getVersion();
    int x = copy.getWidth() - fm.stringWidth(watermark) - 5;
    int y = fm.getHeight();
    g2d.drawString(watermark, x, y);
    g2d.dispose();
    return copy;
  }

  private static BufferedImage broadenImage(BufferedImage source) {
    BufferedImage b = new BufferedImage(source.getWidth() + 60, source.getHeight() + 60, BufferedImage.TYPE_INT_ARGB);
    Graphics g = b.createGraphics();
    g.setColor(new Color(0x00ffffff, true));
    g.fillRect(0, 0, source.getWidth() + 60, source.getHeight() + 60);
    g.setColor(Color.BLACK);
    g.drawImage(source, 30, 30, null);
    g.dispose();
    return b;
  }

  public static int getComplementaryColor(int color) {
    int r = color & 255;
    int g = (color >> 8) & 255;
    int b = (color >> 16) & 255;
    return (255 - r) + ((255 - g) << 8) + ((255 - b) << 16);
  }
}
