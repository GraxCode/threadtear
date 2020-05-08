package me.nov.threadtear.swing.tree;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.jar.JarEntry;

import javax.swing.*;
import javax.swing.tree.*;

import org.apache.commons.io.FilenameUtils;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.io.*;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.dialog.JarAnalysis;
import me.nov.threadtear.swing.frame.*;
import me.nov.threadtear.swing.handler.*;
import me.nov.threadtear.swing.tree.component.SortedClassTreeNode;
import me.nov.threadtear.swing.tree.renderer.ClassTreeCellRenderer;
import me.nov.threadtear.util.Strings;

public class ClassTreePanel extends JPanel implements ILoader {
  private static final long serialVersionUID = 1L;
  private Threadtear threadtear;
  public File inputFile;
  public List<Clazz> classes;
  public DefaultTreeModel model;
  private ClassTree tree;
  private JPanel outerPanel;

  private JButton analysis;
  private JButton decompile;
  private JButton bytecode;
  private JButton ignore;

  public ClassTreePanel(Threadtear threadtear) {
    this.threadtear = threadtear;
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
      SortedClassTreeNode tn = (SortedClassTreeNode) tree.getLastSelectedPathComponent();
      if (tn != null && tn.member != null) {
        new DecompilerFrame(tn.member).setVisible(true);
      }
    });
    panel.add(decompile);
    decompile.setEnabled(false);
    bytecode = new JButton("Bytecode", IconLoader.get().loadSVGIcon("res/bytecode.svg", false));
    bytecode.addActionListener(l -> {
      SortedClassTreeNode tn = (SortedClassTreeNode) tree.getLastSelectedPathComponent();
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
        SortedClassTreeNode tn = (SortedClassTreeNode) paths[i].getLastPathComponent();
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
      outerPanel.setBorder(BorderFactory.createTitledBorder(Strings.min(inputFile.getName(), 40) + " - " + classes.size() + " classes (" + disabled + " ignored)"));
    }
    repaint();
  }

  public void ignore(String className) {
    classes.stream().filter(c -> c.node.name.equals(className)).forEach(c -> c.transform = false);
  }

  public void updateAllNames(SortedClassTreeNode root) {
    root.updateClassName();
    for (int i = 0; i < root.getChildCount(); i++) {
      SortedClassTreeNode child = (SortedClassTreeNode) root.getChildAt(i);
      updateAllNames(child);
    }
  }

  private void ignoreChilds(SortedClassTreeNode node) {
    if (node.member != null) {
      node.member.transform = !node.member.transform;
      node.updateClassName();
    } else {
      for (int i = 0; i < node.getChildCount(); i++) {
        SortedClassTreeNode child = (SortedClassTreeNode) node.getChildAt(i);
        ignoreChilds(child);
      }
    }
  }

  public class ClassTree extends JTreeWithHint {
    private static final long serialVersionUID = 1L;

    public ClassTree() {
      super("Drag a jar or class file here");
      this.setRootVisible(false);
      this.setShowsRootHandles(true);
      this.setFocusable(true);
      this.setCellRenderer(new ClassTreeCellRenderer());
      model = new DefaultTreeModel(new SortedClassTreeNode(""));
      this.setModel(model);
      this.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
      this.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
          if (e.getClickCount() == 2) {
            SortedClassTreeNode tn = (SortedClassTreeNode) getLastSelectedPathComponent();
            if (tn != null && tn.member != null) {
              new DecompilerFrame(tn.member).setVisible(true);
            }
          }
        }
      });
      this.addTreeSelectionListener(l -> {
        SortedClassTreeNode tn = (SortedClassTreeNode) getLastSelectedPathComponent();
        boolean selected = tn != null;
        decompile.setEnabled(selected && tn.member != null);
        bytecode.setEnabled(selected && tn.member != null);
        ignore.setEnabled(selected);
      });
    }
  }

  @Override
  public void onFileDrop(File input) {
    this.remove(outerPanel);
    String type = FilenameUtils.getExtension(input.getAbsolutePath());
    LoadingIndicator loadingLabel = new LoadingIndicator("Loading class file(s)... ", JLabel.CENTER);
    loadingLabel.setRunning(true);
    this.add(loadingLabel, BorderLayout.CENTER);
    this.invalidate();
    this.validate();
    this.repaint();
    try {
      SwingUtilities.invokeLater(() -> {
        new Thread(() -> {
          this.inputFile = input;
          this.loadFile(type);
          loadTree(classes);
          refreshIgnored();
          model.reload();
          analysis.setEnabled(true);
          threadtear.configPanel.run.setEnabled(true);
          threadtear.configPanel.save.setEnabled(true);
          this.remove(loadingLabel);
          this.add(outerPanel, BorderLayout.CENTER);
          this.invalidate();
          this.validate();
          this.repaint();
        }).start();
      });
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadFile(String type) {
    try {
      switch (type) {
      case "jar":
        this.classes = JarIO.loadClasses(inputFile);
        if (classes.stream().anyMatch(c -> c.oldEntry.getCertificates() != null)) {
          JOptionPane.showMessageDialog(this,
              "<html>Warning: File is signed and may not load correctly if already modified, remove the signature<br>(<tt>META-INF\\MANIFEST.MF</tt>) and certificates (<tt>META-INF\\*.SF/.RSA</tt>) first!",
              "Signature warning", JOptionPane.WARNING_MESSAGE);
        }
        break;
      case "class":
        ClassNode node = Conversion.toNode(Files.readAllBytes(inputFile.toPath()));
        this.classes = new ArrayList<>(Collections.singletonList(new Clazz(node, new JarEntry(node.name), inputFile)));
        break;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @SuppressWarnings("unchecked")
  public void loadTree(List<Clazz> classes) {
    SortedClassTreeNode root = new SortedClassTreeNode("");
    model = new DefaultTreeModel(root);
    classes.forEach(c -> {
      String[] packages = c.node.name.split("/");
      addToTree((SortedClassTreeNode) model.getRoot(), c, packages, 0);
    });
    for (Object n : Collections.list(root.depthFirstEnumeration())) {
      SortedClassTreeNode node = (SortedClassTreeNode) n;
      if (!node.isLeaf() && node != root) {
        if (node.getChildCount() == 1) {
          SortedClassTreeNode child = (SortedClassTreeNode) node.getChildAt(0);
          if (child.member == null) {
            node.combinePackage(child);
          }
        }
      }
      node.sort();
    }
    tree.setModel(model);
  }

  public void addToTree(SortedClassTreeNode current, Clazz c, String[] packages, int pckg) {
    String node = packages[pckg];
    if (packages.length - pckg <= 1) {
      current.add(new SortedClassTreeNode(c));
      return;
    }
    for (int i = 0; i < current.getChildCount(); i++) {

      SortedClassTreeNode child = (SortedClassTreeNode) current.getChildAt(i);
      if (child.toString().equals(node)) {
        addToTree(child, c, packages, ++pckg);
        return;
      }
    }
    SortedClassTreeNode newChild = new SortedClassTreeNode(node);
    current.add(newChild);
    addToTree(newChild, c, packages, ++pckg);
  }
}
