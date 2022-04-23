package com.company.scanvas;

import com.codahale.metrics.MetricRegistry;
import org.opennms.integration.api.v1.events.EventListener;
import org.opennms.integration.api.v1.events.EventSubscriptionService;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Vector;

// the Detective investigates newSuspects :)
public class Detective implements EventListener {
    private static final String NEW_SUSPECT_UEI = "uei.opennms.org/internal/discovery/newSuspect";
    private static final String NODE_GAINED_INTERFACE_UEI = "uei.opennms.org/nodes/nodeGainedInterface";
    private static final Logger LOG = LoggerFactory.getLogger(Detective.class);
    private final Vector<String> interestingUEIs = new Vector<>();
    private final EventSubscriptionService eventSubscriptionService;
    private final ApiClient apiClient;


    public Detective(EventSubscriptionService ess, ApiClient ac) {
        this.eventSubscriptionService = Objects.requireNonNull(ess);
        this.apiClient = Objects.requireNonNull(ac);

        interestingUEIs.add(NEW_SUSPECT_UEI);
        interestingUEIs.add(NODE_GAINED_INTERFACE_UEI);
    }

    @Override
    public String getName() {
        return Detective.class.getName();
    }

    @Override
    public int getNumThreads() {
        return 1;
    }

    @Override
    public void onEvent(InMemoryEvent e) {
        LOG.debug("Received event "+e);
        if(e.getUei().equals(NODE_GAINED_INTERFACE_UEI))
            apiClient.scheduleScan(e.getNodeId(), e.getParameterValue("iphostname").orElseThrow());
        else if(e.getUei().equals(NEW_SUSPECT_UEI))
            LOG.error(NEW_SUSPECT_UEI+" is not handled yet");
    }

    public void testPost() {
        apiClient.scheduleScan(1,"127.0.0.1");
    }
    public void init() {
        eventSubscriptionService.addEventListener(this, interestingUEIs);
    }

    public void fini() {
        eventSubscriptionService.removeEventListener(this);
    }

    public MetricRegistry getMetrics() {
        return apiClient.getMetrics();
    }
}
