package me.nov.threadtear.decompiler;

import java.io.File;

public interface IDecompilerBridge {
  public void setAggressive(boolean aggressive);

  public String decompile(File archive, String name, byte[] bytes);
}
