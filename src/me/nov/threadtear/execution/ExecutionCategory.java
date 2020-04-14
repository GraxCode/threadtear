package me.nov.threadtear.execution;

public enum ExecutionCategory {
	GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), STRINGER("Stringer Obfuscator"), ZKM("ZKM Obfuscator");

	public final String name;

	ExecutionCategory(String name) {
		this.name = name;
	}

}
