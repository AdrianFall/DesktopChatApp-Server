import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;


public class ServerThread implements Runnable {
	private Socket link;
	private String userName;
	private JFrame window;
	private JTextArea serverMessagesTextArea;
	private Scanner networkInput;
	private PrintWriter networkOutput;
	
	/**
	 * Constructor for the class ServerThread
	 * @param link - The link Socket between the Server and Client.
	 * @param userName - The username of the client.
	 * @param window - The JFrame of the server window.
	 * @param area - the JTextArea of the server messages area.
	 */
	public ServerThread(Socket link, String userName, JFrame window, JTextArea area) {
		this.link = link;
		this.userName = userName;
		this.window = window;
		serverMessagesTextArea = area;
		
	}

	/**
	 * Run method of the thread which loops for the incoming requests
	 * from the client and performs an adequate action.
	 * 
	 * Clarification - As I have chosen to use a String for the client request types I have occurred
     * a limitation from the switch statement I wanted to use for performing the actions. The limitation
     * of the switch statement has turned out to be that it would not work with any source below the JRE 1.7, because
     * of the String compliance. Therefore even though I would like to use a switch statement for the clarity I have
     * decided that this limitation will affect many java run environments and had to use bunch of if & else if statements.
	 */
	@Override
	public void run() {
		
		try {
			//Create the PrintWriter for this thread's link, for sending messages to its client
			networkOutput = new PrintWriter(link.getOutputStream(),true);
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		
		try {
			//Call the method to announce the user connection to all connected users, including him
			announceUserConnection();
		} catch (IOException e1) {
			JOptionPane.showMessageDialog(window, "Failed to announce user (" + userName + ") connection.", "announceUserConnection error", JOptionPane.ERROR_MESSAGE);
			e1.printStackTrace();
		}
		
		try {
			//Create network input scanner for incoming messages from the client
			networkInput = new Scanner (link.getInputStream());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(window, "Failed to create networkInput", "InputStream error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		//Loop while the input has a message
		while (networkInput.hasNext()) {
			//Obtain the next line of message and store it in userRequest variable
			String clientRequest = networkInput.nextLine();
			System.out.println("userRequest = " + clientRequest);
			serverMessagesTextArea.append("User " +  userName + " has sent a request for: " + clientRequest + "\n");
			
			//If the user requested a disconnection
			if (clientRequest.equals("disconnect")) {
				//Call the method to disconnect user from this thread
				disconnectUser();
				
				try {
					//Call the method to update the online list
					updateOnlineList();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(window, "Failed to update the online list", "Update online list error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else if (clientRequest.equals("private chat")) { //If the request is to initiate a private chat
				//Obtain the name of the user to be communicated with for private chat
				String userNameToChatWith = networkInput.nextLine();
				serverMessagesTextArea.append(userName + " has requested to privately chat with " + userNameToChatWith + "\n");
				//If the user wants automatically scrolled server messages text area
		    	if (ServerMain.automaticScrolling) {
		    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
		    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
		    	}
				
				try {
					sendRequestForPrivateChat(userNameToChatWith);
				} catch (IOException e) {
					networkOutput.println("Could not request the user " + userNameToChatWith + " for a private chat.");
					networkOutput.flush();
					e.printStackTrace();
				}
			} else if (clientRequest.equals("decline private chat")) { //If the request is to decline a private chat
				
				//Obtain the name of the user that initially requested this user for a private chat
				String userNameToDecline = networkInput.nextLine();
				declinePrivateChat(userNameToDecline);
				
			} else if (clientRequest.equals("accept private chat")) { // If the request is to accept a private chat
				//Obtain the name of the user that initially requested this user for a private chat
				String userNameInitialRequestor = networkInput.nextLine();
				
				//Call a method to accept the private chat
				acceptPrivateChat(userNameInitialRequestor);
				
			} else if (clientRequest.equals("announce closure private chat")) { //If the request is to announce the closure of a private chat
				//Obtain the name of the user that this client has privately chatted with, to announce him that the private chat is closing
				String userToAnnounce = networkInput.nextLine();
				//Obtain the index in users names array list of the user to be announced
				int indexOfUserToAnnounce = ServerMain.usersNamesArrayList.indexOf(userToAnnounce);
				
				try {
					//Create a network output for sending a response to the user to announce
					PrintWriter networkOutputUserToAnnounce = new PrintWriter(ServerMain.socketsArrayList.get(indexOfUserToAnnounce).getOutputStream(),true);
				    //Send a response to the user to announce, to announce him of the closure of private chat
					networkOutputUserToAnnounce.println("close private chat");
					//Send a response to the user to announce, with the name of the user that has closed the chat 
					networkOutputUserToAnnounce.println(userName);
					networkOutputUserToAnnounce.flush();
				} catch (IOException e) {
					JOptionPane.showMessageDialog(window, "Failed to create a PrintWriter for networkOutputUserToAnnounce", "Network output error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else if (clientRequest.equals("private message")) {
				//Obtain the name of the user to send the private message to
				String userPrivChattingWith = networkInput.nextLine();
				//Obtain the private message
				String privMessage = networkInput.nextLine();
				
				try {
					//Call the method to send a private message
					sendPrivateMessage(userPrivChattingWith, privMessage);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(window, "Failed to send a message to " + userPrivChattingWith, "Private message error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			} else if (clientRequest.equals("chat room message")) { //If the request is to send a message to all users
				//Obtain the message and store it in message variable
				String message = networkInput.nextLine();
				
				try {
					//Call the method to send message to all connected users, passing in the message
					sendMessageToAllUsers(message);
				} catch (IOException e) {
					JOptionPane.showMessageDialog(window, "Failed to send messages from user: " + userName, "sendMessageToAllUsers error", JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
				}
			}//End of else if (user request is message)
		}//End of loop while the input has a message
	}//End of run() method
	
	/**
	 * A method for announcing the user connection to all the online users including himself.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void announceUserConnection() throws IOException {
		//Loop for all the users names within the array list
		for (int i = 0; i < ServerMain.usersNamesArrayList.size(); i++) {
			//Initialise a temporary network output for sending messages to currently looped user name (i.e. the position of the user name in array list will always match
			//the position of its socket in the sockets array list)
			PrintWriter tempNetworkOutput = new PrintWriter(ServerMain.socketsArrayList.get(i).getOutputStream(),true);
			//Send the message to the client that this is a chat room message response
			tempNetworkOutput.println("chat room message response");
			//If the looped user name is an user within this thread
			if (ServerMain.usersNamesArrayList.get(i) == userName) {
				//Send the message to the user
				tempNetworkOutput.println("You have connected to the chat.");
			} else { //The looped user name is not within this thread
				//Send the message to the user
				tempNetworkOutput.println(userName + " has connected to the chat.");
			}
			tempNetworkOutput.flush();
		}//End of loop for all users names within array list
		updateOnlineList();
	}//End of announceUserConnection method
	
	/**
	 * A method for disconnecting the user from this thread.
	 * This includes removing him from the ServerMain's array lists
	 * and closing the link Socket.
	 */
	private synchronized void disconnectUser() {
		//Obtain the user position from the users names array list
		int userPositionInArrayList = ServerMain.usersNamesArrayList.indexOf(userName);
		//Remove the user's socket from the array list
		ServerMain.socketsArrayList.remove(userPositionInArrayList);
		//Remove the user's name from the array list
		ServerMain.usersNamesArrayList.remove(userPositionInArrayList);
		
		try {
			//Call a method to announce the user disconnection to all online users
			announceUserDisconnection();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(window, "Failed to announce user (" + userName + ") disconnection.", "announceUserDisconnection error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
		try {
			//Close the link 
			link.close();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(window, "Failed to close the link", "Link closure error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	/**
	 * A method for announcing the user (from this thread) disconnection to all online users
	 * including himself
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void announceUserDisconnection() throws IOException {
		
		//Append the server messages text area with the information that this thread's user has disconnected.
		serverMessagesTextArea.append("User " + userName + " has disconnected from the chat. \n");
		
		//If the user wants automatically scrolled server messages text area
    	if (ServerMain.automaticScrolling) {
    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
    	}
		
    	//Loop for all users within the array list
		for (int i = 0; i < ServerMain.usersNamesArrayList.size(); i++) {
			//Initialise a temporary network output for sending messages to currently looped user name (i.e. the position of the user name in array list will always match
			//the position of its socket in the sockets array list)
			PrintWriter tempNetworkOutput = new PrintWriter(ServerMain.socketsArrayList.get(i).getOutputStream(),true);
			
			//Send a message to the client that this is a chat room message response
			tempNetworkOutput.println("chat room message response");
			
			//Send the message to the currently looped user
			tempNetworkOutput.println(userName + " has disconnected from the chat.");
			tempNetworkOutput.flush();
		}
	}
	/**
	 * A method for sending a message to all users online including this client.
	 * @param message - The message to be sent to all users.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void sendMessageToAllUsers(String message) throws IOException {
		
		//Loop for all users within the array list
		for (int i = 0; i < ServerMain.usersNamesArrayList.size(); i++) {
			//Initialise a temporary network output for sending messages to currently looped user name (i.e. the position of the user name in array list will always match
			//the position of its socket in the sockets array list)
			PrintWriter tempNetworkOutput = new PrintWriter(ServerMain.socketsArrayList.get(i).getOutputStream(),true);
			//Send a message to currently looped user with the server response type, that will make the client realise that this is a message for the chat room
			tempNetworkOutput.println("chat room message response");
			
			//If the currently looped user is the user in this thread
			if (ServerMain.usersNamesArrayList.get(i) == userName) {
				//Send a message to this user
				tempNetworkOutput.println("You said: " + message);
			} else { //The currently looped user is not from this thread
				//Send a message to the user
				tempNetworkOutput.println(userName + " has said: " + message);
			}
			tempNetworkOutput.flush();
		}
	}
	
	/**
	 * A method for updating the user online list via the means of sending a message 
	 * to all users that are online (including this client), about the the change to
	 * the online list and sending an output of online list array.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void updateOnlineList() throws IOException {
		
		//Loop for all users within the array list
		for (int i = 0; i < ServerMain.usersNamesArrayList.size(); i++) {
			//Create a temporary network output for sending messages to currently looped user name (i.e. the position of the user name in array list will always match
			//the position of its socket in the sockets array list)
			PrintWriter tempNetworkOutput = new PrintWriter(ServerMain.socketsArrayList.get(i).getOutputStream(),true);
					
			//Send a message to currently looped user with the server response type, that will make client realise that he needs to update the online list and the list of currently online users.
			tempNetworkOutput.println("online list updated");
			//Send a message to currently looped user with the output of users names array list
			tempNetworkOutput.println(ServerMain.usersNamesArrayList);
			tempNetworkOutput.flush();
		}
	}
	
	/**
	 * A method for sending a request for private chat to a specific user.
	 * @param userNameToChatWith - The username to privately chat with.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void sendRequestForPrivateChat(String userNameToChatWith) throws IOException {
		//Send a message to this thread's client, acknowledging that the request has been received.
		networkOutput.println("You have requested to chat with " + userNameToChatWith + ", please wait till the user accepts the request.");
		networkOutput.flush();
		//Obtain the index of the user to chat with position in the array list
		int indexOfUserToChatWith = ServerMain.usersNamesArrayList.indexOf(userNameToChatWith);
		
		//Create a network output for sending response, to a client to chat with
		PrintWriter networkOutputOfUserToChatWith = new PrintWriter(ServerMain.socketsArrayList.get(indexOfUserToChatWith).getOutputStream(),true);
		//Send a message to the userToChatWith with the server response type, indicating the incoming request to private chat
		networkOutputOfUserToChatWith.println("request private chat");
		//Send a message to userToChatWith with the user name that is requesting him for a private chat
		networkOutputOfUserToChatWith.println(userName);
		networkOutputOfUserToChatWith.flush();
	}
	
	
	/**
	 * A method for sending a private message to an user chatting with,
	 * and to this user (i.e. from this thread/the one who sent the message)
	 * @param userPrivChattingWith - The username of the client chatting with.
	 * @param privMessage - The private message to be send.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 */
	private synchronized void sendPrivateMessage(String userPrivChattingWith, String privMessage) throws IOException {
		//Obtain an index of the user chatting with in the array list
		int indexOfUserChattingWith = ServerMain.usersNamesArrayList.indexOf(userPrivChattingWith);
		
		//Create a network output for sending response, to a client chatting with
		PrintWriter networkOutputOfUserChattingWith = new PrintWriter(ServerMain.socketsArrayList.get(indexOfUserChattingWith).getOutputStream(),true);
		//Send a message to the userChattingWith with the server response type, indicating that a private message is incoming
		networkOutputOfUserChattingWith.println("private message response");
		//Send a message to userChattingWith with the user name that sent the message (i.e. the user name from this thread)
		networkOutputOfUserChattingWith.println(userName);
		//Send a message to userChattingWith with the user name that he's chatting with (i.e. the user name from this thread)
		networkOutputOfUserChattingWith.println(userName);
		//Send the private message to the client chatting with
		networkOutputOfUserChattingWith.println(privMessage);
		networkOutputOfUserChattingWith.flush();
		
		//Send a message to this user with the server response type, indicating that a private message is incoming
		networkOutput.println("private message response");
		//Send a message to this user with the user name that sent the message (i.e. the user name from this thread)
		networkOutput.println(userName);
		//Send a message to this user with the user name that he's chatting with
		networkOutput.println(userPrivChattingWith);
		//Send the private message to this client
		networkOutput.println(privMessage);
		networkOutput.flush();
	}
	
	/**
	 * A method for accepting the private chat.
	 * @param userNameInitialRequestor - The username of the client who initially requested
	 * the private chat
	 */
	private synchronized void acceptPrivateChat(String userNameInitialRequestor) {
		
		//If the user is still online
		if (ServerMain.usersNamesArrayList.contains(userNameInitialRequestor)) {
			//Obtain the index in users names array list of the user that initially requested the private chat
			int indexOfInitialRequestorUser = ServerMain.usersNamesArrayList.indexOf(userNameInitialRequestor);
		
			try {
				//Create a network output for sending a response to the user that initially requested the chat and has been accepted
				PrintWriter networkOutputInitialRequestorUser = new PrintWriter(ServerMain.socketsArrayList.get(indexOfInitialRequestorUser).getOutputStream(),true);
				//Send a response to the user that initially requested the chat, to start the private chat
				networkOutputInitialRequestorUser.println("start private chat");
				//Send the name of the user to chat with
				networkOutputInitialRequestorUser.println(userName);
				networkOutputInitialRequestorUser.flush();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(window, "Failed to create a PrintWriter for networkOutputInitialRequestorUser", "Network output error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		
			//Send a message to the user who accepted the chat, to start the private chat
			networkOutput.println("start private chat");
			//Send the name of the user to chat with
			networkOutput.println(userNameInitialRequestor);
			networkOutput.flush();
		} else { //The user is not online anymore
			networkOutput.println("chat room message response");
			networkOutput.println("The user" + userNameInitialRequestor + " is not online anymore, can't accept the private chat.");
		}
	}//End of acceptPrivateChat method
	
	/**
	 * A method for declining the private chat with an user that 
	 * initially requested the private chat.
	 * @param userNameToDecline - The username of the client that initially requested the private chat.
	 */
	private synchronized void declinePrivateChat(String userNameToDecline) {
		//If the user is still online
		if (ServerMain.usersNamesArrayList.contains(userNameToDecline)) {
			//Obtain the index in users names array list of the user to be declined
			int indexOfUserToDecline = ServerMain.usersNamesArrayList.indexOf(userNameToDecline);
		
			try {
				//Create a network output for sending a message to the user to be declined
				PrintWriter networkOutputUserToDecline = new PrintWriter(ServerMain.socketsArrayList.get(indexOfUserToDecline).getOutputStream(),true);
				//Send a message to the user to decline, with the server response to indicate that he has been declined
				networkOutputUserToDecline.println("private chat declined");
				//Send a message to the user to decline, with the name of the user that has been declined by
				networkOutputUserToDecline.println(userName);
				networkOutputUserToDecline.flush();
			} catch (IOException e) {
				JOptionPane.showMessageDialog(window, "Failed to create a PrintWriter for networkOutputUserToDecline", "Network output error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
			//Send a message to this user with the server response
			networkOutput.println("chat room message response");
			//Send a message to this client that he has successfully declined the private chat
			networkOutput.println("You have declined " + userNameToDecline + " from a private chat.");
			networkOutput.flush();
		
			serverMessagesTextArea.append(userName + " has declined to privately chat with " + userNameToDecline + "\n");
			//If the user wants automatically scrolled server messages text area
			if (ServerMain.automaticScrolling) {
				//Automatically scroll the serverMessagesTextArea to the bottom for the user.
				serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
			}
		} else { //If the user is not online anymore
			networkOutput.println("chat room message response");
			networkOutput.println("The user" + userNameToDecline + " is not online anymore, can't decline the private chat.");
		}
	}//End of declinePrivateChat method
}
