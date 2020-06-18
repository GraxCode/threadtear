package me.nov.threadtear.decompiler;

import java.io.File;

public interface IDecompilerBridge {

  void setAggressive(boolean aggressive);

  String decompile(File archive, String name, byte[] bytes);
}
