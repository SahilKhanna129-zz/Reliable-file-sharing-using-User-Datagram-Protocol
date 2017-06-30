import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
/* Importing reader classes
 * and Exception classes
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class Server {
    
    private static int[] iD = new int[100];
    private static double[] value = new double[100];
    private final static String FILE_LOCATION = "J:\\Personal Documents\\ENTS 640\\UDP Socket\\data.txt";
    
    /* look into the array of data
     * return corresponding measurement value
     */
    private static double getMeasurementValue(int measurementID) {
    	
    	int ind = 0;
        for (int i = 0; i < 100; i++)
        	if(iD[i] == measurementID) {
        		ind = i;
        		break;
        	}
        return value[ind];
    } // get function ends
    
    /*
     * Presence of measurement ID
     */
    
    private static boolean checkMeasurementIDMatch (int measurementID) {
    	/*
    	 * Storing the measurement ID and measurement value
    	 * in different arrays named iD and value.
    	 */
    	try (BufferedReader bufferReader = new BufferedReader(new FileReader(FILE_LOCATION))){
            
            String line = null; // Variable to hold the read line data
            int index = 0;
            
            while ((line = bufferReader.readLine()) != null) {
                
                String[] parts = line.split("\\s"); // Split the string and store both parts in the array
                iD[index] = Integer.parseInt(parts[0]);
                value[index++] = Float.parseFloat(parts[1]);
                }
            
            } catch (IOException e) {
             System.out.println("Error while reading file, Please check the location"); 
            }
    	/*
    	 * Checking whether the file consists the ID
    	 */
		for (int j = 0; j < 100 ; j++) {
			if (measurementID == iD[j])
				return true;
		}
		return false;
	} // check function ends
     
    /*
     * Checking the syntax of the response message
     */
    private static boolean syntaxCheck(String aRequestMessage) {
    	String requestMessage = aRequestMessage.replaceAll("\\s", "")
    			.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", "");
    	int indexOfChecksum = requestMessage.indexOf("</request>") + 10;
        String requestMessagewithoutChecksum = requestMessage.substring(0, indexOfChecksum);
        /*
         * If measurement Id has a character instead of integer
         * which can occur while communication then
         * parse integer method will throw an exception.
         * This will be taken care by returning false in catch
         */
        try {
        if (requestMessagewithoutChecksum.indexOf("<request><id>") == 0) {
                if (requestMessagewithoutChecksum.indexOf("</id>") != -1) {
                    int indexOfEndID = requestMessagewithoutChecksum.indexOf("</id>");
                    if (indexOfEndID > 9) {   // to check if </id> is after <id> 
                        String iD = requestMessagewithoutChecksum.substring(13, indexOfEndID);
                        int requestID = Integer.parseInt(iD);
                        if (requestID > 0 && requestID < 65536 && (requestMessagewithoutChecksum.indexOf("<measurement>") == (indexOfEndID + 5))) {
                            int indexOfMeasurementIDEnd = requestMessagewithoutChecksum.indexOf("</measurement>");
                            if (indexOfMeasurementIDEnd > (indexOfEndID + 5)) { // to check if </measurement> is after <measurement>
                                String measurementID = requestMessagewithoutChecksum.substring((indexOfEndID + 18), indexOfMeasurementIDEnd);
                                int measurement = Integer.parseInt(measurementID);
                                if (measurement > 0 && measurement < 65536 && requestMessagewithoutChecksum.indexOf("</request>") == (indexOfMeasurementIDEnd + 14))
                                    return true;
                                else return false;
                            }
                            else return false;
                        }
                        else return false;
                    }
                    else return false;
                }
                else return false;         
        }
        else return false;
        } catch (NumberFormatException e) {
        	return false;
        }
        
    }
   
    
    /* Check the error in the message
     * by comparing checksum
     */
    private static boolean integrityCheck(byte[] receiveMessage, int packetLength) {
    	int checksum;
        /*
         * deleting the empty spaces at the end of the received byte array
         */
        int i = packetLength - 1;
        while (i >= 0 && packetLength == 0)
            --i;
        byte[] receiveMessageWOSpaces = Arrays.copyOf(receiveMessage, i + 1);
        String receivedRequestMessage = new String(receiveMessageWOSpaces); // converting byte message into string message
            
    	String messageWithChecksum = receivedRequestMessage.replaceAll("\\s", "").replaceAll("\\r", "")
    			.replaceAll("\\n", "").replaceAll("\\t", ""); // ignoring the extra spaces in the response message for calculation
    	
    	int ind = messageWithChecksum.length() - 1; // variable to track the character in the char array 
        char[] messageWithChecksumCharArray = messageWithChecksum.toCharArray(); // convert the message into char array
        /*
         * find the index of the checksum code
         * separate the checksum code from the message
         */
        try {
        while (Character.isDigit(messageWithChecksumCharArray[ind--]));
        String checksumString = messageWithChecksum.substring(ind + 2, messageWithChecksum.length());
        checksum = Integer.parseInt(checksumString);
        } catch (NumberFormatException e) {
            return false; 
            }
        String message = messageWithChecksum.substring(0, ind + 2); // separating message from checksum
        int messageChecksum = calculateChecksum(message); // calculating the checksum by calling function
        /*
         * Comparing the calculated checksum and the checksum in the request message
         */
        if (messageChecksum == checksum)
        	return true;
        else
        	return false;    
    }
    
    /* calculate the checksum
     * of the received message without appended checksum
     */ 
    private static int calculateChecksum(String messageWithoutChecksum) {
        
        String message = messageWithoutChecksum.replaceAll("\\s", "").replaceAll("\\r", "")
        		.replaceAll("\\n", "").replaceAll("\\t", ""); // ignoring all spaces, and new line, carriage feed
        
        char[] messageCharacters = message.toCharArray(); // making an character array of the String to find ASCII value.
        /*
         * Declaring the size of the array according to the number of characters
         * if odd number of characters than size of the array will be incremented by 1.
         * if even number of characters than size of the array will remain the same 
         */
        int[] characterValue;
        if (messageCharacters.length % 2 == 0)
            characterValue = new int[messageCharacters.length];
        else
            characterValue = new int[messageCharacters.length + 1];
        
        for (int j = 0; j < messageCharacters.length; j++)
            characterValue[j] = (int) messageCharacters[j]; // making an integer array consists of ASCII value of all the message characters
        
        if (messageCharacters.length % 2 != 0)
            characterValue[messageCharacters.length] = 0; // if odd number of characters then adding zero at the last element.
        
        int[] unsignedWord= new int[characterValue.length/2];
        int wordLocation = 0;
        
        /*
         * loop creates an array of 16 bit unsigned words
         */
        for (int i = 0; i < characterValue.length - 1; i = i+2) {
        	
            int mostSignificant = characterValue[i];
            int leastSignificant = characterValue[i + 1];
            String leastSignificantBinary = Integer.toString(leastSignificant, 2);
            while ((leastSignificantBinary.length()) != 8) // converting to 8 bits
                leastSignificantBinary = 00000000 + leastSignificantBinary;
            String unsigned = new String (Integer.toString(mostSignificant,2) + "" + leastSignificantBinary);
            unsignedWord[wordLocation++] = Integer.parseInt(unsigned, 2);
        }
        
        int sum = 0;
        int C = 7919;
        int D = 65536;
        for (int k = 0; k < unsignedWord.length; k++) {
            
        	 int index = (sum ^ unsignedWord[k]) % 65536; // to convert integer type calculation to unsigned 16 bit calculation
             sum = (C * index) % D;
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        
        DatagramSocket serverSocket = new DatagramSocket(9996); // allocating the size of the server socket as 9996
        
        byte[] receiveData = new byte[200]; // making an array object for storing the request message bytes
        byte[] sendData;
        
        /*
         * Start the server for receiving the request messages for processing.
         * The server runs until the user terminates the program
         */
        while(true) {
            
        DatagramPacket receivePacket = new DatagramPacket (receiveData, receiveData.length); // declaring the datagram packet to receive data
        serverSocket.receive(receivePacket); // receiving the packet
        /*
         * reading the IP address and port number of the client
         */
        InetAddress ipAddressClient = receivePacket.getAddress();
        int port = receivePacket.getPort();
        
        byte[] receiveMessage = receivePacket.getData(); // reading the request message data
        int receivePacketLength = receivePacket.getLength();
        /*
         * deleting the empty spaces at the end of the receive data byte array
         */
        int i = receivePacket.getLength() - 1;
        while (i >= 0 && receiveMessage[i] == 0)
            --i;
        byte[] receiveMessageWOSpacesAtEnds = Arrays.copyOf(receiveMessage, i + 1);
        
        String receivedRequestMessage = new String(receiveMessageWOSpacesAtEnds); // converting byte message into string message
    	String requestMessage = receivedRequestMessage.replaceAll("\\s", "")
    			.replaceAll("\\r", "").replaceAll("\\n", "").replaceAll("\\t", ""); // removing unwanted characters
        int ind = 0; // variable to track the character in the char array 
		char[] messageWithoutChecksumCharArray = requestMessage.toCharArray(); // convert the message in char array
		/*
		 * find the index of the request code
		 * separate the request code from the message
		 */
		while (!Character.isDigit(messageWithoutChecksumCharArray[ind]))
			ind++;
		int indexOfRequestCodeStart = ind;
		while (Character.isDigit(messageWithoutChecksumCharArray[ind]))
			ind++;
		int indexOfRequestCodeEnd = ind;
		String requestID = requestMessage.substring(indexOfRequestCodeStart, indexOfRequestCodeEnd + 1);
        
        if (integrityCheck(receiveMessage, receivePacketLength)) {
        /*
         * Check the syntax of the request
         * if the request has syntax error
         * server will send the response with a specified error code
         */
        if (!syntaxCheck(receivedRequestMessage)) {
        	/*
        	 * Calculating the message without checksum
        	 */
        	int indexOfChecksum = receivedRequestMessage.indexOf("</request>") + 10;
            String messageWithoutChecksum = receivedRequestMessage.substring(0, indexOfChecksum - 1);
            /*
             * response message format
             */
            String responseMessageWithoutChecksum = new String("<reponse>\n\t<id>" + requestID + "</id>\n\t<code>2</code>\n</response>\n");
            String responseMessageWithChecksum = responseMessageWithoutChecksum + calculateChecksum(responseMessageWithoutChecksum);
            /*
             * converting the string response message into bytes
             * sending over the network
             */
            sendData = responseMessageWithChecksum.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddressClient, port);
            serverSocket.send(sendPacket);
        } // if ends
        else {
	        
	        /*
	         * Calculating the checksum
	         */
	        int indexOfChecksum = requestMessage.indexOf("</request>") + 10;
	        CharSequence requestChecksumSeq = requestMessage.subSequence(indexOfChecksum, requestMessage.length());
	        int requestChecksum = Integer.parseInt(requestChecksumSeq.toString());
	        
	        
	        String messageWithoutChecksum = requestMessage.substring(0, indexOfChecksum - 1); // separating the message from the checksum
	        /*
	         * Separating the measurement ID from the message
	         */
	        int indexOfmeasurementIDBegin = messageWithoutChecksum.indexOf("<measurement>") + 13;
	        int indexOfmeasurementIDEnds = messageWithoutChecksum.indexOf("</measurement>") - 1;
	        String measurementIDString = messageWithoutChecksum.substring(indexOfmeasurementIDBegin, indexOfmeasurementIDEnds + 1);
	        int measurementID = Integer.parseInt(measurementIDString);
	        
	        double measurementValue = 0; // initializing the measurement value variable to store measurement value.
	        /*
	         * Check the data file for the measurement value
	         * if the measurement ID has a corresponding value
	         * then send the value in response message format
	         */
	        if (checkMeasurementIDMatch(measurementID)) {
	        	measurementValue = getMeasurementValue(measurementID);
	        	String responseMessageWithoutChecksum = new String("<reponse>\n\t<id>" + requestID + "</id>\n\t<code>0</code>\n<measurement>"
	                + measurementID + "</measurement>\n<value>" + measurementValue + "</value>\n</response>\n");
	        	String responseMessageWithChecksum = responseMessageWithoutChecksum + calculateChecksum(responseMessageWithoutChecksum);
	        	sendData = responseMessageWithChecksum.getBytes();
	        	DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddressClient, port);
	        	serverSocket.send(sendPacket);
	        }
	        /*
	         * if there is no measurement Id in the file
	         * then send a response with specific error code
	         */
	        else {
	            String responseMessageWithoutChecksum = new String("<reponse>\n\t<id>" + requestID + "</id>\n\t<code>3</code>\n</response>\n");
	            String responseMessageWithChecksum = responseMessageWithoutChecksum + calculateChecksum(responseMessageWithoutChecksum);
	            sendData = responseMessageWithChecksum.getBytes();
	            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddressClient, port);
	            serverSocket.send(sendPacket);
	        }
        }
        }
        else {
        String responseMessageWithoutChecksum = new String("<reponse>\n\t<id>" + requestID + "</id>\n\t<code>1</code>\n</response>\n");
        String responseMessageWithChecksum = responseMessageWithoutChecksum + calculateChecksum(responseMessageWithoutChecksum);
        sendData = responseMessageWithChecksum.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddressClient, port);
        serverSocket.send(sendPacket); 
        }
        } // while loop ends
    
    } // main function ends

} // class ends
