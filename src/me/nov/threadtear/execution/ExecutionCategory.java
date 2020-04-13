package me.nov.threadtear.execution;

public enum ExecutionCategory {
	GENERIC("Generic"), CLEANING("Cleaning"), ANALYSIS("Analysis"), STRINGER39("Stringer 3 to 9");

	public final String name;

	ExecutionCategory(String name) {
		this.name = name;
	}

}
