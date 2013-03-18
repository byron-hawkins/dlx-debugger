package org.hawkinssoftware.dlx.debug.controller;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.hawkinssoftware.dlx.DLXUtils;
import org.hawkinssoftware.dlx.debug.DLXDisassembler;
import org.hawkinssoftware.dlx.debug.DebugAssemblyInstruction;
import org.hawkinssoftware.dlx.debug.DebugData;
import org.hawkinssoftware.dlx.debug.DebugStackFrame;
import org.hawkinssoftware.dlx.debug.ExecutionContext;
import org.hawkinssoftware.dlx.debug.ui.ButtonPanel;
import org.hawkinssoftware.dlx.debug.ui.ButtonPanel.Command;
import org.hawkinssoftware.dlx.debug.ui.DebugWindow;
import org.hawkinssoftware.dlx.debug.ui.HeapPanel;
import org.hawkinssoftware.dlx.debug.ui.RegisterPanel;
import org.hawkinssoftware.dlx.debug.ui.SourcePanel;
import org.hawkinssoftware.dlx.debug.ui.StackPanel;
import org.hawkinssoftware.dlx.simulator.DLXSimulator;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugController implements ButtonPanel.Controller, HeapPanel.Controller, StackPanel.Controller, RegisterPanel.Controller, SourcePanel.Controller {
	private final DebugWindow window;

	private final DLXDisassembler disassembler = new DLXDisassembler();

	private DebugData debugData;
	private List<DebugAssemblyInstruction> assembly;

	final File sourceFile;
	final File binaryFile;
	final File debugFile;

	private final DebugStackFrame callStack;
	private DebugStackFrame currentStackFrame;
	private Breakpoint breakpoint;
	private ExecutionContext currentContext;
	private boolean assemblyMode = false;

	private final Object executionLock = new Object();
	private final Object terminationLock = new Object();
	private final StepIntoBreakpoint stepInto = new StepIntoBreakpoint();
	private final StepOverBreakpoint stepOver = new StepOverBreakpoint();
	private final StepOutBreakpoint stepOut = new StepOutBreakpoint();
	private final NoBreakpoint run = new NoBreakpoint();
	private final TerminateBreakpoint stop = new TerminateBreakpoint();
	private Breakpoint activeBreakpoint;
	private final List<Integer> userBreakpoints = new ArrayList<Integer>();
	private boolean userBreakpointsActive = true;

	private final ExecutionThread appThread = new ExecutionThread();

	public DebugController(File sourceFile, File binaryFile, File debugFile) {
		this.sourceFile = sourceFile;
		this.binaryFile = binaryFile;
		this.debugFile = debugFile;

		window = new DebugWindow();

		callStack = new DebugStackFrame();
		currentStackFrame = callStack;
	}

	public void start() throws IOException {
		disassembleBinary();
		loadDebugData();
		attachAssemblyComments();
		window.constructWindow(this, this, this, this, this, loadSourceText(), assembly);
		window.setDebugData(debugData);
		window.displayWindow();
		loadSimulator();

		appThread.start();
		synchronized (terminationLock) {
			try {
				terminationLock.wait();
			} catch (InterruptedException terminated) {
			}
		}
		window.terminateSession();
	}

	void updateWindow() {
		int pc = DLXSimulator.pc();
		window.displayContext(pc, currentContext);
	}

	@Override
	public void navigationRequest(Command command) {
		switch (command) {
			case STEP_INTO:
				stepInto.insert();
				break;
			case RUN:
				run.insert();
				break;
			case STEP_OUT:
				stepOut.insert();
				break;
			case STEP_OVER:
				stepOver.insert();
				break;
			case INTERRUPT:
				stepInto.insert();
				break;
			case TERMINATE:
				stop.insert();
				break;
			default:
				throw DLXUtils.unknownEnumException(command);
		}
	}

	@Override
	public void setAssemblyMode(boolean on) {
		assemblyMode = on;
	}

	@Override
	public int getHeapValue(int location) {
		return DLXSimulator.getHeapValue(location);
	}

	@Override
	public int[] getStackValues() {
		return DLXSimulator.getStackValues();
	}

	@Override
	public int[] getRegisterValues() {
		return DLXSimulator.getRegisterValues();
	}

	@Override
	public void toggleBreakpoint(Integer pc) {
		if (!userBreakpoints.remove(pc)) {
			userBreakpoints.add(pc);
		}
		updateBreakpointDisplay();
	}

	@Override
	public void selectionRequested(int sourceLine) {
		ExecutionContext context = debugData.sourceLineToExecutionContext(sourceLine);
		window.displaySourceSelection(context);
	}

	@Override
	public void clearBreakpoints() {
		userBreakpoints.clear();
		updateBreakpointDisplay();
	}

	@Override
	public void toggleBreakpointsActive(boolean active) {
		userBreakpointsActive = !userBreakpointsActive;
		updateBreakpointDisplay();
	}

	public void updateBreakpointDisplay() {
		window.updateBreakpoints(userBreakpoints, userBreakpointsActive);
	}

	private List<String> loadSourceText() throws IOException {
		List<String> sourceLines = new ArrayList<String>();
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(sourceFile)));
		String line;
		while ((line = input.readLine()) != null) {
			sourceLines.add(line);
		}
		input.close();
		return sourceLines;
	}

	private void disassembleBinary() throws IOException {
		assembly = new ArrayList<DebugAssemblyInstruction>(); 
		DataInputStream input = new DataInputStream(new FileInputStream(binaryFile));
		int index = 0;
		while (input.available() > 0) {
			int instruction = input.readInt();
			assembly.add(disassembler.disassemble(index++, instruction));
		}
		input.close();
	}

	private void loadDebugData() throws IOException {
		debugData = new DebugData(debugFile);
		debugData.loadData();
	}

	private void attachAssemblyComments() {
		for (DebugAssemblyInstruction instruction : assembly) {
			instruction.setComment(debugData.assemblyComments.get(instruction.index));
		}
	}

	private void loadSimulator() throws IOException {
		DLXSimulator.load(binaryFile.getAbsolutePath());
		DLXSimulator.initialize();
	}

	private class ExecutionThread extends Thread {
		boolean terminated = false;

		public ExecutionThread() {
			super("Debug-Execution");
		}

		@Override
		public void run() {
			try {
				DLXSimulator.step();
				currentContext = debugData.getExecutionContext(DLXSimulator.pc());
				stepInto.insert();
				DLXSimulator.step();
				currentContext = debugData.getExecutionContext(DLXSimulator.pc());
				sendWindowUpdate();

				DebugAssemblyInstruction aboutToExecute;
				while (true) {
					synchronized (executionLock) {
						if ((userBreakpointsActive && userBreakpoints.contains(DLXSimulator.pc())) || activeBreakpoint.breakNow()) {
							if (terminated)
								break;
							try {
								sendWindowUpdate();
								executionLock.wait();
							} catch (InterruptedException runtime) {
							}
						}
						aboutToExecute = assembly.get(DLXSimulator.pc());
						if (!DLXSimulator.step()) {
							break;
						}
						updateStack(aboutToExecute);
						currentContext = debugData.getExecutionContext(DLXSimulator.pc());
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			terminate();
		}

		private void updateStack(DebugAssemblyInstruction justExecuted) {
			switch (justExecuted.opcode) {
				case BSR:
					currentStackFrame = currentStackFrame.push();
					break;
				case RET:
					currentStackFrame = currentStackFrame.pop();
					break;
			}
		}

		private void terminate() {
			synchronized (terminationLock) {
				terminated = true;
				terminationLock.notify();
			}
		}

		private void sendWindowUpdate() {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					updateWindow();
				}
			});
		}
	}

	private abstract class Breakpoint {
		abstract boolean breakNow();

		void configure() {
		}

		final void insert() {
			synchronized (executionLock) {
				activeBreakpoint = this;
				configure();
				executionLock.notify();
			}
		}
	}

	private class StepIntoBreakpoint extends Breakpoint {
		private DebugStackFrame referenceFrame;
		private ExecutionContext referenceContext;

		@Override
		void configure() {
			referenceFrame = currentStackFrame;
			referenceContext = currentContext;
		}

		@Override
		public boolean breakNow() {
			if (assemblyMode)
				return true;
			else
				return (currentStackFrame != referenceFrame) || (currentContext != referenceContext);
		}
	}

	private class StepOverBreakpoint extends Breakpoint {
		private DebugStackFrame referenceFrame;
		private ExecutionContext referenceContext;

		void configure() {
			referenceFrame = currentStackFrame;
			referenceContext = currentContext;
		}

		@Override
		public boolean breakNow() {
			if (assemblyMode)
				return currentStackFrame == referenceFrame;
			else
				return (currentStackFrame == referenceFrame) && (currentContext != referenceContext);
		}
	}

	private class StepOutBreakpoint extends Breakpoint {
		private DebugStackFrame referenceFrame;

		void configure() {
			if (currentStackFrame.isBaseFrame()) {
				stepOver.insert();
			} else {
				referenceFrame = currentStackFrame.getPredecessor();
			}
		}

		@Override
		boolean breakNow() {
			return currentStackFrame == referenceFrame;
		}
	}

	private class NoBreakpoint extends Breakpoint {
		@Override
		boolean breakNow() {
			return false;
		}
	}

	private class TerminateBreakpoint extends Breakpoint {
		@Override
		boolean breakNow() {
			appThread.terminated = true;
			return true;
		}
	}
}
