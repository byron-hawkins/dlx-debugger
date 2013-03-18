package org.hawkinssoftware.dlx.debug.ui;

import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.hawkinssoftware.dlx.debug.DebugAssemblyInstruction;
import org.hawkinssoftware.dlx.debug.DebugData;
import org.hawkinssoftware.dlx.debug.ExecutionContext;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugWindow {
	private JFrame window;
	private JPanel mainPanel;
	private JSplitPane centerPane;
	private SourcePanel sourcePanel;
	private ButtonPanel buttonPanel;
	private HeapPanel heapPanel;
	private StackPanel stackPanel;
	private RegisterPanel registerPane;
	private JSplitPane memoryPane;
	private JSplitPane dataPane;

	private DebugData data;

	public void constructWindow(ButtonPanel.Controller buttonController, HeapPanel.Controller heapController, StackPanel.Controller stackController,
			RegisterPanel.Controller registerController, SourcePanel.Controller sourceController, List<String> sourceLines,
			List<DebugAssemblyInstruction> assemblyLines) {
		window = new JFrame("DLX Debugger");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		mainPanel = new JPanel(new BorderLayout());
		centerPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		centerPane.setDividerSize(4);
		mainPanel.add(centerPane, BorderLayout.CENTER);
		sourcePanel = new SourcePanel(sourceController, sourceLines, assemblyLines);
		sourcePanel.assemble();
		centerPane.setLeftComponent(sourcePanel.getComponent());
		buttonPanel = new ButtonPanel(buttonController);
		buttonPanel.assemble();
		mainPanel.add(buttonPanel.getComponent(), BorderLayout.SOUTH);

		heapPanel = new HeapPanel(heapController);
		heapPanel.assemble();
		stackPanel = new StackPanel(stackController);
		stackPanel.assemble();
		registerPane = new RegisterPanel(registerController, assemblyLines);
		registerPane.assemble();
		memoryPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		memoryPane.setLeftComponent(heapPanel.getComponent());
		memoryPane.setRightComponent(stackPanel.getComponent());
		memoryPane.setContinuousLayout(true);
		memoryPane.setDividerSize(4);
		dataPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		dataPane.setTopComponent(memoryPane);
		dataPane.setBottomComponent(registerPane.getComponent());
		dataPane.setContinuousLayout(true);
		dataPane.setDividerSize(4);
		centerPane.setRightComponent(dataPane);
		centerPane.setContinuousLayout(true);

		window.setContentPane(mainPanel);
	}

	public void setDebugData(DebugData data) {
		this.data = data;
		heapPanel.setDebugData(data);
	}

	public void displayWindow() {
		Point screenCenter = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
		window.pack();
		window.setSize(1100, 600);
		centerPane.setDividerLocation(700);
		memoryPane.setDividerLocation(290);
		dataPane.setDividerLocation(382);
		dataPane.setSize(100, dataPane.getSize().height);
		window.setVisible(true);
		window.setLocation(screenCenter.x - (window.getSize().width / 2), screenCenter.y - (window.getSize().height / 2));
	}

	public void displayContext(int pc, ExecutionContext context) {
		sourcePanel.displayContext(pc, context);
		heapPanel.refresh(pc);
		stackPanel.refresh(pc);
		registerPane.refresh(pc);
	}

	public void displaySourceSelection(ExecutionContext selection) {
		sourcePanel.displaySelection(selection);
	}

	public void updateBreakpoints(List<Integer> breakpoints, boolean active) {
		sourcePanel.updateBreakpoints(breakpoints, active);
	}

	public void terminateSession() {
		buttonPanel.setButtonsEnabled(false);
		sourcePanel.clearContext();
	}
}
