package espirit.mpoloczek;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Window;
import java.util.ArrayList;


class TesterThread extends Thread {

	final CrawlerTester crawlerTester;
	final ArrayList<Component> componentListToTest;
	final Component rootWindow;

	int indexCurrentComponentTested;
	boolean isThreadAborted;
	boolean isThreadPaused;


	protected TesterThread(final CrawlerTester crawlerTester, final ArrayList<Component> componentListToTest, final Component rootWindow) {
		this.crawlerTester = crawlerTester;
		this.componentListToTest = componentListToTest;
		this.rootWindow = rootWindow;
	}


	@Override
	public void run() {

		crawlerTester.isCurrentlyTesting = true;
		for (; !isThreadAborted && indexCurrentComponentTested < componentListToTest.size(); indexCurrentComponentTested++) {

			while (isThreadPaused) { Thread.yield(); } // yield is necessary, else thread can be parked permanently wtf

			if (!isThreadAborted) {
				crawlerTester.debugLog("%s now testing it's component %d\n", this, indexCurrentComponentTested);
				final Window w = SwingUtilities.getWindowAncestor(componentListToTest.get(indexCurrentComponentTested));
				if (w != null && !w.isDisplayable()) {
					//  was disposed, did our popup die?
					crawlerTester.debugLog("tester lost a display context, attempting to recreate popup and continue from idx %d\n", indexCurrentComponentTested);
					crawlerTester.popupComponentIndex = indexCurrentComponentTested;
					break;
				}

				crawlerTester.pseudoClickButton(componentListToTest.get(indexCurrentComponentTested), componentListToTest);
				crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
			}
		}

		// was testing a popup, lost the context (closed it?)
		boolean skipStackPop = false;
		if (!isThreadAborted && crawlerTester.popupComponentIndex >= 0) {

			// we need to go back and create the popup again
			// first delete this thread off the stack
			crawlerTester.testerThreadStack.pop();
			// then re-do the last thing
			final TesterThread peek = crawlerTester.testerThreadStack.peek();
			if (peek.indexCurrentComponentTested == peek.componentListToTest.size()) peek.indexCurrentComponentTested--;
			crawlerTester.debugLog("tester attempting to restore rootWindow context from button %s\n", crawlerTester.componentToString(peek.componentListToTest.get(peek.indexCurrentComponentTested)));

			crawlerTester.expectingPopup = true;
			int offset = 0;
			while (!isThreadAborted && crawlerTester.expectingPopup) {
				if (crawlerTester.testerThreadStack.peek().indexCurrentComponentTested +offset >= 0) {
					crawlerTester.pseudoClickButton(peek.componentListToTest.get(peek.indexCurrentComponentTested + offset), null);
					crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
					offset--;
					skipStackPop = true;
				} else {
					crawlerTester.debugLog("popup restoration failed ... just moving on\n");
					skipStackPop = false;
					crawlerTester.popupComponentIndex = -1;
					crawlerTester.expectingPopup = false;
				}
			}
		}

		if (!skipStackPop) {
			// finished testing this threads queue
			// remove this thread from the stack
			if (!crawlerTester.testerThreadStack.empty()) crawlerTester.testerThreadStack.pop();
			// also murder the topmost rootWindow just in case
			if (rootWindow instanceof Window && !crawlerTester.targetGuiName.equals(rootWindow.getClass().getSimpleName())) {
				final Window windowCast = (Window) rootWindow;
				crawlerTester.debugLog("Finshed testing %s, disposing\n", crawlerTester.componentToString(rootWindow));
				windowCast.dispose();
				crawlerTester.counterWindowsHandled++;
			}

			if (crawlerTester.testerThreadStack.empty()) {

				crawlerTester.onTestingFinished();
			} else {

				if (!isThreadAborted) {
					crawlerTester.threadSleep(crawlerTester.config.sleepTimeMillisBetweenFakeMouseClicks);
					crawlerTester.testerThreadStack.peek().isThreadPaused = false;
					crawlerTester.debugLog("resumed isThreadPaused thread %s after popup handling\n", crawlerTester.testerThreadStack.peek());
				} else {
					crawlerTester.testerThreadStack.peek().isThreadPaused = false;
					crawlerTester.testerThreadStack.peek().isThreadAborted = true;
				}
			}
		}

		if (isThreadAborted) {
			crawlerTester.debugLog("Thread %s aborted.\n", this);
		}
	}

	@Override
	public String toString() {
		return super.toString()+ '[' +crawlerTester.componentToString(rootWindow)+ ']';
	}
}