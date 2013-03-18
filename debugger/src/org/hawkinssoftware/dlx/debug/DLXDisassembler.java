package org.hawkinssoftware.dlx.debug;

import org.hawkinssoftware.dlx.simulator.DLXOpcode;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DLXDisassembler {
	public DebugAssemblyInstruction disassemble(int index, int instructionWord) {
		int opcodeNumber = instructionWord >>> 26; // without sign extension
		DLXOpcode opcode = DLXOpcode.lookup(opcodeNumber);
		int a, b, c;
		switch (opcode) {
			case BSR:
			case WRD:
			case WRH:
			case WRL:
			case CHKI:
			case BEQ:
			case BNE:
			case BLT:
			case BGE:
			case BLE:
			case BGT:
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
			case LDW:
			case POP:
			case STW:
			case PSH:
				a = (instructionWord >>> 21) & 0x1F;
				b = (instructionWord >>> 16) & 0x1F;
				c = (short) instructionWord; // another dirty trick
				break;
			case RET:
			case CHK:
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
			case LDX:
			case STX:
				a = (instructionWord >>> 21) & 0x1F;
				b = (instructionWord >>> 16) & 0x1F;
				c = instructionWord & 0x1F;
				break;
			case JSR:
				a = -1; // invalid, for error detection
				b = -1;
				c = instructionWord & 0x3FFFFFF;
				break;

			// unknown instruction code
			default:
				assert false : "Illegal instruction " + instructionWord + "!";
				return null;
		}

		return new DebugAssemblyInstruction(index, opcode, a, b, c);
	}
}
