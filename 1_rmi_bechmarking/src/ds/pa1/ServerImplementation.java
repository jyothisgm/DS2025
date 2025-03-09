package ds.pa1;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The implementation of the remote object (on the server side).
 * TODO: YOU HAVE TO MODIFY AND EXTEND THIS FILE
 */
public class ServerImplementation implements ServerInterface {
	static final Logger logger = LoggerFactory.getLogger(ServerImplementation.class);

	private int sequenceNumber = 0;
	private long aggregatedTimeSequenceNumbers = 0;
	private long aggregatedTimeArray = 0;
	private long aggregatedTimeHash = 0;
	private int clientsDone = 0;
	private long objectSize = 0;

	public long getObjectSize() {
		return objectSize;
	}

	public void setObjectSize(long objectSize) {
		this.objectSize = objectSize;
	}

	public int getClientsDone() {
		return clientsDone;
	}

	private final AtomicInteger numClientsInBarrier = new AtomicInteger(0);

	public ServerImplementation() {
	}

	/**
	 * Gets a new unique sequence number. This method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 * @return the sequence number
	 */
	@Override
	public synchronized int getSequenceNumber() {
		// synchronized means that only one thread will be able to run it but is this
		// what we want? this limits throughput
		sequenceNumber++;
		return sequenceNumber;
	}

	/**
	 * Recieve a large array.
	 *
	 * @param data A large 2 dimensional array of doubles
	 *
	 */
	public void sendLargeArray(double[][] data) throws RemoteException {
		logger.info("Received large array of size: " + data.length + "x" + (data.length > 0 ? data[0].length : 0));
	}

	/**
	 * Recieve a HashMap.
	 *
	 * @param data A hashmap of Complex object of unknown size
	 */
	public void sendComplexObject(HashMap<String, String> data) throws RemoteException {
		logger.info("Received complex object with " + data.size() + " entries.");
	}

	/**
	 * By calling this method, the clients inform the server that they are done. The
	 * pass some timing statistics to the server, so the server can compute
	 * latencies and throughputs. his method MUST be implemented to get a passing
	 * grade for this assignment.
	 * 
	 * @param nanosSequenceNumbers The total time (in nanoseconds) spent in the
	 *                             getSequenceNumber calls.
	 */
	@Override
	public synchronized void setDone(long nanosSequenceNumners) {
		aggregatedTimeSequenceNumbers += nanosSequenceNumners;
		this.clientsDone += 1;
	}
	@Override
	public synchronized void setDoneArray(long nanosSequenceNumners) {
		aggregatedTimeArray += nanosSequenceNumners;
		this.clientsDone += 1;
	}

	/**
	 * By calling this method, the clients inform the server that they are done. The
	 * pass some timing statistics to the server, so the server can compute
	 * latencies and throughputs. This is a function overloaded for sending size of the complex array as well.
	 *
	 * @param nanosSequenceNumbers The total time (in nanoseconds) spent in the
	 *                             getSequenceNumber calls.
	 * @param nanosSequenceNumbers The total size of the complex object
	 */
	@Override
	public synchronized void setDoneHash(long nanosSequenceNumners, long size) {
		aggregatedTimeHash += nanosSequenceNumners;
		this.clientsDone += 1;
		this.objectSize += size;
	}

	/**
	 * A barrier: this method blocks until all clients have called this method.
	 * Then, the clients are released, and the barrier is reset. You can call the
	 * barrier method more than once. his method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 */
	@Override
	public void barrier() {
		// synchronized is not needed because here we use atomic ints

		int currentClientsInBarrier = numClientsInBarrier.incrementAndGet();
		logger.info("Clients in barrier increased to:" + currentClientsInBarrier);
		int numClients = Util.getNrClients();
		while (currentClientsInBarrier % numClients != 0) {
			try {
				currentClientsInBarrier = numClientsInBarrier.get();
				logger.debug(currentClientsInBarrier + " clients waiting");
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		logger.info("DOORS OPEN");
		return;
	}

	// The methods below are only called by the server, and never by a client
	protected long getAggregatedTimeSequenceNumbers() {
		return aggregatedTimeSequenceNumbers;
	}

	protected long getAggregatedArray() {
		return aggregatedTimeArray;
	}

	protected long getAggregatedTimeHash() {
		return aggregatedTimeHash;
	}
}
