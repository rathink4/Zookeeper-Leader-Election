package distributed.systems;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServiceRegistry implements Watcher {

    // Parent Znode name = "/service_registry"
    private static final String REGISTRY_ZNODE = "/service_registry";
    private final ZooKeeper zooKeeper;
    private String currentZnode = null;

    // Cache to hold all the nodes of the cluster to avoid calling .getChildern() method everytime we need to update the addresses
    private List<String> allServiceAddresses = null;

    public ServiceRegistry(ZooKeeper zooKeeper){
        this.zooKeeper = zooKeeper;
        createServiceRegistry();
    }

    // When we instantiate the Service Registry, we try to create the "/service_registry" Znode
    // If it doesn't exist then we can create a Persistent Znode
    // Obviously this creation of the Znode will have a race condition, cause two nodes can run the exists() method and get null
    // However, zookeeper handles this by allowing only one Znode to call on the create() method
    public void createServiceRegistry(){
        try{
            if(zooKeeper.exists(REGISTRY_ZNODE, false) == null){
                zooKeeper.create(REGISTRY_ZNODE, new byte[]{}, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT_SEQUENTIAL);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    // Takes in metadata but this case it is the http://host_name:port
    // Obviously, this metadata can vary according to the use-case of the application and
    // can have any configuration that the service registry is meant to hold and share with the cluster
    public void registerToCluster(String metadata) throws InterruptedException, KeeperException {
        this.currentZnode = zooKeeper.create(REGISTRY_ZNODE + "/n_", metadata.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        System.out.println("Registered to service registry");
    }

    // Initial call for updating addresses
    public void registerForUpdates(){
        try {
            updateAddresses();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    // This is very useful if the node is gracefully shut down itself or if the worker suddenly becomes a leader
    // so it would want to unregister to avoid communicating with itself
    public void unregisterFromCluster() throws InterruptedException, KeeperException {
        if(currentZnode != null && zooKeeper.exists(currentZnode, false) != null){
            zooKeeper.delete(currentZnode, -1);
        }
    }

    // This will give us those cached results
    public synchronized List<String> getAllServiceAddresses() throws InterruptedException, KeeperException {
        // If the allServiceAddresses is null then the caller simply forgot to register for updates so to make
        // this call safe we will first call to updateAddresses() method
        if(allServiceAddresses == null){
            updateAddresses();
        }
        return allServiceAddresses;
    }

    // This will help with getting updates about nodes joining and leaving the cluster from the service registry
    // This will make sure that all the nodes have an up-to-date configuration information about the nodes
    // Because the method is synchronized, this entire update will happen atomically
    public synchronized void updateAddresses() throws InterruptedException, KeeperException {
        List<String> workerZnodes = zooKeeper.getChildren(REGISTRY_ZNODE, this);
        List<String> addresses = new ArrayList<>(workerZnodes.size());

        for(String worker : workerZnodes){
            String workerFullPath = REGISTRY_ZNODE + "/" + worker;

            // Calling the exists() method to get a Stat as a pre-requisite for getting the znode's data
            Stat stat = zooKeeper.exists(workerFullPath, false);
            // If between getting the list of children and calling the exists() method that child znode disappears, stat will be null
            if(stat == null){
                continue;
            }

            byte [] addressBytes = zooKeeper.getData(workerFullPath, false, stat);
            String address = new String(addressBytes);

            addresses.add(address);

        }

        // When we're done getting all addresses from the service registry we will wrap that list with an unmodifiable
        // list object and store it in the all service addresses member variable
        this.allServiceAddresses = Collections.unmodifiableList(addresses);

        System.out.println("The cluster addresses are: " + allServiceAddresses);
    }

    // We have all the addresses in the member variable, and we have also registered for updates on any changes
    // So, handle those changes events inside the process() method.
    @Override
    public void process(WatchedEvent watchedEvent) {
        try {
            // this will update our all service addresses variable and re-register us for future updates.
            updateAddresses();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }
}
