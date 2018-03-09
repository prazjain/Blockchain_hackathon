package com.example.api;

import com.example.flow.NDAExampleFlow;
import com.example.state.NDAState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.node.NodeInfo;
import net.corda.core.transactions.SignedTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

@Path("NDAexample")
public class NDAExampleApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Controller", "Network Map Service");

    static private final Logger logger = LoggerFactory.getLogger(ExampleApi.class);

    public NDAExampleApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }
    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    @GET
    @Path("ndaStates")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getNDAStates() {
        String[] states = new String[] {"PENDING","ACCEPTED","CANCELLED"};
        return Arrays.asList(states);
    }
    /**
     * Displays all NDA states that exist in the node's vault.
     */
    @GET
    @Path("ndas")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<NDAState>> getIOUs() {
        return rpcOps.vaultQuery(NDAState.class).getStates();
    }

    private Date getDateFromString(String strDate){
        String[] str = strDate.split("-");
        if (str.length != 3) {
            //invalid
        }

        int year = Integer.parseInt(str[0]);
        int month = Integer.parseInt(str[1]) + 1;
        int date = Integer.parseInt(str[2]);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        cal.set(year,month,date,0,0);
        Date newDate = cal.getTime();
        return newDate;
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PUT
    @Path("create-nda")
    public Response createNDA(@QueryParam("expiryDate") String expiryDateString, @QueryParam("partyName") CordaX500Name partyName
                            ,@QueryParam("startDate") String startDateString
                              ,@QueryParam("partyNameEntity") String partyNameEntity
                              ,@QueryParam("terms") String terms
                            ,@QueryParam("state") String state
                            ,@QueryParam("juris") String jurisdiction
                            ,@QueryParam("keywords") String keywords) throws InterruptedException, ExecutionException {

        Date expiryDate = getDateFromString(expiryDateString);

        if (expiryDate.compareTo(new Date()) < 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'expiryDate' must not be in past. "+ expiryDateString +"\n").build();
        }

        Date startDate = getDateFromString(startDateString);
        if (startDate.compareTo(expiryDate) >= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'startDate' must be before 'expiryDate'. "+ startDateString +"\n").build();
        }

        if (partyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyName' missing or has wrong format.\n").build();
        }

        if (partyNameEntity == null || partyNameEntity.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'partyNameEntity' missing or has wrong format.\n").build();
        }

        if (terms == null || terms.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'terms' missing or has wrong format.\n").build();
        }

        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(partyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + partyName + "cannot be found.\n").build();
        }
        if (state == null || state.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'state' missing or has wrong format.\n").build();
        }
        if (jurisdiction == null || jurisdiction.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'jurisdiction' missing or has wrong format.\n").build();
        }
        if (keywords == null || keywords.isEmpty()) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'keywords' missing or has wrong format.\n").build();
        }
        try {
            FlowProgressHandle<SignedTransaction> flowHandle = rpcOps
                    .startTrackedFlowDynamic(NDAExampleFlow.Initiator.class, expiryDate, otherParty,startDate, partyNameEntity,terms,state,jurisdiction,keywords);
            flowHandle.getProgress().subscribe(evt -> System.out.printf(">> %s\n", evt));

            // The line below blocks and waits for the flow to return.
            final SignedTransaction result = flowHandle
                    .getReturnValue()
                    .get();

            final String msg = String.format("Transaction id %s committed to ledger.\n", result.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
}
