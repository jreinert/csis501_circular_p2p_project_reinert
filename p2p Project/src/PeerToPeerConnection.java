/* Description: This handles packets processing for peer node to server node and peer node to peer node connections
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 
 * Version: 1.0
 */

// Import packages
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

public class PeerToPeerConnection extends Thread implements Serializable {
	// Var Declaration
	int peerClientPort;
	int peerClientListeningPort;
	int peerClientID;
	InetAddress peerClientIP;
	Socket peerToPeerSocket;
	ObjectInputStream inStream;
	ObjectOutputStream outStream;
	FileOutputStream fileOutStream;
	FileInputStream fileInStream;
	BufferedInputStream bufInStream;
	BufferedOutputStream bufOutStream;
	DataInputStream dataOutStream;
	PeerNode peerNode;
	boolean runConnection = true;
	ArrayList<PeerToPeerConnection> peerClientNodeConnectionList;
	
	// Constructor
	public PeerToPeerConnection(Socket peerToPeerSocket, PeerNode peerNode, ArrayList<PeerToPeerConnection> peerClientNodeConnectionList) throws IOException  {
		this.peerToPeerSocket = peerToPeerSocket;
		this.peerNode = peerNode;
		this.peerClientNodeConnectionList = peerClientNodeConnectionList;
		this.peerClientIP = peerToPeerSocket.getInetAddress();
		this.peerClientPort = peerToPeerSocket.getPort();
		this.inStream = new ObjectInputStream(peerToPeerSocket.getInputStream());
		this.outStream = new ObjectOutputStream(peerToPeerSocket.getOutputStream());
	}
	
	// Run function to loop waiting on packets sent through the peerClientSocket
	@Override
	public void run() {
		Packet packet = new Packet();
		
		while (runConnection) {
			try {
				packet = (Packet) inStream.readObject();
				processEvent(packet);
			} catch (Exception e) {
				//System.out.print(e);
				break;
			}
		}	
	}
	
	// Process Event Codes and call corresponding methods
	public void processEvent(Packet packet) {
		int eventCode = packet.eventCode;
		
		switch(eventCode) {
			case 0: registerPeer(packet);
					break;
			case 2: insertFile(packet);
			        //forwardPacket(packet);
			        break;
			case 3: findFile(packet);
					break;
			case 4: processFileLocation(packet);
					break;
			case 5: fileTransferReq(packet);
					break;
			case 6: recFile(packet);
					break;
			case 7: passingDHT(packet);
					break;
			case 8: peerQuitting(packet);
					break;
			case 9: updateDHTPeerQuit(packet);
					break;
		}
	}
	
	// Function to update DHT when client quits
	public void updateDHTPeerQuit(Packet packet) {
		
		if(peerNode.distributedHashTable.size() > 0) {
			// Remove address location from DHT
			java.util.Date updateDHTDate = new java.util.Date();
			System.out.println(updateDHTDate + ": Peer Node " + packet.packetSenderID + " is leaving the network - Distributed Hash Table being updated");
			InetSocketAddress fileLocation = new InetSocketAddress(packet.peerClientIP, packet.peerClientListenPort);
			
	        Set<String> keys = peerNode.distributedHashTable.keySet();
			for(String key: keys) {
				for(int i = 0; i < peerNode.distributedHashTable.get(key).size(); i++) {
					
					if(peerNode.distributedHashTable.get(key).get(i).getPort() == fileLocation.getPort()) {
						if(peerNode.distributedHashTable.get(key).size() > 1)
							peerNode.distributedHashTable.get(key).remove(i);
						else if(peerNode.distributedHashTable.get(key).size() == 1) {
							peerNode.distributedHashTable.remove(key);
						}
					}
				}
			}
		}
		
		//Pass packet on to next peer
		if(peerNode.succPeerID != packet.packetSenderID) {
			peerNode.findPeer(peerNode);
			if(peerNode.succPeerID == -1) {
				java.util.Date d1 = new java.util.Date();
				System.out.println(d1 + ": You have no neighboring peers to pass this information on to");
			}
			else {
				try {
					java.util.Date date = new java.util.Date();
					System.out.println(date + ": Notifying my Neighbor Peer Node that " + packet.packetSenderID + " is leaving the network and that their Disributed Hash Table might need to be updated");
					peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
					peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
					peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
					peerNode.peer2PeerOutStream.writeObject(packet);
				
					// Close socket/connection
					closeConnection();
				} 
				catch (IOException e) {
					// TODO Auto-generated catch blo
					e.printStackTrace();
				}
			}		
		}
	}
	
