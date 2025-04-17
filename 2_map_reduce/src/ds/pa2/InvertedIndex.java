package ds.pa2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an example MapReduce user application. This application is used as an
 * example in the original MapReduce paper as well. It creates an inverted index
 * of all word occurrences in a set of output files. Note that all IO,
 * communication, load balancing and distributed operations are completely
 * hidden from the user. Still, this can scale to many thousands of files and
 * compute nodes.
 */
public final class InvertedIndex implements MapReduceApplication {
	private Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
	private MapReduce mr;
	private Config config;

	private String[] fileNames;

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

		// Create a config object with the configuration, and then run MapReduce.
		config = new Config(args[1], args[2], args[3]);
		config.setIntermediateChunkSize(1 * 1024 * 1024);
		config.setOutputChunkSize(1 * 1024 * 1024);
		mr.configure(config);

		fileNames = new File(args[1]).list();
		if(fileNames == null) {
			throw new IOException("File " + args[1] + "not found");
		}
		Arrays.sort(fileNames);
	}
		
	/**
	 * The MapReduce framework calls this start method ON THE COORDINATOR node when
	 * the application is started. All other nodes will run clients that ask the
	 * coordinator for work.
	 */
	@Override
	public void start() throws IllegalArgumentException, IOException {
		mr.mapReduce();

		// sort here to make validation easier
		sortOutput();
	}

	private void sortOutput() throws IOException, FileNotFoundException {
		logger.info("sorting output");
		String outputFileName = config.getOutputDir() + File.separator + "final-output.txt";
		String sortedFileName = config.getOutputDir() + File.separator + "sorted-final-output.txt";
		File outputFile = new File(outputFileName);
		File sortedFile = new File(sortedFileName);

		try (BufferedReader in = new BufferedReader(new FileReader(outputFile));
				BufferedWriter out = new BufferedWriter(new FileWriter(sortedFile))) {
			while (true) {
				String line = in.readLine();
				if (line == null)
					break;

				MapReduce.Tuple t = MapReduce.splitLine(line);
				String[] values = t.value.split(":");
				int[] intVals = new int[values.length];
				for (int i = 0; i < values.length; i++) {
					intVals[i] = Integer.parseInt(values[i]);
				}
				Arrays.sort(intVals);

				out.write(t.key + "|");
				for (int i = 0; i < intVals.length; i++) {
					out.write("" + intVals[i]);
					if (i != intVals.length - 1) {
						out.write(":");
					}
				}
				out.write("\n");
			}
		}
		outputFile.delete();
		sortedFile.renameTo(outputFile);

		logger.info("all done");
	}

	/**
	 * We use file indices instead of file names to save space and to reduce parsing
	 * time.
	 * 
	 * @param fileName
	 * @return
	 */
	private int fileIndex(String fileName) {
		for (int i = 0; i < fileNames.length; i++) {
			if (fileNames[i].equals(fileName))
				return i;
		}

		return -1;
	}

	@Override
	public void map(String key, Iterable<String> lines) throws IOException {
		TreeMap<String, String> map = new TreeMap<String, String>();

		for (String line : lines) {
			String[] words = line.split("[^a-zA-Z]+");
			for (String word : words) {
				if (word.length() > 0) {
					map.put(word.toLowerCase(), key);
				}
			}
		}

		String keyIndex = "" + fileIndex(key);
		for (String word : map.keySet()) {
			// important: the key is the word, the value is the filename (=the input key)
			mr.emitIntermediate(word, keyIndex);
		}
	}

	@Override
	public void reduce(String key, Iterable<String> values) throws IOException {
		ArrayList<String> fileList = new ArrayList<String>();

		for (String value : values) {
			if (!fileList.contains(value)) {
				fileList.add(value); // simply filter out duplicate file names
			}
		}

		String output = "";
		for (int i = 0; i < fileList.size(); i++) {
			output += fileList.get(i);
			if (i != fileList.size() - 1) {
				output += ":";
			}
		}

		mr.emitOutput(key, output);
	}

	@Override
	public void postProcess(String key, String value1, String value2) throws IOException {
		mr.emitFinal(key, value1 + ":" + value2);
	}
}