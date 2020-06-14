package me.nov.threadtear.swing.panel;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.LineBorder;

import com.github.weisj.darklaf.ui.splitpane.DarkSplitPaneUI;
import me.nov.threadtear.Threadtear;
import me.nov.threadtear.swing.tree.*;

public class TreePanel extends JPanel {
  private static final long serialVersionUID = 1L;
  public ClassTreePanel classList;
  public ExecutionListPanel executionList;

  public TreePanel(Threadtear threadtear) {
    this.setLayout(new BorderLayout());
    JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, executionList = new ExecutionListPanel(),
            classList = new ClassTreePanel(threadtear));
    split.putClientProperty(DarkSplitPaneUI.KEY_STYLE, DarkSplitPaneUI.STYLE_GRIP_BORDERLESS);
    split.setResizeWeight(0.5);
    this.add(split);
  }
}
