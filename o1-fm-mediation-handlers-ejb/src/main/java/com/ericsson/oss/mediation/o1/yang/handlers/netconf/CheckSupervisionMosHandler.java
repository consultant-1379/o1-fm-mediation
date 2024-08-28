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

package com.ericsson.oss.mediation.o1.yang.handlers.netconf;

import static com.ericsson.oss.mediation.o1.yang.handlers.netconf.api.CommonConstants.ACTIVE;

import javax.inject.Inject;

import org.slf4j.Logger;

import com.ericsson.oss.itpf.common.event.handler.annotation.EventHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1AbstractHandler;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationResult;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationStatus;
import com.ericsson.oss.mediation.o1.common.handlers.netconf.O1NetconfOperationsStatusHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.NetconfHelper;
import com.ericsson.oss.mediation.o1.yang.handlers.netconf.util.XPathFilter;
import com.ericsson.oss.mediation.util.netconf.api.NetconfManager;
import com.ericsson.oss.mediation.util.netconf.api.NetconfResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * This handler is used to support the 'idempotent' FmSupervisionFlow use case and is agnostic to the client type that
 * invoked it. For example, if the alarm supervision
 * controller sends an FmSupervisionRecoveryTaskRequest, this will trigger the FmSupervisionFlow again.
 * <br><br>
 * A call is made to the node to check the existence of the HeartbeatControl MO and this is used along with the value
 * of the supervision header property 'active' to determine whether the subsequent handlers should be skipped or not to
 * support idempotency cases.
 */
@Slf4j
@EventHandler
public class CheckSupervisionMosHandler extends O1AbstractHandler {

    @Inject
    private NetconfHelper netconfHelper;

    @Override
    public O1NetconfOperationStatus executeO1Handler() {

        final O1NetconfOperationStatus o1NetconfOperationStatus = createO1NetconfOperationStatusSuccess();

        NetconfManager netconfManager = null;

        try {
            netconfManager = netconfHelper.getNetconfManager(headers);
            netconfHelper.connect(netconfManager);

            NetconfResponse netconfResponse = netconfHelper.readMo(netconfManager, getHeartbeatControlXpath());
            boolean heartbeatControlExists = checkHeartbeatControlMoExists(netconfResponse);

            boolean isIdempotencyCase = isIdempotencyCase(heartbeatControlExists);
            if (isIdempotencyCase) {
                log.trace("Idempotency case encountered, the subsequent handlers will be aborted...");
                o1NetconfOperationStatus.setResult(O1NetconfOperationResult.OPERATION_FAILED);

                log.trace("Setting flag to suppress terminating the flow by the error handler");
                O1NetconfOperationsStatusHelper.setSuppressTerminateFlow(true, headers);
            }

        } finally {
            netconfHelper.disconnect(netconfManager);
        }
        return o1NetconfOperationStatus;
    }

    private boolean isIdempotencyCase(final boolean heartbeatControlExists) {
        return isSupervisionActive() == heartbeatControlExists;
    }

    private boolean isSupervisionActive() {
        return (boolean) headers.get(ACTIVE);
    }

    /*
     * If no HeartbeatControl MO exists on the node then an <rpc-error> response is returned.
     * If the response is an error - then as the NtfSubscriptionControl MO is the parent MO, this also does not exist.
     */
    private boolean checkHeartbeatControlMoExists(final NetconfResponse netconfResponse) {
        boolean exists = !netconfResponse.isError();
        log.trace("HeartbeatControl MO exists? [{}]", exists);
        return exists;
    }

    private static XPathFilter getHeartbeatControlXpath() {
        return new XPathFilter("/ManagedElement/NtfSubscriptionControl/HeartbeatControl");
    }

    @Override
    protected Logger getLogger() {
        return log;
    }
}
