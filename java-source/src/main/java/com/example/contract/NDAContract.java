package com.example.contract;

import com.example.state.NDAState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.util.Date;
import java.util.stream.Collectors;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class NDAContract implements Contract {
    public static final String NDA_CONTRACT_ID = "com.example.contract.NDAContract";


    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<NDAContract.Commands.Create> command = requireSingleCommand(tx.getCommands(), NDAContract.Commands.Create.class);
        requireThat(require -> {
            // Generic constraints around the IOU transaction.
            require.using("No inputs should be consumed when issuing an NDA.",
                    tx.getInputs().isEmpty());
            require.using("Only one output state should be created.",
                    tx.getOutputs().size() == 1);
            final NDAState out = tx.outputsOfType(NDAState.class).get(0);
            require.using("The issuer and the counterparty cannot be the same entity.",
                    out.getIssuer() != out.getCounterparty());
            require.using("All of the participants must be signers.",
                    command.getSigners().containsAll(out.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));

            // NDA-specific constraints.
            require.using("NDA Terms need to be a bit specific.", (out.getTerms()!=null && !out.getTerms().isEmpty()));
            require.using("NDA Counterparty entity name cannot be blank", out.getCounterpartyEntity() != null && !out.getCounterpartyEntity().isEmpty());
            require.using("NDA Start date must be before the expiry date", out.getStartDate().compareTo(out.getExpiryDate()) < 0);
            require.using("The NDA's expiry date must not be in past.",
                    out.getExpiryDate().compareTo(new Date()) > 0);

            return null;
        });
    }
    /**
     * This contract only implements one command, Create.
     */
    public interface Commands extends CommandData {
        class Create implements NDAContract.Commands {}
    }
}
