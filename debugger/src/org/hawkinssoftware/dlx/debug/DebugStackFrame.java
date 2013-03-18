package org.hawkinssoftware.dlx.debug;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugStackFrame {
	public final int depth;

	final DebugStackFrame predecessor;
	DebugStackFrame successor;

	public DebugStackFrame() {
		this(null);
	}

	private DebugStackFrame(DebugStackFrame predecessor) {
		this.predecessor = predecessor;
		if (predecessor == null)
			depth = 0;
		else
			depth = predecessor.depth + 1;
	}

	public DebugStackFrame push() {
		return new DebugStackFrame(this);
	}

	public DebugStackFrame pop() {
		assert successor == null : "Cannot pop in the middle of the stack!";

		if (predecessor == null)
			return null;

		predecessor.successor = null;
		return predecessor;
	}

	public boolean isBaseFrame() {
		return predecessor == null;
	}

	public DebugStackFrame getPredecessor() {
		return predecessor;
	}

	public DebugStackFrame getSuccessor() {
		return successor;
	}
}
