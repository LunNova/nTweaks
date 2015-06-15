package me.nallar.ntweaks;

public class Util {
	public static long chunkKey(int x, int z) {
		return (long) x & 4294967295L | ((long) z & 4294967295L) << 32;
	}
}
