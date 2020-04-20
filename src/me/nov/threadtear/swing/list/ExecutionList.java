package me.nov.threadtear.swing.list;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.dialog.ExecutionSelection;
import me.nov.threadtear.swing.list.component.ExecutionTreeNode;
import me.nov.threadtear.swing.list.renderer.ExecutionTreeCellRenderer;

public class ExecutionList extends JPanel {
	private static final long serialVersionUID = 1L;
	public DefaultTreeModel model;
	private ExecutionTree executions;

	public ExecutionList() {
		this.setLayout(new BorderLayout());
		this.add(Utils.addTitleAndBorder("Executions in order", new JScrollPane(executions = new ExecutionTree())), BorderLayout.CENTER);

		this.add(createButtons(), BorderLayout.SOUTH);
	}

	private JPanel createButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 4, 4));
		panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		JButton add = new JButton("Add");
		add.addActionListener(e -> {
			ExecutionSelection es = new ExecutionSelection();
			es.setVisible(true);
			ExecutionTreeNode node = (ExecutionTreeNode) es.tree.getLastSelectedPathComponent();
			if (node != null && node.member != null) {
				((ExecutionTreeNode) model.getRoot()).add(new ExecutionTreeNode(node.member, true));
				model.reload();
				executions.repaint();
			}
		});
		panel.add(add);
		JButton remove = new JButton("Remove");
		remove.addActionListener(e -> {
			ExecutionTreeNode node = (ExecutionTreeNode) executions.getLastSelectedPathComponent();
			if (node != null && node.member != null) {
				model.removeNodeFromParent(node);
				model.reload();
				executions.repaint();
			}
		});
		panel.add(remove);
		JButton up = new JButton("Move up");
		up.addActionListener(e -> {
			Utils.moveTreeItem(executions, -1);
			executions.grabFocus();
		});
		panel.add(up);
		JButton down = new JButton("Move down");
		down.addActionListener(e -> {
			Utils.moveTreeItem(executions, 1);
			executions.grabFocus();
		});
		panel.add(down);
		return panel;
	}

	public class ExecutionTree extends JTreeWithHint {
		private static final long serialVersionUID = 1L;

		public ExecutionTree() {
			super("Select your executions here");
			this.setRootVisible(false);
			this.setShowsRootHandles(true);
			this.setFocusable(true);
			this.setCellRenderer(new ExecutionTreeCellRenderer());
			ExecutionTreeNode root = new ExecutionTreeNode("");
			model = new DefaultTreeModel(root);
			this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			this.setModel(model);
			ToolTipManager.sharedInstance().registerComponent(this);
		}
	}

	public ArrayList<Execution> getExecutions() {
		ArrayList<Execution> list = new ArrayList<>();
		ExecutionTreeNode root = (ExecutionTreeNode) model.getRoot();
		for (int i = 0; i < root.getChildCount(); i++) {
			ExecutionTreeNode child = (ExecutionTreeNode) root.getChildAt(i);
			assert (child.member != null);
			list.add(child.member);
		}
		return list;
	}
}
