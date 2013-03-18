package org.hawkinssoftware.dlx.debug.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.hawkinssoftware.dlx.debug.DebugAssemblyInstruction;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class RegisterPanel {

	private class RegisterModel implements TableModel {

		private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
		private int values[] = new int[32];
		private DebugAssemblyInstruction currentInstruction = DebugAssemblyInstruction.EMPTY;

		void refresh(DebugAssemblyInstruction currentInstruction) {
			values = controller.getRegisterValues();
			this.currentInstruction = currentInstruction;

			TableModelEvent event = new TableModelEvent(this);
			for (TableModelListener listener : listeners) {
				listener.tableChanged(event);
			}
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}

		@Override
		public int getColumnCount() {
			return 8;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return "";
		}

		@Override
		public int getRowCount() {
			return 8;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			boolean isLabel = columnIndex % 2 == 0;
			columnIndex /= 2;
			if (isLabel) {
				int index = (rowIndex * 4) + columnIndex;
				return DebugAssemblyInstruction.registerName(index);
			} else {
				int index = (rowIndex * 4) + columnIndex;
				return " " + values[index];
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex) {
			return false;
		}

		@Override
		public void removeTableModelListener(TableModelListener l) {
			listeners.remove(l);
		}

		@Override
		public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
			// not editable
		}
	}

	private class CellPainter extends JLabel implements TableCellRenderer {
		private final Font plainFont;
		private final Font boldFont;

		public CellPainter() {
			setOpaque(true);

			boldFont = getFont();
			plainFont = boldFont.deriveFont(Font.PLAIN);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			setText(value.toString());

			int registerIndex = (row * 4) + (column / 2);
			if (column % 2 == 0) {
				setBackground(Color.lightGray);
				setFont(boldFont);
			} else {
				if (model.currentInstruction.usesRegister(registerIndex))
					setBackground(Color.yellow);
				else
					setBackground(Color.white);
				setFont(plainFont);
			}


			return this;
		}
	}

	public interface Controller {
		int[] getRegisterValues();
	}

	private final Controller controller;

	private RegisterModel model;

	private JTable registerTable;
	private JScrollPane registerPane;

	private final List<DebugAssemblyInstruction> assemblyLines;

	public RegisterPanel(Controller controller, List<DebugAssemblyInstruction> assemblyLines) {
		this.controller = controller;
		this.assemblyLines = assemblyLines;
	}

	public void assemble() {
		model = new RegisterModel();
		registerTable = new JTable(model);
		registerTable.setDefaultRenderer(String.class, new CellPainter());
		registerPane = new JScrollPane(registerTable);
	}

	public void refresh(int pc) {
		model.refresh(assemblyLines.get(pc));
	}

	public JComponent getComponent() {
		return registerPane;
	}
}
