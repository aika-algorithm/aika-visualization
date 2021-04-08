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
package network.aika.debugger;

import network.aika.neuron.activation.Reference;
import network.aika.neuron.excitatory.PatternPartSynapse;
import network.aika.utils.Utils;
import network.aika.neuron.Neuron;
import network.aika.neuron.Synapse;
import network.aika.neuron.sign.Sign;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.function.Consumer;

public abstract class AbstractConsole extends JTextPane {
    public AbstractConsole() {
        addStylesToDocument(getStyledDocument());

        //  setFocusable(false);
        setEditable(false);
    }

    public void render(String headline, Consumer<StyledDocument> content) {
        setDoubleBuffered(true);
        setOpaque(false);
        setEnabled(false);
        setVisible(false);
        DefaultStyledDocument sDoc = new DefaultStyledDocument();
        addStylesToDocument(sDoc);
        clear();
        addHeadline(sDoc, headline);

        content.accept(sDoc);
        setStyledDocument(sDoc);
        setVisible(true);
        setEnabled(true);
    }

    public void addStylesToDocument(StyledDocument doc) {
        Color green = new Color(0, 130, 0);

        Style def = StyleContext.getDefaultStyleContext().
                getStyle(StyleContext.DEFAULT_STYLE);
        StyleConstants.setFontFamily(def, "SansSerif");

        Style regular = doc.addStyle("regular", def);
        StyleConstants.setFontSize(regular, 10);

        Style regularGreen = doc.addStyle("regularGreen", def);
        StyleConstants.setFontSize(regularGreen, 10);
        StyleConstants.setForeground(regularGreen, green);

        Style s = doc.addStyle("italic", regular);
        StyleConstants.setItalic(s, true);

        s = doc.addStyle("bold", regular);
        StyleConstants.setBold(s, true);

        s = doc.addStyle("boldGreen", regular);
        StyleConstants.setBold(s, true);
        StyleConstants.setForeground(s, green);

        s = doc.addStyle("small", regular);
        StyleConstants.setFontSize(s, 10);

        s = doc.addStyle("headline", regular);
        StyleConstants.setFontSize(s, 14);
    }

