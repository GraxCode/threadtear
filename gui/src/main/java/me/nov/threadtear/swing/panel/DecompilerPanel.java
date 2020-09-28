package me.nov.threadtear.swing.panel;

import com.github.weisj.darklaf.components.border.DarkBorders;
import com.github.weisj.darklaf.components.text.SearchTextField;
import com.github.weisj.darklaf.ui.text.DarkTextUI;
import me.nov.threadtear.Threadtear;
import me.nov.threadtear.decompiler.DecompilerInfo;
import me.nov.threadtear.decompiler.IDecompilerBridge;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.swing.SwingUtils;
import me.nov.threadtear.swing.button.ReloadButton;
import me.nov.threadtear.swing.frame.AnalysisFrame;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;
import me.nov.threadtear.util.format.Strings;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.stream.StreamSupport;

import static org.objectweb.asm.Opcodes.*;

public class DecompilerPanel extends JPanel implements ActionListener {
  private static final long serialVersionUID = 1L;
  private static int preferredDecompilerIndex = 0;
  private final AnalysisFrame analysisFrame;
  private final JComboBox<DecompilerInfo<?>> decompilerSelection;
  private final JComboBox<String> conversionMethod;
  private final JCheckBox ignoreTCB;
  private final JCheckBox ignoreMon;
  private final JCheckBox aggressive;
  public File archive;
  public Clazz clazz;
  private DecompilerTextArea textArea;
  private int searchIndex = -1;
  private String lastSearchText = null;

  public DecompilerPanel(AnalysisFrame analysisFrame, File archive, Clazz cn) {
    this.analysisFrame = analysisFrame;
    this.archive = archive;
    this.clazz = cn;
    this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    this.setLayout(new BorderLayout(4, 4));
    JPanel leftActionPanel = new JPanel();
    leftActionPanel.setLayout(new GridBagLayout());
    decompilerSelection = new JComboBox<DecompilerInfo<?>>(DecompilerInfo.getDecompilerInfos().toArray(new DecompilerInfo[0]));
    decompilerSelection.setSelectedIndex(preferredDecompilerIndex);
    decompilerSelection.addActionListener(this);
    leftActionPanel.add(decompilerSelection);
    conversionMethod = new JComboBox<>(new String[]{"Source", "Transformed"});
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
    ReloadButton reload = new ReloadButton();
    reload.addActionListener(this);
    SearchTextField search = new SearchTextField();

    search.putClientProperty(DarkTextUI.KEY_DEFAULT_TEXT, "Search for text or regex");
    search.setPreferredSize(new Dimension(200, reload.getPreferredSize().height));
    search.addSearchListener(l -> {
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
        Label:
        {
          for (int i = 0; i < split.length; i++) {
            String line = split[i];
            if (Strings.regexOrContains(line, searchText)) {
              if (i > searchIndex) {
                textArea.setCaretPosition(
                  textArea.getDocument().getDefaultRootElement().getElement(i).getStartOffset());
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
            textArea.setCaretPosition(
              textArea.getDocument().getDefaultRootElement().getElement(firstIndex).getStartOffset());
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
    topPanel.setBorder(new EmptyBorder(3, 5, 0, 1));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(leftActionPanel, BorderLayout.WEST);
    topPanel.add(rightActionPanel, BorderLayout.EAST);
    this.add(topPanel, BorderLayout.NORTH);
    Threadtear.getInstance().statusBar.runWithLoadIndicator("Decompiling class file... ", () -> {
      this.textArea = new DecompilerTextArea();
      this.textArea.addKeyListener(new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
          if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0)
            if (e.getKeyCode() == KeyEvent.VK_F) {
              search.requestFocusInWindow();
              String text = textArea.getSelectedText();
              if (text != null && !text.trim().isEmpty()) {
                search.setText(text);
                search.postActionEvent();
              }
            }
        }
      });
      this.update();
      this.add(SwingUtils.withBorder(
        SwingUtils.wrap(SwingUtils.createRSyntaxOverlayScrollPane(textArea)),
        DarkBorders.createLineBorder(1, 1, 1, 1)
      ), BorderLayout.CENTER);
      conversionMethod.setEnabled(true);
      invalidate();
      validate();
      repaint();
    });
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (!Threadtear.getInstance().statusBar.isIndicatorRunning()) {
      reload();
    }
  }

  private void reload() {
    Threadtear.getInstance().statusBar.runWithLoadIndicator("Decompiling class file...", () -> {
      analysisFrame.loading.setVisible(true);
      analysisFrame.loading.setRunning(true);
      this.update();
      analysisFrame.loading.setRunning(false);
      analysisFrame.loading.setVisible(false);
    });
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
        copy.methods.forEach(m -> StreamSupport.stream(m.instructions.spliterator(), false)
          .filter(i -> i.getOpcode() == MONITORENTER || i.getOpcode() == MONITOREXIT)
          .forEach(i -> m.instructions.set(i, new InsnNode(POP))));
      }
      bytes = Conversion.toBytecode0(copy);
      IDecompilerBridge decompilerBridge = decompilerSelection.getModel()
        .getElementAt(decompilerSelection.getSelectedIndex())
        .createDecompilerBridge();
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
      highlighter.addHighlight(pos, pos + searchText.length(),
        new DefaultHighlighter.DefaultHighlightPainter(new Color(0x0078d7)));
      pos = text.indexOf(searchText, pos + searchText.length());
    }
  }
}
