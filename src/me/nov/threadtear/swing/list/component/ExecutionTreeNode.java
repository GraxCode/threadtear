package me.nov.threadtear.swing.list.component;

import javax.swing.tree.DefaultMutableTreeNode;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionTag;

public class ExecutionTreeNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1L;

	public Execution member;
	private String text;

	public ExecutionTreeNode(Execution ex, boolean suffix) {
		this.member = ex;
		if (member != null) {
			this.text = member.name;
			if (suffix) {
				text += " (" + ex.type.name + ")";
			}
		}
	}

	public String getTooltip() {
		if (member == null)
			return null;
		StringBuilder b = new StringBuilder();
		b.append("<html><h3>" + member.name);
		b.append("</h3>");
		b.append(member.description);
		b.append("<br><i><b>");
		for (ExecutionTag tag : member.tags) {
			b.append("<br>");
			b.append(tag.info);
		}
		b.append("</b></i><br><br><tt>");
		b.append(member.getClass().getName());
		return b.toString();
	}

	public ExecutionTreeNode(String folder) {
		this.member = null;
		this.text = folder;
	}

	@Override
	public String toString() {
		return text;
	}
	
	
}
