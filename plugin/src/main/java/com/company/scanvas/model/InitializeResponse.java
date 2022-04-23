package com.company.scanvas.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility=JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InitializeResponse {
    @JsonProperty("scanner_id")
    private String scannerId;

    @JsonProperty("config_id")
    private String configId;

    public String getConfigId() { return configId; }
    public String getScannerId() { return scannerId; }
}
