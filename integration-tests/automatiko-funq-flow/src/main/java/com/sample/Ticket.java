package com.sample;

public class Ticket {

    private long id;

    private String type;

    private String owner;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    @Override
    public String toString() {
        return "Ticket [id=" + id + ", type=" + type + ", owner=" + owner + "]";
    }

}
