package com.twitterapp.timelinemicroservice.extrastructures;

public class JwsObject
{
    private String jws;

    public JwsObject() {
        this.jws = "";
    }

    public JwsObject(final String jws) {
        this.jws = jws;
    }

    public String getJws() {
        return this.jws;
    }

    public void setJws(final String jws) {
        this.jws = jws;
    }
}