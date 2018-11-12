package visualblock;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.PatternSyntaxException;

public class BuilderWeight extends Builder{
    private ArrayList<RecordEdge> edges;
    private String regex;
    public BuilderWeight(ConcurrentLinkedQueue<String> clq, String regex, ArrayList<RecordEdge> edges) {
        super(clq);
        this.header = "Source";
        this.regex = regex;
        this.edges = edges;
    }

    @Override
    public void run() {
        String[] splitted = null;
        String read = null;
        double max = 1, act;
        RecordEdge record = null;
        while(true){
            if(clq.size()>0 && !(read = clq.remove().trim()).contains(hashtag) && !read.contains(header) ){
                try {
                    splitted = read.split(regex);
                }catch (PatternSyntaxException ex){     //preso un carattere separatore brutto
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
                if(splitted.length >= 2)
                    edges.add(record = new RecordEdge(splitted[0], splitted[1]));
                if(splitted.length == 3 && (act = Double.parseDouble(splitted[2])) > max) {
                    record.setWeight(act);
                    max = act;
                }
            }else if(clq.isEmpty() && Thread.currentThread().isInterrupted()){
                //System.out.println("Read Weight. Max Value: "+max);
                visualblock.MainBlock.maxValue = max;
                return;
            }
        }
    }

}
