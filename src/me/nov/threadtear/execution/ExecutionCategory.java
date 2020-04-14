package me.nov.threadtear.execution;

public enum ExecutionCategory {
	GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), STRINGER3("Stringer 3"), ZKM("ZKM General"), ZKM8_11("ZKM 8 to 11");

	public final String name;

	ExecutionCategory(String name) {
		this.name = name;
	}

}
