

Deployment instructions
======================
    Configurations
    ===============
    The configurations needed to run the application:
    
    The port number of both client and server need to be the same. By default the port number of both client and server is 4444, in case that this default port is being unusable for the chat application (hence, is already used by some other application) the following needs to be done:
    
    •	Change the portNumber in ServerMain.java, ideally to a port number above the “Well-Known TCP Port Numbers” (i.e. above 1080).
    
    •	Change the portNumber in ClientMain.java, to the same port number that was used for ServerMain.java
    
    Running the application
    =====================
    To run the application follow these instructions:
            Using Windows Console to run the .java files:
    •	Tell the system where to find the JDK, by setting the path e.g. set path=C:\Program Files\Java\jdk1.7.0_45\bin
    •	Compile the .java files using the javac syntax, e.g. “javac ClassName.java” or to compile all .java files in the current directory use “javac *.java”
    •	Run the ServerMain.class using the java syntax, i.e. by typing “java ServerMain” in the Windows console and press the “Start Server” button on the GUI.
    •	Run the ClientMain.class using the java syntax, i.e. by typing “java ClientMain” in the Windows console, insert the username (no longer than 20 characters) and press the “Connect” button on the GUI.
             Using Eclipse to run the .java files:
    •	Create two Java Projects, e.g. calling the first one ChatServer and second ChatClient.
    •	Place the ServerMain and ServerThread into the ChatServer project.
    •	Place the ClientMain into the ChatClient project.
    •	Run the ServerMain and  press the “Start Server” button on the GUI.
    •	Run the ClientMain, insert the username (no longer than 20 characters) and press the “Connect” button on the GUI.
    Possible problems
    =================
    The possible problems when trying to run the application:
    •	When connecting the client chat it presents a “Connection Error” pop out box with a message “Could not establish a link with the server”.  Follow these instructions:
        o	Make sure that the server is running (when the server is running the “Start Server” button in Server GUI is set to disabled, hence u can’t click it).
        o	If the server is running properly make sure that the portNumber of ClientMain.java reflects the portNumber of ServerMain.java, if it doesn’t reflect it then change the portNumber in ClientMain.java and re-launch the client chat (no need to re-launch the server).
    •	When connecting the server it presents a “Port Error” pop out box with a message “Can’t attach to the port number xxx” where xxx is the portNumber used for Server. Follow these instructions:
        o	Make sure that the server is not already running (hence, u can’t launch two servers using the same portNumber).
        o	If there is no other server (of this application) running, then try another port number (see the Configurations section above for more detail).
