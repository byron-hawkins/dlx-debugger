package org.hawkinssoftware.dlx.debug.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.ParagraphView;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

import org.hawkinssoftware.dlx.debug.DebugAssemblyInstruction;
import org.hawkinssoftware.dlx.debug.ExecutionContext;
import org.hawkinssoftware.dlx.simulator.DLXInstruction;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class SourcePanel {

	private class SourceFile {
		final List<?> lines;
		final int linePositions[];
		final String text;

		SourceFile(List<?> lines) {
			this.lines = lines;

			linePositions = new int[lines.size() + 2];

			int index = 0;
			linePositions[index++] = 0;
			StringBuilder builder = new StringBuilder();
			for (Object line : lines) {
				builder.append(line.toString());
				builder.append("\n");
				linePositions[index++] = builder.length();
			}
			linePositions[index] = builder.length();
			text = builder.toString();
		}

		int getSourceLine(int position) {
			for (int i = linePositions.length - 1; i >= 0; i--) {
				if (position >= linePositions[i])
					return i;
			}
			throw new IllegalStateException("Position " + position + " does not exist in this document!");
		}
	}

	private class CodePane extends JEditorPane {
		final StyledDocument document;

		private final MutableAttributeSet defaultAttributes = new SimpleAttributeSet();
		private final MutableAttributeSet currentContext = new SimpleAttributeSet();
		private final MutableAttributeSet selectionHighlight = new SimpleAttributeSet();
		private final MutableAttributeSet paragraphAttributes = new SimpleAttributeSet();

		private int contextStart = -1;
		private int contextLength = -1;
		private int selectionStart = -1;
		private int selectionLength = -1;

		public CodePane(String code) {
			this.setEditorKit(new NoWrapEditorKit());
			document = (StyledDocument) this.getDocument();

			StyleConstants.setBackground(defaultAttributes, Color.WHITE);
			StyleConstants.setForeground(defaultAttributes, Color.BLACK);
			StyleConstants.setFontSize(defaultAttributes, 11);
			StyleConstants.setFontFamily(defaultAttributes, "Consolas");

			StyleConstants.setBackground(currentContext, CURRENT_CODE);

			StyleConstants.setBackground(selectionHighlight, SELECTION_HIGHLIGHT);

			StyleConstants.setTabSet(paragraphAttributes, new TabSet(new TabStop[] { new TabStop(10f), new TabStop(20f), new TabStop(30f) }));
			document.setParagraphAttributes(0, document.getLength(), paragraphAttributes, false);

			try {
				document.insertString(0, code, defaultAttributes);
			} catch (BadLocationException impossible) {
			}

			addMouseListener(new EditorMouseResponder());
		}

		void clearHighlight(Highlight highlight) {
			switch (highlight) {
				case EXECUTION:
					document.setCharacterAttributes(contextStart, contextLength, defaultAttributes, false);
					break;
				case SELECTION:
					document.setCharacterAttributes(selectionStart, selectionLength, defaultAttributes, false);
					break;
			}
		}

		void refreshHighlight(Highlight highlight) {
			clearHighlight(highlight);
			ExecutionContext refreshContext = (highlight == Highlight.EXECUTION) ? context : selection;
			int sourceStartPosition = sourceFile.linePositions[refreshContext.sourceStartIndex - 1];
			int sourceEndIndex;
			if (refreshContext.sourceEndIndex < refreshContext.sourceStartIndex)
				sourceEndIndex = sourceFile.linePositions.length - 1;
			else
				sourceEndIndex = refreshContext.sourceEndIndex - 1;
			int sourceEndPosition = sourceFile.linePositions[sourceEndIndex];

			switch (highlight) {
				case EXECUTION:
					contextStart = sourceStartPosition;
					contextLength = sourceEndPosition - sourceStartPosition;
					document.setCharacterAttributes(contextStart, contextLength, currentContext, false);
					break;
				case SELECTION:
					if ((sourceStartPosition <= contextStart) && (sourceEndPosition > contextStart))
						return;
					selectionStart = sourceStartPosition;
					selectionLength = sourceEndPosition - sourceStartPosition;
					document.setCharacterAttributes(selectionStart, selectionLength, selectionHighlight, false);
					break;
			}

			try {
				scrollRectToVisible(modelToView(sourceEndPosition));
				scrollRectToVisible(modelToView(sourceStartPosition));
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		}
	}

	private class EditorMouseResponder extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			int position = sourceDisplay.viewToModel(e.getPoint());
			int sourceLine = sourceFile.getSourceLine(position);
			if (sourceLine >= 0)
				controller.selectionRequested(sourceLine + 1);
		}
	}

	class NoWrapEditorKit extends StyledEditorKit {
		public ViewFactory getViewFactory() {
			return new NoWrapViewFactory();
		}
	}

	class NoWrapViewFactory implements ViewFactory {
		public View create(Element elem) {
			String kind = elem.getName();
			if (kind != null)
				if (kind.equals(AbstractDocument.ContentElementName)) {
					return new LabelView(elem);
				} else if (kind.equals(AbstractDocument.ParagraphElementName)) {
					return new CodeParagraphView(elem);
				} else if (kind.equals(AbstractDocument.SectionElementName)) {
					return new BoxView(elem, View.Y_AXIS);
				} else if (kind.equals(StyleConstants.ComponentElementName)) {
					return new ComponentView(elem);
				} else if (kind.equals(StyleConstants.IconElementName)) {
					return new IconView(elem);
				}
			return new LabelView(elem);
		}
	}

	private class CodeParagraphView extends ParagraphView {
		public CodeParagraphView(Element elem) {
			super(elem);
		}

		public void layout(int width, int height) {
			super.layout(Short.MAX_VALUE, height);
		}

		public float getMinimumSpan(int axis) {
			return super.getPreferredSpan(axis);
		}
	}

	private class AssemblyModel implements TableModel {

		private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
		final List<DebugAssemblyInstruction> assembly;

		private int executionStart;
		private int executionEnd;
		private int executionFocus;

		private int selectionStart = -1;
		private int selectionEnd = -1;

		private List<Integer> breakpoints = Collections.emptyList();
		private boolean breakpointsActive = true;

		AssemblyModel(List<DebugAssemblyInstruction> assemblyLines) {
			this.assembly = assemblyLines;
		}

		void refresh() {
			TableModelEvent event = new TableModelEvent(this);
			for (TableModelListener listener : listeners) {
				listener.tableChanged(event);
			}
			assemblyDisplay.repaint();
		}

		void refreshHighlight(Highlight highlight) {
			clearHighlight(highlight);

			switch (highlight) {
				case EXECUTION:
					executionStart = context.assemblyStartIndex;
					executionEnd = context.assemblyEndIndex < 0 ? assemblyModel.assembly.size() - 1 : context.assemblyEndIndex;
					executionFocus = pc;
					scrollToRegion(executionStart, executionEnd, executionFocus);
					break;
				case SELECTION:
					selectionStart = selection.assemblyStartIndex;
					selectionEnd = selection.assemblyEndIndex < 0 ? assemblyModel.assembly.size() - 1 : selection.assemblyEndIndex;
					scrollToRegion(selectionStart, selectionEnd, -1);
					break;
			}
		}

		void clearHighlight(Highlight highlight) {
			switch (highlight) {
				case EXECUTION:
					executionStart = executionEnd = executionFocus = -1;
					break;
				case SELECTION:
					selectionStart = selectionEnd = -1;
					break;
			}
			refresh();
		}

		void scrollToRegion(int start, int end, int focus) {
			assemblyDisplay.scrollRectToVisible(assemblyDisplay.getCellRect(start, 0, true));
			assemblyDisplay.scrollRectToVisible(assemblyDisplay.getCellRect(end, 0, true));
			if (focus >= 0)
				assemblyDisplay.scrollRectToVisible(assemblyDisplay.getCellRect(focus, 0, true));
			refresh();
		}

		@Override
		public void addTableModelListener(TableModelListener l) {
			listeners.add(l);
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			switch (columnIndex) {
				case 0:
				case 1:
					return Integer.class;
				case 2:
				case 3:
				case 4:
				case 5:
					return String.class;
				default:
					throw new IllegalArgumentException("Unknown column " + columnIndex);
			}
		}

		@Override
		public int getColumnCount() {
			return 6;
		}

		@Override
		public String getColumnName(int columnIndex) {
			return "";
		}

		@Override
		public int getRowCount() {
			return assembly.size();
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			DebugAssemblyInstruction instruction = assembly.get(rowIndex);
			switch (columnIndex) {
				case 0:
					return instruction.index;
				case 1:
					return instruction.opcode;
				case 2:
					switch (DLXInstruction.getFormat(instruction.opcode)) {
						case ABSOLUTE:
							return String.valueOf(instruction.c);
						case IMMEDIATE:
						case TERNARY:
							return DebugAssemblyInstruction.registerName(assembly.get(rowIndex).a);
					}
				case 3:
					switch (DLXInstruction.getFormat(instruction.opcode)) {
						case ABSOLUTE:
							return "";
						case IMMEDIATE:
						case TERNARY:
							return DebugAssemblyInstruction.registerName(assembly.get(rowIndex).b);
					}
				case 4:
					switch (DLXInstruction.getFormat(instruction.opcode)) {
						case ABSOLUTE:
							return "";
						case IMMEDIATE:
							return String.valueOf(instruction.c);
						case TERNARY:
							return DebugAssemblyInstruction.registerName(assembly.get(rowIndex).c);
					}
				case 5:
					return instruction.getComment();
				default:
					throw new IllegalArgumentException("Unknown column " + columnIndex);
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

	private class AssemblyCellRenderer extends JLabel implements TableCellRenderer {
		private final Font plainFont;
		private final Font boldFont;
		private final Color executionBacklight = new Color(0xfafad2);
		private final Color executionColor = CURRENT_CODE;
		private final Color executionText = new Color(0x00cc00);
		private final Color selectionBacklight = SELECTION_HIGHLIGHT;
		private final Color activeBreakpointColor = new Color(0xaa0000);
		private final Color inactiveBreakpointColor = new Color(0xffe4e1);

		public AssemblyCellRenderer() {
			setOpaque(true);

			boldFont = getFont().deriveFont(10f);
			plainFont = boldFont.deriveFont(Font.PLAIN);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			setText((column > 1 ? " " : "") + value.toString());
			if (row == assemblyModel.executionFocus) {
				setForeground(executionText);
				setBackground(executionColor);
			} else if ((row >= assemblyModel.executionStart) && (row < assemblyModel.executionEnd)) {
				setForeground(Color.black);
				setBackground(executionBacklight);
			} else if ((row >= assemblyModel.selectionStart) && (row < assemblyModel.selectionEnd)) {
				setForeground(Color.black);
				setBackground(selectionBacklight);
			} else {
				setForeground(Color.black);
				setBackground(Color.white);
			}

			if ((column == 0) && assemblyModel.breakpoints.contains(row)) {
				if (assemblyModel.breakpointsActive) {
					setBackground(activeBreakpointColor);
					setForeground(Color.white);
				} else {
					setBackground(inactiveBreakpointColor);
					setForeground(Color.black);
				}
			}

			if (column == 0)
				setFont(boldFont);
			else
				setFont(plainFont);

			return this;
		}
	}

	private class AssemblyMouseResponder extends MouseAdapter {
		@Override
		public void mousePressed(MouseEvent e) {
			int row = assemblyDisplay.rowAtPoint(e.getPoint());
			if (row >= 0)
				controller.toggleBreakpoint(row);
		}
	}

	public interface Controller {
		void toggleBreakpoint(Integer pc);

		void selectionRequested(int sourceLine);
	}

	private enum Highlight {
		EXECUTION,
		SELECTION;
	}

	private static final Color CURRENT_CODE = Color.yellow;
	private static final Color SELECTION_HIGHLIGHT = new Color(0xb0e0e6);

	private final Controller controller;

	private CodePane sourceDisplay;
	private JScrollPane sourcePane;

	private JTable assemblyDisplay;
	private JScrollPane assemblyPane;

	private JSplitPane splitPane;

	private final SourceFile sourceFile;
	private final AssemblyModel assemblyModel;

	private int pc;
	private ExecutionContext context;
	private ExecutionContext selection = null;

	SourcePanel(Controller controller, List<String> sourceLines, List<DebugAssemblyInstruction> assembly) {
		this.controller = controller;
		this.sourceFile = new SourceFile(sourceLines);
		this.assemblyModel = new AssemblyModel(assembly);
	}

	void assemble() {
		sourceDisplay = new CodePane(sourceFile.text);
		sourceDisplay.setEditable(false);
		sourcePane = new JScrollPane(sourceDisplay);

		assemblyDisplay = new JTable(assemblyModel);
		assemblyDisplay.setDefaultRenderer(Integer.class, new AssemblyCellRenderer());
		assemblyDisplay.setDefaultRenderer(String.class, new AssemblyCellRenderer());
		for (int i = 0; i < 5; i++) {
			assemblyDisplay.getColumnModel().getColumn(i).setMaxWidth(30);
		}
		assemblyDisplay.getColumnModel().getColumn(5).setPreferredWidth(200);
		assemblyDisplay.addMouseListener(new AssemblyMouseResponder());
		assemblyPane = new JScrollPane(assemblyDisplay);

		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setLeftComponent(sourcePane);
		splitPane.setRightComponent(assemblyPane);
		splitPane.setDividerLocation(325);
		splitPane.setContinuousLayout(true);
		splitPane.setDividerSize(4);
	}

	JComponent getComponent() {
		return splitPane;
	}

	void displayContext(int pc, ExecutionContext context) {
		this.pc = pc;
		this.context = context;

		sourceDisplay.refreshHighlight(Highlight.EXECUTION);
		assemblyModel.refreshHighlight(Highlight.EXECUTION);
	}

	void clearContext() {
		sourceDisplay.clearHighlight(Highlight.EXECUTION);
		assemblyModel.clearHighlight(Highlight.EXECUTION);
	}

	void displaySelection(ExecutionContext selection) {
		this.selection = selection;
		sourceDisplay.refreshHighlight(Highlight.SELECTION);
		assemblyModel.refreshHighlight(Highlight.SELECTION);
	}

	void updateBreakpoints(List<Integer> breakpoints, boolean active) {
		assemblyModel.breakpoints = breakpoints;
		assemblyModel.breakpointsActive = active;
		assemblyDisplay.repaint();
	}
}
