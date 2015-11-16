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


public class ModelWriter {

	private final DirectedGraph<String, DefaultEdge> graph;
	private final Logger logger;
	//private final Pattern suffixCatcher = Pattern.compile("^(.*)-(\\d*)$");

	public ModelWriter() {

		graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		logger = Util.getLogger("ModelWriter");
	}

	public void logState(final Component stateRootComponent) {

		wrappedAddVertex(stateRootComponent);
	}

	public void logTransition(final Component prevState, final Component transitioner, final Component resultState) {

		final String transString = wrappedToString(transitioner);
		wrappedAddVertex(transString);
		logger.log(Level.INFO, String.format("MW adding transition: %s -> %s -> %s\n", wrappedToString(prevState), transString, wrappedToString(resultState)));
		graph.addEdge(wrappedToString(prevState), transString);
		graph.addEdge(transString, wrappedToString(resultState));
		graph.addEdge(wrappedToString(resultState), wrappedToString(prevState));
	}

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
			graph.addVertex(asString);
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
