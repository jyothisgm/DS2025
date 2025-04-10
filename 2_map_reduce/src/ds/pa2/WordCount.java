package ds.pa2;

import java.io.IOException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Objects;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private static Logger logger = LoggerFactory.getLogger(WordCount.class);

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
	}

	private StubInterface connect() throws RemoteException, NotBoundException {
		String host = Util.getCoordinatorHostname();
		// System.err.println("client connecting to " + host);
		logger.info(Util.getMyHostname()+" | Client connecting to " + host);

		Registry registry = LocateRegistry.getRegistry(host, 1099);
		logger.debug(Util.getMyHostname()+" | Client connected to " + host);
		StubInterface server = (StubInterface) registry.lookup("NumServer");
		logger.debug(Util.getMyHostname()+" | Server stub received for " + host);
		return server;
	}

	/**
	 * The MapReduce framework calls this start method ON THE COORDINATOR node when
	 * the application is started. All other nodes will run clients that ask the
	 * coordinator for work.
	 */
	@Override
	public void start() throws IllegalArgumentException, IOException {
		if (Util.amICoordinator()) {
			StubImpl serverImpl = new StubImpl();
			serverImpl.populateMapQueue(this.mr.getConfig());
			StubInterface serverStub = (StubInterface) UnicastRemoteObject.exportObject(serverImpl, 1099);
			Registry reg = LocateRegistry.createRegistry(1099);

			try {
				reg.bind("NumServer", serverStub);
			} catch (RemoteException | AlreadyBoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.info(Util.getMyHostname()+" | The server node should now be visible on the registry...");
			while (!serverImpl.isTimePopulateReduce()) {
				try {
					logger.info(this.mr.name + " | Sleeping for map phase");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			serverImpl.populateReduceQeueue(this.mr.getConfig());
			while(!serverImpl.isReducePhaseOver()){
				try {
					logger.info(this.mr.name + " | Sleeping for reduce phase");
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			// perpaps have the coordinator do the postprocessing?
			//

		} else {
			logger.info(Util.getMyHostname() + " | Client started and thinks master is: " + Util.getCoordinatorHostname());

			StubInterface server = null;
			while (Objects.isNull(server)) {
				try {
					Thread.sleep(1000);
					server = connect();
				} catch (RemoteException | NotBoundException | InterruptedException e) {
					logger.warn(e.getMessage(), e);
					try {
						Thread.sleep(100);
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
			mr.mapReduce(server);
		}
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
}
