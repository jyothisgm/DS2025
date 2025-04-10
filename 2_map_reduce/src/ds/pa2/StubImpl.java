package ds.pa2;

import java.io.File;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 * Code for the client side. This will run n-1 times, on ranks 1 .. n (where n
 * is the number of nodes passed to srun
 *
 * TODO: You have to modify and extend this file.
 */
public class StubImpl implements StubInterface {
    private int BATCH_SIZE = 32;
    private Queue<List<String>> mapQueue = new LinkedList<>();
    private boolean populateReduceQueueDone = false;
    private String name = Util.getMyHostname();

    public synchronized Queue<List<String>> getMapQueue() {
		return mapQueue;
	}

	private HashMap<String, List<String>> mapTakenList = new HashMap<>();

    @Override
	public synchronized List<String> getMapJob(String key) throws RemoteException {
        List<String> mapTaken = List.<String>of();
        if (!this.getMapQueue().isEmpty()) {
		mapTaken = mapQueue.poll();
		this.mapTakenList.put(key, mapTaken);
        }
		return mapTaken;
	}

	public synchronized HashMap<String, List<String>> getMapTakenList() {
		return mapTakenList;
	}

	public synchronized void removeFromMapTakenList(String key) {
		mapTakenList.remove(key);
	}

	private Queue<List<String>> reduceQueue = new LinkedList<>();
	public Queue<List<String>> getReduceQueue() {
		return reduceQueue;
	}

	private HashMap<String, List<String>> reduceTakenList = new HashMap<>();
	public synchronized void removeFromReduceTakenList(String key) {
		reduceTakenList.remove(key);
	}

    @Override
	public synchronized List<String> getReduceJob(String key) throws RemoteException{
        List<String> reduceTaken = List.<String>of();
        if (!this.getReduceQueue().isEmpty()) {
		reduceTaken = mapQueue.poll();
		this.reduceTakenList.put(key, reduceTaken);
        }
		return reduceTaken;
	}

	public synchronized HashMap<String, List<String>> getReduceTakenList() {
		return reduceTakenList;
	}

    @Override
    public void barrier() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'barrier'");
    }

    @Override
    public boolean heartBeat() throws RemoteException {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'heartBeat'");
    }

    @Override
    public boolean isMapPhaseOver() throws RemoteException {
        if (!this.getMapQueue().isEmpty() || !this.getMapTakenList().isEmpty()|| !this.populateReduceQueueDone) {
            System.out.println(this.name + " | Map phase not done");
            System.out.println(this.name + " | " + this.getMapQueue().size() + "," + this.getMapTakenList().size() + "," + this.populateReduceQueueDone);
            return false;
        }
        return true;
    }

    public boolean isTimePopulateReduce() {
        if (!this.getMapQueue().isEmpty() || !this.getMapTakenList().isEmpty()) {
            return false;
        }
        return true;
    }
    @Override
    public boolean isReducePhaseOver() throws RemoteException {
        if (!this.getReduceQueue().isEmpty() || !this.getReduceTakenList().isEmpty()) {
            return false;
        }
        return true;
    }

    public void populateMapQueue(Config config) {
		File[] files = new File(config.getInputDir()).listFiles();
        System.out.println(this.name+" | populating Map Queue...");
        if (files == null || files.length == 0) {
            System.out.println("No files found in directory: " + config.getInputDir());
            return;
        }
        List<String> batch = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                batch.add(file.getAbsolutePath());
                if (batch.size() == BATCH_SIZE) {
                    // System.out.println(this.name+" | new batch "+ batch);
                    mapQueue.offer(new ArrayList<>(batch));
                    batch.clear();
                }
            }
            else{
            System.out.println(this.name+" | ignoring "+ file.getAbsolutePath());
            }
        }
        // Add any remaining files that didn't complete a full batch
        if (!batch.isEmpty()) {
            System.out.println(this.name+" | last batch "+ batch);
            mapQueue.offer(new ArrayList<>(batch));
            System.out.println(this.name+" | last batch size "+ batch.size());
            batch.clear();
        }

        System.out.println(this.name+" | Batched " + files.length + " files into " + mapQueue.size() + " batches.");
    }

    public void populateReduceQeueue(Config config){
        System.out.println(this.name+" | populating Reduce Queue...");
        File[] files = new File(config.getIntermediateDir()).listFiles();
        if (files == null || files.length == 0 ){
            System.out.println(" No intermediate files found in directory" + config.getIntermediateDir());
            return;
        }
        List<String> batch = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                batch.add(file.getAbsolutePath());
                if (batch.size() == BATCH_SIZE) {
                    System.out.println(this.name+" | new batch "+ batch);
                    reduceQueue.offer(new ArrayList<>(batch));
                    batch.clear();
                }
            }
            else{
            System.out.println(this.name+" | ignoring "+ file.getAbsolutePath());
            }
        }
        // Add any remaining files that didn't complete a full batch
        if (!batch.isEmpty()) {
            // System.out.println(this.name+" | last batch "+ batch);
            reduceQueue.offer(new ArrayList<>(batch));
            // System.out.println(this.name+" | last batch size "+ batch.size());
            batch.clear();
        }
        this.populateReduceQueueDone = true;
        System.out.println(this.name+" | Batched " + files.length + " intermediate files into " + reduceQueue.size() + " batches.");
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
