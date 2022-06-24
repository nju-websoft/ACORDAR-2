package beans;

public class ResultBean {
    public int id;
    public int dataset;
    public int component;
    public String keyword;
    public String snippet;
    public long runningTime;

    public ResultBean(int id, int dataset, String keyword, String snippet, long runningTime) { // KSD
        this.id = id;
        this.dataset = dataset;
        this.keyword = keyword;
        this.snippet = snippet;
        this.runningTime = runningTime;
    }

    public ResultBean(int dataset, String snippet, long runningTime) { // IlluSnip
        this.dataset = dataset;
        this.snippet = snippet;
        this.runningTime = runningTime;
    }

    public ResultBean(int id, int dataset, int component, String keyword, String snippet, long runningTime) { // PrunedDP
        this.id = id;
        this.dataset = dataset;
        this.component = component;
        this.keyword = keyword;
        this.snippet = snippet;
        this.runningTime = runningTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDataset() {
        return dataset;
    }

    public void setDataset(int dataset) {
        this.dataset = dataset;
    }

    public int getComponent() {
        return component;
    }

    public void setComponent(int component) {
        this.component = component;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public long getRunningTime() {
        return runningTime;
    }

    public void setRunningTime(long runningTime) {
        this.runningTime = runningTime;
    }
}
