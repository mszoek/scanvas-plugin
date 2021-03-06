package com.company.scanvas.model;

import com.company.scanvas.IState;
import com.company.scanvas.State;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.company.scanvas.State.STATE_CREATE_TARGET;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Initialize implements IState {
    @JsonProperty("username")
    private final String username;

    @JsonProperty("password")
    private final String password;
    @JsonIgnore
    private final int node;
    @JsonIgnore
    private final String host;

    public Initialize(int node, String user, String pass, String host) {
        this.node = node;
        this.username = user;
        this.password = pass;
        this.host = host;
    }

    public State nextState() { return STATE_CREATE_TARGET; }

    public String endpoint() { return "initialize"; }

    @Override
    public Object parseResponse(String body) {
        ObjectMapper mapper = new ObjectMapper();
        InitializeResponse ir;
        try {
            ir = mapper.readValue(body, InitializeResponse.class);
        } catch(Exception e) {
            return null;
        }
        return ir;
    }

    @Override
    public Credentials credentials() {
        return new Credentials(username, password);
    }
    public String getHost() { return this.host; }
    public int getNodeId() { return this.node; }
}
