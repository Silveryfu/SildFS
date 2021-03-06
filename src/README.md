SildFS:

server -dir DIRECTORY [-f PRIMARY_FILE [-p | [-r -ip IP -port PORT]]] 

Examples:

To run a primary server:

> server -dir DIRECTORY -f PRIMARY_FILE -p

To run a replica server:

> server -dir DIRECTORY -f PRIMARY_FILE -r -ip IP -port PORT

To run a plain server without replication (asm 2):

> server -dir DIRECTORY

Example:

1. We first start a primary server in /home/dif/docs, at IP and port specified in file primary.txt

> -dir /home/dif/docs -p -f ./primary.txt

2. Next, we start a replica server in /home/dif/rdocs, at 127.0.0.1:7777:

> -dir /home/dif/rdocs -r -f ./primary.txt -ip 127.0.0.1 -port 7777

3. Now the primary-replica pair should start to doing the replication upon client's commit request. If the replica crash, simply use the same command to restart it.

4. If the server crashes, and one wants to restart it, there are two candidate options:

> -dir /home/dif/docs -r -f ./primary.txt -ip 127.0.0.1 -port 7779

This is equivalent to starting a new replica to the current primary providing the IP, port, as well as the backup port. Noticeably, the directory remains the same as the original.




 
