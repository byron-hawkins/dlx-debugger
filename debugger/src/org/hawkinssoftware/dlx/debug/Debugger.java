package org.hawkinssoftware.dlx.debug;

import java.io.File;

import org.hawkinssoftware.dlx.debug.controller.DebugController;
import org.hawkinssoftware.dlx.simulator.DLXSimulator;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class Debugger {
	public static void main(String[] args) {
		try {
			File sourceFile = null;
			File debugFile = null;

			int a = 0;
			while ((args.length > a) && (args[a].startsWith("-"))) {
				switch (args[a].charAt(1)) {
					case 'e':
						DLXSimulator.echoInstructions(true);
						a++;
						break;
					case 's':
						sourceFile = new File(args[a + 1]);
						a += 2;
						break;
					case 'd':
						debugFile = new File(args[a + 1]);
						a += 2;
						break;
					default:
						System.err.println("Skipping unknown option " + args[a]);
				}
			}
			if ((args.length - a) != 1) {
				System.err.println("Usage: Debugger [-e] [-s <source-file>] [-d <debug-file>] <program-file>");
				System.exit(1);
			}

			File binaryFile = new File(args[a]);
			if (!binaryFile.exists()) {
				System.err.println("Debug target '" + binaryFile.getAbsolutePath() + "' cannot be found. Exiting!");
				System.exit(1);
			}
			if (sourceFile == null)
				sourceFile = new File(binaryFile.getParentFile(), binaryFile.getName() + ".dlx");
			if (!sourceFile.exists()) {
				System.err.println("Debug source '" + sourceFile.getAbsolutePath() + "' cannot be found. Exiting!");
				System.exit(1);
			}
			if (debugFile == null)
				debugFile = new File(binaryFile.getParentFile(), binaryFile.getName() + ".dbg");
			if (!debugFile.exists()) {
				System.err.println("Debug target '" + debugFile.getAbsolutePath() + "' cannot be found. Exiting!");
				System.exit(1);
			}

			DebugController controller = new DebugController(sourceFile, binaryFile, debugFile);
			controller.start();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
