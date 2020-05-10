package me.nov.threadtear.swing.image;

import java.awt.*;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

public class Images {
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

  public static ImageIcon combineSmall(ImageIcon icon1, ImageIcon icon2, boolean right) {
    Image img1 = icon1.getImage();
    Image img2 = icon2.getImage();

    int w = icon1.getIconWidth();
    int h = icon1.getIconHeight();
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = image.createGraphics();
    g2.drawImage(img1, 0, 0, null);
    g2.drawImage(img2.getScaledInstance(w / 2, h / 2, Image.SCALE_SMOOTH), w - w / 2, h - h / 2, null);
    g2.dispose();

    return new ImageIcon(image);
  }

  public static BufferedImage scaleImage(Image img, int w, int h) {
    BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Graphics2D g2 = resized.createGraphics();

    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g2.drawImage(img, 0, 0, w, h, null);
    g2.dispose();

    return resized;
  }

  public static BufferedImage watermark(BufferedImage old, String watermark) {
    BufferedImage copy = broadenImage(old);
    Graphics2D g2d = copy.createGraphics();
    g2d.setPaint(Color.black);
    g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    FontMetrics fm = g2d.getFontMetrics();
    int x = copy.getWidth() - fm.stringWidth(watermark) - 5;
    int y = fm.getHeight();
    g2d.drawString(watermark, x, y);
    g2d.dispose();
    return copy;
  }

  private static BufferedImage broadenImage(BufferedImage source) {
    BufferedImage b = new BufferedImage(source.getWidth() + 60, source.getHeight() + 60, source.getType());
    Graphics g = b.createGraphics();
    g.setColor(Color.WHITE);
    g.fillRect(0, 0, source.getWidth() + 60, source.getHeight() + 60);
    g.setColor(Color.BLACK);
    g.drawImage(source, 30, 30, null);
    g.dispose();
    return b;
  }
}
