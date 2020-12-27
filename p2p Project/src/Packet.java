	/* Description: This is the packet class for objects sent between the Server Node to Peer Nodes and Peer Nodes to Peer Nodes over the socket connection
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.io.*;
import java.util.*;
import java.net.*;

public class Packet  implements Serializable {
	// Var Declaration
	int packetSenderID; // Packet Sender ID
	int packetRecipientID; // Packet Receiver ID
	int listeningPort; 
	int peerClientID;
	int peerClientListenPort;
	String fileName;
	InetAddress peerClientIP;
	ArrayList<Peer> peerList;
	ArrayList<InetSocketAddress> fileLocations;
	Hashtable<String, ArrayList<InetSocketAddress>> distributedHashTable;
	ArrayList<PeerToPeerConnection> peerClientNodeConnectionList;
	
	// eventCode Defs
	/*
	 * 0 : Peer Node is registering with the Server Node
	 * 1 : Server Node is replying to Peer Node with the ID, IP, and Port # of their neighboring (+2 & -2) Peer Nodes
	 * 2 : Peer Node is inserting file into DHT
	 * 3 : Peer Node is requesting location of file within the network
	 * 4 : Peer Node is responding with location of file within the network
	 * 5 : Peer Node is requesting the file
	 * 6 : Peer Node is responding with the requested file
	 * 7 : Peer Node is quitting and needs to pass DHT to neighboring peer
	 * 8 : Peer Node is quitting the network
	 * 9 : Start network when Server Node goes down
	 * 10 : Pass port number to succ peer upon quit
	 */
	int eventCode;
	
	// File vars
	final int MAX_FILE_SIZE = 1000000;
	int totalFileSize;
    byte buf[]; 
	
	
	// Constructor
	public Packet() {
		packetSenderID = -1;
		packetRecipientID = -1;
		eventCode = -1;
		listeningPort = -1;
		peerClientID = -1;
		peerClientListenPort = -1;
		peerList = new ArrayList<Peer>();
		fileLocations = new ArrayList<InetSocketAddress>();
		peerClientNodeConnectionList = new ArrayList<PeerToPeerConnection>();
		fileName = "";
		try {
			peerClientIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		buf = new byte[MAX_FILE_SIZE];
		totalFileSize = 0;
	}
	
}
