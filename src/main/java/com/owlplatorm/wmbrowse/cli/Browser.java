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
   * Command to search Identifiers with a regular expression.
   */
  public static final String CMD_SEARCH = "search";

  /**
   * Message to print that contains all commands and brief descriptions.
   */
  public static final String HELP_MSG = "Command - Usage\n"
      + "help - Print this information\n"
      + "search ID_REGEX - Search for Identifiers using a regular expression"
      + "quit - Exit the application\n" + "exit - Exit the application";

  /**
   * Expects a server hostname/IP and origin value. Optionally provides the
   * solver and client port numbers, respectively.
   * 
   * @param args
   *          <ol>
   *          <li>hostname/IP address</li>
   *          <li>origin</li>
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
    if (args.length < 2) {
      System.err.println("Missing origin string.");
      return;
    }

    String wmHost = args[0];
    String origin = args[1];
    int solverPort = -1;
    int clientPort = -1;
    if (args.length > 2) {
      try {
        solverPort = Integer.parseInt(args[2]);
        if (solverPort > 65535) {
          System.out.println("Port number must be in the range [0,65535]");
          solverPort = -1;
        }
      } catch (NumberFormatException nfe) {
        System.out.println("Unable to parse " + args[2] + " as a port number.");
      }
    }
    if (args.length > 3) {
      try {
        clientPort = Integer.parseInt(args[3]);
        if (clientPort > 65535) {
          System.out.println("Port number must be in the range [0,65535]");
          clientPort = -1;
        }
      } catch (NumberFormatException nfe) {
        System.out.println("Unable to parse " + args[3] + " as a port number.");
      }
    }

    Browser b = new Browser(wmHost, origin, solverPort, clientPort);
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
   * The host name/IP address of the world model.
   */
  private final String hostString;

  /**
   * The current prompt shown to the user.
   */
  private String currentPrompt = PROMPT;

  /**
   * Constructs a new Browser object using the hostname, solver port, and client
   * port values. If either the solver port or client port values are &lt; 0,
   * uses the defaults included in the world model library.
   * 
   * @param wmHost
   *          the hostname or IP address of the world model
   * @param origin
   *          the origin string for Identifiers and Attributes sent to the world
   *          model
   * @param solverPort
   *          the alternate solver port number
   * @param clientPort
   *          the alternate client port number
   */
  public Browser(final String wmHost, final String origin,
      final int solverPort, final int clientPort) {
    this.cwc.setHost(wmHost);
    this.swc.setHost(wmHost);
    this.swc.setOriginString(origin);
    if (solverPort >= 0) {
      this.swc.setPort(solverPort);
    }
    if (clientPort >= 0) {
      this.cwc.setPort(clientPort);
    }

    this.hostString = wmHost;
    this.currentPrompt = this.hostString + PROMPT;

    this.userIn = new BufferedReader(new InputStreamReader(System.in));
  }

  @Override
  public void run() {
    
    // Client connection
    System.out.print("[Connecting to " + this.cwc + ".");
    if (!this.cwc.connect(10000)) {
      System.out.println("FAIL]");
      return;
    }
    int waitingTimes = 0;
    while (!this.cwc.isConnected() && waitingTimes++ < 20) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        // Ignored
      }
      System.out.print('.');
    }
    if(waitingTimes >= 20){
      System.out.println("FAIL]");
      this.cwc.disconnect();
      return;
    }
    System.out.println("OK]");
    
    // Solver connection
    
    
    System.out.print("[Connecting to " + this.swc + ".");
    if (!this.swc.connect(10000)) {
      System.out.println("FAIL]");
      return;
    }
    waitingTimes = 0;
    while(!this.swc.isConnectionLive() && waitingTimes++ < 20){
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        // Ignored
      }
      System.out.print('.');
    }
    if(waitingTimes >= 20){
      System.out.println("FAIL]");
      this.swc.disconnect();
      return;
    }
    
    System.out.println("OK]");
    System.out.println();
    System.out.print(this.currentPrompt);

    while (this.keepRunning) {
      if (!this.mainLoop()) {
        try {
          Thread.sleep(10);
        } catch (InterruptedException ie) {
          // Ignored
        }
      } else if (this.keepRunning) {
        System.out.println();
        System.out.print(this.currentPrompt);
      }

    }

    this.cwc.disconnect();
    this.swc.disconnect();

    System.out.println("--Disconnected--");
  }

  /**
   * Checks if user input is available, interprets it, takes action, and returns
   * with {@code true}.
   * 
   * @return {@code true} if a command was handled, else {@code false}.
   */
  protected boolean mainLoop() {

    try {
      if (System.in.available() > 0) {

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
   * Parses the command and if valid, takes an action; if invalid, prints an
   * error message.
   * 
   * @param command
   *          the user command
   */
  protected void handleCommand(final String command) {
    if (CMD_EXIT.equalsIgnoreCase(command)
        || CMD_QUIT.equalsIgnoreCase(command)) {
      this.shutdown();
    } else if (CMD_HELP.equalsIgnoreCase(command)) {
      this.getHelp();
    } else if (command.startsWith(CMD_SEARCH)) {
      this.performIdSearch(command);
    } else {
      System.out.println("Command not found \"" + command
          + "\".\nType \"help\" for a list of commands.");
    }
  }

  /**
   * Terminates the application. Performs any necessary clean-up before the
   * connections are terminated.
   */
  protected void shutdown() {
    this.keepRunning = false;
  }

  /**
   * Provides the user with command-based help.
   */
  protected void getHelp() {
    System.out.println(HELP_MSG);
  }

  /**
   * Searches for matching Identifier values given a regular expression
   * 
   * @param command
   */
  protected void performIdSearch(final String command) {
    // Extract the regular expression from the command line
    int startCmd = command.indexOf(CMD_SEARCH);
    if (startCmd == -1 || command.length() <= (CMD_SEARCH.length() + 1)) {
      System.out.println("Missing regular expression. Unable to search.");
      return;
    }
    // Grab the string from the end of the command +1 (space)
    String regex = command.substring(startCmd + CMD_SEARCH.length() + 1);
    if (regex.startsWith("\"") && regex.endsWith("\"") && regex.length() > 1) {
      regex = regex.substring(1, regex.length() - 1);
    }

    if(regex.length() == 0){
      System.out.println("Empty regular expression. Unable to search.");
      return;
    }
    
    System.out.println("Searching Identifiers for \"" + regex + "\"...");
    String[] matched = this.cwc.searchId(regex);
    if (matched == null || matched.length == 0) {
      System.out.println("[No results found.]");
      return;
    }

    for (String id : matched) {
      System.out.println("+ " + id);
    }

  }
}