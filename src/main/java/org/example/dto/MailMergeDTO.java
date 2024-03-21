package org.example.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MailMergeDTO implements Serializable {
    @JsonProperty("texts")
    private Map<String, String> texts;

    @JsonProperty("tables")
    private Map<String, List<List<String>>> tables;

    @JsonProperty("images")
    private Map<String, String> images;

    @JsonProperty("charts")
    private Map<String, List<List<String>>> charts;

    public Map<String, String> getTexts() {
        return texts;
    }

    public Map<String, List<List<String>>> getTables() {
        return tables;
    }

    public Map<String, String> getImages() {
        return images;
    }

    public Map<String, List<List<String>>> getCharts() {
        return charts;
    }

    public void setTexts(Map<String, String> texts) {
        this.texts = texts;
    }

    public void setTables(Map<String, List<List<String>>> tables) {
        this.tables = tables;
    }

    public void setImages(Map<String, String> images) {
        this.images = images;
    }

    public void setCharts(Map<String, List<List<String>>> charts) {
        this.charts = charts;
    }
}
