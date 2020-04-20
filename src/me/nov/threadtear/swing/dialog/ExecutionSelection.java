package me.nov.threadtear.swing.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.execution.ExecutionCategory;
import me.nov.threadtear.execution.allatori.ExpirationDateRemoverAllatori;
import me.nov.threadtear.execution.allatori.StringObfuscationAllatori;
import me.nov.threadtear.execution.analysis.ReobfuscateClassNames;
import me.nov.threadtear.execution.analysis.ReobfuscateMembers;
import me.nov.threadtear.execution.analysis.RestoreSourceFiles;
import me.nov.threadtear.execution.cleanup.InlineMethods;
import me.nov.threadtear.execution.cleanup.remove.RemoveUnnecessary;
import me.nov.threadtear.execution.generic.ConvertCompareInstructions;
import me.nov.threadtear.execution.generic.KnownConditionalJumps;
import me.nov.threadtear.execution.generic.ObfuscatedAccess;
import me.nov.threadtear.execution.stringer.AccessObfusationStringer;
import me.nov.threadtear.execution.stringer.StringObfuscationStringer;
import me.nov.threadtear.execution.tools.IsolatePossiblyMalicious;
import me.nov.threadtear.execution.tools.Java7Compatibility;
import me.nov.threadtear.execution.tools.Java8Compatibility;
import me.nov.threadtear.execution.zkm.AccessObfusationZKM;
import me.nov.threadtear.execution.zkm.StringObfuscationZKM;
import me.nov.threadtear.execution.zkm.TryCatchObfuscationRemover;
import me.nov.threadtear.swing.list.component.ExecutionTreeNode;
import me.nov.threadtear.swing.list.renderer.ExecutionTreeCellRenderer;

public class ExecutionSelection extends JDialog {
	private static final long serialVersionUID = 1L;
	public JTree tree = null;
	private JButton ok;

	public ExecutionSelection() {
		setModalityType(ModalityType.APPLICATION_MODAL);
		setTitle("Select execution");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		getContentPane().setLayout(new BorderLayout());
		JPanel cp = new JPanel();
		cp.setBorder(new EmptyBorder(10, 10, 10, 10));
		getContentPane().add(cp, BorderLayout.CENTER);
		cp.setLayout(new BorderLayout(0, 0));
		tree = new ExecutionSelectionTree();
		JPanel treePanel = new JPanel(new BorderLayout());
		treePanel.setBorder(BorderFactory.createLoweredBevelBorder());
		treePanel.add(new JScrollPane(tree), BorderLayout.CENTER);
		cp.add(treePanel);
		JPanel buttons = new JPanel();
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(e -> {
			tree.clearSelection();
			dispose();
		});
		buttons.add(cancel);
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		getContentPane().add(buttons, BorderLayout.SOUTH);
		ok = new JButton("OK");
		ok.setEnabled(false);
		ok.addActionListener(e -> {
			dispose();
		});
		ok.setActionCommand("OK");
		buttons.add(ok);
		getRootPane().setDefaultButton(ok);
		cancel.setActionCommand("Cancel");

	}

	public class ExecutionSelectionTree extends JTree implements TreeSelectionListener {
		private static final long serialVersionUID = 1L;

		public ExecutionSelectionTree() {
			this.setRootVisible(false);
			this.setShowsRootHandles(true);
			this.setFocusable(true);
			this.setCellRenderer(new ExecutionTreeCellRenderer());
			ExecutionTreeNode root = new ExecutionTreeNode("");
			DefaultTreeModel model = new DefaultTreeModel(root);
			this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
			for (ExecutionCategory t : ExecutionCategory.values()) {
				root.add(new ExecutionTreeNode(t.name));
			}
			addExecution(root, new InlineMethods());
			addExecution(root, new RemoveUnnecessary());

			addExecution(root, new ObfuscatedAccess());
			addExecution(root, new KnownConditionalJumps());
			addExecution(root, new ConvertCompareInstructions());
			
			addExecution(root, new RestoreSourceFiles());
			addExecution(root, new ReobfuscateClassNames());
			addExecution(root, new ReobfuscateMembers());

			addExecution(root, new StringObfuscationStringer());
			addExecution(root, new AccessObfusationStringer());

			addExecution(root, new TryCatchObfuscationRemover());
			addExecution(root, new StringObfuscationZKM());
			addExecution(root, new AccessObfusationZKM());

			addExecution(root, new StringObfuscationAllatori());
			addExecution(root, new ExpirationDateRemoverAllatori());

			addExecution(root, new Java7Compatibility());
			addExecution(root, new Java8Compatibility());
			addExecution(root, new IsolatePossiblyMalicious());

			this.setModel(model);
			ToolTipManager.sharedInstance().registerComponent(this);
			this.addTreeSelectionListener(this);
			this.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2) {
						ExecutionTreeNode tn = (ExecutionTreeNode) getLastSelectedPathComponent();
						if (tn != null && tn.member != null)
							ExecutionSelection.this.dispose();
					}
				}
			});
		}

		private void addExecution(ExecutionTreeNode root, Execution e) {
			for (int i = 0; i < root.getChildCount(); i++) {

				ExecutionTreeNode child = (ExecutionTreeNode) root.getChildAt(i);
				if (child.toString().equalsIgnoreCase(e.type.name)) {
					child.add(new ExecutionTreeNode(e, false));
					return;
				}
			}
		}

		@Override
		public void valueChanged(TreeSelectionEvent e) {
			ExecutionTreeNode node = (ExecutionTreeNode) tree.getLastSelectedPathComponent();
			ok.setEnabled(node != null && node.member != null);
		}
	}
}
