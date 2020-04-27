package me.nov.threadtear.execution;

public enum ExecutionCategory {
	GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), TOOLS("Tools"), STRINGER("Obfuscators.Stringer Obfuscator"), ZKM("Obfuscators.ZKM Obfuscator"), ALLATORI("Obfuscators.Allatori Obfuscator"),
	SB27("Obfuscators.Superblaubeere27 Obfuscator"), DASHO("Obfuscators.DashO Obfuscator");

	public final String name;

	ExecutionCategory(String name) {
		this.name = name;
	}
}
