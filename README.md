## Design decisions: 
- The FS is designed to scale horizontally and be highly available
- If there is less than the minimum # of services 'up', then everything fails.
- We will prioritize high availability of reads even though this may result
in old data. No read locks are used.
- Only write locks are used to avoid data corruption.
    - Write locks are acquired at the beginning of a write operation
    - Locks are negotiated using 2-phase locks with backoff
    - Write locks restrict other writes and reads
- Writes
    - The last write on a file will always win.
    - Writes fail fast!!
        - if we can't reach the minimum # of servers we fail
        - if we use a white/black list for MIME then there must be atlest 
        enough white-listed servers to satisfy the minimum limit
- Reads:
    - Reads are meant to be highly available
    - the file might change after the user requests it (write happens)
    - the file might not be 'available' if there is a write (write lock)
    in progress. This is to avoid data corruption
- Unless the minimum limit is set to the total # of services, it is possible 
to get inconsistent data across services.
    - The FS server will employ resolution by updating all the services with
    the latest file found
    - Each file will have a timestamp that determines which is the 'latest'
- Caching:
    - The current version will not implement any local chaching on the server 
    nodes however to improve performance this should be added in future versions

## Implications of the design:
- By not using read locks, it is possible to have old data.
     - User (A) might read a file, which might then be updated by user (B)
     - User (A) now has an outdated copy of the file
- Due to this and the policy that the last write wins, it is possible to 
lose data if a user updates a file that had old data.
    - If user (A) now save the file, changes that user (B) made are lost
    - In future version it would be possible to employ read locks to 
    prevent read and write discrepancy
    - In future version we could also employ manual and automated 
    conflict resolution
    - In future it might also be possible to acquire these locks ahead of time
    and prevent any reads until the locks is released. This sacrifices HA.
- Given that we have 4 services and a minimum limit of 3
    - If 2 services go 'offline' then all actions will fail 
- The timestamp is the single source source of truth and failure
    - Clock synchronization is necessary amongst FS servers to ensure we 
    clearly identify the latest file. However, this might be left to future updates

## Recommendations
Reads are highly available by design but can be made to be consistent by
sacrificing availability. CAP theorem :)

**To gain consistency, it is recommended that the minimum limit maintain 
atleast 51% quorum**
- If the minimum limit is set to atleast 51% quorum (reads and writes) 
then we can guarantee the user will read the latest data written. 
    - Because we write to atleast 51% and then ask 51%, atleast 1 server 
    must have the latest file. 
- We do sacrifice availability since if less than minimum servers are 'up' 
then the entire FS is not available 

