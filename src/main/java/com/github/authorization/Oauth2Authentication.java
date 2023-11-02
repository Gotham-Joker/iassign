package com.github.authorization;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Set;

public class Oauth2Authentication implements Authentication {
    public String id;
    public String clientId;
    public String expireIn;

    public Oauth2Authentication() {

    }

    public Oauth2Authentication(String id, String clientId) {
        this.id = id;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getCredentials() {
        return null;
    }

    @JsonIgnore
    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public Set<String> getPermissions() {
        return null;
    }

    @Override
    public UserDetails getDetails() {
        return null;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
