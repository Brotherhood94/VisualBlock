package visualblock;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.AutoLayout;
import org.gephi.layout.plugin.force.StepDisplacement;
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout;
import org.gephi.layout.plugin.forceAtlas.ForceAtlasLayout;
import org.gephi.layout.plugin.fruchterman.FruchtermanReingold;
import org.gephi.layout.plugin.labelAdjust.LabelAdjust;
import org.gephi.layout.plugin.noverlap.NoverlapLayout;
import org.gephi.layout.plugin.openord.OpenOrdLayout;
import org.gephi.layout.plugin.random.RandomLayout;

import java.util.concurrent.TimeUnit;

public class Layouts {

    private static GraphModel graphModel = null;
    public static void createLayout(GraphModel graphModel, String[] algorithm, Integer seconds){
        Layouts.graphModel = graphModel;
        AutoLayout autoLayout = new AutoLayout(seconds, TimeUnit.MILLISECONDS);
        autoLayout.setGraphModel(graphModel);
        float ratio = 1.0f/((float)algorithm.length);
        //System.out.println(algorithm.length+" "+ratio+" "+seconds);
        if(seconds <= 0 || !(seconds instanceof Integer)){
            System.out.println("-t parameter must be an integer > 0");
            System.exit(0);
        }
        RandomLayout(graphModel);
        for(String a : algorithm){
            switch(a){
                case "yh": autoLayout.addLayout(YifanHuLayout(),ratio); break;
                case "la": autoLayout.addLayout(LabelAdjustLayout(),ratio); break;
                case "oo": autoLayout.addLayout(OpenOrdLayout(),ratio); break;
                case "fa": autoLayout.addLayout(ForceAtlasLayout(),ratio); break;
                case "fr": autoLayout.addLayout(FruchtermanReingoldLayout(),ratio); break;
                case "nl": autoLayout.addLayout(NoverlapLayout(),ratio); break;
                default: System.out.println("Specified algorithm not Found. --help to show available algorithm"); break;
            }
        }
        autoLayout.execute();
    }

    public static void RandomLayout(GraphModel graphModel){
        RandomLayout rl = new RandomLayout(null, 500.0D);
        rl.setGraphModel(graphModel);
        rl.initAlgo();
        if(rl.canAlgo())
            rl.goAlgo();
        rl.endAlgo();
    }

    private static YifanHuLayout YifanHuLayout(){
        YifanHuLayout yhl = new YifanHuLayout(null , new StepDisplacement(0f) );
        yhl.resetPropertiesValues();
        yhl.setOptimalDistance(100f);
        yhl.setRelativeStrength(0.2F);
        yhl.setInitialStep(20.0f);
        yhl.setStepRatio(0.95f);
        yhl.setAdaptiveCooling(Boolean.TRUE);
        yhl.setConvergenceThreshold(0.001f);
        yhl.setQuadTreeMaxLevel(10);
        yhl.setBarnesHutTheta(1.2f);
        return yhl;
    }

    public static void ConvergedYifanHuLayout(GraphModel graphModel){
        YifanHuLayout yhl = new YifanHuLayout(null , new StepDisplacement(0f) );
        yhl.setGraphModel(graphModel);
        yhl.initAlgo();
        yhl.resetPropertiesValues();
        yhl.setOptimalDistance(100f);
        yhl.setRelativeStrength(0.2F);
        yhl.setInitialStep(20.0f);
        yhl.setStepRatio(0.95f);
        yhl.setAdaptiveCooling(Boolean.TRUE);
        yhl.setConvergenceThreshold(0.001f);
        yhl.setQuadTreeMaxLevel(10);
        yhl.setBarnesHutTheta(1.2f);
        if(yhl.canAlgo())
            while(!yhl.isConverged())
                yhl.goAlgo();
        yhl.endAlgo();
    }


    private static LabelAdjust LabelAdjustLayout(){
        LabelAdjust la = new LabelAdjust(null);
        la.resetPropertiesValues();
        la.setSpeed(2.0);
        la.setAdjustBySize(Boolean.TRUE);
        return la;
    }

    private static OpenOrdLayout OpenOrdLayout(){
        OpenOrdLayout ol = new OpenOrdLayout(null);
        ol.resetPropertiesValues();
        ol.setLiquidStage(25);
        ol.setExpansionStage(25);
        ol.setCooldownStage(25);
        ol.setCrunchStage(10);
        ol.setSimmerStage(15);
        ol.setEdgeCut(0.8f);
        ol.setNumThreads(7);
        ol.setNumIterations(750);
        ol.setRealTime(0.2f);
        ol.setRandSeed(6029429179641710226L);
        return ol;
    }


    private static ForceAtlasLayout ForceAtlasLayout(){
        ForceAtlasLayout fa = new ForceAtlasLayout(null);
        fa.resetPropertiesValues();
        fa.setInertia(0.1);
        fa.setRepulsionStrength(200.00);
        fa.setAttractionStrength(10.0);
        fa.setMaxDisplacement(10.0);
        fa.setAdjustSizes(Boolean.TRUE);
        fa.setFreezeStrength(80d);
        fa.setGravity(30.0);
        fa.setSpeed(1.0);
        return fa;
    }

    private static FruchtermanReingold FruchtermanReingoldLayout(){
        FruchtermanReingold fr = new FruchtermanReingold(null);
        fr.resetPropertiesValues();
        fr.setArea(10000.0f);
        fr.setGravity(10.0);
        fr.setSpeed(1.0);
        return fr;
    }

    private static NoverlapLayout NoverlapLayout(){
        NoverlapLayout nl = new NoverlapLayout(null);
        nl.resetPropertiesValues();
        nl.setSpeed(3.0);
        nl.setRatio(1.2);
        nl.setMargin(5.0);
        return nl;
    }

}
