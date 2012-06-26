Owl Platform World Model Browser
================================

Version 1.0.0-BETA

Last updated June 26, 2012

Project Website: <https://github.com/romoore/cli-wm-browse>

Copyright (C) 2012 Robert Moore

This application is free software according to the terms and conditions of
the GNU General Purpose License, version 2.0 (or higher at your discretion).
You should have received a copy of the GNU General Purpose License v2.0 along
with this software as the file LICENSE.  If not, you may download a copy from
<http://www.gnu.org/licenses/gpl-2.0.txt>.

## About ##
Owl Platform World Model Browser is a user-contributed utility for
interacting with World Model servers through a simple command-line interface
(CLI).  It is not part of the official Owl Platform distribution, and is
maintained independently.  Any problems or bugs should be reported to the 
project website listed above, and not to the Owl Platform developers.

## Compiling ##
This browser is intended to be compiled using the Apache Maven project
management tool.  The project is currently compatible with Apache Maven
version 3, which can be downloaded for free at <http://maven.apache.org/>.
To build the static JAR file output, the following command should be run
from the project root (where the pom.xml file is located):

    mvn clean install -U

If everything compiles correctly, then near the end of the Maven output,
this line should appear:

    [INFO] BUILD SUCCESS

In this case, the JAR file will be located in the ``target'' subdirectory.
If not, please visit the project website listed at the top of this
document for support.

## Running ##
The browser can be run from the command line using the Java launcher (java
or java.exe).  To run via the Java launcher directly, you only need to
include the path to the Jar file:

    java -jar path/to/cli-wm-browse-1.0.0-SNAPSHOT-jar-with-dependencies.jar

The browser requires two parameters: the World Model server hostname/IP
address and the username/origin name.  The origin name should uniquely
identify the user for any data inserted into the world model.  If
non-standard port numbers are needed (not 7009/7010), they can be provided
as 2 additional, optional parameters after the origin string.  The solver
port is first, and the client port is second.
  
    java -jar path/to/cli-wm-browse-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
      grail.mydomain.com myuser
 
Or to specify alternate ports:
  
    java -jar path/to/cli-wm-browse-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
      grail.mydomain.com myuser 8123 8124

## Notes ##
This utility makes use of [Jan Goyvaerts]' regular expression/Java code for
extracting quoted strings from a String variable.  The [Original Post] can be found here: 

[Original Post]: http://stackoverflow.com/questions/366202/regex-for-splitting-a-string-using-space-when-not-surrounded-by-single-or-double
[Jan Goyvaerts]: http://stackoverflow.com/users/33358/jan-goyvaerts
