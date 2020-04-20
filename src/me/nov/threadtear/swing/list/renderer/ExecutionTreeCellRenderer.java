package me.nov.threadtear.swing.list.renderer;

import java.awt.Component;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.objectweb.asm.Opcodes;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.swing.list.component.ExecutionTreeNode;

public class ExecutionTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
	private static final long serialVersionUID = 1L;

	private Icon executionRed, executionGreen, executionBlue, directory;

	public ExecutionTreeCellRenderer() {
		this.directory = IconLoader.get().loadSVGIcon("/res/folder.svg", false);
		Toolkit tk = Toolkit.getDefaultToolkit();
		this.executionRed = new ImageIcon(tk.getImage(this.getClass().getResource("/res/execution_red.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.executionGreen = new ImageIcon(tk.getImage(this.getClass().getResource("/res/execution_green.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.executionBlue = new ImageIcon(tk.getImage(this.getClass().getResource("/res/execution_blue.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node instanceof ExecutionTreeNode) {
			ExecutionTreeNode tn = ((ExecutionTreeNode) node);
			Execution exec = tn.member;
			setToolTipText(tn.getTooltip());
			if (exec != null) {
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