package me.nov.threadtear;

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
}
