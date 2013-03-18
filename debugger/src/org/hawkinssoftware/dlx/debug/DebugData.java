package org.hawkinssoftware.dlx.debug;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * @author Byron Hawkins (byron@hawkinssoftware.net)
 */
public class DebugData {
	public static final String HEAP_HEADER = ".Heap";
	private static final String LINE_NUMBER_HEADER = ".Lines";
	private static final String ASSEMBLY_COMMENT_HEADER = ".Comment";

	private TreeMap<Integer, ExecutionContext> contextsByAssemblyLine = new TreeMap<Integer, ExecutionContext>();
	private TreeMap<Integer, ExecutionContext> contextsBySourceLine = new TreeMap<Integer, ExecutionContext>();
	public final List<DebugHeapVariable> heap = new ArrayList<DebugHeapVariable>();
	public final List<String> assemblyComments = new ArrayList<String>();

	private final File debugFile;

	public DebugData(File debugFile) {
		this.debugFile = debugFile;
	}

	public void loadData() throws IOException {
		BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(debugFile)));

		String line = input.readLine();
		assert line.equals(LINE_NUMBER_HEADER) : "Line number header '.Lines' expected to start the debug data file '" + debugFile.getAbsolutePath() + "'";

		line = input.readLine();
		StringTokenizer tokens = new StringTokenizer(line, "(,) ");

		int tokenCount = tokens.countTokens();
		assert (tokenCount % 2) == 0 : "Debug data file has an odd number of assembly-source index entries!";

		if (!tokens.hasMoreTokens())
			return;
		int sourceStartIndex = Integer.parseInt(tokens.nextToken());
		int assemblyStartIndex = Integer.parseInt(tokens.nextToken());
		while (tokens.hasMoreTokens()) {
			int sourceEndIndex = Integer.parseInt(tokens.nextToken());
			int assemblyEndIndex = Integer.parseInt(tokens.nextToken());
			if (assemblyEndIndex < 0)
				continue; // this case is now handled in the file writer
			ExecutionContext context = new ExecutionContext(sourceStartIndex, sourceEndIndex, assemblyStartIndex, assemblyEndIndex);
			contextsBySourceLine.put(context.sourceStartIndex, context);
			contextsByAssemblyLine.put(context.assemblyStartIndex, context);
			assemblyStartIndex = assemblyEndIndex;
			sourceStartIndex = sourceEndIndex;
		}
		ExecutionContext context = new ExecutionContext(sourceStartIndex, sourceStartIndex + 1, assemblyStartIndex, -1);
		contextsBySourceLine.put(context.sourceStartIndex, context);
		contextsByAssemblyLine.put(context.assemblyStartIndex, context);

		line = input.readLine(); // Skip section label
		if (line == null)
			return;
		assert line.equals(HEAP_HEADER) : "Heap header '.Heap' expected in debug data file '" + debugFile.getAbsolutePath() + "'";

		while ((line = input.readLine()) != null) {
			if (line.equals(ASSEMBLY_COMMENT_HEADER))
				break;
			tokens = new StringTokenizer(line, ":[]");
			int location = Integer.parseInt(tokens.nextToken());
			String name = tokens.nextToken();
			List<Integer> degreeList = new ArrayList<Integer>();
			if (tokens.hasMoreTokens()) {
				while (tokens.hasMoreTokens()) {
					degreeList.add(Integer.parseInt(tokens.nextToken()));
				}
			}
			heap.add(new DebugHeapVariable(name, location, degreeList));
		}

		while ((line = input.readLine()) != null) {
			int colonIndex = line.indexOf(":");
			int commentIndex = Integer.parseInt(line.substring(0, colonIndex));
			while ((assemblyComments.size() - 1) < commentIndex) {
				assemblyComments.add("");
			}
			assemblyComments.set(commentIndex, line.substring(colonIndex + 1));
		}
		
		input.close();
	}

	public ExecutionContext getExecutionContext(int pc) {
		SortedMap<Integer, ExecutionContext> tail = contextsByAssemblyLine.headMap(pc, true);
		if (tail.isEmpty())
			return contextsByAssemblyLine.lastEntry().getValue();
		else
			return contextsByAssemblyLine.get(tail.lastKey());
	}

	public ExecutionContext sourceLineToExecutionContext(int sourceLine) {
		SortedMap<Integer, ExecutionContext> tail = contextsBySourceLine.headMap(sourceLine, true);
		if (tail.isEmpty())
			return contextsBySourceLine.lastEntry().getValue();
		else
			return contextsBySourceLine.get(tail.lastKey());
	}
}
