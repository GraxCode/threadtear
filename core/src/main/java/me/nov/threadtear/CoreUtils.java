package me.nov.threadtear;

import me.nov.threadtear.vm.VM;
import org.objectweb.asm.tree.ClassNode;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Objects;

public class CoreUtils {
  public static String getVersion() {
    try {
      return Objects.requireNonNull(CoreUtils.class.getPackage().getImplementationVersion());
    } catch (NullPointerException e) {
      return "(dev)";
    }
  }

  public static boolean isNoverify() {
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();
    return arguments.contains("-Xverify:none");
  }

  public static boolean isAttachable() {
    try {
      Class.forName("com.sun.tools.attach.VirtualMachine");
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static int getClassSupport() {
    VM vm = VM.constructVM(null);
    int i = -1;
    try {
      for (i = 49; i < 60; i++) {
        ClassNode cn = new ClassNode();
        cn.version = i;
        cn.name = String.valueOf(i);
        cn.superName = "java/lang/Object";
        vm.explicitlyPreload(cn);
        if (vm.loadClass(cn.name) == null)
          return i - 1;
      }
    } catch (Exception e) {
      // ignore
    }
    return i - 1;
  }
}
