package me.nov.threadtear.swing.frame;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.swing.frame.AnalysisFrame;
import me.nov.threadtear.vm.Sandbox;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;

/**
 * A wrapper around {@link AnalysisFrame} that will
 * display class, method and exception for quick and easy
 * debugging.
 */
public class BytecodeDebugger {
  /**
   * Blocking.
   */
  public static void show(MethodNode method, Exception error) {
    final ClassNode clazz = Sandbox.createClassProxy("ProxyClass");
    clazz.methods.add(method);
    show(clazz, error);
  }

  /**
   * Blocking.
   */
  public static void show(ClassNode clazz, Exception error) {
    String message = getErrorMessage(error);
    String title = getErrorTitle(error);
    show(clazz, title, message);
  }

  private static String getErrorMessage(Exception e) {
    try (StringWriter sw = new StringWriter()) {
      try (PrintWriter pw = new PrintWriter(sw)) {
        e.printStackTrace(pw);
        return sw.toString();
      }
    } catch (IOException ignored) {
      return "";
    }
  }

  private static String getErrorTitle(Exception error) {
    String title;
    if (error instanceof InvocationTargetException) {
      InvocationTargetException e = (InvocationTargetException) error;
      title = e.getTargetException().getStackTrace()[0].toString();
    } else {
      title = "Error";
    }
    return String.format("Bytecode Debugger - %s", title);
  }

  private static void show(ClassNode clazz, String title, String message) {
    final AnalysisFrame frame = new AnalysisFrame(title, new Clazz(clazz, null, null));
    frame.setVisible(true);

    // wait to spawn
    while (!frame.isVisible()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e1) {
        break;
      }
    }

    showMessageBox(title, message);

    // wait to close
    while (frame.isVisible()) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e1) {
        break;
      }
    }
  }

  private static void showMessageBox(String title, String message) {
    JOptionPane pane = new JOptionPane(message, JOptionPane.ERROR_MESSAGE);
    JDialog dialog = pane.createDialog(title);
    dialog.setModal(false);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
  }
}
