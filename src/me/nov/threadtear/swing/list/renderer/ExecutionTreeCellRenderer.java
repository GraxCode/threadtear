package me.nov.threadtear.swing.list.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.objectweb.asm.Opcodes;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.swing.list.component.ExecutionTreeNode;

public class ExecutionTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
	private static final long serialVersionUID = 1L;

	private ImageIcon execution, directory;

	public ExecutionTreeCellRenderer() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		this.directory = new ImageIcon(
				tk.getImage(this.getClass().getResource("/res/directory.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.execution = new ImageIcon(
				tk.getImage(this.getClass().getResource("/res/execution.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel,
			final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node instanceof ExecutionTreeNode) {
			ExecutionTreeNode tn = ((ExecutionTreeNode) node);
			Execution exec = (Execution) tn.member;
			setToolTipText(tn.getTooltip());
			if (exec != null) {
				this.setIcon(this.execution);
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