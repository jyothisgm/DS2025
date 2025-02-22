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

	private ServerInterface connect() throws RemoteException, NotBoundException {
		String host = Util.getCoordinatorHostname();
		logger.info("client connecting to " + host);

		Registry registry = LocateRegistry.getRegistry(host);
		logger.info("client connected to " + host);
		ServerInterface clientStub = (ServerInterface) registry.lookup("NumServer");
		logger.info("client got server stub to " + host);
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
			}

			Thread.sleep(1000);
		}

		// Warmup
		for (int i = 0; i < 100; i++) {
			serverInterface.getSequenceNumber();
		}
		System.out.println("Warmup done");
		serverInterface.barrier();
		long start = System.nanoTime();
		for (int i = 0; i < ClientServer.getNrSequenceNumberCalls(); i++) {
			// for (int i = 0; i < 100000; i++) { // LOCAL TESTING
			int num = serverInterface.getSequenceNumber();
			System.out.println(num);
		}
		long end = System.nanoTime();

		serverInterface.setDone(end - start);

		logger.info("Client " + Util.getMyHostname() + " done");
	}

	public static void main(String[] args) {
		try {
			new Client().start();
		} catch (RemoteException | NotBoundException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
