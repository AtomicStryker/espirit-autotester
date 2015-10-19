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


public class Util {

	private static final ConsoleHandler handler = new ConsoleHandler();

	/**
	 * Get a slightly better or atleast more concise description of most elements over their respective .toString()
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

	public static Logger getLogger(final String s) {
		final Logger l = Logger.getLogger(s);
		l.setLevel(Level.FINER);
		handler.setLevel(Level.FINER);
		for (final Handler h : l.getHandlers()) {
			l.removeHandler(h);
		}
		l.addHandler(handler);
		return l;
	}
}
