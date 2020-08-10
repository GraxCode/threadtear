package me.nov.threadtear.swing.tree;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;

public class JTreeWithHint extends JTree {
  private static final long serialVersionUID = 1L;
  private final JLabel hintRenderer;

  public JTreeWithHint(String hint) {
    hintRenderer = new JLabel(hint);
    hintRenderer.setOpaque(false);
    //this.putClientProperty("JTree.alternateRowColor", true);
    //this.putClientProperty("JTree.lineStyle", "none");
  }

  @Override
  public void doLayout() {
    super.doLayout();
    hintRenderer.setSize(hintRenderer.getPreferredSize());
    hintRenderer.doLayout();
  }

  @Override
  public void updateUI() {
    super.updateUI();
    if (hintRenderer != null) hintRenderer.updateUI();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    DefaultMutableTreeNode tn = (DefaultMutableTreeNode) getModel().getRoot();
    if (tn.getChildCount() == 0) {
      int x = (getWidth() - hintRenderer.getWidth()) / 2;
      int y = (getHeight() - hintRenderer.getHeight()) / 2;
      g.translate(x, y);
      hintRenderer.setEnabled(isEnabled());
      hintRenderer.paint(g);
    }
  }
}
