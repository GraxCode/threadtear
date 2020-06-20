package me.nov.threadtear.swing.panel;

import java.awt.*;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;

import com.github.weisj.darklaf.components.border.DarkBorders;
import com.github.weisj.darklaf.components.text.SearchTextField;
import me.nov.threadtear.swing.SwingUtils;
import me.nov.threadtear.swing.button.ReloadButton;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.ui.text.DarkTextUI;

import me.nov.threadtear.io.Conversion;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;
import me.nov.threadtear.util.format.Strings;

public class BytecodePanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private DecompilerTextArea textArea;

  private int searchIndex = -1;
  private String lastSearchText = null;

  public BytecodePanel(ClassNode cn) {
    this.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    this.setLayout(new BorderLayout(4, 4));
    JPanel actionPanel = new JPanel();
    actionPanel.setLayout(new GridBagLayout());
    JButton reload = new ReloadButton();
    reload.addActionListener(l -> textArea.setText(Conversion.textify(cn)));
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

    actionPanel.add(search);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.anchor = GridBagConstraints.EAST;
    actionPanel.add(reload, gbc);
    JPanel topPanel = new JPanel();
    topPanel.setBorder(new EmptyBorder(3, 5, 0, 1));
    topPanel.setLayout(new BorderLayout());
    topPanel.add(new JLabel("<html>Bytecode of <tt>" + Strings.min(cn.name.replace('/', '.'), 128) + "</tt>"),
            BorderLayout.WEST);
    topPanel.add(actionPanel, BorderLayout.EAST);
    this.add(topPanel, BorderLayout.NORTH);
    this.textArea = new DecompilerTextArea();
    textArea.setText(Conversion.textify(cn));
    this.add(SwingUtils.withBorder(
      SwingUtils.wrap(SwingUtils.createRSyntaxOverlayScrollPane(textArea)),
      DarkBorders.createLineBorder(1, 1, 1, 1)
    ), BorderLayout.CENTER);
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
