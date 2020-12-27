/* Description: This is the peer class with attributes defining a peer
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.net.*;
import java.io.*;

public class Peer implements Serializable {
	// Var Declaration
	int peerID;
	InetAddress peerIP;
	int peerPort;
	
	// Constructor
	public Peer() {
		peerID = -1;
		peerPort = -1;
		
		try {
			peerIP = InetAddress.getByName("localhost");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
}
