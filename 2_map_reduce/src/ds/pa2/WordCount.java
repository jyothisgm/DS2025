package ds.pa2;

import java.io.IOException;

/**
 * This is an example MapReduce user application. It counts word occurrences in
 * a set of output files. This application is used as an example in the original
 * MapReduce paper as well. Note that all IO, communication, load balancing and
 * distributed operations are completely hidden from the user. Still, this can
 * scale to many thousands of files and compute nodes.
 */
public final class WordCount implements MapReduceApplication {
	String[] args;
	MapReduce mr;
	Config config;

	/**
	 * The MapReduce framework calls this configure method on ALL nodes (the
	 * coordinator and all clients) when the application is startedThe arguments
	 * passed in via "args" are: 0: the class name of the user application (e.g.,
	 * WordCount) 1: the input directory 2: the intermediate directory 3. the output
	 * directory
	 * 
	 */
	@Override
	public void configure(MapReduce mr, String[] args) throws IllegalArgumentException, IOException {
		this.mr = mr;
		this.args = args;

		// Create a config object with the configuration, and then configure MapReduce.
		config = new Config(args[1], args[2], args[3]);
		mr.configure(config);
		mr.generateMapQueue();
	}

	/**
	 * The MapReduce framework calls this start method ON THE COORDINATOR node when
	 * the application is started. All other nodes will run clients that ask the
	 * coordinator for work.
	 */
	@Override
	public void start(String worker) throws IllegalArgumentException, IOException {
		mr.mapReduce(worker);
	}

	@Override
	public void map(String key, Iterable<String> lines) throws IOException {
		for (String line : lines) {
			String[] words = line.split("[^a-zA-Z]+");
			for (String word : words) {
				if (word.length() > 0)
					// We convert all words to lower case, we are interested in the words, not in
					// their capitalization.
					mr.emitIntermediate(word.toLowerCase(), "1");
			}
		}
	}

	@Override
	public void reduce(String key, Iterable<String> values) throws IOException {
		int sum = 0;
		for (String value : values) {
			sum += Integer.parseInt(value);
		}

		mr.emitOutput(key, "" + sum);
	}

	@Override
	public void postProcess(String key, String value1, String value2) throws IOException {
		mr.emitFinal(key, "" + (Integer.parseInt(value1) + Integer.parseInt(value2)));
	}

	@Override
	public MapReduce getMr() {
		return this.mr;
	}
}
