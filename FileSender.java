import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.CRC32;


 class FileSender extends TimerTask {
	
	final int DATA_BYTE_PKT = 984;
	final static String ACK = "ACK";
	final static String NAK = "NAK";
	final static String COR = "CORRUPTED";
	
	
	public static DatagramSocket socket ;
	public static DatagramPacket pkt ;
	public static int resendNumber;
	 static Timer timer = new Timer(); 
	
	int totalWindow;
	static int portNumber;
	int lastPacketSize;
	static int sequenceNumber = 0;
	InetAddress hostAddress;
	//byte[] sendPacketByte = new byte[1000]; //concat in window
	byte[] sendLastByte; //send last byte
	byte[] sendEofByte; //send EOF byte
	byte[] totalByte;
	byte[] fileNameByte;
	byte[] eofByte; 
	
	public static void main(String[] args) throws IOException, InterruptedException 
	 {
	    	
	        // check if the number of command line argument is 4
	        if (args.length != 3) 
	        {
	            System.out.println("Usage: java FileSender <path/filename> "
	                                   + "<rcvPort> <rcvFileName>");
	            System.exit(1);
	        }
	        
	        new FileSender(args[0], "Localhost", args[1], args[2]);
	 }


    public  FileSender(String fileToOpen, String host, String port, String rcvFileName) throws IOException, InterruptedException 
    {
    	//initialize
    	//********
    	totalByte = getTotalByteInFile(fileToOpen);
    	totalWindow = getTotalWindow(totalByte);
    	lastPacketSize = getLastPacket(totalByte);
    	fileNameByte = rcvFileName.getBytes();
    	eofByte = "eof".getBytes();
    	hostAddress = InetAddress.getByName(host);
        portNumber = Integer.parseInt(port);
        socket = new DatagramSocket(); 
        //*********
       
        	//send file name
        	sendFile(fileNameByte, portNumber , hostAddress);
        
        	
        for(int i = 0 ; i < totalWindow ; i++)
        {
        	//send data
        	byte[] pktFile = chopIntoPacket(i,totalByte);
        	
        	sendFile(pktFile, portNumber , hostAddress);   	
        
        }
        	// send last packet
        	byte[] pktFile = Arrays.copyOfRange(totalByte, (totalWindow*DATA_BYTE_PKT), totalByte.length);
        	sendFile(pktFile, portNumber , hostAddress); 
        	
        	// send EOF
        	sendFile(eofByte, portNumber , hostAddress); 
   
        	System.out.println("Finish Sending");
        	System.exit(0);
    }
    
    public FileSender()
    {
    	
    }
    
    private byte[] chopIntoPacket(int i , byte[] totalByte) 
    {
		byte[] pktFile = Arrays.copyOfRange(totalByte, i*DATA_BYTE_PKT, (i+1)*DATA_BYTE_PKT);	
    	return pktFile;
	}

    

    public static void sendFile(byte[] sendFileByte, int portNumber , InetAddress hostAddress) throws IOException
    {	 
    	boolean resend = true;
    	  	
    	String status;
    	sendFileByte = packetService(sendFileByte);
		pkt = new DatagramPacket(sendFileByte , sendFileByte.length , hostAddress , portNumber);
		timer = new Timer();
		timer.schedule(new FileSender(), 0, 1);
		
    	while(resend)
    	{	
    		
    		status = waitForReply();
    		
    		if(status.equals(NAK) || status.equals(COR))
    		{
    			resend = true;
    			
    		}
    		if(status.equals(ACK))
    		{
    			resend = false;
    			timer.cancel();
    		}
    	}
    	try
    	{
    		timer.cancel();
    	}catch(Exception e)
    	{
    		
    	}
    	
    	
    }
    
    //Wait for receiver to reply 
    public static String waitForReply() throws IOException
    {
    	    	
    	byte[] buffer = new byte[1000];
    	DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
	    	//start timer
	    socket.receive(reply);
	   
        
        byte[] IncomeData = new byte[reply.getLength()];
        IncomeData = Arrays.copyOfRange(reply.getData(), 0, reply.getLength());
        
        String status = checkReply(IncomeData);

		return status;	
    }
    
    
    
    @Override
	public void run() 
    {
    	try 
    	{
			socket.send(pkt);
		} catch (IOException e)
    	{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    //Check receiver reply pkt - ACK / NAK / COR
    private static String checkReply(byte[] incomeData) {
		
    	String sequence = Integer.toString(sequenceNumber-1);
    	String expectedAck = ACK + sequence;
    	//System.out.println(expectedAck);
    	
    	byte[] ackByte = expectedAck.getBytes();
    	byte[] nakByte = NAK.getBytes();
    	
    	String status = COR;
    	
    	if( Arrays.equals(incomeData, ackByte) )
    	{
    		status = ACK;
    		timer.cancel();
    	}
    	else if( Arrays.equals(incomeData, nakByte) )
    	{
    		status = NAK;
    	    
    	}
    	
    	
    	
    	
		return status;
	}


	// repackage every byte
    public static byte[] packetService( byte[] byteArray)
    {
    	byteArray = appendPacketSize(byteArray);
    	byteArray = appendSequenceNumber(byteArray);
    	byteArray = appendChecksum(byteArray);
    	
    	return byteArray;
    }
    
    // append check sum to array : 8
    public static byte[] appendChecksum( byte[] data )
    {
    	
    	CRC32 crc = new CRC32();
		crc.update(data);
    	byte[] checksum = longToBytes(crc.getValue());

    	data = concat(checksum,data);
		
    	return data; 
    }
    
    // append sequence number : 4 byte
    private static byte[] appendSequenceNumber(byte[] data)
    {
    	
       ByteBuffer buffer = ByteBuffer.allocate(4);
       buffer.putInt(sequenceNumber);
 	   byte[] packetSequence = buffer.array();
 	   
	   data = concat(packetSequence,data);
	 
	   sequenceNumber++;
    
 	   return data;
    }
    
    // append packet Size : 4 byte
   private static byte[] appendPacketSize(byte[] data)
   {
	   ByteBuffer buffer = ByteBuffer.allocate(4);
	   buffer.putInt(data.length);
   	   byte[] packetSize = buffer.array();
   
   	   data = concat(packetSize,data);
	   return data;
   }
   
   
    private int getLastPacket(byte[] allByte) {
    	
    	int total = allByte.length % DATA_BYTE_PKT;  	
		return total;
	}

	private int getTotalWindow(byte[] allByte) {
		
    	int total = allByte.length / DATA_BYTE_PKT;  	
		return total;
	}

	public byte[] getTotalByteInFile(String fileToOpen) throws IOException
    {
    	byte[] allByte = Files.readAllBytes(Paths.get(fileToOpen));
		return allByte;
    }
    
    
    //combine 2 byte array
    public static byte[] concat(byte[] a, byte[] b) 
    {
    	   int aLen = a.length;
    	   int bLen = b.length;
    	   byte[] c= new byte[aLen+bLen];
    	   System.arraycopy(a, 0, c, 0, aLen);
    	   System.arraycopy(b, 0, c, aLen, bLen);
    	   return c;
    }
    
    //convert long to bytes
    /*
    public static byte[] longToBytes(long x) {
    	
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(x);
	    return buffer.array();
	}*/

    public static byte[] longToBytes(long l) {
    	  byte b[] = new byte[8];
    	  
    	  ByteBuffer buf = ByteBuffer.wrap(b);
    	  buf.putLong(l);
    	  return b;
    	  
    	}
}
