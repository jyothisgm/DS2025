package ds.pa2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class implementing the MapReduce functionality and I/O. This
 * class contains the main method that is started by the user. The actual
 * MapRedeuce application is loaded dynamically based on the class name the user
 * provided. Your distributed version should call Mapreduce.configure on all
 * nodes, and then call userApplication.start() only on the coordinator node,
 * while the clients run new code that you have to write.
 */
public class MapReduce {
    private static Logger logger = LoggerFactory.getLogger(MapReduce.class);
    private Config config;

    public Config getConfig() {
		return config;
	}

	private static MapReduceApplication userApplication;

	private int currentIntermediateFileNumber = 0;
    private int currentIntermediateSize = 0;

	// For Mapping files as Tuples
    // private final ArrayList<Tuple> currentIntermediateTuples = new ArrayList<Tuple>();

	// For Mapping files as Hashmaps
	private final HashMap<String,Integer> currentIntermediateHashmap = new HashMap<String,Integer>(); 

    private int currentOutputFileNumber = 0;
    private int currentOutputSize = 0;
    private final ArrayList<Tuple> currentOutputTuples = new ArrayList<Tuple>();

	private long batchKey;
	public String name = Util.getMyHostname();
	public String type = Util.amICoordinator() ? "COORDINATOR" : "WORKER";

    /**
     * A treeMap is sorted. This is very useful for checking whether the output is
     * correct. Some steps can be non-deterministic, so the order of key/value pairs
     * may be different. Sorting resolves this, and ensures we can simply do a diff.
     */
    private final TreeMap<String, String> postProcessingMap = new TreeMap<String, String>();

