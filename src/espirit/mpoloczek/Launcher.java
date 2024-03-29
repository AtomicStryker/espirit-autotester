package espirit.mpoloczek;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Master-Abschlussarbeit Matth�us Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
public class Launcher {

	static final String HELP =
			"Autotester application can be executed like this:\n" +
					"Automatic mode:\n" +
					"  java -cp autotest.jar[;<your_class_path>] espirit.mpoloczek.Launcher <your_main_class> <your_test_config> [<output_folder>]\n";


	/***
	 * JAR main class main method, to launch the application with the required inputs or help gathering those
	 * If launched with no arguments, prints some help on how to launch correctly.
	 * If launched with an invalid config file path, the test application will launch but no testing will occur.
	 * @param args expects at least one arguments, the target testable applications Main class
	 *             first argument is a filepath to the test config file
	 *             second and optional argument is the filepath to the model output folder
	 */
	public static void main(final String[] args) {

		final Method mainMethod;
		final Class<?> mainClass;
		final Logger logger = Util.getLogger("Launcher");

		logger.log(Level.INFO, MessageFormat.format("AutoTester running, detected arg count {0}", args.length));
		if(args.length == 0) {
			logger.log(Level.INFO, HELP);
			return;
		}

		try {
			mainClass = Class.forName(args[0]);
			mainMethod = mainClass.getMethod("main", String[].class);
		} catch(final ClassNotFoundException ex) {
			logger.log(Level.SEVERE, MessageFormat.format("ERROR: can not find class {0} specified as argument. Please check classpath and class name.", args[0]));
			return;
		} catch(final NoSuchMethodException ex) {
			logger.log(Level.SEVERE, MessageFormat.format("ERROR: can not find main method in the class {0}.", args[0]));
			return;
		}

		// launching application
		try {
			logger.log(Level.INFO, MessageFormat.format("Now invoking main method of class [{0}]", args[0]));
			mainMethod.invoke(mainClass, new Object[]{new String[]{}});
		} catch(final Exception ex) {
			logger.log(Level.SEVERE, MessageFormat.format("ERROR: cannot invoke main method in the class {0}.", args[0]), ex);
			return;
		}

		// replace backslashes with slashes for windows batchfile compatibility
		final CrawlerTester ct = new CrawlerTester(args[1].replace("\\", "/"), args.length > 2 ? args[2] : "");
		ct.execute();
	}
}
