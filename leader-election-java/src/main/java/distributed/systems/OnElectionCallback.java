package distributed.systems;

import org.apache.zookeeper.KeeperException;

// Since our programming model is event driven, we need a way to trigger different events
// based on whether the current node was elected to be a leader or it became a worker in order
// to keep the service registry and discovery separate from the leader election logic.
// We will integrate the two using a callback
public interface OnElectionCallback {
    void onElectedToBeLeader() throws InterruptedException, KeeperException;
    void onWorker();
}
