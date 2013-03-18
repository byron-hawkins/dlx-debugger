package org.hawkinssoftware.dlx.debug.ui;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class StackPanel {

	private class StackModel implements TableModel {
		private int values[] = new int[0];

		private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();

		void refresh() {
			values = controller.getStackValues();

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
			switch (columnIndex) {
				case 0:
					return Integer.class;
				case 1:
					return Integer.class;
				default:
					assert false : "No such column " + columnIndex + " in the heap table";
					return null;
			}
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			switch (columnIndex) {
				case 0:
					return "Position";
				case 1:
					return "Value";
				default:
					assert false : "No such column " + columnIndex + " in the heap table";
					return null;
			}
		}

		@Override
		public int getRowCount() {
			return values.length;
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return values.length - (rowIndex + 1);
				case 1:
					return values[values.length - (rowIndex + 1)];
				default:
					assert false : "No such column " + columnIndex + " in the heap table";
					return null;
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

	public interface Controller {
		int[] getStackValues();
	}

	private StackModel model;
	private JTable stackTable;
	private JScrollPane stackPane;

	private final Controller controller;

	StackPanel(Controller controller) {
		this.controller = controller;
	}

	public void assemble() {
		model = new StackModel();
		stackTable = new JTable(model);
		stackPane = new JScrollPane(stackTable);
		stackPane.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
	}

	public JComponent getComponent() {
		return stackPane;
	}

	public void refresh(int pc) {
		if (pc > 1) // wait for stack pointer to be set
			model.refresh();
	}

}
