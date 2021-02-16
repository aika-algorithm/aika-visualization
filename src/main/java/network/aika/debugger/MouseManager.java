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

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;
import org.graphstream.ui.geom.Point3;
import org.graphstream.ui.graphicGraph.GraphicElement;
import org.graphstream.ui.graphicGraph.GraphicGraph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.camera.Camera;
import org.graphstream.ui.view.util.InteractiveElement;

import javax.swing.event.MouseInputListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.EnumSet;
import java.util.Iterator;

public class MouseManager implements MouseInputListener, org.graphstream.ui.view.util.MouseManager, MouseWheelListener {
    protected View view;
    protected GraphicGraph graph;
    private final EnumSet<InteractiveElement> types;
    protected GraphicElement curElement;

    private AbstractViewManager viewManager;
    private MouseEvent lastMouseDragEvent;


    public MouseManager(AbstractViewManager viewManager) {
        this(EnumSet.of(InteractiveElement.NODE, InteractiveElement.SPRITE));
        this.viewManager = viewManager;
    }

    public MouseManager(EnumSet<InteractiveElement> types) {
        this.types = types;
    }

    public void init(GraphicGraph graph, View view) {
        this.view = view;
        this.graph = graph;
        view.addListener("Mouse", this);
        view.addListener("MouseMotion", this);
    }

    public EnumSet<InteractiveElement> getManagedTypes() {
        return this.types;
    }

    public void release() {
        this.view.removeListener("Mouse", this);
        this.view.removeListener("MouseMotion", this);
    }

    protected void mouseButtonPress(MouseEvent event) {
        this.view.requireFocus();
/*        if (!event.isShiftDown()) {
            this.graph.nodes().filter((n) -> {
                return n.hasAttribute("ui.selected");
            }).forEach((n) -> {
                n.removeAttribute("ui.selected");
            });
            this.graph.sprites().filter((s) -> {
                return s.hasAttribute("ui.selected");
            }).forEach((s) -> {
                s.removeAttribute("ui.selected");
            });
            this.graph.edges().filter((e) -> {
                return e.hasAttribute("ui.selected");
            }).forEach((e) -> {
                e.removeAttribute("ui.selected");
            });
        }*/
    }


    protected void mouseButtonRelease(MouseEvent event, Iterable<GraphicElement> elementsInArea) {
        Iterator var3 = elementsInArea.iterator();
/*
        while(var3.hasNext()) {
            GraphicElement element = (GraphicElement)var3.next();
            if (!element.hasAttribute("ui.selected")) {
                element.setAttribute("ui.selected", new Object[0]);
            }
        }
*/
    }

    protected void mouseButtonPressOnElement(GraphicElement element, MouseEvent event) {
        this.view.freezeElement(element, true);
        if (event.getButton() == 3) {
            element.setAttribute("ui.selected", new Object[0]);
        } else {
            element.setAttribute("ui.clicked", new Object[0]);

            viewManager.showElementContext("Selected", element);
        }
    }

    protected void elementMoving(GraphicElement element, MouseEvent event) {
        this.view.moveElementAtPx(element, (double)event.getX(), (double)event.getY());
    }

    protected void mouseButtonReleaseOffElement(GraphicElement element, MouseEvent event) {
        this.view.freezeElement(element, false);
        if (event.getButton() != 3) {
            element.removeAttribute("ui.clicked");
        }
    }

    public void mouseClicked(MouseEvent event) {
        viewManager.click(event.getX(), event.getY());
    }

    public void mousePressed(MouseEvent event) {
        if(!event.isShiftDown()) {
            this.curElement = view.findGraphicElementAt(this.types, event.getX() /* * 2.0  */, event.getY() /* * 2.0 */); // oder -Dsun.java2d.uiScale=100%

            if (this.curElement != null) {
                this.mouseButtonPressOnElement(this.curElement, event);
            } else {
//                System.out.println("Click");
//                viewManager.click();
            }
            float x = (float)event.getX();
            float y = (float)event.getY();
//            this.mouseButtonPress(event);
//            this.view.beginSelectionAt((double)this.x1, (double)this.y1);

            Camera camera = view.getCamera();
            Point3 pointGU = camera.transformPxToGu(x, y);

            System.out.println("Clicked: " + " x:" + pointGU.x + " y:" + pointGU.y);

            graph.edges()
                    .filter(e -> withinEdgeBoundingBox(e, pointGU))
                    .filter(e -> edgeSelected(e, pointGU))
                    .forEach(e -> System.out.println("Selected edge candidate: " + e.getId()));
        }

    }


