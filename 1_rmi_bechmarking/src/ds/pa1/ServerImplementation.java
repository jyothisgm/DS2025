package ds.pa1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
		/* THANOS: this might run concurently. Need to make threadsafe */
		sequenceNumber++;
		return sequenceNumber;
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
		// THANOS: this might run concurently. Need to make threadsafe
		aggregatedTimeSequenceNumbers += nanosSequenceNumners;
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
		// the barrier
		// THANOS: this might run concurently. Need to make threadsafe
		int currentClientsInBarrier = numClientsInBarrier.incrementAndGet();
		System.out.println("Counters in barrier increased to:" + currentClientsInBarrier);
		int numClients = Util.getNrClients();
		while (currentClientsInBarrier < numClients) {
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}
		System.out.println("DOORS OPEN");
		return;
		// TODO implement!
	}

	// The methods below are only called by the server, and never by a client

	protected long getAggregatedTimeSequenceNumbers() {
		return aggregatedTimeSequenceNumbers;
	}
}
