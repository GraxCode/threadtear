package me.nov.threadtear.execution;

public enum ExecutionCategory {
	GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), STRINGER("Stringer Obfuscator"), ZKM("ZKM Obfuscator"), ALLATORI("Allatori Obfuscator"), TOOLS("Tools"),
	SB27("Superblaubeere27 Obfuscator");

	public final String name;

	ExecutionCategory(String name) {
		this.name = name;
	}
}
