# TFTP-File-Transfer-System
## SYSC 3303 - Project Iteration 1

### Teammates 
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
    * Client. java class is the class which is responsible to ask the user which requests to be processed read or write.
    * ClientConnectionThread.java class is responsible to create a thread and send all the required data and packets to the intermediate host which has a port number of 23
    * IntermediateHost.java class is the class which is responsible to receive the requests and send them to the Server on port 69, while receive and send the data or Acknowledgements given by the server to the client.
    * ThreadedServer.java class is the class which implements the thread and is responsible to recive the requests and check whether the request receive is a read or an write, based on the requests, the server class sends backs Data or acknowlegemnts to the Host class which is expected to send it to client. 

### Instructions to run:

 * *1) run the ThreadedServer.java class*
 * *2) run the IntermediateHost.java class*
 * *3) run the client.java class and check the console for all the three classes for details about the package.*
