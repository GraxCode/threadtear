package me.nov.threadtear.security;

import me.nov.threadtear.ThreadtearCore;
import me.nov.threadtear.logging.LogWrapper;

import java.io.FileDescriptor;
import java.lang.reflect.ReflectPermission;
import java.net.InetAddress;
import java.security.Permission;


public final class VMSecurityManager extends SecurityManager {
  private static final String granted = "sun\\..*";
  private boolean grantAll;
  private boolean checkReflection = true;

  @Override
  public final void checkPermission(Permission perm) {
    if (perm instanceof ReflectPermission || perm instanceof RuntimePermission)
      return;
    throwIfNotGranted();
  }

  @Override
  public final void checkPermission(Permission perm, Object context) {
    throwIfNotGranted();
  }

  @Override
  public final void checkExec(String cmd) {
    throwIfNotGranted();
  }

  @Override
  public final void checkLink(String lib) {
    throwIfNotGranted();
  }

  @Override
  public final void checkWrite(FileDescriptor fd) {
    throwIfNotGranted();
  }

  @Override
  public final void checkWrite(String file) {
    throwIfNotGranted();
  }

  @Override
  public final void checkDelete(String file) {
    throwIfNotGranted();
  }

  @Override
  public final void checkConnect(String host, int port) {
    throwIfNotGranted();
  }

  @Override
  public final void checkConnect(String host, int port, Object context) {
    throwIfNotGranted();
  }

  @Override
  public final void checkPropertiesAccess() {
    throwIfNotGranted();
  }

  @Override
  public final void checkCreateClassLoader() {
    if (checkReflection)
      throwIfNotGranted();
  }

  @Override
  public final void checkSecurityAccess(String target) {
    throwIfNotGranted();
  }

  @Override
  public final void checkAccept(String host, int port) {
    throwIfNotGranted();
  }

  @Override
  public final void checkExit(int status) {
    throwIfNotGranted();
  }

  @Override
  public final void checkListen(int port) {
    throwIfNotGranted();
  }

  @Override
  public final void checkMulticast(InetAddress maddr) {
    throwIfNotGranted();
  }

  @Override
  public final void checkPackageAccess(String pkg) {
    if (pkg.startsWith("javax.swing") || pkg.startsWith("sun.misc") || pkg.startsWith(ThreadtearCore.class.getPackage().getName()) || checkReflection(pkg)) {
      throwIfNotGranted();
    }
  }

  private final boolean checkReflection(String pkg) {
    return checkReflection && pkg.startsWith("java.lang.reflect");
  }

  public final void allowReflection(boolean allow) {
    if (grantAccess())
      checkReflection = !allow;
  }

  private final void throwIfNotGranted() {
    if (!grantAccess())
      throw new SecurityException("An execution ran code that it's not supposed to. If you think this is a " +
        "false call, open an issue on GitHub.");
  }

  private final boolean grantAccess() {
    if (grantAll) {
      return true;
    }
    // check all stacktrace elements if they're in classpath
    for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
      if (ste.getClassName().matches(granted))
        continue;
      if (!isLocal(ste.getClassName())) {
        LogWrapper.logger.warning("Dynamic class was blocked trying to execute forbidden code: {}, {}",
          ste.getClassName(), Thread.currentThread().getStackTrace()[3].getMethodName());
        return false;
      }
    }
    return true;
  }

  public final boolean isLocal(String name) {
    try {
      grantAll = true; // we have to grant everything for
      // the next check, otherwise we would end up in a loop
      /*
       * This way you could check if they are really in
       * the jar file itself
       *
       * return Class.forName(name, false, ClassLoader
       * .getSystemClassLoader()).getProtectionDomain()
       * .getCodeSource().getLocation().getPath()
       * .equals(Threadtear.class.getProtectionDomain()
       * .getCodeSource().getLocation().getPath());
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
