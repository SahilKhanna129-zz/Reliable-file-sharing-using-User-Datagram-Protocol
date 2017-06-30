/* Importing reader classes
 * and Exception classes
 */
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.net.InetAddress;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class Client {
    
    private static int[] iD = new int[100];
    private static double[] value = new double[100];
    private final static String FILE_LOCATION = "J:\\Personal Documents\\ENTS 640\\UDP Socket\\data.txt";
    
    /*
     * This method randomly returns the measurement ID.
     */
    
    private static int getMeasurementID() {
        /*
         * Storing the measurement ID and measurement value
         * in different arrays named iD and value.
         */
        try (BufferedReader bufferReader = new BufferedReader(new FileReader(FILE_LOCATION))){
            
            String line = null; // Variable to hold the read line data
            int index = 0;
            
            while ((line = bufferReader.readLine()) != null) {
                
                String[] parts = line.split("\\s"); // Split the string and store both parts in an array
                iD[index] = Integer.parseInt(parts[0]);
                value[index++] = Float.parseFloat(parts[1]);
                }
            
            } catch (IOException e) {
             System.out.println("Error while reading file, Please check the location"); 
            }
        Random num = new Random();
        int measurementID = num.nextInt(100); // generate random number to get random measurement ID
        return iD[measurementID];
        
    }
    
    /* Check the error in the message
     * by comparing checksum
     */
    private static boolean integrityCheck(String requestMessage) {
        
        int checksum;
        String messageWithChecksum = requestMessage.replaceAll("\\s", "").replaceAll("\\r", "")
                .replaceAll("\\n", "").replaceAll("\\t", ""); // ignoring the extra spaces in the response message for calculation
        /*
         * Separating checksum
         */
        int ind = messageWithChecksum.length() - 1; // variable to track the character in the char array 
        char[] messageWithChecksumCharArray = messageWithChecksum.toCharArray(); // convert the message in char array
        /*
         * find the index of the checksum code
         * separate the checksum code from the message
         */
        try {
        while (Character.isDigit(messageWithChecksumCharArray[ind--]));
        String checksumString = requestMessage.substring(ind + 2, messageWithChecksum.length());
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
     * of the received message without checksum
     */ 
    private static int calculateChecksum(String messageWithoutChecksum) {
        int[] characterValue; // array for storing character's ASCII values.
        
        String message = messageWithoutChecksum.replaceAll("\\s", "").replaceAll("\\r", "")
                .replaceAll("\\n", "").replaceAll("\\t", ""); // ignoring all spaces and next lines.
        
        char[] messageCharacters = message.toCharArray(); // making an character array of the String to find ASCII value.
        
        /*
         * Declaring the size of the array according to the number of characters
         * if odd number of characters than size of the array will be incremented by 1
         * if even number of characters than size of the array will remain same as that of message length
         */
        if (messageCharacters.length % 2 == 0)
            characterValue = new int[messageCharacters.length];
        else
            characterValue = new int[messageCharacters.length + 1];
        
        /*
         * loop to store ASCII values of all the characters in the message string.
         */
        for (int j = 0; j < messageCharacters.length; j++)
            characterValue[j] = (int) messageCharacters[j]; // making an integer array which consists the ASCII value of all the message characters
        
        if (messageCharacters.length % 2 != 0)
            characterValue[messageCharacters.length] = 0; // if odd number of characters than add zero as the last element.
        
        int[] unsignedWord= new int[characterValue.length/2];
        int wordLocation = 0;
        
        /*
         * loop to create an array of 16 bit unsigned words
         */
        for (int i = 0; i < characterValue.length - 1; i = i+2) {
            
            int mostSignificant = characterValue[i];
            int leastSignificant = characterValue[i + 1];
            String leastSignificantBinary = Integer.toString(leastSignificant, 2);
            while ((leastSignificantBinary.length()) != 8) // converting to 8 bit word 
                leastSignificantBinary = 00000000 + leastSignificantBinary;
            String unsigned = new String (Integer.toString(mostSignificant,2) + "" + leastSignificantBinary);
            unsignedWord[wordLocation++] = Integer.parseInt(unsigned, 2);
        }
        
        
       /*
        * calculating checksum with the help of iteration code
        */
        int sum = 0;
        int C = 7919;
        int D = 65536;
        for (int k = 0; k < unsignedWord.length; k++) {
            
            int index = (sum ^ unsignedWord[k]) % 65536;
            sum = (C * index) % D;
        }
        return sum;
    }
    
    public static void main (String[] arg) throws Exception {
        
        InetAddress serverIpAddress = InetAddress.getLocalHost(); // getting IP address of my computer
        byte[] receiveData = new byte[200]; // Array for receiving the data. 
        DatagramPacket receivePacket = new DatagramPacket (receiveData , receiveData.length); // declaring the datagram
        DatagramSocket clientSocket = new DatagramSocket(); // making a socket with random port number allocated
        String receivedResponseMessage, responseMessage;
        boolean resendFlag = true; // flag to re-sent message after integrity check error. 
        char responseCode = '0'; // variable to read response code.
        
        do {
            int measurementID = getMeasurementID();
            do {
                Random num = new Random();
                int requestID = num.nextInt(65536); // generate random 16 bit unsigned number for request code

                /*
                 * Format of the request message
                 */
                String messageWOChecksum = new String("<request>\n\t<id>" + requestID + "</id>\n\t<measurement>"+ measurementID
                        + "</measurement>\n</request>\n");
                String message = messageWOChecksum + calculateChecksum(messageWOChecksum);
                /*
                 * Sending the message over the network
                 */
                byte[] requestMessage = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(requestMessage, requestMessage.length, serverIpAddress, 9996);
                clientSocket.send(sendPacket);
                
                int timeoutInterval = 1000; // initialize timeout interval with 1 second
                
                /*
                 * condition to track timeout interval.
                 */
                try {
                    clientSocket.setSoTimeout(timeoutInterval);
                    clientSocket.receive(receivePacket); 
                    } catch (InterruptedIOException e) {
                        clientSocket.send(sendPacket);
                        try {
                            timeoutInterval *= 2;
                            clientSocket.setSoTimeout(timeoutInterval);
                            clientSocket.receive(receivePacket);
                            } catch (InterruptedIOException r) {
                                clientSocket.send(sendPacket);
                                try {
                                    timeoutInterval *= 2;
                                    clientSocket.setSoTimeout(timeoutInterval);
                                    clientSocket.receive(receivePacket);
                                    } catch (InterruptedIOException t) {
                                        clientSocket.send(sendPacket);
                                        try {
                                            timeoutInterval *= 2;
                                            clientSocket.setSoTimeout(timeoutInterval);
                                            clientSocket.receive(receivePacket);
                                            } catch (InterruptedIOException y) {
                                                    System.out.println("Connection Failure!!");
                                                    System.exit(0);
                                                } // inner catch loop ends
                                    } 
                        }
                } // outer catch loop ends
                   
            /*
             * receive response message
             */
            byte[] serverMessage = receivePacket.getData();
            int receivePacketLength = receivePacket.getLength();
            /*
             * deleting the empty spaces at the end of the receive data byte array
             */
            int i = receivePacket.getLength() - 1;
            while (i >= 0 && serverMessage[i] == 0)
                --i;
            byte[] receiveMessageWOSpacesAtEnds = Arrays.copyOf(serverMessage, i + 1);
            
            receivedResponseMessage = new String(receiveMessageWOSpacesAtEnds);
            responseMessage = receivedResponseMessage.replaceAll("\\s", "").replaceAll("\\r", "").
                    replaceAll("\\n", "").replaceAll("\\t", "");
            int indexOfResponseCode;

            /*
             * if integrity check fails 
             * re-send the message with new random number.
             */
            if (integrityCheck(responseMessage)) {
            /*
             * Separating the response code from the response message
             */
            indexOfResponseCode = responseMessage.indexOf("<code>") + 6;
            responseCode = responseMessage.charAt(indexOfResponseCode);
            
            /*
             * Conditions related to response code
             */
            
            if (responseCode == '1') {
                Scanner in = new Scanner(System.in);
                System.out.println("Integrity check has failed at server end, would you like to resend enter: y/n");
                if (in.next() == "y")
                    resendFlag = true;
                else
                    resendFlag = false;
            }
            else
                resendFlag = false;
            } // if ends
            
            
            } while(resendFlag); // inner do while loop ends
            
            if (responseCode == '0') {
                int startIndexOfMeasurementValue = responseMessage.indexOf("<value>") + 7;
                int endIndexOfMeasurementValue = responseMessage.indexOf("</value>");
                String measurementValueString = responseMessage.substring(startIndexOfMeasurementValue, endIndexOfMeasurementValue);
                double measurementValue = Double.parseDouble(measurementValueString);
                System.out.printf("The measurement value corresponding to measurement ID %d is: %2.2f\n",measurementID, measurementValue);
            }
            
            if (responseCode == '2')
                System.out.println("Error: malformed request. The syntax of the request message is not correct.");
            
            if (responseCode == '3')
                System.out.println("Error: non-existent measurement. "
                        + "The measurement with the requested measurement ID does not exist.");

            
        } while (true);
 
    }
    }

