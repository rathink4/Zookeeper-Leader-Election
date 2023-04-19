import distributed.systems.OnElectionCallback;
import distributed.systems.ServiceRegistry;
import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

// A class that would implement those callbacks.
public class OnElectionAction implements OnElectionCallback {
    private final ServiceRegistry serviceRegistry;
    private final int port;

    public OnElectionAction(ServiceRegistry serviceRegistry, int port){
        this.serviceRegistry = serviceRegistry;
        this.port = port;
    }

    // Calls unregisterFromCluster method.
    // If the node just joined the cluster this method won't do anything
    // Else if it used to be a worker, but now it got promoted to be a leader, then it would remove itself from the registry
    // After that call the registerForUpdates() method
    @Override
    public void onElectedToBeLeader() throws InterruptedException, KeeperException {
        serviceRegistry.unregisterFromCluster();
        serviceRegistry.registerForUpdates();
    }

    // Create the current server address by combining the local hostname and the port as a single string
    // and then call the registerToCluster() method with the address as the metadata we store for others to see
    @Override
    public void onWorker() {
        try{
            String currentServerAddr =
                    String.format("http://%s:%d", InetAddress.getLocalHost().getCanonicalHostName(), port);
            serviceRegistry.registerToCluster(currentServerAddr);
        } catch (UnknownHostException e) {
        } catch (InterruptedException e) {
        } catch (KeeperException e) {
        }
    }
}
