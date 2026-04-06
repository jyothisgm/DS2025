# Distributed Systems: Programming Assignment 1 documentation

## Team name: **MinMax**

Student 1

      Name: Michail Athansios Kalligeris Skentzos (Thanos)

      Student number: 4398831

Student 2:

      Name: Jyothis Gireesan Mini (Jo)

      Student number: 3777103

---

Design and implementation decisions
-----------------------------------
One or two paragraphs per question below is fine.

- Explain who did what for this assignment.

The initial RMI implementation, the sequence number version, and the result plotting were done by Thanos. Jo focused on improving the sequence number and implementing the array and hash map versions. We helped each other with debugging.
  <!-- - Thanos: barrier and sequence number, result plotting
  - Jo: sequence number, array, hashmap -->
- Explain the design and implementation decisions you made.
  
We converted the procedures to remote procedures by having the ServerInterface extend the Remote interface and adding its functions to throw RemoteExceptions. On the server side, we first created a ServerImplementation and exported it as a UnicastRemoteObject to get the server stub, created a Registry, and bound the server stub to it. Then we blocked the server until all clients were done and printed the results. The clients try to connect to the registry until they don't get a Remote Exception. Then, they get 100 sequence numbers as a warmup and call the barrier function. In the barrier, we used an atomic variable to keep track of the number of clients in different threads and block them until all clients have entered. This allowed us to handle race conditions and wait each thread inside the barrier function, which also gave us more control over the behaviour of the barrier function. We used modulo arithmetic in the blocking condition to allow the barrier to be used multiple times, to set up concurrent data transfer for sequential, array, and complex objects with any number of desired clients.

After exiting the barrier, the clients started tracking the time, performed the communication and finally sent the time taken to the server. After this initial implementation, we decided to block the clients with another barrier and follow up with the array and complex object communication. Since the client is sending the objects, these cases do not need the server to implement synchronized functions as the server is not keeping track of a state, only of the communicated objects. For each experiment (sequential, array, object), we printed metrics in a comma-separated fashion so that we can extract them in a CSV file. We wrote a short script for this aggregation and a Python file to gather the metrics in a dataframe and plot them. The final results are shown below.

  <!-- - rmi registry: is created by the server, default port, bind and get stub, clients try to connect until they get a valid connection before starting (while loop)
  - barrier: implemented using atomic variables to count number of clients joining the barrier
  - handling concurrency: made getSequenceNumber and setDone sequential so that the number and aggregated time are updated properly without race conditions
  - array: just created a 1000x1000 double array without even initializing and sent from client to server using sendLargeArray (did not need to use synchronized function)
  - hashmap: created string to string hashmap of size 100.000 and sent from client to server using sendComplexObject function (did not need to use synchronized function)
  - experimets: did sequential experimentation by doing each experiment one after another. used barrier to synchronize clients before each experiment (apart from warmup) (had to adjust barrier to use modulo arithmetic) -->

Results
-------
- Show the scalability plot for 1 to 15 clients. You can use the run-all.sh script to run your code with different numbers of clients.

In the two images below, we see the results of our experiments for latency and throughput. We modified **run-all.sh** to run for 1 to 15 clients and then gathered the results into the results.csv file using **gather.sh**. Finally, we plot the results using **plot.py**. The requirements are just numpy, pandas, and matplotlib.

![Latency plot](results_latency.png)

![Throughput plot](results_throughput.png)

## Explain the behavior you observe.

We take note of three interesting observations: First, latency increases almost exponentially with the increasing number of clients. This happens due to synchronization conditions (we also tested without the synchronous keyword, which led to slightly lower latency but also incorrect sequential behaviour due to race conditions). We speculate that this takes place because the more clients establish a connection with a server using the same remote procedure call, the more threads are trying to access the same synchronized function which leads to queueing of the function call from the threads as well as scheduling overhead. Furthermore, the higher dimensionality of the objects sent also results in a larger serialization overhead and leads to significantly higher latency for the array and complex object experiments.


The second observation is that throughput for arrays and complex objects is much higher than for the sequential case. This probably happens because the former two have a lot more bytes transferred per remote procedure call than the latter. Due to the increasing number of procedure calls, the sequential case has the overhead of multiple copies, context switching and scheduling for a simple integer, whereas the former performs much larger data transfer for each of these calls, which leads to a higher throughput. However, the communication overhead for multiple clients still causes the throughput to drop exponentially, especially in the array case where there are two orders of magnitude difference between 1 and 15 clients.

The third is that latency and throughput are better for Array transfer than Complex Objects (Hashmaps), but it worsens with the increasing number of clients. We speculate this is possibly due to Java Serialization and memory allocation factors. Java can serialize arrays better than complex objects, which leads to faster transmission of data. Moreover, the memory allocated for Arrays is contiguous, while Hashmaps are stored as a collection of smaller objects spread across memory. However, this becomes an issue when multiple transmissions are done concurrently. As concurrency increases, the efficiency of the Array's single transfer becomes a bottleneck and leads to higher latency, whereas complex objects are sent as multiple key-value pairs, which can accommodate concurrency.

Acknowledgements for any collaboration or outside help received
---------------------------------------------------------------
If applicable...


What does not work in your implementation
-----------------------------------------
If applicable...
