package com.example.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Date;
import java.util.UUID;

public class NDASchemaV1  extends MappedSchema {
    public NDASchemaV1() {
        super(NDASchema.class, 1, ImmutableList.of(PersistentNDA.class));
    }

    @Entity
    @Table(name = "nda_states")
    public static class PersistentNDA extends PersistentState {
        @Column(name = "issuer") private final String issuer;
        @Column(name = "counterparty") private final String counterparty;
        @Column(name = "counterpartyEntity") private final String counterpartyEntity;
        @Column(name = "expiryDate") private final Date expiryDate;
        @Column(name = "startDate") private final Date startDate;
        @Column(name = "terms") private final String terms;
        @Column(name = "state") private final String state;
        @Column(name = "jurisdiction") private final String jurisdiction;
        @Column(name = "keywords") private final String keywords;
        @Column(name = "linear_id") private final UUID linearId;

        public PersistentNDA(String issuer, String counterparty, Date expiryDate, UUID linearId, String counterpartyEntity
                , Date startDate, String terms,String state, String juris, String keywords) {
            this.issuer = issuer;
            this.counterparty = counterparty;
            this.expiryDate = expiryDate;
            this.linearId = linearId;
            this.counterpartyEntity = counterpartyEntity;
            this.startDate = startDate;
            this.terms = terms;
            this.state = state;
            this.jurisdiction = juris;
            this.keywords = keywords;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getCounterparty() {
            return counterparty;
        }

        public Date getExpiryDate() {
            return expiryDate;
        }

        public UUID getId() {
            return linearId;
        }

        public String getCounterpartyEntity() {
            return counterpartyEntity;
        }

        public Date getStartDate() {
            return startDate;
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
    }

}
