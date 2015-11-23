package espirit.mpoloczek;

import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
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
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Master-Abschlussarbeit Matthäus Poloczek, TU Dortmund, e-Spirit 2015
 */
public class CrawlerTester {


	public final String targetGuiName;
	public final Config config;
	public final ModelWriter modelWriter;
	public final StringProblemFactory problemStringManager;
	public final Stack<TesterThread> testerThreadStack;
	public final HashSet<Component> previousWindows;
	private final int delayToTestStartSeconds;
	private final Logger logger;
	private final Random random;
	public int popupComponentIndex;
	public boolean isCurrentlyTesting;
	public boolean expectingPopup;
	public long startTimeTest;
	public int counterButtonsPushed;
	public int counterWindowsHandled;
	protected JFrame masherframe;
	protected Thread timerThread;
	private final TimerRunner deadLockDetector;
	private Window targetGUI;
	private int componentIndexDebugPrint;
	private Component lastPopup;
	private JLabel labelTime;
	private Method setStrictCMSMethod;
	private Method methodFsMultiPaneGetSlotCount;
	private Method methodFsMultiPaneGetComponentsAtSlotID;

	private static String previousRootComponent;


	public CrawlerTester(final String configPath) {

		logger = Util.getLogger("CrawlerTester");
		config = new Config();
		if (config.loadConfigFile(configPath)) {
			targetGuiName = config.properties.getProperty("targetGUISimpleClassName");
			delayToTestStartSeconds = Integer.valueOf(config.properties.getProperty("testStartDelayInSeconds"));
			testerThreadStack = new Stack<TesterThread>();
			TesterThread.logger.setLevel(Level.FINE);
		} else {
			final Window[] allWindows = Window.getWindows();
			logger.log(Level.INFO, "Invalid config specified, maybe you want to find a target GUI? I'll print all open GUIs now");
			for (final Window curWindow : allWindows) {
				logger.log(Level.INFO, String.format("[%s] is simple name of instance %s\n", curWindow.getClass().getSimpleName(), curWindow));
			}

			throw new RuntimeException("Invalid config file, check path.");
		}

		problemStringManager = new StringProblemFactory();
		random = new Random();
		previousWindows = new HashSet<>();
		modelWriter = new ModelWriter();
		deadLockDetector = new TimerRunner();
	}

	public void execute() {
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

		labelTime = new JLabel("0", JLabel.CENTER);

		final JPanel abortPanel = new JPanel(new GridLayout(3, 1));
		abortPanel.add(giflabel);
		abortPanel.add(labelTime);
		abortPanel.add(aborter);
		masherframe.getContentPane().add(abortPanel);

		masherframe.pack();
		//masherframe.setLocationRelativeTo(null);
		masherframe.setLocation(0, 0);
		masherframe.setAlwaysOnTop(true);
		masherframe.setVisible(true);

		for (final Window curWindow : allWindows) {
			logger.log(Level.FINE, String.format("iterating rootWindow: %s\n", curWindow.getClass().getSimpleName()));
			if (targetGuiName.equals(curWindow.getClass().getSimpleName())) {

				logger.log(Level.FINE, String.format("In target GUI: %s\n", curWindow));
				componentIndexDebugPrint = 0;

				targetGUI = curWindow;
				previousRootComponent = Util.componentToString(targetGUI);
				break;
			}
		}

		if (targetGUI == null) {
			throw new RuntimeException("Did not find a target GUI named " + targetGuiName);
		}

		modelWriter.logState(targetGUI);
		detectChildren(targetGUI, componentListPrev);

		popupComponentIndex = -1;
		testerThreadStack.push(new TesterThread(this, componentListPrev, targetGUI));
		testerThreadStack.peek().start();

		timerThread = new Thread(deadLockDetector);
		timerThread.setPriority(Thread.MIN_PRIORITY);
		timerThread.start();
	}

