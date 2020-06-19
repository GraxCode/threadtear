package me.nov.threadtear.execution;

public enum ExecutionCategory {
  GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), TOOLS("Tools"), STRINGER("Obfuscators.Stringer"),
  ZKM("Obfuscators.ZKM"), ALLATORI("Obfuscators.Allatori"), SB27("Obfuscators.Superblaubeere27"),
  DASHO("Obfuscators.DashO"), PARAMORPHISM("Obfuscators.Paramorphism");

  public final String name;

  ExecutionCategory(String name) {
    this.name = name;
  }
}
