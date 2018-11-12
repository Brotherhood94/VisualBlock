package visualblock;

public class RecordEdge {
    private String source;
    private String target;
    private double weight = -1;

    public RecordEdge(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }
}
