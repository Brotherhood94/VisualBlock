package visualblock;

import com.martiansoftware.jsap.JSAPResult;
import org.gephi.graph.api.*;
import org.openide.util.Exceptions;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.PatternSyntaxException;

public class BuilderNode extends Builder{
    private JSAPResult config;
    private String regex;
    public BuilderNode(ConcurrentLinkedQueue<String> clq, GraphModel graphModel, DirectedGraph gDGraph, JSAPResult config, String regex) {
        super(clq, graphModel, gDGraph);
        this.header = "Id";
        this.config = config;
        this.regex = regex;
    }

    @Override
    public void run() {
        Node node = null;
        String[] splitted = null;
        String read = null;
        Color color = null;
        Random random = new Random();
        while(true){
            if(clq.size()>0 && !(read = clq.remove().trim()).contains(hashtag) && !read.contains(header) ){
                try {
                    splitted = read.split(regex);
                }catch (PatternSyntaxException ex){     //preso un carattere separatore brutto
                    System.err.println(ex.getMessage());
                    System.exit(1);
                }
                if( (node = gDGraph.getNode(splitted[0])) != null){        //Aggiungo il nodo solo se già presente, cioè è già stato creando aggiungendo gli archi. Così evito la presenza di nodi isoltati
                    if(splitted.length>1)  //Controllo che abbia la colonna Label
                        node.setLabel(splitted[1]);
                    if(splitted.length>2){ //Controllo che abbia la colonna Group
                        Field field = null;
                        try {
                            //System.out.println("Node "+node.getId());
                            field = Class.forName("java.awt.Color").getField(splitted[2].toLowerCase());
                            color = (Color)field.get(null);
                            node.setColor(color);
                        } catch (Exception ex) {
                            Exceptions.printStackTrace(ex);
                        }
                    }else{ //nel caso in cui non sia specificato il colore
                        color = new Color(random.nextInt(255),random.nextInt(255),random.nextInt(255));
                        node.setColor(color);
                        System.err.println("Warning! Node<"+node.getId()+">'s group color not found. Auto-Set: R<"+color.getRed()+"> G<"+color.getGreen()+"> B<"+color.getBlue()+">");
                    }
                    if(config.getBoolean("reverse")){
                        Iterator<Edge> ei = gDGraph.getInEdges(node).iterator();
                        while(ei.hasNext()){
                            Edge e = ei.next();
                            e.setColor(new Color(255-color.getRed(),255-color.getGreen(),255-color.getBlue()));
                        }
                    }
                    if(config.getBoolean("outcoming")){
                        Iterator<Node> ni = gDGraph.getSuccessors(node).iterator();
                        while(ni.hasNext()){
                            Node n = ni.next();
                            if(n.getColor().equals(Color.BLACK)) //Se il colore è nero del nodo allora è uno di quelli NON colorati perciò lo coloro
                                n.setColor(color.darker());
                        }
                    }
                    if(config.getBoolean("incoming")){
                        Iterator<Node> ni = gDGraph.getPredecessors(node).iterator();
                        while(ni.hasNext()){
                            Node n = ni.next();
                            if(n.getColor().equals(Color.BLACK)) //Se il colore è nero del nodo allora è uno di quelli NON colorati perciò lo coloro
                                n.setColor(color.brighter());
                        }
                    }
                }
            }else if(clq.isEmpty() && Thread.currentThread().isInterrupted()){
                //System.out.println("Read Nodes");
                return;
            }
        }
    }
}
