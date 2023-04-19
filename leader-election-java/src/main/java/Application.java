import distributed.systems.LeaderElection;
import distributed.systems.ServiceRegistry;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Application implements Watcher {

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";
    // Zookeeper will constantly ping to client, if session timeout limit exceeds,
    // zookeeper will consider the client dead
    private static final int SESSION_TIMEOUT = 3000;
    private static final int DEFAULT_PORT = 8080;
    private ZooKeeper zooKeeper;

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        int currentServerPort = args.length == 1 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Application application = new Application();
        ZooKeeper zooKeeper = application.connectToZookeeper();

        ServiceRegistry serviceRegistry = new ServiceRegistry(zooKeeper);
        OnElectionAction onElectionAction = new OnElectionAction(serviceRegistry, currentServerPort);

        LeaderElection leaderElection = new LeaderElection(zooKeeper, onElectionAction);
        leaderElection.volunteerForLeadership();
        leaderElection.reelectLeader();

        application.run();
        application.close();
        System.out.println("Disconnected from Zookeeper, closing application ....");

    }

    public ZooKeeper connectToZookeeper() throws IOException {
        // Zookeeper API is a synchronous and event driven and to get notified about events
        // such as successful connection or disconnection, we need to register an event handler
        // and pass it to the zookeeper as a watcher object
        this.zooKeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, SESSION_TIMEOUT, this);
        return zooKeeper;
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

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            // general zookeeper connection events don't have any type
            // so check the state of the zookeeper and if "SyncConnected" means we successfully
            // synchronized with the zookeeper server
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to the Zookeeper server !!!");
                }
                // if zookeeper connection is lost or closed
                else {
                    // wake up the main thread, allow application to close resources and exit
                    synchronized (zooKeeper) {
                        System.out.println("Disconnected from Zookeeper event");
                        zooKeeper.notifyAll();
                    }
                }
                break;
        }
    }
}
