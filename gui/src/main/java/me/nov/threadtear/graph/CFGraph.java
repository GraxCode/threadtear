package me.nov.threadtear.graph;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.shape.mxRectangleShape;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphView;
import com.mxgraph.view.mxStylesheet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

public class CFGraph extends mxGraph {
  private final CFGComponent component;

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
    size.setWidth(size.getWidth() + 10); // some items touch the
    // border
    return size;
  }

  private void setStyles() {
    Map<String, Object> edgeStyle = this.getStylesheet().getDefaultEdgeStyle();
    edgeStyle.put(mxConstants.STYLE_ROUNDED, true);
    edgeStyle.put(mxConstants.STYLE_ELBOW, mxConstants.ELBOW_VERTICAL);
    edgeStyle.put(mxConstants.STYLE_ENDARROW, mxConstants.ARROW_DIAMOND);
    edgeStyle.put(mxConstants.STYLE_TARGET_PERIMETER_SPACING, 1d);
    edgeStyle.put(mxConstants.STYLE_STROKEWIDTH, 1.25d);

    Map<String, Object> vertexStyle = this.getStylesheet().getDefaultVertexStyle();
    vertexStyle.put(mxConstants.STYLE_AUTOSIZE, 1);
    vertexStyle.put(mxConstants.STYLE_SPACING, "5");
    vertexStyle.put(mxConstants.STYLE_ORTHOGONAL, "true");
    vertexStyle.put(mxConstants.STYLE_ROUNDED, true);
    vertexStyle.put(mxConstants.STYLE_ARCSIZE, 5);
    vertexStyle.put(mxConstants.STYLE_ALIGN, mxConstants.ALIGN_LEFT);
    mxGraphics2DCanvas.putShape(mxConstants.SHAPE_RECTANGLE, new mxRectangleShape() {
      @Override
      protected int getArcSize(mxCellState state, double w, double h) {
        return 10;
      }
    });
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
      this.setAntiAlias(true);
      this.setTextAntiAlias(true);
      this.getHorizontalScrollBar().setEnabled(false);
      this.getVerticalScrollBar().setEnabled(false);
      this.getGraphControl().addMouseWheelListener(e -> {
        if (e.isControlDown()) {
          if (e.getWheelRotation() < 0) {
            zoomIn();
          } else {
            zoomOut();
          }
          repaint();
          revalidate();
          scp.revalidate();
        } else if (scp != null) {
          // do we need this on linux too?
          scp.getVerticalScrollBar().setValue(scp.getVerticalScrollBar().getValue() +
            e.getUnitsToScroll() * scp.getVerticalScrollBar().getUnitIncrement());
        }
      });
    }

    @Override
    public void updateUI() {
      super.updateUI();
      Color bg = UIManager.getColor("backgroundContainer");
      setPageBackgroundColor(bg);
      setBackground(bg);
      SwingUtilities.invokeLater(() -> getViewport().setBackground(bg));
    }

    @Override
    public void zoomIn() {
      mxGraphView view = graph.getView();
      double scale = view.getScale();
      if (scale < 4) {
        zoom(zoomFactor);
        repaint();

      }
    }

    @Override
    public void zoomOut() {
      mxGraphView view = graph.getView();
      double scale = view.getScale();
      if (scp != null && (scp.getVerticalScrollBar().isVisible() || scale >= 1) && scale > 0.3) {
        zoom(1 / zoomFactor);
        repaint();
      }
    }

    public void resetZoom() {
      zoomTo(1.0, false);
    }
  }
}
