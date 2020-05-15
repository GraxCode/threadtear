package me.nov.threadtear.swing.panel;

import java.awt.*;

import javax.swing.*;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.swing.tree.*;

public class TreePanel extends JPanel {
  private static final long serialVersionUID = 1L;
  public ClassTreePanel classList;
  public ExecutionListPanel executionList;

  public TreePanel(Threadtear threadtear) {
    this.setLayout(new BorderLayout(16, 16));
    this.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    JPanel inner = new JPanel();
    inner.setLayout(new GridLayout(1, 2, 16, 16));
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, executionList = new ExecutionListPanel(), classList = new ClassTreePanel(threadtear));
    split.putClientProperty("JSplitPane.style", "invisible");
    split.setResizeWeight(0.5);
    inner.add(split);
    this.add(inner, BorderLayout.CENTER);
    this.add(new JSeparator(), BorderLayout.PAGE_END);
  }
}
