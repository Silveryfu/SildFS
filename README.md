SildFS
==
Steady as she goes.

![alt text][logo]

[logo]: https://github.com/Silveryfu/SildFS/blob/master/doc/Sild-stime-610x200.jpg "Sild"

SildFS:

server -dir DIRECTORY [-f PRIMARY_FILE [-p | [-r -ip IP -port PORT -bp BACKUPPORT]]] 

Examples:

To run a primary server:

> server -dir DIRECTORY -f PRIMARY_FILE -p

To run a replica server:

> server -dir DIRECTORY -f PRIMARY_FILE -r -ip IP -port PORT -bp BACKUPPORT

To run a plain server without replication (asm 2):

> server -dir DIRECTORY

# Notice: If you try to run the primary and the replica on the same machine, please make sure the directories are different (so as to the IP if necessary).

Example:

1. We first start a primary server in /home/dif/docs, at IP and port specified in file primary.txt

> -dir /home/dif/docs -p -f ./primary.txt

2. Next, we start a replica server in /home/dif/rdocs, at 127.0.0.1:7777, with backup port number specified by -bp 7778:

> -dir /home/dif/rdocs -r -f ./primary.txt -ip 127.0.0.1 -port 7777 -bp 7778

3. Now the primary-replica pair should start to doing the replication upon client's commit request. If the replica crash, simply use the same command to restart it.

4. If the server crashes, and one wants to restart it, there are two candidate options:

> -dir /home/dif/docs -r -f ./primary.txt -ip 127.0.0.1 -port 7779 -bp 7780

This is equivalent to starting a new replica to the current primary providing the IP, port, as well as the backup port. Noticeably, the directory remains the same as the original.




 
