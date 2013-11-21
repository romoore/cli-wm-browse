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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.owlplatform.worldmodel.Attribute;
import com.owlplatform.worldmodel.client.ClientWorldConnection;
import com.owlplatform.worldmodel.client.StepResponse;
import com.owlplatform.worldmodel.client.WorldState;
import com.owlplatform.worldmodel.solver.SolverWorldConnection;
import com.owlplatform.worldmodel.solver.protocol.messages.AttributeAnnounceMessage.AttributeSpecification;
import com.owlplatform.worldmodel.types.DataConverter;

/**
 * The startup class.
 * 
 * @author Robert Moore
 * 
 */
public class Browser extends Thread {

  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(Browser.class);

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
      + "World Model command line tools for the Owl Platform.\n\n"
      + "Copyright (C) 2012 Robert Moore\n"
      + TITLE
      + " comes with ABSOLUTELY NO WARRANTY.\n"
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
   * Command to retrieve a current snapshot of a Identifer regular expression.
   */
  public static final String CMD_STATUS = "status";

  /**
   * Command to retrieve a history of an Identifier regular expression.
   */
  public static final String CMD_HISTORY = "history";

  /**
   * Command to create a new Identifier value in the world model.
   */
  public static final String CMD_CREATE_ID = "touch";

  /**
   * Command to create or update an Attribute value in the world model.
   */
  public static final String CMD_UPDATE_ATTRIB = "update";

  /**
   * Command to expire an Identifier or a specific Attribute in the world model.
   */
  public static final String CMD_EXPIRE = "expire";

  /**
   * Command to delete an Identifier or a specific Attribute in the world model.
   */
  public static final String CMD_DELETE = "rm";

  /**
   * Command to copy an Identifier's current state or its historic attribute
   * values into a new Identifier value.
   */
  public static final String CMD_COPY = "cp";

