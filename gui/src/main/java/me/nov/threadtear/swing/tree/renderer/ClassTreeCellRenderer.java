package me.nov.threadtear.swing.tree.renderer;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import me.nov.threadtear.swing.SwingUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.tree.component.ClassTreeNode;
import me.nov.threadtear.util.asm.Access;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
  private static final long serialVersionUID = 1L;

  private static final Icon pack, clazz, innerClazz, mainClazz, enu, itf, failOverlay, ignoreOverlay;

  static {
    pack = SwingUtils.getIcon("package.svg", true);
    clazz = SwingUtils.getIcon("class.svg");
    innerClazz = SwingUtils.getIcon("innerClass.svg");
    mainClazz = SwingUtils.getIcon("mainClass.svg");
    enu = SwingUtils.getIcon("enum.svg");
    itf = SwingUtils.getIcon("interface.svg");
    failOverlay = SwingUtils.getIcon("failure.svg", 10, 10, true);
    ignoreOverlay = SwingUtils.getIcon("ignore.svg", 10, 10, true);
  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
                                                final boolean expanded, final boolean leaf, final int row,
                                                final boolean hasFocus) {
    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    if (node instanceof ClassTreeNode) {
      Clazz member = ((ClassTreeNode) node).member;
      if (member != null) {
        ClassNode cn = member.node;
        if (Access.isInterface(cn.access)) {
          this.setIcon(itf);
        } else if (Access.isEnum(cn.access)) {
          this.setIcon(enu);
        } else {
          if (cn.methods.stream().anyMatch(mn -> mn.name.equals("main") && mn.desc.equals("([Ljava/lang/String;)V"))) {
            this.setIcon(mainClazz);
          } else if (cn.name.contains("$") && cn.outerClass != null) {
            this.setIcon(innerClazz);
          } else {
            this.setIcon(clazz);
          }
        }
        if (!member.failures.isEmpty()) {
          this.setToolTipText(
                  "<html><font color=\"#ff6b6b\">" + String.join("<br><hr><font color=\"#ff6b6b\">", member.failures));
          this.setIcon(new OverlayIcon(this.getIcon(), failOverlay));
        } else if (!member.transform) {
          this.setIcon(new OverlayIcon(this.getIcon(), ignoreOverlay));
        }
      } else {
        this.setIcon(pack);
      }
    }
    return this;
  }

  @Override
  public Font getFont() {
    return new Font(Font.SANS_SERIF, Font.PLAIN, new JLabel().getFont().getSize());
  }
}
