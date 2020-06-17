package me.nov.threadtear.swing.button;

import me.nov.threadtear.swing.SwingUtils;

import javax.swing.*;

public class ReloadButton extends JButton {

  public ReloadButton() {
    setToolTipText("Refresh");
    setIcon(SwingUtils.getIcon("refresh.svg", true));
    setDisabledIcon(SwingUtils.getIcon("refresh_disabled.svg", true));
  }
}
