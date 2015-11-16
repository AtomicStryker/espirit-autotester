package espirit.mpoloczek;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Launcher {

	static final String HELP =
			"Autotester application can be executed like this:\n" +
					"Automatic mode:\n" +
					"  java -cp autotest.jar[;<your_class_path>] espirit.mpoloczek.Launcher <your_main_class> <your_test_config>\n";

	public static void main(final String[] args) {

		final Method mainMethod;
		final Class<?> mainClass;
		final String[] newArgs;
		boolean launch = false;
		final Logger logger = Util.getLogger("Launcher");

		logger.log(Level.INFO, MessageFormat.format("AutoTester running, detected arg count {0}", args.length));
		if(args.length == 0) {
			logger.log(Level.INFO, HELP);
			return;
		} else if (args.length == 2) {
			newArgs = new String[0];
			launch = true;
		} else {
			newArgs = new String[args.length - 1];
			System.arraycopy(args, 1, newArgs, 0, newArgs.length);
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
			logger.log(Level.INFO, MessageFormat.format("Now invoking main method of class [{0}] with args [{1}]", args[0], args.length > 1 ? args[1] : ""));
			mainMethod.invoke(mainClass, new Object[] {newArgs});
		} catch(final Exception ex) {
			logger.log(Level.SEVERE, MessageFormat.format("ERROR: cannot invoke main method in the class {0}.", args[0]), ex);
		}

		if (launch) {
			final CrawlerTester ct = new CrawlerTester(args[1]);
			ct.execute();
		}
	}
}
