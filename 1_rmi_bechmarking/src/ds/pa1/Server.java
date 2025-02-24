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
			ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(serverImpl, 1099);
			Registry reg = LocateRegistry.createRegistry(1099);
			// Registry reg = LocateRegistry.getRegistry();
			reg.bind("NumServer", serverStub);
			logger.info("The server should now be visible on the registry...");
			while (serverImpl.getClientsDone() < Util.getNrClients()) {
				Thread.sleep(5000);//
			}

			double aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			long totalCalls = Util.getNrClients() * ClientServer.getNrSequenceNumberCalls();
			double microsPerCall = aggregatedTime / totalCalls;
			// double microsPerCall = aggregatedTime / 100000; // LOCAL TESTING
			System.out.printf("Time per getSequenceNumber call with %s and %d calls = %.3f microseconds\n",
					nrClientsString(), totalCalls, microsPerCall);
			logger.info(String.format("Time per getSequenceNumber call with %s and %d calls = %.3f microseconds",
					nrClientsString(), totalCalls, microsPerCall));

			System.out.printf("Latency for %s and %d calls = %.3f microseconds\n",
					nrClientsString(), totalCalls, microsPerCall / 2);
			logger.info(String.format("Latency for %s and %d calls = %.3f microseconds",
					nrClientsString(), totalCalls, microsPerCall / 2));

			System.out.printf("End-to-End Throughput for %s and %d calls = %.3f bps\n",
					nrClientsString(), totalCalls, Integer.SIZE * 1_000_000.0 / microsPerCall);
			logger.info(String.format("End-to-End Throughput for %s and %d calls = %.3f bps",
					nrClientsString(), totalCalls, Integer.SIZE * 1_000_000.0 / microsPerCall));

			System.out.println("NClients,TotalCalls,Time,MicrosPerCall,Latency,Throughput");
			logger.info("NClients,TotalCalls,Time,MicrosPerCall,Latency,Throughput");
			System.out.printf("%s,%d,%.5f,%.5f,%.5f,%.5f\n",
					Util.getNrClients(), totalCalls, aggregatedTime, microsPerCall, microsPerCall / 2,
					Integer.SIZE * 1_000_000.0 / microsPerCall);
			logger.info(String.format("%s,%d,%.5f,%.5f,%.5f,%.5f\n", Util.getNrClients(), totalCalls, aggregatedTime,
					microsPerCall, microsPerCall / 2,
					Integer.SIZE * 1_000_000.0 / microsPerCall));
			System.exit(0);
		} catch (Exception e) {
			System.err.println("Eception Occurred in the server: " + e.getMessage());
			e.printStackTrace();

			logger.error("Exception Occurred in the server: " + e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		new Server().start();
	}
}
