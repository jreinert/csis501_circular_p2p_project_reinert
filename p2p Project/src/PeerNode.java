/* Description: This starts the Peer Node, connects to the network, makes req to and processes responses from the network.
 * Authors: Michael Mark and Jeremy Reinert
 * Date: 3/11/2020
 * Version: 1.0
 */



// Import packages
import java.io.*;
import java.net.*;
import java.util.*;

public class PeerNode {
	// Var Declaration for Server Node Connection
	
	int serverPort = 5000;
	int peerPort;
	int peerClientID;
	int peerClientListenPort;
	char peerSuperNode;
	PeerToPeerServ superPeer;
	PeerToPeerServ	peer2PeerServ;
	InetAddress peerClientIP;
	Socket serverNodeSocket;
	Socket peer2Peer;
	ObjectOutputStream outStream;
	ObjectInputStream inStream;
	ObjectOutputStream peer2PeerOutStream;
	ObjectInputStream peer2PeerInStream;
	FileOutputStream fileOutStream;
	FileInputStream fileInStream;
	BufferedInputStream bufInStream;
	BufferedOutputStream bufOutStream;
	
	// Var Declaration for Neighboring Peers
	ArrayList<Peer> peerList = new ArrayList<Peer>();
	int predPeerID;
	int predPeerPort;
	int succPeerID;
	int succPeerPort;
	InetAddress predPeerIP;
	InetAddress succPeerIP;
	String fileDir;
	
	public PeerNode() {
		predPeerID = -1;
		succPeerID = -1;
	}
	
	// Var Declaration for Distributed Hash Table
	Hashtable<String, ArrayList<InetSocketAddress>> distributedHashTable = new Hashtable<String, ArrayList<InetSocketAddress>>();

