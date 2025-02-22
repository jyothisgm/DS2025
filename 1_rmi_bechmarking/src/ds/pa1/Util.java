package ds.pa1;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

public final class Util {
	private static ArrayList<String> nodeList = internalGetHostnames();

	/**
	 * 
	 * @return a list of Strings that contains all hostnames of the nodes who
	 *         participate in this run, including the master.
	 */
	private static ArrayList<String> internalGetHostnames() {
		ArrayList<String> result = new ArrayList<String>();

		String tmp = System.getenv("SLURM_NODELIST");
		if (tmp == null) {
			result.add(getMyHostname());
			return result;
		}

		String prefix = tmp.split("\\[")[0];
		String nodeExpresssion = tmp.split("\\[")[1];
		nodeExpresssion = nodeExpresssion.substring(0, nodeExpresssion.length() - 1);

		String[] elements = nodeExpresssion.split(",");
		for (String e : elements) {
			if (e.contains("-")) { // this is a list of nodes
				String startString = e.split("-")[0];
				String endString = e.split("-")[1];
				int start = Integer.parseInt(startString);
				int end = Integer.parseInt(endString);

				for (int i = start; i <= end; i++) {
					result.add(prefix + String.format("%3d", i));
				}
			} else { // this is a single node
				result.add(prefix + e);
			}
		}

		return result;
	}

	/**
	 * 
	 * @return a list of Strings that contains all hostnames of the nodes who
	 *         participate in this run, including the master.
	 */

	public static ArrayList<String> getHostnames() {
		return nodeList;
	}

	/**
	 * 
	 * @return true if the current machine is the coordinator.
	 */
	public static boolean amICoordinator() {
		return getMyHostname().equals(getCoordinatorHostname());
	}

	/**
	 * 
	 * @return the hostname of the coordinator node (the server).
	 */
	public static String getCoordinatorHostname() {
		return nodeList.get(0);
	}

	/**
	 * 
	 * @return The total number of machines participating in this run, including the
	 *         coordinator.
	 */
	public static int getNrHosts() {
		return nodeList.size();
	}

	/**
	 * 
	 * @return The total number of client machines participating in this run,
	 *         excluding the
	 *         coordinator (i.e., the total number of machines -1).
	 */
	public static int getNrClients() {
		return 3;
		// return nodeList.size() - 1;
	}

	/**
	 * 
	 * @return The IP address of the local machine
	 */
	public static String getMyIP() {
		String result = null;
		try {
			result = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.err.println("Error while getting my own IP address: " + e);
			System.exit(1);
		}
		return result;
	}

	/**
	 * 
	 * @return The hostname of the local machine
	 */
	public static String getMyHostname() {
		String result = null;
		try {
			result = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			System.err.println("Error while getting my own IP address: " + e);
			System.exit(1);
		}
		return result;
	}
}
