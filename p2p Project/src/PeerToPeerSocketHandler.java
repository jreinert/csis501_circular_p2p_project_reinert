/* Description: This handles threading for the ServerNode class
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class PeerToPeerSocketHandler extends Thread {
	// Class Vars
	PeerToPeerServ peer2PeerServ;
	PeerNode peerNode;
	ArrayList<PeerToPeerConnection> peerClientNodeConnectionList;
	
	// Constructor
	public PeerToPeerSocketHandler(PeerToPeerServ peer2PeerServ, PeerNode peerNode, ArrayList<PeerToPeerConnection> peerClientNodeConnectionList) {
		this.peer2PeerServ = peer2PeerServ;
		this.peerNode = peerNode;
		this.peerClientNodeConnectionList = peerClientNodeConnectionList;
	}
	
	public void run() {
		// Vars
		Socket peerToPeerSocket;
		boolean peerToPeerSocketHandlerRunning = true;
		
		while(peerToPeerSocketHandlerRunning) {
			try {
				peerToPeerSocket = peer2PeerServ.listenerSocket.accept();
				java.util.Date connDate = new java.util.Date();
				System.out.println(connDate + ": Another Peer Node is connecting");
				PeerToPeerConnection peerToPeerConnection = new PeerToPeerConnection(peerToPeerSocket, peerNode, peerClientNodeConnectionList);
				peerToPeerConnection.start();
			} catch (SocketException e) {
				java.util.Date closeDate = new java.util.Date();
				System.out.println(closeDate + ": Peer Node is shutting down");
				
				if(peerNode.peerSuperNode == 'Y') {
					for(PeerToPeerConnection c: peerClientNodeConnectionList) {
						c.closeConnections();
					}
					peerClientNodeConnectionList.clear();
				}
				
				break;
				
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	
	
	
}