	public static void main(String args[]) {
		// Create new PeerNode object
		Scanner input = new Scanner(System.in);
		PeerNode peerNode = new PeerNode();
		boolean runPeerNode = true;
		boolean networkRun = true;
		
		// Parse command line args
		if(args.length == 0 || args.length > 1) {
			System.out.println("Too Many/Not enough parameters to start Peer Node");
			System.exit(0);
		}
		
		// Parse peer config file
		File file = new File(args[0]);
		peerNode.configFileParse(file);
		
		try {
			ServerSocket s = new ServerSocket(peerNode.serverPort);
			s.close();
			peerNode.peerClientListenPort = 5000;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			peerNode.serverPort = peerNode.peerClientListenPort;
		}
		
		
		try {
			// Start new Server Socket for P2P Connections
			
			peerNode.peer2PeerServ = new PeerToPeerServ();
			peerNode.peer2PeerServ.serverPort = peerNode.serverPort;
			peerNode.peer2PeerServ.listenerSocket = new ServerSocket();
			peerNode.peer2PeerServ.listenerSocket.setReuseAddress(true);
			peerNode.peer2PeerServ.listenerSocket.bind(new InetSocketAddress(peerNode.peer2PeerServ.serverPort));
			PeerToPeerSocketHandler peer2PeerHandler = new PeerToPeerSocketHandler(peerNode.peer2PeerServ, peerNode, peerNode.peer2PeerServ.peerClientNodeConnectionList);
			peer2PeerHandler.start();
			
			java.util.Date peer2PeerDate = new java.util.Date();
			System.out.println(peer2PeerDate + ": Peer Node running and waiting for other Peer Nodes to connect");
			
			// Set peer IP to localhost IP and connect to Server Node socket running on localhost : 5000
			peerNode.peerClientIP = InetAddress.getByName("localhost");
			peerNode.serverNodeSocket = new Socket(InetAddress.getByName("localhost"), 5000);
			peerNode.outStream = new ObjectOutputStream(peerNode.serverNodeSocket.getOutputStream());
			peerNode.inStream = new ObjectInputStream(peerNode.serverNodeSocket.getInputStream());
			java.util.Date connDate = new java.util.Date();
			System.out.println(connDate + ": Connected to the network: " + peerNode.serverNodeSocket.getInetAddress() + ":" + peerNode.serverNodeSocket.getLocalPort());
			
			// Create new registration packet to send to Super Node
			java.util.Date regDate = new java.util.Date();
			System.out.println(regDate + ": Registering with the network");
			peerNode.register();
			
			// Start new thread to receive packets from Server Node and Peer Nodes
			Thread peerToServer = new PeerPacketHandler(peerNode, peerNode.peerList);
			peerToServer.start();
			
			
			// Loop waiting on commands from user
			System.out.println("\nCommands --> Quit: 'q' | Print Neighbor Peer Nodes: 'p' | Print Distributed Hash Table: 'd'");
			System.out.println("Insert File into Distributed Hash Table: 'i' | Print Peer Node ID: 'm' | Find File Location in Network: 'f'\n");
	
			System.out.println("Print Peer Node Commands '?'");

			System.out.println("Enter Peer Node command...");
			
			while(runPeerNode) {
				Thread.sleep(1000);				
				// Peer Super Node Commands
				char peerNodeCommand = input.next().charAt(0);				
				// Peer Node Commands
				switch(peerNodeCommand) {
					case 'q': java.util.Date quitDate = new java.util.Date();
							  System.out.println(quitDate + ": Quitting the network");
							  if(peerNode.peer2PeerServ.serverPort == 5000) {
								  if(peerNode.peer2PeerServ.peerClientNodeConnectionList.size() == 1) {
									  peerNode.peer2PeerServ.listenerSocket.close();
								  }
								  else {
									  peerNode.sendQuit(peerNode);
									  peerNode.passDHT(peerNode);
										
									  // Remove Peer from other Peer Node's DHT
									  peerNode.removeFromDHT(peerNode);
									  peerNode.peer2PeerServ.listenerSocket.close();
									  
								  }
								  
							  }
							  else {
								  peerNode.sendQuit(peerNode);
								  // Pass DHT to Successor Peer Node
								  peerNode.passDHT(peerNode);
								  // Remove Peer from other Peer Node's DHT
								  peerNode.removeFromDHT(peerNode);
								  peerNode.peer2PeerServ.listenerSocket.close();
								  
								
							  }
							  
							  runPeerNode = false;
							  break;
					case 'p': java.util.Date printDate = new java.util.Date();
							  System.out.println(printDate + ": Peer Node requested that neighbor nodes are printed");
							  peerNode.printPeers(peerNode);
							  break;
					case 'i': java.util.Date insertDate = new java.util.Date();
							  System.out.println(insertDate + ": Peer Node wants to insert a file into the Distributed Hash Table");
							  peerNode.insertFile(peerNode);
							  break;
					case 'd': java.util.Date dhtPrintDate = new java.util.Date();
							  System.out.println(dhtPrintDate + ": Peer Node requested to print its Distributed Hash Table");
							  System.out.println(peerNode.distributedHashTable.toString());
							  break;
					case 'm': java.util.Date clientIDDate = new java.util.Date();
							  System.out.println(clientIDDate + ": Peer Node requested to print its Peer Node ID");
							  System.out.println(clientIDDate + ": Your Peer Node ID is: " + peerNode.peerClientID + " " + peerNode.peerClientIP + ":" + peerNode.peerClientListenPort);
							  break;
					case 'f': java.util.Date findFileDate = new java.util.Date();
							  System.out.println(findFileDate + ": Peer Node requested to find a file location in the Distributed Hash Table");
							  peerNode.findFile(peerNode);
							  break;
					case 'P': if(peerNode.peer2PeerServ.serverPort == 5000) {
								peerNode.printNodeConnections(peerNode);
							  }
							  else {
						        java.util.Date notSuperDate = new java.util.Date();
						        System.out.println(notSuperDate + ": You are not a Super Peer and do not have access to that command");
					          }
					          break;
					case '?': java.util.Date helpDate = new java.util.Date();
							  System.out.println(helpDate + ": Peer Node requested to print commands");
							  peerNode.printCommands();
							  break;
					default: java.util.Date nvCmdDate = new java.util.Date();
							 System.out.println(nvCmdDate + ": Not a valid command"); 
							 break;
				}
				
				System.out.println("\nCommands --> Quit: 'q' | Print Neighbor Peer Nodes: 'p' | Print Distributed Hash Table: 'd'");
				System.out.println("Insert File into Distributed Hash Table: 'i' | Print Peer Node ID: 'm' | Find File Location in Network: 'f'\n");
				System.out.println("Print Peer Node Commands '?'");
				System.out.println("Enter Peer Node command...");
			}
		} 
		
		catch(Exception e) {
			peerNode.peerSuperNode = 'N';
		}
	
	}
	
	// Function to send packets to Server Node
	void sendPacket(Packet packet) {
		try {
			outStream.writeObject(packet);
		} 
		
		catch (Exception e) {
			System.out.println("Packet not sent");
			
		}
	}
	
	// Function to register with Super Node
	void register() {
		Packet regPacket = new Packet();
		regPacket.eventCode = 0;
		regPacket.packetSenderID = peerClientID;
		regPacket.peerClientListenPort = peerClientListenPort;
		sendPacket(regPacket);
	}
	
