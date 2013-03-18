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

import org.hawkinssoftware.dlx.debug.DebugData;
import org.hawkinssoftware.dlx.debug.DebugHeapVariable;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class HeapPanel {
	private class HeapEntry {
		final String displayName;
		final int heapLocation;

		HeapEntry(String displayName, int heapLocation) {
			this.displayName = displayName;
			this.heapLocation = heapLocation;
		}
	}

	private class HeapModel implements TableModel {
		private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
		private final List<HeapEntry> entries = new ArrayList<HeapEntry>();

		void refresh() {
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
					return String.class;
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
					return "Variable";
				case 1:
					return "Value";
				default:
					assert false : "No such column " + columnIndex + " in the heap table";
					return null;
			}
		}

		@Override
		public int getRowCount() {
			return entries.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			switch (columnIndex) {
				case 0:
					return entries.get(rowIndex).displayName;
				case 1:
					return controller.getHeapValue(entries.get(rowIndex).heapLocation);
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
		int getHeapValue(int location);
	}

	private class ArrayHeapEntryGenerator {
		private int location;
		StringBuilder buffer = new StringBuilder();

		void reset(DebugHeapVariable variable) {
			location = variable.location;
			buffer.setLength(0);
			buffer.append(variable.name);
		}

		void generateArrayLocations(DebugHeapVariable variable, int degreePosition) {
			if (degreePosition == variable.degrees.size()) {
				model.entries.add(new HeapEntry(buffer.toString(), location));
				location += 4;
			} else {
				for (int i = 0; i < variable.degrees.get(degreePosition); i++) {
					int bufferLength = buffer.length();
					buffer.append("[");
					buffer.append(i);
					buffer.append("]");
					generateArrayLocations(variable, degreePosition + 1);
					buffer.setLength(bufferLength);
				}
			}
		}
	}

	private final Controller controller;

	private HeapModel model;

	private JTable dataTable;
	private JScrollPane dataPane;

	private DebugData data;

	private final ArrayHeapEntryGenerator arrayGenerator = new ArrayHeapEntryGenerator();

	public HeapPanel(Controller controller) {
		this.controller = controller;
	}

	public void assemble() {
		model = new HeapModel();
		dataTable = new JTable(model);
		dataPane = new JScrollPane(dataTable);
		dataPane.setMaximumSize(new Dimension(100, Integer.MAX_VALUE));
	}

	public void setDebugData(DebugData data) {
		this.data = data;

		for (DebugHeapVariable variable : data.heap) {
			if (variable.isArray()) {
				arrayGenerator.reset(variable);
				arrayGenerator.generateArrayLocations(variable, 0);
			} else {
				model.entries.add(new HeapEntry(variable.name, variable.location));
			}
		}
	}

	public void refresh(int pc) {
		if (pc > 0)
			model.refresh();
	}

	public JComponent getComponent() {
		return dataPane;
	}
}
