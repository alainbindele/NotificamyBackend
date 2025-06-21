package com.notifyme.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "keychain")
public class Keychain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "apikey", nullable = false, length = 200)
    private String apikey;

    @Column(name = "alias", length = 100)
    private String alias;

    @Column(name = "expired", nullable = false, columnDefinition = "TINYINT")
    private Boolean expired = false;

    @Column(name = "disabled", columnDefinition = "TINYINT")
    private Boolean disabled = false;

    public Keychain() {}

    public Keychain(String apikey, String alias) {
        this.apikey = apikey;
        this.alias = alias;
        this.expired = false;
        this.disabled = false;
    }

    // Getters and setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public Boolean getExpired() {
        return expired;
    }

    public void setExpired(Boolean expired) {
        this.expired = expired;
    }

    public Boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(Boolean disabled) {
        this.disabled = disabled;
    }

    public boolean isValid() {
        return !expired && !disabled;
    }
}