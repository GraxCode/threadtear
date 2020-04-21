package me.nov.threadtear.swing.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;

import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.tree.ClassNode;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.decompiler.CFR;
import me.nov.threadtear.swing.textarea.DecompilerTextArea;

public class DecompilerPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private DecompilerTextArea textArea;

	private int searchIndex = -1;
	private String lastSearchText = null;

	public DecompilerPanel(ClassNode cn) {
		this.setLayout(new BorderLayout(4, 4));
		JPanel actionPanel = new JPanel();
		actionPanel.setLayout(new GridBagLayout());
		JButton reload = new JButton(IconLoader.get().loadSVGIcon("/res/refresh.svg", false));
		reload.addActionListener(l -> {
			textArea.setText(CFR.decompile(cn));
		});
		JTextField search = new JTextField();
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
						if (line.toLowerCase().contains(searchText)) {
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

		actionPanel.add(search);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.EAST;
		actionPanel.add(reload, gbc);
		JPanel topPanel = new JPanel();
		topPanel.setBorder(new EmptyBorder(1, 5, 0, 1));
		topPanel.setLayout(new BorderLayout());
		topPanel.add(new JLabel("CFR Decompiler 0.149"), BorderLayout.WEST);
		topPanel.add(actionPanel, BorderLayout.EAST);
		this.add(topPanel, BorderLayout.NORTH);
		this.textArea = new DecompilerTextArea();
		textArea.setText(CFR.decompile(cn));
		JScrollPane scp = new RTextScrollPane(textArea);
		scp.getVerticalScrollBar().setUnitIncrement(16);
		scp.setBorder(BorderFactory.createLoweredSoftBevelBorder());
		this.add(scp, BorderLayout.CENTER);
	}

	private void hightlightText(String searchText) throws BadLocationException {
		Highlighter highlighter = textArea.getHighlighter();
		highlighter.removeAllHighlights();
		Document document = textArea.getDocument();
		String text = document.getText(0, document.getLength()).toLowerCase();
		int pos = text.indexOf(searchText);
		while (pos >= 0) {
			highlighter.addHighlight(pos, pos + searchText.length(), (Highlighter.HighlightPainter) new DefaultHighlighter.DefaultHighlightPainter(new Color(0x0078d7)));
			pos = text.indexOf(searchText, pos + searchText.length());
		}
	}
}
