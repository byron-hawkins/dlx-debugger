package org.hawkinssoftware.dlx.debug;

import java.util.List;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugHeapVariable {
	public final String name;
	public final int location;
	private final int entryCount;
	public final List<Integer> degrees;

	DebugHeapVariable(String name, int location, List<Integer> degrees) {
		this.name = name;
		this.location = location;
		this.degrees = degrees;

		int entryCount = 1;
		for (Integer degree : degrees) {
			entryCount *= degree;
		}
		this.entryCount = entryCount;
	}

	public int getEntryCount() {
		return entryCount;
	}
	
	public boolean isArray() {
		return !degrees.isEmpty();
	}

	public int getLocation(int... degrees) {
		int location = this.location;
		for (int degree : degrees) {
			
		}
		return location;
	}
}
