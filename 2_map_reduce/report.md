# Distributed Systems: Programming Assignment 2 documentation

Team name:

Student 1

  name: Michail Athanasios Kalligeris Skentzos (Thanos)

  student number: 439831

Student 2:

  name: Jyothis Gireesan Mini (Jo)

  student number: 3777103
  

Design and implementation decisions
-----------------------------------
One or two paragraphs per question below is fine.

- Explain who did what for this assignment.

Jo did the RMI implementation and the map phase in a distributed manner with queues and the coordinator hearbeats as well as node failute logic. Thanos did the logs, implemented the combiner logic with hashmaps, as well as the reduce phase. Both of us worked on the plots.

- Explain the design and implementation decisions you made to parallelize the code.

The coordinator creates an RMI registry and registers themselves with their hostname so that the clients are able to get map jobs and reduce jobs. Then he splits the filenames into batches. We adjust the batch size according to the number of workers that were passed in the main function. The batches are put into a queue from which the nodes take their jobs using a remote procedure call. The same mechanism is used with the original filenames for the map phase and the intermediate files for the reduce phase. Finally, the postprocessing phase is done only by the Coordinator, we did not implement it in a parallel manner.

- Explain the design and implementation decisions you made to make the framework fault tolerant.

We made each node create an RMI registry and register themselves so that the coordinator can run heartbeat checks using a remote procedure call. This also serves a second purpose, in case of coordination failure the other nodes can access the registry of the newly elected coordinator. Each worker also has a while loop with exception handling to ensure that the connection with the coordinator is going to happen in case they register before him.

The cordinator keeps track of the tasks to be done in a queue and returns lists of filenames to the worker along with an index. When a job is taken from the queue the index and list of filenames are stored as tuples in a list of taken jobs. When the worker finishes the job they inform the coordinator so that the job is removed from the taken list before asking for a new job. If a worker fails during any part of the job he will not respond to the coordinators heartbeats. In that case the coordinator puts the job back in the queue so that it is done at a later time. All workers keep on asking the coordinator if the phase is over and ask for a job if it isnt. This severs both as a barrier and as a way of ensuring that all jobs will be done even if there are worker failures.


- How did you make sure that the consistency of the output is guaranteed?

 The coordinator keeps track of variables for the termination of each stage. For the map phase that depends on if the map job queue is empty, if the list of taken map jobs in empty as well as on if the coordinator has split the intermediate map filenames into the reduce jobs. In this manner workers wont start with the intermediate files before all map jobs are done correctly which protects us from reading corrupted files from failed jobs.

 All filenames from intermediate and output jobs are generated not just by increasing the index since multiple nodes would just overwrite each others files. We include the job number in the filename as well as an index for the flushed files so that nodes don't overwrite each other. In this manner the previous bug of overwriting works as a protective feature since all intermediate corrupted files are overwritten by a new worker starting a failed job.

 Finally, we used synchronized functions for job management from the coordinator so that we avoid race conditions. 

Additional Features
-------------------
- Explain any additional optional features you implemented.

- InfiniBand
We also wanted to test speedup for the application when using InfiniBand instead of Ethernet. For this the infiniband IPs for the nodes were used for registry stub binding and communication. We achieved this by changing the getIP function in Utils so it can be applied across nodes. Eventhough we expected some speedup it was negligible because the communication overhead for Map Reduce is significantly less than the time taken for compute and network file read. We were not able to use infiniband for file read because the NFS was only mounted over Ethernet, and beyond the scope of the current work. We were able to reproduce the performance difference for Assignment 1, which resulted in 6 times lower latency.


Results
-------
- Show the scalability plot for 1 to 15 clients, for BOTH applications. you can use the run-all.sh script to run your code with different numbers of clients.

- Explain the behaviour you observe.

- How does the framework scale? 

- What are the bottlenecks?

- Did you have to compromise on performance when implementing fault-tolerance?


Acknowledgements for any collaboration or outside help received
---------------------------------------------------------------
If applicable...


What does not work in your implementation
-----------------------------------------
If applicable...
