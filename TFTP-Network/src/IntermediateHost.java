import java.io.*;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class IntermediateHost {
	private DatagramSocket sendReceiveSocket = null;
	private DatagramSocket tempSocket = null;
	private DatagramPacket sendReceivePacket;
	private final int serverPort = 69;
	private int clientPort, threadPort = 0;
	private boolean delaySim = false;
	private boolean lostSim = false;
	private boolean duplicateSim = false;
	private boolean rrqSim = false;
	private boolean wrqSim = false;
	private boolean dataSim = false;
	private boolean ackSim = false;
	private boolean TIDChange = false;
	private boolean invalidatePacket=false;
	private boolean invalidateMode=false;
	private boolean changeBlockNum=false;
	private boolean packetError=false;
	private int packetNum;// this will be the packet number to be used for the
	// given error simulation
	private int delayTime;

	private InetAddress inetAddressServer = null;
	private InetAddress inetAddressClient = null;

	public IntermediateHost() {

		try {
			sendReceiveSocket = new DatagramSocket(23);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		}
	}

	// delay requests, duplicate, lost packets
	public void run() throws IOException, InterruptedException {

		operationSetup();

		byte data[] = new byte[512];
		/*
		 * initial connection
		 */

		sendReceivePacket = new DatagramPacket(data, data.length);

		// waiting for a request from client to server

		System.out.println("Intermediate host: waiting for a packet from client");
		sendReceiveSocket.receive(sendReceivePacket);

		clientPort = sendReceivePacket.getPort();

		sendReceivePacket.setPort(serverPort);
		// send request to server
		System.out.println("Intermediate host: sending packet to server");
		// sendReceiveSocket.send(sendReceivePacket);
		sendReceivePacket.setAddress(inetAddressServer);
		sim();

		// waiting for response from server
		System.out.println("Intermediate host: waiting for a packet from thread");
		sendReceiveSocket.receive(sendReceivePacket);

		threadPort = sendReceivePacket.getPort();
		System.out.println("Intermediate host: received packet from thread");
		// sending response to client
		System.out.println("Intermediate host: sending packet to client");
		sendReceivePacket.setPort(clientPort);
		// sendReceiveSocket.send(sendReceivePacket);
		sendReceivePacket.setAddress(inetAddressClient);
		sim();

		System.out.println("\nTRANSFER HAS BEGUN.................................");
		while (true) {
			try {
				System.out.println("Intermediate host: waiting for a packet from client");
				sendReceiveSocket.receive(sendReceivePacket);
				System.out.println("host received packet from client: " + sendReceivePacket.getData()[0]
						+ sendReceivePacket.getData()[1]);

				sendReceivePacket.setPort(threadPort);
				sendReceivePacket.setAddress(inetAddressServer);

				sim();
				System.out.println("Intermediate host: sending packet to thread");

				// sendReceiveSocket.send(sendReceivePacket);
				System.out.println("host sending packet to thread: " + sendReceivePacket.getData()[0]
						+ sendReceivePacket.getData()[1]);

				System.out.println("Intermediate host: waiting for a packet from thread");
				sendReceiveSocket.receive(sendReceivePacket);
				System.out.println("host received packet from thread: " + sendReceivePacket.getData()[0]
						+ sendReceivePacket.getData()[1]);
			} catch (SocketTimeoutException se) {
				System.out.println("Exiting");
				break;
				// continue;

			}
			System.out.println("Intermediate host: sending packet to client");
			sendReceivePacket.setPort(clientPort);
			sendReceivePacket.setAddress(inetAddressClient);
			sim();
			System.out.println("host sending packet to client: " + sendReceivePacket.getData()[0]
					+ sendReceivePacket.getData()[1]);

		}
	}// handle rrq and wrq

	public void sim() throws InterruptedException, IOException {
		byte data[] = sendReceivePacket.getData();

		if (duplicateSim) {
			if ((wrqSim && data[0] == 0 && data[1] == 2) || (rrqSim && data[0] == 0 && data[1] == 1))
				duplicatePacketErrorSim(sendReceivePacket);
			else if ((dataSim && data[0] == 0 && data[1] == 3) || (ackSim && data[0] == 0 && data[1] == 4)) {
				if (blockNumMatch(sendReceivePacket)) {
					System.out.println("Block numbers matched for duplicate");
					duplicatePacketErrorSim(sendReceivePacket);
				} else
					sendReceiveSocket.send(sendReceivePacket);

			} else
				sendReceiveSocket.send(sendReceivePacket);

		}

		if (delaySim) {
			if ((wrqSim && data[0] == 0 && data[1] == 2) || (rrqSim && data[0] == 0 && data[1] == 1)) {
				delayPacketErrorSim(sendReceivePacket);
				sendReceiveSocket.send(sendReceivePacket);
			} else if ((dataSim && data[0] == 0 && data[1] == 3) || (ackSim && data[0] == 0 && data[1] == 4)) {
				if (blockNumMatch(sendReceivePacket)) {
					System.out.println("Block numbers matched for duplicate");
					delayPacketErrorSim(sendReceivePacket);
					sendReceiveSocket.send(sendReceivePacket);
				} else
					sendReceiveSocket.send(sendReceivePacket);
			} else
				sendReceiveSocket.send(sendReceivePacket);

		}

		if (lostSim) {
			if ((wrqSim && data[0] == 0 && data[1] == 2) || (rrqSim && data[0] == 0 && data[1] == 1))
				lostPacketErrorSim(sendReceivePacket);
			else if ((dataSim && data[0] == 0 && data[1] == 3) || (ackSim && data[0] == 0 && data[1] == 4)) {
				if (blockNumMatch(sendReceivePacket)) {
					System.out.println("Block numbers matched for duplicate");
					lostPacketErrorSim(sendReceivePacket);
				} else
					sendReceiveSocket.send(sendReceivePacket);
			} else
				sendReceiveSocket.send(sendReceivePacket);

		}

		if (TIDChange) {
			if ((dataSim && data[0] == 0 && data[1] == 3) || (ackSim && data[0] == 0 && data[1] == 4)) {
				if (blockNumMatch(sendReceivePacket)) {
					System.out.println(
							"TD has been Changed................................................................");
					TIDChange = false;

					TIDchangeErrorSim(sendReceivePacket);
					return;
				} else
					sendReceiveSocket.send(sendReceivePacket);
			} else
				sendReceiveSocket.send(sendReceivePacket);
		}
		if(packetError) {//user wants to simulate a packet error either blockNum change, invalid mode, or invalid packet
			//check if user wants to invalidate packet

			if(invalidatePacket) {
				//invalidate rrq or wrq... or data or acknowledge
				if(((data[0]==0 && (data[1]==2 || data[1]==1))&& (packetNum<-1)) ||/* <-- invalidate rrq or wrq*/
						((data[0]==0&&data[1]==3||data[0]==0 && data[1]==4)&&blockNumMatch(sendReceivePacket))){/*<-- invalidate data or ack*/ 
					byte[] tempData=sendReceivePacket.getData();
					tempData[1]=7;//this is an invalid opcode packet now invalid
					sendReceivePacket=new DatagramPacket(tempData, tempData.length,sendReceivePacket.getAddress(), sendReceivePacket.getPort());

					System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~Invalidating Packet~~~~~~~~~~~~~~~~~~~~~~~~");

					invalidatePacket=false;
				}

			}
			//check if user wants to invalidate mode
			else if(invalidateMode) {
				if(data[0]==0 &&(data[1]==1||data[1]==2)) {//if its a rrq or wrq and invalidateMode is true then change mode to be false

					byte[] tempData=sendReceivePacket.getData();

					for(int i =0;i<data.length;i++) {
						if(i>1)
							tempData[i]=1;
					}
					System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~Invalidating Mode~~~~~~~~~~~~~~~~~~~~~~~~");
					sendReceivePacket=new DatagramPacket(tempData, tempData.length,sendReceivePacket.getAddress(), sendReceivePacket.getPort());

					invalidateMode=false;
				}
			}
			//check if user wants to invalidate blockNum
			else if(changeBlockNum) {
				if((data[0]==0&&data[1]==3||data[0]==0 && data[1]==4)&&blockNumMatch(sendReceivePacket)){/*<-- invalidate data or ack*/ //if its a ack or data pack and blocknum matches then change blocknum
					byte[] tempData=sendReceivePacket.getData();
					System.out.println("Block num before change"+sendReceivePacket.getData()[3]+"tempdata[3]="+tempData[3]);
					if(tempData[2]!=1)
						tempData[2]=1;
					else
						tempData[2]=2;
					if(tempData[3]!=1)
						tempData[3]=1;
					else
						tempData[3]=2;
					//		if()//coming from server
					sendReceivePacket=new DatagramPacket(tempData, tempData.length,sendReceivePacket.getAddress(), sendReceivePacket.getPort());

					//		if()//co,ing from client
					//	sendReceivePacket=new DatagramPacket(tempData, tempData.length, you're awesome, threadPort);
					System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~Changing Block num~~~~~~~~~~~~~~~~~~~~~~~~");
					System.out.println("Block num after change"+sendReceivePacket.getData()[3]);
					changeBlockNum=false;
				}
			}

			sendReceiveSocket.send(sendReceivePacket);
		}
		if (!duplicateSim && !delaySim && !lostSim && !TIDChange && !packetError) {
			sendReceiveSocket.send(sendReceivePacket);
		}

	}

	public void lostPacketErrorSim(DatagramPacket packet) throws IOException {

		try {
			sendReceiveSocket.setSoTimeout(5000);
			sendReceiveSocket.receive(sendReceivePacket);

		} catch (SocketTimeoutException se) {
			// se.printStackTrace();
			System.out.println("\nNOTHING RECEIVED YET, MAYBE WE LOST A PACKET ......RETRYING........\n");
			sendReceiveSocket.send(sendReceivePacket);
		}
	}

	public boolean blockNumMatch(DatagramPacket packet) {
		byte[] blockNumber = { packet.getData()[2], packet.getData()[3] };
		int blockNum = byteArrToInt(blockNumber);

		return blockNum == packetNum;
	}

	private int byteArrToInt(byte[] blockNumber) {

		return ((byte) (blockNumber[0] & 0xFF) | (byte) ((blockNumber[1] >> 8) & 0xFF));

	}

	public void delayPacketErrorSim(DatagramPacket packet) throws InterruptedException {

		TimeUnit.SECONDS.sleep(delayTime);

	}

	private void duplicatePacketErrorSim(DatagramPacket packet) throws InterruptedException, IOException {
		TimeUnit.SECONDS.sleep(delayTime);
		sendReceiveSocket.send(packet);
		duplicateSim = false;
	}

	private void TIDchangeErrorSim(DatagramPacket packet) {
		try {
			tempSocket = new DatagramSocket();
			tempSocket.send(packet);
		} catch (SocketException se) {
			se.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void operationSetup() throws UnknownHostException {
		Scanner input = new Scanner(System.in);

		System.out.println("This computer address is: " + InetAddress.getLocalHost() + " Enter IP address of server:");
		inetAddressServer = InetAddress.getByName(input.next());
		inetAddressClient = InetAddress.getLocalHost();

		System.out.println(
				"Choose an operation. 0: normal operation, 1: lose a packet, 2: delay a packet, 3: duplicate a packet, 4: Illegal TFTP operation 5: Unknown TID");
		int chosenOperation = input.nextInt();

		int packetTypeToLose = -1;
		int packetTypeToDuplicate = -1;
		int packetTypeToDelay = -1;
		int packetTypeToTID = -1;
		int packetTypeToInvalidate=-1;
		int invalidateResponse=-1;
		switch (chosenOperation) {

		case 1:
			System.out.println("Do you want to lose a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToLose = input.nextInt();
			if (packetTypeToLose == 2 || packetTypeToLose == 3) {
				System.out.println("Which number DATA or ACK packet should be lost during transfer: ");
				packetNum = input.nextInt();
			}
			break;
		case 2:
			System.out.println("Which packet should be delayed: 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToDelay = input.nextInt();
			if (packetTypeToDelay == 2 || packetTypeToDelay == 3) {
				System.out.println("Which number DATA or ACK packet should be delayed during transfer: ");
				packetNum = input.nextInt();
			}
			System.out.println("How long should the delay be in seconds: ");
			delayTime = input.nextInt();
			break;
		case 3:
			System.out.println("Do you want to duplicate a 0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToDuplicate = input.nextInt();
			if (packetTypeToDuplicate == 2 || packetTypeToDuplicate == 3) {
				System.out.println("Which number DATA or ACK packet should be duplicated during transfer: ");
				packetNum = input.nextInt();
			}
			System.out.println("How long should the delay be in seconds: ");
			delayTime = input.nextInt();
			break;

		case 4:
			// packet error

			System.out.println("Which packet type would you like to invalidate\n0: RRQ, 1: WRQ, 2: DATA, or 3: ACK");
			packetTypeToInvalidate=input.nextInt();
			if(packetTypeToInvalidate==1||packetTypeToInvalidate==0) {
				System.out.println("Would you like to make the request mode invalid?\n 0:Yes 1:No");
				invalidateResponse=input.nextInt();
				if(invalidateResponse==0) {
					invalidateMode=true;
				}
			}
			if(!(invalidateMode)&&(packetTypeToInvalidate==2 || packetTypeToInvalidate==3)) {
				System.out.println("Would you like to change the block number or opcode?\n 0: Block number 1: Opcode");
				invalidateResponse=input.nextInt();
				if(invalidateResponse==0) {
					changeBlockNum=true;
					System.out.println("Which block number would you like to change");
					packetNum=input.nextInt();
				}
			}
			if(!invalidateMode && !changeBlockNum) {
				System.out.println("The opcode will be changed.");
				invalidatePacket=true;
				if((packetTypeToInvalidate==0 ||packetTypeToInvalidate==1))
					packetNum=-2;
				else {
					System.out.println("Which data or ack number would you like to make invalid");
					packetNum=input.nextInt();
				}
			}
			break;

		case 5:
			// which packet do you want the to send from different port.
			System.out.println("Do you want to change TID for 2: DATA, or 3: ACK");
			packetTypeToTID = input.nextInt();
			System.out.println("Which number DATA or ACK packet should be duplicated during transfer: ");
			packetNum = input.nextInt();
			break;
		}

		input.close();

		if (packetTypeToLose == 0 || packetTypeToDelay == 0 || packetTypeToDuplicate == 0)
			rrqSim = true;
		else if (packetTypeToLose == 1 || packetTypeToDelay == 1 || packetTypeToDuplicate == 1)
			wrqSim = true;
		else if (packetTypeToLose == 2 || packetTypeToDelay == 2 || packetTypeToDuplicate == 2 || packetTypeToTID == 2)
			dataSim = true;
		else if (packetTypeToLose == 3 || packetTypeToDelay == 3 || packetTypeToDuplicate == 3 || packetTypeToTID == 3)
			ackSim = true;
		if (chosenOperation == 1)
			lostSim = true;
		if (chosenOperation == 2)
			delaySim = true;
		if (chosenOperation == 3)
			duplicateSim = true;
		if(chosenOperation==4)
			packetError=true;
		if (chosenOperation == 5)
			TIDChange = true;

	}

	public byte[] intToBytes(int num) {
		byte[] numAsBytes = new byte[2];
		numAsBytes[1] = (byte) (num & 0xFF);
		numAsBytes[0] = (byte) ((num >> 8) & 0xFF);
		return numAsBytes;
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		IntermediateHost host = new IntermediateHost();
		host.run();
	}
}