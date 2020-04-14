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
import org.objectweb.asm.tree.ClassNode;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.util.Access;
import me.nov.threadtear.swing.list.component.SortedTreeClassNode;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
	private static final long serialVersionUID = 1L;

	private ImageIcon pack, clazz, enu, itf;

	public ClassTreeCellRenderer() {
		Toolkit tk = Toolkit.getDefaultToolkit();
		this.pack = new ImageIcon(tk.getImage(this.getClass().getResource("/res/package.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.clazz = new ImageIcon(tk.getImage(this.getClass().getResource("/res/class.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.enu = new ImageIcon(tk.getImage(this.getClass().getResource("/res/enum.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
		this.itf = new ImageIcon(tk.getImage(this.getClass().getResource("/res/interface.png")).getScaledInstance(16, 16, Image.SCALE_SMOOTH));
	}

	@Override
	public Component getTreeCellRendererComponent(final JTree tree, final Object value, final boolean sel, final boolean expanded, final boolean leaf, final int row, final boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node instanceof SortedTreeClassNode) {
			Clazz clazz = ((SortedTreeClassNode) node).member;
			if (clazz != null) {
				ClassNode cn = clazz.node;
				if (Access.isInterface(cn.access)) {
					this.setIcon(this.itf);
				} else if (Access.isEnum(cn.access)) {
					this.setIcon(this.enu);
				} else {
					this.setIcon(this.clazz);
				}
			} else {
				this.setIcon(this.pack);
			}
		}
		return this;
	}

	@Override
	public Font getFont() {
		return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
	}
}
