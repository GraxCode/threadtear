package me.nov.threadtear.swing.panel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.Timer;

import me.nov.threadtear.swing.tree.ClassTreePanel;
import me.nov.threadtear.swing.tree.ExecutionListPanel;

public class TreePanel extends JPanel {
  private static final long serialVersionUID = 1L;
  public ClassTreePanel classList;
  public ExecutionListPanel executionList;

  public TreePanel() {
    this.setLayout(new BorderLayout(16, 16));
    this.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    JPanel inner = new JPanel();
    inner.setLayout(new GridLayout(1, 2, 16, 16));
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, executionList = new ExecutionListPanel(), classList = new ClassTreePanel());
    split.putClientProperty("JSplitPane.style", "invisible");
    inner.add(split);
    this.add(inner, BorderLayout.CENTER);
    this.add(new JSeparator(), BorderLayout.PAGE_END);
    Timer t = new Timer(200, (k) -> {
      split.setDividerLocation(0.5);
    });
    t.setRepeats(false);
    t.start();
  }
}
