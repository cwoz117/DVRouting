
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;


/**
 * Router Class
 * 
 * This class implements the functionality of a router
 * when running the distance vector routing algorithm.
 * 
 * The operation of the router is as follows:
 * 1. send/receive HELLO message
 * 2. while (!QUIT)
 *      receive ROUTE messages
 *      update mincost/nexthop/etc
 * 3. Cleanup and return
 * 
 * A separate process broadcasts routing update messages
 * to directly connected neighbors at regular intervals.
 * 
 *      
 * @author 	Majid Ghaderi
 * @version	2.0, Oct 11, 2015
 *
 */
public class Router {
	
	private int routerID;
	private int update;
	
	private RtnTable forwardTable;
	private Socket soc;

	private InetSocketAddress addr;
	
	/**
	 * Constructor to initialize the rouer instance 
	 * 
	 * @param routerId			Unique ID of the router starting at 0
	 * @param serverName		Name of the host running the network server
	 * @param serverPort		TCP port number of the network server
	 * @param updateInterval	Time interval for sending routing updates to neighboring routers (in milli-seconds)
	 */
	public Router(int routerId, String serverName, int serverPort, int updateInterval) {
		addr = new InetSocketAddress(serverName, serverPort);
		if (addr.isUnresolved())
			throw new IllegalArgumentException("Please give a valid server/port combination");
		
		this.routerID = routerId;
		this.update = updateInterval;
	}

	/**
	 * starts the router 
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {
		boolean connected = relayHandshake();
		
		// regular operation;
		
		
		
		return new RtnTable();
	}

	public boolean relayHandshake(){
		try {
			soc = new Socket(addr.getAddress(), addr.getPort());
			ObjectOutputStream ds = new ObjectOutputStream(soc.getOutputStream());
			ObjectInputStream is = new ObjectInputStream(soc.getInputStream());
			
			DvrPacket message = new DvrPacket(routerID, DvrPacket.SERVER,DvrPacket.HELLO);
			ds.writeObject(message);
			ds.flush();
			
			DvrPacket returned = (DvrPacket)is.readObject();
			int[] neighbors = new int[returned.getMinCost().length];
			for (int i = 0; i < neighbors.length; i++){
				neighbors[i] = i;
			}
			forwardTable = new RtnTable(returned.getMinCost(), neighbors);
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * A simple test driver
	 * 
	 */
	public static void main(String[] args) {
		String serverName = "localhost";
		int serverPort = 2227;
		int updateInterval = 1000;
		int routerId = 0;


		if (args.length == 1) {
			routerId = Integer.parseInt(args[0]);
		}
		else if (args.length == 4) {
			routerId = Integer.parseInt(args[0]);
			serverName = args[1];
			serverPort = Integer.parseInt(args[2]);
			updateInterval = Integer.parseInt(args[3]);
		}
		else {
			System.out.println("incorrect usage, try again.");
			System.exit(0);
		}

		System.out.printf("starting Router #%d with parameters:\n", routerId);
		System.out.printf("Relay server host name: %s\n", serverName);
		System.out.printf("Relay server port number: %d\n", serverPort);
		System.out.printf("Routing update intwerval: %d (milli-seconds)\n", updateInterval);

		Router router = new Router(routerId, serverName, serverPort, updateInterval);
		RtnTable rtn = router.start();
		System.out.println("Router terminated normally");

		System.out.println();
		System.out.println("Routing Table at Router #" + routerId);
		System.out.print(rtn.toString());
	}

}
