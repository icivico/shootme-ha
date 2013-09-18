Shootme-HA
==========
This project is a testing implementation of a custom cache for jain-sip-ha from Mobicents. 

Shootme is a simple example from jain-sip library. Jain-sip implementation has been taken from mobicents project. Libraries are included in lib forder or you cat get it from mobicents distribution.

Cache module is implemented with Hazelcast, which is amazingly easy to use.

A timestamp is stored in dialog application data on INVITE request and call length is printed on disconnection. This application data is replicated between nodes.

dialogId is stored in cache on ACK request. In case of attending node failure, if other node has to hangup call (pressing 'h' key in console), it can recover dialog from stack using saved dialogId.


Compilation
-----------
Import this project into eclipse and export project as jar with name shootme.jar


Installation
------------
You need 2 instances or more of shootme-ha for testing. Create 2 forlders and copy these files in each of them:
* exported shootme.jar
* libraries from lib folder
* run.sh
* shootme.properties and log4j.properties

You will need also a sip-balancer 1.6+ installation.

Configuration
-------------
You need to configure these parameters in shootme.properties:
* sip.stack.address: ip address for stack
* sip.stack.port: sip port, set up different ports for each instance
* org.mobicents.ha.javax.sip.CACHE_CLASS_NAME: your cache class
* org.mobicents.ha.javax.sip.BALANCERS: internal port of balancer


You can also tune log4j.properties for more or less traces. jain-sip and hazelcast can write a lot of information.

Running
-------
* cd to sip-balancer folder and start it:

$ java -jar sip-balancer-jar-1.6.0.FINAL-jar-with-dependencies.jar -mobicents-balancer-config=./lb-configuration.properties

* cd to each shootme instance folder and start it:

$ ./run.sh

Now you can call to sip-balancer external port and will see the call progresing on one of the nodes.

Testing
-------
Shootme-ha has been tested in these scenarios:

Node failure
* Call to balancer
* Call attended on node1
* Kill node1
* Hangup call on sip phone
* Node2 hangups call succesfully

Balancer failure
* Call to balancer
* Call attended on node1
* Kill balancer
* Start balancer
* Hangup call on sip phone
* Node1 hangups call succesfully

Node failure and hangup from node
* Call to balancer
* Call attended on node1
* Kill node1
* Press 'h' on node2
* Call is disconnected succesfully


References
----------
* Mobicents project - http://www.telestax.com/opensource/
* Hazelcast - http://www.hazelcast.com
