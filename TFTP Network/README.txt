SYSC3303 - REAL TIME CONCURRENT SYSTEMS

Assignment 1

Author(s): Aly Khan Barolia

Version: 1.0

Execution/Termination Instructions: 
In order to run this Client-Server interaction, you will need to start off by extracting the jar file using an application equipped to do this. Command prompt can also be used. Once extracted, open three different Eclipse-java windows. Selecting a new workspace each time may be required. Once those are open, open each file as a Java Project on each scrren. For example, window 1 will contain the Client.java file, window 2 will contain Intermediate.java and widow three will contain the Server.java file. Place those three files side-by-side for an easier view of the system. Once three windows are side-by-side, run Server.java first, then IntermediateHost then finally the Client (all as Java Applications). You will observe that the client has sent 11 commands containing a mixture of read and write instructions with the 11th one containing an error request. View the console of each window to view the results.

Known Bugs: The client is able to send read and write commands to the server through the intermediate host. The server also knows how to respond to read, write and invalid commands sent by the client. The only bug is that the server cannot send the byte buffer back to the intermediate host. Due to this the client cannot receive any data from the server after issuing a command.

Included files:
Client.java
IntermediateHost.java
Server.java
Assignment1.jar
Client UML Calloration (PNG file)
IntermediateHost UML Collabortaion (PNG file)
Sever UML Collaboration (PNG file)
Complete UML (PNG file)
UCM (PNG file)