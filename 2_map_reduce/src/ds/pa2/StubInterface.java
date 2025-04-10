package ds.pa2;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;


/**
 * This is the interface defining the API of the remote object.
 */

public interface StubInterface extends Remote {
	/**
	 * A barrier: this method blocks until all clients have called this method.
	 * Then, the clients are released, and the barrier is reset. You can call the
	 * barrier method more than once. his method MUST be implemented to get a
	 * passing grade for this assignment.
	 *
	 */
	public void barrier() throws RemoteException;

	public boolean heartBeat(boolean end) throws RemoteException;

    public List<String> getMapJob(String hostname) throws RemoteException;

    public boolean isMapPhaseDone() throws RemoteException;

    public List<String> getReduceJob(String hostname) throws RemoteException;

    public boolean isReducePhaseDone() throws RemoteException;

    public void mapJobCompleted(String hostname) throws RemoteException;

    public void reduceJobCompleted(String hostname) throws RemoteException;

    public boolean isPostProcessingDone() throws RemoteException;
}
