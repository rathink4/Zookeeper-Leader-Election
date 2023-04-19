package distributed.systems;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ELECTION_NAMESPACE = "/election";
    private final ZooKeeper zooKeeper;
    private final OnElectionCallback onElectionCallback;
    private String currentZnodeName;

    public LeaderElection(ZooKeeper zooKeeper, OnElectionCallback onElectionCallback){
        this.zooKeeper = zooKeeper;
        this.onElectionCallback = onElectionCallback;
    }


    public void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zooKeeper.create(znodePrefix, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    public void reelectLeader() throws InterruptedException, KeeperException {
        Stat prevLeaderStat = null;
        String prevLeaderName = "";

        while(prevLeaderStat == null){
            List<String> children = zooKeeper.getChildren(ELECTION_NAMESPACE, false);

            Collections.sort(children);
            String smallestChild = children.get(0);

            if(smallestChild.equals(currentZnodeName)){
                System.out.println("I am leader");
                // if we are elected to be a leader then before returning from the method ,
                // we call the on elected to be leader callback method.
                onElectionCallback.onElectedToBeLeader();
                return;
            }else{
                System.out.println("I am not the leader");
                int prevLeaderIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                prevLeaderName = children.get(prevLeaderIndex);
                prevLeaderStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + prevLeaderName, this);
            }

        }
        // if we're not a leader but a worker then we call onWorker() callback method instead
        onElectionCallback.onWorker();

        System.out.println("Watching znode " + prevLeaderName);
        System.out.println();


    }


    // process method is called by the zookeeper library on a separate (event thread)
    // whenever there is a new event coming from the zookeeper server
    @Override
    public void process(WatchedEvent watchedEvent) {
        // figure out which event type it is
        switch (watchedEvent.getType()){
            // general zookeeper connection events don't have any type
            // so check the state of the zookeeper and if "SyncConnected" means we successfully
            // synchronized with the zookeeper server
//            case None:
//                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
//                    System.out.println("Successfully connected to the Zookeeper server !!!");
//                }
//                // if zookeeper connection is lost or closed
//                else {
//                    // wake up the main thread, allow application to close resources and exit
//                    synchronized (zooKeeper){
//                        System.out.println("Disconnected from Zookeeper event");
//                        zooKeeper.notifyAll();
//                    }
//                }
//                break;

            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException e) {
                } catch (KeeperException e) {
                }
        }

    }
}