	// Function to insert file into DHT
	void insertFile(PeerNode peerNode) {
		Scanner input = new Scanner(System.in);
		String fileName = "";
		System.out.println("Enter the name of the file you want to insert into the Distributed Hash Table:");
		fileName = input.nextLine();
		
		// Check to see if entered an input
		if(fileName.length() == 0) {
			while(fileName.length() == 0) {
				System.out.println("You did not enter anything. Enter the name of the file you want to insert into the Distributed Hash Table");
				fileName = input.nextLine();
			}
		}
		
		// Check if there are any neighboring peers and if not, enter into own DHT
		if(peerNode.peerList.size() >= 2) {
			try {
				peerNode.findPeer(peerNode);
				// Create Packet
				Packet insertPacket = new Packet();
				insertPacket.eventCode = 2;
				insertPacket.packetSenderID = peerClientID;
				insertPacket.peerClientIP = peerClientIP;
				insertPacket.peerClientListenPort = peerClientListenPort;
				insertPacket.fileName = fileName.toLowerCase();
				insertPacket.packetRecipientID = succPeerID;
				
				peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				java.util.Date date = new java.util.Date();
				System.out.println(date + ": Notifying Neighbor Peer Nodes of my file " + insertPacket.fileName + " so that they can add it to their Distributed Hash Tables");
				
				peerNode.peer2PeerOutStream.writeObject(insertPacket);
				
				peerNode.peer2Peer.close();
				peerNode.peer2PeerOutStream.close();
				peerNode.peer2PeerInStream.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		// If there are neighbors, insert file search for peer to enter the file into their DHT
		else {
			ArrayList<InetSocketAddress> fileLocationList = new ArrayList<InetSocketAddress>();
			InetSocketAddress fileLocation = new InetSocketAddress(peerClientIP, peerClientListenPort);
			java.util.Date enteredDate = new java.util.Date();
			if(peerNode.distributedHashTable.containsKey(fileName)) {
				peerNode.distributedHashTable.get(fileName).add(fileLocation);
				System.out.println(enteredDate + ": Entered file: " + fileName + " into Distributed Hash Table");
			}
			else {
				fileLocationList.add(fileLocation);
				peerNode.distributedHashTable.put(fileName, fileLocationList);
				System.out.println(enteredDate + ": Entered file: " + fileName + " into Distributed Hash Table");
			}
		}
	}
	
	// Function to find file in DHT
	void findFile(PeerNode peerNode) {
		Scanner input = new Scanner(System.in);
		String fileName = "";
		java.util.Date queryDate = new java.util.Date();
		System.out.println(queryDate + ": Enter the name of the file you want to find the location of in the Distributed Hash Table:");
		fileName = input.nextLine();
		
		// Check to see if entered an input
		if(fileName.length() == 0) {
			while(fileName.length() == 0) {
				java.util.Date queryDate2 = new java.util.Date();
				System.out.println(queryDate2 + ": You did not enter anything. Enter the name of the file you want to find the location of in the Distributed Hash Table");
				fileName = input.nextLine();
			}
		}
		
		if(peerNode.peerList.size() == 1) {
			java.util.Date queryDate3 = new java.util.Date();
			System.out.println(queryDate3 + ": You are the only Peer Node connected. Searching your Distributed Hash Table to see if you have the file");
			
			if(peerNode.distributedHashTable.containsKey(fileName)) {
				java.util.Date queryDate4 = new java.util.Date();
				System.out.println(queryDate4 + ": You have the file in your Distributed Hash Table");
			}
			
		}
		else {
		
			// Find neighboring Peer Node and connect
			peerNode.findPeer(peerNode);
			
			// Create Packet
			Packet findFilePacket = new Packet();
			findFilePacket.eventCode = 3;
			findFilePacket.packetSenderID = peerClientID;
			findFilePacket.peerClientIP = peerClientIP;
			findFilePacket.peerClientListenPort = peerClientListenPort;
			findFilePacket.fileName = fileName.toLowerCase();
			findFilePacket.packetRecipientID = succPeerID;
			
			try {
				peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
				peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
				peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
				java.util.Date connDate = new java.util.Date();
				
				peerNode.peer2PeerOutStream.writeObject(findFilePacket);
				
				peerNode.peer2Peer.close();
				peerNode.peer2PeerOutStream.close();
				peerNode.peer2PeerInStream.close();
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	// Function to send quit to Super Node
	void sendQuit(PeerNode peerNode) {
		Packet quitPacket = new Packet();
		quitPacket.packetSenderID = peerNode.peerClientID;
		quitPacket.eventCode = 8;
		sendPacket(quitPacket);
	}
	
	// Function to signal other Peer Nodes to delete quitting Peer Node from their DHT
	void removeFromDHT(PeerNode peerNode) {
		Packet removeFromDHT = new Packet();
		removeFromDHT.eventCode = 9;
		removeFromDHT.packetSenderID = peerNode.peerClientID;
		removeFromDHT.peerClientIP = peerNode.peerClientIP;
		removeFromDHT.peerClientListenPort = peerNode.peerClientListenPort;
		
		// Find neighboring Peer Node and connect
		peerNode.findPeer(peerNode);
				
		try {
			peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
			peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
			peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
			java.util.Date date = new java.util.Date();
			System.out.println(date + ": Notifying Neighbor Peer Nodes to remove me from their Distributed Hash Tables");
			
			peerNode.peer2PeerOutStream.writeObject(removeFromDHT);
			
			peerNode.peer2Peer.close();
			peerNode.peer2PeerOutStream.close();
			peerNode.peer2PeerInStream.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				peerNode.peer2PeerServ.listenerSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	// Function to pass Peer Node's Distributed Hash Table to Successor Peer Node upon quit
	void passDHT(PeerNode peerNode) {
		
		// Find neighboring Peer Node and connect
		peerNode.findPeer(peerNode);
		
		Packet passDHTPacket = new Packet();
		passDHTPacket.eventCode = 7;
		passDHTPacket.packetSenderID = peerNode.peerClientID;
		passDHTPacket.packetRecipientID = peerNode.succPeerID;
		passDHTPacket.distributedHashTable = peerNode.distributedHashTable;
		
		try {
			peerNode.peer2Peer = new Socket(peerNode.succPeerIP, peerNode.succPeerPort);
			peerNode.peer2PeerOutStream = new ObjectOutputStream(peerNode.peer2Peer.getOutputStream());
			peerNode.peer2PeerInStream = new ObjectInputStream(peerNode.peer2Peer.getInputStream());
			java.util.Date passDHTDate = new java.util.Date();
			System.out.println(passDHTDate + ": Passing Distributed Hash Table to Peer Node " + peerNode.succPeerID);
			
			peerNode.peer2PeerOutStream.writeObject(passDHTPacket);
			
			peerNode.peer2Peer.close();
			peerNode.peer2PeerOutStream.close();
			peerNode.peer2PeerInStream.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				peerNode.peer2PeerServ.listenerSocket.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
	}
	
	// Function to parse peer config file from command line args
	void configFileParse(File file) {
		try {
			Scanner fileScanner = new Scanner(file);
			while(fileScanner.hasNextLine()) {
				String fileLine = fileScanner.nextLine();
				String element[] = fileLine.split(" ", 2);
				switch(element[0]) {
				case "CLIENTID": peerClientID = Integer.parseInt(element[1]);
								 break;
				case "MYPORT": peerClientListenPort = Integer.parseInt(element[1]);
							   break;
				case "SUPERNODE": peerSuperNode = element[1].charAt(0);
							   break;
				case "FILEPATH": fileDir = element[1];
				}
			}
		}
		catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	// Function to find preceding and succeeding peers
	void findPeer(PeerNode peerNode) {
		for(int i = 0; i < peerList.size(); i++) {
			if(peerList.size() == 1) {
				System.out.println("You are the only Peer Node Connected right now");
				System.out.println("Please wait for more Peer Nodes to connect");
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
	
	// Function to print commands
	void printCommands() {
		System.out.println("Commands --> Quit: 'q' | Print Neighbor Peer Nodes: 'p' | Print Distributed Hash Table: 'd'");
		System.out.println("Print Peer Node Commands '?'");
		System.out.println("Enter Peer Node command...");
	}
	
	// Function to print peers
	void printPeers(PeerNode peerNode) {
		java.util.Date date = new java.util.Date();
		if(peerNode.predPeerID == -1 && peerNode.succPeerID == -1) {
			System.out.println(date + ": You are the only Peer Node Connected right now");
			System.out.println(date + ": Please wait for more Peer Nodes to connect");
		}
		else {
			System.out.println(date + ": Predecessor is Peer Node " + peerNode.predPeerID + " at " + peerNode.predPeerIP + ":" + peerNode.predPeerPort);
			System.out.println(date + ": Successor is Peer Node " + peerNode.succPeerID + " at " + peerNode.succPeerIP + ":" + peerNode.succPeerPort);
		}
	}
	
	public void printNodeConnections(PeerNode peerNode) {
		System.out.println("****************************************");
		System.out.println("Total active Peer Nodes: " + peerNode.peer2PeerServ.peerClientNodeConnectionList.size());
		System.out.println("Active Peer Nodes: ");
		for(int i = 0; i < peerNode.peer2PeerServ.peerClientNodeConnectionList.size(); i++) {
			System.out.println("Peer Node ID: " + peerNode.peer2PeerServ.peerClientNodeConnectionList.get(i).peerClientID);
			System.out.println("Peer Node IP: " + peerNode.peer2PeerServ.peerClientNodeConnectionList.get(i).peerClientIP);
			System.out.println("Peer Node Port: " + peerNode.peer2PeerServ.peerClientNodeConnectionList.get(i).peerClientListeningPort);
			System.out.println("****************************************");
		}
		if(peerNode.peer2PeerServ.peerClientNodeConnectionList.size() == 0) {
			System.out.println("****************************************");
		}
	}

}


