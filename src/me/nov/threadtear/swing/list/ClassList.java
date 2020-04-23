package me.nov.threadtear.swing.list;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.io.JarIO;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.dialog.JarAnalysis;
import me.nov.threadtear.swing.frame.DecompilerFrame;
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
	private JPanel outerPanel;

	public ClassList() {
		this.setLayout(new BorderLayout());
		this.add(outerPanel = Utils.addTitleAndBorder("Class list", new JScrollPane(tree = new ClassTree())), BorderLayout.CENTER);
		this.add(createButtons(), BorderLayout.SOUTH);
		this.setTransferHandler(new JarDropHandler(this));
	}

	@Override
	public Dimension getMinimumSize() {
		Dimension minSize = super.getMinimumSize();
		minSize.width = 150;
		return minSize;
	}

	private JPanel createButtons() {
		JPanel panel = new JPanel(new GridLayout(1, 4, 4, 4));
		panel.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
		JButton analysis = new JButton("Full analysis", IconLoader.get().loadSVGIcon("res/analysis.svg", false));
		analysis.addActionListener(l -> {
			new JarAnalysis(classes).setVisible(true);
		});
		panel.add(analysis);
		JButton decompile = new JButton("Decompile", IconLoader.get().loadSVGIcon("res/decompile.svg", false));
		decompile.addActionListener(l -> {
			SortedTreeClassNode tn = (SortedTreeClassNode) tree.getLastSelectedPathComponent();
			if (tn != null && tn.member != null) {
				new DecompilerFrame(tn.member.node).setVisible(true);
			}
		});
		panel.add(decompile);

		JButton ignore = new JButton("Ignore", IconLoader.get().loadSVGIcon("res/ignore.svg", false));
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
		JButton toggle = new JButton("Toggle all", IconLoader.get().loadSVGIcon("res/toggle.svg", false));
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
			outerPanel.setBorder(BorderFactory.createTitledBorder("Class list - " + classes.size() + " classes (" + disabled + " ignored)"));
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
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						SortedTreeClassNode tn = (SortedTreeClassNode) getLastSelectedPathComponent();
						if (tn != null && tn.member != null) {
							new DecompilerFrame(tn.member.node).setVisible(true);
						}
					}
				}
			});
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
		SortedTreeClassNode root = new SortedTreeClassNode("");
		model = new DefaultTreeModel(root);
		classes.forEach(c -> {
			String[] packages = c.node.name.split("/");
			addToTree((SortedTreeClassNode) model.getRoot(), c, packages, 0);
		});
		@SuppressWarnings("unchecked")
		Enumeration<SortedTreeClassNode> e = root.depthFirstEnumeration();
		while (e.hasMoreElements()) {
			SortedTreeClassNode node = e.nextElement();
			if (!node.isLeaf()) {
				node.sort();
			}
		}
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
