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
	static final int ARRAY_SIZE = 1024 * 1024; // size of double arrays for the optional bonus assignment

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

			reg.bind("NumServer", serverStub);
			logger.info("The server should now be visible on the registry...");
			while (serverImpl.getClientsDone() < Util.getNrClients()) {
				System.out.println("SLEEPING");
				Thread.sleep(5000);
			}

			System.out.println("NOT SLEEPING");
			double aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			long totalCalls = Util.getNrClients() * ClientServer.getNrSequenceNumberCalls();
			double microsPerCall = aggregatedTime / 100000; // LOCAL TESTING
			// double microsPerCall = aggregatedTime / totalCalls;
			double latency = microsPerCall / 2;
			double throughput = Integer.SIZE * 1_000_000.0 / microsPerCall;
			System.out.printf("Time per getSequenceNumber call with %s and %d calls = %.3f microseconds\n",
					nrClientsString(), totalCalls, microsPerCall);
			logger.info(String.format("Time per getSequenceNumber call with %s and %d calls = %.3f microseconds",
					nrClientsString(), totalCalls, microsPerCall));

			System.out.printf("Latency for %s and %d calls = %.3f microseconds\n",
					nrClientsString(), totalCalls, latency);
			logger.info(String.format("Latency for %s and %d calls = %.3f microseconds",
					nrClientsString(), totalCalls, latency));

			System.out.printf("End-to-End Throughput for %s and %d calls = %.3f bps\n",
					nrClientsString(), totalCalls, throughput);
			logger.info(String.format("End-to-End Throughput for %s and %d calls = %.3f bps",
					nrClientsString(), totalCalls, throughput));

			System.out.println("NClients,TotalCalls,Time,MicrosPerCall,Latency,Throughput");
			System.out.printf("%s,%d,%.5f,%.5f,%.5f,%.5f\n",
					Util.getNrClients(), totalCalls, aggregatedTime, microsPerCall, latency, throughput);

			// Recieve Large Array
			while (serverImpl.getClientsDone() < Util.getNrClients() * 2) {
				Thread.sleep(5000);
			}

			aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			latency = aggregatedTime / 2;
			throughput = Double.SIZE * 1024 * 1024 * 1_000_000.0 / aggregatedTime;
			System.out.printf("Time for Large Array transfer with %s = %.3f microseconds\n",
					nrClientsString(), microsPerCall);
			logger.info(String.format("Time for Large Array transfer with %s = %.3f microseconds\n",
					nrClientsString(), microsPerCall));

			System.out.printf("Latency for Large Array transfer with %s = %.3f microseconds\n",
					nrClientsString(), latency);
			logger.info(String.format("Latency for Large Array transfer with %s = %.3f microseconds",
					nrClientsString(), latency));

			System.out.printf("End-to-End Throughput for Large Array transfer with %s = %.3f bps\n",
					nrClientsString(), throughput);
			logger.info(String.format("End-to-End Throughput for Large Array transfer with %s = %.3f bps",
					nrClientsString(), throughput));

			System.out.println("NClients,TotalCalls,Time,MicrosPerCall,Latency,Throughput");
			System.out.printf("%s,%d,%.5f,%.5f,%.5f,%.5f\n",
					Util.getNrClients(), totalCalls, aggregatedTime, aggregatedTime, latency, throughput);

			while (serverImpl.getClientsDone() < Util.getNrClients() * 3) {
				Thread.sleep(5000);
			}

			// Recieve Complex Object
			aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			latency = aggregatedTime / 2;
			throughput = serverImpl.getObjectSize() * 8 * 1_000_000.0 / aggregatedTime; // Convert to bits
			System.out.printf("Time for Complex Object transfer with %s = %.3f microseconds\n",
					nrClientsString(), microsPerCall);
			logger.info(String.format("Time for Complex Object transfer with %s = %.3f microseconds\n",
					nrClientsString(), microsPerCall));

			System.out.printf("Latency for Complex Object transfer with %s = %.3f microseconds\n",
					nrClientsString(), latency);
			logger.info(String.format("Latency for Complex Object transfer with %s = %.3f microseconds",
					nrClientsString(), latency));

			System.out.printf("End-to-End Throughput for Complex Object transfer with %s = %.3f bps\n",
					nrClientsString(), throughput);
			logger.info(String.format("End-to-End Throughput for Complex Object transfer with %s = %.3f bps",
					nrClientsString(), throughput));

			System.out.println("NClients,TotalCalls,Time,MicrosPerCall,Latency,Throughput");
			System.out.printf("%s,%d,%.5f,%.5f,%.5f,%.5f\n",
					Util.getNrClients(), totalCalls, aggregatedTime, aggregatedTime, latency, throughput);

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
