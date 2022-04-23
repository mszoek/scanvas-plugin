package com.company.scanvas.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Credentials {
    @JsonProperty("username")
    private final String username;

    @JsonProperty("password")
    private final String password;

    public Credentials(String user, String pass) {
        this.username = user;
        this.password = pass;
    }

    @Override
    public String toString() {
        return "Credentials{" +
                "username='" + username + '\'' +
                ", password='" + "XXXXXX" + '\'' +
                '}';
    }
}
