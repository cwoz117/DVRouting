
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
	private int updateTimer;
	private RtnTable forwardTable;
	private Timer t;
	
	private Socket soc;
	ObjectOutputStream ds;
	ObjectInputStream is;
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

		if (routerId >= 999)
			throw new IllegalArgumentException("Do not use a router ID above 998");

		this.routerID = routerId;

		if (updateInterval < 0)
			throw new IllegalArgumentException("Update interval cannot be negative");

		updateTimer = updateInterval;
	}

	/**
	 * starts the router 
	 * 
	 * @return The forwarding table of the router
	 */
	public RtnTable start() {
		try{
			relayHandshake();
			startTimer(updateTimer);
			int type = 0;
			do{
				DvrPacket returned = (DvrPacket)is.readObject();
				type = returned.type;
				if (type == DvrPacket.ROUTE){
					if (returned.sourceid == DvrPacket.SERVER){
							newTable(returned);
							startTimer(updateTimer);
					} else {
						
						
						
						
						
					}
				}
			} while(type != DvrPacket.QUIT);

			soc.shutdownInput();
			soc.shutdownOutput();
			soc.close();
		} catch(UnknownHostException e){
			System.out.println("Could not find relay server");
			return forwardTable;
		} catch(ClassNotFoundException e){
			System.out.println("Could not understand reply from relay server");
			return forwardTable;
		} catch (IOException e){
			e.printStackTrace();
			return forwardTable;
		}
		return forwardTable;
	}


	public void relayHandshake() throws UnknownHostException, ClassNotFoundException, IOException{

		soc = new Socket(addr.getAddress(), addr.getPort());
		ds = new ObjectOutputStream(soc.getOutputStream());
		is = new ObjectInputStream(soc.getInputStream());

		DvrPacket message = new DvrPacket(routerID, DvrPacket.SERVER,DvrPacket.HELLO);
		ds.writeObject(message);
		ds.flush();

		DvrPacket returned = (DvrPacket)is.readObject();
		newTable(returned);
	}
	
	public void newTable(DvrPacket dvr){
		int[] neighbors = new int[dvr.getMinCost().length];
		for (int i = 0; i < neighbors.length; i++){
			neighbors[i] = i;
		}
		forwardTable = new RtnTable(dvr.getMinCost(), neighbors);
	}

	public void startTimer(int time)throws IOException{
		t = new Timer(true);
		t.schedule(new TimerTask(){
			public void run() {
				for (int i = 0; i < forwardTable.getMinCost().length; i++){
					int cost = forwardTable.getMinCost()[i];
					if ((cost < DvrPacket.INFINITY) && (cost > 0)){
						DvrPacket msg = new DvrPacket(routerID, forwardTable.getNextHop()[i], DvrPacket.ROUTE, forwardTable.getMinCost());
						try {
							ds.writeObject(msg);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}, 0, time);
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
