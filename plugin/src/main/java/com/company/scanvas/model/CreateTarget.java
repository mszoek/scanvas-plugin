package com.company.scanvas.model;

import com.company.scanvas.IState;
import com.company.scanvas.State;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.company.scanvas.State.*;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTarget implements IState {
    @JsonProperty("user")
    private Credentials user;

    public class Params {
        @JsonProperty("name")
        private String name;

        @JsonProperty("hosts")
        private String hosts;

        public Params(String n, String h) {
            this.name = n;
            this.hosts = h;
        }
    }

    @JsonProperty("params")
    private Params params;

    @JsonIgnore
    private String scannerId;
    @JsonIgnore
    private String configId;

    public CreateTarget(Credentials cred, String scanner, String config, String host) {
        this.params = new Params(host, host);
        this.user = cred;
        this.scannerId = scanner;
        this.configId = config;
    }

    public State nextState() { return STATE_CREATE_TASK; }

    public String endpoint() { return "createtarget"; }

    @Override
    public Object parseResponse(String body) {
        ObjectMapper mapper = new ObjectMapper();
        CreateTargetResponse ctr;
        try {
            ctr = mapper.readValue(body, CreateTargetResponse.class);
        } catch(Exception e) {
            return null;
        }
        return ctr;
    }

    @Override
    public Credentials credentials() {
        return user;
    }

    public String getConfigId() { return configId; }
    public String getScannerId() { return scannerId; }
}
