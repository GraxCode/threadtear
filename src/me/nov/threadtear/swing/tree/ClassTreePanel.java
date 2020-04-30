package me.nov.threadtear.swing.tree;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.io.JarIO;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.dialog.JarAnalysis;
import me.nov.threadtear.swing.frame.BytecodeFrame;
import me.nov.threadtear.swing.frame.DecompilerFrame;
import me.nov.threadtear.swing.handler.ILoader;
import me.nov.threadtear.swing.handler.JarDropHandler;
import me.nov.threadtear.swing.tree.component.SortedTreeClassNode;
import me.nov.threadtear.swing.tree.renderer.ClassTreeCellRenderer;

public class ClassTreePanel extends JPanel implements ILoader {
  private static final long serialVersionUID = 1L;
  public File inputFile;
  public ArrayList<Clazz> classes;
  public DefaultTreeModel model;
  private ClassTree tree;
  private JPanel outerPanel;

  private JButton analysis;
  private JButton decompile;
  private JButton bytecode;
  private JButton ignore;

  public ClassTreePanel() {
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
    analysis = new JButton("Full analysis", IconLoader.get().loadSVGIcon("res/analysis.svg", false));
    analysis.addActionListener(l -> {
      new JarAnalysis(classes).setVisible(true);
    });
    analysis.setEnabled(false);
    panel.add(analysis);
    decompile = new JButton("Decompile", IconLoader.get().loadSVGIcon("res/decompile.svg", false));
    decompile.addActionListener(l -> {
      SortedTreeClassNode tn = (SortedTreeClassNode) tree.getLastSelectedPathComponent();
      if (tn != null && tn.member != null) {
        new DecompilerFrame(tn.member.node).setVisible(true);
      }
    });
    panel.add(decompile);
    decompile.setEnabled(false);
    bytecode = new JButton("Bytecode", IconLoader.get().loadSVGIcon("res/bytecode.svg", false));
    bytecode.addActionListener(l -> {
      SortedTreeClassNode tn = (SortedTreeClassNode) tree.getLastSelectedPathComponent();
      if (tn != null && tn.member != null) {
        new BytecodeFrame(tn.member.node).setVisible(true);
      }
    });

    panel.add(bytecode);
    bytecode.setEnabled(false);
    ignore = new JButton("Ignore", IconLoader.get().loadSVGIcon("res/ignore.svg", false));
    ignore.addActionListener(l -> {
      TreePath[] paths = tree.getSelectionPaths();
      for (int i = 0; i < paths.length; i++) {
        SortedTreeClassNode tn = (SortedTreeClassNode) paths[i].getLastPathComponent();
        ignoreChilds(tn);
      }
      refreshIgnored();
      tree.grabFocus();
    });
    ignore.setEnabled(false);
    panel.add(ignore);
    return panel;
  }

  public void refreshIgnored() {
    if (classes != null) {
      long disabled = classes.stream().filter(c -> !c.transform).count();
      outerPanel.setBorder(BorderFactory.createTitledBorder("Class list - " + classes.size() + " classes (" + disabled + " ignored)"));
    }
    repaint();
  }

  public void ignore(String className) {
    classes.stream().filter(c -> c.node.name.equals(className)).forEach(c -> c.transform = false);
  }

  public void updateAllNames(SortedTreeClassNode root) {
    root.updateClassName();
    for (int i = 0; i < root.getChildCount(); i++) {
      SortedTreeClassNode child = (SortedTreeClassNode) root.getChildAt(i);
      updateAllNames(child);
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
      this.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
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
      this.addTreeSelectionListener(l -> {
        SortedTreeClassNode tn = (SortedTreeClassNode) getLastSelectedPathComponent();
        boolean selected = tn != null;
        decompile.setEnabled(selected && tn.member != null);
        bytecode.setEnabled(selected && tn.member != null);
        ignore.setEnabled(selected);
      });
    }
  }

  @Override
  public void onJarLoad(File input) {
    tree.isLoading = true;
    tree.setModel(new DefaultTreeModel((TreeNode) tree.getModel().getRoot()));
    tree.invalidate();
    tree.validate();
    tree.repaint();
    try {
      SwingUtilities.invokeLater(() -> {
        Thread loadingThread = new Thread(() -> {
          this.inputFile = input;
          try {
            this.classes = JarIO.loadClasses(input);
            if (classes.stream().anyMatch(c -> c.oldEntry.getCertificates() != null)) {
              JOptionPane.showMessageDialog(this,
                  "<html>Warning: File is signed and may not load correctly if already modified, remove the signature<br>(<tt>META-INF\\MANIFEST.MF</tt>) and certificates (<tt>META-INF\\*.SF/.RSA</tt>) first!",
                  "Signature warning", JOptionPane.WARNING_MESSAGE);
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
          loadTree(classes);
          refreshIgnored();
          model.reload();
          analysis.setEnabled(true);
          tree.isLoading = false;
        });
        loadingThread.start();
      });
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
