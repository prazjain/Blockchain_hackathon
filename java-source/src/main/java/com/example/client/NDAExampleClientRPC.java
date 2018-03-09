package com.example.client;

import com.example.state.NDAState;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.concurrent.ExecutionException;

public class NDAExampleClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(ExampleClientRPC.class);

    private static void logState(StateAndRef<NDAState> state) {
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: NDAExampleClientRPC <node address>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        // Grab all signed transactions and all future signed transactions.
        final DataFeed<Vault.Page<NDAState>, Vault.Update<NDAState>> dataFeed = proxy.vaultTrack(NDAState.class);
        final Vault.Page<NDAState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<NDAState>> updates = dataFeed.getUpdates();

        // Log the 'placed' IOUs and listen for new ones.
        snapshot.getStates().forEach(NDAExampleClientRPC::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(NDAExampleClientRPC::logState));
    }
}
