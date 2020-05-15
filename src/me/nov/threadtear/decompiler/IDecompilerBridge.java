package me.nov.threadtear.decompiler;

public interface IDecompilerBridge {
  public void setAggressive(boolean aggressive);

  public String decompile(String name, byte[] bytes);
}
