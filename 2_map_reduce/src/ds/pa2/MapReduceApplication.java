package ds.pa2;

import java.io.IOException;

/**
 * This interface defines all methods that a user defined MapReduce application
 * must implement. This is the API between the user application and the
 * MapReduce framework. Note that all IO is completely hidden from the user.
 * Likewise, the reduce only calls emitOutput(String key, String value).
 * Finally, we have an optional postProcessing step that removes duplicates in
 * the output. It should call emitFinal(String key, String value) to write the
 * final result.
 * 
 */
public interface MapReduceApplication {
	/**
	 * This method is used to pass the user configuration to the MapReduce
	 * application. This method should be called on the coordinator and all worker nodes.
	 * 
	 * @param mr   The MapReduce class that implements the actual MapReduce
	 *             functionality, including I/O.
	 * @param args the arguments that were passed on the commandline when starting
	 *             the application.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 */
	public void configure(MapReduce mr, String[] args) throws IOException, IllegalArgumentException;

	/**
	 * This method should be invoked on the coordinator node. It starts the MapReduce
	 * application.
	 * 
	 * @param mr   The MapReduce class that implements the actual MapReduce
	 *             functionality, including I/O.
	 * @param args the arguments that were passed on the commandline when starting
	 *             the application.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws InterruptedException
	 */
	public void start() throws IOException, IllegalArgumentException, InterruptedException;

	/**
	 * The map method should invoke emitIntermediate(String key, String value) on
	 * the MapReduce object passed to the start method.
	 * 
	 * @param key   The key. For the map phase, this is the input file name.
	 * @param lines a set of lines to iterate over. Each line can contain many
	 *              words.
	 * @throws IOException
	 */
	public void map(String key, Iterable<String> lines) throws IOException;

	/**
	 * The reduce method should invoke emitOutput(String key, String value) on the
	 * MapReduce object passed to the start method.
	 * 
	 * @param key    the key
	 * @param values a set of values to iterate over. These values have to be
	 *               reduced.
	 * @throws IOException
	 */
	public void reduce(String key, Iterable<String> values) throws IOException;

	/**
	 * The postProcess method should invoke emitFinal(String key, String value) on
	 * the MapReduce object passed to the start method.
	 * 
	 * @param key    the key
	 * @param value1 first of the two values that should be combined
	 * @param value2 second of the two values that should be combined
	 * @throws IOException
	 */
	public void postProcess(String key, String value1, String value2) throws IOException;
}
