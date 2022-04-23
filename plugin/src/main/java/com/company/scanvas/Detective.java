package com.company.scanvas;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.company.scanvas.model.Alert;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.events.EventListener;
import org.opennms.integration.api.v1.events.EventSubscriptionService;
import org.opennms.integration.api.v1.model.Alarm;
import org.opennms.integration.api.v1.model.InMemoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Vector;

public class Detective implements EventListener {
    private static final String NEW_SUSPECT_UEI = "uei.opennms.org/internal/discovery/newSuspect";
    private static final String NODE_GAINED_INTERFACE_UEI = "uei.opennms.org/nodes/nodeGainedInterface";
    private static final Logger LOG = LoggerFactory.getLogger(Detective.class);
    private final Vector<String> interestingUEIs = new Vector<>();
    private final EventSubscriptionService eventSubscriptionService;
    private final EventForwarder eventForwarder;
    private final ApiClient apiClient;
    private final MetricRegistry metrics = new MetricRegistry();
    private final Counter scansRequested = metrics.counter("scansRequested");
    private final Counter scanRequestsFailed = metrics.counter("scanRequestsFailed");


    public Detective(EventSubscriptionService ess, EventForwarder ef, ApiClient ac) {
        this.eventSubscriptionService = Objects.requireNonNull(ess);
        this.eventForwarder = Objects.requireNonNull(ef);
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
        LOG.error("Received event "+e);
    }

    public void testPost() {
        apiClient.testPost();
    }
    public void init() {
        eventSubscriptionService.addEventListener(this, interestingUEIs);
    }

    public void fini() {
        eventSubscriptionService.removeEventListener(this);
    }

    public void incrementRequestCount() { scansRequested.inc(); }
    public void incrementFailedCount() { scanRequestsFailed.inc(); }

//    @Override
//    public void handleNewOrUpdatedAlarm(Alarm alarm) {
//        if (alarm.getReductionKey().startsWith(UEI_PREFIX)) {
//            // Never forward alarms that the plugin itself creates
//            return;
//        }
//
//        // Map the alarm to the corresponding model object that the API requires
//        Alert alert = toAlert(alarm);
//
//        // Forward the alarm
//        apiClient.sendAlert(alert).whenComplete((v,ex) -> {
//            if (ex != null) {
//                eventsForwarded.mark();
//                eventForwarder.sendAsync(ImmutableInMemoryEvent.newBuilder()
//                        .setUei(SEND_EVENT_FAILED_UEI)
//                        .addParameter(ImmutableEventParameter.newBuilder()
//                                .setName("reductionKey")
//                                .setValue(alarm.getReductionKey())
//                                .build())
//                        .addParameter(ImmutableEventParameter.newBuilder()
//                                .setName("message")
//                                .setValue(ex.getMessage())
//                                .build())
//                        .build());
//                LOG.warn("Sending event for alarm with reduction-key: {} failed.", alarm.getReductionKey(), ex);
//            } else {
//                eventsFailed.mark();
//                eventForwarder.sendAsync(ImmutableInMemoryEvent.newBuilder()
//                        .setUei(SEND_EVENT_SUCCESSFUL_UEI)
//                        .addParameter(ImmutableEventParameter.newBuilder()
//                                .setName("reductionKey")
//                                .setValue(alarm.getReductionKey())
//                                .build())
//                        .build());
//                LOG.info("Event sent successfully for alarm with reduction-key: {}", alarm.getReductionKey());
//            }
//        });
//    }

    public static Alert toAlert(Alarm alarm) {
        Alert alert = new Alert();
        alert.setStatus(toStatus(alarm));
        alert.setDescription(alarm.getDescription());
        return alert;
    }

    private static Alert.Status toStatus(Alarm alarm) {
        if (alarm.isAcknowledged()) {
            return Alert.Status.ACKNOWLEDGED;
        }
        switch (alarm.getSeverity()) {
            case INDETERMINATE:
            case CLEARED:
            case NORMAL:
                return Alert.Status.OK;
            case WARNING:
            case MINOR:
                return Alert.Status.WARNING;
            case MAJOR:
            case CRITICAL:
            default:
                return Alert.Status.CRITICAL;
        }
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }
}
