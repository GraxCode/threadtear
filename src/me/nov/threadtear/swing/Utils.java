package me.nov.threadtear.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class Utils {
  public static JPanel addTitleAndBorder(String title, Component c) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.setBorder(BorderFactory.createTitledBorder(title));
    JPanel panel2 = new JPanel(new BorderLayout());
    panel2.add(c, BorderLayout.CENTER);
    panel2.setBorder(BorderFactory.createLoweredBevelBorder());
    panel.add(panel2, BorderLayout.CENTER);
    return panel;
  }

  public static void moveTreeItem(JTree tree, int direction) {
    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
    MutableTreeNode moveNode = (MutableTreeNode) tree.getLastSelectedPathComponent();
    if (moveNode == null)
      return;
    MutableTreeNode parent = (MutableTreeNode) moveNode.getParent();
    if (parent == null)
      return;
    int targetIndex = model.getIndexOfChild(parent, moveNode) + direction;
    if (targetIndex < 0 || targetIndex >= parent.getChildCount())
      return;
    model.removeNodeFromParent(moveNode);
    model.insertNodeInto(moveNode, parent, targetIndex);
    // make the node visible by scroll to it
    TreeNode[] nodes = model.getPathToRoot(moveNode);
    TreePath path = new TreePath(nodes);
    tree.scrollPathToVisible(path);
    // select the newly added node
    tree.setSelectionPath(path);
  }

  public static String getVersion() {
    try {
      return Objects.requireNonNull(Utils.class.getPackage().getImplementationVersion());
    } catch (NullPointerException e) {
      return "(dev)";
    }
  }

  public static Image iconToImage(Icon icon) {
    if (icon instanceof ImageIcon) {
      return ((ImageIcon) icon).getImage();
    } else {
      BufferedImage image = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
      icon.paintIcon(null, image.getGraphics(), 0, 0);
      return image;
    }
  }
}
