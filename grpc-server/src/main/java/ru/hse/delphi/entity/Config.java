package ru.hse.delphi.entity;

public class Config {

    private final String question;
    private final int numberOfExperts;

    public Config(String question, int numberOfExperts) {
        this.question = question;
        this.numberOfExperts = numberOfExperts;
    }

    public String getQuestion() {
        return question;
    }

    public int getNumberOfExperts() {
        return numberOfExperts;
    }

    @Override
    public String toString() {
        return "Config{" +
                "question='" + question + '\'' +
                ", numberOfExperts=" + numberOfExperts +
                '}';
    }
}
