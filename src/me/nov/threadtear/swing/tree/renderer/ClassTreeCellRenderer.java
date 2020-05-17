package me.nov.threadtear.swing.tree.renderer;

import java.awt.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.tree.*;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.tree.component.ClassTreeNode;
import me.nov.threadtear.util.asm.Access;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
  private static final long serialVersionUID = 1L;

  private static final Icon pack, clazz, enu, itf, failOverlay, ignoreOverlay;

  static {
    pack = IconLoader.get().loadSVGIcon("res/package.svg", false);
    clazz = IconLoader.get().loadSVGIcon("res/class.svg", false);
    enu = IconLoader.get().loadSVGIcon("res/enum.svg", false);
    itf = IconLoader.get().loadSVGIcon("res/interface.svg", false);
    failOverlay = IconLoader.get().loadSVGIcon("res/failure.svg", 10, 10, false);
    ignoreOverlay = IconLoader.get().loadSVGIcon("res/ignore.svg", 10, 10, false);
  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
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
          this.setIcon(clazz);
        }
        if (!member.failures.isEmpty()) {
          this.setToolTipText("<font color=\"#ff6b6b\">" + member.failures.stream().collect(Collectors.joining("<br><hr><font color=\"#ff6b6b\">")));
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
    return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  }
}
