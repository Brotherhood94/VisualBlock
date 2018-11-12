package visualblock;

import org.gephi.graph.api.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class Builder extends Thread {
    protected GraphModel graphModel;
    protected DirectedGraph gDGraph;
    protected ConcurrentLinkedQueue<String> clq;

    protected CharSequence hashtag = "#", header = null;

    public Builder(ConcurrentLinkedQueue<String> clq){
        this.clq = clq;
    }

    public Builder(ConcurrentLinkedQueue<String> clq, GraphModel graphModel, DirectedGraph gDGraph){
        this.clq = clq;
        this.graphModel = graphModel;
        this.gDGraph = gDGraph;
    }

    @Override
    public abstract void run();
}
