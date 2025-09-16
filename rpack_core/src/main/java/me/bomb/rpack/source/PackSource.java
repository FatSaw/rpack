package me.bomb.rpack.source;

public abstract class PackSource {
	public abstract String[] ids();
	public abstract byte[] get(String id);
}
