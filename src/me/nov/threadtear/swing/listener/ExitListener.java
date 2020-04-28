package me.nov.threadtear.swing.listener;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class ExitListener extends WindowAdapter {
  private JFrame frame;

  public ExitListener(JFrame frame) {
    this.frame = frame;
  }

  @Override
  public void windowClosing(WindowEvent we) {
    if (JOptionPane.showConfirmDialog(frame, "Do you really want to exit?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      Runtime.getRuntime().exit(0);
    }
  }
}
