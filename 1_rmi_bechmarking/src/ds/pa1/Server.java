package ds.pa1;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main class of the server.
 * TODO YOU HAVE TO MODIFY AND EXTEND THIS FILE!
 */
public class Server {
	static final Logger logger = LoggerFactory.getLogger(Server.class);

	private String nrClientsString() {
		int nrClients = Util.getNrClients();
		if (nrClients == 1) {
			return "1 client";
		}
		return nrClients + " clients";
	}

	public void start() {
		logger.info("server: my hostname = " + Util.getMyHostname());

		try {
			ServerImplementation serverImpl = new ServerImplementation();

			// TODO Implement your code there that creates a remote object, and exposes it
			// to the world
			ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(serverImpl, 42);

			Registry reg = LocateRegistry.getRegistry();
			reg.bind("NumServer", serverStub);
			System.err.println("The server should now be visible on the registry...");

			double aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			long totalCalls = Util.getNrClients() * ClientServer.getNrSequenceNumberCalls();
			double microsPerCall = aggregatedTime / totalCalls;
			System.out.printf("Time per getSequenceNumber call with %s and %d calls = %.3f microseconds\n",
					nrClientsString(), totalCalls, microsPerCall);

			System.exit(0);
		} catch (Exception e) {
			System.err.println("Eception Occurred in the server: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		new Server().start();
	}
}
