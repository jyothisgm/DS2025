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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
	private static StubImpl stubImpl = new StubImpl();

	private int currentIntermediateFileNumber = 0;
    private int currentIntermediateSize = 0;

	// For Mapping files as Tuples
    private final ArrayList<Tuple> currentIntermediateTuples = new ArrayList<Tuple>();

	// For Mapping files as Hashmaps
	// private final HashMap<String,Integer> currentIntermediateHashmap = new HashMap<String,Integer>();

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
		MapReduce mr = new MapReduce();
	    userApplication.configure(mr, args);

		stubImpl = new StubImpl();
		Registry reg = LocateRegistry.createRegistry(1099);
		StubInterface stub = (StubInterface) UnicastRemoteObject.exportObject(stubImpl, 1099);

		try {
			reg.bind(mr.name, stub);
		} catch (RemoteException | AlreadyBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		logger.info(mr.type + ": " + mr.name + " | The node should now be visible on the registry...");
	
		if (Util.amICoordinator()) {
			logger.info(mr.type + ": " + mr.name + " | Starting Coordinator");
			System.out.println("Starting Coordinator");
			userApplication.start();

			stubImpl.setPostProcessingDone();
			List<String> killedClients = new ArrayList<>();
			logger.info(mr.type + ": " + mr.name + " | Terminating Clients");
			while (stubImpl.getClientStubs().size() > 0) {
				Iterator<Map.Entry<String, StubInterface>> iterator = stubImpl.getClientStubs().entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, StubInterface> entry = iterator.next();
					try {
						entry.getValue().heartBeat(false);
					} catch (Exception e) {
						killedClients.add(entry.getKey());
						logger.info(mr.type + ": " + mr.name + " | Terminated Client " + entry.getKey());
					}
				}
				for (String key : killedClients) {
					stubImpl.removeClient(key);
				}
			}
			logger.info(mr.type + ": " + mr.name + " | Terminated Server");
			System.exit(0);
		} else {
			runClient(mr);
			System.exit(0);
		}
	} catch (IllegalArgumentException | IOException | NotBoundException e) {
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

	private static void runClient(MapReduce mr) throws NotBoundException, IOException {
		logger.info(mr.type + ": " + mr.name + " | Client started and thinks master is: " + Util.getCoordinatorHostname());

		StubInterface server = null;
		while (Objects.isNull(server)) {
			try {
				String host = Util.getCoordinatorHostname();
				logger.info(mr.type + ": " + mr.name + " | Client connecting to " + host);

				Registry registry = LocateRegistry.getRegistry(host, 1099);
				logger.debug(mr.type + ": " + mr.name + " | Client connected to " + host);
				server = (StubInterface) registry.lookup(host);
				logger.debug(mr.type + ": " + mr.name + " | Server stub received for " + host);
				
			} catch (RemoteException | NotBoundException e) {
				logger.warn(mr.type + ": " + mr.name + " | " + e.getMessage());
				try {
					Thread.sleep(50);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		long numOfMaps = 0, mapTime = 0, numOfReduce = 0, reduceTime = 0, startTime, start, elapsed;
		logger.info(mr.type + ": " + mr.name + " | Starting Map Phase");
		startTime = System.nanoTime();
		boolean isMapPhaseDone = false;
		while (!isMapPhaseDone) {
			start = System.nanoTime();
			logger.debug(mr.type + ": " + mr.name + " | Asking for work");
			HashMap<Long, List<String>> job = server.getMapJob(mr.name);
			if(!job.isEmpty()) {
				mr.runMapPhase(job);
				logger.debug(mr.type + ": " + mr.name + " | Contacting Server");
				server.mapJobCompleted(mr.name);
				logger.debug(mr.type + ": " + mr.name + " | Notified Server");

				elapsed = (System.nanoTime() - start) / 1000000;
				mapTime += elapsed;
				numOfMaps++;
				logger.info(mr.type + ": " + mr.name + " | Map Job Took: " + elapsed + " milliseconds.");
			} else {
				try {
					logger.debug(mr.type + ": " + mr.name + " | sleeping after map for 50 milliseconds");
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			isMapPhaseDone = server.isMapPhaseDone();
			logger.debug(mr.type + ": " + mr.name + " | Is Map Phase Done?: " + isMapPhaseDone);
		}

		logger.info(mr.type + ": " + mr.name + " | Map Phase Took: " + mapTime + " milliseconds for " + numOfMaps + " map operations");
		logger.info(mr.type + ": " + mr.name + " | Starting Reduce Phase");
		boolean isReducePhaseDone = false;

		while (!isReducePhaseDone){
			logger.debug(mr.type + ": " + mr.name + " | Asking for work");
			HashMap<Long, List<String>> job = server.getReduceJob(mr.name);
			start = System.nanoTime();
			if (!job.isEmpty()){
				mr.runReducePhase(job);
				logger.debug(mr.type + ": " + mr.name + " | Contacting Server");
				server.reduceJobCompleted(mr.name);
				logger.debug(mr.type + ": " + mr.name + " | Notified Server");

				elapsed = (System.nanoTime() - start) / 1000000;
				reduceTime += elapsed;
				numOfReduce++;
				logger.info(mr.type + ": " + mr.name + " | Reduce Job Took: " + elapsed + " milliseconds.");
			} else {
				try {
					logger.debug(mr.type + ": " + mr.name + " | sleeping after reduce for 50 milliseconds");
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			isReducePhaseDone = server.isReducePhaseDone();
			logger.debug(mr.type + ": " + mr.name + " | Is Reduce Phase Done?: " + isMapPhaseDone);
		}
		elapsed = (System.nanoTime() - startTime) / 1000000;
		logger.info(mr.type + ": " + mr.name + " | Reduce Phase Took: " + reduceTime + " milliseconds for " + numOfReduce + " reduce operations");
		logger.info(mr.type + ": " + mr.name + " | Total application time is: " + elapsed + " milliseconds. Did " + numOfMaps +
					" Map operations and " + numOfReduce + " Reduce operations");
		boolean isPostProcessingDone = server.isPostProcessingDone();
		while (!isPostProcessingDone){
			try {
				logger.trace(mr.name + " | waiting for PostProcessing to finish for 1 sec");
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			isPostProcessingDone = server.isPostProcessingDone();
		}
		System.out.printf("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d\n", Util.getNrClients(), mr.name, "Worker", numOfMaps, mapTime, numOfReduce, reduceTime, 0, 0, elapsed);
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
    public void mapReduce() throws IOException, IllegalArgumentException {
		if (config == null) {
			throw new IllegalArgumentException(
				"You forgot to provide a Config to mapReduce via the method configure()");
		}

		long startTime = System.nanoTime();
		long start, elapsed;
		long numOfMaps, mapTime, numOfReduce, reduceTime;

		logger.info(this.type + ": " + this.name + " | Populate Map Queue");
		System.out.println("Populate Map Queue");

		start = System.nanoTime();
		numOfMaps = stubImpl.populateMapQueue(this.getConfig());
		elapsed = (System.nanoTime() - start) / 1000000;

		logger.info(this.type + ": " + this.name + " | Map Queue Populated. Took: " + elapsed + " milliseconds.");
		System.out.println("Map Queue Populated. Took: " + elapsed + " milliseconds.");
		logger.info(this.type + ": " + this.name + " | Map Phase Started");
		System.out.println("Map Phase Started");
		start = System.nanoTime();
		while (!stubImpl.isTimePopulateReduce()) {
			try {
				List<String> failedClients = new ArrayList<>();
				logger.trace(this.type + ": " + this.name + " | Sleeping for map phase");
				Thread.sleep(50);
				Iterator<Map.Entry<String, StubInterface>> iterator = stubImpl.getClientStubs().entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, StubInterface> entry = iterator.next();
					boolean end = false;
					try {
						// Testing Random Worker Failures
						// if (Math.random() < 0.01 && stubImpl.getClientStubs().size() > 4 && failedClients.size() < 1) {
						// 	end = true;
						// }
						entry.getValue().heartBeat(end);
						logger.trace(this.type + ": " + this.name + " | Heartbeat " + entry.getKey());
					} catch (Exception e) {
						failedClients.add(entry.getKey());
						logger.warn(this.type + ": " + this.name + " | Heartbeat Failed " + entry.getKey());
					}
				}
				for (String key : failedClients) {
					stubImpl.removeClient(key);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		mapTime = (System.nanoTime() - start) / 1000000;
		logger.info(this.type + ": " + this.name + " | Map Phase Done. Took: " + mapTime + " milliseconds.");
		System.out.println("Map Phase Done. Took: " + mapTime + " milliseconds.");

		logger.info(this.type + ": " + this.name + " | Populate Reduce Queue");
		System.out.println("Populate Reduce Queue");
		start = System.nanoTime();
		numOfReduce = stubImpl.populateReduceQueue(this.getConfig());
		elapsed = (System.nanoTime() - start) / 1000000;
		logger.info(this.type + ": " + this.name + " | Reduce Queue Populated. Took: " + elapsed + " milliseconds.");
		System.out.println("Reduce Queue Populated. Took: " + elapsed + " milliseconds.");

		// Set map phase over after Reduce Job is populated
		stubImpl.setMapPhaseDone();
		logger.info(this.type + ": " + this.name + " | Reduce Phase Started");
		System.out.println("Reduce Phase Started");
		start = System.nanoTime();
		while(!stubImpl.isTimePostProcessing()){
			try {
				List<String> failedClients = new ArrayList<>();
				logger.trace(this.type + ": " + this.name + " | Sleeping for reduce phase");
				Thread.sleep(50);
				Iterator<Map.Entry<String, StubInterface>> iterator = stubImpl.getClientStubs().entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, StubInterface> entry = iterator.next();
					boolean end = false;
					try {
						// Testing Random Worker Failures
						// if (Math.random() < 0.01 && stubImpl.getClientStubs().size() > 2 && failedClients.size() < 1) {
						// 	end = true;
						// }
						entry.getValue().heartBeat(end);
						logger.trace(this.type + ": " + this.name + " | Heartbeat " + entry.getKey());
					} catch (Exception e) {
						failedClients.add(entry.getKey());
						logger.warn(this.type + ": " + this.name + " | Heartbeat Failed " + entry.getKey());
					}
				}
				for (String key : failedClients) {
					stubImpl.removeClient(key);
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		reduceTime = (System.nanoTime() - start) / 1000000;
		logger.info(this.type + ": " + this.name + " | Reduce Phase Done. Took: " + reduceTime + " milliseconds.");
		System.out.println("Reduce Phase Done. Took: " + reduceTime + " milliseconds.");

		// Set reduce phase over
		stubImpl.setReducePhaseDone();

		logger.info(this.type + ": " + this.name + " | Post Processing Phase Started");
		System.out.println("Post Processing Phase Started");

		start = System.nanoTime();
		long numOut = this.runPostProcessingPhase();
		long postProcessTime = (System.nanoTime() - start) / 1000000;
		logger.info(this.type + ": " + this.name + " | Post Processing Phase Done. Took: " + postProcessTime + " milliseconds.");
		System.out.println("Post Processing Phase Done. Took: " + postProcessTime + " milliseconds.");

		elapsed = (System.nanoTime() - startTime) / 1000000;
		logger.info(this.type + ": " + this.name + " | Total time: " + elapsed + " milliseconds.");
		System.out.println("Total time: " + elapsed + " milliseconds.");

		System.out.println("NClients,Node,Type,NumOfMaps,MapTime,NumOfReduce,ReduceTime,NumOfOutputFiles,PostProcessingTime,TotalTime");
		System.out.printf("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d\n", Util.getNrClients(), this.name, "Coordinator",
				numOfMaps, mapTime, numOfReduce, reduceTime, numOut, postProcessTime, elapsed);

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
		currentIntermediateSize += key.length() + value.length();

		// For Mapping files as Hashmaps
		// if (!currentIntermediateHashmap.containsKey(key)){
		// currentIntermediateSize += key.length() + 32;

		if (currentIntermediateSize >= config.getIntermediateChunkSize()) {
			flushIntermediate();
		}
		// }
		// For Mapping files as Tuples
		currentIntermediateTuples.add(new Tuple(key, value));

		// For Mapping files as Hashmaps
		// currentIntermediateHashmap.merge(key,Integer.parseInt(value),Integer::sum);
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
	logger.info(this.type + ": " + this.name + " | Starting Map job on: " + entry.getValue().size() + " file(s).");
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
	logger.info(this.type + ": " + this.name + " | Starting Reduce job on: " + entry.getValue().size() + " file(s).");
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
	    for (int i = 0; i < currentIntermediateTuples.size(); i++) {
			Tuple t = currentIntermediateTuples.get(i);
			bw.write(t.key + "|" + t.value + "\n");
		}

		// For Mapping files as Hashmaps
		// for (HashMap.Entry<String,Integer> entry: currentIntermediateHashmap.entrySet()) {
		// 	bw.write(entry.getKey() + "|" + entry.getValue() + "\n");
	    // }
	} finally {
		// For Mapping files as Tuples
	    currentIntermediateTuples.clear();

		// For Mapping files as Hashmaps
		// currentIntermediateHashmap.clear();

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
