package me.nov.threadtear.swing.tree;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class JTreeWithHint extends JTree {
  private static final long serialVersionUID = 1L;
  protected String hint;
  protected boolean isLoading;

  public JTreeWithHint(String hint) {
    this.hint = hint;
//		this.putClientProperty("JTree.alternateRowColor", true);
//		this.putClientProperty("JTree.lineStyle", "none");
  }

  private Image loading = Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/res/spin.gif")).getScaledInstance(32, 32, Image.SCALE_DEFAULT);

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    DefaultMutableTreeNode tn = (DefaultMutableTreeNode) getModel().getRoot();
    ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    if (isLoading) {
      g.drawImage(loading, getWidth() / 2 - 16, getHeight() / 2 - 16, new NodeImageObserver());
    } else if (tn.getChildCount() == 0) {
      g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
      g.drawString(hint, getWidth() / 2 - g.getFontMetrics().stringWidth(hint) / 2, getHeight() / 2);
    }
  }

  public class NodeImageObserver implements ImageObserver {

    public boolean imageUpdate(Image img, int flags, int x, int y, int w, int h) {
      if ((flags & (FRAMEBITS | ALLBITS)) != 0) {
        repaint();
      }
      return (flags & (ALLBITS | ABORT)) == 0;
    }
  }
}
