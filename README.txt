# TFTP-File-Transfer-System
## SYSC 3303 - Project Iteration 1
### Version: 1.0
### Author(s) 
 * Harshan Anton
 * Aly Khan Barolia
 * Arsalan Sadiq
 * Jeff Tudor

### Introduction:
The goal of this iteration is to create the client, Intermediate Host, and server programs to support steady-state file transfer. We design and implemented a file transfer system based on the TFTP specification (RFC 1350). The system will consist of TFTP client(s) running on one computer, an intermediate Host, and a multithreaded TFTP server. 

****Updates in this version
Implemented an error simulator in intermediate host.
With the error simulator, the user can simulate delayed, duplicate, or lost packets.
The user selects which packet will be simulated, and a desginated time if neccasary(duplicate or delay)


### Included Files:
 * Client.java
 * ClientConnectionThread.java
 * IntermediateHost.java
 * ThreadedServer.java
 * Complete UML diagram (PNG file)
 * UCM diagrams for both RRQ and WRQ(PNG file)
 Timing diagrams
 
   * Client.java: The client communicates with the intermediate host and can send rrq, wrq, or
acknowledgements to the host. This is the class that the user communicates with.
Client will either write out a file in its own system out to server, or read in 
data from server and write it into a file of its own.

   * ClientConnectionThread.java: This class is the class which does the work for the server. One thread of this class
is spawned for each client connected. After communication is established between 
server and client, this thread takes over and deals with reads and writes.

   * IntermediateHost.java: The intermediate host simply funnels data packets from the client to the server,
and vice versa. If received from client, sends to server. If received from server,
sends to client.

   * ThreadedServer.java: This class represents the object which is the server. This class has a dedicated
port which is port 69. The server listens to this port and whenever a client 
contacts it, the server spawns a thread of class client connection thread, 
dedicated to serving that client. There will only be one server, with a 
thread for each client connecting 

### Execution/Termination Instructions
Begin by placing a file to use for transfers in the root of the program directory. In our testing we used the file "sample.txt". Place any file in the same folder as sample.txt you wish to use for transfers.
 * *1) run the ThreadedServer.java class as java applications*
 * *2) run the IntermediateHost.java class as java applications*
 * *3) run the client.java class as java applications*

 First within the console for the intermediate host the error simulator options must be chosen
	Here the user can choose which type of simulation to run (or to not run a simulation) 
	and the specifications
Second within the console for client the user chooses which type of request to send, and the name of the
	neccasary files.
	