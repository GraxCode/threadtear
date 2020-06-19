package me.nov.threadtear;

import com.github.weisj.darklaf.components.help.HelpMenuItem;
import com.github.weisj.darklaf.settings.ThemeSettings;
import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.security.VMSecurityManager;
import me.nov.threadtear.swing.SwingUtils;
import me.nov.threadtear.swing.frame.LogFrame;
import me.nov.threadtear.swing.laf.LookAndFeel;
import me.nov.threadtear.swing.listener.ExitListener;
import me.nov.threadtear.swing.panel.ConfigurationPanel;
import me.nov.threadtear.swing.panel.StatusBar;
import me.nov.threadtear.swing.panel.TreePanel;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Threadtear extends JFrame {
  private static final long serialVersionUID = 1L;
  public TreePanel listPanel;
  public ConfigurationPanel configPanel;
  public LogFrame logFrame;
  public StatusBar statusBar;
  private static Threadtear instance;

  public static Threadtear getInstance() {
    if (instance == null) instance = new Threadtear();
    return instance;
  }

  public Threadtear() {
    logFrame = new LogFrame();
    this.initBounds();
    this.setTitle("Threadtear " + CoreUtils.getVersion());
    this.setIconImage(SwingUtils.iconToFrameImage(SwingUtils.getIcon("threadtear.svg", true), this));
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new ExitListener(this));
    this.initializeFrame();
    this.initializeMenu();
  }

  private void initializeMenu() {
    JMenuBar bar = new JMenuBar();
    JMenu file = new JMenu("File");
    JMenuItem ws = new JMenuItem("Reset Workspace");
    ws.addActionListener(l -> {
      if (JOptionPane
        .showConfirmDialog(Threadtear.this, "Do you" + " really want to reset your " + "workspace?", "Warning",
          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
        this.dispose();
        System.gc();
        new Threadtear().setVisible(true);
      }
    });
    file.add(ws);
    JMenuItem load = new JMenuItem("Load file");
    load.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));
    load.addActionListener(l -> {
      UIManager.put("FileChooser.readOnly", Boolean.TRUE);
      JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
      jfc.setMultiSelectionEnabled(false);
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setDialogTitle("Load file");
      jfc.setFileFilter(new FileNameExtensionFilter("Java" + " class or class archive", "jar", "class"));
      int result = jfc.showOpenDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File input = jfc.getSelectedFile();
        listPanel.classList.onFileDrop(input);
      }
    });
    file.add(load);
    bar.add(file);
    JMenu help = new JMenu("Help");
    JMenuItem log = new JMenuItem("Open logging frame");
    log.setIcon(LogFrame.getIcon());
    log.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK));
    log.addActionListener(l -> logFrame.setVisible(true));
    help.add(log);
    JMenuItem laf = new JMenuItem("Look and feel settings");
    laf.setIcon(ThemeSettings.getInstance().getIcon());
    laf.addActionListener(l -> ThemeSettings.showSettingsDialog(this));
    JMenuItem about = new HelpMenuItem("About threadtear " + CoreUtils.getVersion());
    about.addActionListener(l -> JOptionPane.showMessageDialog(this,
      "<html>This tool is " + "not intended to produce runnable " + "code, but rather " + "analyzable code" +
        ".<br>Add executions to the list on " + "the left side. Make sure to have " +
        "them in right order." + "<br>If you " + "click \"Run\", they will get " +
        "executed in order and transform the " + "loaded classes.<br><br>Threadtear " +
        "was made by <i>noverify</i> a.k.a " + "<i>GraxCode</i> in 2020.<br><br>" +
        "This project is licensed under GNU " + "GENERAL PUBLIC LICENSE Version 3" +
        ".<br>You are welcome to contribute " + "to this project on " +
        "GitHub!<br><br><b>Do <i>NOT</i> use " + "this on files you don't have legal " + "rights for!</b>",
      "About", JOptionPane.INFORMATION_MESSAGE));
    help.add(about);
    help.add(laf);
    bar.add(help);
    this.setJMenuBar(bar);
  }

  private void initializeFrame() {
    JPanel content = new JPanel(new BorderLayout());
    content.add(SwingUtils.withEmptyBorder(SwingUtils.horizontallyDivided(
      listPanel = new TreePanel(this),
      SwingUtils.pad(configPanel = new ConfigurationPanel(this), 10, 0, 8, 0)
    ), 16), BorderLayout.CENTER);
    content.add(statusBar = new StatusBar(), BorderLayout.SOUTH);
    setContentPane(content);
  }

  private void initBounds() {
    Rectangle screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    int width = screenSize.width / 2;
    int height = screenSize.height / 2;
    setBounds(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2, width, height);
    setMinimumSize(new Dimension((int) (width / 1.25), (int) (height / 1.25)));
  }

  public static void main(String[] args) throws Exception {
    LookAndFeel.init();
    LookAndFeel.setLookAndFeel();
    configureEnvironment();
    configureLoggers();
    getInstance().setVisible(true);
  }

  private static void configureEnvironment() throws Exception {
    System.setProperty("file.encoding", "UTF-8");
    Field charset = Charset.class.getDeclaredField("defaultCharset");
    charset.setAccessible(true);
    charset.set(null, null);
  }

  private static void configureLoggers() {
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("logfile"));
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("console"));
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("form"));
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("statusbar"));
  }

  public void run(boolean verbose, boolean disableSecurity) {
    List<Clazz> classes = listPanel.classList.classes;
    ArrayList<Execution> executions = listPanel.executionList.getExecutions();
    if (classes == null || classes.isEmpty()) {
      return;
    }
    if (executions.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No executions to run.");
      return;
    }
    logFrame.setVisible(true);
    SwingUtilities.invokeLater(() -> new Thread(() -> {
      LogWrapper.logger.info("Executing {} tasks on {} classes!", executions.size(), classes.size());
      if (!disableSecurity) {
        LogWrapper.logger.info("Initializing security manager if something goes horribly wrong");
        System.setSecurityManager(new VMSecurityManager());
      } else {
        LogWrapper.logger.warning("Starting without security manager!");
      }
      List<Clazz> ignoredClasses = classes.stream().filter(c -> !c.transform).collect(Collectors.toList());
      LogWrapper.logger.warning("{} classes will be ignored", ignoredClasses.size());
      classes.removeIf(c -> !c.transform);
      Map<String, Clazz> map = classes.stream().collect(Collectors.toMap(c -> c.node.name, c -> c, (c1, c2) -> {
        LogWrapper.logger.warning("Warning: Duplicate class definition of {}, one class may not get decrypted", c1.node.name);
        return c1;
      }));
      LogWrapper.logger.info("If an execution doesn't work properly on your file, please open an issue: https://github" +
        ".com/GraxCode/threadtear/issues");
      RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
      List<String> arguments = runtimeMxBean.getInputArguments();
      if (!CoreUtils.isNoverify()) {
        LogWrapper.logger.warning("You started threadtear without -noverify, this results in less decryption! Your VM " +
          "args: {}", arguments);
        try {
          Thread.sleep(2000);
        } catch (InterruptedException e1) {
        }
      }
      executions.forEach(e -> {
        long ms = System.currentTimeMillis();
        LogWrapper.logger.info("Executing " + e.getClass().getName());
        boolean success = e.execute(map, verbose);
        LogWrapper.logger.collectErrors(null);
        LogWrapper.logger.errorIf("Finish with {}. Took {} ms.", !success, success ? "success" : "failure",
          (System.currentTimeMillis() - ms));
        logFrame.append("-----------------------------------------------------------\n");
      });
      classes.addAll(ignoredClasses); // re-add ignored
      // classes to export them
      try {
        Thread.sleep(500);
      } catch (InterruptedException e1) {
      }
      LogWrapper.logger.info("Successful completion!");
      System.setSecurityManager(null);
      listPanel.classList.loadTree(classes);
      configPanel.run.setEnabled(true);
    }, "Execution-Thread").start());
  }
}
