package ds.pa1;

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
	static final int ARRAY_SIZE = 1024 * 1024; // size of double arrays for the optional bonus assignment
	static final int HASH_MAP_SIZE = 100000; // size of the HashMaps sent to the server for the optional bonus
												// assignment

	static final Logger logger = LoggerFactory.getLogger(Client.class);

	/**
	 * Utility function to create a HashMap. It can be used for the optional bonus
	 * assigment.
	 * 
	 * @return A new HashMap with HASH_MAP_SIZE (key, value) pairs.
	 */
	private HashMap<String, String> createHashMap() {
		HashMap<String, String> result = new HashMap<String, String>();

		for (int i = 0; i < HASH_MAP_SIZE; i++) {
			result.put("" + i, String.format("%.10f", Math.random()));
		}

		return result;
	}

	/**
	 * Utility function to compute the storage size of a HashMap. It can be used for
	 * the optional bonus assignment, to compute the throughput achieve when
	 * transferring complex data to an rmi server.
	 * 
	 * @return A new HashMap with HASH_MAP_SIZE (key, value) pairs.
	 */
	private long getHashMapSize(HashMap<String, String> h) {
		int size = 0;
		for (String k : h.keySet()) {
			size += k.length() + h.get(k).length();
		}
		size *= 2; // characters are two bytes in Java
		return size;
	}

	private ServerInterface connect() {
		String host = Util.getCoordinatorHostname();
		System.err.println("client connecting to " + host);

		// TODO implement
		Registry registry = LocateRegistry.getRegistry(host);
		try {
			ServerInterface clientStub = (ServerInterface) registry.lookup("NumServer");
			return clientStub;
		} catch (RemoteException re) {
			return null;
		}

	}

	public void start() {
		logger.info("Client started on host " + Util.getMyHostname() + " master = "
				+ Util.getCoordinatorHostname());

		ServerInterface serverInterface = connect();
		if (Objects.isNull(serverInterface)) {
			System.exit(1);
		}

		// Warmup
		for (int i = 0; i < 100; i++) {
			serverInterface.getSequenceNumber();
		}
		serverInterface.barrier();
		long start = System.nanoTime();
		for (int i = 0; i < ClientServer.getNrSequenceNumberCalls(); i++) {
			serverInterface.getSequenceNumber();
		}
		long end = System.nanoTime();

		serverInterface.setDone(end - start);

		logger.info("Client " + Util.getMyHostname() + " done");
	}

	public static void main(String[] args) {
		new Client().start();
	}
}
