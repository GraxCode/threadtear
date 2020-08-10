package me.nov.threadtear;

import me.nov.threadtear.execution.Clazz;
import me.nov.threadtear.execution.Execution;
import me.nov.threadtear.logging.LogWrapper;
import me.nov.threadtear.security.VMSecurityManager;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ThreadtearCore {
  public static void configureEnvironment() throws Exception {
    System.setProperty("file.encoding", "UTF-8");
    Field charset = Charset.class.getDeclaredField("defaultCharset");
    charset.setAccessible(true);
    charset.set(null, null);
  }

  public static void configureLoggers() {
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("logfile"));
    LogWrapper.logger.addLogger(LoggerFactory.getLogger("form"));
  }

  public static void run(List<Clazz> classes, List<Execution> executions, boolean disableSecurity, boolean verbose) {
    LogWrapper.logger.info("Threadtear version {}", CoreUtils.getVersion());
    LogWrapper.logger.info("Executing {} tasks on {} classes!", executions.size(), classes.size());
    if (!disableSecurity) {
      LogWrapper.logger.info("Initializing security manager if something goes horribly wrong");
      System.setSecurityManager(new VMSecurityManager());
    } else {
      LogWrapper.logger.warning("Starting without security manager!");
    }
    List<Clazz> ignoredClasses = classes.stream().filter(c -> !c.transform).collect(Collectors.toList());
    LogWrapper.logger.warning("{} classes will be ignored", ignoredClasses.size());
    classes.removeIf(c -> !c.transform);
    Map<String, Clazz> map = classes.stream().collect(Collectors.toMap(c -> c.node.name, c -> c, (c1, c2) -> {
      LogWrapper.logger.warning("Warning: Duplicate class definition of {}, one class may not get decrypted", c1.node.name);
      return c1;
    }));
    LogWrapper.logger.info("If an execution doesn't work properly on your file, please open an issue: https://github" +
      ".com/GraxCode/threadtear/issues");
    RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    List<String> arguments = runtimeMxBean.getInputArguments();
    if (!CoreUtils.isNoverify()) {
      LogWrapper.logger.warning("You started threadtear without -noverify, this results in less decryption! Your VM " +
        "args: {}", arguments);
      try {
        Thread.sleep(2000);
      } catch (InterruptedException e1) {
      }
    }
    executions.forEach(e -> {
      long ms = System.currentTimeMillis();
      LogWrapper.logger.info("Executing " + e.getClass().getName());
      boolean success = e.execute(map, verbose);
      LogWrapper.logger.collectErrors(null);
      LogWrapper.logger.errorIf("Finish with {}. Took {} ms.", !success, success ? "success" : "failure",
        (System.currentTimeMillis() - ms));
    });
    classes.addAll(ignoredClasses); // re-add ignored
    // classes to export them
    try {
      Thread.sleep(500);
    } catch (InterruptedException e1) {
    }
    LogWrapper.logger.info("Successful completion!");
    System.setSecurityManager(null);
  }

  // TODO: make a CLI
}
