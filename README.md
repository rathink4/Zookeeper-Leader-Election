# Zookeeper-Leader-Election

The project uses Apache Zookeeper `(v3.4.12)` to solve Leader Election algorithm in Distributed Systems.

Makes use of the Zookeeper API to create children Znodes in `/election` zNode in the Zookeeper.

Leader keeps track of each systems hostname, port and the protocol used in a Service Registry.

There exists a `/service_registry` Persistent zNode in the ZooKeeper and all the systems make an Ephemeral Znode which will hold the hostname and port.
The `/service_registry` zNode will keep a track of these and update accordingly when a system goes down and comes back up again.

Everything was done in Ubuntu OS (recommended to do in any Linux OS if you are new to Zookeeper, Windows can be a bit tricky to set up the ZK and run it)

## Project Setup

The project code is in the `LeaderElection.java` file.

Before you work on this project, you need to install Apache Zookeeper `(v3.4.12)` and edit the `zoo.sample.conf` file in the `conf` folder.
Rename it to `zoo.conf`, make a `logs` folder in the Zookeeper folder (one level below the conf folder), and change the dataDirs = `Zookeeper_file_location\logs`.

Also depending on your java jdk, change the `pom.xml` to have the correct source and target compiler.

## How to run

Open a new terminal in the Zookeeper file location and go to `cd bin`.

Run the `zkServer.sh` file by executing the following command --> `./zkServer.sh start`

## Server started, now what to do?

Open the maven project in Intellij, open a terminal and create a jar file.

Do `mvn clean package` to create a jar file. It will be located in the `target` folder and you can open this folder in the terminal.

Open 2-3 additional terminals and run the jar file by executing `java -jar jar_filename (port_number)` 

You will see the result.