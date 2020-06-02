package me.nov.threadtear.graph;

import java.util.Map;

import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.*;
import com.mxgraph.view.*;

public class CFGraph extends mxGraph {
  private CFGComponent component;

  public CFGraph() {
    this.component = new CFGComponent(this);
    this.setAutoOrigin(true);
    this.setAutoSizeCells(true);
    this.setHtmlLabels(true);
    this.setAllowDanglingEdges(true);
    this.setStyles();
    this.resetEdgesOnMove = true;
  }

  public CFGComponent getComponent() {
    return component;
  }

  @Override
  public mxRectangle getPreferredSizeForCell(Object arg0) {
    mxRectangle size = super.getPreferredSizeForCell(arg0);
    size.setWidth(size.getWidth() + 10); // some items touch the border
    return size;
  }

  private void setStyles() {
    Map<String, Object> edgeStyle = this.getStylesheet().getDefaultEdgeStyle();
    edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
    edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_VERTICAL);
    edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_OPEN);
    edgeStyle.put(mxConstants.STYLE_TARGET_PERIMETER_SPACING, 1d);
    Map<String, Object> vertexStyle = this.getStylesheet().getDefaultVertexStyle();
    vertexStyle.put(mxConstants.STYLE_SHADOW, false);
    mxStylesheet stylesheet = new mxStylesheet();
    stylesheet.setDefaultEdgeStyle(edgeStyle);
    stylesheet.setDefaultVertexStyle(vertexStyle);
    this.setStylesheet(stylesheet);

  }

  public static class CFGComponent extends mxGraphComponent {
    private static final long serialVersionUID = 1L;
    public JScrollPane scp;

    public CFGComponent(mxGraph g) {
      super(g);
      this.setEnabled(false);
      this.setBorder(new EmptyBorder(0, 0, 0, 0));
      this.setZoomFactor(1.1);
      this.getGraphControl().addMouseWheelListener(e -> {
        if (e.isControlDown()) {
          if (e.getWheelRotation() < 0) {
            zoomIn();
          } else {
            zoomOut();
          }
          repaint();
          revalidate();
        } else if (scp != null) {
          // do we need this on linux too?
          scp.getVerticalScrollBar().setValue(scp.getVerticalScrollBar().getValue() + e.getUnitsToScroll() * scp.getVerticalScrollBar().getUnitIncrement());
        }
      });
    }

    @Override
    public void zoomIn() {
      mxGraphView view = graph.getView();
      double scale = view.getScale();
      if (scale < 4) {
        zoom(zoomFactor);
      }
    }

    @Override
    public void zoomOut() {
      mxGraphView view = graph.getView();
      double scale = view.getScale();
      if (scp != null && (scp.getVerticalScrollBar().isVisible() || scale >= 1) && scale > 0.3) {
        zoom(1 / zoomFactor);
      }
    }
  }
}
