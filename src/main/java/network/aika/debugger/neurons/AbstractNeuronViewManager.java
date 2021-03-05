/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.aika.debugger.neurons;

import network.aika.Model;
import network.aika.debugger.AbstractViewManager;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.inhibitory.InhibitoryNeuron;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static network.aika.debugger.AbstractLayout.STANDARD_DISTANCE_Y;

public abstract class AbstractNeuronViewManager extends AbstractViewManager<NeuronConsole, NeuronGraphManager> {

    private Model model;

    public AbstractNeuronViewManager(Model model) {
        super();
        this.model = model;
    }

    public Model getModel() {
        return model;
    }

    public abstract void initGraphNeurons();

    protected void drawNeuron(Neuron<?> n) {
        graphManager.lookupNode(n,
                node -> {
                    node.setAttribute("aika.neuronId", n.getId());
                    Consumer<Node> neuronTypeModifier = neuronTypeModifiers.get(n.getClass());
                    if (neuronTypeModifier != null) {
                        neuronTypeModifier.accept(node);
                    }

                    double y = n.getInputSynapses()
                            .map(s -> s.getInput())
                            .map(in -> graphManager.getNode(in))
                            .map(in -> graphManager.getParticle(in))
                            .mapToDouble(p -> p.y + STANDARD_DISTANCE_Y)
                            .max()
                            .orElse(0.0);

                    node.setAttribute("y", y);

                    n.getInputSynapses().forEach(s -> drawSynapse(s));
                    n.getOutputSynapses().forEach(s -> drawSynapse(s));
                });
    }

    protected void drawSynapse(Synapse s) {
        if(graphManager.getNode(s.getInput()) == null || graphManager.getNode(s.getOutput()) == null)
            return;

        Edge edge = graphManager.lookupEdge(s, e -> {});

        BiConsumer<Edge, Synapse> synapseTypeModifier = synapseTypeModifiers.get(s.getClass());
        if(synapseTypeModifier != null) {
            synapseTypeModifier.accept(edge, s);
        }
    }
}
