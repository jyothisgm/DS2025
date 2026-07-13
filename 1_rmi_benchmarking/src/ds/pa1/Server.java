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
	static final int ARRAY_LEN = 200000; // size of double arrays for the optional bonus assignment
	static final int NUM_ARRAYS = 10;
	static final int NUM_HASH = 10;

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
			String ibIP = Util.getMyIP();
			System.setProperty("java.rmi.server.hostname", ibIP);
			System.out.println("java.rmi.server.hostname: " + System.getProperty("java.rmi.server.hostname"));
			ServerInterface serverStub = (ServerInterface) UnicastRemoteObject.exportObject(serverImpl, 1199);
			Registry reg = LocateRegistry.createRegistry(1099);

			reg.bind("NumServer", serverStub);
			logger.info("The server should now be visible on the registry...");
			while (serverImpl.getClientsDone() < Util.getNrClients()) {
				Thread.sleep(5000);
			}

			double aggregatedTime = serverImpl.getAggregatedTimeSequenceNumbers() / 1000.0;
			long totalCalls = Util.getNrClients() * ClientServer.getNrSequenceNumberCalls();
			// long totalCalls = Util.getNrClients() * 100000; // LOCAL TESTING
			double microsPerCall = aggregatedTime / totalCalls;
			double latency = microsPerCall / 2;
			double num_avg_size_bits = Integer.SIZE;
			double throughput = totalCalls * num_avg_size_bits * 1_000_000.0 / aggregatedTime;
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

			System.out.println("NClients,Type,TotalCalls,Time,MicrosPerCall,Latency,Throughput,Size");
			System.out.printf("%s,%s,%d,%.5f,%.5f,%.5f,%.5f,%.0f\n",
					Util.getNrClients(), "Sequence", totalCalls, aggregatedTime, microsPerCall, latency, throughput,
					num_avg_size_bits/8);

			// Recieve Large Array
			while (serverImpl.getClientsDone() < Util.getNrClients() * 2) {
				Thread.sleep(5000);
			}
			aggregatedTime = serverImpl.getAggregatedArray() / 1000.0;
			totalCalls = Util.getNrClients() * NUM_ARRAYS;
			microsPerCall = aggregatedTime / totalCalls;
			latency = microsPerCall / 2;
			double arr_avg_size_bits = Double.SIZE * ARRAY_LEN ; // .SIZE already in bits
			throughput = totalCalls * arr_avg_size_bits * 1_000_000.0 / aggregatedTime;

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

			System.out.println("NClients,Type,TotalCalls,Time,MicrosPerCall,Latency,Throughput,Size");
			System.out.printf("%s,%s,%d,%.5f,%.5f,%.5f,%.5f,%.0f\n",
					Util.getNrClients(), "Vector", totalCalls, aggregatedTime, microsPerCall, latency, throughput,
					arr_avg_size_bits/8);

			while (serverImpl.getClientsDone() < Util.getNrClients() * 3) {
				Thread.sleep(5000);
			}

			// Recieve Complex Object
			aggregatedTime = serverImpl.getAggregatedTimeHash() / 1000.0;
			totalCalls = Util.getNrClients() * NUM_HASH;
			microsPerCall = aggregatedTime / totalCalls;
			latency = microsPerCall / 2;
			double hash_size_bits = serverImpl.getObjectSize() * 8 ; // bytes to bits
			double hash_avg_size_bits = hash_size_bits/totalCalls;
			throughput = hash_size_bits * 1_000_000.0 / aggregatedTime;

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

			System.out.println("NClients,Type,TotalCalls,Time,MicrosPerCall,Latency,Throughput,Size");
			System.out.printf("%s,%s,%d,%.5f,%.5f,%.5f,%.5f,%.0f\n",
					Util.getNrClients(), "Complex", totalCalls, aggregatedTime, microsPerCall, latency, throughput,
					hash_avg_size_bits/8);

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