	// Function to determine if file should be entered into Distributed Hash Table at the given peer or whether it should be forwarded on to the successor
	public void insertFile(Packet packet) {
		
		// Use hashCode function to get a hash of the file name in the range 0 - 2^n - 1
		int fileHash = (int) (packet.fileName.hashCode() % (Math.pow(2, 4) - 1));
		
		// If hash is a negative number, multiply by -1 to get a positive hash value
		if(fileHash < 0) {
			fileHash = fileHash * -1;
		}
		
		// Create InetSocketAddress ArrayList, add InetSocketAddress
		ArrayList<InetSocketAddress> fileLocationList = new ArrayList<InetSocketAddress>();
		InetSocketAddress fileLocation = new InetSocketAddress(packet.peerClientIP, packet.peerClientListenPort);
		
		// Check if fileHash is equal to Peer Node ID, check if file is in the DHT - if so, just add inet address else add file name and location to the DHT
		if(fileHash == peerNode.peerClientID) {
			java.util.Date date = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				peerNode.distributedHashTable.get(packet.fileName).add(fileLocation);
				System.out.println(date + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(packet.fileName, fileLocationList);
				System.out.println(date + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
		}
		
		// Check if fileHash < Peer Node ID and Peer Node ID < Peer Node Predecessor Peer ID - if so, add file
		else if(fileHash < peerNode.peerClientID && peerNode.peerClientID < peerNode.predPeerID) {
			java.util.Date date2 = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				peerNode.distributedHashTable.get(packet.fileName).add(fileLocation);
				System.out.println(date2 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(packet.fileName, fileLocationList);
				System.out.println(date2 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
		}
		
		// Check if fileHash < Peer Node ID and fileHash > Peer Node Predecessor Peer ID - if so, add file
		else if(fileHash < peerNode.peerClientID && fileHash > peerNode.predPeerID) {
			java.util.Date date3 = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				peerNode.distributedHashTable.get(packet.fileName).add(fileLocation);
				System.out.println(date3 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(packet.fileName, fileLocationList);
				System.out.println(date3 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			
		}
		
		// Check if fileHash > Peer Node ID and Peer Node Successor Peer ID < Peer Node ID - if so, add file
		else if(fileHash > peerNode.peerClientID && peerNode.succPeerID < peerNode.peerClientID) {
			java.util.Date date4 = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				peerNode.distributedHashTable.get(packet.fileName).add(fileLocation);
				System.out.println(date4 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(packet.fileName, fileLocationList);
				System.out.println(date4 + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
		}
		
		// Otherwise, forward packet on
		else {
			Packet p = packet;
			try {
				peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				peerNode.peer2PeerOutStream.writeObject(p);
				
				// Close connection/socket
				closeConnection();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Function to find file location in the Distributed Hash Table
	public void findFile(Packet packet) {
		Scanner input = new Scanner(System.in);
		char reqFile;
		// If packet has circled network
		if(peerNode.peerClientID == packet.packetSenderID) {
			// If peer has file in its own DHT
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				java.util.Date hasFileDate = new java.util.Date();
				System.out.print(hasFileDate + ": File is in my Distributed Hash Table at " );
				for(int i = 0; i < peerNode.distributedHashTable.get(packet.fileName).size(); i++) {	
					System.out.println(peerNode.distributedHashTable.get(packet.fileName).get(i) + " ");
				}
				
				ArrayList<InetSocketAddress> fileLocations = new ArrayList<InetSocketAddress>();
				for(int i = 0; i < peerNode.distributedHashTable.get(packet.fileName).size(); i++) {
					fileLocations.add(peerNode.distributedHashTable.get(packet.fileName).get(i));
				}
				
				// Propmpt user if they want to transfer file
				java.util.Date inDate = new java.util.Date();
				System.out.println(inDate + ": Request the file? Yes: 'y' | No: 'n'");
				reqFile = input.next().charAt(0);
				// input validation
				while(reqFile == 0 && (reqFile != 'y' || reqFile != 'n')) {
					java.util.Date errDate = new java.util.Date();
					System.out.println(errDate + ": You did not enter a valid command. Try Again. Request the file? Yes: 'y' | No: 'n'");
					reqFile = input.next().charAt(0);
				}
				// if yes to transfer
				if(reqFile == 'y') {
					if(fileLocations.size() == 1) {
						if(fileLocations.get(0).getPort() == peerNode.peerClientListenPort) {
							java.util.Date haveFile = new java.util.Date();
							System.out.println(haveFile + ": I already have this file");
						}
						else {
							java.util.Date reqDate = new java.util.Date();
							System.out.println(reqDate + ": Requesting the file named " + packet.fileName + " from Peer at IP" + fileLocations.get(0).getAddress() + ":" + fileLocations.get(0).getPort());
							
							Packet reqFilePckt = new Packet();
							reqFilePckt.eventCode = 5;
							reqFilePckt.packetSenderID = peerNode.peerClientID;
							reqFilePckt.peerClientIP = peerNode.peerClientIP;
							reqFilePckt.peerClientListenPort = peerNode.peerClientListenPort;
							reqFilePckt.fileName = packet.fileName;
							
							try {
								// req file
								peerNode.peer2Peer = new Socket(fileLocations.get(0).getAddress(), fileLocations.get(0).getPort());
								peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
								peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
								peerNode.peer2PeerOutStream.writeObject(reqFilePckt);
								
								// Close socket/connection
								closeConnection();
							} 
							catch (IOException e) {
								// TODO Auto-generated catch blo
								e.printStackTrace();
							}
						}
					}
					else {
						int index = (int) (Math.random() * (fileLocations.size()+1));
						while( fileLocations.get(index).getPort() == peerNode.peerClientListenPort) {
							index = (int) (Math.random() * (fileLocations.size()+1));
						}
						
						java.util.Date reqDate = new java.util.Date();
						System.out.println(reqDate + ": Requesting the file named " + packet.fileName + " from Peer at IP" + packet.fileLocations.get(index).getAddress() + ":" + packet.fileLocations.get(index).getPort());
						
						Packet reqFilePckt = new Packet();
						reqFilePckt.eventCode = 5;
						reqFilePckt.packetSenderID = peerNode.peerClientID;
						reqFilePckt.peerClientIP = peerNode.peerClientIP;
						reqFilePckt.peerClientListenPort = peerNode.peerClientListenPort;
						reqFilePckt.fileName = packet.fileName;
						
						try {
							// req file
							peerNode.peer2Peer = new Socket(packet.fileLocations.get(index).getAddress(), packet.fileLocations.get(index).getPort());
							peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
							peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
							peerNode.peer2PeerOutStream.writeObject(reqFilePckt);
						
							// Close socket/connection
							closeConnection();
						} 
						catch (IOException e) {
							// TODO Auto-generated catch blo
							e.printStackTrace();
						}
					}
				}
				else if(reqFile == 'n') {
					java.util.Date noReqDate = new java.util.Date();
					System.out.println(noReqDate + ": File transfer is not requested");
				}
			}
			else {
				java.util.Date notFndDate = new java.util.Date();
				System.out.println(notFndDate + ": File was not found in the network. Please try again later");
			}
		}
		// Check if peer DHT has file and if so, send response
		else if(peerNode.peerClientID != packet.packetSenderID && peerNode.distributedHashTable.containsKey(packet.fileName)) {
			java.util.Date fileFoundDate = new java.util.Date();
			System.out.println(fileFoundDate + ": I have the file in my Distributed Hash Table");
			Packet fileLocationPacket = new Packet();
			fileLocationPacket.eventCode = 4;
			fileLocationPacket.fileName = packet.fileName;
			fileLocationPacket.packetSenderID = peerNode.peerClientID;
			fileLocationPacket.packetRecipientID = packet.packetSenderID;
			
			for(int i = 0; i < peerNode.distributedHashTable.get(packet.fileName).size(); i++) {
				fileLocationPacket.fileLocations.add(peerNode.distributedHashTable.get(packet.fileName).get(i));
			}
			try {
				java.util.Date fileLocSentDate = new java.util.Date();
				System.out.println(fileLocSentDate + ": Sending file location to Peer Node " + packet.packetSenderID);
				peerNode.peer2Peer = new Socket(packet.peerClientIP, packet.peerClientListenPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				peerNode.peer2PeerOutStream.writeObject(fileLocationPacket);
				
				// Close socket/connection
				closeConnection();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}				
		}
		
		// Forward packet to Successor Peer Nodes until Peer with file in their Distributed Hash Table is found
		else {
			Packet p = packet;
			try {
				peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				peerNode.peer2PeerOutStream.writeObject(p);
				
				// Close socket/connection
				closeConnection();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		closeConnection();
	}

	// Function to process file locations and request file transfer
	public void processFileLocation(Packet packet) {
		Scanner input = new Scanner(System.in);
		char reqFile;
		java.util.Date recFileLocDate = new java.util.Date();
		System.out.println(recFileLocDate + ": Received the file locations from Peer Node " + packet.packetSenderID);
		
		if(packet.fileLocations.size() == 0) {
			java.util.Date fileNotFoundDate = new java.util.Date();
			System.out.println(fileNotFoundDate + ": File location was not found in any other Peer Node's Distributed Hash Table. Try again later when new Peer Nodes connect to the network");
		}
		
		// Print File Locations
		else {
			for(int i = 0; i < packet.fileLocations.size(); i++) {
				java.util.Date fileLocDate = new java.util.Date();
				InetAddress fileIP = packet.fileLocations.get(i).getAddress();
				int filePort = packet.fileLocations.get(i).getPort();
				System.out.println(fileLocDate + ": The file is located at " + fileIP + " : " + filePort);
			}
			
			// input to request file transfer
			java.util.Date inDate = new java.util.Date();
			System.out.println(inDate + ": Request the file? Yes: 'y' | No: 'n'");
			reqFile = input.next().charAt(0);
			// input validation
			while(reqFile == 0 && (reqFile != 'y' || reqFile != 'n')) {
				java.util.Date errDate = new java.util.Date();
				System.out.println(errDate + ": You did not enter a valid command. Try Again. Request the file? Yes: 'y' | No: 'n'");
				reqFile = input.next().charAt(0);
			}
			
			// user wants to request file transfer
			if(reqFile == 'y') {
				if(packet.fileLocations.size() == 1) {
					java.util.Date reqDate = new java.util.Date();
					System.out.println(reqDate + ": Requesting the file named " + packet.fileName + " from Peer at IP" + packet.fileLocations.get(0).getAddress() + ":" + packet.fileLocations.get(0).getPort());
					
					Packet reqFilePckt = new Packet();
					reqFilePckt.eventCode = 5;
					reqFilePckt.packetSenderID = peerNode.peerClientID;
					reqFilePckt.peerClientIP = peerNode.peerClientIP;
					reqFilePckt.peerClientListenPort = peerNode.peerClientListenPort;
					reqFilePckt.fileName = packet.fileName;
					
					try {
						// req file
						peerNode.peer2Peer = new Socket(packet.fileLocations.get(0).getAddress(), packet.fileLocations.get(0).getPort());
						peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
						peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
						peerNode.peer2PeerOutStream.writeObject(reqFilePckt);
						
						// Close socket/connection
						closeConnection();
					} 
					catch (IOException e) {
						// TODO Auto-generated catch blo
						e.printStackTrace();
					}
				}
				else if(packet.fileLocations.size() > 1) {
					int index = (int) (Math.random() * (packet.fileLocations.size()+1));
					
					while(packet.fileLocations.get(index).getPort() == peerNode.peerClientListenPort) {
						index = (int) (Math.random() * (packet.fileLocations.size()+1));
					}
					java.util.Date reqDate = new java.util.Date();
					System.out.println(reqDate + ": Requesting the file named " + packet.fileName + " from Peer at IP" + packet.fileLocations.get(index).getAddress() + ":" + packet.fileLocations.get(index).getPort());
					
					Packet reqFilePckt = new Packet();
					reqFilePckt.eventCode = 5;
					reqFilePckt.packetSenderID = peerNode.peerClientID;
					reqFilePckt.peerClientIP = peerNode.peerClientIP;
					reqFilePckt.peerClientListenPort = peerNode.peerClientListenPort;
					reqFilePckt.fileName = packet.fileName;
					
					try {
						// req file
						peerNode.peer2Peer = new Socket(packet.fileLocations.get(index).getAddress(), packet.fileLocations.get(index).getPort());
						peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
						peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
						peerNode.peer2PeerOutStream.writeObject(reqFilePckt);
					
						// Close socket/connection
						closeConnection();
					} 
					catch (IOException e) {
						// TODO Auto-generated catch blo
						e.printStackTrace();
					}
				}
			}
			else if(reqFile == 'n') {
				java.util.Date noReqDate = new java.util.Date();
				System.out.println(noReqDate + ": File transfer is not requested");
			}
		}
	}
	
	//Function to rec file
	public void recFile(Packet packet) {
		java.util.Date recDate = new java.util.Date();
		System.out.println(recDate + ": Received file " + packet.fileName + " from Peer Node " + packet.packetSenderID);
		// rec file
		byte[] buf = new byte[packet.totalFileSize];
		
		// unpack buf byte array into file buf arr
		for(int i = 0; i < buf.length; i++)
			buf[i] = packet.buf[i];
		
		// write to file
		File file = new File("./"+peerNode.fileDir,packet.fileName);
		try {
			fileOutStream = new FileOutputStream(file);
			bufOutStream = new BufferedOutputStream(fileOutStream);
			bufOutStream.write(buf, 0, buf.length);
			
			fileOutStream.close();
			bufOutStream.close();
			closeConnection();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		updateDHT(packet);
	}
	
	// Function to update DHTs
	public void updateDHT(Packet packet) {
		// If there are neighbors, insert file search for peer to enter the file into their DHT
		if(peerNode.peerList.size() >= 2) {
			try {
				peerNode.findPeer(peerNode);
				// Create Packet
				Packet insertPacket = new Packet();
				insertPacket.eventCode = 2;
				insertPacket.packetSenderID = peerNode.peerClientID;
				insertPacket.peerClientIP = peerNode.peerClientIP;
				insertPacket.peerClientListenPort = peerNode.peerClientListenPort;
				insertPacket.fileName = packet.fileName.toLowerCase();
				insertPacket.packetRecipientID = peerNode.succPeerID;
				
				peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				
				peerNode.peer2PeerOutStream.writeObject(insertPacket);
				
				closeConnection();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		// Check if there are any neighboring peers and if not, enter into own DHT
		else {
			ArrayList<InetSocketAddress> fileLocationList = new ArrayList<InetSocketAddress>();
			InetSocketAddress fileLocation = new InetSocketAddress(peerNode.peerClientIP, peerNode.peerClientListenPort);
			java.util.Date date = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(packet.fileName)) {
				peerNode.distributedHashTable.get(packet.fileName).add(fileLocation);
				System.out.println(date + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(packet.fileName, fileLocationList);
				System.out.println(date + ": Entered file: " + packet.fileName + " into Distributed Hash Table");
			}
		}
	}
	
	// Function to transfer file
	public void fileTransferReq(Packet packet) {
		java.util.Date date = new java.util.Date();
		System.out.println(date + " " + packet.packetSenderID + " is requesting file " + packet.fileName);
		System.out.println(peerNode.fileDir);
		File reqFile = new File("./"+peerNode.fileDir, packet.fileName);
		byte[] buf = new byte [(int)reqFile.length()]; 
		
		try {
			java.util.Date fileSendDate = new java.util.Date();
			System.out.println(fileSendDate + ": Sending the file " + packet.fileName + " to Peer Node " + packet.packetSenderID);
			fileInStream = new FileInputStream(reqFile);
			bufInStream = new BufferedInputStream(fileInStream);
			bufInStream.read(buf, 0, buf.length);

			Packet fileTransPckt = new Packet();
			for(int i = 0; i < buf.length; i++ ) {
				fileTransPckt.buf[i] = buf[i];
			}
			
			fileTransPckt.eventCode = 6;
			fileTransPckt.packetSenderID = peerNode.peerClientID;
			fileTransPckt.packetRecipientID = packet.packetSenderID;
			fileTransPckt.totalFileSize = buf.length;
			fileTransPckt.fileName = packet.fileName;
			peerNode.peer2Peer = new Socket(packet.peerClientIP, packet.peerClientListenPort);
			peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
			peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
			
			peerNode.peer2PeerOutStream.writeObject(fileTransPckt);
			peerNode.peer2PeerOutStream.flush();
			
			// Close socket/connection
			fileInStream.close();
			bufInStream.close();
			closeConnection();
		} 
		catch (IOException e) {
			// TODO Auto-generated catch blo
			e.printStackTrace();
		}
	}
		
	// Function to received passed DHT from preceding neighboring peer
	public void passingDHT(Packet packet) {
		java.util.Date passDHTDate = new java.util.Date();
		System.out.println(passDHTDate + ": Received Peer Node " + packet.packetSenderID+"'s Distributed Hash Table");
		
		// Iterate through Distribution Hash Table sent in packet and add to Peer Node's distribution Hash table accordingly/as needed
		for(Entry<String, ArrayList<InetSocketAddress>> keys : packet.distributedHashTable.entrySet()) {
			if(peerNode.distributedHashTable.containsKey(keys.getKey())) {
				peerNode.distributedHashTable.get(keys.getKey()).addAll(keys.getValue());	
				System.out.println(keys.getKey());
			}
			else if (peerNode.distributedHashTable.contains(keys.getKey()) == false) {
				peerNode.distributedHashTable.put(keys.getKey(), keys.getValue());
			}
		}
		closeConnection();
		
	}
	
	// Function to close connection called from client
	public void peerQuitting(Packet packet) {
		// Search for client in peerClientNodeConnectionList and remove them from the ArrayList
		int peerPosition = peerSearch(packet.packetSenderID);
		peerClientNodeConnectionList.remove(peerPosition);
		sendPeerListToAll();
		java.util.Date date = new java.util.Date();
		System.out.println(date + ": Peer Node " + packet.packetSenderID + " has left the network");
		//printNodeConnections();
		closeConnections();
	}
	
	// Function to close all connections/sockets/threads
	public void closeConnection() {
		java.util.Date connClosedDate = new java.util.Date();
		try {
			peerNode.peer2PeerOutStream.close();
			peerNode.peer2PeerInStream.close();
			peerNode.peer2Peer.close();
			outStream.close();
			inStream.close();
			peerToPeerSocket.close();
		} catch (IOException e) {
			System.out.println(connClosedDate + ": Could not close connection");
		}
		
		
	}
	
	public int getID() {
		return peerClientID;
	}
	
	// Function to register Peers with the server
	public void registerPeer(Packet packet) {
		peerClientListeningPort = packet.peerClientListenPort;
		peerClientID = assignID();
		peerClientNodeConnectionList.add(this);
		java.util.Date date = new java.util.Date();
		System.out.println(date + ": Peer Node ID: " + peerClientID + " connected to the Server Node...");
		peerClientNodeConnectionList.sort(Comparator.comparing(PeerToPeerConnection::getID));
		//printNodeConnections();
		
		// Send peerID to registered peer
		Packet regPacket = new Packet();
		regPacket.eventCode = 0;
		regPacket.peerClientID = peerClientID;
		try {
			outStream.writeObject(regPacket);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// Send peerClientNodeConnectionList to ALL peers
		sendPeerListToAll();
	}
	
	public int assignID() {
		int id;
		int n = 4;
		
		id = (int)(Math.random() * (((Math.pow(2, n)-1) - 1) + 1));

		if(peerClientNodeConnectionList.isEmpty()) {
			return id;
		}
		else {
			while(true) {
				int searchedID = peerSearch(id);
				if(searchedID == -1) {
					break;
				}
				
				else {
					id = (int)(Math.random() * ((Math.pow(2, n - 1) - 1) + 1));
				}				
			}
		}		
		return id;
	}
	
	// Function to send peerList to all connected Peer Nodes
	public void sendPeerListToAll() {
		Packet packet = new Packet();
		packet.eventCode = 1;
		for(int i = 0; i < peerClientNodeConnectionList.size(); i++) {
			Peer peer = new Peer();
			peer.peerID = peerClientNodeConnectionList.get(i).peerClientID;
			peer.peerIP = peerClientNodeConnectionList.get(i).peerClientIP;
			peer.peerPort = peerClientNodeConnectionList.get(i).peerClientListeningPort;
			packet.peerList.add(peer);
		}
		
		for(int i = 0; i < peerClientNodeConnectionList.size(); i++) {
			try {
				peerClientNodeConnectionList.get(i).outStream.writeObject(packet);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
	}
	
	// Function to search for Peer Nodes in peerClientNodeConnectionList
	public int peerSearch(int peerID) {
		// Loop through ArrayList to find Peer Node pos and return it
		for(int i = 0; i < peerClientNodeConnectionList.size(); i++) {
			if(peerClientNodeConnectionList.get(i).peerClientID == peerID) {
				return i;
			}
		}
		return -1;
	}
	
	public void printNodeConnections() {
		System.out.println("****************************************");
		System.out.println("Total active Peer Nodes: " + peerClientNodeConnectionList.size());
		System.out.println("Active Peer Nodes: ");
		for(int i = 0; i < peerClientNodeConnectionList.size(); i++) {
			System.out.println("Peer Node ID: " + peerClientNodeConnectionList.get(i).peerClientID);
			System.out.println("Peer Node IP: " + peerClientNodeConnectionList.get(i).peerClientIP);
			System.out.println("Peer Node Port: " + peerClientNodeConnectionList.get(i).peerClientListeningPort);
			System.out.println("****************************************");
		}
		if(peerClientNodeConnectionList.size() == 0) {
			System.out.println("****************************************");
		}
	}
	
	// Function to close connections
	public void closeConnections() {
			try {
				outStream.close();
				inStream.close();
				peerToPeerSocket.close();
				//System.out.println("Connection closing...");
			}
			catch(Exception e) {
				System.out.println("Couldn't close connections...");
			}
		}
		
}
