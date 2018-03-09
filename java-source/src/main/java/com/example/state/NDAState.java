package com.example.state;

import com.example.schema.NDASchemaV1;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class NDAState implements LinearState, QueryableState {

    private final Date expiryDate;
    private final Date startDate;
    private final Party issuer;
    private final Party counterparty;
    private final String counterpartyEntity;
    private final UniqueIdentifier linearId;
    private final String terms;
    private final String state;
    private final String jurisdiction;
    private final String keywords;

    public NDAState(Date expDate,
                    Date startDate, Party issuer,
                    Party counterparty, String counterpartyEntity, String terms, String state, String jurisdiction,String keywords)
    {
        this.expiryDate = expDate;
        this.startDate  = startDate;
        this.issuer = issuer;
        this.counterparty = counterparty;
        this.counterpartyEntity = counterpartyEntity;
        this.terms = terms  ;
        this.state = state;
        this.jurisdiction = jurisdiction;
        this.keywords = keywords;
        this.linearId = new UniqueIdentifier();
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public Party getIssuer() {
        return issuer;
    }

    public Party getCounterparty() {
        return counterparty;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getCounterpartyEntity() {
        return counterpartyEntity;
    }

    public String getTerms() {
        return terms;
    }

    public String getState() {
        return state;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getKeywords() {
        return keywords;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(issuer, counterparty);
    }

    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof NDASchemaV1) {
            return new NDASchemaV1.PersistentNDA(
                    this.issuer.getName().toString(),
                    this.counterparty.getName().toString(),
                    this.expiryDate,
                    this.linearId.getId(),this.counterpartyEntity , this.startDate , this.terms,this.state, this.jurisdiction, keywords );
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new NDASchemaV1());
    }

    @Override
    public String toString() {
        return String.format("%s(nda=%s, issuer=%s, counterparty=%s, linearId=%s)", getClass().getSimpleName(), expiryDate.toString(), issuer, counterparty, linearId);
    }

}
