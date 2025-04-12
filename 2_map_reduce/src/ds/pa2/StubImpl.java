package ds.pa2;

import java.io.File;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Code for the client side. This will run n-1 times, on ranks 1 .. n (where n
 * is the number of nodes passed to srun
 *
 * TODO: You have to modify and extend this file.
 */
public class StubImpl implements StubInterface {
    private int batchNormalizer = 2;
    private boolean mapPhaseDone = false;
    private boolean reducePhaseDone = false;
    private boolean postProcessingDone = false;

    private String name = Util.getMyHostname();
    public String type = Util.amICoordinator() ? "COORDINATOR" : "WORKER";

    private static Logger logger = LoggerFactory.getLogger(WordCount.class);
    private ConcurrentHashMap<String, StubInterface> clientStubs = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, StubInterface> getClientStubs() {
        return clientStubs;
    }

    private Queue<HashMap<Long, List<String>>> mapQueue = new LinkedList<>();
    public synchronized Queue<HashMap<Long, List<String>>> getMapQueue() {
		return mapQueue;
	}

	private HashMap<String, HashMap<Long, List<String>>> mapTakenList = new HashMap<>();

    @Override
	public synchronized HashMap<Long, List<String>> getMapJob(String key) throws RemoteException {
        HashMap<Long, List<String>> mapTaken = new HashMap<>();
        try {
            if (!clientStubs.containsKey(key)) {
                Registry registry = LocateRegistry.getRegistry(key, 1099);
                logger.debug(this.type + ": " + this.name + " | Server connected to " + key);
                StubInterface client = (StubInterface) registry.lookup(key);
                logger.debug(this.type + ": " + this.name + " | Server got stub " + key);
                clientStubs.put(key, client);
            }
        } catch (NotBoundException e) {
            logger.error(this.type + ": " + this.name + " | Error " + e);
            e.printStackTrace();
        }
        if (!this.getMapQueue().isEmpty()) {
            mapTaken = mapQueue.poll();
            this.mapTakenList.put(key, mapTaken);
        }
		return mapTaken;
	}

	public synchronized HashMap<String, HashMap<Long, List<String>>> getMapTakenList() {
		return mapTakenList;
	}

    public void setMapPhaseDone() {
        this.mapPhaseDone = true;
    }

	public synchronized void removeFromMapTakenList(String key) {
		mapTakenList.remove(key);
	}

	private Queue<HashMap<Long, List<String>>> reduceQueue = new LinkedList<>();
	public Queue<HashMap<Long, List<String>>> getReduceQueue() {
		return reduceQueue;
	}

	private HashMap<String, HashMap<Long, List<String>>> reduceTakenList = new HashMap<>();
	public synchronized void removeFromReduceTakenList(String key) {
		reduceTakenList.remove(key);
	}

    @Override
	public synchronized HashMap<Long, List<String>> getReduceJob(String key) throws RemoteException{
        HashMap<Long, List<String>> reduceTaken = new HashMap<>();
        try {
            if (!clientStubs.containsKey(key)) {
                Registry registry = LocateRegistry.getRegistry(key, 1099);
                logger.debug(this.type + ": " + this.name + " | Server connected to " + key);
                StubInterface client = (StubInterface) registry.lookup(key);
                logger.debug(this.type + ": " + this.name + " | Server got stub " + key);
                clientStubs.put(key, client);
            }
        } catch (NotBoundException e) {
            logger.error(this.type + ": " + this.name + " | Error " + e);
            e.printStackTrace();
        }
        if (!this.getReduceQueue().isEmpty()) {
            reduceTaken = reduceQueue.poll();
            this.reduceTakenList.put(key, reduceTaken);
        }
		return reduceTaken;
	}

	public synchronized HashMap<String, HashMap<Long, List<String>>> getReduceTakenList() {
		return reduceTakenList;
	}

    public void setReducePhaseDone() {
        this.reducePhaseDone = true;
    }

    public synchronized void removeClient(String key) {
        clientStubs.remove(key);
        if (mapTakenList.containsKey(key)) {
            logger.debug(this.type + ": " + this.name + " | Client Map Failed " + key);
            mapQueue.offer(mapTakenList.get(key));
            mapTakenList.remove(key);
        }
        if (reduceTakenList.containsKey(key)) {
            logger.debug(this.type + ": " + this.name + " | Client Reduce Failed " + key);
            reduceQueue.offer(reduceTakenList.get(key));
            reduceTakenList.remove(key);
        }
    }

    @Override
    public boolean heartBeat(boolean end) throws RemoteException {
        if (end) {
            System.exit(0);
        }
        return true;
    }

    @Override
    public boolean isMapPhaseDone() throws RemoteException {
        return mapPhaseDone;
    }

    public boolean isTimePopulateReduce() {
        if (!this.getMapQueue().isEmpty() || !this.getMapTakenList().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isReducePhaseDone() throws RemoteException {
        return reducePhaseDone;
    }

    public boolean isTimePostProcessing() {
        if (!this.getReduceQueue().isEmpty() || !this.getReduceTakenList().isEmpty()) {
            return false;
        }
        return true;
    }

    public boolean isPostProcessingDone() throws RemoteException{
        return this.postProcessingDone;
    }

    public void setPostProcessingDone() {
        this.postProcessingDone = true;
    }

    public int populateMapQueue(Config config) {
		File[] files = new File(config.getInputDir()).listFiles();
        logger.info(this.type + ": " + this.name + " | Populating Map Queue...");
        if (files == null || files.length == 0) {
            logger.warn(this.type + ": " + this.name + " | No files found in directory: " + config.getInputDir());
            return 0;
        }
        List<String> batch = new ArrayList<>();
        long batchNum = 1;
        int batchSize = (files.length + Util.getNrClients() * batchNormalizer - 1) / (Util.getNrClients() * batchNormalizer);
        for (File file : files) {
            if (file.isFile()) {
                batch.add(file.getAbsolutePath());
                if (batch.size() == batchSize) {
                    HashMap<Long, List<String>> batchMap = new HashMap<>();
                    batchMap.put(batchNum, new ArrayList<>(batch));
                    mapQueue.offer(batchMap);
                    batch.clear();
                    batchNum++;
                }
            } else {
                logger.warn(this.type + ": " + this.name + " | ignoring "+ file.getAbsolutePath());
            }
        }
        // Add any remaining files that didn't complete a full batch
        if (!batch.isEmpty()) {
            HashMap<Long, List<String>> batchMap = new HashMap<>();
            batchMap.put(batchNum, new ArrayList<>(batch));
            mapQueue.offer(batchMap);
            batch.clear();
        }
        System.out.println("Batched " + files.length + " files into " + mapQueue.size() + " batches.");
        logger.info(this.type + ": " + this.name + " | Batched " + files.length + " files into " + mapQueue.size() + " batches.");
        return mapQueue.size();
    }

    public int populateReduceQueue(Config config) {
        logger.info(this.type + ": " + this.name + " | Populating Reduce Queue...");
        File[] files = new File(config.getIntermediateDir()).listFiles();
        if (files == null || files.length == 0 ) {
            logger.warn(this.type + ": " + this.name + " | No intermediate files found in directory" + config.getIntermediateDir());
            return 0;
        }
        int batchSize = (files.length + Util.getNrClients() * batchNormalizer - 1) / (Util.getNrClients() * batchNormalizer);
        long batchNum = 1;
        List<String> batch = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                batch.add(file.getAbsolutePath());
                if (batch.size() == batchSize) {
                    HashMap<Long, List<String>> batchMap = new HashMap<>();
                    batchMap.put(batchNum, new ArrayList<>(batch));
                    reduceQueue.offer(batchMap);
                    batch.clear();
                    batchNum++;
                }
            } else {
                logger.warn(this.type + ": " + this.name + " | ignoring " + file.getAbsolutePath());
            }
        }
        // Add any remaining files that didn't complete a full batch
        if (!batch.isEmpty()) {
            HashMap<Long, List<String>> batchMap = new HashMap<>();
            batchMap.put(batchNum, new ArrayList<>(batch));
            reduceQueue.offer(batchMap);
            batch.clear();
        }
        this.mapPhaseDone = true;
        System.out.println("Batched " + files.length + " intermediate files into " + reduceQueue.size() + " batches.");
        logger.info(this.type + ": " + this.name + " | Batched " + files.length + " intermediate files into " + reduceQueue.size() + " batches.");
        return reduceQueue.size();
    }
    @Override
    public void mapJobCompleted(String hostname) throws RemoteException {
        removeFromMapTakenList(hostname);
    }
    @Override
    public void reduceJobCompleted(String hostname) throws RemoteException {
        removeFromReduceTakenList(hostname);
    }
}
