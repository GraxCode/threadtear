package me.nov.threadtear.swing.tree.renderer;

import java.awt.*;

import javax.swing.*;
import javax.swing.tree.*;

import me.nov.threadtear.swing.Utils;
import org.objectweb.asm.Opcodes;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.swing.tree.component.ExecutionTreeNode;

public class ExecutionTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
  private static final long serialVersionUID = 1L;

  private Icon executionRed, executionGreen, executionBlue, executionPurple, directory;

  public ExecutionTreeCellRenderer() {
    this.directory = Utils.getIcon("res/folder.svg", true);
    this.executionRed = Utils.getIcon("res/execution_red.svg");
    this.executionGreen = Utils.getIcon("res/execution_green.svg");
    this.executionBlue = Utils.getIcon("res/execution_blue.svg");
    this.executionPurple = Utils.getIcon("res/execution_purple.svg");

  }

  @Override
  public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
                                                final boolean expanded, final boolean leaf, final int row,
                                                final boolean hasFocus) {
    super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
    if (node instanceof ExecutionTreeNode) {
      ExecutionTreeNode tn = ((ExecutionTreeNode) node);
      Execution exec = tn.member;
      setToolTipText(tn.getTooltip());
      if (exec != null) {
        if (exec.type.name.startsWith("Obfuscators.")) {
          this.setIcon(this.executionPurple);
        } else {
          switch (exec.type) {
            case ANALYSIS:
              this.setIcon(this.executionGreen);
              break;
            case CLEANING:
            case TOOLS:
              this.setIcon(this.executionBlue);
              break;
            default:
              this.setIcon(this.executionRed);
              break;
          }
        }
      } else {
        this.setIcon(this.directory);
      }
    }
    return this;
  }

  @Override
  public Font getFont() {
    return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
  }
}