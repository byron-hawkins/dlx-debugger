package org.hawkinssoftware.dlx.debug;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class ExecutionContext {
	public final int sourceStartIndex;
	public final int sourceEndIndex; // exclusive
	public final int assemblyStartIndex;
	public final int assemblyEndIndex; // exclusive

	ExecutionContext(int sourceStartIndex, int sourceEndIndex, int assemblyStartIndex, int assemblyEndIndex) {
		this.sourceStartIndex = sourceStartIndex;
		this.sourceEndIndex = sourceEndIndex;
		this.assemblyStartIndex = assemblyStartIndex;
		this.assemblyEndIndex = assemblyEndIndex;
	}
}
