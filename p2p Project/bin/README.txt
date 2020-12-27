Description: p2p Project is a circular peer to peer network that allows peers to insert image files
into a distributed hash table, to query for images in the distributed hash table, and request that
the image file is sent to them.

Authors: Jeremy Reinert and Michael Mark
Date: 3/8/2020
Version: 1.0

p2p Project - readme.txt
 
== NETWORK/SUPERNODE ==
Start network/supernode by:
1) Opening a command line terminal and navigating to the directory where ../p2p Project/src is located
2) Compile the program with: javac *.java (hit enter)
3) Start the network/supernode with: java PeerNode peerconfig1.txt (hit enter) <-- This config file is bound to being
the starting supernode that all other peers join the network through.

== PEER NODES ==
Start the remaining peer nodes by:
1) Opening a command line terminal and navigating to the directory where ../p2p Project/src is located
2) If program has not been compiled, compile with: javac *.java (hit enter)
3) Start Client with: java PeerNode "NAME_OF_PEERCONFIG_FILE_HERE.txt" (hit enter) <-- See list of peerconfig files below

-Each peer node has a folder in the p2p Project\src\ directory where images are saved after they have been transferred to said peer.
-Each folder is associated with a given peer node and bound in their peerconfig file.
-Each folder comes preloaded with files, but the user must manually enter them into the distrbute hash table.
-Each folder must be manually (i.e. images deleted) returned to their init states after each round of testing. 

Peer Node Commands:

-'q': Quit Peer Node <-- Also shuts down the network/supernode for peer with peerconfig1.txt file
-'i': Insert File <-- Prompts peer to enter file name to insert into the distrbuted hash table
-'d': Print Distributed Hash Table <-- Prints peer's distributed hash table
-'m': Print Peer Node's Client ID Number <-- Prints peer's assigned client ID number
-'f': Find File <-- Prompts peer to enter file they wish to find and, if found, prompts them on
whether or not they want to request the file
-'?': Print Peer Commmand <-- Prints list of peer commands

== PEERCONFIG FILES, PEER NODE FILES, and INIT FILES LOADED ==

- "peerconfig1.txt"
--->peer1Files
------>cutekittenwithball.jpg
------>galaxy.jpg
- "peerconfig2.txt"
--->peer2Files
------>beach.jpg
------>cryingjordanmeme.jpg
------>kermit.jpg
- "peerconfig3.txt"
--->peer3Files
------>mountain.jpg
- "peerconfig4.txt"
------>jedimasteryoda.jpg
------>sithlorddarthvader.jpg





