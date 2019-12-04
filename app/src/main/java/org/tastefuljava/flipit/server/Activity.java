package org.tastefuljava.flipit.server;

import java.util.Date;

public class Activity {
    private int facetNumber;
    private Date startTime;
    private String comment;

    public int getFacetNumber() {
        return facetNumber;
    }

    public void setFacetNumber(int facetNumber) {
        this.facetNumber = facetNumber;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
