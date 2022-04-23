package com.company.scanvas;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.company.scanvas.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.opennms.integration.api.v1.events.EventForwarder;
import org.opennms.integration.api.v1.model.immutables.ImmutableEventParameter;
import org.opennms.integration.api.v1.model.immutables.ImmutableInMemoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ApiClient implements HostnameVerifier {
    private final String SCAN_REQUESTED_UEI = "uei.opennms.org/scanvasPlugin/securityScanRequested";
    private final MetricRegistry metrics = new MetricRegistry();
    private final Counter scansRequested = metrics.counter("scansRequested");
    private final Counter scanRequestsFailed = metrics.counter("scanRequestsFailed");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);
    private final String vasUsername;
    private final String vasPassword;
    private final EventForwarder eventForwarder;

    private HashMap<CompletableFuture<Void>, IState> stateMap;

    public ApiClient(String url, String keystore, String password, String vasuser, String vaspass, EventForwarder ef) {
        this.url = Objects.requireNonNull(url);
        this.vasUsername = vasuser;
        this.vasPassword = vaspass;
        this.eventForwarder = ef;
        stateMap = new HashMap<>();

        // we need to use the client certificate to auth ourselves with openVAS
        try {
            FileInputStream fis = new FileInputStream(keystore);
            KeyStore ks = KeyStore.getInstance("jks");
            ks.load(fis, password.toCharArray());

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, password.toCharArray());
            KeyManager keyManager = kmf.getKeyManagers()[0];

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
            tmf.init(ks);
            TrustManager tm = tmf.getTrustManagers()[0];

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{keyManager}, new TrustManager[]{tm}, null);
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

            this.client = new OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, (X509TrustManager) tm)
                .hostnameVerifier(this)
                .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public CompletableFuture<Void> scheduleScan(int node, String host) {
        Initialize init = new Initialize(node, vasUsername, vasPassword, host);
        scansRequested.inc();
        return doPost(url + init.endpoint(), init);
    }

    private CompletableFuture<Void> doPost(String url, IState requestBodyPayload) {
        RequestBody body;
        try {
            body = RequestBody.create(JSON, mapper.writeValueAsString(requestBodyPayload));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", ApiClient.class.getCanonicalName())
                .post(body)
                .build();

        CompletableFuture<Void> future = new CompletableFuture<>();
        stateMap.put(future, requestBodyPayload);
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        LOG.error("request failed: "+e);
                        scanRequestsFailed.inc();
                        stateMap.remove(future);
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            String bodyPayload = "";
                            ResponseBody body = response.body();
                            if (body != null) {
                                try {
                                    bodyPayload = body.string();
                                } catch (IOException e) {
                                    // pass
                                }
                                body.close();
                            }

                            if (!response.isSuccessful()) {
                                LOG.error("request got response but was not successful");
                                scanRequestsFailed.inc();
                                stateMap.remove(future);
                                future.completeExceptionally(new Exception("Request failed with response code: "
                                        + response.code() + " and body: " + bodyPayload));
                            } else {
                                handleGatewayResponse(future, bodyPayload);
                                stateMap.remove(future);
                                future.complete(null);
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
        return future;
    }

    private void handleGatewayResponse(CompletableFuture<Void> future, String bodyPayload) {
        LOG.info("Request complete: "+bodyPayload);
        IState state = stateMap.get(future);

        if(state == null) {
            LOG.error("no future for you!");
            return;
        }

        Object o = state.parseResponse(bodyPayload);
        if(o == null) {
            LOG.error("failed to parse response");
            return;
        }
        switch(state.nextState()) {
            case STATE_CREATE_TARGET:
                LOG.info("moving to CreateTarget");
                CreateTarget target = new CreateTarget(
                        state.getNodeId(),
                        state.credentials(),
                        ((InitializeResponse)o).getScannerId(),
                        ((InitializeResponse)o).getConfigId(),
                        ((Initialize)state).getHost());
                doPost(url + target.endpoint(), target);
                break;
            case STATE_CREATE_TASK:
                LOG.info("moving to CreateTask");
                CreateTask task = new CreateTask(
                        state.getNodeId(),
                        state.credentials(),
                        ((CreateTarget)state).getScannerId(),
                        ((CreateTarget)state).getConfigId(),
                        ((CreateTarget)state).getHost(),
                        ((CreateTargetResponse)o).getTargetId());
                doPost(url + task.endpoint(), task);
                break;
            case STATE_START_TASK:
                LOG.info("moving to StartTask");
                StartTask stask = new StartTask(
                        state.getNodeId(),
                        state.credentials(),
                        ((CreateTaskResponse)o).getTaskId());
                doPost(url + stask.endpoint(), stask);
                break;
            case STATE_GET_REPORT:
                LOG.info("waiting for scan report ID "+
                        ((StartTaskResponse)o).getReportId());
                // since we have no way to set an Alert on the new GVM Task,
                // we can't tell it to invoke us when the task finishes. We'll
                // just send an event with the report URL for now.
                sendScanRequestedEvent(state.getNodeId(), ((StartTaskResponse)o).getReportId());
                break;
        }
    }

    public void sendScanRequestedEvent(int node, String reportId) {
        eventForwarder.sendAsync(ImmutableInMemoryEvent.newBuilder()
            .setUei(SCAN_REQUESTED_UEI)
            .setSource("ScanVAS")
            .setNodeId(node)
            .addParameter(ImmutableEventParameter.newBuilder()
                .setName("reporturl")
                .setValue(url.replaceAll(":\\d+/","/report/") + reportId)
                .build())
            .build());
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        // we don't care if the hostname matches the cert because we only
        // connect to a server with our specific certificate installed
        return true;
    }
    public MetricRegistry getMetrics() {
        return metrics;
    }
}
