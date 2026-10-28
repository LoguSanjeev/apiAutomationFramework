package com.qa.api.pojo;

/** PATCH sends only the field being changed. */
public class PostPatchRequest {
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
