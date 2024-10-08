package com.checkmk.pdctLifeCycle.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;

@Entity
@JsonDeserialize(using = HostDeserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Table(name = "host")
public class Host {

    @Id
    private String id;
    private String hostName;
    private String ipAddress;
    private String creationDate;
    private String expirationDate;
    @ManyToOne
    @JoinColumn(name="notification_id")
    private HostNotification hostNotification;
    private String hostUser;
    private String hostUserEmail;
    @Transient
    private boolean imported;

    // Constructors

    public Host() {}

    public Host(String hostName, String ipAddress) {
        this.hostName = hostName;
        this.ipAddress = ipAddress;
    }

    public Host(String id, String hostName, String ipAddress, String creationDate) {
        this.id = id;
        this.hostName = hostName;
        this.ipAddress = ipAddress;
        this.creationDate = creationDate;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(String expirationDate) {
        this.expirationDate = expirationDate;
    }

    public HostNotification getHostNotification() {
        return hostNotification;
    }

    public void setHostNotification(HostNotification hostNotification) {
        this.hostNotification = hostNotification;
    }


    public boolean isImported() {
        return imported;
    }

    public void setImported(boolean imported) {
        this.imported = imported;
    }

    public String getHostUser() {
        return hostUser;
    }

    public void setHostUser(String hostUser) {
        this.hostUser = hostUser;
    }

    public String getHostUserEmail() {
        return hostUserEmail;
    }

    public void setHostUserEmail(String hostUserEmail) {
        this.hostUserEmail = hostUserEmail;
    }
}