    /**
     * The entry point for MapReduce. The arguments are: 0: the class name of the
     * user application (e.g., WordCount) 1: the input directory 2: the intermediate
     * directory 3. the output directory
     * 
     * The reason we do this complicated class loading is that this will be needed
     * in a distributed setting. There, all nodes will run this main method, and you
     * have to make sure that the coordinator starts the MapReduce, while the other
     * nodes are clients, and ask the coordinator for work.
     * 
     * @param args
     */
    public static void main(String[] args) {
	if (args.length != 4) {
	    System.err.println(
		    "Usage: java ds.pa2.MapReduce <application class name> <input dir> <intermediate dir> <output dir>");
	    System.exit(1);
	}

	// We will now find the Class containing the user program, based on the name
	// provided by the user.
	// Next, we instantiate that class and call the default constructor (without
	// arguments)
	try {
	    Class<?> appClass = Class.forName(args[0]);
	    userApplication = (MapReduceApplication) appClass.getDeclaredConstructor().newInstance();
	} catch (Exception e) {
	    System.err.println("Cannot find the constructor of the main class " + args[0] + ": " + e);
	    System.exit(1);
	}

	// We now create a new MapReduce object that will handle all IO and scheduling.
	// Finally, we call start on the user application, to give control to the
	// application.
	// It should create a configuration and invoke mapReduce() when it has
	// initialized its data structures.
	logger.info("starting user application: " + args[0]);
	try {
	    userApplication.configure(new MapReduce(), args);
	    userApplication.start();
	} catch (IllegalArgumentException | IOException | InterruptedException e) {
	    System.err.println("An error occurred: " + e.getMessage());
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    /**
     * The user application mast first pass a configuration to MapRecude with this
     * method, before the actual MapReduce starts with invoking mapReduce(). The
     * Config object defines the input, intermediate and output files, as well as
     * the maximum sizes for those files. Feel free to add additional configuration
     * parameters to Config if you need them in the distributed setting. This method
     * validates the user supplied configuration.
     * 
     * @param config
     * @throws IOException
     * @throws IllegalArgumentException
     */
    public void configure(Config config) throws IOException, IllegalArgumentException {
	this.config = config;

	// validate config
	checkExists(config.getInputDir());
	checkExists(config.getIntermediateDir());
	checkExists(config.getOutputDir());
    }

    /**
     * This is the "main" stating point for the real MapReduce. The user application
     * must first have configured the MapReduce instance with a Config object via
     * the configure() method. This method starts the map, reduce, and
     * postProcessing phases.
     * 
     * @throws IOException
     * 
     */
    public void mapReduce(StubInterface server) throws IOException, IllegalArgumentException {
	if (config == null) {
	    throw new IllegalArgumentException(
		    "You forgot to provide a Config to mapReduce via the method configure()");
	}

	long numOfMaps = 0, mapTime = 0, numOfReduce = 0, reduceTime = 0, startTime, start, elapsed;
	logger.info(this.type + ": " + this.name + " | starting map phase");
	startTime = System.nanoTime();
	boolean isMapPhaseDone = false;
	while (!isMapPhaseDone) {
		start = System.nanoTime();
		logger.debug(this.type + ": " + this.name + " | asking for work");
		HashMap<Long, List<String>> job = server.getMapJob(this.name);
		if(!job.isEmpty()) {
			runMapPhase(job);
			logger.debug(this.type + ": " + this.name + " | contacting server");
			server.mapJobCompleted(this.name);
			logger.debug(this.type + ": " + this.name + " | notified server");

			elapsed = (System.nanoTime() - start) / 1000000;
			mapTime += elapsed;
			numOfMaps++;
			logger.info(this.type + ": " + this.name + " | map job took: " + elapsed + " milliseconds" + numOfMaps + " operations");
		} else {
			try {
				logger.debug(this.type + ": " + this.name + " | sleeping after map for 100 milliseconds");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		isMapPhaseDone = server.isMapPhaseDone();
		logger.debug(this.type + ": " + this.name + " | Is map phase done?: " + isMapPhaseDone);
	}

	logger.info(this.type + ": " + this.name + " | map phase took:" + mapTime + " milliseconds.");
	logger.info(this.type + ": " + this.name + " | starting reduce phase");
	boolean isReducePhaseDone = false;

	while (!isReducePhaseDone){
		logger.debug(this.type + ": " + this.name + " | asking for work");
		HashMap<Long, List<String>> job = server.getReduceJob(this.name);
		start = System.nanoTime();
		if (!job.isEmpty()){
			runReducePhase(job);
			logger.debug(this.type + ": " + this.name + " | contacting server");
			server.reduceJobCompleted(this.name);
			logger.debug(this.type + ": " + this.name + " | notified server");

			elapsed = (System.nanoTime() - start) / 1000000;
			reduceTime += elapsed;
			numOfReduce++;
			logger.info(this.type + ": " + this.name + " | Reduce job took: " + elapsed + " milliseconds.");
		} else {
			try { 
				logger.debug(this.type + ": " + this.name + " | sleeping after reduce for 100 milliseconds");
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		isReducePhaseDone = server.isReducePhaseDone();
	}
	elapsed = (System.nanoTime() - startTime) / 1000000;
	logger.info(this.type + ": " + this.name + " | reduce phase took:" + reduceTime + " milliseconds for " + numOfReduce + " operations");
	logger.info(this.type + ": " + this.name + " | Total application time is: " + elapsed + " milliseconds. Did " + numOfMaps +
				" map operations and " + numOfReduce + " reduce operations");
	boolean isPostProcessingOver = server.isPostProcessingDone();
	while (!isPostProcessingOver){
		try {
			logger.trace(this.name + " | waiting for postprocessing to finish for 1 sec");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		isPostProcessingOver = server.isPostProcessingDone();
	}
	System.out.printf("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d\n", Util.getNrClients(), this.name, "Worker", numOfMaps, mapTime, numOfReduce, reduceTime, 0, 0, elapsed);
    }

    /**
     * The user map method should call this whenever it want to emit a
     * key,value-pair to the intermediate output file. Note that this method will
     * buffer the key/value-pairs until the user-defined intermediate chunk size is
     * reached. Only then will it actually perform the real write. This is done for
     * efficiency reasons.
     * 
     * @param key   The key to be stored. Note that it is OK to have duplicate keys.
     *              They will be reduced later.
     * @param value The value accompanying this key
     * @throws IOException
     */
	public void emitIntermediate(String key, String value) throws IOException {
		// For Mapping files as Tuples
		// currentIntermediateSize += key.length() + value.length();

		// For Mapping files as Hashmaps
		if (!currentIntermediateHashmap.containsKey(key)){
		currentIntermediateSize += key.length() + 32;

		if (currentIntermediateSize >= config.getIntermediateChunkSize()) {
			flushIntermediate();

		}
		}
		// For Mapping files as Tuples
		// currentIntermediateTuples.add(new Tuple(key, value));

		// For Mapping files as Hashmaps
		currentIntermediateHashmap.merge(key,Integer.parseInt(value),Integer::sum);
	}

    /**
     * The user reduce method should call this whenever it want to emit a
     * key,value-pair to an output file. Note that this method will buffer the
     * key/value-pairs until the user-defined output chunk size is reached. Only
     * then will it actually perform the real write. This is done for efficiency
     * reasons.
     * 
     * @param key   The key to be stored. Note that it is OK to have duplicate keys.
     * @param value The value accompanying this key
     * @throws IOException
     */
    public void emitOutput(String key, String value) throws IOException {
	currentOutputSize += key.length() + value.length();
	if (currentOutputSize >= config.getOutputChunkSize()) {
	    flushOutput();
	}

	currentOutputTuples.add(new Tuple(key, value));
    }

    /**
     * The user postProcess method should call this whenever it want to emit a
     * key,value-pair to the final output file. Note that this method will buffer
     * all key/value-pairs in memory until the application is done. Only then will
     * it actually perform the real write. This is done for efficiency reasons.
     * 
     * @param key   The key to be stored.
     * @param value The value accompanying this key
     * @throws IOException
     */
    public void emitFinal(String key, String value) throws IOException {
	postProcessingMap.put(key, value);
    }

    /**
     * Used for validation of the user supplied config.
     * 
     * @param dir a directory for input, intermediate results, or output.
     * @throws FileNotFoundException
     */
    private void checkExists(String dir) throws FileNotFoundException {
	File f = new File(dir);

	if (!f.exists()) {
	    throw new FileNotFoundException("cannot find " + dir);
	}
	if (!f.isDirectory()) {
	    throw new FileNotFoundException(dir + " is not a directory");
	}
    }

    /**
     * Helper method that splits a line into a key and value string, stored in a
     * Tuple.
     * 
     * @param line
     * @return
     */
    public static Tuple splitLine(String line) {
	String[] splitLine = line.split("\\|"); // the output and final files have the structure "key|value"
	if (splitLine.length != 2) { // sanity check
	    logger.warn("Illegal line in intermediate file: " + line);
	    return null;
	}
	return new Tuple(splitLine[0], splitLine[1]);
    }

    /**
     * This method runs the entire map phase. It iterates over the input files, and
     * calls the user defined map operation for every line in the file. Note that we
     * assume that only text files are used.
     * 
     * @throws IOException
     */
    private void runMapPhase(HashMap<Long, List<String>> job) throws IOException {
	HashMap.Entry<Long, List<String>> entry = job.entrySet().iterator().next();
    this.batchKey = entry.getKey();
	this.currentIntermediateFileNumber = 0;
	logger.info(this.type + ": " + this.name + " | starting map job on: " + entry.getValue().size() + " books.");
	for (String filePath : entry.getValue()) {
	    logger.trace(this.type + ": " + this.name + " | mapping file: " + filePath);
		File file = new File(filePath);
	    try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			List<String> allLines = Arrays.asList(in.lines().toArray(String[]::new));
			userApplication.map(file.getName(), allLines);
	    }
	}

	flushIntermediate();
    }

    /**
     * This method runs the entire reduce phase. It iterates over the intermediate
     * files, and calls the user defined reduce operation for every line in the
     * file. Note that we assume that only text files are used.
     * 
     * @throws IOException
     */
    private void runReducePhase(HashMap<Long, List<String>> job) throws IOException {
	HashMap.Entry<Long, List<String>> entry = job.entrySet().iterator().next();
    this.batchKey = entry.getKey();
	this.currentOutputFileNumber = 0;
	logger.info(this.type + ": " + this.name + " | starting reduce job on: " + entry.getValue().size() + " books.");
	for (String filePath : entry.getValue()) {
		logger.trace(this.type + ": " + this.name + " | reducing file: " + filePath);
		File file = new File(filePath);
		reduceFile(file);
		}
	flushOutput();
    }

    /**
     * Intermediate files contain one key, value-pair per line. An intermediate file
     * may contain the same key many times. We group all values with the same key
     * together, and then call reduce once for that key. It is also guaranteed that
     * the keys for each individual intermediate file will be processed in order
     * (i.e., they are sorted).
     * 
     * @param f the File to process
     * @throws IOException
     */
    private void reduceFile(File f) throws IOException {
	TreeMap<String, List<String>> map = new TreeMap<String, List<String>>();

	try (BufferedReader in = new BufferedReader(new FileReader(f))) {
	    List<String> allLines = Arrays.asList(in.lines().toArray(String[]::new));

	    for (String line : allLines) {
		Tuple t = splitLine(line);
		if (t == null)
		    continue;

		List<String> values = (List<String>) map.remove(t.key);
		if (values == null) {
		    values = new ArrayList<String>();
		}
		values.add(t.value);
		map.put(t.key, values);
	    }

	    for (String key : map.keySet()) {
		List<String> values = map.get(key);
		userApplication.reduce(key, values);
	    }
	}
    }

    /**
     * Flushes a intermediate results that were kept in memory to disk.
     * 
     * @throws IOException
     */
    private void flushIntermediate() throws IOException {
	String fileName = config.getIntermediateDir() + File.separator + this.batchKey+ "_intermediate" + currentIntermediateFileNumber
		+ ".txt";
	logger.trace(this.type + ": " + this.name + " | Flush intermediate: " + fileName);
	currentIntermediateFileNumber++;

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
		// For Mapping files as Tuples
	    // for (int i = 0; i < currentIntermediateTuples.size(); i++) {
		// 	Tuple t = currentIntermediateTuples.get(i);
		// 	bw.write(t.key + "|" + t.value + "\n");
		// }

		// For Mapping files as Hashmaps
		for (HashMap.Entry<String,Integer> entry: currentIntermediateHashmap.entrySet()) {
			bw.write(entry.getKey() + "|" + entry.getValue() + "\n");
	    }
	} finally {
		// For Mapping files as Tuples
	    // currentIntermediateTuples.clear();

		// For Mapping files as Hashmaps
		currentIntermediateHashmap.clear();

	    currentIntermediateSize = 0;
	}
    }

    /**
     * Flushes a output results that were kept in memory to disk. It tries to do
     * that atomically to make the implementation of fault tolerance easier. This is
     * implemented by first writing to a temporary file, and then renaming that to
     * the correct output with an ATOMIC move.
     * 
     * @throws IOException
     */
    private void flushOutput() throws IOException {
	int myOutputFileNumber = currentOutputFileNumber;
	currentOutputFileNumber++;

	String fileName = config.getOutputDir() + File.separator + this.batchKey + "_output" + myOutputFileNumber + ".txt";
	File out = new File(fileName);

	File tmpFile = File.createTempFile("tempOutput_" + myOutputFileNumber, ".txt", new File(config.getOutputDir()));
	logger.debug(this.type + ": " + this.name + " | Flush output: " + myOutputFileNumber + ": using temp file: " + tmpFile.getName());

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
	    for (int i = 0; i < currentOutputTuples.size(); i++) {
		Tuple t = currentOutputTuples.get(i);
		bw.write(t.key + "|" + t.value + "\n");
	    }
	} finally {
	    currentOutputTuples.clear();
	    currentOutputSize = 0;
	}

	logger.debug(this.type + ": " + this.name + " | Rename temp output " + tmpFile.getName() + " to output: " + fileName);

	java.nio.file.Files.move(tmpFile.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * This method reads all output files, does one final reduce step, sorts the
     * output, and writes the final result to disk. We need this additional step to
     * validate the output.
     * 
     * @throws IOException
     */
    public int runPostProcessingPhase() throws IOException {
	File[] files = new File(config.getOutputDir()).listFiles();
	logger.info(this.type + ": " + this.name + " | post processing files: " + files.length);
	for (File f : files) {
	    logger.trace(this.type + ": " + this.name + " | post processing file: " + f);

	    try (BufferedReader in = new BufferedReader(new FileReader(f))) {
		String[] allLines = in.lines().toArray(String[]::new);

		for (String line : allLines) {
		    Tuple t = splitLine(line);
		    if (t == null)
			continue;

		    String old = postProcessingMap.get(t.key);
		    if (old == null) {
			postProcessingMap.put(t.key, t.value);
		    } else {
			userApplication.postProcess(t.key, old, t.value);
		    }
		}
	    }
	}

	String fileName = config.getOutputDir() + File.separator + "final-output.txt";
	File out = new File(fileName);

	File tmpFile = File.createTempFile("finalOutput_", ".txt", new File(config.getOutputDir()));
	logger.debug(this.type + ": " + this.name + " | writing final output");

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
	    for (String key : postProcessingMap.keySet()) {
		bw.write(key + "|" + postProcessingMap.get(key) + "\n");
	    }
	}

	logger.info(this.type + ": " + this.name + " | Rename temp output " + tmpFile.getName() + " to output: " + fileName);
	java.nio.file.Files.move(tmpFile.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE);
	return files.length;
    }

    /**
     * A helper class that wraps a key/value-pair in a tuple
     */
    public final static class Tuple implements Comparable<Tuple> {
	final String key;
	final String value;

	public Tuple(String key, String value) {
	    this.key = key;
	    this.value = value;
	}

	@Override
	public int compareTo(Tuple o) {
	    return key.compareTo(o.key);
	}

	@Override
	public int hashCode() {
	    return key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
	    return key.equals(obj);
	}
    }
}
