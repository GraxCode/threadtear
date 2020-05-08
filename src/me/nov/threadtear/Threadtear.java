package me.nov.threadtear;

import java.awt.*;
import java.lang.management.*;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.*;

import com.github.weisj.darklaf.settings.ThemeSettings;

import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.io.Clazz;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.security.VMSecurityManager;
import me.nov.threadtear.swing.Utils;
import me.nov.threadtear.swing.frame.LogFrame;
import me.nov.threadtear.swing.laf.LookAndFeel;
import me.nov.threadtear.swing.listener.ExitListener;
import me.nov.threadtear.swing.panel.*;

public class Threadtear extends JFrame {
  private static final long serialVersionUID = 1L;
  public static final LogWrapper logger = new LogWrapper();
  public TreePanel listPanel;
  public ConfigurationPanel configPanel;
  private LogFrame logFrame;

  public Threadtear() {
    this.initBounds();
    this.setTitle("Threadtear " + Utils.getVersion());
    this.setIconImage(new ImageIcon(getClass().getResource("/res/threadtear.png")).getImage());
    this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    this.addWindowListener(new ExitListener(this));
    this.initializeFrame();
    this.initializeMenu();
  }

  private void initializeMenu() {
    JMenuBar bar = new JMenuBar();
    JMenu help = new JMenu("Help");
    JMenuItem about = new JMenuItem("About threadtear " + Utils.getVersion());
    about.addActionListener(l -> {
      JOptionPane.showMessageDialog(this,
          "<html>This tool is not intended to produce runnable code, but rather analyzable code.<br>Add executions to the list on the left side. Make sure to have them in right order."
              + "<br>If you click \"Run\", they will get executed in order and transform the loaded classes.<br><br>Threadtear was made by <i>noverify</i> a.k.a <i>GraxCode</i> in 2020.<br><br>"
              + "This project is licensed under GNU GENERAL PUBLIC LICENSE Version 3.<br>You are welcome to contribute to this project on GitHub!<br><br><b>Do <i>NOT</i> use this on files you don't have legal rights for!</b>",
          "About", JOptionPane.INFORMATION_MESSAGE);
    });
    help.add(about);
    JMenuItem log = new JMenuItem("Open log frame");
    log.addActionListener(l -> {
      if (logFrame != null) {
        logFrame.setVisible(true);
      }
    });
    help.add(log);
    JMenuItem laf = new JMenuItem("Look and feel");
    laf.addActionListener(l -> {
      ThemeSettings.showSettingsDialog(this);
    });
    help.add(laf);
    bar.add(help);
    this.setJMenuBar(bar);

  }

  private void initializeFrame() {
    this.setLayout(new BorderLayout(16, 16));
    this.add(listPanel = new TreePanel(this), BorderLayout.CENTER);
    this.add(configPanel = new ConfigurationPanel(this), BorderLayout.SOUTH);
  }

  private void initBounds() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int width = (int) (screenSize.width * 0.5);
    int height = (int) (screenSize.height * 0.5);
    setBounds(screenSize.width / 2 - width / 2, screenSize.height / 2 - height / 2, width, height);
    setMinimumSize(new Dimension((int) (width / 1.25), (int) (height / 1.25)));
  }

  public static void main(String[] args) throws Exception {
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

  public void run(boolean verbose, boolean disableSecurity) {
    List<Clazz> classes = listPanel.classList.classes;
    ArrayList<Execution> executions = listPanel.executionList.getExecutions();
    if (classes == null || classes.isEmpty()) {
      JOptionPane.showMessageDialog(this, "You have to load a jar file first.");
      return;
    }
    if (executions.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No executions are selected.");
      return;
    }
    if (disableSecurity) {
      if (JOptionPane.showConfirmDialog(this, "Are you sure you wan't to start without a security manager?\nMalicious code could be executed!", "Warning",
          JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return;
      }
    }
    if (logFrame == null) {
      logFrame = new LogFrame();
    }
    logFrame.setVisible(true);
    SwingUtilities.invokeLater(() -> {
      new Thread(() -> {
        logger.info("Executing " + executions.size() + " tasks on " + classes.size() + " classes!");
        if (!disableSecurity) {
          logger.info("Initializing security manager if something goes horribly wrong");
          System.setSecurityManager(new VMSecurityManager());
        } else {
          logger.warning("Starting without security manager!");
        }
        List<Clazz> ignoredClasses = classes.stream().filter(c -> !c.transform).collect(Collectors.toList());
        logger.warning("{} classes will be ignored", ignoredClasses.size());
        classes.removeIf(c -> !c.transform);
        Map<String, Clazz> map = classes.stream().collect(Collectors.toMap(c -> c.node.name, c -> c));
        logger.info("If an execution doesn't work properly on your file, please open an issue: https://github.com/GraxCode/threadtear/issues");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        if (!arguments.contains("-Xverify:none")) {
          logger.warning("You started threadtear without -noverify, this result in less decryption! Your VM args: {}", arguments);
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e1) {
          }
        }
        executions.forEach(e -> {
          long ms = System.currentTimeMillis();
          logger.info("Executing " + e.getClass().getName());
          boolean success = e.execute(map, verbose);
          String finish = "Finish with {}. Took {} ms.";
          if (success) {
            logger.info(finish, "success", (System.currentTimeMillis() - ms));
          } else {
            logger.error(finish, "failure", (System.currentTimeMillis() - ms));
          }
          logFrame.append("-----------------------------------------------------------\n");
        });
        classes.addAll(ignoredClasses); // re-add ignored classes to export them
        try {
          Thread.sleep(500);
        } catch (InterruptedException e1) {
        }
        logger.info("Successful completion!");
        System.setSecurityManager(null);
        listPanel.classList.loadTree(classes);
        configPanel.run.setEnabled(true);
      }, "Execution-Thread").start();
    });
  }
}
