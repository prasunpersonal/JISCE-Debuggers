package com.prasunpersonal.jiscedebuggers.Models;

import static com.prasunpersonal.jiscedebuggers.App.ME;

import org.parceler.Parcel;

import java.util.ArrayList;
import java.util.Objects;

@Parcel
public class FAQ {
    private String questionId, questionTitle, questionAuthor;
    private long questionTime;
    private ArrayList<String> answers;

    public FAQ() {}

    public FAQ(String questionTitle) {
        this.questionTitle = questionTitle;
        this.questionAuthor = ME.getUserID();
        this.questionTime = System.currentTimeMillis();
        this.questionId = this.questionTime+"_"+this.questionAuthor;
        this.answers = new ArrayList<>();
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionAuthor() {
        return questionAuthor;
    }

    public void setQuestionAuthor(String questionAuthor) {
        this.questionAuthor = questionAuthor;
    }

    public long getQuestionTime() {
        return questionTime;
    }

    public void setQuestionTime(long questionTime) {
        this.questionTime = questionTime;
    }

    public ArrayList<String> getAnswers() {
        return answers;
    }

    public void setAnswers(ArrayList<String> answers) {
        this.answers = answers;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FAQ)) return false;
        FAQ faq = (FAQ) o;
        return getQuestionId().equals(faq.getQuestionId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getQuestionId());
    }
}
