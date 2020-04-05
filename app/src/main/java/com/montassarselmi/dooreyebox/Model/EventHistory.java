package com.montassarselmi.dooreyebox.Model;

import androidx.annotation.Nullable;

public class EventHistory {

    private int id;
    private String eventTime;
    private int icon;
    private String status;
    private String responder;
    private String visitorImage;

    public EventHistory(int id, String eventTime, int icon, String status, @Nullable String responder, @Nullable String visitorImage) {
        this.id = id;
        this.eventTime = eventTime;
        this.icon = icon;
        this.status = status;
        if (responder != null)
            this.responder = responder;
        if (visitorImage != null)
            this.visitorImage = visitorImage;
    }

    public EventHistory() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getStatus() {
        return status;
    }

    public String getResponder() {
        return responder;
    }

    public void setResponder(@Nullable String responder) {
        if (responder != null)
        this.responder = responder;
    }

    public String getVisitorImage() {
        return visitorImage;
    }

    public void setVisitorImage(String visitorImage) {
        if (visitorImage != null)
            this.visitorImage = visitorImage;
    }
}
