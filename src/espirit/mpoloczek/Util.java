package espirit.mpoloczek;

import javax.swing.AbstractButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;
import java.awt.Component;


public class Util {

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
			answer = c.getClass().getSimpleName() + ": [" + ab.getText() + ']';
		} else if (c instanceof JDialog) {
			final JDialog jd = (JDialog) c;
			answer = c.getClass().getSimpleName() + ": [" + jd.getTitle() + ']';
		} else if (c instanceof JFrame) {
			final JFrame jf = (JFrame) c;
			answer = c.getClass().getSimpleName() + ": [" + jf.getTitle() + ']';
		} else if (c instanceof JTextField) {
			final JTextField tf = (JTextField) c;
			answer = c.getClass().getSimpleName() + ": [" + tf.getText() + ']';
		}
		return ": []".equals(answer) ? c.toString() : answer;
	}
}
