/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.oss.mediation.fm.o1.handlers.dps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import com.ericsson.oss.itpf.datalayer.dps.BucketProperties;
import com.ericsson.oss.itpf.datalayer.dps.DataBucket;
import com.ericsson.oss.itpf.datalayer.dps.DataPersistenceService;
import com.ericsson.oss.itpf.datalayer.dps.query.ObjectField;
import com.ericsson.oss.itpf.datalayer.dps.query.Query;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.QueryPathExecutor;
import com.ericsson.oss.itpf.datalayer.dps.query.Restriction;
import com.ericsson.oss.itpf.datalayer.dps.query.TypeRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.graph.QueryPath;
import com.ericsson.oss.itpf.datalayer.dps.query.graph.QueryPathRestrictionBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.graph.TargetModel;
import com.ericsson.oss.itpf.datalayer.dps.query.graph.builder.MainPathBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.graph.builder.RelationshipBuilder;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.Projection;
import com.ericsson.oss.itpf.datalayer.dps.query.projection.ProjectionBuilder;
import com.ericsson.oss.itpf.sdk.core.annotation.EServiceRef;

import lombok.extern.slf4j.Slf4j;

/**
 * Class for read only access to DPS.
 * <p>
 * A new transaction is used to prevent it running as part of the flow transaction and terminating the supervision flow prematurely.
 * <p>
 * Read only transactions do not need to be rolled back.
 * <p>
 * Mediation is suppressed.
 */

@Slf4j
@Stateless
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
public class DpsQuery {

    private static final String ACTIVE = "active";
    private static final String OSS_NE_DEF = "OSS_NE_DEF";
    private static final String OSS_NE_FM_DEF = "OSS_NE_FM_DEF";
    private static final String NETYPE = "neType";
    private static final String NETWORK_ELEMENT = "NetworkElement";
    private static final String FM_ALARM_SUPERVISION = "FmAlarmSupervision";

    private static final String O1NODE = "O1Node";
    private static final String FDN = "fdn";

    @EServiceRef
    private DataPersistenceService dps;

    /**
     * Returns a list of the fdn for an O1Node with active supervision.
     * <p>
     */
    public List<Object> getSupervisedListOfNodesFromDatabase() {
        List<Object> supervisionFdns = new ArrayList<>();
        final DataBucket liveBucket = getLiveBucket();
        final QueryBuilder queryBuilder = dps.getQueryBuilder();
        final Query<TypeRestrictionBuilder> typeQuery = queryBuilder.createTypeQuery(OSS_NE_DEF, NETWORK_ELEMENT);
        final Restriction netypeRestricstion = typeQuery.getRestrictionBuilder().equalTo(NETYPE, O1NODE);
        final Projection fdnProjection = ProjectionBuilder.field(ObjectField.MO_FDN);
        typeQuery.setRestriction(netypeRestricstion);

        QueryPathRestrictionBuilder restrictionBuilder =
                liveBucket.getQueryPathExecutor().getRestrictionBuilder();

        final QueryPathExecutor queryPathExecutor = liveBucket.getQueryPathExecutor();

        final QueryPath activeO1NodePath = MainPathBuilder.startWith(MainPathBuilder.fromQuery(typeQuery))
                .withRelationships(
                        RelationshipBuilder.descendant().withTargetModels(TargetModel.exact(OSS_NE_FM_DEF, FM_ALARM_SUPERVISION))
                                .withRestriction(restrictionBuilder.equalTo(ACTIVE, true)))
                .build();
        final Iterable<Map<String, Object>> supervisionFdnsProjection = queryPathExecutor.projectFromTargets(activeO1NodePath, fdnProjection);
        for (Map<String, Object> fdn : supervisionFdnsProjection) {
            supervisionFdns.add(fdn.get(FDN));
        }
        log.debug("NetworkElements with supervision enabled are:{}", supervisionFdns);
        return supervisionFdns;
    }

    private DataBucket getLiveBucket() {
        return dps.getDataBucket("live", BucketProperties.SUPPRESS_MEDIATION);
    }
}
