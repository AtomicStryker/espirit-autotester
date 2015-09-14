package espirit.mpoloczek;

import java.lang.reflect.Method;
import java.text.MessageFormat;


public class Launcher {

	static final String HELP =
			"Autotester application can be executed like this:\n" +
					"Automatic mode:\n" +
					"  java -cp autotest.jar[;<your_class_path>] espirit.mpoloczek.Launcher <your_main_class> <your_test_config>\n";

	public static void main(String[] args) {

		final Method mainMethod;
		final Class<?> mainClass;
		final String[] newArgs;
		boolean launch = false;

		if(args.length == 0) {
			System.err.println(HELP);
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
			String msg = MessageFormat.format("ERROR: can not find class {0} specified as argument. Please check classpath and class name.", args[0]);
			System.err.println(msg);
			return;
		} catch(final NoSuchMethodException ex) {
			String msg = MessageFormat.format("ERROR: can not find main method in the class {0}.", args[0]);
			System.err.println(msg);
			return;
		}

		// launching application
		try {
			mainMethod.invoke(mainClass, new Object[] {newArgs});
		} catch(final Exception ex) {
			String msg = MessageFormat.format("ERROR: cannot invoke main method in the class {0}.", args[0]);
			System.err.println(msg);
			ex.printStackTrace(System.err);
		}

		if (launch) new Thread(new CrawlerTester(args[1])).start();
	}
}
