# TFTP-File-Transfer-System
## SYSC 3303 - Project Iteration 5
### Version: 5.0
### Author(s) 
 * Harshan Anton
 * Aly Khan Barolia
 * Arsalan Sadiq
 * Jeff Tudor

### Introduction:
The goal of this iteration is to create multiple clients, an Intermediate Host, and a server to support steady-state file transfer. We designed and implemented a file transfer system based on the TFTP specification (RFC 1350). The system will consist of TFTP client(s) running on one computer, an intermediate Host, and a multithreaded TFTP server on separate computers.
****Updates in this version
 * Starting Version: Established Connections for File Transfer without Error Detection and Correction
 * Version 1.0: Implemented of File Transfer without Error Detection and Correction
 * Version 2.0: Added I/O Error Handling (ERROR Packets 1, 2, 3, 6)
 * Version 3.0: Added Network Error Handling (Timeout/Retransmission)
 * Version 4.0: Added TFTP Packet Format Errors (ERROR Packets 4, 5)
 * Version 5.0: Implemented of File Transfer between Different Computers


### Included Files:
 * Client.java
 * ClientConnectionThread.java
 * IntermediateHost.java
 * ThreadedServer.java
 * Complete UML diagram (PNG file)
 * UCM diagrams for both RRQ and WRQ(PNG file)
 * Timing diagrams
 
   * Client.java: The client communicates to the server through the intermediate host and can send either of the following packets: RRQ, WRQ, ACK and DATA. This is the class that the user interacts with to specify which kind of request will be sent to the server. The client will either write out a file in its own system out to the server or read in data from server and write it into a file of its own.

   * ClientConnectionThread.java: This class does the work for the server. This class spawns a new thread for any new client connections made to the server. After communication is established between server and client, this thread takes over and deals with all future transactions.

   * IntermediateHost.java: The intermediate host also known as the Error Simulator, is a class that exists between the Client and Server class. All packets are gone through this class to check for duplication, missing packets, error packets, etc. The intermediate host simply funnels ACK and DATA packets from the client to the server, and vice versa.

   * ThreadedServer.java: This class represents the server. This class has a dedicated port which is port 69. The server listens to this port and whenever a client contacts it (through the intermediate host), the server spawns a thread of class ClientConnectionThread, dedicated to dealing with transactions from that client. There will only be one server, with a thread for each connecting client. 

### Execution/Termination Instructions
Begin by placing a file to use for transfers in the root of the program directory. In our testing we used the file "sample.txt". Place any file in the same folder as sample.txt you wish to use for transfers.
 * *1) run the ThreadedServer.java class as java applications*
 * *2) run the IntermediateHost.java class as java applications*
 * *3) run the client.java class as java applications*
 * *4)	Follow the prompts in the console window for the IntermediateHost class*
 * *5)	Once it says “waiting for packet from client” move over to the console screen for the Client class*
 * *6)	Follow and answer those prompts and the file Transfer will begin*

First within the console for the intermediate host the error simulator options must be chosen, here the user can choose which type of simulation to run (or to not run a simulation) and the specifications
Second within the console for client the user chooses which type of request to send, and the name of the necessary files.
	
