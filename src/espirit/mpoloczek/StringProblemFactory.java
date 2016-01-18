package espirit.mpoloczek;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Random;

/**
 *
 * Master-Abschlussarbeit Matthäus Poloczek
 * TU Dortmund, Matrikel-Nr. 126826
 * e-Spirit 2015/2016
 *
 */
public class StringProblemFactory {

	private final ArrayList<String> problemStrings;
	private final Random random;


	/***
	 * Constructs and loads the Helper class around the stringlist.txt file
	 * located at the root of the jar (or the resources folder in the source).
	 * If any errors occur while loading, the stacktrace is printed.
	 */
	public StringProblemFactory() {

		problemStrings = new ArrayList<>(463); // happens to be the number in the file
		random = new Random();

		try (BufferedReader in = new BufferedReader(new InputStreamReader(getClass().getClassLoader().getResourceAsStream("stringlist.txt"))))
		{
			String line;
			while ((line = in.readLine()) != null) {
				if (!line.isEmpty() && !line.startsWith("#")) {
					getProblemStrings().add(line);
				}
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println("StringProblemFactory finished loading, has "+ getProblemStrings().size()+" problematic Strings ready.");
	}


	/***
	 * @return getter for the problematic String ArrayList, not a copy!
	 */
	public ArrayList<String> getProblemStrings() {
		return problemStrings;
	}

	/***
	 * @return retrieves a random problematic String from the loaded repository
	 */
	public String getRandomProblemString() {
		return problemStrings.get(random.nextInt(problemStrings.size()));
	}

}
