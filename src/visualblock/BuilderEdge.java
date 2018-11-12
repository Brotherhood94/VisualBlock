package visualblock;

import org.gephi.graph.api.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.PatternSyntaxException;


public class BuilderEdge extends Thread{
    private String regex;
    private CharSequence hashtag = "#", header = null;
    private GraphModel graphModel;
    private DirectedGraph gDGraph;
    private ArrayList<RecordEdge> edges;
    public BuilderEdge(ArrayList<RecordEdge> edges, GraphModel graphModel, DirectedGraph gDGraph, String regex) {
        this.header = "Source";
        this.regex = regex;
        this.edges = edges;
        this.graphModel = graphModel;
        this.gDGraph = gDGraph;
    }

    @Override
    public void run(){
        Edge edge;
        Node source, target;
        //regex = "[,\\s+]";
        Iterator<RecordEdge> it = edges.iterator();
        double counter = 1;
        while(it.hasNext()){
            RecordEdge record = it.next();
            if( ( source = gDGraph.getNode(record.getSource()) ) == null){
                gDGraph.addNode(source = graphModel.factory().newNode(record.getSource()));
                source.setSize(13.0f);
                source.addInterval(new Interval(counter, edges.size()));
            }
            if( (target = gDGraph.getNode(record.getTarget())) == null){
                gDGraph.addNode(target = graphModel.factory().newNode(record.getTarget()));
                target.setSize(13.0f);
                target.addInterval(new Interval(counter, edges.size()));
            }
            if( (edge = gDGraph.getEdge(source, target)) == null){
                gDGraph.addEdge(edge = graphModel.factory().newEdge(source, target, true));
                edge.addInterval(new Interval(counter, edges.size()));
            }
            if(record.getWeight() != -1)
                edge.setWeight(MainBlock.scaledWeight((record.getWeight())));
            counter++;
        }
        return;
    }

}
