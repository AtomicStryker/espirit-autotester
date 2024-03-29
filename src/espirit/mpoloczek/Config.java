package espirit.mpoloczek;


import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Master-Abschlussarbeit Matth�us Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
public class Config {

	public Properties properties;
	public BlackListedAbstractButton[] blackListedAbstractButtons;
	public ArrayList<String> blackListedComponentsSimpleClassNames = new ArrayList<String>();
	public ArrayList<String> blackListedComponentsByUtilString = new ArrayList<String>();
	public ArrayList<String> blackListedWindowKeywords = new ArrayList<String>();
	public long sleepTimeMillisTextfieldEntries;
	public long sleepTimeMillisBetweenFakeMouseClicks;
	public String modelOutputFolder;
	private final Logger logger = Util.getLogger("Config");


	/***
	 * Takes a local or absolute filepath and attempts to load a config file at that path.
	 * Will print some information while attempting to locate the intended target and
	 * especially any I/O errors occuring while doing so.
	 *
	 * @param filePath relative or absolute filepath to try and locate a config file at
	 * @return true when a config file was found and loaded successfully, false otherwise
	 */
	public boolean loadConfigFile(final String filePath) {

		final String tryPath = "file:/" + filePath;
		logger.log(Level.INFO, String.format("Reading [%s] as URI, attempt: [%s]", filePath, tryPath));
		final Path path = Paths.get(URI.create(tryPath));
		properties = new Properties();

		try {
			final InputStream inStream = Files.newInputStream(path);
			properties.load(inStream);
			inStream.close();
			loadFileContent();
			return true;

		} catch (final IOException e1) {

			logger.log(Level.INFO, String.format("Reading [%s] as filepath [%s] didn't work, trying it as resource...\n", filePath, path.toFile().getAbsolutePath()));
			final InputStream inStream = getClass().getClassLoader().getResourceAsStream(filePath);
			try {
				properties.load(inStream);
				inStream.close();
				loadFileContent();
				return true;

			} catch (final Exception e2) {
				logger.log(Level.SEVERE, "Config read FAIL", e1);
				logger.log(Level.SEVERE, "Config read FAIL", e2);
				return false;
			}
		}
	}

	private void loadFileContent() {
		sleepTimeMillisTextfieldEntries = Long.valueOf(properties.getProperty("sleepTimeMillisTextfieldEntries", "100"));
		sleepTimeMillisBetweenFakeMouseClicks = Long.valueOf(properties.getProperty("sleepTimeMillisBetweenFakeMouseClicks", "500"));
		modelOutputFolder = properties.getProperty("modelOutputFolder");

		final String str = properties.getProperty("blackListedAbstractButtons", "");
		if (str != null && !str.isEmpty()) {
			final String[] entries = str.split("(?<!\\\\),");
			blackListedAbstractButtons = new BlackListedAbstractButton[entries.length];
			for (int i = 0; i < entries.length; i++) {
				final String[] keyValue = entries[i].split("#");
				blackListedAbstractButtons[i] = new BlackListedAbstractButton();
				blackListedAbstractButtons[i].command = keyValue[0];
				if (keyValue.length > 1) {
					blackListedAbstractButtons[i].title = keyValue[1];
					if (keyValue.length > 2) {
						blackListedAbstractButtons[i].name = keyValue[2];
					} else {
						blackListedAbstractButtons[i].name = "";
					}
				} else {
					blackListedAbstractButtons[i].title = "";
					blackListedAbstractButtons[i].name = "";
				}
				logger.log(Level.INFO, "Blacklisted Abstract Button: [" + blackListedAbstractButtons[i].command
						+ '|' + blackListedAbstractButtons[i].title+ '|' + blackListedAbstractButtons[i].name + ']');
			}
		} else {
			blackListedAbstractButtons = new BlackListedAbstractButton[0];
		}

		final String blcstr = properties.getProperty("blackListedComponentsSimpleClassNames");
		if (blcstr != null) {
			for (final String e : blcstr.split(",")) {
				if (!e.isEmpty()) {
					blackListedComponentsSimpleClassNames.add(e);
					logger.log(Level.INFO, "Blacklisted Component classname: " + e);
				}
			}
		}

		final String blcus = properties.getProperty("blackListedComponentsByUtilString");
		if (blcus != null) {
			for (final String e : blcus.split(",")) {
				if (!e.isEmpty()) {
					blackListedComponentsByUtilString.add(e);
					logger.log(Level.INFO, "Blacklisted Component by Util name: " + e);
				}
			}
		}

		final String blcstrwdw = properties.getProperty("blackListedWindowKeywords");
		if (blcstrwdw != null) {
			for (final String s : blcstrwdw.split(",")) {
				if (!s.isEmpty()) {
					blackListedWindowKeywords.add(s);
					logger.log(Level.INFO, "Blacklisted Window Keyword: " + s);
				}
			}
		}
	}


	/***
	 * Wrapper for the configs blacklists, checks all of them for components against input
	 * @param component Java AWT instance to check for having been blacklisted by config
	 * @return true if the component matches any of the blacklists, false otherwise
	 */
	public boolean isComponentBlacklisted(final Component component) {

		String name = component.getClass().getName();
		for (final String s : blackListedComponentsSimpleClassNames) {
			if (!s.isEmpty() && name.contains(s)) {
				return true;
			}
		}

		name = Util.componentToString(component);
		for (final String s : blackListedComponentsByUtilString) {
			if (!s.isEmpty() && name.contains(s)) {
				return true;
			}
		}

		return false;
	}


	/***
	 * Data holder class for Blacklist instances, all of the entries
	 * are optional (but one should be valid, for obvious reasons).
	 * Optional means not null but empty! if unused.
	 */
	public static class BlackListedAbstractButton {
		String title;
		String command;
		String name;
	}

}