    public void clear() {
        StyledDocument sDoc = getStyledDocument();
        try {
            sDoc.remove(0, sDoc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public String getRoundStr(int round) {
        return Integer.MAX_VALUE == round ? "MAX" : "" + round;
    }

    public void renderNeuronConsoleOutput(StyledDocument sDoc, Neuron n, Reference ref) {
        appendText(sDoc, "Neuron\n\n", "headline");

        appendEntry(sDoc, "Id: ", "" + n.getId());
        appendEntry(sDoc, "Label: ", n.getLabel());
        appendEntry(sDoc, "Type: ", n.getClass().getSimpleName());
        appendEntry(sDoc, "Is Input Neuron: ", "" + n.isInputNeuron());
        appendEntry(sDoc, "Bias: ", "" + Utils.round(n.getBias()));
        appendEntry(sDoc, "Bias (recurrent): ", "" + Utils.round(n.getRecurrentBias()));
        if(!n.isTemplate()) {
            appendEntry(sDoc, "Frequency: ", "" + Utils.round(n.getFrequency()));
            appendEntry(sDoc, "N: ", "" + Utils.round(n.getSampleSpace().getN(ref)));
            appendEntry(sDoc, "LastPos: ", "" + (n.getSampleSpace().getLastPos() != null ? Utils.round(n.getSampleSpace().getLastPos()) : "X"));
            appendEntry(sDoc, "P(POS): ", "" + Utils.round(n.getP(Sign.POS, n.getSampleSpace().getN(ref))));
            appendEntry(sDoc, "P(NEG): ", "" + Utils.round(n.getP(Sign.NEG, n.getSampleSpace().getN(ref))));
            appendEntry(sDoc, "Surprisal(POS): ", "" + Utils.round(n.getSurprisal(Sign.POS, ref)));
            appendEntry(sDoc, "Surprisal(NEG): ", "" + Utils.round(n.getSurprisal(Sign.NEG, ref)));
            appendEntry(sDoc, "Template Neuron: ", templatesToString(n));
        }
    }

    private String templatesToString(Neuron<?> n) {
        StringBuilder sb = new StringBuilder();
        n.getTemplates().forEach(tn -> sb.append(tn.getId() + ":" + tn.getLabel() + ", "));
        return sb.toString();
    }

    public void renderSynapseConsoleOutput(StyledDocument sDoc, Synapse s, Reference ref) {
        appendText(sDoc, "Synapse\n\n", "headline");

        appendEntry(sDoc, "Type: ", s.getClass().getSimpleName());
        appendEntry(sDoc, "Weight: ", "" + Utils.round(s.getWeight()));
        appendEntry(sDoc, "Input: ", s.getInput().toString());
        appendEntry(sDoc, "Output: ", s.getOutput().toString());
        if(s instanceof PatternPartSynapse) {
            PatternPartSynapse pps = (PatternPartSynapse) s;

            appendEntry(sDoc, "InputScope: ", "" + pps.isInputScope());
            appendEntry(sDoc, "SamePattern: ", "" + pps.isSamePattern());
            appendEntry(sDoc, "Recurrent: ", "" + pps.isRecurrent());
            appendEntry(sDoc, "is Negative: ", "" + pps.isNegative());
        }

        appendEntry(sDoc, "Frequency(POS, POS): ", "" + Utils.round(s.getFrequency(Sign.POS, Sign.POS, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "Frequency(POS, NEG): ", "" + Utils.round(s.getFrequency(Sign.POS, Sign.NEG, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "Frequency(NEG, POS): ", "" + Utils.round(s.getFrequency(Sign.NEG, Sign.POS, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "Frequency(NEG, NEG): ", "" + Utils.round(s.getFrequency(Sign.NEG, Sign.NEG, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "N: ", "" + Utils.round(s.getSampleSpace().getN(ref)));
        appendEntry(sDoc, "LastPos: ", "" + (s.getSampleSpace().getLastPos() != null ? Utils.round(s.getSampleSpace().getLastPos()) : "X"));
        appendEntry(sDoc, "P(POS, POS) :", "" + Utils.round(s.getP(Sign.POS, Sign.POS, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "P(POS, NEG) :", "" + Utils.round(s.getP(Sign.POS, Sign.NEG, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "P(NEG, POS) :", "" + Utils.round(s.getP(Sign.NEG, Sign.POS, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "P(NEG, NEG) :", "" + Utils.round(s.getP(Sign.NEG, Sign.NEG, s.getSampleSpace().getN(ref))));
        appendEntry(sDoc, "Surprisal(POS, POS): ", "" + Utils.round(s.getSurprisal(Sign.POS, Sign.POS, ref)));
        appendEntry(sDoc, "Surprisal(POS, NEG): ", "" + Utils.round(s.getSurprisal(Sign.POS, Sign.NEG, ref)));
        appendEntry(sDoc, "Surprisal(NEG, POS): ", "" + Utils.round(s.getSurprisal(Sign.NEG, Sign.POS, ref)));
        appendEntry(sDoc, "Surprisal(NEG, NEG): ", "" + Utils.round(s.getSurprisal(Sign.NEG, Sign.NEG, ref)));
        appendEntry(sDoc, "Template: ", s.getTemplate() != null ? s.getTemplate().toString() : null);
    }

    public void appendEntry(StyledDocument sDoc, String fieldName, String fieldValue) {
        appendEntry(sDoc, fieldName, fieldValue, "bold", "regular");
    }

    public void appendEntry(StyledDocument sDoc, String fieldName, String fieldValue, String titleStyle, String style) {
        appendText(sDoc, fieldName, titleStyle);
        appendText(sDoc, fieldValue + "\n", style);
    }

    protected void appendText(StyledDocument sDoc, String txt, String style) {
        try {
            sDoc.insertString(sDoc.getLength(), txt, sDoc.getStyle(style));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void addHeadline(StyledDocument sDoc, String headline) {
        appendText(sDoc, headline + "\n\n", "headline");
    }
}
