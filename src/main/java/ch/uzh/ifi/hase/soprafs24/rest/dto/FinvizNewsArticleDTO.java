package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class FinvizNewsArticleDTO {

    private String title;
    private String source;
    private String date; // Keeping as String for simplicity, parsing to LocalDateTime is possible
    private String url;
    private String category;

    // --- Constructors ---
    public FinvizNewsArticleDTO() {
    }

    public FinvizNewsArticleDTO(String title, String source, String date, String url, String category) {
        this.title = title;
        this.source = source;
        this.date = date;
        this.url = url;
        this.category = category;
    }

    // --- Getters and Setters ---
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    @Override
    public String toString() {
        return "FinvizNewsArticleDTO{" +
               "title='" + title + '\'' +
               ", source='" + source + '\'' +
               ", date='" + date + '\'' +
               ", url='" + url + '\'' +
               ", category='" + category + '\'' +
               '}';
    }
}