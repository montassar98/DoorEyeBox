package com.montassarselmi.dooreyebox.Model;

import androidx.annotation.Nullable;

import java.util.Date;

public class EventHistory {

    private int id;
    private Date eventTime;
    private String status;
    private String responder;
    private String visitorImage;

    public EventHistory(int id, Date eventTime, String status, @Nullable String responder, @Nullable String visitorImage) {
        this.id = id;
        this.eventTime = eventTime;
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

    public Date getEventTime() {
        return eventTime;
    }

    public void setEventTime(Date eventTime) {
        this.eventTime = eventTime;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
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

    @Override
    public String toString() {
        return "EventHistory{" +
                "id=" + id +
                ", eventTime=" + eventTime +
                ", status='" + status + '\'' +
                ", responder='" + responder + '\'' +
                ", visitorImage='" + visitorImage + '\'' +
                '}';
    }
}
