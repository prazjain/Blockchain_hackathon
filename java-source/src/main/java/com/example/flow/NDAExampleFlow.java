package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.NDAContract;
import com.example.state.NDAState;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndContract;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.Date;
import java.util.stream.Collectors;

import static com.example.contract.NDAContract.NDA_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class NDAExampleFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private final Date startDate;
        private final String otherPartyEntity;
        private final String terms;
        private final Date expiryDate;
        private final Party otherParty;
        private final String state;
        private final String jurisdiction;
        private final String keywords;

        private final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new NDA.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints.");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the counterparty's signature.") {
            @Override public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            @Override public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
        // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
        // function.
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );

        public Initiator(Date expiryDate, Party otherParty,Date startDate, String otherPartyEntity,String terms,String state, String juris,String keywords) {
            this.expiryDate = expiryDate;
            this.otherParty = otherParty;
            this.startDate = startDate;
            this.otherPartyEntity = otherPartyEntity;
            this.terms = terms;
            this.state = state;
            this.jurisdiction = juris;
            this.keywords = keywords;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to the notary we want to use.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            // Stage 1.
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            // Generate an unsigned transaction.
            NDAState ndaState = new NDAState(expiryDate, startDate , getServiceHub().getMyInfo().getLegalIdentities().get(0), otherParty, otherPartyEntity , terms, state , jurisdiction, keywords );
            final Command<NDAContract.Commands.Create> txCommand = new Command<>(new NDAContract.Commands.Create(),
                    ndaState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary).withItems(new StateAndContract(ndaState, NDA_CONTRACT_ID), txCommand);

            // Stage 2.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid.
            txBuilder.verify(getServiceHub());

            // Stage 3.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);


            FlowSession otherPartySession = initiateFlow(otherParty);

            // Stage 4.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(partSignedTx, Sets.newHashSet(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Stage 5.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            // Notarise and record the transaction in both parties' vaults.
            return subFlow(new FinalityFlow(fullySignedTx));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Acceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Acceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be an NDA transaction.", output instanceof NDAState);
                        NDAState ndaState = (NDAState) output;
                        require.using("NDA Terms need to be a bit specific.", (ndaState.getTerms()!=null && !ndaState.getTerms().isEmpty()));
                        require.using("NDA Counterparty entity name cannot be blank", ndaState.getCounterpartyEntity() != null && !ndaState.getCounterpartyEntity().isEmpty());
                        require.using("NDA Start date must be before the expiry date", ndaState.getStartDate().compareTo(ndaState.getExpiryDate()) < 0);
                        require.using("I won't accept NDA that have expired in past.", ndaState.getExpiryDate().compareTo(new Date()) > 0);
                        require.using("NDA state cannot be undefined.", (ndaState.getState()!=null && !ndaState.getState().isEmpty()));
                        require.using("NDA jurisdiction need to be a bit specific.", (ndaState.getJurisdiction()!=null && !ndaState.getJurisdiction().isEmpty()));

                        return null;
                    });
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
