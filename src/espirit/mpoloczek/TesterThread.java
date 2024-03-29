package espirit.mpoloczek;


import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Master-Abschlussarbeit Matth�us Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
class TesterThread extends Thread {

	static final Logger logger = Util.getLogger("TesterThread");
	final CrawlerTester crawlerTester;
	final ArrayList<Component> componentListToTest;
	final Container rootWindow;

	int indexCurrentComponentTested;
	boolean isThreadAborted;
	boolean isThreadPaused;


	protected TesterThread(final CrawlerTester crawlerTester, final ArrayList<Component> componentListToTest, final Container rootWindow) {
		this.crawlerTester = crawlerTester;
		this.componentListToTest = componentListToTest;
		this.rootWindow = rootWindow;
	}


	@Override
	public void run() {

		// lets bring some chaos into the equation
		Collections.shuffle(componentListToTest);

		crawlerTester.isCurrentlyTesting = true;
		for (; !isThreadAborted && indexCurrentComponentTested < componentListToTest.size(); indexCurrentComponentTested++) {

			if (!isThreadAborted) {
				logger.log(Level.FINE, String.format("%s now testing it's component %d (from %d)\n", this, indexCurrentComponentTested, componentListToTest.size()));
				final Window w = SwingUtilities.getWindowAncestor(componentListToTest.get(indexCurrentComponentTested));
				if (w != null && !w.isDisplayable()) {
					//  was disposed, did our popup die?
					if (crawlerTester.testerThreadStack.size() > 1) {
						logger.log(Level.FINE, String.format("tester %s lost display context, attempting to recreate popup and continue from idx %d\n", this, indexCurrentComponentTested));
						crawlerTester.popupComponentIndex = indexCurrentComponentTested;
					} else {
						logger.log(Level.FINE, String.format("tester %s lost display context, but there is no previous context to restore anything from...\n", this));
					}
					break;
				}

				crawlerTester.pseudoClickButton(componentListToTest.get(indexCurrentComponentTested), rootWindow, componentListToTest);
				crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
			}

			while (isThreadPaused) { Thread.yield(); } // yield is necessary, else thread can be parked permanently wtf
		}

		// was testing a popup, lost the context (closed it?)
		boolean skipStackPop = false;
		// or popup restore failed
		boolean popupRestoreFailed = false;
		if (!isThreadAborted && crawlerTester.popupComponentIndex >= 0) {

			// we need to go back and create the popup again
			// first delete this thread off the stack
			if (crawlerTester.testerThreadStack.peek() == this) {
				crawlerTester.testerThreadStack.pop();
			} else {
				logger.log(Level.SEVERE, String.format("#1 TesterThread running %s is not the one ontop of the stack %s WTF\n", this, crawlerTester.testerThreadStack.peek()));
			}

			// then re-do the last thing
			final TesterThread peek = crawlerTester.testerThreadStack.peek();
			// subsequent presses may have removed components
			peek.indexCurrentComponentTested = Math.min(peek.componentListToTest.size()-1, peek.indexCurrentComponentTested);
			logger.log(Level.FINE, String.format("tester attempting to restore rootWindow context from button %s, index %d\n", Util.componentToString(peek.componentListToTest.get(peek.indexCurrentComponentTested)), peek.indexCurrentComponentTested));

			crawlerTester.expectingPopup = true;
			int offset = 0;
			int remainingAttempts = 10;
			while (!isThreadAborted && crawlerTester.expectingPopup) {
				if (remainingAttempts > 0 && crawlerTester.testerThreadStack.peek().indexCurrentComponentTested + offset >= 0) {
					crawlerTester.pseudoClickButton(peek.componentListToTest.get(peek.indexCurrentComponentTested + offset), null, null);
					logger.log(Level.FINEST, String.format("tester now trying button index %d to recreate lost context\n", peek.indexCurrentComponentTested + offset));
					crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
					offset--;
					remainingAttempts--;
					skipStackPop = true;
				} else {
					logger.log(Level.FINE, "popup restoration failed ... just moving on");
					skipStackPop = false;
					crawlerTester.popupComponentIndex = -1;
					crawlerTester.expectingPopup = false;
					popupRestoreFailed = true;
				}
			}
		}

		if (!skipStackPop) {
			// finished testing this threads queue
			// remove this thread from the stack
			if (!popupRestoreFailed) {
				if (!crawlerTester.testerThreadStack.empty()) {
					if (crawlerTester.testerThreadStack.peek() == this) {
						crawlerTester.testerThreadStack.pop();
					} else {
						logger.log(Level.SEVERE, String.format("#2 TesterThread running %s is not the one ontop of the stack %s WTF\n", this, crawlerTester.testerThreadStack.peek()));
					}
				}
			}

			// also murder the topmost rootWindow just in case
			if (rootWindow instanceof Window && !crawlerTester.targetGuiName.equals(rootWindow.getClass().getSimpleName())) {
				final Window windowCast = (Window) rootWindow;
				logger.log(Level.FINE, String.format("Finshed testing %s from %s, disposing\n", Util.componentToString(rootWindow), this));
				crawlerTester.killWindow(windowCast, toString());
				crawlerTester.counterWindowsHandled++;
				crawlerTester.previousWindows.add(rootWindow);
				crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks*2);
			}

			if (crawlerTester.testerThreadStack.empty()) {

				crawlerTester.onTestingFinished();
			} else {

				if (!isThreadAborted) {
					crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
					logger.log(Level.FINE, String.format("resuming paused thread %s after popup handling\n", crawlerTester.testerThreadStack.peek()));
					crawlerTester.testerThreadStack.peek().isThreadPaused = false;
				} else {
					crawlerTester.testerThreadStack.peek().isThreadAborted = true;
					crawlerTester.testerThreadStack.peek().isThreadPaused = false;
				}
			}
		}

		if (isThreadAborted) {
			logger.log(Level.INFO, String.format("Thread %s aborted.\n", this));
		}
	}


	@Override
	public String toString() {
		return super.toString()+ '[' +Util.componentToString(rootWindow)+ ']';
	}
}