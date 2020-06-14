package me.nov.threadtear.swing.panel;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class StatusBar extends JToolBar {

  private final Box contentBox;
  private final JLabel messageLabel;
  private final JLabel progressLabel;
  private final LoadingIndicator loadingIndicator;
  private final JProgressBar progressBar;

  public StatusBar() {
    setFloatable(false);
    setMargin(new Insets(4,16,4,16));

    add(messageLabel = new JLabel());
    add(Box.createHorizontalGlue());

    Box loadingBox = Box.createHorizontalBox();
    loadingBox.add(progressBar = new JProgressBar());
    loadingBox.add(Box.createHorizontalStrut(4));
    loadingBox.add(loadingIndicator = new LoadingIndicator());
    loadingBox.add(progressLabel = new JLabel());
    progressBar.setMaximumSize(new Dimension(200, 200));
    loadingIndicator.setRunning(false);
    loadingIndicator.setVisible(false);
    progressBar.setVisible(false);
    add(loadingBox);

    add(contentBox = Box.createHorizontalBox());
  }

  @Override
  public boolean isVisible() {
    if (!super.isVisible()) return false;
    Insets ins = getInsets();
    Insets margins = getMargin();
    return getHeight() > ins.top + ins.bottom + margins.top + margins.bottom;
  }

  public void runWithLoadIndicator(Runnable action) {
    runWithLoadIndicator(null, action);
  }

  public void runWithLoadIndicator(String actionName, Runnable action) {
    progressLabel.setText(actionName);
    progressLabel.setVisible(true);
    loadingIndicator.setRunning(true);
    loadingIndicator.setEnabled(true);
    loadingIndicator.setVisible(true);
    doLayout();
    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      @Override
      protected Void doInBackground() {
        action.run();
        loadingIndicator.setRunning(false);
        loadingIndicator.setVisible(false);
        progressLabel.setVisible(false);
        return null;
      }
    };
    worker.execute();
  }

  public void runWithProgressbar(Consumer<ProgressCallback> action) {
    runWithProgressbar(null, action, 0, 100);
  }

  public void runWithProgressbar(String actionName, Consumer<ProgressCallback> action) {
    runWithProgressbar(actionName, action, 0, 100);
  }

  public void runWithProgressbar(String actionName, Consumer<ProgressCallback> action, int minValue, int maxValue) {
    progressLabel.setText(actionName);
    progressLabel.setVisible(true);
    progressBar.setVisible(true);
    progressBar.setMinimum(minValue);
    progressBar.setMaximum(maxValue);
    progressBar.setValue(minValue);
    doLayout();
    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
      private final ProgressCallback callback = i -> SwingUtilities.invokeLater(() -> progressBar.setValue(i));

      @Override
      protected Void doInBackground() {
        action.accept(callback);
        progressBar.setVisible(false);
        progressLabel.setVisible(false);
        return null;
      }
    };
    worker.execute();
  }

  public void setMessage(String message) {
    messageLabel.setText(message);
  }

  public String getMessage() {
    return messageLabel.getText();
  }

  public void addStatusComponent(JComponent component) {
    contentBox.add(component);
  }

  public void removeStatusComponent(JComponent component) {
    contentBox.remove(component);
  }

  public boolean isIndicatorRunning() {
    return loadingIndicator.isVisible();
  }

  public boolean isProgressbarRunning() {
    return progressBar.isVisible();
  }

  public interface ProgressCallback extends Consumer<Integer> {
    void setProgress(int progress);

    @Override
    default void accept(Integer integer) {
      setProgress(integer);
    }
  }
}
