import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

public class FileReceiver {
	
	final static String ACK = "ACK";
	final static String NAK = "NAK";
	final static String COR = "CORRUPTED";
	public static DatagramSocket sk; 
	public static DatagramPacket pkt;
	public static int rcvPortNumber;
	public static InetAddress rcvAddress;
	public static String fileName;
	public static int currSeq = -1;
	public static int trueSeq = 0;
	public static FileOutputStream fos;
	public static BufferedOutputStream bos;
	public static byte[] dataToWrite;
	
	public static void main(String[] args) throws IOException {
		// Input validation
		if (args.length != 1) {
			System.err.println("Usage: FileReceiver <port>");
			System.exit(-1);
		}
		int port = Integer.parseInt(args[0]);
		new FileReceiver(port); 
	}
	
	public FileReceiver(int port) throws IOException {
		boolean listening = true;
		System.out.println("Listening on port " + port);
		sk = new DatagramSocket(port);
		
		try {
			while (listening) {
				listening = receiveFile(port);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Call this method to start receiving packets from FileSender
	public static boolean receiveFile(int portNumber) throws Exception {
		byte[] buf = new byte[1000];
		boolean resend = true;
		boolean next = true;
		boolean repeat = false;
		  
		int seqNum = -1;
		int size;
		String endOfFile = "";
		
		while(resend) {
			DatagramPacket incoming = new DatagramPacket(buf, buf.length);
			sk.receive(incoming);
			rcvPortNumber = incoming.getPort();
			rcvAddress = incoming.getAddress();
			
			buf = new byte[incoming.getLength()];
			buf = Arrays.copyOfRange(incoming.getData(), 0 , incoming.getLength());
			resend = inspectChecksum(buf);
			seqNum = inspectSeqNum(buf);
			
			if (resend) {
				sendNAK();
			}
			else {
				sendACK(seqNum);
			}  	
		}
		
		seqNum = inspectSeqNum(buf);
		if(currSeq + 1 == seqNum) {
			currSeq = seqNum;
		}
		else { 
			repeat = true;
		}
		size = inspectSize(buf);
		   
		if(!repeat) {
			trueSeq++;
			System.out.println("Current packet: " + currSeq);
			//System.out.println("Data length : "+ size);
		}
		   
		byte[] fileData = Arrays.copyOfRange(buf, 16, buf.length); 
		   
		if(size < 100) {
			endOfFile = new String(fileData);
		}
		   
		if(currSeq == 0 && !repeat) {
			fileName = new String(fileData);
			System.out.println("Writing " + fileName);
			fos = new FileOutputStream(fileName);
			bos = new BufferedOutputStream(fos);
		}
		   
		else if(currSeq > 0 && !endOfFile.contains("EOF") && !repeat) {
			dataToWrite = Arrays.copyOf(fileData, fileData.length);
			writeData(dataToWrite);
		}
		
		if(endOfFile.contains("EOF") && currSeq == seqNum) {
			//System.out.println("Calling the closeStreams() method...");
			closeStreams();
			for(int i = 0 ; i < 10 ; i++) {
				sendACK(currSeq);
			}
			System.out.println("Finish Writing File!");
			System.exit(0);
			next = false;
		}
		return next;	   
	}
	
	private static int inspectSize(byte[] data) {
		byte[] sizeByte = Arrays.copyOfRange(data,12,16);
		return byteArrayToInt(sizeByte);
	}
	   
	private static int inspectSeqNum(byte[] data) {
		byte[] sequenceByte = Arrays.copyOfRange(data,8,12);
		return byteArrayToInt(sequenceByte);
	}
	   
	private static boolean inspectChecksum(byte[] incoming) {
		boolean resend = true;
		CRC32 crc = new CRC32();
		byte[] data= Arrays.copyOfRange(incoming, 8 , incoming.length);
		crc.update(data);
		byte[] chksum = Arrays.copyOfRange(incoming,0,8);
		ByteBuffer b = ByteBuffer.wrap(chksum);
		   
		if(b.getLong() == crc.getValue()) {
			resend = false;
		}   
		return resend;
	}
  
	public static int byteArrayToInt(byte[] b) {
		int integer = ByteBuffer.wrap(b).getInt();
	    return integer;
	}
	
	public static void writeData(byte[] file) throws IOException {
		bos.write(file);
	    System.out.println("Just written packet no. " + trueSeq);
	}
	
	public static void closeStreams() throws IOException {
		bos.close();
		fos.close();
		System.out.println("Closed streams");
	}

	public static void sendACK(int sequence) throws IOException {
		String sequenceNumber = Integer.toString(sequence);
		String ack_sequence = ACK + sequenceNumber;
		byte[] replyData = ack_sequence.getBytes();
		pkt = new DatagramPacket(replyData , replyData.length , rcvAddress , rcvPortNumber);
		sk.send(pkt);
	}
	
	public static void sendNAK() throws IOException {
		byte[] replyData = NAK.getBytes();
		pkt = new DatagramPacket(replyData , replyData.length , rcvAddress , rcvPortNumber);
		sk.send(pkt);  		   
	}
}
