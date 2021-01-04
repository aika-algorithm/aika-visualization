package network.aika.visualization;

import network.aika.EventListener;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Fired;
import network.aika.neuron.activation.Link;
import network.aika.neuron.excitatory.PatternNeuron;
import network.aika.neuron.excitatory.PatternPartNeuron;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import network.aika.neuron.phase.Phase;
import network.aika.neuron.phase.activation.ActivationPhase;
import network.aika.text.Document;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.stream.thread.ThreadProxyPipe;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.swing.SwingGraphRenderer;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.camera.Camera;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static network.aika.neuron.activation.Fired.NOT_FIRED;

public class ActivationViewerManager implements EventListener, ViewerListener {

    private Document doc;

    private Graph graph;
    private SwingViewer viewer;

    private ViewerPipe fromViewer;

    private ViewPanel graphView;

    private JSplitPane splitPane;
    private JTextPane consoleTextPane;

    private boolean clicked;

    private Node lastActEventNode;

    Map<String, Activation> nodeIdToActivation = new TreeMap<>();
    public Map<Integer, ActivationParticle> actIdToParticle = new TreeMap<>();


//    private Map<ActivationPhase, Consumer<Node>> actPhaseModifiers = new TreeMap<>(Comparator.comparing(p -> p.getRank()));
//    private Map<LinkPhase, Consumer<Edge>> linkPhaseModifiers = new TreeMap<>(Comparator.comparing(p -> p.getRank()));
    private Map<Class<? extends Neuron>, Consumer<Node>> neuronTypeModifiers = new HashMap<>();
    private Map<Class<? extends Synapse>, BiConsumer<Edge, Synapse>> synapseTypeModifiers = new HashMap<>();


    public ActivationViewerManager(Document doc) {
        this.doc = doc;

        initModifiers();
        doc.addEventListener(this);
/*
        viewer = new SwingViewer(graph, SwingViewer.ThreadingModel.GRAPH_IN_GUI_THREAD);

        add((DefaultView)viewer.addDefaultView(false, new SwingGraphRenderer()), BorderLayout.CENTER);
        viewer = new Viewer(graph,Viewer.ThreadingModel.GRAPH_IN_ANOTHER_THREAD);
*/

        graph = initGraph();

        //viewer = display.display(graph, false);

        viewer = new SwingViewer(new ThreadProxyPipe(graph));

        viewer.enableAutoLayout(new AikaLayout(this, graph));

        //view = (ViewPanel) viewer.getDefaultView();
 //       view = (DefaultView)viewer.addDefaultView(false, new AikaRenderer());
        graphView = (DefaultView)viewer.addDefaultView(false, new SwingGraphRenderer());
        graphView.enableMouseOptions();

        AikaMouseManager mouseManager = new AikaMouseManager(this);
        graphView.setMouseManager(mouseManager);
        graphView.addMouseWheelListener(mouseManager);

        Camera camera = graphView.getCamera();
        camera.setAutoFitView(false);

        // The default action when closing the view is to quit
        // the program.
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

        // We connect back the viewer to the graph,
        // the graph becomes a sink for the viewer.
        // We also install us as a viewer listener to
        // intercept the graphic events.
        fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(this);
        fromViewer.addSink(graph);

        splitPane = initSplitPane();
    }

    private JSplitPane initSplitPane() {

        //Create a text pane.
        consoleTextPane = new JTextPane();
        addStylesToDocument(consoleTextPane.getStyledDocument());

        JScrollPane paneScrollPane = new JScrollPane(consoleTextPane);
        paneScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        paneScrollPane.setPreferredSize(new Dimension(250, 155));
        paneScrollPane.setMinimumSize(new Dimension(10, 10));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, graphView, paneScrollPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.7);

