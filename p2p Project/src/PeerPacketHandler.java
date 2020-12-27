/* Description: This handles packets sent from the peer node at port 5000 to the other peer nodes
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.io.*;
import java.net.*;
import java.util.*;

public class PeerPacketHandler extends Thread {
	// Var Declaration
	PeerNode peerNode;
	boolean runHandler = true;
	
	ArrayList<Peer> peerList;
	
	// Constructor Method
	public PeerPacketHandler(PeerNode peerNode, ArrayList<Peer> peerList) {
		this.peerNode = peerNode;
		this.peerList = peerList;
	}
	
	public void run() {
		Packet packet;
		
		while(runHandler) {
			try {
				packet = (Packet) peerNode.inStream.readObject();
				processPacket(packet);
			}
			catch(Exception e) {
				break;
			}
		}
	}
	
	// Function to process packets received by peer
	void processPacket(Packet packet) {
		int eventCode = packet.eventCode;
		
		switch(eventCode) {
		
			case 0: java.util.Date regDate = new java.util.Date();
					System.out.println(regDate + ": You have been assigned Peer Node ID # " + packet.peerClientID);
					peerNode.peerClientID = packet.peerClientID;
					break;
			case 1: java.util.Date peerListDate = new java.util.Date();
					System.out.println(peerListDate + ": Peer Node joined or left the network - New neighbor Peer Nodes may be available");
				    copyPeerList(packet);
				    findPeer(peerNode);
					break;
		}
	}
	
	// Unpack peerList to find peer's neighbor Peer Nodes
	public void copyPeerList(Packet packet) {
		peerList.clear();
		for(int i = 0; i < packet.peerList.size(); i++) {
			peerList.add(packet.peerList.get(i));
		}
	}
	
	// Function to find preceding and succeeding peers
	void findPeer(PeerNode peerNode) {
		for(int i = 0; i < peerList.size(); i++) {
			if(peerList.size() == 1) {
				java.util.Date onlyPeerDate = new java.util.Date();
				System.out.println(onlyPeerDate + ": You are the only Peer Node Connected right now. Please wait for more Peer Nodes to connect");
				peerNode.predPeerID = -1;
				peerNode.succPeerID = -1;
			}
			
			else if(peerList.size() == 2) {
				if(peerNode.peerClientID == peerList.get(i).peerID && i == 0) {
					peerNode.predPeerID = peerList.get(i+1).peerID;
					peerNode.predPeerPort = peerList.get(i+1).peerPort;
					peerNode.predPeerIP = peerList.get(i+1).peerIP;
					
					peerNode.succPeerID = peerList.get(i+1).peerID;
					peerNode.succPeerPort = peerList.get(i+1).peerPort;
					peerNode.succPeerIP = peerList.get(i+1).peerIP;
					break;
				}
				else if(peerNode.peerClientID == peerList.get(i).peerID && i == 1) {
					peerNode.predPeerID = peerList.get(i-1).peerID;
					peerNode.predPeerPort = peerList.get(i-1).peerPort;
					peerNode.predPeerIP = peerList.get(i-1).peerIP;
					
					peerNode.succPeerID = peerList.get(i-1).peerID;
					peerNode.succPeerPort = peerList.get(i-1).peerPort;
					peerNode.succPeerIP = peerList.get(i-1).peerIP;
					break;
				}
			}
			else if(peerList.size() > 2) {
				if(peerNode.peerClientID == peerList.get(i).peerID && i == 0) {
					peerNode.predPeerID = peerList.get(peerList.size()-1).peerID;
					peerNode.predPeerPort = peerList.get(peerList.size()-1).peerPort;
					peerNode.predPeerIP = peerList.get(peerList.size()-1).peerIP;
					
					peerNode.succPeerID = peerList.get(i+1).peerID;
					peerNode.succPeerPort = peerList.get(i+1).peerPort;
					peerNode.succPeerIP = peerList.get(i+1).peerIP;
					break;
				}
				else if(peerNode.peerClientID == peerList.get(i).peerID && i == peerList.size()-1) {
					peerNode.predPeerID = peerList.get(i-1).peerID;
					peerNode.predPeerPort = peerList.get(i-1).peerPort;
					peerNode.predPeerIP = peerList.get(i-1).peerIP;
					
					peerNode.succPeerID = peerList.get(0).peerID;
					peerNode.succPeerPort = peerList.get(0).peerPort;
					peerNode.succPeerIP = peerList.get(0).peerIP;
					break;
				}
				else if(peerNode.peerClientID == peerList.get(i).peerID) {
					peerNode.predPeerID = peerList.get(i-1).peerID;
					peerNode.predPeerPort = peerList.get(i-1).peerPort;
					peerNode.predPeerIP = peerList.get(i-1).peerIP;
					
					peerNode.succPeerID = peerList.get(i+1).peerID;
					peerNode.succPeerPort = peerList.get(i+1).peerPort;
					peerNode.succPeerIP = peerList.get(i+1).peerIP;
					break;
				}
			}
		}
	}
	
	public void printPeerList(Packet packet) {
		for(int i = 0; i < packet.peerList.size(); i++) {
			System.out.println(packet.peerList.get(i).peerID + " " + packet.peerList.get(i).peerIP + ":" + packet.peerList.get(i).peerPort);
		}
	}
		
}
