package me.nov.threadtear.swing.list.renderer;

import java.awt.Component;
import java.awt.Font;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.list.component.SortedTreeClassNode;
import me.nov.threadtear.util.asm.Access;

public class ClassTreeCellRenderer extends DefaultTreeCellRenderer implements Opcodes {
	private static final long serialVersionUID = 1L;

	private Icon pack, clazz, enu, itf;

	public ClassTreeCellRenderer() {
		this.pack = IconLoader.get().loadSVGIcon("/res/package.svg", false);
		this.clazz = IconLoader.get().loadSVGIcon("/res/class.svg", false);
		this.enu = IconLoader.get().loadSVGIcon("/res/enum.svg", false);
		this.itf = IconLoader.get().loadSVGIcon("/res/interface.svg", false);
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
