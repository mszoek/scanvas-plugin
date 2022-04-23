package com.company.scanvas;

import com.company.scanvas.model.Credentials;

public interface IState {
    public State nextState();

    public String endpoint();
    public Object parseResponse(String body);
    public Credentials credentials();
    public int getNodeId();
}
