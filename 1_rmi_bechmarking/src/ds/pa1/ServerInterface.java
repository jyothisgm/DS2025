package ds.pa1;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * TODO This is the interface defining the API of the remote object.
 * TODO YOU HAVE TO CHANGE THIS FILE!
 */

public interface ServerInterface extends Remote {
	/**
	 * Gets a new unique sequence number. This method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 * @return the sequence number
	 */
	public int getSequenceNumber() throws RemoteException;

	/**
	 * A barrier: this method blocks until all clients have called this method.
	 * Then, the clients are released, and the barrier is reset. You can call the
	 * barrier method more than once. his method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 */
	public void barrier() throws RemoteException;

	/**
	 * By calling this method, the clients inform the server that they are done. The
	 * pass some timing statistics to the server, so the server can compute
	 * latencies and throughputs. his method MUST be implemented to get a
	 * passing grade for this assignment.
	 * 
	 * @param nanosSequenceNumbers The total time (in nanoseconds) spent in the
	 *                             getSequenceNumber calls.
	 */
	public void setDone(long nanosSequenceNumbers) throws RemoteException;

	/**
	 * By calling this method, the clients inform the server that they are done. The
	 * pass some timing statistics to the server, so the server can compute
	 * latencies and throughputs. This is a function overloaded for sending size of the complex array as well.
	 *
	 * @param nanosSequenceNumbers The total time (in nanoseconds) spent in the
	 *                             getSequenceNumber calls.
	 * @param nanosSequenceNumbers The total size of the complex object
	 */
	public void setDone(long nanosSequenceNumbers, long size) throws RemoteException;

	/**
	 * Recieve a large array.
	 *
	 * @param data A large 2 dimensional array of doubles
	 *
	 */
	public void sendLargeArray(double[][] arr) throws RemoteException;

	/**
	 * Recieve a HashMap.
	 *
	 * @param data A hashmap of Complex object of unknown size
	 */
	public void sendComplexObject(HashMap<String, String> data) throws RemoteException;

}
