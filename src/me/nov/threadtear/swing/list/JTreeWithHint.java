package me.nov.threadtear.swing.list;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

public class JTreeWithHint extends JTree {
	private static final long serialVersionUID = 1L;
	protected String hint;

	public JTreeWithHint(String hint) {
		this.hint = hint;
//		this.putClientProperty("JTree.alternateRowColor", true);
//		this.putClientProperty("JTree.lineStyle", "none");
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		DefaultMutableTreeNode tn = (DefaultMutableTreeNode) getModel().getRoot();
		if (tn.getChildCount() == 0) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
			g.drawString(hint, getWidth() / 2 - g.getFontMetrics().stringWidth(hint) / 2, getHeight() / 2);
		}
	}
}