    private boolean withinEdgeBoundingBox(Edge e, Point3 pointGU) {
        double[] ps = getCoords(e.getSourceNode());
        double[] pt = getCoords(e.getTargetNode());

        double minX = Math.min(ps[0], pt[0]);
        double minY = Math.min(ps[1], pt[1]);
        double maxX = Math.max(ps[0], pt[0]);
        double maxY = Math.max(ps[1], pt[1]);

        return minX <= pointGU.x && pointGU.x <= maxX && minY <= pointGU.y && pointGU.y <= maxY;
    }

    private double[] getCoords(Node n) {
        AbstractParticle ap = viewManager.graphManager.getParticle(n);
        return new double[] {
                ap.x,
                ap.y
        };
    }

    private boolean edgeSelected(Edge e, Point3 p) {
        return false;
    }

    public void mouseDragged(MouseEvent event) {
//        if(event.isShiftDown()) {
            if (this.curElement != null) {
                this.elementMoving(this.curElement, event);
            } else {
                //       this.view.selectionGrowsAt((double)event.getX(), (double)event.getY());

                if (lastMouseDragEvent != null) {
                    dragGraphMouseMoved(event, lastMouseDragEvent, view.getCamera());
                }
                lastMouseDragEvent = event;
            }
 //       }
    }

    public void mouseReleased(MouseEvent event) {
        lastMouseDragEvent = null;

        if (this.curElement != null) {
            this.mouseButtonReleaseOffElement(this.curElement, event);
            this.curElement = null;
        } else {
/*            float x2 = (float)event.getX();
            float y2 = (float)event.getY();
            float t;
            if (this.x1 > x2) {
                t = this.x1;
                this.x1 = x2;
                x2 = t;
            }

            if (this.y1 > y2) {
                t = this.y1;
                this.y1 = y2;
                y2 = t;
            }

            this.mouseButtonRelease(event, this.view.allGraphicElementsIn(this.types, (double)this.x1, (double)this.y1, (double)x2, (double)y2));
            this.view.endSelectionAt((double)x2, (double)y2);

 */
        }

    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
    }

    public void mouseMoved(MouseEvent event) {
        this.curElement = this.view.findGraphicElementAt(this.types, (double)event.getX() /* * 2*/, (double)event.getY() /* * 2*/);
        if(curElement != null) {
       //     System.out.println("Hover:" + curElement.getLabel());
        }

//        System.out.println("Mouse Pos: x:" + event.getX() + "y:" + event.getY());
    }

    public void dragGraphMouseMoved(MouseEvent me, MouseEvent lastMe, Camera camera) {
        // https://github.com/graphstream/gs-core/issues/301

        Point3 centerGU = camera.getViewCenter();
        Point3 centerPX = camera.transformGuToPx(centerGU.x, centerGU.y, 0);

        Point3 newCenterGU = camera.transformPxToGu(
                centerPX.x - (me.getX() - lastMe.getX()),
                centerPX.y - (me.getY() - lastMe.getY())
        );

        camera.setViewCenter(newCenterGU.x, newCenterGU.y, newCenterGU.z);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
        zoomGraphMouseWheelMoved(mwe, view.getCamera());
    }

    public static void zoomGraphMouseWheelMoved(MouseWheelEvent mwe, Camera camera) {
        // https://github.com/graphstream/gs-core/issues/301

        if (mwe.getWheelRotation() > 0) {
            double newViewPercent = camera.getViewPercent() + 0.05;
            camera.setViewPercent(newViewPercent);
        } else if (mwe.getWheelRotation() < 0) {
            double currentViewPercent = camera.getViewPercent();
            if (currentViewPercent > 0.05) {
                camera.setViewPercent(currentViewPercent - 0.05);
            }
        }
    }
}
