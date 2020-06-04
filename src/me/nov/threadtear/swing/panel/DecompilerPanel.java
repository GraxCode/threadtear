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
import org.benf.cfr.reader.util.CfrVersionInfo;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.*;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.icons.IconLoader;
import com.github.weisj.darklaf.ui.text.DarkTextUI;

import me.nov.threadtear.decompiler.*;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;
import me.nov.threadtear.util.format.Strings;

public class DecompilerPanel extends JPanel implements ActionListener {
  private static final long serialVersionUID = 1L;

  private DecompilerTextArea textArea;

  private int searchIndex = -1;
  private String lastSearchText = null;

  public File archive;
  public Clazz clazz;

  private RTextScrollPane scp;
  private JComboBox<String> decompilerSelection;
  private JComboBox<String> conversionMethod;
  private JCheckBox ignoreTCB;
  private JCheckBox ignoreMon;
  private JCheckBox aggressive;

  private static int preferredDecompilerIndex = 0;
  private IDecompilerBridge decompilerBridge;

  public DecompilerPanel(File archive, Clazz cn) {
    this.archive = archive;
    this.clazz = cn;
    this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    this.setLayout(new BorderLayout(4, 4));
    JPanel leftActionPanel = new JPanel();
    leftActionPanel.setLayout(new GridBagLayout());
    decompilerSelection = new JComboBox<>(new String[] { "CFR " + CfrVersionInfo.VERSION, "Fernflower 15-05-20", "Krakatau 22-05-20" });
    decompilerSelection.setSelectedIndex(preferredDecompilerIndex);
    decompilerSelection.addActionListener(this);
    leftActionPanel.add(decompilerSelection);
    conversionMethod = new JComboBox<>(new String[] { "Source", "Transformed" });
    conversionMethod.setSelectedIndex(1);
    conversionMethod.addActionListener(this);
    leftActionPanel.add(conversionMethod);
    leftActionPanel.add(ignoreTCB = new JCheckBox("Ignore try catch blocks"));
    leftActionPanel.add(ignoreMon = new JCheckBox("Ignore synchronized"));
    leftActionPanel.add(aggressive = new JCheckBox("Aggressive"));
    ignoreTCB.setFocusable(false);
    ignoreTCB.addActionListener(this);
    ignoreMon.setFocusable(false);
    ignoreMon.addActionListener(this);
    aggressive.addActionListener(this);
    aggressive.setFocusable(false);
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
    SwingUtilities.invokeLater(() -> new Thread(() -> {
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
    }, "decompile-thread").start());
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (scp != null && scp.isShowing()) {
      reload();
    }
  }

  private void reload() {
    LoadingIndicator loadingLabel = new LoadingIndicator("Decompiling class file... ", JLabel.CENTER);
    loadingLabel.setRunning(true);
    this.add(loadingLabel, BorderLayout.CENTER);
    this.remove(scp);
    invalidate();
    validate();
    SwingUtilities.invokeLater(() -> new Thread(() -> {
      update();
      this.remove(loadingLabel);
      this.add(scp, BorderLayout.CENTER);
      invalidate();
      validate();
      repaint();
    }).start());
  }

  public void update() {
    try {
      byte[] bytes;
      if (conversionMethod.getSelectedIndex() == 0) {
        // use the original jar file to retrieve bytes
        bytes = IOUtils.toByteArray(clazz.streamOriginal());
      } else {
        // use the local code
        bytes = Conversion.toBytecode0(clazz.node);
      }
      ClassNode copy = Conversion.toNode(bytes);
      // do some asm action here
      if (ignoreTCB.isSelected()) {
        copy.methods.forEach(m -> m.tryCatchBlocks = null);
      }
      if (ignoreMon.isSelected()) {
        copy.methods.forEach(m -> StreamSupport.stream(m.instructions.spliterator(), false).filter(i -> i.getOpcode() == MONITORENTER || i.getOpcode() == MONITOREXIT)
            .forEach(i -> m.instructions.set(i, new InsnNode(POP))));
      }
      bytes = Conversion.toBytecode0(copy);
      switch (decompilerSelection.getSelectedIndex()) {
      case 0:
        decompilerBridge = new CFRBridge();
        break;
      case 1:
        decompilerBridge = new FernflowerBridge();
        break;
      case 2:
        decompilerBridge = new KrakatauBridge();
        break;
      }
      preferredDecompilerIndex = decompilerSelection.getSelectedIndex();
      decompilerBridge.setAggressive(aggressive.isSelected());
      String decompiled = decompilerBridge.decompile(archive, clazz.node.name, bytes);
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
