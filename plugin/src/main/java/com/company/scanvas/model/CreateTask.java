package com.company.scanvas.model;

import com.company.scanvas.IState;
import com.company.scanvas.State;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.company.scanvas.State.STATE_START_TASK;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTask implements IState {
    @JsonProperty("user")
    private Credentials user;

    public class Params {
        @JsonProperty("config_id")
        private String configId;
        @JsonProperty("target_id")
        private String targetId;
        @JsonProperty("name")
        private String name;
        @JsonProperty("scanner_id")
        private String scannerId;

        public Params(String n, String config, String scanner, String target) {
            this.name = n;
            this.configId = config;
            this.scannerId = scanner;
            this.targetId = target;
        }
    }

    @JsonProperty("params")
    private Params params;
    @JsonIgnore
    private final int node;

    public CreateTask(int node, Credentials cred, String scanner, String config, String host, String target) {
        this.params = new Params("scan_"+host, config, scanner, target);
        this.user = cred;
        this.node = node;
    }

    public State nextState() { return STATE_START_TASK; }

    public String endpoint() { return "createtask"; }

    @Override
    public Object parseResponse(String body) {
        ObjectMapper mapper = new ObjectMapper();
        CreateTaskResponse ctr;
        try {
            ctr = mapper.readValue(body, CreateTaskResponse.class);
        } catch(Exception e) {
            return null;
        }
        return ctr;
    }

    @Override
    public Credentials credentials() {
        return user;
    }
    public int getNodeId() { return this.node; }

}
