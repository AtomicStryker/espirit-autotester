package espirit.mpoloczek;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Stack;


/**
 * Master-Abschlussarbeit Matthäus Poloczek, TU Dortmund, e-Spirit 2015
 */
public class CrawlerTester implements Runnable {


	public final String targetGuiName;
	public final Config config;
	public final StringProblemFactory problemStringManager;
	public final Stack<TesterThread> testerThreadStack;
	private final int delayToTestStartSeconds;
	public int popupComponentIndex;
	public boolean isCurrentlyTesting;
	public boolean expectingPopup;
	public long startTimeTest;
	public int counterButtonsPushed;
	public int counterWindowsHandled;
	protected JFrame masherframe;
	protected Thread timerThread;
	private Window targetGUI;
	private int componentIndexDebugPrint;
	private Component lastPopup;
	private JLabel time;
	private Method setStrictCMSMethod;


	public CrawlerTester(final String configPath) {

		config = new Config();
		if (config.loadConfigFile(configPath)) {
			targetGuiName = config.properties.getProperty("targetGUISimpleClassName");
			delayToTestStartSeconds = Integer.valueOf(config.properties.getProperty("testStartDelayInSeconds"));
			testerThreadStack = new Stack<TesterThread>();
		} else {
			final Window[] allWindows = Window.getWindows();
			debugLog("Invalid config specified, maybe you want to find a target GUI? I'll print all open GUIs now\n");
			for (final Window curWindow : allWindows) {
				debugLog("[%s] is simple name of instance %s\n", curWindow.getClass().getSimpleName(), curWindow);
			}

			throw new RuntimeException("Invalid config file, check path.");
		}

		problemStringManager = new StringProblemFactory();
	}


	@Override
	public void run() {
		threadSleep(delayToTestStartSeconds * 1000l);
		initSwingPopupEventHook();
		initTesterGUI();
	}


	private void initTesterGUI() {
		isCurrentlyTesting = false;

		final Window[] allWindows = Window.getWindows();

		targetGUI = null;
		final ArrayList<Component> componentListPrev = new ArrayList<>(200);

		startTimeTest = System.currentTimeMillis();
		counterButtonsPushed = 0;
		counterWindowsHandled = 0;

		masherframe = new JFrame("Scientific Autotest in progress");
		masherframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final URL resource = getClass().getClassLoader().getResource("buttonmasher.gif");
		final Image img = Toolkit.getDefaultToolkit().getImage(resource);
		final ImageIcon iicon = new ImageIcon(img);
		final JLabel giflabel = new JLabel(iicon);
		giflabel.setVerticalTextPosition(JLabel.TOP);

		@SuppressWarnings("serial") final JButton aborter = new JButton(new AbstractAction("Abort Autotest") {
			public void actionPerformed(final ActionEvent e) {
				while (!testerThreadStack.empty()) {
					testerThreadStack.peek().isThreadAborted = true;
				}
			}
		});

		aborter.setVerticalTextPosition(AbstractButton.BOTTOM);
		aborter.setHorizontalTextPosition(AbstractButton.LEADING);

		time = new JLabel("0", JLabel.CENTER);

		final JPanel abortPanel = new JPanel(new GridLayout(3, 1));
		abortPanel.add(giflabel);
		abortPanel.add(time);
		abortPanel.add(aborter);
		masherframe.getContentPane().add(abortPanel);

		masherframe.pack();
		//masherframe.setLocationRelativeTo(null);
		masherframe.setLocation(0, 0);
		masherframe.setAlwaysOnTop(true);
		masherframe.setVisible(true);

		for (final Window curWindow : allWindows) {
			debugLog("\niterating rootWindow: %s\n", curWindow.getClass().getSimpleName());
			if (targetGuiName.equals(curWindow.getClass().getSimpleName())) {

				debugLog("In target GUI: %s\n", curWindow);
				debugLog("\n");
				componentIndexDebugPrint = 0;

				targetGUI = curWindow;
				break;
			}
		}

		if (targetGUI == null) {
			throw new RuntimeException("Did not find a target GUI named " + targetGuiName);
		}

		detectChildren(targetGUI, componentListPrev);

		popupComponentIndex = -1;
		testerThreadStack.add(new TesterThread(this, componentListPrev, targetGUI));
		testerThreadStack.peek().start();

		timerThread = new Thread(new TimerRunner());
		timerThread.setPriority(Thread.MIN_PRIORITY);
		timerThread.start();
	}


