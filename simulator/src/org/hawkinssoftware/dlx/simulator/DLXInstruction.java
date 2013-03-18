package org.hawkinssoftware.dlx.simulator;

import org.hawkinssoftware.dlx.DLXUtils;

public abstract class DLXInstruction {
	public enum Format {
		TERNARY,
		IMMEDIATE,
		ABSOLUTE;
	}

	public static DLXInstruction.Format getFormat(DLXOpcode opcode) {
		switch (opcode) {
			case ADD:
			case SUB:
			case MUL:
			case DIV:
			case MOD:
			case CMP:
			case OR:
			case AND:
			case BIC:
			case XOR:
			case LSH:
			case ASH:
			case CHK:
			case LDX:
			case STX:
			case RET:
			case RDD:
			case WRD:
			case WRH:
				return DLXInstruction.Format.TERNARY;
			case BEQ:
			case BGE:
			case BGT:
			case BLE:
			case BLT:
			case BNE:
			case BSR:
			case ADDI:
			case SUBI:
			case MULI:
			case DIVI:
			case MODI:
			case CMPI:
			case ORI:
			case ANDI:
			case BICI:
			case XORI:
			case LSHI:
			case ASHI:
			case CHKI:
			case LDW:
			case POP:
			case STW:
			case PSH:
			case WRL:
				return DLXInstruction.Format.IMMEDIATE;
			case JSR:
				return DLXInstruction.Format.ABSOLUTE;
		}
		throw DLXUtils.unknownEnumException(opcode);
	}

	protected final DLXOpcode opcode;
	protected final Format format;
	protected final String description;

	public DLXInstruction(DLXOpcode opcode, Format format, String description) {
		this.opcode = opcode;
		this.format = format;
		this.description = description;
	}

	public abstract int encode(int pc);

	@Override
	public String toString() {
		// TODO: early pc assignment?
		return opcode + " (0x" + Integer.toHexString(encode(0)) + "): " + description;
	}
}
