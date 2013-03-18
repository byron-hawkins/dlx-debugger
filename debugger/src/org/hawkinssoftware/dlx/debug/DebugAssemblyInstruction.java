package org.hawkinssoftware.dlx.debug;

import org.hawkinssoftware.dlx.DLXUtils;
import org.hawkinssoftware.dlx.simulator.DLXInstruction;
import org.hawkinssoftware.dlx.simulator.DLXOpcode;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugAssemblyInstruction {
	public static String registerName(int register) {
		switch (register) {
			case 0:
				return "Z";
			case 28:
				return "FP";
			case 29:
				return "SP";
			case 30:
				return "HP";
			case 31:
				return "RA";
		}
		return "R" + register;
	}

	public static final DebugAssemblyInstruction EMPTY = new DebugAssemblyInstruction(-1, DLXOpcode.ADD, 0, 0, 0);

	public final int index;
	public final DLXOpcode opcode;
	public final int a;
	public final int b;
	public final int c;

	private final String displayText;
	private String comment;

	DebugAssemblyInstruction(int index, DLXOpcode opcode, int a, int b, int c) {
		this.index = index;
		this.opcode = opcode;
		this.a = a;
		this.b = b;
		this.c = c;

		switch (DLXInstruction.getFormat(opcode)) {
			case ABSOLUTE:
				displayText = index + " | " + opcode + " " + String.valueOf(c);
				break;
			case IMMEDIATE:
				displayText = index + " | " + opcode + " " + registerName(a) + " " + registerName(b) + " " + c;
				break;
			case TERNARY:
				displayText = index + " | " + opcode + " " + registerName(a) + " " + registerName(b) + " " + registerName(c);
				break;
			default:
				throw DLXUtils.unknownEnumException(opcode);
		}
	}
	
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String getComment() {
		return comment;
	}

	public boolean usesRegister(int register) {
		switch (DLXInstruction.getFormat(opcode)) {
			case ABSOLUTE:
				return false;
			case IMMEDIATE:
				return ((register == a) || (register == b));
			case TERNARY:
				return ((register == a) || (register == b) || (register == c));
			default:
				throw DLXUtils.unknownEnumException(opcode);
		}
	}

	public String toString() {
		return displayText;
	}
}
