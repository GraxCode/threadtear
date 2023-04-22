package me.nov.threadtear.swing;

import com.github.weisj.darklaf.components.OverlayScrollPane;
import com.github.weisj.darklaf.components.border.DarkBorders;
import com.github.weisj.darklaf.properties.icons.IconLoader;
import com.github.weisj.darklaf.ui.button.DarkButtonUI;
import me.nov.threadtear.Threadtear;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionListener;

public class SwingUtils {

  private static final IconLoader ICON_LOADER = IconLoader.get(Threadtear.class);

  public static TitledPanel withTitleAndBorder(String title, JComponent c) {
    Border border = DarkBorders.createLineBorder(1, 1, 1, 1);
    return new TitledPanel(title, withBorder(wrap(c), border));
  }

  public static GridBagConstraints createGridBagConstraints(int x, int y) {
    return createGridBagConstraints(x, y, false);
  }

  public static OverlayScrollPane createRSyntaxOverlayScrollPane(DecompilerTextArea textArea) {
    RTextScrollPane sp = new RTextScrollPane(textArea);
    OverlayScrollPane overlayScrollPane = new OverlayScrollPane(sp);
    overlayScrollPane.getVerticalScrollBar().setUnitIncrement(16);
    sp.setLineNumbersEnabled(true);
    return overlayScrollPane;
  }

  public static GridBagConstraints createGridBagConstraints(int x, int y, boolean fullWidth) {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = x;
    gbc.gridy = y;
    gbc.gridwidth = 1;
    gbc.gridheight = 1;

    if (fullWidth) {
      gbc.fill = GridBagConstraints.HORIZONTAL;
      gbc.gridwidth = GridBagConstraints.REMAINDER;
    }

    gbc.anchor = (x == 0) ? GridBagConstraints.WEST : GridBagConstraints.EAST;
    gbc.fill = (x == 0) ? GridBagConstraints.BOTH
      : GridBagConstraints.HORIZONTAL;

    gbc.weightx = (x == 0) ? 0.1 : 1.0;
    gbc.weighty = 1.0;
    return gbc;
  }

  public static JComponent pad(JComponent comp, int top, int left, int bottom, int right) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(comp, BorderLayout.CENTER);
    if (top > 0) {
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(0, top));
      panel.add(p, BorderLayout.NORTH);
    }
    if (bottom > 0) {
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(0, bottom));
      panel.add(p, BorderLayout.SOUTH);
    }
    if (left > 0) {
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(left, 0));
      panel.add(p, BorderLayout.WEST);
    }
    if (right > 0) {
      JPanel p = new JPanel();
      p.setPreferredSize(new Dimension(right, 0));
      panel.add(p, BorderLayout.EAST);
    }
    return panel;
  }

  public static JComponent horizontallyDivided(JComponent top, JComponent bottom) {
    JPanel content = new JPanel(new BorderLayout());
    JPanel topHolder = new JPanel(new BorderLayout());
    topHolder.add(top, BorderLayout.CENTER);
    topHolder.add(SwingUtils.createHorizontalSeparator(8), BorderLayout.SOUTH);
    content.add(topHolder, BorderLayout.CENTER);
    content.add(bottom, BorderLayout.SOUTH);
    return content;
  }

  public static JComponent verticallyDivided(JComponent left, JComponent right) {
    JPanel content = new JPanel(new BorderLayout());
    JPanel leftHolder = new JPanel(new BorderLayout());
    leftHolder.add(left, BorderLayout.CENTER);
    leftHolder.add(SwingUtils.createVerticalSeparator(8), BorderLayout.EAST);
    content.add(leftHolder, BorderLayout.CENTER);
    content.add(right, BorderLayout.EAST);
    return content;
  }

  public static JComponent alignBottom(JComponent component) {
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.SOUTH);
    return panel;
  }

  public static JComponent createHorizontalSeparator() {
    return createHorizontalSeparator(0);
  }

  public static JComponent createHorizontalSeparator(int padding) {
    return withEmptyBorder(wrap(new JSeparator(JSeparator.HORIZONTAL)), padding, 0, padding, 0);
  }

  public static JComponent createVerticalSeparator() {
    return createVerticalSeparator(0);
  }

  public static JComponent createVerticalSeparator(int padding) {
    return withEmptyBorder(wrap(new JSeparator(JSeparator.VERTICAL)), 0, padding, 0, padding);
  }

  public static <T extends JComponent> T withEmptyBorder(T comp, int pad) {
    return withEmptyBorder(comp, pad, pad, pad, pad);
  }

  public static <T extends JComponent> T withEmptyBorder(T comp, int top, int left, int bottom, int right) {
    return withBorder(comp, BorderFactory.createEmptyBorder(top, left, bottom, right));
  }

  public static <T extends JComponent> T withBorder(T comp, Border border) {
    comp.setBorder(border);
    return comp;
  }

  public static JComponent wrap(final JComponent component) {
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.add(component);
    return wrap;
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

  public static Image iconToFrameImage(Icon icon, Window window) {
    return IconLoader.createFrameIcon(icon, window);
  }

  public static Icon getIcon(String path) {
    return getIcon(path, false);
  }

  public static Icon getIcon(String path, boolean themed) {
    return ICON_LOADER.getIcon(path, themed);
  }

  public static Icon getIcon(String path, int width, int height) {
    return ICON_LOADER.getIcon(path, width, height, false);
  }

  public static Icon getIcon(String path, int width, int height, boolean themed) {
    return ICON_LOADER.getIcon(path, width, height, themed);
  }

  public static JButton createSlimButton(Icon icon, ActionListener l) {
    JButton jButton = new JButton(icon);
    jButton.putClientProperty(DarkButtonUI.KEY_NO_BORDERLESS_OVERWRITE, true);
    jButton.putClientProperty(DarkButtonUI.KEY_VARIANT, DarkButtonUI.VARIANT_BORDERLESS);
    jButton.putClientProperty(DarkButtonUI.KEY_THIN, true);
    jButton.putClientProperty(DarkButtonUI.KEY_SQUARE, true);
    if (l != null)
      jButton.addActionListener(l);
    return jButton;
  }
}
