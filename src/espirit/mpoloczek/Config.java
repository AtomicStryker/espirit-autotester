package espirit.mpoloczek;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Properties;


public class Config {

	public Properties properties;
	public BlackListedAbstractButton[] blackListedAbstractButtons;
	public ArrayList<String> blackListedComponentsSimpleClassNames = new ArrayList<String>();
	public long sleepTimeMillisTextfieldEntries;
	public long sleepTimeMillisBetweenFakeMouseClicks;

	public boolean loadConfigFile(final String filePath) {

		final Path path = Paths.get(URI.create("file:/" + filePath));
		properties = new Properties();

		try {
			final InputStream inStream = Files.newInputStream(path);
			properties.load(inStream);
			inStream.close();
			loadFileContent();
			return true;

		} catch (final IOException e1) {

			System.out.printf("Reading [%s] as filepath didn't work, trying it as resource...\n", filePath);
			final InputStream inStream = getClass().getClassLoader().getResourceAsStream(filePath);
			try {
				properties.load(inStream);
				inStream.close();
				loadFileContent();
				return true;

			} catch (final IOException e2) {
				e1.printStackTrace();
				e2.printStackTrace();
				return false;
			}
		}
	}

	private void loadFileContent() {
		sleepTimeMillisTextfieldEntries = Long.valueOf(properties.getProperty("sleepTimeMillisTextfieldEntries", "100"));
		sleepTimeMillisBetweenFakeMouseClicks = Long.valueOf(properties.getProperty("sleepTimeMillisBetweenFakeMouseClicks", "500"));

		final String str = properties.getProperty("blackListedAbstractButtons", "");
		final String[] entries = str.split("(?<!\\\\),");
		blackListedAbstractButtons = new BlackListedAbstractButton[entries.length];
		for (int i = 0; i < entries.length; i++) {
			final String[] keyValue = entries[i].split("#");
			blackListedAbstractButtons[i] = new BlackListedAbstractButton();
			blackListedAbstractButtons[i].command = keyValue[0];
			if (keyValue.length > 1) {
				blackListedAbstractButtons[i].title = keyValue[1];
			} else {
				blackListedAbstractButtons[i].title = "";
			}
			System.out.println("Blacklisted Abstract Button: ["+blackListedAbstractButtons[i].command+ '|' +blackListedAbstractButtons[i].title+ ']');
		}

		final String blcstr = properties.getProperty("blackListedComponentsSimpleClassNames");
		for (final String e : blcstr.split(",")) {
			blackListedComponentsSimpleClassNames.add(e);
			System.out.println("Blacklisted Component classname: "+e);
		}
	}

	public boolean isComponentBlacklisted(final Class componentClass) {

		final String name = componentClass.getName();
		for (final String s : blackListedComponentsSimpleClassNames) {
			if (name.contains(s)) {
				return true;
			}
		}

		return false;
	}

	public static class BlackListedAbstractButton {
		String title;
		String command;
	}

}