	// TODO figure out why the logger prints to console twice, eventually
	public void onTestingFinished() {

		threadSleep(config.sleepTimeMillisBetweenFakeMouseClicks * 10);
		if (!testerThreadStack.empty()) {
			logger.log(Level.INFO, "all done? NOT done. There was atleast one new testthread added during the final sleep. Continue!");
			return; // the new testerthread will call onTestingFinished again
		}

		logger.log(Level.INFO, String.format("all done? all done. Ran %d seconds, pushed %d buttons, handled %d windows.\n", (int) Math.rint((System.currentTimeMillis() - startTimeTest) / 1000), counterButtonsPushed, counterWindowsHandled));
		// wait a wee bit, events may be still underway

		logger.log(Level.INFO, "Cooldown ended. Exporting graphml file...\n");
		modelWriter.exportToFile(config.modelOutputFolder);
		logger.log(Level.INFO, "Cleaning up the mess...\n");
		masherframe.dispose();
		timerThread.interrupt();
		isCurrentlyTesting = false;
		targetGUI.dispose();
		logger.log(Level.INFO, "And finally calling System.exit(0)!\n");
		System.exit(0);
	}


	private void initSwingPopupEventHook() {
		final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent e) {

				final String prop = e.getPropertyName();
				logger.log(Level.FINEST, String.format("Keyboardfocusmanager prop [%s], [%s]->[%s]\n", prop, e.getOldValue(), e.getNewValue()));
				if (e.getOldValue() == null && "activeWindow".equals(prop)) {

					if (e.getNewValue() instanceof Component) {

						final Component comp = (Component) e.getNewValue();
						if (comp != lastPopup && comp != masherframe && !targetGuiName.equals(comp.getClass().getSimpleName())) {

							logger.log(Level.FINE, String.format("Keyboardfocusmanager new popup detected: [%s]\n", e.getNewValue()));
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
		detectChildren(popup, popupParts, Level.FINEST);
		final Component lastComponent = popupParts.get(popupParts.size() - 1);

		final Window windowAncestor = SwingUtilities.getWindowAncestor(lastComponent);
		if (windowAncestor != null && isCurrentlyTesting) {

			boolean ignorePopup = false;
			final String windowStringRepresentation = Util.componentToString(windowAncestor);
			for (final TesterThread tt : testerThreadStack) {
				if (tt.rootWindow == windowAncestor) {
					logger.log(Level.FINE, String.format("Window %s is already being handed by Thread %s, ignoring popup\n", windowStringRepresentation, tt));
					ignorePopup = true;
					break;
				}
			}

			if (previousWindows.contains(popup)) {
				logger.log(Level.INFO, String.format("Window %s was already fully handled previously, loop occurring? Skipping it.\n", windowStringRepresentation));
				ignorePopup = true;
			}

			for (final String keyword : config.blackListedWindowKeywords) {
				if (windowStringRepresentation.contains(keyword)) {
					logger.log(Level.INFO, String.format("Window %s contains banned window keyword [%s]. Skipping it.\n", windowStringRepresentation, keyword));
					ignorePopup = true;
					break;
				}
			}

			if (!ignorePopup) {
				expectingPopup = false;
				if (!testerThreadStack.empty()) {
					final TesterThread peek = testerThreadStack.peek();
					peek.isThreadPaused = true;
					logger.log(Level.FINE, String.format("paused thread %s for popup handling\n", testerThreadStack.peek()));
					modelWriter.logState(popup);
					modelWriter.logTransition(peek.rootWindow, peek.componentListToTest.get(Math.min(peek.indexCurrentComponentTested, peek.componentListToTest.size())), popup);
				}

				final ArrayList<Component> popupContent = new ArrayList<Component>();
				detectChildren(windowAncestor, popupContent, Level.FINEST);
				final TesterThread popTester = new TesterThread(this, popupContent, windowAncestor);
				testerThreadStack.push(popTester);
				if (popupComponentIndex >= 0) {
					popTester.indexCurrentComponentTested = popupComponentIndex;
					popupComponentIndex = -1;
					logger.log(Level.FINER, String.format("resuming popup test from idx %d\n", popTester.indexCurrentComponentTested));
					threadSleep(config.sleepTimeMillisBetweenFakeMouseClicks);
				}
				logger.log(Level.INFO, String.format("starting new popup thread %s\n", popTester));
				popTester.start();
			}
		}
	}


	public void pseudoClickButton(final Component target, @Nullable final Container root, @Nullable final ArrayList<Component> adjacentComponents) {

		logger.log(Level.FINER, String.format("about to pseudo click component %s\n", Util.componentToString(target)));
		applicationHeartbeat();

		ActionEvent event = new ActionEvent(target, 42, "");

		final Window w = SwingUtilities.getWindowAncestor(target);
		if (w != null && "CMSDialog".equals(w.getClass().getSimpleName())) {

			try {
				if (setStrictCMSMethod == null) {
					setStrictCMSMethod = w.getClass().getDeclaredMethod("setStrict", boolean.class);
				}
				setStrictCMSMethod.invoke(w, false);
			} catch (final Exception e) {
				logger.log(Level.SEVERE, "CMSDialog hack failure - setStrict method", e);
				throw new RuntimeException("CMSDialog hack failure - setStrict method...");
			}
		}

		if (target instanceof JToggleButton || target instanceof JRadioButtonMenuItem || target instanceof JCheckBoxMenuItem) {
			if (isBlackListedButton((AbstractButton) target, w)) {
				if (w != null) {
					w.dispose();
				}
				return;
			}
			final EDTCompliantSelector selector = new EDTCompliantSelector();
			selector.buttonToSelect = (AbstractButton) target;
			SwingUtilities.invokeLater(selector);

		} else if (target instanceof AbstractButton) { // order is important as AbstractButton is a base class of the above
			final AbstractButton fsb = (AbstractButton) target;
			if (isBlackListedButton(fsb, w)) {
				if (w != null) {
					w.dispose();
				}
				return;
			}
			event = new ActionEvent(target, 42, fsb.getActionCommand());

		} else if (target instanceof JTextComponent) {

			final JTextComponent jTextComponent = (JTextComponent) target;
			if (jTextComponent.isEditable()) {

				if (jTextComponent.isVisible()) {

					if (jTextComponent instanceof JTextField) {
						for (final String s : problemStringManager.getProblemStrings()) {
							logger.log(Level.FINEST, String.format("setting jtextfield text to [%s] and firing action event\n", s));
							final EDTCompliantTextSetter setter = new EDTCompliantTextSetter();
							setter.jTextComponent = jTextComponent;
							setter.textToSet = s;
							SwingUtilities.invokeLater(setter);
							threadSleep(config.sleepTimeMillisTextfieldEntries);
						}
					}
				}
				setJTextComponentToRandomString(jTextComponent);
			}

			return;
		}
		counterButtonsPushed++;

		final EDTCompliantActionPerformer actionDoer = new EDTCompliantActionPerformer();
		actionDoer.event = event;
		actionDoer.listeners = target.getListeners(ActionListener.class);
		actionDoer.componentRoot = root;
		actionDoer.componentList = adjacentComponents;
		actionDoer.componentTarget = target;
		SwingUtilities.invokeLater(actionDoer);
	}


	private boolean isBlackListedButton(final AbstractButton fsb, @Nullable final Object w) {
		for (final Config.BlackListedAbstractButton bab : config.blackListedAbstractButtons) {
			if (bab != null) {

				if ((bab.command.isEmpty() || (fsb.getActionCommand() != null && fsb.getActionCommand().contains(bab.command)))
				&& (bab.title.isEmpty()	|| (w != null && getTitle(w) != null && getTitle(w).contains(bab.title)))
				&& (bab.name.isEmpty() || (fsb.getName() != null && fsb.getName().contains(bab.name)))) {
					logger.log(Level.INFO, String.format("Nope-ing away from hardcoded blacklisted button [%s|%s|%s]\n", bab.command, bab.title, bab.name));
					return true;
				}
			}
		}
		return false;
	}


	private String getTitle(final Object w) {
		if (w instanceof Dialog) {
			return ((Dialog) w).getTitle();
		}
		if (w instanceof JFrame) {
			return ((JFrame) w).getTitle();
		}
		return "";
	}


	private void setJTextComponentToRandomString(final JTextComponent jTextComponent) {
		final String s = problemStringManager.getRandomProblemString();
		logger.log(Level.INFO, String.format("leaving jtextcomponent text at randomly chosen string [%s]\n", s));
		final EDTCompliantTextSetter setter = new EDTCompliantTextSetter();
		setter.jTextComponent = jTextComponent;
		setter.textToSet = s;
		SwingUtilities.invokeLater(setter);
	}


	private void detectChildren(final Component component, final ArrayList<Component> componentList) {

		detectChildren(component, componentList, Level.FINER);
	}


	private void detectChildren(final Component component, final ArrayList<Component> componentList, final Level logLevel) {

		final String compdesc = Util.componentToString(component);
		if (component == null) {
			return;
		}
		if (!component.isVisible()) {

			logger.log(logLevel, String.format("Ignoring invisible %s\n", compdesc));

		} else if (config.isComponentBlacklisted(component)) {

			logger.log(logLevel, String.format("Ignoring blacklisted component %s\n", compdesc));

		} else if (component instanceof JFrame || component instanceof JPanel || component instanceof JRootPane || component instanceof JLayeredPane || component instanceof JMenuBar || component instanceof JToolBar || component instanceof JPopupMenu.Separator || component instanceof javax.swing.JSeparator || component instanceof Box.Filler || component instanceof JScrollPane || component instanceof JViewport || component instanceof JList) {

			// not targets
			logger.log(logLevel, String.format("Ignoring general uninteresting class %s\n", compdesc));

		} else if ("JXLayer".equals(component.getClass().getSimpleName())) {

			logger.log(logLevel, "Ran into JXLayer, skipping all of that (for now?!)");
			return;
		} else if (!component.isEnabled()) {

			logger.log(logLevel, String.format("Ignoring disabled: %s\n", compdesc));
		} else {

			logger.log(logLevel, String.format("componentList add %d: %s\n", componentIndexDebugPrint++, compdesc));
			componentList.add(component);
		}

		if (component instanceof JMenu) {
			final JMenu menu = (JMenu) component;
			logger.log(logLevel, String.format("found a menu [%s] with subitem count: [%d] ===================================\n", menu.getText(), menu.getMenuComponents().length));
			for (final Component c : menu.getMenuComponents()) {
				detectChildren(c, componentList, logLevel);
			}
			logger.log(logLevel, "MENU END =================================================================================");
		} else if ("FsMultiSplitPane".equals(component.getClass().getSimpleName())) {

			if (methodFsMultiPaneGetComponentsAtSlotID == null) {
				try {
					methodFsMultiPaneGetComponentsAtSlotID = component.getClass().getDeclaredMethod("getComponentAt", int.class);
					methodFsMultiPaneGetSlotCount = component.getClass().getDeclaredMethod("getSlotCount");
				} catch (final NoSuchMethodException e) {
					throw new RuntimeException(e);
				}
			}

			try {
				final int slotCount = (int) methodFsMultiPaneGetSlotCount.invoke(component);
				for (int i = 0; i < slotCount; i++) {
					final Component chack = (Component) methodFsMultiPaneGetComponentsAtSlotID.invoke(component, i);
					logger.log(Level.FINEST, String.format("Ran into FsMultiSplitPane, checking out component slot %d: %s\n", i, Util.componentToString(chack)));
					detectChildren(chack, componentList, logLevel);
				}

			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
		} else if (component instanceof Container) {

			final Container container = (Container) component;
			final int componentCount = container.getComponentCount();
			for (int i = 0; i < componentCount; i++) {
				final Component nextComp = container.getComponent(i);
				detectChildren(nextComp, componentList, logLevel);
			}
		}
	}


	public void threadSleep(final long millis) {
		try {
			applicationHeartbeat();
			Thread.sleep(millis);
			applicationHeartbeat();
		} catch (final InterruptedException e) {
			logger.log(Level.SEVERE, "Threadsleep got interrupted externally?!", e);
		}
	}


	public void applicationHeartbeat() {
		// to prove to the background timer we are still alive and kicking
		deadLockDetector.lastAction = System.currentTimeMillis();
	}


	public void killWindow(final Window rootWindow, final String killer) {

		final EDTCompliantWindowMurderer murder = new EDTCompliantWindowMurderer();
		murder.windowCast = rootWindow;
		murder.killer = killer;
		SwingUtilities.invokeLater(murder);
	}

	class EDTCompliantWindowMurderer implements Runnable {

		Window windowCast;
		String killer;

		@Override
		public void run() {
			// TODO this still does not kill the freaking about screen?!
			try {
				windowCast.dispatchEvent(new KeyEvent(windowCast, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, (char)KeyEvent.VK_ESCAPE));
				windowCast.dispatchEvent(new WindowEvent(windowCast, WindowEvent.WINDOW_CLOSING));
			} catch (final Exception e) {
				logger.log(Level.FINE, "Exception thrown by fake key/window events", e);
			}
			threadSleep(config.sleepTimeMillisBetweenFakeMouseClicks);
			if (windowCast.isValid()) windowCast.dispose();
			logger.log(Level.FINE, String.format("%s disposed by %s., disposing\n", Util.componentToString(windowCast), killer));
		}
	}


	class EDTCompliantActionPerformer implements Runnable {

		ActionEvent event;
		ActionListener[] listeners;
		Container componentRoot;
		ArrayList<Component> componentList;
		Component componentTarget;


		@Override
		public void run() {

			if (componentTarget.isVisible()) {

				final String rootStatePre = Util.componentToString(componentRoot);

				for (final ActionListener el : listeners) {
					try {
						el.actionPerformed(event);
					} catch (final Exception | Error e) {
						logger.log(Level.WARNING, "ActionListener threw something", e);
					}
				}

				if (componentRoot != null) {
					final String rootStatePost = Util.componentToString(componentRoot);
					if (!rootStatePost.equals(previousRootComponent)) {

						logger.log(Level.FINER, String.format("last buttonpress changed root component state! [%s] -> [%s]\n", previousRootComponent, rootStatePost));
						modelWriter.logState(componentRoot);
						modelWriter.logTransitionString(previousRootComponent, Util.componentToString(componentTarget), rootStatePost);
					}
					previousRootComponent = rootStatePost;
				}

				if (componentRoot != null && componentList != null) {
					final ArrayList<Component> oldComponentList = new ArrayList<>(componentList);
					final ArrayList<Component> newComponentList = new ArrayList<>(componentList.size());
					detectChildren(componentRoot, newComponentList, Level.FINEST);

					int actualAdditions = 0;
					for (final Component c : newComponentList) {
						if (!oldComponentList.remove(c)) {
							logger.log(Level.FINEST, "Found new Component after pushing a button and comparing with old component list!!");
							logger.log(Level.FINEST, String.format("detected new Component: %s\n", c));
							final ArrayList<Component> newlyDetectedComponents = new ArrayList<>();
							detectChildren(c, newlyDetectedComponents, Level.FINEST);
							for (final Component newc : newlyDetectedComponents) {
								if (!componentList.contains(newc)) {
									componentList.add(newc);
									actualAdditions++;
								}
							}
						}
					}
					if (actualAdditions > 0 || !oldComponentList.isEmpty()) {
						logger.log(Level.FINER, String.format("last buttonpress resulted in %d new components for the master component list, and %d old gone\n", actualAdditions, oldComponentList.size()));
					}

					for (final Component c : oldComponentList) {
						logger.log(Level.FINEST, String.format("old AWOL Component: %s, removed: %s\n", c, componentList.remove(c) ? "success" : "notfound"));
					}
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

	class EDTCompliantSelector implements Runnable {

		AbstractButton buttonToSelect;


		@Override
		public void run() {
			buttonToSelect.setSelected(random.nextBoolean());
			logger.log(Level.INFO, String.format("Togglebutton %s, randomly leaving it %s...\n", Util.componentToString(buttonToSelect), buttonToSelect.isSelected() ? "ON" : "OFF"));
		}
	}

	class TimerRunner implements Runnable {

		long lastAction;

		@Override
		public void run() {
			while (!Thread.interrupted()) {
				final long curTime = System.currentTimeMillis();

				if (lastAction > 0 && curTime - lastAction > 10000l) {
					// nothing happened for 10 seconds! are we deadlocked?!
					lastAction = -1;
					logger.log(Level.SEVERE, "No actions were taken for 10 seconds, deadlock situation??");
				}

				final int seconds = (int) Math.rint((curTime - startTimeTest) / 1000);
				labelTime.setText(String.format("%02d:%02d", (int) Math.floor(seconds / 60), seconds % 60));
			}
		}
	}

}
