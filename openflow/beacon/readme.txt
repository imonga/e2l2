{\rtf1\ansi\ansicpg1252\cocoartf1038\cocoasubrtf360
{\fonttbl\f0\fswiss\fcharset0 Helvetica;}
{\colortbl;\red255\green255\blue255;}
\margl1440\margr1440\vieww37900\viewh21300\viewkind0
\pard\tx720\tx1440\tx2160\tx2880\tx3600\tx4320\tx5040\tx5760\tx6480\tx7200\tx7920\tx8640\ql\qnatural\pardirnatural

\f0\fs24 \cf0 \
GETTING BEACON CODE\
-------------------------------------\
Instructions for checking out Beacon and OpenflowJ code and installing the prerequisites are given in the following link:\
https://openflow.stanford.edu/display/Beacon/Quick+Start\
\
FILE ORGANIZATION\
-----------------------------\
E2L2 source code contains the following :\
\
1. net.beaconcontroller.brokerPlugin - This osgi plugin can be deployed as a part of beacon openflow controller. Using this plugin, we can compute the shortest path from source to destination OF Switch and program all switches It has the following files:\
	i. BrokerInterface.java - This is the RMI interface exposed to the client code. The methods defined in this code can be called remotely by the client\
	ii. Node.java - This describes a node in the openflow topology graph. It is used to compute the shortest path from source to destination OF Switch\
	iii. PathDiscovery.java - This class has methods to compute the best path from source to destination and program all openflow switches in the path\
\
2. AppBroker - this java project acts as a client to the brokerPlugin inside beacon and make calls to the methods of RMI BrokerInterface exposed to it. It has the following files:\
	i. Client.java - This class grabs the remote object from the rim registry and calls its methods\
	ii. InvokeClient - This has a main method which invokes the client\
\
3. net.beaconcontroller.loader - This osgi plugin can be deployed as a part of beacon openflow controller. Using this plugin, we can dynamically install, start, stop and uninstall any osgi plugin from its jar file. It has the following files:\
	i. LoaderInterface.java - This is the RMI interface exposed to the client code. The methods defined in this code can be called remotely by the client\
	ii. Loader.java - This class uses its own bundle context as a dummy to install a plugin. When the plugin is installed it's bundle is returned and it is used to call start, stop and uninstall methods on it.\
\
BUILDING BROKER PLUGIN CODE WITH MAVEN\
---------------------------------------------------------------------\
Building brokerPlugin source code with maven:\
\
1. Make sure you have a copy of beacon source on your computer.\
\
2. Copy the source folder net.beaconcontroller.brokerPlugin into cs244-beacon directory\
\
3. Make sure that the <version>0.1.0-SNAPSHOT</version> in "pom.xml" in net.beaconcontroller.brokerPlugin is same is as the version number in the MANIFEST.MF file in META-INF directory :  Bundle-Version: 0.1.0.qualifier \
\
4. Now, change the "pom.xml" file in net.beaconcontroller.parent directory to include your plugin in the modules section : <module>../net.beaconcontroller.brokerPlugin</module>\
\
5. Now, you should be able to build brokerPlugin using maven (make sure you have maven 3.0 or above to build it), \
     - If you have not already done so, go to openflowj directory and type: mvn install\
    - Next, go to net.beaconcontroller.parent and type: mvn integration-test (it will take about 2-3 minutes to build the whole project)\
\
6. To deploy brokerPlugin along with beacon-product, change the configuration file "beacon.product" inside net.beaconcontroller.product directory to include your plugin:\
     - Modify "plugins" section to include brokerPlugin: <plugin id="net.beaconcontroller.brokerPlugin"/>\
     - Modify "configurations" sections to include brokerPlugin: <plugin id="net.beaconcontroller.brokerPlugin" autoStart="true" startLevel="0" />\
\
7. Build it again following step 5 again\
\
8. Find the beacon product for your platform in the /usr/local/src/cs244-beacon/net.beaconcontroller.product/target/products/beacon.product.id folder. To deploy it, find the binary file for your platform and run it from the same directory by typing: ./beacon \
\
Following the same steps you will be able to build the source code for getAppContext.\
\
BUILDING BROKER PLUGIN CODE RESIDING IN A DIRECTORY OTHER THAN BEACON\
---------------------------------------------------------------------------------------------------------------------------\
\
1. Open pom.xml for broker plugin and modify the <relativePath> for the parent project to wherever your net.beaconcontroller.parent directory is. For me it looked like the following:\
\
  <parent>\
    <artifactId>parent</artifactId>\
    <groupId>net.beaconcontroller</groupId>\
    <version>0.1.0-SNAPSHOT</version>\
    <relativePath>../../../usr/local/src/cs244-beacon/net.beaconcontroller.parent</relativePath>\
  </parent>\
\
2. Modify the pom.xml in net.beaconcontroller.parent folder of the beacon source code to add your module into the list of modules for the parent project with the relative path to the directory where your brokerPlugin resides. For me it looks like the following:\
\
<module>../../../../../home/username/net.beaconcontroller.brokerPlugin</module>\
\
3. In pom.xml of net.beaconcontroller.parent , go to <repostitories> section and change the path of the beaconlibs to the absolute path of the libs folder under your beacon directory. For me it looked like the following:\
\
   <repository>\
      <id>beaconlibs</id>\
      <layout>p2</layout>\
      <url>file:/usr/local/src/cs244-beacon/libs</url>\
    </repository>\
\
4. Now, you should be able to build brokerPlugin using maven (make sure you have maven 3.0 or above to build it), \
     - If you have not already done so, go to openflowj directory and type: mvn install\
    - Next, go to net.beaconcontroller.parent and type: mvn integration-test (it will take about 2-3 minutes to build the whole project)\
\
5. To deploy brokerPlugin along with beacon-product, change the configuration file "beacon.product" inside net.beaconcontroller.product directory to include your plugin:\
     - Modify "plugins" section to include brokerPlugin: <plugin id="net.beaconcontroller.brokerPlugin"/>\
     - Modify "configurations" sections to include brokerPlugin: <plugin id="net.beaconcontroller.brokerPlugin" autoStart="true" startLevel="0" />\
\
6. Build it again following step 4 again\
\
7. Find the beacon product for your platform in the /usr/local/src/cs244-beacon/net.beaconcontroller.product/target/products/beacon.product.id folder. To deploy it, find the binary file for your platform and run it from the same directory by typing: ./beacon \
\
Following the same steps you can also build source code for getAppContext if it is not in the same directory as beacon.\
\
\
CHANGING JAVA SECURITY POLICY FOR RMI\
------------------------------------------------------------------\
Changing java.policy file on your system to trust brokerPlugin code and give security permissions to it:\
\
For my mac-mini I found the file in /Library/Java/Home/lib/security. For linux systems , you could find it in one of these locations- /etc/java-6-openjdk/security/, /etc/java-6-sun/security or just do "whereas java" to know where all you have java libraries and then you can find the file in the security folder. \
\
1. Modify your java.policy file to grant permission for beacon codeBase. Add the following:\
\
grant codeBase "file:path/to/beacon/directory/-" \{\
    permission java.security.AllPermission;\
\};\
\
2. Do the same for the AppBroker code as well\
\
\
RUNNING THE CLIENT FOR BROKER PLUGIN\
-----------------------------------------------------------------\
1. Make sure that it has been long enough since the deployment of beacon that it has discovered the LAN topology - Check that through beacon's web-interface hosted on port 8080 of the server on which beacon is deployed\
2. Compile your code using javac giving the correct classpath, and then run the code. For my code, it looked like the following:\
\
javac -classpath /usr/local/src/cs244-beacon/AppBroker/src/:/usr/local/src/cs244-beacon/net.beaconcontroller.brokerPlugin/target/classes/ InvokeClient.java Client.java\
java -classpath /usr/local/src/cs244-beacon/AppBroker/src/:/usr/local/src/cs244-beacon/net.beaconcontroller.brokerPlugin/target/classes:. InvokeClient\
\
\
\
DEVELOPING YOUR OWN BEACON PLUGIN\
---------------------------------------------------------------\
Instructions for developing you own beacon plugin can be found at: https://openflow.stanford.edu/display/Beacon/Your+First+Bundle. I personally did the development using eclipse and then copied my plugin files to the beacon directory on the linux machine and build it using maven.\
\
BUILDING YOUR PLUGIN USING MAVEN\
----------------------------------------------------------\
Building your own plugin along with beacon using maven:\
\
1. Make sure you have done your plugin development for beacon as described in the section above.\
\
2. Create a "pom.xml" file in the same folder as your plugin (in the same directory where your src, bin, META-INF directory resides)\
\
3. Copy and paste the following in your "pom.xml" file:\
\
<?xml version="1.0" encoding="UTF-8"?>\
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"\
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">\
  <modelVersion>4.0.0</modelVersion>\
  <parent>\
    <artifactId>parent</artifactId>\
    <groupId>net.beaconcontroller</groupId>\
    <version>0.1.0-SNAPSHOT</version>\
    <relativePath>../net.beaconcontroller.parent</relativePath>\
  </parent>\
  <groupId>net.beaconcontroller</groupId>\
  <artifactId>PLUGIN_NAME</artifactId>\
  <version>0.1.0-SNAPSHOT</version>\
  <packaging>eclipse-plugin</packaging>\
</project>\
\
4. Replace PLUGIN-NAME with the name of your beacon plugin\
\
5. Make sure that the <version>0.1.0-SNAPSHOT</version> in "pom.xml" is same is as the version number in the MANIFEST.MF file in your META-INF directory :  Bundle-Version: 0.1.0.qualifier\
\
6. Now, change the "pom.xml" file in net.beaconcontroller.parent directory to include your plugin in the modules section : <module>../PLUGIN-NAME</module>\
\
7. Now, you should be able to build your plugin using maven (make sure you have maven 3.0 or above to build it), \
     - If you have not already done so, go to openflowj directory and type: mvn install\
    - Next, go to net.beaconcontroller.parent and type: mvn integration-test (it will take about 3 minutes to build the whole project)\
\
8. To deploy your plugin along with beacon-product, change the configuration file "beacon.product" inside net.beaconcontroller.product directory to include your plugin:\
     - Modify "plugins" section to include your plugin: <plugin id="PLUGIN-NAME"/>\
     - Modify "configurations" sections to include your plugin: <plugin id="PLUGIN-NAME" autoStart="true" startLevel="0" />\
\
}