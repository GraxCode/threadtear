package me.nov.threadtear.swing.list;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.io.JarIO;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.handler.ILoader;
import me.nov.threadtear.swing.handler.JarDropHandler;
import me.nov.threadtear.swing.list.component.SortedTreeClassNode;
import me.nov.threadtear.swing.list.renderer.ClassTreeCellRenderer;

public class ClassList extends JPanel implements ILoader {
	private static final long serialVersionUID = 1L;
	public File inputFile;
	public ArrayList<Clazz> classes;
	public DefaultTreeModel model;
	private ClassTree tree;
	private JLabel ignored;

	public ClassList() {
		this.setLayout(new BorderLayout());
		this.add(Utils.addTitleAndBorder("Classes", new JScrollPane(tree = new ClassTree())), BorderLayout.CENTER);
		this.add(createButtons(), BorderLayout.SOUTH);
		this.setTransferHandler(new JarDropHandler(this));
	}

	private JPanel createButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 4, 4));
		ignored = new JLabel("", SwingConstants.CENTER);
		panel.add(ignored);
		panel.add(new JPanel());
		JButton ignore = new JButton("Ignore");
		ignore.addActionListener(l -> {
			SortedTreeClassNode node = (SortedTreeClassNode) tree.getLastSelectedPathComponent();
			if (node != null) {
				ignoreChilds(node);
				refreshIgnored();
				repaint();
				tree.grabFocus();
			}
		});
		panel.add(ignore);
		JButton toggle = new JButton("Toggle All");
		toggle.addActionListener(l -> {
			ignoreChilds((SortedTreeClassNode) model.getRoot());
			refreshIgnored();
			repaint();
			tree.grabFocus();
		});

		panel.add(toggle);
		return panel;
	}

	private void refreshIgnored() {
		if (classes != null) {
			long disabled = classes.stream().filter(c -> !c.transform).count();
			ignored.setText("<html>" + classes.size() + " classes<br>" + disabled + " ignored");
		}
	}

	private void ignoreChilds(SortedTreeClassNode node) {
		if (node.member != null) {
			node.member.transform = !node.member.transform;
			node.updateClassName();
		} else {
			for (int i = 0; i < node.getChildCount(); i++) {
				SortedTreeClassNode child = (SortedTreeClassNode) node.getChildAt(i);
				ignoreChilds(child);
			}
		}
	}

	public class ClassTree extends JTreeWithHint {
		private static final long serialVersionUID = 1L;

		public ClassTree() {
			super("Drag a java archive file here");
			this.setRootVisible(false);
			this.setShowsRootHandles(true);
			this.setFocusable(true);
			this.setCellRenderer(new ClassTreeCellRenderer());
			model = new DefaultTreeModel(new SortedTreeClassNode(""));
			this.setModel(model);
			this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		}
	}

	@Override
	public void onJarLoad(File input) {
		try {
			this.inputFile = input;
			try {
				this.classes = JarIO.loadClasses(input);
			} catch (IOException e) {
				e.printStackTrace();
			}
			loadTree(classes);
			refreshIgnored();
			model.reload();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void loadTree(ArrayList<Clazz> classes) {
		model = new DefaultTreeModel(new SortedTreeClassNode(""));
		classes.forEach(c -> {
			String[] packages = c.node.name.split("/");
			addToTree((SortedTreeClassNode) model.getRoot(), c, packages, 0);
		});
		tree.setModel(model);
	}

	public void addToTree(SortedTreeClassNode current, Clazz c, String[] packages, int pckg) {
		String node = packages[pckg];
		if (packages.length - pckg <= 1) {
			current.add(new SortedTreeClassNode(c));
			return;
		}
		for (int i = 0; i < current.getChildCount(); i++) {

			SortedTreeClassNode child = (SortedTreeClassNode) current.getChildAt(i);
			if (child.toString().equals(node)) {
				addToTree(child, c, packages, ++pckg);
				return;
			}
		}
		SortedTreeClassNode newChild = new SortedTreeClassNode(node);
		current.add(newChild);
		addToTree(newChild, c, packages, ++pckg);
	}
}
