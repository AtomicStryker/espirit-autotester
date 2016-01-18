package espirit.mpoloczek;



import org.jgraph.graph.DefaultEdge;
import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.GraphMLExporter;
import org.jgrapht.ext.IntegerEdgeNameProvider;
import org.jgrapht.ext.IntegerNameProvider;
import org.jgrapht.ext.StringEdgeNameProvider;
import org.jgrapht.ext.StringNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;

import java.awt.Component;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
//import java.util.regex.Pattern;

/**
 *
 * Master-Abschlussarbeit Matthäus Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
public class ModelWriter {

	private final DirectedGraph<String, DefaultEdge> graph;
	private final Logger logger;
	//private final Pattern suffixCatcher = Pattern.compile("^(.*)-(\\d*)$");


	/***
	 * Helper class to write and track transitions to a jGraphT graph object,
	 * and eventually print that to a graphml file after testing.
	 */
	public ModelWriter() {

		graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		logger = Util.getLogger("ModelWriter");
	}


	/***
	 * Write a new state to the graph, ideally immediatly after it popped up and was checked against the blacklists
	 * @param stateRootComponent Java AWT Component representing the new state, typically a new Window
	 */
	public void logState(final Component stateRootComponent) {

		wrappedAddVertex(stateRootComponent);
	}


	/***
	 * Write a transition from one state to another to the graph, also noting down the input component
	 * which was deemed responsible for the transition to occur.
	 * @param prevState Java AWT Component representing the previous state
	 * @param transitioner Java AWT Component that accepted some input and resulted in the transition
	 * @param resultState Java AWT Component representing the resulting state
	 */
	public void logTransition(final Component prevState, final Component transitioner, final Component resultState) {

		logTransitionString( wrappedToString(prevState), wrappedToString(transitioner), wrappedToString(resultState));
	}


	/***
	 * Write a transition from one state to another to the graph, also noting down the input component
	 * which was deemed responsible for the transition to occur. Since the Component instances
	 * may have changed because of inputs, this method accepts the respective String representations instead.
	 * see @Util.componentToString as to how these representations are made.
	 * @param prevState String representing the previous state,
	 * @param transString String representing the component that triggered the transition
	 * @param resultState String representing the resulting state
	 */
	public void logTransitionString(String prevState, final String transString, final String resultState) {

		wrappedAddVertex(transString);
		logger.log(Level.INFO, String.format("MW adding transition: %s -> %s -> %s\n", prevState, transString, resultState));
		if (!graph.containsVertex(prevState)) {
			boolean found = false;
			for (final String v : graph.vertexSet()) {
				if (prevState.contains(v)) {
					logger.log(Level.WARNING, String.format("MW did not have vertex [%s], amended prevState to [%s]\n", prevState, v));
					prevState = v;
					found = true;
					break;
				}
			}
			if (!found) {
				logger.log(Level.WARNING, String.format("MW did not have vertex [%s], no partial matches either, aborting\n", prevState));
				return;
			}
		}
		graph.addEdge(prevState, transString);
		graph.addEdge(transString, resultState);
		graph.addEdge(resultState, prevState);
	}


	/***
	 * Writes a file tagged with the current date (as of execution of this method) in format
	 * model_dd.MM.yyyy_HH.mm.ss.graphml in the GraphML file format to the target folder.
	 * Prints any I/O errors while doing so to the console, but does not stop.
	 * @param modelOutputFolder target output folder path
	 */
	public void exportToFile(final String modelOutputFolder) {
		@SuppressWarnings("unchecked")
		final GraphMLExporter<String, DefaultEdge> gml =
				new GraphMLExporter<>(new IntegerNameProvider(), new StringNameProvider<String>(), new IntegerEdgeNameProvider(), new StringEdgeNameProvider());
		try {
			final SimpleDateFormat fmt = new SimpleDateFormat("dd.MM.yyyy_HH.mm.ss");
			final String filename = "model_"+fmt.format(new Date())+".graphml";
			final FileWriter fw = new FileWriter(modelOutputFolder + '/' + filename);
			gml.export(fw, graph);
		} catch (final Exception e) {
			logger.log(Level.SEVERE, "ModelWriter exportToFile problem", e);
		}
	}


	private void wrappedAddVertex(final Component c) {

		wrappedAddVertex(wrappedToString(c));
	}

	private void wrappedAddVertex(final String asString) {

		/*
		while (graph.containsVertex(asString)) {
			final String prev = asString;
			final Matcher m = suffixCatcher.matcher(asString);
			if (m.matches()) {
				asString = m.group(1) + '-' + (Integer.valueOf(m.group(2)) + 1);
			} else {
				asString += "-1";
			}
			//System.out.println("MW regexed dupe "+prev+" to "+asString);
		}
		//System.out.println("MW adding vertex: "+asString);
		*/

		if (!graph.containsVertex(asString)) {
			logger.log(Level.INFO, String.format("MW adding vertex: %s\n", asString));
			graph.addVertex(asString);
		} else {
			logger.log(Level.INFO, String.format("MW ignoring duplicate vertex: %s\n", asString));
		}
	}

	private String wrappedToString(final Component c) {

		return wrappedToString(c, Util.componentToString(c));
	}

	private String wrappedToString(final Component c, final String s) {

		if (s.isEmpty() || " ".equals(s)) {
			return "unnamed"+c.getClass().getSimpleName();
		}
		return s;
	}

}
