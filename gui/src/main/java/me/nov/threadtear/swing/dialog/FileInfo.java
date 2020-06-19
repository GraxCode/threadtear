package me.nov.threadtear.swing.dialog;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.github.weisj.darklaf.components.border.DarkBorders;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.util.format.Strings;

public class FileInfo extends JDialog {
  private static final long serialVersionUID = 1L;

  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  public FileInfo(Component parent, Clazz member) {
    setModalityType(ModalityType.APPLICATION_MODAL);
    setLocationRelativeTo(parent);
    setTitle("File information");
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setMinimumSize(new Dimension(450, 300));
    setResizable(false);
    getContentPane().setLayout(new BorderLayout());
    JPanel cp = new JPanel(new BorderLayout());
    cp.setBorder(new EmptyBorder(10, 10, 10, 10));
    getContentPane().add(cp, BorderLayout.CENTER);
    JPanel descriptions = new JPanel(new GridLayout(9, 1));
    descriptions.setBorder(new EmptyBorder(8, 8, 8, 8));
    JPanel values = new JPanel(new GridLayout(9, 1));
    values.setBorder(new EmptyBorder(8, 8, 8, 8));
    descriptions.add(new CustomLabel("File name: "));
    values.add(new CustomLabel(member.oldEntry.getName()));
    descriptions.add(new CustomLabel("Size: "));
    values.add(new CustomLabel(Strings.formatBytes(member.oldEntry.getSize()) + "; compressed: " +
            Strings.formatBytes(member.oldEntry.getCompressedSize())));
    if (member.oldEntry.getCreationTime() != null) {
      descriptions.add(new CustomLabel("Creation time: "));
      values.add(new CustomLabel(format.format(new Date(member.oldEntry.getCreationTime().toMillis()))));
    }
    if (member.oldEntry.getLastAccessTime() != null) {
      descriptions.add(new CustomLabel("Last access: "));
      values.add(new CustomLabel(format.format(new Date(member.oldEntry.getLastAccessTime().toMillis()))));
    }
    descriptions.add(new CustomLabel("Last modified: "));
    values.add(new CustomLabel(format.format(new Date(member.oldEntry.getTime()))));
    if (member.oldEntry.getComment() != null) {
      descriptions.add(new CustomLabel("Comment: "));
      values.add(new CustomLabel(Strings.min(member.oldEntry.getComment(), 100)));
    }
    if (member.oldEntry.getExtra() != null) {
      descriptions.add(new CustomLabel("First 8 extra bytes: "));
      byte[] arr = new byte[8];
      System.arraycopy(member.oldEntry.getExtra(), 0, arr, 0, 8);
      values.add(new CustomLabel(Arrays.toString(arr)));
    }
    descriptions.add(new CustomLabel("Signature: "));
    values.add(new CustomLabel(member.oldEntry.getCertificates() != null ?
            "<font color=\"red\">signed, please remove certs</font>" :
            "<font color=\"green\">not signed</font>"));

    descriptions.add(new CustomLabel("CRC-32 hash: "));
    values.add(new CustomLabel(Long.toHexString(member.oldEntry.getCrc())));
    JPanel inner = new JPanel(new BorderLayout());
    inner.setBorder(DarkBorders.createLineBorder(1, 1, 1, 1));
    inner.add(descriptions, BorderLayout.WEST);

    inner.add(values, BorderLayout.CENTER);
    cp.add(inner, BorderLayout.CENTER);
    JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
    getContentPane().add(buttons, BorderLayout.SOUTH);
    JButton ok = new JButton("OK");
    ok.addActionListener(e -> dispose());
    ok.setActionCommand("OK");
    buttons.add(ok);
    getRootPane().setDefaultButton(ok);
    pack();
  }

  public static class CustomLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    public CustomLabel(String s) {
      super("<html>" + s);
      this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
  }
}
