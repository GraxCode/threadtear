package me.nov.threadtear.execution;

public enum ExecutionTag {
  POSSIBLE_DAMAGE("Code can possibly get inexecutable."), RUNNABLE("Code should remain runnable."),
  POSSIBLE_VERIFY_ERR("Code could throw VerifyErrors, use \"-noverify\" at execution."),
  POSSIBLY_MALICIOUS("Can possibly run malicious code."), BETTER_DECOMPILE("Improves decompilability."),
  BETTER_DEOBFUSCATE("Can help other executions."), SHRINK("Shrinks the file size.");

  public final String info;

  ExecutionTag(String info) {
    this.info = info;
  }

}
