package com.galeos.smartalert;

public class Incidents {
    String emergency;
    String location;
    String timestamp;
    String comments;

    public Incidents(String emergency, String location, String timestamp) {
        this.emergency = emergency;
        this.location = location;
        this.timestamp = timestamp;
    }

    public Incidents(String emergency, String location, String timestamp, String comments) {
        this.emergency = emergency;
        this.location = location;
        this.timestamp = timestamp;
        this.comments = comments;
    }

    public String getEmergency() {
        return emergency;
    }

    public void setEmergency(String emergency) {
        this.emergency = emergency;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
