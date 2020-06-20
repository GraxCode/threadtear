package me.nov.threadtear.swing.frame;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.ui.tabbedpane.DarkTabbedPaneUI;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.panel.CFGPanel;
import me.nov.threadtear.swing.SwingUtils;
import me.nov.threadtear.swing.panel.BytecodePanel;
import me.nov.threadtear.swing.panel.DecompilerPanel;
import me.nov.threadtear.util.format.Strings;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class AnalysisFrame extends JFrame {
  private static final long serialVersionUID = 1L;
  private final File archive;
  private final Clazz clazz;
  private final String title;
  public LoadingIndicator loading;

  public AnalysisFrame(String title, Clazz clazz) {
    this.clazz = clazz;
    this.archive = new File("");
    this.title = title;
    createFrame();
  }

  public AnalysisFrame(File archive, Clazz clazz) {
    this.clazz = clazz;
    this.archive = archive;
    this.title = Strings.min(clazz.node.name.replace('/', '.'), 128);
    createFrame();
  }

  private void createFrame() {
    setTitle(title);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 1000, 600);
    setMinimumSize(new Dimension(900, 540));
    setLayout(new BorderLayout());
    setIconImage(SwingUtils.iconToFrameImage(SwingUtils.getIcon("decompile.svg", true), this));
    int pad = 8;
    JPanel cp = SwingUtils.withEmptyBorder(new JPanel(new BorderLayout()), pad, 0, pad, 0);
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.putClientProperty(DarkTabbedPaneUI.KEY_LEADING_COMP, Box.createHorizontalStrut(pad));
    tabbedPane.putClientProperty(DarkTabbedPaneUI.KEY_TRAILING_COMP, Box.createHorizontalStrut(pad));
    tabbedPane.addTab("Decompiler", SwingUtils.getIcon("decompile.svg", true),
      SwingUtils.withEmptyBorder(new DecompilerPanel(this, archive, clazz), 0, pad, 0, pad));
    tabbedPane.setDisabledIconAt(0, SwingUtils.getIcon("decompile_disabled.svg", true));
    tabbedPane.addTab("Bytecode", SwingUtils.getIcon("bytecode.svg", true),
      SwingUtils.withEmptyBorder(new BytecodePanel(clazz.node), 0, pad, 0, pad));
    tabbedPane.setDisabledIconAt(1, SwingUtils.getIcon("bytecode_disabled.svg", true));
    tabbedPane.addTab("Graph", SwingUtils.getIcon("graph.svg", true),
      SwingUtils.withEmptyBorder(new CFGPanel(clazz.node), 0, pad, 0, pad));
    tabbedPane.setDisabledIconAt(2, SwingUtils.getIcon("graph_disabled.svg", true));

    cp.add(tabbedPane, BorderLayout.CENTER);
    this.add(cp, BorderLayout.CENTER);
    JPanel actionBar = new JPanel();
    actionBar.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    loading = new LoadingIndicator();
    loading.setVisible(false);
    loading.setRunning(false);
    actionBar.add(loading);
    actionBar.add(close);
    getContentPane().add(actionBar, BorderLayout.SOUTH);
  }
}
