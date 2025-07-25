package com.notifyme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelConfigResponse {
    
    private String channelType;
    private String configuration;  // Mascherato per sicurezza
    private boolean configured;    // Se il canale è configurato
    private boolean editable;      // Se il canale può essere modificato
    
    public ChannelConfigResponse(String channelType, String configuration, boolean configured) {
        this.channelType = channelType;
        this.configuration = configuration;
        this.configured = configured;
        this.editable = true; // Default editabile
    }
}