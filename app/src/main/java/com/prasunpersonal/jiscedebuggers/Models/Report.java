package com.prasunpersonal.jiscedebuggers.Models;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

public class Report {
    private String userId, reportId, pageName, errorDescription;
    private ArrayList<String> errorImages;
    private long time;

    public Report() {}

    public Report(String userId, String pageName, String errorDescription, ArrayList<String> errorImages) {
        this.userId = userId;
        this.pageName = pageName;
        this.errorDescription = errorDescription;
        this.errorImages = errorImages;
        this.time = System.currentTimeMillis();
        this.reportId = String.format(Locale.getDefault(), "%d_%s", this.time, this.userId);
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public ArrayList<String> getErrorImages() {
        return errorImages;
    }

    public void setErrorImages(ArrayList<String> errorImages) {
        this.errorImages = errorImages;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Report)) return false;
        Report report = (Report) o;
        return getReportId().equals(report.getReportId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getReportId());
    }
}
