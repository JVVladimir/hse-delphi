package ru.hse.delphi.entity;

public class User {

    private String id;
    private String mark;
    private String comment;

    public User(String id, String mark, String comment) {
        this.id = id;
        this.mark = mark;
        this.comment = comment;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", mark='" + mark + '\'' +
                ", comment='" + comment + '\'' +
                '}';
    }
}
