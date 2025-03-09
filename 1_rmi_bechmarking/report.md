# Distributed Systems: Programming Assignment 1 documentation

Team name:

Student 1

  name: Michail Athansios Kalligeris Skentzos (Thanos)

  student number: 4398831

Student 2:

  name: Jyothis Gireesan Mini (Jo)

  student number: 3777103
  

Design and implementation decisions
-----------------------------------
One or two paragraphs per question below is fine.

- Explain who did what for this assignment.
The initial rmi implementation, the sequence number version and the result plotting was done by Thanos. Jo focused on improving the sequence number and implementing the array and hashmap versions. We helped each other with debugging.
  <!-- - Thanos: barrier and sequence number, result plotting
  - Jo: sequence number, array, hashmap -->
- Explain the design and implementation decisions you made.
We converted the procedures to remote procedures by having the ServerInterface extend the Remote interface and its functions throw RemoteExceptions. On the server side we first created a ServerImplementation and exported it as a UnicastRemoteObject to get the server stub, created a Registry and bound the server stub to it. Then we blocked the server until all clients are done and printed the results. The clients try wo connect to the regirsty until they dont get a Remote Exception. Then they get 100 sequence numbers as a warmup and call the barrier function. In the barrier, the use of an atomic variable allowed us to keep track of the nubmer of clients in different threads and block them until all clients have entered them. We used modulo arithmetic in the blocking condition to allow the barrier to be used multiple times. After exiting the barrier the clients started tracking the time and performed the communication and finally sent the time taken to the server. After this initali implementation we decided to block the clients with another barrier and then follow up with the array and complex object connunication. Since the client is the one sending the objects these cases do not need the server to implement synchronized functions as the server is not keeping track of a state, only of the communicated objects. For each experiment (sequential,array,object) we printed metrics in a comma seperated fashion so that we can extract them in a csv file. We wrote a short script for this aggregation and a python file to gather the metrics in a dataframe and plot them. The final results are shown below.

  <!-- - rmi registry: is created by the server, default port, bind and get stub, clients try to connect until they get a valid connection before starting (while loop)
  - barrier: implememted using atomic variables to count number of clients joining the barrier
  - handling concurrency: made getSequenceNumber and setDone sequential so that the number and aggregated time are updated properly without race conditions
  - array: just created a 1000x1000 double array without even initializing and sent from client to server using sendLargeArray (did not need to use synchronized function)
  - hashmap: created string to string hashmap of size 100.000 and sent from client to server using sendComplexObject function (did not need to use synchronized function)
  - experimets: did sequential experimentation by doing each experiment one after another. used barrier to synchronize clients before each experiment (apart from warmup) (had to adjust barrier to use modulo arithmetic) -->

Results
-------
- Show the scalability plot for 1 to 15 clients. you can use the run-all.sh script to run your code with different numbers of clients.

- Explain the behaviour you observe.
We take note of two interesting observations: First latency increases slightly with number of clients, mostly due to synchronization conditions (we also tested without the synchronous keyword which led to lower latency but also incorrect sequential behaviour due to race conditions). We speculate that this takes place because the more clients that establish a connection with a server using the same remote procedure call, the more threads are trying to access the same synchronized function which leads to queueing of the function call from the threads as well as scheduling overhead. Second is that throughput for arrays and complex objects is much higher than for the sequential case. This probably happens because the former two have a lot more bytes transferred per remote procedure call than the later. Due to the increasing number of procedure calls the sequential case has the overhead of multiple copies, context switching as well as scheduling for a simple integer whereas the former perform much larger data transfer for each of these calls which leads to a higher throughput. 


Acknowledgements for any collaboration or outside help received
---------------------------------------------------------------
If applicable...


What does not work in your implementation
-----------------------------------------
If applicable...
