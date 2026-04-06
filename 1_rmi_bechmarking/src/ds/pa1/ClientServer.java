package ds.pa1;

/**
 * The main class of the whole program, started by prun. 
 * You should not modify this file.
 */
public class ClientServer {
	private static int nrCalls;
	
	public static int getNrSequenceNumberCalls() {
		return nrCalls;
	}
	
	public static void main(String[] args) {
		if(args.length != 1) {
			System.err.println("Usage: java ClientServer <nrCalls>");
			System.err.println("Currently called with " + args.length + " arguments:");
			for(String s: args) {
				System.err.println("    " + s);
			}
			System.exit(1);
		}
		nrCalls = Integer.parseInt(args[0]);
		
		if(Util.getHostnames().size() < 2) {
			System.err.println("This application need to be executed on at least 2 machines.");
			System.exit(1);
		}
		
		if(Util.amICoordinator()) {
    		// I am the server
    		Server.main(args);
    	} else {
    		// I am a client
    		Client.main(args);
    	}
    }
}
