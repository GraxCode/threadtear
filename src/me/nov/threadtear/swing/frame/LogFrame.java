package me.nov.threadtear.swing.frame;

import java.awt.BorderLayout;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public class LogFrame extends JFrame {

	private static final long serialVersionUID = 1L;
	public JTextArea area;

	public LogFrame() {
		setTitle("Log");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 600, 800);
		setLayout(new BorderLayout());
		area = new JTextArea();
		area.setEditable(false);
		this.setAlwaysOnTop(true);
		JPanel cp = new JPanel(new BorderLayout());
		cp.setBorder(
				BorderFactory.createCompoundBorder(new EmptyBorder(16, 16, 16, 16), BorderFactory.createLoweredBevelBorder()));
		cp.add(new JScrollPane(area), BorderLayout.CENTER);
		this.add(cp, BorderLayout.CENTER);
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
			return String.format("[%1$tF %1$tT] [%4$-7s] %5$s %n", new Date(record.getMillis()), record.getSourceClassName(), record.getLoggerName(),
					record.getLevel().getLocalizedName(), record.getMessage(), record.getThrown());
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
