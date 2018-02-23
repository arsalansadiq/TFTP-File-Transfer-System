import java.net.DatagramPacket;


public class Packet {
	private DatagramPacket packet;
	public Packet(DatagramPacket packet){
		this.packet=packet;
	}
	/*
	 * This method will extract a byte array from a packet
	 * if type is error, it will extract byte array from byte 4 to length
	 * if type is normal, it will extract byte array from byte 2 to length
	 */
	public byte[] getPacketData(){
		int index;
		if((packet.getData()[0]==0 &&packet.getData()[1]==1) || (packet.getData()[0]==0 &&packet.getData()[1]==2))//rrq or wrq:data starts at byte 3
			index=2;
		else //error, ack, or data : actual data starts at byte 5
			index=4;
		int stringLength=0;
		for (;packet.getData()[index]!=0;index++){
			stringLength++;
		}
		byte[] packetData = new byte[stringLength];
		System.arraycopy(packet.getData(), index, packetData, 0, stringLength);
		return packetData;
	}
	public byte[] setCode(Integer first, Integer second){
		byte[] code = new byte[2];
		code[0]=first.byteValue();
		code[1]=second.byteValue();
		return code;
	}
	/**
	 * 
	 * @param type :should be 2 bytes, signals what type of packet it is
	 * @param code :if ack, data, or error has block number or error code
	 * @param data :data associated with given type
	 */
	public void setPacketData(byte[] type, byte[] code, byte[] data){
		int index;
		if((type[0]==0 &&type[1]==1) || (packet.getData()[0]==0 &&packet.getData()[1]==2))//rrq or wrq:data starts at byte 3
			index=2;//read and write data starts at third byte
		else //error, ack, or data : actual data starts at byte 5
			index=4;
		byte[] packetData = new byte[data.length+index];//the size of this packet will be the data length plus the size of the opcode
		for(int i=0;i<packetData.length;i++){
			if(i<index){
				if (i<2)
					packetData[i]=type[i];				
				else
					packetData[i]=code[i-2];
			}
			else{
				packetData[i]=data[i-index];
			}
		}
		//if error packet, rrq, or wrq must add a terminating zero
		if(type[1]== 0 || type[1]==1 || type[1]==5){
			byte[] temp = new byte[packetData.length];
			System.arraycopy(packetData, 0, temp, 0, packetData.length-1);
			packetData=new byte[temp.length+1];
			System.arraycopy(temp, 0, packetData, 0, temp.length-1);
			packetData[packetData.length-1]=0x0;
		}
		packet = new DatagramPacket(packetData, packetData.length, packet.getAddress(), packet.getPort());
	}
	/**
	 * this version should just be called by wrq, rrq, calls standard setPacketData, and then sets mode after
	 * @param type
	 * @param code
	 * @param data
	 * @param mode
	 */
	public void setPacketData(byte[] type, byte[] code, byte[] data, byte[] mode){
		setPacketData(type, code, data);
		byte[] newPacketData = new byte[packet.getData().length+mode.length+1];
		System.arraycopy(packet.getData(), 0, newPacketData, 0, packet.getData().length);
		System.arraycopy(mode, 0, newPacketData, packet.getData().length-1, mode.length);//from beginning of mode of length mode to the end of the old packet data
		newPacketData[newPacketData.length-1]=0x0;
		packet = new DatagramPacket(newPacketData, newPacketData.length, packet.getAddress(), packet.getPort());
	}

	/**
	 * 
	 * @return returns error code or block number in packet
	 * *should only be called if packet is of error type, ack, or data
	 */
	public byte[] getPacketCode(){
		byte[] temp = new byte[2];
		temp[0]=packet.getData()[2];
		temp[1]= packet.getData()[3];
		return temp;
	}
	/**
	 * 
	 * @return the op code of the packet
	 */
	public byte[] getPacketType(){
		byte[] temp = new byte[2];
		temp[0]=packet.getData()[0];
		temp[1]= packet.getData()[1];
		return temp;
	}
	public DatagramPacket usePacket(){
		return packet;
	}
}
