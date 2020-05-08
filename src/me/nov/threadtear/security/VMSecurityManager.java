package me.nov.threadtear.security;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

import me.nov.threadtear.Threadtear;

public class VMSecurityManager extends SecurityManager {
  private static boolean grantAll;

  @Override
  public void checkPermission(Permission perm) {
    throwIfNotGranted();
  }

  @Override
  public void checkPermission(Permission perm, Object context) {
    throwIfNotGranted();
  }

  @Override
  public void checkExec(String cmd) {
    throwIfNotGranted();
  }

  @Override
  public void checkLink(String lib) {
    throwIfNotGranted();
  }

  @Override
  public void checkWrite(FileDescriptor fd) {
    throwIfNotGranted();
  }

  @Override
  public void checkWrite(String file) {
    throwIfNotGranted();
  }

  @Override
  public void checkDelete(String file) {
    throwIfNotGranted();
  }

  @Override
  public void checkConnect(String host, int port) {
    throwIfNotGranted();
  }

  @Override
  public void checkConnect(String host, int port, Object context) {
    throwIfNotGranted();
  }

  @Override
  public void checkPropertiesAccess() {
    throwIfNotGranted();
  }

  @Override
  public void checkCreateClassLoader() {
    throwIfNotGranted();
  }

  @Override
  public void checkSecurityAccess(String target) {
    throwIfNotGranted();
  }

  @Override
  public void checkAccept(String host, int port) {
    throwIfNotGranted();
  }

  @Override
  public void checkExit(int status) {
    throwIfNotGranted();
  }

  @Override
  public void checkListen(int port) {
    throwIfNotGranted();
  }

  @Override
  public void checkMulticast(InetAddress maddr) {
    throwIfNotGranted();
  }

  @Override
  public void checkPackageAccess(String pkg) {
    if (pkg.startsWith("sun.misc") && pkg.startsWith(Threadtear.class.getPackage().getName())) {
      throwIfNotGranted();
    }
  }

  private final void throwIfNotGranted() {
    if (!grantAccess())
      throw new SecurityException("An execution ran code that it's not supposed to. If you think this is a false call, open an issue on GitHub.");
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
      grantAll = true; // we have to grant everything for the next check, otherwise we would end up in a loop

      /*
       * This way you could check if they are really in the jar file itself
       * 
       * return Class.forName(name, false, ClassLoader.getSystemClassLoader()).getProtectionDomain().getCodeSource().getLocation().getPath()
       * .equals(Threadtear.class.getProtectionDomain().getCodeSource().getLocation().getPath());
       */

      Class.forName(name, false, ClassLoader.getSystemClassLoader());
      // no exception thrown, class is local
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