  /**
   * Message to print that contains all commands and brief descriptions.
   */
  public static final String HELP_MSG = "Command - Usage\n"
      + "help - Print this information\n"
      + "search ID_REGEX [ID_REGEX...] - Search for Identifiers using a regex\n"
      + "status ID_REGEX [ID_REGEX...]- Current status for Identifiers using a regex\n"
      + "history ID_REGEX [ID_REGEX...] - Entire history for Identifiers using a regex\n"
      + "touch ID [ID...]- Create a new Identifier in the world model\n"
      + "update ID ATTR - Update an Identifier's Attribute in the world model\n"
      + "expire ID [ATTR] - Expire an Identifier or a single Attribute in the world model\n"
      + "rm ID [ATTR] - Delete an Identifier or a single Attribute in the world model\n"
      + "cp [-r] SRC_ID DST_ID - Copy an Identifier's current or historic state to\n"
      + "  a new Identifier value\n" + "quit - Exit the application\n"
      + "exit - Exit the application";

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
      System.out.println("Missing world model hostname/IP address.");
      return;
    }
    if (args.length < 2) {
      System.out.println("Missing origin string.");
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
   * Origin value to use when updating Attribute values.
   */
  private final String origin;

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
    this.origin = origin;
    this.currentPrompt = "[" + origin + "@" + this.hostString + "]" + PROMPT;

    this.userIn = new BufferedReader(new InputStreamReader(System.in));
  }

  @Override
  public void run() {

    // Client connection
    System.out.print("[Connecting to " + this.cwc + "...");
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
    if (waitingTimes >= 20) {
      System.out.println("FAIL]");
      this.cwc.disconnect();
      return;
    }
    System.out.println("OK]");

    // Solver connection

    System.out.print("[Connecting to " + this.swc + "...");
    if (!this.swc.connect(10000)) {
      System.out.println("FAIL]");
      return;
    }
    waitingTimes = 0;
    while (!this.swc.isConnectionLive() && waitingTimes++ < 20) {
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        // Ignored
      }
      System.out.print('.');
    }
    if (waitingTimes >= 20) {
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
        System.out.print(this.currentPrompt);
      }

    }

    this.shutdown();
  }

  /**
   * Shuts down the world model connections. Cleans-up any remaining threads,
   * objects, etc.
   */
  protected void shutdown() {
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
        if (line.length() == 0) {
          return true;
        }

        this.handleCommand(line);
        return true;
      }
    } catch (Throwable e) {
      System.out
          .println("An error has occurred. See the log for more information.");
      log.error("An unhandled exception occurred.", e);
      this.keepRunning = false;
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
    if (command == null) {
      return;
    }
    if (CMD_EXIT.equalsIgnoreCase(command)
        || CMD_QUIT.equalsIgnoreCase(command)) {
      this.stopRunning();
    } else if (CMD_HELP.equalsIgnoreCase(command)) {
      this.getHelp();
    } else if (command.startsWith(CMD_SEARCH)) {
      this.performIdSearch(command);
    } else if (command.startsWith(CMD_STATUS)) {
      this.currentStatus(command);
    } else if (command.startsWith(CMD_HISTORY)) {
      this.history(command);
    } else if (command.startsWith(CMD_CREATE_ID)) {
      this.createId(command);
    } else if (command.startsWith(CMD_UPDATE_ATTRIB)) {
      this.updateAttribute(command);
    } else if (command.startsWith(CMD_EXPIRE)) {
      this.expire(command);
    } else if (command.startsWith(CMD_DELETE)) {
      this.delete(command);
    } else if (command.startsWith(CMD_COPY)) {
      this.copy(command);
    } else {
      System.out.println("Command not found \"" + command
          + "\".\nType \"help\" for a list of commands.");
    }
  }

  /**
   * Terminates the application. Performs any necessary clean-up before the
   * connections are terminated.
   */
  protected void stopRunning() {
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
   *          the full command provided by the user.
   */
  protected void performIdSearch(final String command) {
    String regex = removeCommand(CMD_SEARCH, command);
    if (regex == null) {
      System.out
          .println("Empty regular expression. Unable to retrieve status.");
      return;
    }

    List<String> idList = extractComponents(regex);
    if (idList == null || idList.isEmpty()) {
      System.out.println("Missing Identifier. Unable to create.");
      return;
    }
    for (String entry : idList) {
      System.out.println("Searching Identifiers for \"" + entry + "\"...");
      String[] matched = this.cwc.searchId(entry);
      if (matched == null || matched.length == 0) {
        System.out.println("[No results found.]");
        return;
      }

      for (String id : matched) {
        System.out.println("+ " + id);
      }
    }

  }

  /**
   * Removes the String {@code command} from the String {@code source},
   * returning the remainder of {@code source} if there is anything left.
   * 
   * @param command
   *          the command to remove.
   * @param source
   *          the string to remove {@code command} from.
   * @return the remainder of {@code source} after {@code command} has been
   *         removed, or {@code null} if an error occurred.
   */
  protected static String removeCommand(final String command,
      final String source) {
    try {
      // Extract the regular expression from the command line
      int startCmd = source.indexOf(command);
      if (startCmd == -1 || source.length() <= (command.length() + 1)) {
        return null;
      }
      // Grab the string from the end of the command +1 (space)
      String remainder = source.substring(startCmd + command.length() + 1);
      if (remainder.length() == 0) {
        return null;
      }
      return remainder;
    } catch (Exception e) {
      System.out
          .println("An exception occurred while parsing the command.  See the log for details.");
      log.error("Exception while removing a command (" + command + "/" + source
          + ").", e);
      return null;
    }

  }

  /**
   * Sends a snapshot request to the world model for the current state of the
   * Identifiers in a regular expression provided
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void currentStatus(final String command) {
    String idRegex = removeCommand(CMD_STATUS, command);
    if (idRegex == null) {
      System.out
          .println("Empty regular expression. Unable to retrieve status.");
      return;
    }

    List<String> idList = extractComponents(idRegex);
    if (idList == null || idList.isEmpty()) {
      System.out.println("Missing Identifier. Unable to create.");
      return;
    }

    for (String element : idList) {
      System.out
          .println("Retrieving current status for \"" + element + "\"...");
      try {
        WorldState state = this.cwc.getCurrentSnapshot(element, ".*").get();
        if (state == null) {
          System.out.println("[No status available.]");
          return;
        }
        printState(state);

      } catch (Exception e) {
        System.out
            .println("Unable to retrieve current status. See the log for more details.");
        log.error("Unable to retrieve current snapshot for \"" + element
            + "\".", e);
        return;
      }
    }
  }

  /**
   * Requests the complete history of the Identifiers matched in the regular
   * expression provided in the command.
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void history(final String command) {
    String idRegex = removeCommand(CMD_HISTORY, command);
    if (idRegex == null) {
      System.out
          .println("Empty regular expression. Unable to retrieve status.");
      return;
    }

    List<String> idList = extractComponents(idRegex);
    if (idList == null || idList.isEmpty()) {
      System.out.println("Missing Identifier. Unable to create.");
      return;
    }

    for (String element : idList) {
      System.out.println("Retrieving historic information for \"" + element
          + "\".\nThis may take some time..");
      try {
        StepResponse responses = this.cwc.getRangeRequest(element, 0,
            System.currentTimeMillis(), ".*");

        if (responses == null) {
          System.out.println("[No history available.]");
          return;
        }

        // Keep going while there is data OR the data is incomplete
        while (responses.hasNext() || !responses.isComplete()) {
          if (responses.isError()) {
            System.out
                .println("An error occurred. Please see the log for details.");
            log.error("Error while retrieving range response for " + element
                + ".", responses.getError());
            return;
          }
          /*
           * Get the next available state. If complete, should return
           * immediately, else may block until data arrives. Can throw an
           * exception if something happens while waiting.
           */

          WorldState state = responses.next();
          System.out.println("==========");
          printState(state);
        }
      } catch (Exception e) {
        System.out
            .println("Unable to some or all historic status information. See the log for more details.");
        log.error("Unable to retrieve full history for \"" + element + "\".", e);
        return;
      }
    }
  }

  /**
   * Creates a new Identifier value in the world model.
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void createId(final String command) {
    String identifier = removeCommand(CMD_CREATE_ID, command);
    if (identifier == null) {
      System.out.println("Missing Identifier. Unable to create.");
      return;
    }

    List<String> idList = extractComponents(identifier);
    if (idList == null || idList.isEmpty()) {
      System.out.println("Missing Identifier. Unable to create.");
      return;
    }

    for (String element : idList) {
      if (this.swc.createId(element)) {
        System.out.println("Create \"" + element + "\" command was sent.");
        try {
          printState(this.cwc.getSnapshot(element, 0, 0, "creation").get());
        } catch (Exception e) {
          log.error("Unable to retrieve state after creating \"" + element
              + "\".", e);
        }
      } else {
        System.out.println("Unable to create \"" + element
            + "\" in the world model.");
      }
    }
  }

  /**
   * Prints a WorldState object to System.out.
   * 
   * @param state
   *          the state to print.
   */
  protected static void printState(final WorldState state) {
    if (state == null) {
      System.out.println("+ [NO DATA]");
      return;
    }
    for (String id : state.getIdentifiers()) {
      System.out.println("+ " + id);
      Collection<Attribute> attribs = state.getState(id);
      if (attribs == null || attribs.isEmpty()) {
        System.out.println("  [NO DATA]");
        continue;
      }
      for (Attribute a : attribs) {
        System.out.println(" - " + a);
      }
    }
  }

  /**
   * Updates an Identifier Attribute in the world model.
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void updateAttribute(final String command) {
    String idAndAttrib = removeCommand(CMD_UPDATE_ATTRIB, command);

    if (idAndAttrib == null) {
      System.out
          .println("Missing Identifier. Unable to update attribute value.");
      return;
    }

    List<String> components = extractComponents(idAndAttrib);
    if (components.size() != 2) {
      System.out
          .println("Invalid number of arguments.  Cannot update attribute value.");
      return;
    }

    String identifier = components.get(0);
    String attribute = components.get(1);

    int attempts = 0;
    String line = null;
    String[] supportedTypes = DataConverter.getSupportedTypes();
    String typeName = null;
    while (!DataConverter.hasConverterForAttribute(attribute) && attempts < 3) {
      System.out.println("Unknown attribute type \"" + attribute
          + "\".\nPlease select a data type:");
      for (int i = 0; i < supportedTypes.length; ++i) {
        System.out.println(i + ") " + supportedTypes[i]);
      }
      try {
        line = this.userIn.readLine();
        int index = Integer.parseInt(line);
        if (index < 0 || index >= supportedTypes.length) {
          throw new NumberFormatException("Selection is out of range.");
        }
        typeName = supportedTypes[index];
        DataConverter.putConverter(attribute, typeName);
      } catch (NumberFormatException nfe) {
        System.out.println("Invalid selection: \"" + line
            + "\". Please make another selection.");
      } catch (IOException ioe) {
        System.out.println("Unable to read your selection. Aborting.");
        log.error("Unable to read data type selection.", ioe);
        return;
      }
    }

    if (!DataConverter.hasConverterForAttribute(attribute)) {
      System.out.println("Your response was not recognized after 3 attempts.");
      return;
    }

    System.out.println("Please enter a value for " + attribute
        + " as a String:");
    try {
      line = this.userIn.readLine();
    } catch (IOException e) {
      System.out.println("Unable to read your data. Cannot update.");
      log.error("Unable to read attribute data.", e);
      return;
    }

    byte[] data = DataConverter.encode(attribute, line);

    if (data == null) {
      return;
    }

    boolean success = this.insertAttributeValue(identifier, attribute, data);
    if (!success) {
      System.out.println("Unable to update world model. Reason unknown.");
      return;
    }

    try {
      printState(this.cwc.getSnapshot(identifier, 0, 0, attribute).get());
    } catch (Exception e) {
      log.error("Unable to retrieve state after updatng \"" + identifier + "/"
          + attribute + "\".", e);
    }
    return;
  }

  /**
   * Updates an Identifier's Attribute in the world model.
   * 
   * @param identifier
   *          the Identifier to update
   * @param attribute
   *          the Attribute to update
   * @param data
   *          the encoded form of the Attribute value
   * @return {@code true} on successfully sending the message, else
   *         {@code false}.
   */
  private boolean insertAttributeValue(final String identifier,
      final String attribute, final byte[] data) {

    AttributeSpecification spec = new AttributeSpecification();
    spec.setAttributeName(attribute);
    spec.setIsOnDemand(false);
    this.swc.addAttribute(spec);

    Attribute newAttr = new Attribute();
    newAttr.setAttributeName(attribute);
    newAttr.setCreationDate(System.currentTimeMillis());
    newAttr.setData(data);
    newAttr.setId(identifier);
    newAttr.setOriginName(this.origin);

    return this.swc.updateAttribute(newAttr);
  }

  /**
   * <p>
   * Extracts the components of a String. Separating based on spaces, quotes
   * group words with spaces, but are removed.
   * </p>
   * 
   * <p>
   * Originally from Stack Overflow:<br />
   * http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-
   * using-space-when-not-surrounded-by-single-or-double
   * </p>
   * <p>
   * Original author:<br />
   * <a href="http://stackoverflow.com/users/33358/jan-goyvaerts">Jan
   * Goyvaerts</a>
   * </p>
   * 
   * @param whole
   *          the entire string to separate.
   * @return the components of the whole, separated into individual Strings.
   */
  protected static List<String> extractComponents(final String whole) {
    if (whole == null) {
      return null;
    }
    List<String> matchList = new ArrayList<String>();
    Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
    Matcher regexMatcher = regex.matcher(whole);
    while (regexMatcher.find()) {
      if (regexMatcher.group(1) != null) {
        // Add double-quoted string without the quotes
        matchList.add(regexMatcher.group(1));
      } else if (regexMatcher.group(2) != null) {
        // Add single-quoted string without the quotes
        matchList.add(regexMatcher.group(2));
      } else {
        // Add unquoted word
        matchList.add(regexMatcher.group());
      }
    }
    return matchList;
  }

  /**
   * Expires all or just one of an Identifier's Attribute values.
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void expire(final String command) {
    String idAndAttrib = removeCommand(CMD_EXPIRE, command);

    if (idAndAttrib == null) {
      System.out.println("Missing Identifier. Unable to expire.");
      return;
    }

    List<String> components = extractComponents(idAndAttrib);
    if (components.size() < 1) {
      System.out.println("Invalid number of arguments.  Cannot expire value.");
      return;
    }

    if (components.size() == 1) {
      this.expireIdentifier(components.get(0));
    } else {
      this.expireAttribute(components.get(0), components.get(1));
    }
  }

  /**
   * Expires an Identifier after prompting for the expiration date.
   * 
   * @param identifier
   *          the Identifier to expire.
   */
  protected void expireIdentifier(final String identifier) {
    Date expireTime = getDate("expiration");
    if (expireTime == null) {
      System.out.println("Unable to determine expiration time.");
      return;
    }

    if (!this.swc.expire(identifier, expireTime.getTime())) {
      System.out.println("Unable to expire \"" + identifier
          + "\" due to an unknown error.");
    }
  }

  /**
   * Expires the current value of a specific Identifier and Attribute in the
   * world model after prompting for the expiration time.
   * 
   * @param identifier
   *          the Identifier of the Attribute to expire.
   * @param attribute
   *          the Attribute name to expire.
   */
  protected void expireAttribute(final String identifier, final String attribute) {
    Date expireTime = getDate("expiration");
    if (expireTime == null) {
      System.out.println("Unable to determine expiration time.");
      return;
    }

    if (!this.swc.expire(identifier, expireTime.getTime(), attribute)) {
      System.out.println("Unable to expire \"" + identifier + "\"/\""
          + attribute + "\" due to an unknown error.");
    }
  }

  /**
   * Prompts the user for a date and time (in 24-hour format), returned as a
   * {@code java.util.Date} object.
   * 
   * @param typeName
   *          the type of date to ask the user for.
   * @return a {@code Date} object based on the user input, or {@code null} if
   *         the input was invalid or missing.
   */
  protected Date getDate(final String typeName) {
    System.out.println("Please enter the " + typeName
        + " date in the format YYYYMMDD: ");
    String dateLine;
    try {
      dateLine = this.userIn.readLine();
    } catch (IOException e) {
      System.out
          .println("An error has occurred.  Please see the log for details.");
      log.error("Unable to read user input (date).", e);
      return null;
    }
    if (dateLine.length() != 8) {
      System.out.println("Invalid date format.");
      return null;
    }
    int year = Integer.parseInt(dateLine.substring(0, 4));
    int month = Integer.parseInt(dateLine.substring(4, 6));
    int date = Integer.parseInt(dateLine.substring(6, 8));

    System.out.println("Please enter the " + typeName
        + " time in the 24-hour format hhmmss: ");
    String timeLine = "";
    try {
      timeLine = this.userIn.readLine();
    } catch (IOException e) {
      System.out
          .println("An error has occurred.  Please see the log for details.");
      log.error("Unable to read user input (time).", e);
      return null;
    }
    if (timeLine.length() != 6) {
      System.out.println("Invalid time format.");
      return null;
    }

    int hour = Integer.parseInt(timeLine.substring(0, 2));
    int minute = Integer.parseInt(timeLine.substring(2, 4));
    int second = Integer.parseInt(timeLine.substring(4, 6));

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, year);
    cal.set(Calendar.MONTH, month);
    cal.set(Calendar.DAY_OF_MONTH, date);
    cal.set(Calendar.HOUR_OF_DAY, hour);
    cal.set(Calendar.MINUTE, minute);
    cal.set(Calendar.SECOND, second);
    return cal.getTime();
  }

  /**
   * Deletes all or only one of an Identifier's Attribute values.
   * 
   * @param command
   *          the full command provided by the user.
   */
  protected void delete(final String command) {
    String idAndAttrib = removeCommand(CMD_DELETE, command);

    if (idAndAttrib == null) {
      System.out.println("Missing Identifier. Unable to delete.");
      return;
    }

    List<String> components = extractComponents(idAndAttrib);
    if (components.size() < 1) {
      System.out.println("Invalid number of arguments.  Cannot delete value.");
      return;
    }

    if (components.size() == 1) {
      this.deleteIdentifier(components.get(0));
      System.out.println("Deleted identifier \"" + components.get(0) + "\".");
    } else {
      this.deleteAttribute(components.get(0), components.get(1));
      System.out.println("Deleted attribute \"" + components.get(1) + "\" for \"" + components.get(0) + "\".");
    }
  }

  /**
   * Deletes an Identifier from the world model.
   * 
   * @param identifier
   *          the Identifier to delete.
   */
  protected void deleteIdentifier(final String identifier) {
    if (!this.swc.delete(identifier)) {
      System.out.println("Unable to delete \"" + identifier
          + "\" due to an unknown error.");
    }
  }

  /**
   * Deletes an Attribute from an Identifier in the world model.
   * 
   * @param identifier
   *          the Identifier for the Attribute
   * @param attribute
   *          the Attribute name to delete
   */
  protected void deleteAttribute(final String identifier, final String attribute) {
    if (!this.swc.delete(identifier, attribute)) {
      System.out.println("Unable to delete \"" + identifier + "\"/\""
          + attribute + "\" due to an unknown error.");
    }
  }

  /**
   * Copies the state from one Identifier to another. If the "-r" (recursive)
   * flag is provided, it copies the entire history of Attribute values.
   * 
   * @param command
   *          the full command provided by the user
   */
  protected void copy(final String command) {
    String flagAndIds = removeCommand(CMD_COPY, command);

    if (flagAndIds == null) {
      System.out.println("Missing Identifier. Unable to copy state.");
      return;
    }
    boolean withHistory = false;
    if (flagAndIds.contains("-r")) {
      flagAndIds = removeCommand("-r", flagAndIds);
      withHistory = true;
    }

    if (flagAndIds == null) {
      System.out.println("Missing Identifier. Unable to copy state.");
      return;
    }

    List<String> parts = extractComponents(flagAndIds);

    if (parts == null || parts.size() != 2) {
      System.out
          .println("Source or destination Identifier is missing. Unable to copy.");
      return;
    }

    if (withHistory) {
      this.recursiveCopy(parts.get(0), parts.get(1));
    } else {
      this.shallowCopy(parts.get(0), parts.get(1));
    }
  }

  /**
   * Performs a copy of the entire historical state from the source Identifier
   * to destination.
   * 
   * @param source
   *          the Identifier to copy from.
   * @param destination
   *          the Identifier to copy to.
   */
  protected void recursiveCopy(final String source, final String destination) {
    WorldState origState = null;
    StepResponse resp = null;

    resp = this.cwc.getRangeRequest(source, 0, Long.MAX_VALUE, ".*");
    int totalCopies = 0;
    try {
      while (!resp.isComplete() && !resp.isError()) {

        origState = resp.next();
        if (origState == null) {
          System.out.println("The source is empty.");
          break;
        }
        int numCopies = this.copyAttributes(origState.getState(source), destination);
        if(numCopies < 0){
          System.out.println("Error while copying one or more Attributes. Aborting.");
          break;
        }
        totalCopies += numCopies;
      }
    } catch (Exception e) {
      System.out
          .println("Unable to read from source.  See the log for details.");
      log.error("Unable to retrieve state for \"" + source + "\".", e);
      return;
    }
    System.out.println("Copied " + totalCopies + " Attributes.");
  }

  /**
   * Performs a copy of the current state from the source Identifier to
   * destination.
   * 
   * @param source
   *          the Identifier to copy from.
   * @param destination
   *          the Identifier to copy to.
   */
  protected void shallowCopy(final String source, final String destination) {

    WorldState origState = null;
    try {
      origState = this.cwc.getCurrentSnapshot(source, ".*").get();
    } catch (Exception e) {
      System.out
          .println("Unable to read from source.  See the log for details.");
      log.error("Unable to retrieve current state for \"" + source + "\".", e);
      return;
    }
    if (origState == null) {
      System.out.println("The source is empty.");
      return;
    }
    int numAttr = this.copyAttributes(origState.getState(source), destination);
    if(numAttr >= 0){
      System.out.println("Copied " + numAttr + " attributes.");
    }
    else{
      System.out.println("An error has occurred. One or more Attributes was not copied.");
    }
  }

  /**
   * Copies a collection of Attribute values to a destination Identifier.
   * 
   * @param attributes
   *          the Attributes to copy.
   * @param destination
   *          the destination Identifier.
   * @return {@code true} if all Attributes were copied successfully, else
   *         {@code false}.
   */
  protected int copyAttributes(final Collection<Attribute> attributes,
      final String destination) {
    String currOrigin = this.origin;
    boolean success = true;
    int numAttr = 0;
    for (Attribute attr : attributes) {
      attr.setId(destination);
      AttributeSpecification spec = new AttributeSpecification();
      spec.setAttributeName(attr.getAttributeName());
      spec.setIsOnDemand(false);
      this.swc.addAttribute(spec);
      if (!attr.getOriginName().equals(currOrigin)) {
        currOrigin = attr.getOriginName();
        this.swc.setOriginString(currOrigin);
      }
      success = success && this.swc.updateAttribute(attr);
      if (!success) {
        System.out.println("Unable to copy " + attr + ".");
        break;
      }
      ++numAttr;
    }
    this.swc.setOriginString(this.origin);
    if(success){
      return numAttr;
    }
    return -1;
  }
}