package ds.pa2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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
	private static MapReduceApplication userApplication;

	private int currentIntermediateFileNumber = 0;
	private int currentIntermediateSize = 0;
	private final ArrayList<Tuple> currentIntermediateTuples = new ArrayList<Tuple>();

	private int currentOutputFileNumber = 0;
	private int currentOutputSize = 0;
	private final ArrayList<Tuple> currentOutputTuples = new ArrayList<Tuple>();

	private Queue<List<String>> mapQueue = new LinkedList<>();
	public synchronized Queue<List<String>> getMapQueue() {
		return mapQueue;
	}

	private HashMap<String, List<String>> mapTakenList = new HashMap<>();
	public synchronized List<String> getMapJob(String key) {
		List<String> mapTaken = mapQueue.poll();
		this.mapTakenList.put(key, mapTaken);
		return mapTaken;
	}

	public synchronized HashMap<String, List<String>> getMapTakenList() {
		return mapTakenList;
	}

	public synchronized void removeFromMapTakenList(String key) {
		mapTakenList.remove(key);
	}

	private Queue<List<String>> reduceQueue = new LinkedList<>();
	public synchronized Queue<List<String>> getReduceQueue() {
		return reduceQueue;
	}

	private HashMap<String, List<String>> reduceTakenList = new HashMap<>();
	public synchronized void removeFromReduceTakenList(String key) {
		mapTakenList.remove(key);
	}

	public synchronized List<String> getReduceJob(String key) {
		List<String> reduceTaken = reduceQueue.poll();
		this.reduceTakenList.put(key, reduceTaken);
		return reduceTaken;
	}

	public synchronized HashMap<String, List<String>> getReduceTakenList() {
		return reduceTakenList;
	}

	private int BATCH_SIZE = 32;

	/**
	 * A treeMap is sorted. This is very useful for checking whether the output is
	 * correct. Some steps can be non-deterministic, so the order of key/value pairs
	 * may be different. Sorting resolves this, and ensures we can simply do a diff.
	 */
	private final TreeMap<String, String> postProcessingMap = new TreeMap<String, String>();

	public static MapReduceApplication registerStub(MapReduceApplication userApplication) throws RemoteException, AlreadyBoundException{
		MapReduceApplication serverStub = (MapReduceApplication) UnicastRemoteObject.exportObject(userApplication, 1099);
		Registry reg = LocateRegistry.createRegistry(1099);

		reg.bind("NumServer", serverStub);
		logger.info("The server should now be visible on the registry...");
		return serverStub;
	}

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

		if(Util.amICoordinator()) {
			try {
				// I am the Master
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
				userApplication = registerStub(userApplication);
				// userApplication.start();
				// TODO: Run Server till Map and Reduce are Done
				while (!userApplication.getMr().getMapQueue().isEmpty() || !userApplication.getMr().getMapTakenList().isEmpty() ||
						!userApplication.getMr().getReduceQueue().isEmpty() || !userApplication.getMr().getReduceTakenList().isEmpty()) {
					try {
						Thread.sleep(5000);
						// TODO: HeartBeat
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			} catch (IllegalArgumentException | IOException e) {
				System.err.println("An error occurred: " + e.getMessage());
				e.printStackTrace();
				System.exit(1);
			} catch (AlreadyBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			// I am a Worker
			try {
				String host = Util.getCoordinatorHostname();
				System.err.println("client connecting to " + host);
				logger.info("Client connecting to " + host + " : Client on host " + Util.getMyHostname());

				Registry registry = LocateRegistry.getRegistry(host, 1099);
				logger.debug("Client connected to " + host + " : Client on host " + Util.getMyHostname());
				MapReduceApplication clientStub = (MapReduceApplication) registry.lookup("NumServer");
				logger.debug("Server stub recieved for " + host + " : Client on host " + Util.getMyHostname());
				clientStub.start(Util.getMyHostname());
			} catch (IOException | NotBoundException | IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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
    public void mapReduce(String worker) throws IOException, IllegalArgumentException {
	if (config == null) {
	    throw new IllegalArgumentException(
		    "You forgot to provide a Config to mapReduce via the method configure()");
	}
	long totalTime = 0;
	long start;
	long elapsed;
	while (!userApplication.getMr().getMapQueue().isEmpty() ||
			!userApplication.getMr().getMapTakenList().isEmpty()) {
		logger.info("starting map phase");
		start = System.nanoTime();
		List<String> mapJob = getMapJob(worker);
		if (!mapJob.isEmpty()) {
			runMapPhase(mapJob);
			removeFromMapTakenList(worker);
			elapsed = (System.nanoTime() - start) / 1000000;
			totalTime += elapsed;
			logger.info("map phase took: " + elapsed + " milliseconds.");
		}
	}
	logger.info("starting reduce phase");
	start = System.nanoTime();
	runReducePhase();
	elapsed = (System.nanoTime() - start) / 1000000;
	totalTime += elapsed;
	logger.info("reduce phase took: " + elapsed + " milliseconds.");

	logger.info("starting sequential post processing phase");
	start = System.nanoTime();
	runPostProcessingPhase();
	elapsed = (System.nanoTime() - start) / 1000000;
	totalTime += elapsed;
	logger.info("post processing phase took: " + elapsed + " milliseconds.");

	logger.info("total application time is: " + totalTime + " milliseconds.");
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
	currentIntermediateSize += key.length() + value.length();
	if (currentIntermediateSize >= config.getIntermediateChunkSize()) {
	    flushIntermediate();
	}

	currentIntermediateTuples.add(new Tuple(key, value));
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

	public void generateMapQueue () {
		File[] files = new File(config.getInputDir()).listFiles();
        if (files == null || files.length == 0) {
            System.out.println("No files found in directory: " + config.getInputDir());
            return;
        }
        List<String> batch = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                batch.add(file.getAbsolutePath());
                if (batch.size() == BATCH_SIZE) {
                    mapQueue.offer(new ArrayList<>(batch));
                    batch.clear();
                }
            }
        }
        // Add any remaining files that didn't complete a full batch
        if (!batch.isEmpty()) {
            mapQueue.offer(batch);
        }

        System.out.println("Batched " + files.length + " files into " + mapQueue.size() + " batches.");
    }

    /**
     * This method runs the entire map phase. It iterates over the input files, and
     * calls the user defined map operation for every line in the file. Note that we
     * assume that only text files are used.
     * 
     * @throws IOException
     */
    private void runMapPhase(List<String> batchFiles) throws IOException {
		for (String filePath : batchFiles) {
			logger.debug("mapping file: " + filePath);
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
    private void runReducePhase() throws IOException {
	File[] files = new File(config.getIntermediateDir()).listFiles();
	for (File f : files) {
	    logger.debug("reducing file: " + f);
	    reduceFile(f);
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
	String fileName = config.getIntermediateDir() + File.separator + "intermediate" + currentIntermediateFileNumber
		+ ".txt";
	logger.debug("Flush intermediate: " + fileName);
	currentIntermediateFileNumber++;

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(fileName))) {
	    for (int i = 0; i < currentIntermediateTuples.size(); i++) {
		Tuple t = currentIntermediateTuples.get(i);
		bw.write(t.key + "|" + t.value + "\n");
	    }
	} finally {
	    currentIntermediateTuples.clear();
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

	String fileName = config.getOutputDir() + File.separator + "output" + myOutputFileNumber + ".txt";
	File out = new File(fileName);

	File tmpFile = File.createTempFile("tempOutput_" + myOutputFileNumber, ".txt", new File(config.getOutputDir()));
	logger.debug("Flush output: " + myOutputFileNumber + ": using temp file: " + tmpFile.getName());

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
	    for (int i = 0; i < currentOutputTuples.size(); i++) {
		Tuple t = currentOutputTuples.get(i);
		bw.write(t.key + "|" + t.value + "\n");
	    }
	} finally {
	    currentOutputTuples.clear();
	    currentOutputSize = 0;
	}

	logger.debug("Rename temp output " + tmpFile.getName() + " to output: " + fileName);

	java.nio.file.Files.move(tmpFile.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE);
    }

    /**
     * This method reads all output files, does one final reduce step, sorts the
     * output, and writes the final result to disk. We need this additional step to
     * validate the output.
     * 
     * @throws IOException
     */
    private void runPostProcessingPhase() throws IOException {
	File[] files = new File(config.getOutputDir()).listFiles();

	for (File f : files) {
	    logger.debug("post processing file: " + f);

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
	logger.debug("writing final output");

	try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpFile))) {
	    for (String key : postProcessingMap.keySet()) {
		bw.write(key + "|" + postProcessingMap.get(key) + "\n");
	    }
	}

	logger.debug("Rename temp output " + tmpFile.getName() + " to output: " + fileName);
	java.nio.file.Files.move(tmpFile.toPath(), out.toPath(), StandardCopyOption.ATOMIC_MOVE);
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
