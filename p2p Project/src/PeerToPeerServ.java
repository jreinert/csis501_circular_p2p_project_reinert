/* Description: This starts the server for directing connecting nodes/peers to their place in the circular p2p network as well as maintaining a list of all connected peers
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;


public class PeerToPeerServ {
	// ServerNode Class Vars
	int serverPort;
	ServerSocket listenerSocket;
	PeerNode peerNode;
	ArrayList<PeerToPeerConnection> peerClientNodeConnectionList;
	
	// Constructor function
	public PeerToPeerServ() {
		serverPort = 0;
		listenerSocket = null;
		peerClientNodeConnectionList = new ArrayList<PeerToPeerConnection>();
	}

}
