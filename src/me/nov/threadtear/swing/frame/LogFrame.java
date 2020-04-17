package me.nov.threadtear.swing.frame;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.nov.threadtear.Threadtear;

public class LogFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	public JTextArea area;
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd-hh-mm");

	public LogFrame() {
		setTitle("Execution log");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 1000, 800);
		setLayout(new BorderLayout());
		this.setAlwaysOnTop(true);
		area = new JTextArea();
		area.setEditable(false);
		area.setMargin(new Insets(8, 8, 8, 8));
		area.setFont(new Font("Consolas", Font.PLAIN, 11));
		area.getKeymap().addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), TransferHandler.getCopyAction());
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(8, 8, 8, 8), BorderFactory.createLoweredBevelBorder()));
		cp.add(new JScrollPane(area), BorderLayout.CENTER);
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
				Threadtear.logger.info("Saved log to " + output.getAbsolutePath());
			}
		});
		buttons.add(save);
		JButton copy = new JButton("Copy log");
		copy.addActionListener(e -> {
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(area.getText()), null);
		});
		buttons.add(copy);
		JButton close = new JButton("Close");
		close.addActionListener(e -> {
			dispose();
		});
		buttons.add(close);
		getContentPane().add(buttons, BorderLayout.SOUTH);
	}

	public static class LogHandler extends Handler {

		private JTextArea textArea;

		public LogHandler(JTextArea textArea) {
			this.textArea = textArea;
		}

		@Override
		public void publish(LogRecord record) {
			textArea.append(format(record));
			textArea.setCaretPosition(textArea.getDocument().getLength());
		}

		private String format(LogRecord record) {
			return String.format("[%1$tF %1$tT] [%4$-7s] %5$s %n", new Date(record.getMillis()), record.getSourceClassName(), record.getLoggerName(), record.getLevel().getLocalizedName(), record.getMessage(), record.getThrown());
		}

		@Override
		public void flush() {
			textArea.repaint();
		}

		@Override
		public void close() throws SecurityException {
		}
	}
}
