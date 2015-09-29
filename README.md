# CS2105-Reliable-File-Transfer-Protocol

CS2105 Programming Assignment 2
Done by: Chng Hui Yie

Work in progress!

The FileSender and FileReceiver programs implement a reliable file transfer protocol over UDP.

FileSender sends all packets to UnreliNET, which simulates an unreliable channel by randomly
discarding, corrupting and reordering packets before forwarding them to the FileReceiver.

FileReceiver receives a file from FileSender (via UnreliNET) and saves it in the same directory
it is running in, with the filename specified by the FileSender program.

