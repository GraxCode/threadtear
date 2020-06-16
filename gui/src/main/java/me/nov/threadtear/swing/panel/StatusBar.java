package me.nov.threadtear.swing.panel;

import com.github.weisj.darklaf.components.loading.LoadingIndicator;
import com.github.weisj.darklaf.ui.button.DarkButtonUI;
import me.nov.threadtear.Threadtear;
import me.nov.threadtear.swing.frame.LogFrame;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class StatusBar extends JToolBar {

  private final Box contentBox;
  private final JLabel messageLabel;
  private final JLabel progressLabel;
  private final LoadingIndicator loadingIndicator;
  private final JProgressBar progressBar;
  private final int padding = 2;

  public StatusBar() {
    setFloatable(false);

    add(Box.createHorizontalStrut(16));

    JButton logButton = new JButton();
    logButton.setToolTipText("Open logging frame");
    logButton.putClientProperty(DarkButtonUI.KEY_VARIANT, DarkButtonUI.VARIANT_BORDERLESS_RECTANGULAR);
    logButton.putClientProperty(DarkButtonUI.KEY_SQUARE, true);
    logButton.putClientProperty(DarkButtonUI.KEY_THIN, true);
    logButton.setBorder(BorderFactory.createEmptyBorder());
    logButton.setIcon(LogFrame.getIcon());
    logButton.addActionListener(e -> Threadtear.getInstance().logFrame.setVisible(true));

    add(logButton);

    add(Box.createHorizontalStrut(padding));
    add(messageLabel = createMessageLabel());
    add(Box.createHorizontalStrut(padding));
    add(Box.createHorizontalGlue());

    Box loadingBox = Box.createHorizontalBox();
    loadingBox.add(progressBar = new JProgressBar());
    loadingBox.add(Box.createHorizontalStrut(padding));
    loadingBox.add(loadingIndicator = new LoadingIndicator());
    loadingBox.add(progressLabel = new JLabel());
    progressBar.setMaximumSize(new Dimension(200, 200));
    loadingIndicator.setRunning(false);
    loadingIndicator.setVisible(false);
    progressBar.setVisible(false);
    add(loadingBox);

    add(contentBox = Box.createHorizontalBox());
    add(Box.createHorizontalStrut(16));
  }

  protected JLabel createMessageLabel() {
    return new JLabel() {
      @Override
      public Dimension getPreferredSize() {
          Dimension dim = StatusBar.this.getSize();
          Dimension pref = super.getPreferredSize();
          pref.width = Math.min(pref.width, dim.width / 2);
          return pref;
      }
    };
  }

  @Override
  public void doLayout() {
    super.doLayout();
    Insets ins = getInsets();
    int h = getHeight() - ins.bottom - ins.top;
    int y = ins.top;
    for (Component component : getComponents()) {
      int w = component.getWidth();
      int x = component.getX();
      component.setBounds(x, y, w, h);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension dim = super.getPreferredSize();
    dim.height += 2 * padding;
    return dim;
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
