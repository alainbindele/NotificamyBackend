package com.notifyme.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChannelConfigRequest {
    
    @NotBlank(message = "Channel configuration cannot be empty")
    @Size(max = 500, message = "Channel configuration cannot exceed 500 characters")
    private String configuration;
    
    @Size(max = 100, message = "Description cannot exceed 100 characters")
    private String description;
}