        return splitPane;
    }


    protected void addStylesToDocument(StyledDocument doc) {
        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontFamily(def, "SansSerif");
        StyleConstants.setFontSize(regular, 20);

        Style s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);
     //   StyleConstants.setFontSize(s, 16);

        s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);
  //     StyleConstants.setFontSize(s, 16);

        s = doc.addStyle("small", regular);
        StyleConstants.setFontSize(s, 14);

        s = doc.addStyle("headline", regular);
        StyleConstants.setFontSize(s, 24);
    }

    public void showElementContext(String headlinePrefix, GraphicElement ge) {
        if(ge instanceof Node) {
            Node n = (Node) ge;

            Activation act = nodeIdToActivation.get(n.getId());
            if(act == null)
                return;

            renderConsoleOutput(headlinePrefix, act);
        }
    }

    private void renderConsoleOutput(String headlinePrefix, Activation act) {
        StyledDocument sDoc = consoleTextPane.getStyledDocument();
        try {
            sDoc.remove(0, sDoc.getLength());

            appendText(sDoc, headlinePrefix + " Activation\n\n", "headline");

            appendText(sDoc, "Id: ", "bold");
            appendText(sDoc, "" + act.getId() + "\n","regular" );

            appendText(sDoc, "Label: ", "bold");
            appendText(sDoc, act.getLabel() + "\n", "regular");

            appendText(sDoc, "Phase: ", "bold");
            appendText(sDoc, Phase.toString(act.getPhase()) + "\n", "regular");

            appendText(sDoc, "Fired: ", "bold");
            appendText(sDoc, act.getFired() + "\n", "regular");

            appendText(sDoc, "Reference: ", "bold");
            appendText(sDoc, act.getReference() + "\n", "regular");

            ActivationParticle ap = actIdToParticle.get(act.getId());
            if(ap != null) {
                appendText(sDoc, "X: " + ap.getPosition().x + " Y: " + ap.getPosition().y + "\n", "bold");
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendText(StyledDocument sDoc, String txt, String style) {
        try {
            sDoc.insertString(sDoc.getLength(), txt, sDoc.getStyle(style));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private Graph initGraph() {
        //        System.setProperty("org.graphstream.ui", "org.graphstream.ui.swing.util.Display");

        Graph graph = new SingleGraph("0");

        graph.setAttribute("ui.stylesheet",
                "node {" +
                    "size: 20px;" +
//                  "fill-color: #777;" +
//                  "text-mode: hidden;" +
                    "z-index: 1;" +
//                  "shadow-mode: gradient-radial; shadow-width: 2px; shadow-color: #999, white; shadow-offset: 3px, -3px;" +
                    "stroke-mode: plain; stroke-width: 2px;" +
                    "text-size: 20px;" +
                    "text-alignment: under;" +
                    "text-color: white;" +
                    "text-style: bold;" +
                    "text-background-mode: rounded-box;" +
                    "text-background-color: #222C; " +
                    "text-padding: 2px;" +
                    "text-offset: 0px, 2px;" +
        "}" +
                " edge {" +
                    "size: 2px;" +
                    "shape: cubic-curve;" +
                    "z-index: 0;" +
                    "arrow-size: 8px, 5px;" +
                "}");

        graph.setAttribute("ui.antialias");
        graph.setAutoCreate(true);

        return graph;
    }


    public Graph getGraph() {
        return graph;
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public JSplitPane getView() {
        return splitPane;
    }

    private void initModifiers() {
        neuronTypeModifiers.put(PatternNeuron.class, n -> n.setAttribute("ui.style", "fill-color: rgb(0,130,0);"));
        neuronTypeModifiers.put(PatternPartNeuron.class, n -> n.setAttribute("ui.style", "fill-color: rgb(0,205,0);"));
        neuronTypeModifiers.put(InhibitoryNeuron.class, n -> n.setAttribute("ui.style", "fill-color: rgb(100,100,255);"));

        synapseTypeModifiers.put(PatternPartSynapse.class, (e, s) -> {
            PatternPartSynapse pps = (PatternPartSynapse) s;
            if(pps.isRecurrent()) {
                e.setAttribute("ui.style", "fill-color: rgb(104,34,139);");
            }
            if(pps.isNegative()) {
                e.setAttribute("ui.style", "fill-color: rgb(100,0,0);");
            }
        });
    }

    private void pump() {
        waitForClick();

        fromViewer.pump();
        // fromViewer.blockingPump();
    }

    public synchronized void click() {
        clicked = true;
        notifyAll();
    }

    private synchronized void waitForClick() {
        try {
            while(!clicked) {
                wait();
            }
            clicked = false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivationCreationEvent(Activation act, Activation originAct) {
        Node n = onActivationEvent(act, originAct);

        n.setAttribute("aika.init-node", true);

        renderConsoleOutput("New", act);

        pump();
    }


    @Override
    public void onActivationProcessedEvent(Activation act) {
        Node n = onActivationEvent(act, null);
        n.setAttribute("aika.init-node", false);

        renderConsoleOutput("Processed", act);

        pump();
    }

    private Node onActivationEvent(Activation act, Activation originAct) {
        Graph g = getGraph();
        String id = "" + act.getId();
        Node node = g.getNode(id);

        if (node == null) {
            node = g.addNode(id);

            if(originAct != null) {
                Edge initialEdge = graph.addEdge(getEdgeId(originAct, act), "" + originAct.getId(), "" + act.getId(), true);
                initialEdge.setAttribute("ui.style", "fill-color: rgb(200,200,200);");
            }
        }

        nodeIdToActivation.put(node.getId(), act);

        node.setAttribute("aika.id", act.getId());
        if(originAct != null) {
            node.setAttribute("aika.originActId", originAct.getId());
        }
        node.setAttribute("ui.label", act.getLabel());

        if(act.getNeuron().isInputNeuron() && act.getNeuron() instanceof PatternNeuron) {
            node.setAttribute("layout.frozen");
        }
        if(act.getFired() != NOT_FIRED) {
            Fired f = act.getFired();
            node.setAttribute("x", f.getInputTimestamp() * 0.1);
            node.setAttribute("y", 0.0);
        }

        if(lastActEventNode != null) {
            lastActEventNode.setAttribute("ui.style", "stroke-color: black;");
        }

        node.setAttribute("ui.style", "stroke-color: red;");

        ActivationPhase phase = act.getPhase();
        if(phase != null) {
            Consumer<Node> neuronTypeModifier = neuronTypeModifiers.get(act.getNeuron().getClass());
            if(neuronTypeModifier != null) {
                neuronTypeModifier.accept(node);
            }
        }

        lastActEventNode = node;

        return node;
    }

    @Override
    public void onLinkProcessedEvent(Link l) {
        String edgeId = getEdgeId(l.getInput(), l.getOutput());
        Edge edge = graph.getEdge(edgeId);
        if (edge == null) {
            edge = graph.addEdge(edgeId, "" + l.getInput().getId(), "" + l.getOutput().getId(), true);
        }
        BiConsumer<Edge, Synapse> synapseTypeModifier = synapseTypeModifiers.get(l.getSynapse().getClass());
        if(synapseTypeModifier != null) {
            synapseTypeModifier.accept(edge, l.getSynapse());
        }
    }

    private String getEdgeId(Activation iAct, Activation oAct) {
        return iAct.getId() + "-" + oAct.getId();
    }

    public void viewClosed(String id) {
   //     loop = false;
    }

    public void buttonPushed(String id) {
        System.out.println("Button pushed on node "+id);
    }

    public void buttonReleased(String id) {
        System.out.println("Button released on node "+id);
    }

    public void mouseOver(String id) {
        System.out.println("Need the Mouse Options to be activated");
    }

    public void mouseLeft(String id) {
        System.out.println("Need the Mouse Options to be activated");
    }

    public Document getDocument() {
        return doc;
    }
}
