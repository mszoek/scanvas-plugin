package com.company.scanvas;

import com.company.scanvas.model.Alert;
import com.company.scanvas.model.Credentials;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class ApiClient implements HostnameVerifier {

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String url;
    private static final Logger LOG = LoggerFactory.getLogger(ApiClient.class);
    private final String vasUsername;
    private final String vasPassword;

    public ApiClient(String url, String keystore, String password, String vasuser, String vaspass) {
        this.url = Objects.requireNonNull(url);
        this.vasUsername = vasuser;
        this.vasPassword = vaspass;

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

    public CompletableFuture<Void> sendAlert(Alert alert) {
        return doPost(url, alert);
    }

    public CompletableFuture<Void> testPost() {
        String initurl = url + "initialize";
        Credentials creds = new Credentials(vasUsername, vasPassword);
        System.out.println("YAY, testing "+initurl+" with body "+creds);
        return doPost(initurl, new Credentials());
    }

    private CompletableFuture<Void> doPost(String url, Object requestBodyPayload) {
        RequestBody body;
        try {
            body = RequestBody.create(JSON, mapper.writeValueAsString(requestBodyPayload));
            LOG.error("request body="+mapper.writeValueAsString(requestBodyPayload));
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
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        LOG.error("request failed: "+e);
                        future.completeExceptionally(e);
                    }

                    @Override
                    public void onResponse(Call call, Response response) {
                        try {
                            if (!response.isSuccessful()) {
                                LOG.error("request got response but was not successful");
                                String bodyPayload = "(empty)";
                                ResponseBody body = response.body();
                                if (body != null) {
                                    try {
                                        bodyPayload = body.string();
                                    } catch (IOException e) {
                                        // pass
                                    }
                                    body.close();
                                }

                                future.completeExceptionally(new Exception("Request failed with response code: "
                                        + response.code() + " and body: " + bodyPayload));
                            } else {
                                LOG.error("request completed");
                                future.complete(null);
                            }
                        } finally {
                            response.close();
                        }
                    }
                });
        return future;
    }

    @Override
    public boolean verify(String hostname, SSLSession session) {
        // we don't care if the hostname matches the cert because we only
        // connect to a server with our specific certificate installed
        return true;
    }
}
