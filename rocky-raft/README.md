# rocky-raft
An implementation of the [Raft Consensus Algorithm] (http://web.stanford.edu/~ouster/cgi-bin/papers/raft-atc14)

##Architecture
1. RaftServer: Handles all network connections and ServerLogic transitions
  * LeaderLogic: Contains all leader related logic
  * CandidateLogic: Contains all candidate related logic
  * FollowerLogic: Contains all follower related logic
2. RaftClient: Provides a way to `lookup`, `post` and `changeConfig`
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

##Build
Uses `ant` build system. Just type the below command to build both `client.jar` and `server.jar`
```
ant
```

##Run
###Server
RaftServer uses the last config entry in it's log if present or reads the config from `default.config`. Expects an integer argument `<id>` to determine it's IP address and ports from the config.
```
java -jar server.jar <id>
```

###Client
#####Lookup
```
java -jar client.jar l <optional server ID>
```
#####Post
```
java -jar client.jar p "Message" "<optional ID>"
```
#####Config changes
```
java -jar client.jar c "<config file>"
```