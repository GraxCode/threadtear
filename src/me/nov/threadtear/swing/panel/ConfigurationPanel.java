package me.nov.threadtear.swing.panel;

import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;

import com.github.weisj.darklaf.icons.IconLoader;

import me.nov.threadtear.Threadtear;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.io.JarIO;
import me.nov.threadtear.swing.tree.component.ExecutionTreeNode;
import me.nov.threadtear.swing.tree.component.SortedTreeClassNode;

public class ConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 1L;
  private Threadtear main;
  private JCheckBox verbose;
  private JCheckBox watermark;
  private JCheckBox disableSecurity;
  private JCheckBox removeSignature;

  public ConfigurationPanel(Threadtear main) {
    this.main = main;
    this.setLayout(new GridLayout(2, 1, 16, 16));
    this.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    this.add(createCheckboxes());
    this.add(createBottomButtons());

  }

  private JPanel createCheckboxes() {
    JPanel panel = new JPanel(new GridLayout(2, 2));
    panel.add(verbose = new JCheckBox("Verbose logging"));
    verbose.setToolTipText("Log more information and print full stack traces.");
    panel.add(watermark = new JCheckBox("<html>Watermark <tt>MANIFEST.MF</tt>"));
    watermark.setToolTipText("<html>Adds a \"<tt>Deobfuscated-By\" attribute to the manifest file.");
    watermark.setSelected(true);
    panel.add(disableSecurity = new JCheckBox("<html>Disable <tt>SecurityManager</tt> protection"));
    disableSecurity.setToolTipText("Remove the protection agains unwanted executions. Could improve deobfuscation.");
    panel.add(removeSignature = new JCheckBox("Remove manifest signature"));
    removeSignature.setToolTipText("Remove the signature from the manifest file, if available.");
    return panel;
  }

  private JPanel createBottomButtons() {
    JPanel panel = new JPanel(new GridLayout(1, 4, 16, 16));
    JButton loadCfg = new JButton("Load config", IconLoader.get().loadSVGIcon("res/load_config.svg", false));
    loadCfg.addActionListener(l -> {
      JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setDialogTitle("Load config");
      jfc.setFileFilter(new FileNameExtensionFilter("Threadtear config file (*.tcf)", "tcf"));
      int result = jfc.showOpenDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File input = jfc.getSelectedFile();
        loadConfig(input);
      }
    });
    panel.add(loadCfg);
    JButton saveCfg = new JButton("Save config", IconLoader.get().loadSVGIcon("res/save_config.svg", false));
    saveCfg.addActionListener(l -> {
      JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setSelectedFile(new File(jfc.getCurrentDirectory(), "config.tcf"));
      jfc.setDialogTitle("Save config");
      jfc.setFileFilter(new FileNameExtensionFilter("Threadtear config file (*.tcf)", "tcf"));
      int result = jfc.showSaveDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File output = jfc.getSelectedFile();
        saveConfig(output);
      }
    });
    panel.add(saveCfg);
    JButton save = new JButton("Save as jar file", IconLoader.get().loadSVGIcon("res/save.svg", false));
    save.addActionListener(l -> {
      File inputFile = main.listPanel.classList.inputFile;
      if (inputFile == null) {
        JOptionPane.showMessageDialog(this, "You have to load a jar file first.");
        return;
      }
      JFileChooser jfc = new JFileChooser(inputFile.getParentFile());
      jfc.setAcceptAllFileFilterUsed(false);
      jfc.setSelectedFile(inputFile);
      jfc.setDialogTitle("Save transformed jar archive");
      jfc.setFileFilter(new FileNameExtensionFilter("Java Package (*.jar)", "jar"));
      int result = jfc.showSaveDialog(this);
      if (result == JFileChooser.APPROVE_OPTION) {
        File output = jfc.getSelectedFile();
        JarIO.saveAsJar(inputFile, output, main.listPanel.classList.classes, removeSignature.isSelected(), watermark.isSelected());
        Threadtear.logger.info("Saved to " + output.getAbsolutePath());
      }
    });
    panel.add(save);
    JButton run = new JButton("Run", IconLoader.get().loadSVGIcon("res/run.svg", false));
    run.addActionListener(l -> {
      main.run(verbose.isSelected(), disableSecurity.isSelected());
    });
    panel.add(run);
    return panel;
  }

  private void saveConfig(File output) {
    try {
      output.createNewFile();
      FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
          .configure(new Parameters().fileBased().setFile(output));
      FileBasedConfiguration config = builder.getConfiguration();
      config.setProperty("verbose", verbose.isSelected());
      config.setProperty("no_sec", disableSecurity.isSelected());
      config.setProperty("rem_sig", removeSignature.isSelected());

      File input = main.listPanel.classList.inputFile;
      if (input != null) {
        config.setProperty("file", input.getAbsolutePath());
        config.setProperty("ignored", main.listPanel.classList.classes.stream().filter(c -> !c.transform).map(c -> c.node.name).toArray(String[]::new));
      }
      ArrayList<Execution> executions = main.listPanel.executionList.getExecutions();
      if (!executions.isEmpty()) {
        config.setProperty("executions", executions.stream().map(e -> e.getClass().getName()).toArray(String[]::new));
      }
      builder.save();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadConfig(File input) {
    try {
      FileBasedConfigurationBuilder<FileBasedConfiguration> builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
          .configure(new Parameters().fileBased().setFile(input));
      Configuration config = builder.getConfiguration();
      verbose.setSelected(config.getBoolean("verbose"));
      watermark.setSelected(true);
      disableSecurity.setSelected(config.getBoolean("no_sec"));
      removeSignature.setSelected(config.getBoolean("rem_sig"));
      if (config.containsKey("file")) {
        File file = new File(config.getString("file"));
        if (file.exists()) {
          main.listPanel.classList.onJarLoad(file);
          if (config.containsKey("ignored")) {
            String[] ignored = config.getStringArray("ignored");
            for (String ignore : ignored) {
              main.listPanel.classList.ignore(ignore);
            }
            main.listPanel.classList.refreshIgnored();
            main.listPanel.classList.model.reload();
            main.listPanel.classList.updateAllNames((SortedTreeClassNode) main.listPanel.classList.model.getRoot());
            main.listPanel.classList.repaint();
          }
        } else {
          JOptionPane.showMessageDialog(this, "Input file not found: " + file.getAbsolutePath());
        }
      }
      if (config.containsKey("executions")) {
        String[] executions = config.getStringArray("executions");
        for (String execution : executions) {
          try {
            Execution e = (Execution) Class.forName(execution).newInstance();
            ((ExecutionTreeNode) main.listPanel.executionList.model.getRoot()).add(new ExecutionTreeNode(e, true));
            main.listPanel.executionList.model.reload();
            main.listPanel.executionList.repaint();
          } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Execution failed to initialize: " + execution + " (" + e.toString() + ")");
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
