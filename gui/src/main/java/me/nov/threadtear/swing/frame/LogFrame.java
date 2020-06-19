package me.nov.threadtear.swing.frame;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import com.github.weisj.darklaf.components.OverlayScrollPane;
import com.github.weisj.darklaf.components.border.DarkBorders;

import me.nov.threadtear.logging.Appender;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.swing.SwingUtils;

public class LogFrame extends JDialog {
  private static final long serialVersionUID = 1L;

  private static final Icon icon = SwingUtils.getIcon("run.svg");
  private final JTextPane area;
  private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-hh-mm");

  public LogFrame() {
    setTitle("Logger");
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    setBounds(100, 100, 1000, 800);
    setMinimumSize(new Dimension(500, 400));
    setModalityType(ModalityType.MODELESS);
    setLayout(new BorderLayout());
    setIconImage(SwingUtils.iconToFrameImage(getIcon(), this));
    area = new JTextPane();
    area.setEditable(false);
    area.setMargin(new Insets(8, 8, 8, 8));
    area.setFont(new Font("Consolas", Font.PLAIN, 11));
    area.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK),
            TransferHandler.getCopyAction());
    JPanel cp = new JPanel(new BorderLayout());
    cp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(8, 8, 8, 8),
      DarkBorders.createLineBorder(1, 1, 1, 1)));
    cp.add(new OverlayScrollPane(area), BorderLayout.CENTER);
    this.add(cp, BorderLayout.CENTER);
    JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
    JButton save = new JButton("Save to file");
    save.addActionListener(e -> {
      File inputFile = new File(System.getProperty("user.home"), dateFormat.format(new Date()) + ".log");
      JFileChooser jfc = new JFileChooser(inputFile.getParentFile());
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setSelectedFile(inputFile);
      jfc.setDialogTitle("Save log to file");
      jfc.setFileFilter(new FileNameExtensionFilter("Log file (*.log)", "log"));
      int result = jfc.showSaveDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File output = jfc.getSelectedFile();
        try {
          Files.write(output.toPath(), area.getText().getBytes());
        } catch (IOException e1) {
          e1.printStackTrace();
        }
        LogWrapper.logger.info("Saved log to " + output.getAbsolutePath());
      }
    });
    buttons.add(save);
    JButton copy = new JButton("Copy to clipboard");
    copy.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard()
            .setContents(new StringSelection(area.getText()), null));
    buttons.add(copy);
    JButton close = new JButton("Close");
    close.addActionListener(e -> dispose());
    buttons.add(close);
    getContentPane().add(buttons, BorderLayout.SOUTH);
  }

  public void append(String string) {
    try {
      area.getDocument().insertString(area.getDocument().getLength(), string, Appender.RESTO_ATT);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  public JTextPane getTextArea() {
    return area;
  }

  public static Icon getIcon() {
    return icon;
  }
}
