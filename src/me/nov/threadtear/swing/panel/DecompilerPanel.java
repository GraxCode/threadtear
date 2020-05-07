package me.nov.threadtear.swing.panel;

import static org.objectweb.asm.Opcodes.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Objects;
import java.util.stream.StreamSupport;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

import org.apache.commons.io.IOUtils;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.*;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.ui.text.DarkTextUI;

import me.nov.threadtear.decompiler.CFR;
import me.nov.threadtear.io.*;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;
import me.nov.threadtear.util.Strings;

public class DecompilerPanel extends JPanel implements ActionListener {
  private static final long serialVersionUID = 1L;

  private DecompilerTextArea textArea;

  private int searchIndex = -1;
  private String lastSearchText = null;

  private Clazz clazz;

  private RTextScrollPane scp;
  private JComboBox<String> conversionMethod;
  private JCheckBox ignoreTCB;
  private JCheckBox ignoreMon;

  public DecompilerPanel(Clazz cn) {
    this.clazz = cn;
    this.setLayout(new BorderLayout(4, 4));

    JPanel leftActionPanel = new JPanel();
    leftActionPanel.setLayout(new GridBagLayout());
    leftActionPanel.add(new JLabel("<html><tt>CFR 0.149 "));
    conversionMethod = new JComboBox<>(new String[] { "Use source", "Pass through ASM" });
    conversionMethod.addActionListener(this);
    conversionMethod.setEnabled(false);
    leftActionPanel.add(conversionMethod);
    leftActionPanel.add(ignoreTCB = new JCheckBox("Ingore try catch blocks"));
    leftActionPanel.add(ignoreMon = new JCheckBox("Ignore synchronized"));
    ignoreTCB.setEnabled(false);
    ignoreTCB.addActionListener(this);
    ignoreMon.setEnabled(false);
    ignoreMon.addActionListener(this);
    JPanel rightActionPanel = new JPanel();
    rightActionPanel.setLayout(new GridBagLayout());
    JButton reload = new JButton(IconLoader.get().loadSVGIcon("res/refresh.svg", false));
    reload.addActionListener(this);
    JTextField search = new JTextField();
    search.putClientProperty(DarkTextUI.KEY_DEFAULT_TEXT, "Search for text or regex");
    search.setPreferredSize(new Dimension(200, reload.getPreferredSize().height));
    search.addActionListener(l -> {
      try {
        String text = search.getText();
        if (text.isEmpty()) {
          textArea.getHighlighter().removeAllHighlights();
          return;
        }
        String searchText = text.toLowerCase();
        if (!Objects.equals(searchText, lastSearchText)) {
          searchIndex = -1;
          lastSearchText = searchText;
        }
        String[] split = textArea.getText().split("\\r?\\n");
        int firstIndex = -1;
        boolean first = false;
        Label: {
          for (int i = 0; i < split.length; i++) {
            String line = split[i];
            if (Strings.containsRegex(line, searchText)) {
              if (i > searchIndex) {
                textArea.setCaretPosition(textArea.getDocument().getDefaultRootElement().getElement(i).getStartOffset());
                searchIndex = i;
                break Label;
              } else if (!first) {
                firstIndex = i;
                first = true;
              }
            }
          }
          Toolkit.getDefaultToolkit().beep();
          if (first) {
            // go back to first line
            textArea.setCaretPosition(textArea.getDocument().getDefaultRootElement().getElement(firstIndex).getStartOffset());
            searchIndex = firstIndex;
          }
        }
        hightlightText(searchText);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    rightActionPanel.add(search);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    rightActionPanel.add(reload, gbc);
    JPanel topPanel = new JPanel();
    topPanel.setBorder(new EmptyBorder(1, 5, 0, 1));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(leftActionPanel, BorderLayout.WEST);
    topPanel.add(rightActionPanel, BorderLayout.EAST);
    this.add(topPanel, BorderLayout.NORTH);
    LoadingIndicator loadingLabel = new LoadingIndicator("Decompiling class file... ", JLabel.CENTER);
    loadingLabel.setRunning(true);
    this.add(loadingLabel, BorderLayout.CENTER);
    SwingUtilities.invokeLater(() -> {
      new Thread(() -> {
        this.textArea = new DecompilerTextArea();
        this.update();
        scp = new RTextScrollPane(textArea);
        scp.getVerticalScrollBar().setUnitIncrement(16);
        scp.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        this.remove(loadingLabel);
        this.add(scp, BorderLayout.CENTER);
        conversionMethod.setEnabled(true);
        invalidate();
        validate();
        repaint();
      }, "decompile-thread").start();
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    reload();
  }

  private void reload() {
    LoadingIndicator loadingLabel = new LoadingIndicator("Decompiling class file... ", JLabel.CENTER);
    loadingLabel.setRunning(true);
    this.add(loadingLabel, BorderLayout.CENTER);
    this.remove(scp);
    invalidate();
    validate();
    SwingUtilities.invokeLater(() -> {
      new Thread(() -> {
        update();
        this.remove(loadingLabel);
        this.add(scp, BorderLayout.CENTER);
        invalidate();
        validate();
        repaint();
      }).start();
    });
  }

  public void update() {
    try {
      byte[] bytes;
      if (conversionMethod.getSelectedIndex() == 0) {
        // use the original jar file to retrieve bytes
        bytes = IOUtils.toByteArray(clazz.streamOriginal());
        ignoreTCB.setEnabled(false);
        ignoreMon.setEnabled(false);
      } else {
        System.out.println("decompile ASM " + conversionMethod.getSelectedIndex());
        ignoreTCB.setEnabled(true);
        ignoreMon.setEnabled(true);
        ClassNode copy = Conversion.toNode(Conversion.toBytecode0(clazz.node));
        // do some asm action here
        if (ignoreTCB.isSelected()) {
          copy.methods.forEach(m -> m.tryCatchBlocks = null);
        }
        if (ignoreMon.isSelected()) {
          copy.methods.forEach(m -> StreamSupport.stream(m.instructions.spliterator(), false).filter(i -> i.getOpcode() == MONITORENTER || i.getOpcode() == MONITOREXIT)
              .forEach(i -> m.instructions.set(i, new InsnNode(POP))));
        }
        bytes = Conversion.toBytecode0(copy);
      }
      String decompiled = CFR.decompile(clazz.node.name, bytes);
      this.textArea.setText(decompiled);
    } catch (IOException e) {
      e.printStackTrace();
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      this.textArea.setText(sw.toString());
    }
  }

  private void hightlightText(String searchText) throws BadLocationException {
    Highlighter highlighter = textArea.getHighlighter();
    highlighter.removeAllHighlights();
    Document document = textArea.getDocument();
    String text = document.getText(0, document.getLength()).toLowerCase();
    int pos = text.indexOf(searchText);
    while (pos >= 0) {
      highlighter.addHighlight(pos, pos + searchText.length(), new DefaultHighlighter.DefaultHighlightPainter(new Color(0x0078d7)));
      pos = text.indexOf(searchText, pos + searchText.length());
    }
  }
}
