package org.tastefuljava.flipit.domain;

public class Activity {
    private Integer facetNumber;
    private String startTime;
    private String comment;

    public Integer getFacetNumber() {
        return facetNumber;
    }

    public void setFacetNumber(Integer facetNumber) {
        this.facetNumber = facetNumber;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
