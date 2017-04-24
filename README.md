# pub_sub_eventService-Java
To run this demo, you need to add zmq library to your enviornment and add zmq .o and .dll files in to your java path.
I tested it on Intellij idea ide, it works well

You can run this sample code in any network environment you want. You should input port number for each pub, eventService and sub, notice that one pub can connect to only one eventService but one eventService can bind any numbers of pub or sub. In subscriber, you should point out which topic you want to subscribe.

Do javac pub.java & javac eventService.java & javac sub.java to compile, then do java pub & java eventService & java sub to run the sample

History function has been implemented, strength function has not been implemented