	public void onTestingFinished() {

		masherframe.dispose();
		debugLog("all done? all done. Ran %d seconds, pushed %d buttons, handled %d windows.\n", (int) Math.rint((System.currentTimeMillis() - startTimeTest) / 1000), counterButtonsPushed, counterWindowsHandled);
		timerThread.interrupt();
		// wait a wee bit, events may be still underway
		threadSleep(config.sleepTimeMillisBetweenFakeMouseClicks * 3);
		isCurrentlyTesting = false;

		targetGUI.dispose();
		System.exit(0);
	}


	private void initSwingPopupEventHook() {
		final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent e) {

				final String prop = e.getPropertyName();
				//debugLog("Keyboardfocusmanager prop [%s], [%s]->[%s]\n", prop, e.getOldValue(), e.getNewValue());
				if (e.getOldValue() == null && "activeWindow".equals(prop)) {

					if (e.getNewValue() instanceof Component) {

						final Component comp = (Component) e.getNewValue();
						if (comp != lastPopup && comp != masherframe && !targetGuiName.equals(comp.getClass().getSimpleName())) {

							debugLog("Keyboardfocusmanager new popup detected: [%s]\n", e.getNewValue());
							lastPopup = comp;
							onDialogPopup((Component) e.getNewValue());
						}
					}
				}
			}
		});
	}


	private void onDialogPopup(final Component popup) {

		componentIndexDebugPrint = 0;
		final ArrayList<Component> popupParts = new ArrayList<Component>();
		detectChildren(popup, popupParts, false);
		final Component lastComponent = popupParts.get(popupParts.size() - 1);

		final Window windowAncestor = SwingUtilities.getWindowAncestor(lastComponent);
		if (windowAncestor != null && isCurrentlyTesting) {

			boolean ignorePopup = false;
			for (final TesterThread tt : testerThreadStack) {
				if (tt.rootWindow == windowAncestor) {
					debugLog("Window %s is already being handed by Thread %s, ignoring popup\n", componentToString(windowAncestor), tt);
					ignorePopup = true;
					break;
				}
			}

			if (!ignorePopup) {
				expectingPopup = false;
				testerThreadStack.peek().isThreadPaused = true;
				debugLog("isThreadPaused thread %s for popup handling\n", testerThreadStack.peek());

				final ArrayList<Component> popupContent = new ArrayList<Component>();
				detectChildren(windowAncestor, popupContent, popupComponentIndex < 1);
				final TesterThread popTester = new TesterThread(this, popupContent, windowAncestor);
				testerThreadStack.push(popTester);
				if (popupComponentIndex >= 0) {
					popTester.indexCurrentComponentTested = popupComponentIndex;
					popupComponentIndex = -1;
					debugLog("resuming popup test from idx %d\n", popTester.indexCurrentComponentTested);
					threadSleep(config.sleepTimeMillisBetweenFakeMouseClicks);
				}
				popTester.start();
			}
		}
	}


	public void pseudoClickButton(final Component target, final ArrayList<Component> adjacentComponents) {

		debugLog("about to pseudo click component %s\n", componentToString(target));

		ActionEvent event = new ActionEvent(target, 42, "");

		final Window w = SwingUtilities.getWindowAncestor(target);
		// TODO magic hack, but i dont see a way to put this in a config, way too specific
		if (w != null && "CMSDialog".equals(w.getClass().getSimpleName())) {

			try {
				if (setStrictCMSMethod == null) {
					setStrictCMSMethod = w.getClass().getDeclaredMethod("setStrict", boolean.class);
				}
				setStrictCMSMethod.invoke(w, false);
			} catch (final Exception e) {
				e.printStackTrace();
				throw new RuntimeException("CMSDialog hack failure - setStrict method...");
			}
		}

		if (target instanceof JToggleButton) {
			// TODO do more than toggle on off?
			debugLog("JToggleButton, need to do something clever...\n");
			final JToggleButton jtb = (JToggleButton) target;
			final boolean select = jtb.isSelected();
			// we just switch it on-off to trigger any code implemented in that change
			jtb.setSelected(!select);
			jtb.setSelected(select);

		} else if (target instanceof JRadioButtonMenuItem) {
			// TODO handle this, select everything, randomize, pseudorandom init?
			if (adjacentComponents != null) {
				final ArrayList<JRadioButtonMenuItem> otherRadioButtons = new ArrayList<>();
				for (final Component otherC : adjacentComponents) {
					if (otherC != target && otherC instanceof JRadioButtonMenuItem) {
						otherRadioButtons.add((JRadioButtonMenuItem) otherC);
					}
				}
				for (final JRadioButtonMenuItem radioItem : otherRadioButtons) {
					radioItem.setSelected(true);
					radioItem.setSelected(false);
				}

				debugLog("JRadioButtonMenuItem, toggled it and %d other adjacents ...\n", otherRadioButtons.size());
			} else {
				debugLog("JRadioButtonMenuItem, it's alone so i just wiggle it once\n");
			}
			final JRadioButtonMenuItem menuItem = (JRadioButtonMenuItem) target;
			final boolean select = menuItem.isSelected();
			menuItem.setSelected(!select);
			menuItem.setSelected(select);

		} else if (target instanceof AbstractButton) {
			final AbstractButton fsb = (AbstractButton) target;
			for (final Config.BlackListedAbstractButton bab : config.blackListedAbstractButtons) {
				if (bab.command.equals(fsb.getActionCommand()) && (bab.title.isEmpty() || (w != null && bab.title.equals(((Dialog) w).getTitle())))) {
					debugLog("Nope-ing away from hardcoded blacklisted button [%s|%s]\n", bab.command, bab.title);
					if (w != null) {
						w.dispose();
					}
					return;
				}
			}
			event = new ActionEvent(target, 42, fsb.getActionCommand());

		} else if (target instanceof JTextComponent) {
			// TODO bwahaha evil strings come here

			final JTextComponent jTextComponent = (JTextComponent) target;
			if (jTextComponent.isEditable()) {
				debugLog("JTextComponent, now trying problematic strings lol\n");

				if (jTextComponent instanceof JTextField) {
					for (final String s : problemStringManager.getProblemStrings()) {
						//debugLog("setting jtextfield text to [%s] and firing action event\n", s);
						final EDTCompliantTextSetter setter = new EDTCompliantTextSetter();
						setter.jTextComponent = jTextComponent;
						setter.textToSet = s;
						SwingUtilities.invokeLater(setter);
						threadSleep(config.sleepTimeMillisTextfieldEntries);
					}
				}
				setJTextComponentToRandomString(jTextComponent);
			}

			return;
		}
		counterButtonsPushed++;

		// TODO improve fake clicking?
		final EDTCompliantActionPerformer actionDoer = new EDTCompliantActionPerformer();
		actionDoer.event = event;
		actionDoer.listeners = target.getListeners(ActionListener.class);
		SwingUtilities.invokeLater(actionDoer);
	}


	private void setJTextComponentToRandomString(final JTextComponent jTextComponent) {
		final String s = problemStringManager.getRandomProblemString();
		debugLog("leaving jtextcomponent text at randomly chosen string [%s]\n", s);
		final EDTCompliantTextSetter setter = new EDTCompliantTextSetter();
		setter.jTextComponent = jTextComponent;
		setter.textToSet = s;
		SwingUtilities.invokeLater(setter);
	}


	private void detectChildren(final Component component, final ArrayList<Component> componentList) {
		detectChildren(component, componentList, true);
	}


	private void detectChildren(final Component component, final ArrayList<Component> componentList, final boolean log) {

		final String compdesc = componentToString(component);
		if (component == null || !component.isVisible() || component instanceof JFrame || component instanceof JPanel || component instanceof JRootPane || component instanceof JLayeredPane || component instanceof JMenuBar || component instanceof JToolBar || component instanceof JPopupMenu.Separator || component instanceof javax.swing.JSeparator || component instanceof Box.Filler || component instanceof JScrollPane || component instanceof JViewport || component instanceof JList || config.isComponentBlacklisted(component.getClass())) {

			// not targets
			if (log) {
				debugLog("Ignoring %s\n", compdesc);
			}
		} else if ("JXLayer".equals(component.getClass().getSimpleName())) {

			if (log) {
				debugLog("\nRan into JXLayer, skipping all of that (for now?!)\n\n"); //TODO browser exclusion?
			}
			return;
		} else if (!component.isEnabled()) {

			if (log) {
				debugLog("Disabled: %s\n", compdesc);
			}
		} else if (component instanceof JTextField) {
			componentList.add(component);
		} else {

			if (log) {
				debugLog("%d: %s\n", componentIndexDebugPrint++, compdesc);
			}
			componentList.add(component);
		}

		if (component instanceof JMenu) {
			final JMenu menu = (JMenu) component;
			if (log) {
				debugLog("found a menu [%s] with subitem count: [%d] ===================================\n", menu.getText(), menu.getMenuComponents().length);
			}
			for (final Component c : menu.getMenuComponents()) {
				detectChildren(c, componentList, log);
			}
			if (log) {
				debugLog("MENU END =================================================================================\n");
			}
		}

		if (component instanceof Container) {
			final Container container = (Container) component;
			final int componentCount = container.getComponentCount();
			for (int i = 0; i < componentCount; i++) {
				final Component nextComp = container.getComponent(i);
				detectChildren(nextComp, componentList, log);
			}
		}
	}


	/**
	 * Get a slightly better or atleast more concise description of most elements over c.toString()
	 */
	public String componentToString(final Component c) {
		if (c == null) {
			return "null?!";
		}
		String answer = ": ";
		if (c instanceof AbstractButton) {
			final AbstractButton ab = (AbstractButton) c;
			answer = c.getClass().getSimpleName() + ": text:" + ab.getText() + ", command:" + ab.getActionCommand();
		} else if (c instanceof JDialog) {
			final JDialog jd = (JDialog) c;
			answer = c.getClass().getSimpleName() + ": " + jd.getTitle();
		}
		return ": ".equals(answer) ? c.toString() : answer;
	}


	public void threadSleep(final long millis) {
		try {
			Thread.sleep(millis);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void debugLog(final String s, final Object... args) {
		System.out.printf(s, args);
	}


	static class EDTCompliantActionPerformer implements Runnable {

		ActionEvent event;
		ActionListener[] listeners;


		@Override
		public void run() {
			for (final ActionListener el : listeners) {
				try {
					el.actionPerformed(event);
				} catch (final Exception | Error e) {
					e.printStackTrace();
				}
			}
		}
	}

	static class EDTCompliantTextSetter implements Runnable {

		JTextComponent jTextComponent;
		String textToSet;


		@Override
		public void run() {
			if (jTextComponent instanceof JTextField) {
				final JTextField jTextField = (JTextField) jTextComponent;
				jTextField.setText(textToSet);
				jTextField.postActionEvent();
			} else {
				jTextComponent.setText(textToSet);
			}
		}
	}

	class TimerRunner implements Runnable {

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				final int seconds = (int) Math.rint((System.currentTimeMillis() - startTimeTest) / 1000);
				time.setText(String.format("%02d:%02d", (int) Math.floor(seconds / 60), seconds % 60));
			}
		}
	}

}
