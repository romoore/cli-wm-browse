/*
 * Owl Platform Command Line Browser
 * Copyright (C) 2012 Robert Moore and the Owl Platform
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *  
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *  
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.owlplatorm.wmbrowse.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;

/**
 * The startup class.
 * 
 * @author Robert Moore
 * 
 */
public class Browser extends Thread {

  /**
   * Title for this app.
   */
  public static final String TITLE = "Owl Platform WM Browser";

  /**
   * Current version as a string.
   */
  public static final String VERSION = "1.0.0-BETA";

  /**
   * Text printed at startup for license notification, about, etc.
   */
  public static final String ABOUT_TXT = TITLE
      + " version "
      + VERSION
      + "\n"
      + "Signal Visualization tools for the Owl Platform.\n\n"
      + "Copyright (C) 2012 Robert Moore and the Owl Platform\n"
      + "SigVis comes with ABSOLUTELY NO WARRANTY.\n"
      + "This is free software, and you are welcome to redistribute it\n"
      + "under certain conditions; see the included file LICENSE for details.\n";
  
  /**
   * User prompt.
   */
  public static final String PROMPT = ">";
  /**
   * Command to exit.
   */
  public static final String CMD_QUIT = "quit";
  /**
   * Command to exit.
   */
  public static final String CMD_EXIT = "exit";
  /**
   * Command to print help information.
   */
  public static final String CMD_HELP = "help";
  
  /**
   * Message to print that contains all commands and brief descriptions.
   */
  public static final String HELP_MSG = 
      "Command - Usage\n"+
      "help - Print this information\n"+
      "quit - Exit the application\n"+
      "exit - Exit the application\n";
  

  /**
   * Expects a server hostname/IP. Optionally provides the solver and client
   * port numbers, respectively.
   * 
   * @param args
   *          <ol>
   *          <li>hostname/IP address</li>
   *          <li>(<em>Optional</em>) solver port</li>
   *          <li>(<em>Optional</em>) client port</li>
   *          </ol>
   */
  public static void main(String[] args) {
    System.out.println(ABOUT_TXT);
    if (args.length < 1) {
      System.err.println("Missing world model hostname/IP address.");
      return;
    }

    String wmHost = args[0];
    int solverPort = -1;
    int clientPort = -1;
    if (args.length > 1) {
      try {
        solverPort = Integer.parseInt(args[1]);
        if (solverPort > 65535) {
          System.err.println("Port number must be in the range [0,65535]");
          solverPort = -1;
        }
      } catch (NumberFormatException nfe) {
        System.err.println("Unable to parse " + args[1] + " as a port number.");
      }
    }
    if (args.length > 2) {
      try {
        clientPort = Integer.parseInt(args[2]);
        if (clientPort > 65535) {
          System.err.println("Port number must be in the range [0,65535]");
          clientPort = -1;
        }
      } catch (NumberFormatException nfe) {
        System.err.println("Unable to parse " + args[2] + " as a port number.");
      }
    }

    Browser b = new Browser(wmHost, solverPort, clientPort);
    b.start();
  }

  /**
   * Client connection to the world model.
   */
  private final ClientWorldConnection cwc = new ClientWorldConnection();

  /**
   * Solver connection to the world model.
   */
  private final SolverWorldConnection swc = new SolverWorldConnection();

  /**
   * Flag to keep running the main application loop.
   */
  private boolean keepRunning = true;
  
  /**
   * Input stream for user commands.
   */
  private BufferedReader userIn;

  /**
   * Constructs a new Browser object using the hostname, solver port, and client
   * port values. If either the solver port or client port values are &lt; 0,
   * uses the defaults included in the world model library.
   * 
   * @param wmHost
   *          the hostname or IP address of the world model
   * @param solverPort
   *          the alternate solver port number
   * @param clientPort
   *          the alternate client port number
   */
  public Browser(final String wmHost, final int solverPort, final int clientPort) {
    this.cwc.setHost(wmHost);
    this.swc.setHost(wmHost);
    if (solverPort >= 0) {
      this.swc.setPort(solverPort);
    }
    if (clientPort >= 0) {
      this.cwc.setPort(clientPort);
    }
    
    this.userIn = new BufferedReader(new InputStreamReader(System.in));
  }

  @Override
  public void run() {
    if (!this.cwc.connect(10000)) {
      System.err.println("Unable to connect to " + this.cwc + ". Exiting.");
      return;
    }
    if (!this.swc.connect(10000)) {
      System.err.println("Unable to connect to " + this.swc + ". Exiting.");
      return;
    }
    System.out.println();
    System.out.print(PROMPT);

    while (this.keepRunning) {
      if (!this.mainLoop()) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ie) {
          // Ignored
        }
      }else {
        System.out.println();
        System.out.print(PROMPT);
      }

    }

    this.cwc.disconnect();
    this.swc.disconnect();
    
    System.out.println("Disconnect!");
  }
  
  /**
   * Checks if user input is available, interprets it, takes action, and returns
   * with {@code true}.
   * @return {@code true} if a command was handled, else {@code false}.
   */
  protected boolean mainLoop(){
    
    try {
      if(System.in.available() > 0){
        
        String line = this.userIn.readLine().trim();
        
        this.handleCommand(line);
        return true;
      }
    } catch (IOException e) {
      System.err.println("An error has occurred: " + e);
      e.printStackTrace();
    }
    return false;
  }
  
  /**
   * Parses the command and if valid, takes an action; if invalid, prints an error message.
   * @param command the user command
   */
  protected void handleCommand(final String command){
    if(CMD_EXIT.equalsIgnoreCase(command) || CMD_QUIT.equalsIgnoreCase(command)){
      this.shutdown();
    }else if(CMD_HELP.equalsIgnoreCase(command)){
      this.getHelp();
    }
  }
  
  /**
   * Terminates the application.  Performs any necessary clean-up before the connections
   * are terminated.
   */
  protected void shutdown(){
    this.keepRunning = false;
  }
  
  /**
   * Provides the user with command-based help.
   */
  protected void getHelp(){
    System.out.println(HELP_MSG);
  }
}