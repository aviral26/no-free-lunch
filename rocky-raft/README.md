# Raft
An implementation of the [Raft consensus algorithm] (http://web.stanford.edu/~ouster/cgi-bin/papers/raft-atc14) [1].

[1] Diego Ongaro and John Ousterhout. 2014. In search of an understandable consensus algorithm. In Proceedings of the 2014 USENIX conference on USENIX Annual Technical Conference (USENIX ATC'14), Garth Gibson and Nickolai Zeldovich (Eds.). USENIX Association, Berkeley, CA, USA, 305-320.

##Architecture
1. RaftServer: Handles all network connections and ServerLogic transitions
  * LeaderLogic: Contains all leader related logic
  * CandidateLogic: Contains all candidate related logic
  * FollowerLogic: Contains all follower related logic
2. RaftClient: Provides a way to "lookup", "post" and "changeConfig"
3. RaftLog: Manages the log. Caches log entries and last config entry
  * StackFile: Implements a doubly linked disk based file
4. SuperClient: Wrapper around RaftClient responsible for generating UUIDs and retries

##Features
1. Leader elections
2. Leader/non-leader failures
3. Handles configuration changes
4. Leader waits until it hears from majority before replying to "lookup"
5. Client retries up to 3 times on failures. Provides ID with every "post" to avoid duplicates
6. Log maintains in-memory write-through LRU cache for log entries
7. Log file is doubly linked. Can be efficiently traversed in both directions
