import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import javax.swing.*;


public class ServerMain {

	private static int portNumber = 4444;
	private static JFrame window;
	private static ServerSocket serverSocket;
	private static JTextArea serverMessagesTextArea;
	//Initialise array list for storing/retrieving/manipulating the user names
	public static ArrayList<String> usersNamesArrayList = new ArrayList<String>();
	//Initialise array list for storing/retrieving/manipulating the sockets
	public static ArrayList<Socket> socketsArrayList = new ArrayList<Socket>();
	private static Socket link;
	private static PrintWriter networkOutput;
	private static Scanner networkInput;
	//Declare a boolean for the automatic scrolling of the server messages area
	public static boolean automaticScrolling = true;
	
	/**
	 * The constructor of this class creates the GUI with all the functionality 
	 * for its components and action listeners 
	 */
	public ServerMain() {
		//Create an instance of the JFrame object
		window = new JFrame("Server");
		
		//Add an action listener to the window, so it allows for custom behaviour when closing the window
		window.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				//If the link is still up
				if (link != null) {
					
					try {
						//Call a method to disconnect all users from the server
						disconnectAllUsersFromServer();
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(window, "Could not disconnect all users from the server.", "Disconnect error", JOptionPane.ERROR_MESSAGE);
						e1.printStackTrace();
					}
					System.exit(0);
				}
			}//End of windowClosing method
		});//End of action listener for window
		
		//Set the default operation when user closes the window (frame)
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//Set the size of the window
		window.setSize(600, 600);
		//Do not allow resizing of the window
		window.setResizable(false);
		//Set the position of the window to be in middle of the screen when program is started
		window.setLocationRelativeTo(null);
		
		//Call the setUpWindow method for setting up all the components needed in the window
		window = setUpWindow(window);
		
		//Set the window to be visible
		window.setVisible(true);
	}//End of constructor
	
	/**
	 * The main method of this class
	 * @param args - For example for the command line arguments when running the java class
	 */
	public static void main (String[] args) {
		
		//Initialise the constructor of this class
		new ServerMain();
		
		//Do while loop for listening to the incoming connections
		do {
			System.out.println("about to run service");
			try {
				TimeUnit.MILLISECONDS.sleep(1000);
			} catch (InterruptedException e1) {
				System.out.println("Exception with sleeping/delaying");
				e1.printStackTrace();
			}
			try {
				//Call the runService method to establish the link between client and server
				runService();
				
			} catch (IOException e) {
				if (serverSocket != null) {
					JOptionPane.showMessageDialog(window, "Could not run service/link.", "Service error", JOptionPane.ERROR_MESSAGE);
				}
				e.printStackTrace();
			}
		} while (true);
	}//End of main method
	
	/**
	 * A method for establishing the link with the client
	 * @throws IOException - Throws an exception in the case the service could not be run, i.e. problem withe the serverSocket
	 */
	private static void runService() throws IOException  {
		System.out.println("Hello from runService()");
		//If the server socket exists
		if (serverSocket != null) {
			
			try {
				//Accept the link with the client
				link = serverSocket.accept();
				//Call a method to add the client's link
				addUser(link);
			} catch (IOException e) {
				System.out.println("Catched socket closed.");
			}
		}//End of if the server socket exists
	}//End of runService method
	
	/**
	 * A method for adding a new user, with checking if the username
	 * already exists (i.e. no duplicate names allowed), adding the client's link and 
	 * username to the arraylists and starting a ServerThread for the client.
	 * @param link - The link Socket of the client to be added.
	 * @return - returns the username of the added client.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised.
	 */
	private static String addUser(Socket link) throws IOException {
		
		try {
			//Initialise a network input scanner for obtaining the messages passed from client
			networkInput = new Scanner(link.getInputStream());
		} catch (IOException e) {
			JOptionPane.showMessageDialog(window, "Could not create networkInput", "Input Stream error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
		//Obtain the userName from the message passed from the client
		String userName = networkInput.nextLine();
		
		//Check if the userName already exists in the arrayList
		if (usersNamesArrayList.contains(userName)) {
			serverMessagesTextArea.append("User " + userName + " has attempted to connect to the chat, declined since another user already uses this name \n");
			
			//If the user wants automatically scrolled server messages text area
	    	if (automaticScrolling) {
	    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
	    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
	    	}
			
			//Initialise a network output printWriter for sending the message to a client
			networkOutput = new PrintWriter(link.getOutputStream());
			//Send the server response type to the client
			networkOutput.println("name already used");
			networkOutput.flush();
		} else { //If the userName doesn't exist in arrayList
			
			serverMessagesTextArea.append("User " + userName + " has been connected to the chat \n");
			
			//If the user wants automatically scrolled server messages text area
	    	if (automaticScrolling) {
	    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
	    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
	    	}
			
			//Add the user to the array list
			usersNamesArrayList.add(userName);
			//Add the socket to the array list
			socketsArrayList.add(link);
			System.out.println("Debugging size of socket vs userNames: " + socketsArrayList.size() + " vs " + usersNamesArrayList.size());
		
			//Initialise the thread passing the link, userName, window frame and serverMessagesTextArea into constructor and then start the thread.
			ServerThread thread = new ServerThread(link, userName, window, serverMessagesTextArea);
			Thread t = new Thread(thread);
			t.start();
		}
		return userName;
	}//End of addUser method
	
	/**
	 * A method for disconnecting all currently online users from the server.
	 * @throws IOException - Throws an exception in case the PrintWriter couldn't be initialised
	 * or the socket couldn't be closed.
	 */
	//A method for disconnecting all currently online users from the server
	private static void disconnectAllUsersFromServer() throws IOException {
		serverMessagesTextArea.append("Disconnecting " + usersNamesArrayList.size() + " users." + "\n");
		//Loop for all the users names within the array list
		for (int i = 0; i < usersNamesArrayList.size(); i++) {
			System.out.println("Sending server shut down message to user: " + usersNamesArrayList.get(i));
			//Initialise a temporary network output for sending messages to currently looped user name (i.e. the position of the user name in array list will always match
			//the position of its socket in the sockets array list)
			PrintWriter tempNetworkOutput = new PrintWriter(socketsArrayList.get(i).getOutputStream(),true);
			serverMessagesTextArea.append("Disconnecting user named: " + usersNamesArrayList.get(i) + "\n");
			//Send a message to the currently looped user that the server is shutting down, with the type of server response
			tempNetworkOutput.println("server shutting down");
			//Close the currently looped user's socket
			socketsArrayList.get(i).close();
			//Remove the userName from the array list
			usersNamesArrayList.remove(i);
			//Remove the user's socket from the array list
			socketsArrayList.remove(i);
			//Decrement the index, since the size of array list has just been decremented by one
			i--;
			tempNetworkOutput.flush();
		}
	}//End of disconnectAllUsersFromServer method
	
	/**
	 * A method for setting up the server window, i.e. its layout, components, their positions and action listeners.
	 * @param window - The JFrame of the server window.
	 * @return - Returns a JFrame
	 */
	private static JFrame setUpWindow(final JFrame window) {
		//Create an instance of the JPanel object
    	JPanel panel = new JPanel();
    	//Set the panel's layout manager to null
    	panel.setLayout(null);
    	
    	//Set the bounds of the window
    	panel.setBounds(0, 0, 600, 600);
    	
    	//Create an instance of the JLabel object
    	JLabel serverMessagesLabel = new JLabel("Server messages:");
    	//Set the userNameLabel font
    	serverMessagesLabel.setFont(new Font("Serif", Font.PLAIN, 17));
    	
    	//Create an instance of the JTextArea object
    	serverMessagesTextArea = new JTextArea();
    	//Create an instance of the JScrollPane object, placing the serverMessagesTextArea inside of it
    	JScrollPane scrollServerMessagesTextArea = new JScrollPane(serverMessagesTextArea);
    	
    	//A button for allowing the user to choose whether he wants an automatic scrolling of the chatArea
    	final JButton automaticScrollingButton = new JButton("Disable automatic scrolling");
    	
    	//Create instances of the JButton objects
    	final JButton startServerButton = new JButton("Start Server");
    	final JButton stopServerButton = new JButton("Stop Server");
    	
    	JLabel authorLabel = new JLabel("By Adrian Fall");
    	
    	//Add an action listener for the automaticScrollingButton
    	automaticScrollingButton.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			//If the automatic scrolling is on, change the text of the button to reflect the change and set it to false
    			if (automaticScrolling) {
    				automaticScrollingButton.setText("Enable automatic scrolling");
    				automaticScrolling = false;
    			} else { //If the automatic scrolling is off, change the text of the button to reflect the change and set it to true
    				automaticScrollingButton.setText("Disable automatic scrolling");
    				automaticScrolling = true;
    			}
    		}
    	});
    	
    	//Add an action listener for the startServerButton
    	startServerButton.addActionListener(new ActionListener() {
    		public void actionPerformed(ActionEvent e) {
    			
    			//If the socket already exists
    			if (serverSocket != null) {
					serverMessagesTextArea.append("Server is already running \n");
					
					//If the user wants automatically scrolled server messages text area
			    	if (automaticScrolling) {
			    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
			    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
			    	}
    			} else { //If the socket doesn't exist
    				try {
    					System.out.println("Creating the socket");
    					//Create the socket
						serverSocket = new ServerSocket(portNumber);
    				} catch (IOException e1) {
						JOptionPane.showMessageDialog(window, "Can't attach to the port number " + portNumber, "Port error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}
    				
					serverMessagesTextArea.append("Started the server \n");

					//If the user wants automatically scrolled server messages text area
				    if (automaticScrolling) {
				    	//Automatically scroll the serverMessagesTextArea to the bottom for the user.
				    	serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
				    }
					//Set the startServerButton to disabled, since the server is running
					startServerButton.setEnabled(false);
					//Set the stopServerButton to enabled, since the server is running
					stopServerButton.setEnabled(true);
						
					
    			}//End of else (i.e. socket doesn't exist)
    		}//End of actionPerformed
    	});//End of action listener for startServerButton
    	
    	//Add an action listener for the stopServerButton
    	stopServerButton.addActionListener(new ActionListener() {
    		public void actionPerformed (ActionEvent e) {
    			
    			//If the socket doesn't exist
    			if (serverSocket == null) {
    				serverMessagesTextArea.append("Server is not running. \n");
    				
    				
    			} else { //If the socket exists
    				
    				//If the user wants automatically scrolled server messages text area
    		    	if (automaticScrolling) {
    		    		//Automatically scroll the serverMessagesTextArea to the bottom for the user.
    		    		serverMessagesTextArea.setCaretPosition(serverMessagesTextArea.getDocument().getLength());
    		    	}
    				try {
    					//Call a disconnect all users from server method
    					disconnectAllUsersFromServer();
    					
    					//Close the serverSocket and set it to null
						serverSocket.close();
						System.out.println("setting serverSocket to null");
						serverSocket = null;
						serverMessagesTextArea.append("Stopped the server. \n");
						
						//Set the startServerButton to enabled, since the server is not running
						startServerButton.setEnabled(true);
						//Set the stopServerButton to disabled, since the server is not running
						stopServerButton.setEnabled(false);
					} catch (IOException e1) {
						JOptionPane.showMessageDialog(window, "Could not stop the server.", "Stop server error", JOptionPane.ERROR_MESSAGE);
						System.exit(1);
					}
    			}
    		}
    	});
    	
       	//Add the serverMessagesLabel to the panel
    	panel.add(serverMessagesLabel);
    	//Position the serverMessagesLabel
    	serverMessagesLabel.setBounds(10, 10, 150, 25);
    	
    	//Add the scrollServerMessagesTextArea to the panel
    	panel.add(scrollServerMessagesTextArea);
    	//Position the scrollServerMessagesTextArea
    	scrollServerMessagesTextArea.setBounds(10, 40, 455, 450);
    	//Do not allow the serverMessagesTextArea to be editable
    	serverMessagesTextArea.setEditable(false);
    	//Allow the serverMessagesTextArea to be line wrapped
    	serverMessagesTextArea.setLineWrap(true);
    	
    	//Add the startServerButton to the panel
    	panel.add(startServerButton);
    	//Position the startServerButton
    	startServerButton.setBounds(10, 495, 225, 50);
    	
    	//Add the stopServerButton to the panel
    	panel.add(stopServerButton);
    	//Position the stopServerButton
    	stopServerButton.setBounds(240, 495, 225, 50);
    	
    	//Add the automaticScrollingButton to the panel
    	panel.add(automaticScrollingButton);
    	//Position the automaticScrollingButton
    	automaticScrollingButton.setBounds(262, 12, 200, 25);
    	
    	//Add the authorLabel to the panel
    	panel.add(authorLabel);
    	authorLabel.setBounds(490, 550, 150, 25);
    	
    	//Add the panel to the window
    	window.add(panel);
    	
		return window;
	}
}
