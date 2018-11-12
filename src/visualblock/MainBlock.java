package visualblock;

import com.martiansoftware.jsap.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.gephi.graph.api.*;
import org.gephi.io.exporter.api.ExportController;
import org.gephi.preview.api.PreviewController;
import org.gephi.preview.api.PreviewModel;
import org.gephi.preview.api.PreviewProperty;
import org.gephi.preview.types.EdgeColor;
import org.gephi.project.api.*;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MainBlock {

    private static int range = 10;
    public static double maxValue;

    public static void main(String[] args) {
        ProjectController pc;
        Workspace workspace;
        GraphModel graphModel;
        DirectedGraph gDGraph;
        ExportController ec;
        PreviewModel previewModel;

        /*Setting Up*/
        pc = Lookup.getDefault().lookup(ProjectController.class);
        pc.newProject();
        workspace = pc.getCurrentWorkspace();
        graphModel = Lookup.getDefault().lookup(GraphController.class).getGraphModel(workspace);
        gDGraph = graphModel.getDirectedGraph();
        ec = Lookup.getDefault().lookup(ExportController.class);
        previewModel = Lookup.getDefault().lookup(PreviewController.class).getModel();

        /*********************************************/
        JSAPResult config = JSAPArgument(args);
        /*********************************************/

        if(!FilenameUtils.getExtension(config.getString("/path/node.csv")).endsWith("csv")){
            System.err.println("ERROR: "+config.getString("/path/node.csv")+" is not a .csv file!");
            System.exit(0);
        }
        boolean once = true;
        if(!config.getBoolean("asDir")){
            if(!FilenameUtils.getExtension(config.getString("/path/edge.csv")).endsWith("csv")){
                System.err.println("ERROR: "+config.getString("/path/edge.csv")+" is not a .csv file!");
                System.exit(0);
            }
            processing(once, config.getString("/path/edge.csv"), gDGraph, graphModel, config);
            finalizing(config, graphModel, previewModel);
            export(ec, config.getString("/path/name.xxx"), config.getString("format") );    //ce ne sono 2
        }else{
            String edgePath = null;
            Iterator<File> it = checkDirs(config);
            while(it.hasNext()){
                edgePath = it.next().getAbsolutePath();
                once = processing(once, edgePath, gDGraph, graphModel, config);
                finalizing(config, graphModel, previewModel);
                export(ec, config.getString("/path/name.xxx")+"/"+FilenameUtils.getBaseName(edgePath), config.getString("format") );
                gDGraph.clear();        //Se tolto si sovrappongono!
            }
        }
    }

    private static JSAPResult JSAPArgument(String args[]){
        SimpleJSAP jsap = null;
        try {
            jsap = new SimpleJSAP("java -jar VisualBlock", "Edit and Convert Csv Format Graph",
                    new Parameter[]{
                            new UnflaggedOption("/path/node.csv", JSAP.STRING_PARSER, JSAP.REQUIRED,"Node.csv file path."),
                            new UnflaggedOption("/path/edge.csv", JSAP.STRING_PARSER, JSAP.REQUIRED,"Edge.csv file path."),
                            new UnflaggedOption("/path/name.xxx", JSAP.STRING_PARSER, JSAP.REQUIRED,"Name.xxx file path."),
                            new UnflaggedOption("range", JSAP.INTEGER_PARSER,"10", JSAP.NOT_REQUIRED, JSAP.NOT_GREEDY,"Value to adjust edge thickness"),
                            new FlaggedOption("format", JSAP.STRING_PARSER, "pdf", JSAP.NOT_REQUIRED, 'f', JSAP.NO_LONGFLAG, "Set export format.\n"
                                    +"Supported Format:\n"
                                    + "\t.pdf\n"
                                    + "\t.csv\n"
                                    + "\t.png\n"
                                    + "\t.gexf\n"
                                    + "\t.gml\n"
                                    + "\t.graphml\n"),
                            new FlaggedOption("algorithm", JSAP.STRING_PARSER, "yh", JSAP.NOT_REQUIRED, 'a', JSAP.NO_LONGFLAG, "Set drawing algorithm.\n"
                                    + "\tyh -> YifanHuLayout\n"
                                    + "\tla -> LabelAdjustLayout\n"
                                    + "\too -> OpenOrdLayout (unstable)\n"
                                    + "\tfa -> ForceAtlasLayout\n"
                                    + "\tfr -> FruchtermanReingoldLayout\n"
                                    + "\tnl -> NoverlapLayout\n").setList(true).setListSeparator(';'),
                            //new FlaggedOption("separator", JSAP.CHARACTER_PARSER, ",", JSAP.NOT_REQUIRED, 'x', JSAP.NO_LONGFLAG, "Set separator over edge's pairs.\n"),
                            new FlaggedOption("time", JSAP.INTEGER_PARSER, "2000", JSAP.NOT_REQUIRED, 't', JSAP.NO_LONGFLAG, "Drawing algorithm duration time. <milliseconds>"),
                            new Switch("sourceCol", 's', JSAP.NO_LONGFLAG, "Node<x>'s OUT edges will be colored with the same node<x>'s color."),
                            new Switch("reverse",'r', JSAP.NO_LONGFLAG, "Node<x>'s IN edges will be colored with the complementary (rgb) node<x>'s color."),
                            new Switch("outcoming",'o', JSAP.NO_LONGFLAG, "Node<x>'s neighbors OUT will be colored with node<x> darker's color."),
                            new Switch("incoming",'i', JSAP.NO_LONGFLAG, "Node<x>'s neighbors IN will be colored with node<x> brighter's color."),
                            new Switch("asDir",'d', JSAP.NO_LONGFLAG, "Enable exporting multiple graph.format in specified Directory.\n/path/edge.csv = /path/edgeDirectory\n/path/name.xxx = /path/saveDirectory"),
                            new Switch("converge",'c', JSAP.NO_LONGFLAG, "Apply YiFan-Hu Layout until graph is not converged.\n Enabling this flag, '-a' and '-t' flags will be ignored."),
                    });
        } catch (JSAPException ex) {
            Exceptions.printStackTrace(ex);
        }
        JSAPResult config = jsap.parse(args);
        if ( jsap.messagePrinted() ) System.exit( 0 );
        return config;
    }

    private static void setLayout(JSAPResult config, GraphModel graphModel){
        if(config.getBoolean("converge")){
            System.out.println("Wait for YiFan-Hu convergence...");
            Layouts.RandomLayout(graphModel);
            Layouts.ConvergedYifanHuLayout(graphModel);
        }else{
            System.out.println("Applyng Drawing Algorithms!");
            Layouts.createLayout(graphModel, config.getStringArray("algorithm"), config.getInt("time"));
        }
    }

    private static void readFiles(Builder builder, ConcurrentLinkedQueue<String> clq, String path){
        RandomAccessFile raf;
        FileChannel inCh;
        MappedByteBuffer mbb;
        String inQueue = "";
        char h;
        try{
            raf = new RandomAccessFile(path, "r");
            inCh = raf.getChannel();
            mbb = inCh.map(FileChannel.MapMode.READ_ONLY,0,inCh.size());
            //mbb.load(); //Non sono sicuro di questa scelta
            builder.start();
            for(int i = 0; i < mbb.capacity(); i++){
                if((h = (char) mbb.get(i)) =='\n'  && !inQueue.isEmpty()){
                    clq.add(inQueue);
                    inQueue = "";
                }else
                    inQueue+=h;
            }
            if(!inQueue.trim().equals("")) //Messo perché se nell'ultima linea non c'era il carattere \n, non la processava ma rimaneva inQueue senza esse mandata a clq.add()
                clq.add(inQueue);
            builder.interrupt();
            builder.join();
            raf.close();
        }catch (InterruptedException e) {
            System.out.println("Current Thread interrupted.");
        }catch (FileNotFoundException e1) {
            System.out.println("File Not Found in /"+ path+".\n");
            System.exit(0);
        }catch (IOException e){
            System.out.println("I/O error while reading. Retry."+ e);
            System.exit(0);
        }
    }

    private static File export(ExportController ec,String savePath, String format){
        File pdf = null;
        try {
            ec.exportFile(pdf = new File(savePath+"."+format));
        } catch (Exception ex) {
            System.err.println("Something went wrong while exporting. --help to check out supported format.");
            Exceptions.printStackTrace(ex);
            System.exit(0);
        }
        System.out.println("Correctly exported: "+savePath+"."+format);
        return pdf;
    }

    private static void setProperties(PreviewModel previewModel, JSAPResult config){
        Font f = new Font("Verdana", Font.BOLD, 5);
        previewModel.getProperties().putValue(PreviewProperty.SHOW_NODE_LABELS, Boolean.TRUE);
        //previewModel.getProperties().putValue(PreviewProperty.EDGE_RESCALE_WEIGHT, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_CURVED, Boolean.FALSE);
        previewModel.getProperties().putValue(PreviewProperty.DIRECTED, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.EDGE_THICKNESS, 1.0f);
        previewModel.getProperties().putValue(PreviewProperty.ARROW_SIZE, 5.0f);
        if(config.getBoolean("sourceCol"))
            previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.SOURCE));
        else{
            previewModel.getProperties().putValue(PreviewProperty.EDGE_COLOR, new EdgeColor(EdgeColor.Mode.ORIGINAL));
        }
        //previewModel.getProperties().putValue(PreviewProperty.MARGIN, 100);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_FONT, f);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_SHOW_BOX, Boolean.TRUE);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_BOX_OPACITY , 50);
        previewModel.getProperties().putValue(PreviewProperty.NODE_LABEL_OUTLINE_SIZE, 3f);
    }

    private static Iterator<File> checkDirs(JSAPResult config){
        String[] ext = {"csv"};
        File dirEdges = null, saveDir = null;
        String path = null;
        try{
            path = config.getString("/path/edge.csv");
            dirEdges = new File(path);
            path = config.getString("/path/name.xxx");
            saveDir = new File(path);
            if(!dirEdges.isDirectory())
                throw new NullPointerException();
            if(!saveDir.isDirectory())
                throw new NullPointerException();
        }catch(NullPointerException e){
            System.err.println("ERROR: Not valid path "+path+". Be sure to be a DIRECTORY path.");
            System.exit(0);
        }
        return FileUtils.iterateFiles(dirEdges, ext, false);
    }

    private static void finalizing(JSAPResult config, GraphModel graphModel, PreviewModel previewModel){
        setLayout(config, graphModel);
        setProperties(previewModel, config);
        System.out.println("Exporting...");
    }

    private static boolean processing(boolean once, String edgePath, DirectedGraph gDGraph, GraphModel graphModel, JSAPResult config){
        ConcurrentLinkedQueue<String> clq;
        System.out.println("Started! Processing: "+edgePath);
        String nodeRegex = getRegex(config.getString("/path/node.csv"));
        String edgeRegex = getRegex(edgePath);
        ArrayList<RecordEdge> edges = null;
        readFiles(new BuilderWeight( (clq = new ConcurrentLinkedQueue<String>()), edgeRegex, edges = new ArrayList<>()), clq, edgePath );
        System.out.println("Reading Edges...");
        new BuilderEdge(edges, graphModel, gDGraph, edgeRegex).run();
        if(once){           //da cambiare
            System.out.println("Reading Nodes..."); //devo leggerlo solo una volta altrimenti mi cambia sempre il colore
            readFiles(new BuilderNode( (clq = new ConcurrentLinkedQueue<String>()), graphModel, gDGraph, config, nodeRegex), clq, config.getString("/path/node.csv"));
            once = false;
        }
        return once;
    }

    public static double scaledWeight(Double weight){
        double res = (Math.round(weight/(maxValue/range)));
        if(res < 1)
            return 1.0f;
        else
            return res;
    }

    private static String getRegex(String path){
        String firstLine = null;
        String regex = "";
        try {
            firstLine = Files.lines(Paths.get(path)).findFirst().get();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        char[] chars = firstLine.toCharArray();
        int i = 0;
        while(Character.isDigit(chars[i]) || Character.isAlphabetic(chars[i]))
            i++;
        while(!Character.isDigit(chars[i]) && !Character.isAlphabetic(chars[i])) {
            regex += chars[i];
            i++;
        }
        if(regex.isEmpty()) {
            System.err.println("Delimiter has not been found. (Letters/Numbers are NOT allowed as separator)");
            System.exit(1);
        }
        System.err.println("The delimiter found in '"+path+"' is: '"+regex+"'. Check if it is correct with yours specifications.");
        return regex;
    }

    //ATTENZIONE: la getRegex, prende il primo carattere che non è nè lettera nè numero, ciò significa che se ho es: 222 - 333, allora mi prende come separatore " ", ovvero
    //il carattere vuoto e chiaramente non è corretto. Però posso fare la somma dei character in fila che non sono nè alphabetic ne numeri. Cioè "Prendi come delimiter tutto ciò che non è numero o lettera tra i due numeri"

    //Libreria Csv Common Apache??

}
