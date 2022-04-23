package com.company.scanvas.shell;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.company.scanvas.Detective;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.concurrent.TimeUnit;

@Command(scope = "opennms-scanvas", name = "stats", description = "Show statistics.")
@Service
public class StatsCommand implements Action {

    @Reference
    private Detective detective;

    @Override
    public Object execute() {
        final MetricRegistry metrics = detective.getMetrics();

        final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();
        reporter.report();
        return null;
    }
}
