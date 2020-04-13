package me.nov.threadtear;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import me.nov.threadtear.asm.Clazz;
import me.nov.threadtear.asm.io.JarIO;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.logging.CustomOutputStream;
import me.nov.threadtear.swing.component.panel.ConfigurationPanel;
import me.nov.threadtear.swing.component.panel.ListPanel;
import me.nov.threadtear.swing.frame.LogFrame;
import me.nov.threadtear.swing.laf.LookAndFeel;
import me.nov.threadtear.swing.listener.ExitListener;

public class Threadtear extends JFrame {
	private static final long serialVersionUID = 1L;
	public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	private ListPanel listPanel;

	public Threadtear() {
		this.initBounds();
		this.setTitle("Threadtear");
		this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new ExitListener(this));
		this.initializeFrame();
		this.initializeMenu();
	}

	private void initializeMenu() {
		JMenuBar bar = new JMenuBar();
		JMenu help = new JMenu("Help");
		JMenuItem about = new JMenuItem("About Threadtear");
		about.addActionListener(e -> {
			JOptionPane.showMessageDialog(this,
					"<html>This tool is not intended to produce runnable code, but rather analyzable code.<br>Add executions to the list on the left side. Make sure to have them in right order."
					+ "<br>If you click \"Run\", they will get executed in order and transform the loaded classes.<br><br>Threadtear was made by noverify a.k.a GraxCode.",
					"About", JOptionPane.INFORMATION_MESSAGE);
		});
		help.add(about);
		bar.add(help);
		this.setJMenuBar(bar);

	}

	private void initializeFrame() {
		this.setLayout(new BorderLayout(16, 16));
		this.add(listPanel = new ListPanel(), BorderLayout.CENTER);
		this.add(new ConfigurationPanel(this), BorderLayout.SOUTH);
	}

	private void initBounds() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int width = (int) (screenSize.width * 0.5);
		int height = (int) (screenSize.height * 0.5);
		setBounds(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2, width, height);
	}

	public static void main(String[] args) throws Exception {
		logger.setLevel(Level.ALL);
		LookAndFeel.setLookAndFeel();
		configureEnvironment();
		new Threadtear().setVisible(true);
	}

	private static void configureEnvironment() throws Exception {
		System.setProperty("file.encoding", "UTF-8");
		Field charset = Charset.class.getDeclaredField("defaultCharset");
		charset.setAccessible(true);
		charset.set(null, null);
	}

	public void run(boolean verbose, boolean frames, boolean ignoreErr, boolean noSign) {
		ArrayList<Clazz> classes = listPanel.classList.classes;
		ArrayList<Execution> executions = listPanel.executionList.getExecutions();
		LogFrame logFrame = new LogFrame();
		logFrame.setVisible(true);
		logger.setUseParentHandlers(true);
		logger.addHandler(new LogFrame.LogHandler(logFrame.area));
		System.setErr(new PrintStream(new CustomOutputStream(logger, Level.SEVERE)));
		System.setOut(new PrintStream(new CustomOutputStream(logger, Level.FINE)));
		SwingUtilities.invokeLater(() -> {
			new Thread(() -> {
				logger.info("Executing " + executions.size() + " tasks on " + classes.size() + " classes!");
				List<Clazz> ignoredClasses = classes.stream().filter(c -> !c.transform).collect(Collectors.toList());
				logger.info(ignoredClasses.size() + " classes will be ignored");
				classes.removeIf(c -> !c.transform);
				logger.info("If an execution doesn't work properly on your file, please open an issue: https://github.com/GraxCode/threadtear/issues");
				executions.forEach(e -> {
					long ms = System.currentTimeMillis();
					logger.info("Executing " + e.getClass().getName());
					boolean success = e.execute(classes, verbose, ignoreErr);
					logger.info("Finish with " + (success ? "success" : "failure") + ". Took " + (System.currentTimeMillis() - ms)
							+ " ms");
				});
				classes.addAll(ignoredClasses); //re-add ignored classes to export them
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
				}
				logger.info("Successful finish!");
				File inputFile = listPanel.classList.inputFile;
				JFileChooser jfc = new JFileChooser(inputFile.getParentFile());
				jfc.setAcceptAllFileFilterUsed(false);
				jfc.setSelectedFile(inputFile);
				jfc.setDialogTitle("Save transformed jar archive");
				jfc.setFileFilter(new FileNameExtensionFilter("Java Package (*.jar)", "jar"));
				int result = jfc.showSaveDialog(this);
				if (result == JFileChooser.APPROVE_OPTION) {
					File output = jfc.getSelectedFile();
					JarIO.saveAsJar(inputFile, output, classes, noSign);
					logger.info("Saving successful!");
				}
			}).start();
		});
	}
}
