import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    // Zookeeper will constantly ping to client, if session timeout limit exceeds,
    // zookeeper will consider the client dead
    private static final int SESSION_TIMEOUT = 3000;
    private static final String ELECTION_NAMESPACE = "/election";
    private ZooKeeper zooKeeper;
    private String currentZnodeName;

    public static void main (String[] args) throws IOException, InterruptedException, KeeperException {
        LeaderElection leaderElection = new LeaderElection();
        leaderElection.connectToZookeeper();
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();
        leaderElection.run();
        leaderElection.close();
        System.out.println("Disconnected from Zookeeper, exiting application :)");

    }

    public void connectToZookeeper() throws IOException {
        // Zookeeper API is a synchronous and event driven and to get notified about events
        // such as successful connection or disconnection, we need to register an event handler
        // and pass it to the zookeeper as a watcher object
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
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
                return;
            }else{
                System.out.println("I am not the leader");
                int prevLeaderIndex = Collections.binarySearch(children, currentZnodeName) - 1;
                prevLeaderName = children.get(prevLeaderIndex);
                prevLeaderStat = zooKeeper.exists(ELECTION_NAMESPACE + "/" + prevLeaderName, this);
            }

        }

        System.out.println("Watching znode " + prevLeaderName);
        System.out.println();


    }

    // Since Zookeeper is event driven, events from zookeeper come from different thread
    // So, before zookeeper has a chance to respond and trigger an event on that thread, the application finishes
    // Hence, implement the run() method to put the "main" thread to wait state
    public void run() throws InterruptedException {
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    // When "main" thread wakes up from notifyAll() method, it runs this method and the close the resources
    public void close() throws InterruptedException {
        zooKeeper.close();
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
            case None:
                if(watchedEvent.getState() == Event.KeeperState.SyncConnected){
                    System.out.println("Successfully connected to the Zookeeper server !!!");
                }
                // if zookeeper connection is lost or closed
                else {
                    // wake up the main thread, allow application to close resources and exit
                    synchronized (zooKeeper){
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;

            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException e) {
                } catch (KeeperException e) {
                }
        }

    }
}
