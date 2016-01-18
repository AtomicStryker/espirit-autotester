package espirit.mpoloczek;


import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Master-Abschlussarbeit Matthäus Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
public class Util {

	private static final ConsoleHandler handler = new ConsoleHandler();

	/***
	 * Get a slightly better or atleast more concise String description of most Java AWT Components
	 * over their respective .toString() implementations, for testing purposes.
	 * Also removes certain contextual elements such as memory location.
	 * @param c Java AWT component instance to represent in unified String format
	 * @return some error String if input is null, otherwise a unified simple representation
	 */
	public static String componentToString(final Component c) {
		if (c == null) {
			return "null?!";
		}

		String answer = ": []";
		if (c instanceof AbstractButton) {
			final AbstractButton ab = (AbstractButton) c;
			answer = c.getClass().getSimpleName() + ": [" + ab.getText() + '|' + ab.getName() + '|' + ab.getToolTipText() + ']';
		} else if (c instanceof JDialog) {
			final JDialog jd = (JDialog) c;
			answer = c.getClass().getSimpleName() + ": [" + jd.getTitle() + ']';
		} else if (c instanceof JFrame) {
			final JFrame jf = (JFrame) c;
			answer = c.getClass().getSimpleName() + ": [" + jf.getTitle() + ']';
		} else if (c instanceof JTextField) {
			final JTextField tf = (JTextField) c;
			answer = c.getClass().getSimpleName() + ": [" + tf.getText() + ']';
		} else if (c instanceof JLabel) {
			final JLabel jl = (JLabel) c;
			answer = c.getClass().getSimpleName() + ": [" + jl.getText() + ']';
		}
		return ": []".equals(answer) ? c.toString() : answer;
	}


	/***
	 * Central method to get per-class Logging, wraps around Apache Logger
	 * @param s Logger target name, usually the class to log in
	 * @return new named Logger instance to use when printing information and errors
	 */
	public static Logger getLogger(final String s) {
		final Logger l = Logger.getLogger(s);
		l.setLevel(Level.FINER);
		handler.setLevel(Level.FINER);
		l.setUseParentHandlers(false);
		for (final Handler h : l.getHandlers()) {
			l.removeHandler(h);
		}
		l.addHandler(handler);
		return l;
	}
}
