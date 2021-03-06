package network.aika.debugger.activations;

import network.aika.neuron.activation.Activation;
import network.aika.neuron.activation.Link;
import network.aika.debugger.AbstractGraphManager;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
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
import org.graphstream.graph.Node;


import java.util.function.Consumer;

public class ActivationGraphManager extends AbstractGraphManager<Activation, Link, ActivationParticle> {

    public ActivationGraphManager(Graph graph) {
        super(graph);
    }

    protected Long getAikaNodeId(Activation act) {
        return act != null ? Long.valueOf(act.getId()) : null;
    }

    public Edge lookupEdge(Link l, Consumer<Node> onCreate) {
        return lookupEdge(l.getInput(), l.getOutput(), onCreate);
    }

    public Edge getEdge(Link l) {
        return getEdge(l.getInput(), l.getOutput());
    }

    @Override
    public Link getLink(Edge e) {
        Activation iAct = getAikaNode(e.getSourceNode());
        Activation oAct = getAikaNode(e.getTargetNode());

        return oAct.getInputLinks()
                .filter(l -> l.getInput() == iAct)
                .findAny()
                .orElseGet(null);
    }
}
