package me.nov.threadtear.swing;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TitledPanel extends JPanel {

  private TitledBorder titledBorder;
  private Component content;

  public TitledPanel(String title, Component content) {
    super(new BorderLayout());
    this.titledBorder = BorderFactory.createTitledBorder(title);
    add(SwingUtils.withBorder(Box.createHorizontalBox(), titledBorder), BorderLayout.NORTH);
    setContent(content);
  }

  public Component getContent() {
    return content;
  }

  public void setContent(Component content) {
    this.content = content;
    this.add(content, BorderLayout.CENTER);
  }

  public void setTitle(String title) {
    this.titledBorder.setTitle(title);
  }

  public String getTitle() {
    return titledBorder.getTitle();
  }
}
