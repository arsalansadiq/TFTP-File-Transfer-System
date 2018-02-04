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



### There are four files in this project:
 * Client.java
 * ClientConnectionThread.java
 * IntermediateHost.java
 * ThreadedServer.java
 * Complete UML diagram (PNG file)
 * UCM diagrams for both RRQ and WRQ(PNG file)
   * Client.java: The client communicates with the intermediate host and can send rrq, wrq, or
acknowledgements to the host. This is the class that the user communicates with.
Client will either write out a file in its own system out to server, or read in 
data from server and write it into a file of its own.

   * ClientConnectionThread.java: This class is the class which does the work for the server. One thread of this class
is spawned for each client connected. After communication is established between 
server and client, this thread takes over and deals with reads and writes.

   * IntermediateHost.java: The intermediate host simply funnels data packets from the client to the server,
and vice versa. If recieved from client, sends to server. If recieved from server,
sends to client.

   * ThreadedServer.java: This class represents the object which is the server. This class has a dedicated
port which is port 69. The server listens to this port and whenever a client 
contacts it, the server spawns a thread of class client connection thread, 
dedicated to serving that client. There will only be one server, with a 
thread for each client connecting 

### Execution/Termination Instructions
 * *1) run the ThreadedServer.java class as java applications*
 * *2) run the IntermediateHost.java class as java applications*
 * *3) run the client.java class as java applications*
 
Then the user uses the client console window.
In this window the user should be prompted with "(R)EAD or (W)RITE"
user types r for read or w for write

Then the user will be asked to enter the name of the file to be accessed,where the user types in the filename (includeing extension) and then the user is asked to enter the name of the file to be written, and again the user enters a filename including the extension. The program will then proceed to complete the command entered by the user.
