package ds.pa2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

	private StubInterface connect() throws RemoteException, NotBoundException {
		String host = Util.getCoordinatorHostname();
		logger.info(this.mr.type + ": " + this.mr.name + " | Client connecting to " + host);

		Registry registry = LocateRegistry.getRegistry(host, 1099);
		logger.debug(this.mr.type + ": " + this.mr.name + " | Client connected to " + host);
		StubInterface server = (StubInterface) registry.lookup(host);
		logger.debug(this.mr.type + ": " + this.mr.name + " | Server stub received for " + host);
		return server;
	}

	/**
	 * The MapReduce framework calls this start method ON THE COORDINATOR node when
	 * the application is started. All other nodes will run clients that ask the
	 * coordinator for work.
	 */
	@Override
	public void start() throws IllegalArgumentException, IOException {
		long numOfMaps, mapTime, numOfReduce, reduceTime;
		if (Util.amICoordinator()) {
			long startTime = System.nanoTime();
			long start, elapsed;

			logger.info(this.mr.type + ": " + this.mr.name + " | Starting Coordinator");
			System.out.println("Starting Coordinator");

			StubImpl serverImpl = new StubImpl();

			logger.info(this.mr.type + ": " + this.mr.name + " | Populate Map Queue");
			System.out.println("Populate Map Queue");

			start = System.nanoTime();
			numOfMaps = serverImpl.populateMapQueue(this.mr.getConfig());
			elapsed = (System.nanoTime() - start) / 1000000;

			logger.info(this.mr.type + ": " + this.mr.name + " | Map Queue Populated. Took: " + elapsed + " milliseconds.");
			System.out.println("Map Queue Populated. Took: " + elapsed + " milliseconds.");

			StubInterface serverStub = (StubInterface) UnicastRemoteObject.exportObject(serverImpl, 1099);
			Registry reg = LocateRegistry.createRegistry(1099);

			try {
				reg.bind(this.mr.name, serverStub);
			} catch (RemoteException | AlreadyBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info(this.mr.type + ": " + this.mr.name + " | The server node should now be visible on the registry...");
			logger.info(this.mr.type + ": " + this.mr.name + " | Map Phase Started");
			System.out.println("Map Phase Started");
			start = System.nanoTime();
			while (!serverImpl.isTimePopulateReduce()) {
				try {
					List<String> failedClients = new ArrayList<>();
					logger.trace(this.mr.type + ": " + this.mr.name + " | Sleeping for map phase");
					Thread.sleep(50);
					Iterator<Map.Entry<String, StubInterface>> iterator = serverImpl.getClientStubs().entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, StubInterface> entry = iterator.next();
						boolean end = false;
						try {
							// Testing Random Worker Failures
							// if (Math.random() < 0.01 && serverImpl.getClientStubs().size() > 4 && failedClients.size() < 1) {
							// 	end = true;
							// }
							entry.getValue().heartBeat(end);
							logger.trace(this.mr.type + ": " + this.mr.name + " | Heartbeat " + entry.getKey());
						} catch (Exception e) {
							failedClients.add(entry.getKey());
							logger.warn(this.mr.type + ": " + this.mr.name + " | Heartbeat Failed " + entry.getKey());
						}
					}
					for (String key : failedClients) {
						serverImpl.removeClient(key);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			mapTime = (System.nanoTime() - start) / 1000000;
			logger.info(this.mr.type + ": " + this.mr.name + " | Map Phase Done. Took: " + mapTime + " milliseconds.");
			System.out.println("Map Phase Done. Took: " + mapTime + " milliseconds.");

			logger.info(this.mr.type + ": " + this.mr.name + " | Populate Reduce Queue");
			System.out.println("Populate Reduce Queue");
			start = System.nanoTime();
			numOfReduce = serverImpl.populateReduceQueue(this.mr.getConfig());
			elapsed = (System.nanoTime() - start) / 1000000;
			logger.info(this.mr.type + ": " + this.mr.name + " | Reduce Queue Populated. Took: " + elapsed + " milliseconds.");
			System.out.println("Reduce Queue Populated. Took: " + elapsed + " milliseconds.");

			// Set map phase over after Reduce Job is populated
			serverImpl.setMapPhaseDone();
			logger.info(this.mr.type + ": " + this.mr.name + " | Reduce Phase Started");
			System.out.println("Reduce Phase Started");
			start = System.nanoTime();
			while(!serverImpl.isTimePostProcessing()){
				try {
					List<String> failedClients = new ArrayList<>();
					logger.trace(this.mr.type + ": " + this.mr.name + " | Sleeping for reduce phase");
					Thread.sleep(50);
					Iterator<Map.Entry<String, StubInterface>> iterator = serverImpl.getClientStubs().entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, StubInterface> entry = iterator.next();
						boolean end = false;
						try {
							// Testing Random Worker Failures
							// if (Math.random() < 0.01 && serverImpl.getClientStubs().size() > 2 && failedClients.size() < 1) {
							// 	end = true;
							// }
							entry.getValue().heartBeat(end);
							logger.trace(this.mr.type + ": " + this.mr.name + " | Heartbeat " + entry.getKey());
						} catch (Exception e) {
							failedClients.add(entry.getKey());
							logger.warn(this.mr.type + ": " + this.mr.name + " | Heartbeat Failed " + entry.getKey());
						}
					}
					for (String key : failedClients) {
						serverImpl.removeClient(key);
					}
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			reduceTime = (System.nanoTime() - start) / 1000000;
			logger.info(this.mr.type + ": " + this.mr.name + " | Reduce Phase Done. Took: " + reduceTime + " milliseconds.");
			System.out.println("Reduce Phase Done. Took: " + reduceTime + " milliseconds.");

			// Set reduce phase over
			serverImpl.setReducePhaseDone();

			logger.info(this.mr.type + ": " + this.mr.name + " | Post Processing Phase Started");
			System.out.println("Post Processing Phase Started");

			start = System.nanoTime();
			long numOut = this.mr.runPostProcessingPhase();
			// sort here to make validation easier
			sortOutput();
			long postProcessTime = (System.nanoTime() - start) / 1000000;
			logger.info(this.mr.type + ": " + this.mr.name + " | Post Processing Phase Done. Took: " + postProcessTime + " milliseconds.");
			System.out.println("Post Processing Phase Done. Took: " + postProcessTime + " milliseconds.");

			elapsed = (System.nanoTime() - startTime) / 1000000;
			logger.info(this.mr.type + ": " + this.mr.name + " | Total time: " + elapsed + " milliseconds.");
			System.out.println("Total time: " + elapsed + " milliseconds.");

			serverImpl.setPostProcessingDone();

			List<String> killedClients = new ArrayList<>();
			System.out.println("NClients,Node,Type,NumOfMaps,MapTime,NumOfReduce,ReduceTime,NumOfOutputFiles,PostProcessingTime,TotalTime");
			System.out.printf("%s,%s,%s,%d,%d,%d,%d,%d,%d,%d\n", Util.getNrClients(), this.mr.name, "Coordinator",
					numOfMaps, mapTime, numOfReduce, reduceTime, numOut, postProcessTime, elapsed);
			logger.info(this.mr.type + ": " + this.mr.name + " | Terminating Clients");
			while (serverImpl.getClientStubs().size() > 0) {
				Iterator<Map.Entry<String, StubInterface>> iterator = serverImpl.getClientStubs().entrySet().iterator();
				while (iterator.hasNext()) {
					Map.Entry<String, StubInterface> entry = iterator.next();
					try {
						entry.getValue().heartBeat(false);
					} catch (Exception e) {
						killedClients.add(entry.getKey());
						logger.info(this.mr.type + ": " + this.mr.name + " | Terminated Client " + entry.getKey());
					}
				}
				for (String key : killedClients) {
					serverImpl.removeClient(key);
				}
			}
			logger.info(this.mr.type + ": " + this.mr.name + " | Terminated Server");
			System.exit(0);
		} else {
			logger.info(this.mr.type + ": " + this.mr.name + " | Client started and thinks master is: " + Util.getCoordinatorHostname());

			StubInterface server = null;
			while (Objects.isNull(server)) {
				try {
					server = connect();
				} catch (RemoteException | NotBoundException e) {
					logger.warn(this.mr.type + ": " + this.mr.name + " | " + e.getMessage(), e);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			StubImpl clientImpl = new StubImpl();
			Registry reg = LocateRegistry.createRegistry(1099);
			StubInterface clientStub = (StubInterface) UnicastRemoteObject.exportObject(clientImpl, 1099);
			try {
				reg.bind(this.mr.name, clientStub);
			} catch (RemoteException | AlreadyBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mr.mapReduce(server);
			System.exit(0);
		}
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
