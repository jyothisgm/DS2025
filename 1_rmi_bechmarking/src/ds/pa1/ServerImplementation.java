package ds.pa1;

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

	public ServerImplementation() {
	}

	/**
	 * Gets a new unique sequence number. This method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 * @return the sequence number
	 */
	@Override
	public int getSequenceNumber() {
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
	public void setDone(long nanosSequenceNumners) {
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
		// TODO implement!
	}

	// The methods below are only called by the server, and never by a client

	protected long getAggregatedTimeSequenceNumbers() {
		return aggregatedTimeSequenceNumbers;
	}
}
