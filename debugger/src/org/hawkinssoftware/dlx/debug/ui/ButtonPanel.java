package org.hawkinssoftware.dlx.debug.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class ButtonPanel implements ActionListener {

	public enum Command {
		STEP_INTO("In", "Step into current instruction"),
		STEP_OVER("Over", "Step over current instruction"),
		STEP_OUT("Out", "Step out of current stack frame"),
		RUN("Run", "Run program"),
		INTERRUPT("Int", "Interrupt running program"),
		TERMINATE("X", "Terminate");

		public final String name;
		public final String hint;

		private Command(String name, String hint) {
			this.name = name;
			this.hint = hint;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private class NavigationButton extends JButton implements ActionListener {
		private final Command command;

		NavigationButton(Command command) {
			super(command.name);
			this.command = command;
			setToolTipText(command.hint);
			setMargin(new Insets(2, 2, 2, 2));

			addActionListener(this);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			controller.navigationRequest(command);
		}
	}

	public interface Controller {
		void navigationRequest(Command command);

		void setAssemblyMode(boolean on);

		void clearBreakpoints();

		void toggleBreakpointsActive(boolean active);
	}

	private JPanel panel;
	private JPanel buttonRow;

	private final Controller controller;
	private final Map<Command, NavigationButton> navigationButtons = new EnumMap(Command.class);
	private JCheckBox assemblyMode;
	private JButton clearBreakpoints;
	private JCheckBox breakpointsActive;

	ButtonPanel(Controller controller) {
		this.controller = controller;
	}

	public void assemble() {
		buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
		for (Command command : Command.values()) {
			NavigationButton button = new NavigationButton(command);
			navigationButtons.put(command, button);
			buttonRow.add(button);
		}

		assemblyMode = new JCheckBox("Step Assembly");
		assemblyMode.setFont(assemblyMode.getFont().deriveFont(Font.PLAIN));
		buttonRow.add(assemblyMode);
		assemblyMode.addActionListener(this);

		buttonRow.add(new JSeparator(JSeparator.VERTICAL));

		JLabel breakpointLabel = new JLabel("Breakpoints:");
		breakpointLabel.setToolTipText("Click an assembly instruction to toggle a breakpoint there.");
		breakpointLabel.addMouseListener(new MouseAdapter() {
		});
		buttonRow.add(breakpointLabel);
		
		clearBreakpoints = new JButton("Clear");
		clearBreakpoints.setToolTipText("Clear all breakpoints");
		clearBreakpoints.addActionListener(this);
		buttonRow.add(clearBreakpoints);

		breakpointsActive = new JCheckBox("Enabled");
		breakpointsActive.setToolTipText("Breakpoints enabled");
		breakpointsActive.addActionListener(this);
		breakpointsActive.setSelected(true);
		buttonRow.add(breakpointsActive);

		panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		panel.add(buttonRow);
	}

	public JComponent getComponent() {
		return panel;
	}

	public void setButtonsEnabled(boolean b) {
		for (NavigationButton button : navigationButtons.values()) {
			button.setEnabled(b);
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == assemblyMode) {
			controller.setAssemblyMode(assemblyMode.isSelected());
		} else if (e.getSource() == clearBreakpoints) {
			controller.clearBreakpoints();
		} else if (e.getSource() == breakpointsActive) {
			controller.toggleBreakpointsActive(breakpointsActive.isSelected());
		}
	}
}
