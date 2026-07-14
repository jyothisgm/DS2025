package ds.pa1;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Code for the client side. This will run n-1 times, on ranks 1 .. n (where n
 * is the number of nodes passed to srun
 * 
 * TODO: You have to modify and extend this file.
 */
public class Client {
	static final int ARRAY_LEN = 200000; // size of double arrays for the optional bonus assignment
	static final int HASH_MAP_LEN = 100000; // size of the HashMaps sent to the server for the optional bonus
    static final int NUM_ARRAYS = 10;
    static final int NUM_HASH = 10;
												// assignment

	static final Logger logger = LoggerFactory.getLogger(Client.class);

	/**
	 * Utility function to create a HashMap. It can be used for the optional bonus
	 * assigment.
	 * 
	 * @return A new HashMap with HASH_MAP_LEN (key, value) pairs.
	 */
	private HashMap<String, String> createHashMap() {
		HashMap<String, String> result = new HashMap<String, String>();

		for (int i = 0; i < HASH_MAP_LEN; i++) {
			result.put("" + i, String.format("%.10f", Math.random()));
		}

		return result;
	}

	/**
	 * Utility function to compute the storage size of a HashMap. It can be used for
	 * the optional bonus assignment, to compute the throughput achieve when
	 * transferring complex data to an rmi server.
	 * 
	 * @return A new HashMap with HASH_MAP_LEN (key, value) pairs.
	 */
	private long getHashMapSize(HashMap<String, String> h) {
		int size = 0;
		for (String k : h.keySet()) {
			size += k.length() + h.get(k).length();
		}
		// size *= 2; // characters are two bytes in Java BUT WHEN THE If a character c is in the range \u0001 through \u007f, it is represented by one byte
		return size;
	}

	private ServerInterface connect() throws RemoteException, NotBoundException {
		String host = Util.getCoordinatorHostname();
		System.err.println("client connecting to " + host);
		logger.info("Client connecting to " + host + " : Client on host " + Util.getMyHostname());
		Registry registry = LocateRegistry.getRegistry(host, 1099);
		
		logger.debug("Client connected to " + host + " : Client on host " + Util.getMyHostname());
		ServerInterface clientStub = (ServerInterface) registry.lookup("NumServer");
		logger.debug("Server stub recieved for " + host + " : Client on host " + Util.getMyHostname());
		return clientStub;

	}

	public void start() throws RemoteException, NotBoundException, InterruptedException {
		logger.info("Client started on host " + Util.getMyHostname() + " master = "
				+ Util.getCoordinatorHostname());

		ServerInterface serverInterface = null;
		while (Objects.isNull(serverInterface)) {
			try {
				serverInterface = connect();
			} catch (RemoteException e) {
				logger.warn(e.getMessage(), e);
				Thread.sleep(100);
			}
		}

		// Warmup
		for (int i = 0; i < 100; i++) {
			serverInterface.getSequenceNumber();
		}
		logger.info("Warmup Done: Client on host " + Util.getMyHostname());
		serverInterface.barrier();
		long start = System.nanoTime();

		logger.info("Barrier open at " + start + ": Client on host " + Util.getMyHostname());
		int sequenceNumber = 0;
		for (int i = 0; i < ClientServer.getNrSequenceNumberCalls(); i++) {
			sequenceNumber = serverInterface.getSequenceNumber();
		}
		long end = System.nanoTime();

		logger.info("Last Sequence Number " + sequenceNumber + " recieved at " + end +
			": Client on host " + Util.getMyHostname());

		serverInterface.setDone(end - start);
		logger.info("Client " + Util.getMyHostname() + " done in " + (end - start) + " nanosecond(s)");

		// Send Double Array to Server
		double []arr = new double[ARRAY_LEN];
		serverInterface.barrier();

		start = System.nanoTime();
		logger.info("Barrier open for Large array at " + start + ": Client on host " + Util.getMyHostname());

		for (int i = 0; i < NUM_ARRAYS ; i++) {
		serverInterface.sendLargeArray(arr);
        }

		end = System.nanoTime();
		logger.info("Large array sent at " + end + ": Client on host " + Util.getMyHostname());

		serverInterface.setDoneArray(end - start);
		logger.info("Client " + Util.getMyHostname() + " done in " + (end - start) + " nanosecond(s)");

		// Send Complex Object to Server
		HashMap<String, String> result = createHashMap();
		long objSize = getHashMapSize(result);
		serverInterface.barrier();

		start = System.nanoTime();
		logger.info("Barrier open for Complex Object at " + start + ": Client on host " + Util.getMyHostname());

		for (int i = 0; i < NUM_HASH ; i++) {
		serverInterface.sendComplexObject(result);
        }

		end = System.nanoTime();
		logger.info("Complex Object sent at " + end + ": Client on host " + Util.getMyHostname());

		serverInterface.setDoneHash(end - start, objSize);
		logger.info("Client " + Util.getMyHostname() + " done in " + (end - start) + " nanosecond(s)");
	}

	public static void main(String[] args) {
		try {
			new Client().start();
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			logger.error("Client " + Util.getMyHostname() + " : " + e.getMessage(), e);
		}
	}
}
