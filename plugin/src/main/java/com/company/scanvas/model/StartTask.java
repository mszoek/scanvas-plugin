package com.company.scanvas.model;

import com.company.scanvas.IState;
import com.company.scanvas.State;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.company.scanvas.State.STATE_GET_REPORT;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StartTask implements IState {
    @JsonProperty("user")
    private Credentials user;

    public class Params {
        @JsonProperty("task_id")
        private String taskId;

        public Params(String task) {
            this.taskId = task;
        }
    }

    @JsonProperty("params")
    private Params params;
    @JsonIgnore
    private final int node;

    public StartTask(int node, Credentials cred, String task) {
        this.params = new Params(task);
        this.user = cred;
        this.node = node;
    }

    public State nextState() { return STATE_GET_REPORT; }

    public String endpoint() { return "starttask"; }

    @Override
    public Object parseResponse(String body) {
        ObjectMapper mapper = new ObjectMapper();
        StartTaskResponse str;
        try {
            str = mapper.readValue(body, StartTaskResponse.class);
        } catch(Exception e) {
            return null;
        }
        return str;
    }

    @Override
    public Credentials credentials() {
        return user;
    }
    public int getNodeId() { return this.node; }

}
