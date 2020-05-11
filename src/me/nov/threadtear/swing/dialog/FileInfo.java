package me.nov.threadtear.swing.dialog;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import me.nov.threadtear.execution.Clazz;

public class FileInfo extends JDialog {
  private static final long serialVersionUID = 1L;
  private JButton ok;

  public FileInfo(Clazz member) {
    setModalityType(ModalityType.APPLICATION_MODAL);
    setTitle("Select one or more executions");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setBounds(100, 100, 450, 300);
    setMinimumSize(new Dimension(450, 300));
    getContentPane().setLayout(new BorderLayout());
    JPanel cp = new JPanel();
    cp.setBorder(new EmptyBorder(10, 10, 10, 10));
    getContentPane().add(cp, BorderLayout.CENTER);
    JPanel treePanel = new JPanel(new BorderLayout());
    treePanel.setBorder(BorderFactory.createLoweredBevelBorder());
    treePanel.add(new JScrollPane(new JTextArea()), BorderLayout.CENTER);
    cp.add(treePanel);
    JPanel buttons = new JPanel();
    JButton cancel = new JButton("Cancel");
    cancel.addActionListener(e -> {
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
}
