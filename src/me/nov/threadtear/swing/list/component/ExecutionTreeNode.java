package me.nov.threadtear.swing.list.component;

import javax.swing.tree.DefaultMutableTreeNode;

import me.nov.threadtear.execution.Execution;

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
		return "<html>" + member.description + "<br>Class: " + member.getClass().getName();
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
