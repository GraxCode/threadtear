package me.nov.threadtear.security;

import java.io.FileDescriptor;
import java.security.Permission;

import me.nov.threadtear.Threadtear;

public class VMSecurityManager extends SecurityManager {
  private static final String MSG = "An execution ran code that it's not supposed to. If you think this is a false call, open an issue on GitHub.";
  private static boolean grantAll;

  @Override
  public void checkPermission(Permission perm) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkPermission(Permission perm, Object context) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkExec(String cmd) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkLink(String lib) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkWrite(FileDescriptor fd) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkWrite(String file) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkDelete(String file) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkConnect(String host, int port) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkConnect(String host, int port, Object context) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkPropertiesAccess() {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkCreateClassLoader() {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkSecurityAccess(String target) {
    if (grantAccess())
      return;
    throw new SecurityException(MSG);
  }

  @Override
  public void checkPackageAccess(String pkg) {
    if (pkg.equals("java.lang.reflect") || pkg.startsWith("sun.") || pkg.startsWith(Threadtear.class.getPackage().getName())) {
      if (grantAccess())
        return;
      throw new SecurityException(MSG);
    }
  }

  private static final String granted = "sun\\..*";

  private static final boolean grantAccess() {
    if (grantAll) {
      return true;
    }
    // check all stacktrace elements if they're in classpath
    for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
      if (ste.getClassName().matches(granted))
        continue;
      if (!isLocal(ste.getClassName())) {
        Threadtear.logger.warning("Dynamic class was blocked trying to execute forbidden code: " + ste.getClassName());
        return false;
      }
    }
    return true;
  }

  public static final boolean isLocal(String name) {
    try {
      grantAll = true; // we have to grant everything for a millisecond, otherwise we would end up in a loop

      /*
       * This way you could check if they are really in the jar file itself
       * 
       * return Class.forName(name, false, ClassLoader.getSystemClassLoader()).getProtectionDomain().getCodeSource().getLocation().getPath()
       * .equals(Threadtear.class.getProtectionDomain().getCodeSource().getLocation().getPath());
       */

      Class.forName(name, false, ClassLoader.getSystemClassLoader());
      // no exception, class is local
      return true;
    } catch (Throwable e) {
      // class not found or whatever, class is not local
      return false;
    } finally {
      // disable bypass
      grantAll = false;
    }
  }
}
