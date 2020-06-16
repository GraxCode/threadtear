package me.nov.threadtear.swing.button;

import me.nov.threadtear.swing.Utils;

import javax.swing.*;

public class ReloadButton extends JButton {

  public ReloadButton() {
    setToolTipText("Refresh");
    setIcon(Utils.getIcon("refresh.svg", true));
    setDisabledIcon(Utils.getIcon("refresh_disabled.svg", true));
  }
}
