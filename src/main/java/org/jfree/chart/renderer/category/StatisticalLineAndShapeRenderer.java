/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2022, by David Gilbert and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 * ------------------------------------
 * StatisticalLineAndShapeRenderer.java
 * ------------------------------------
 * (C) Copyright 2005-2022, by David Gilbert and Contributors.
 *
 * Original Author:  Mofeed Shahin;
 * Contributor(s):   David Gilbert;
 *                   Peter Kolb (patch 2497611);
 *
 */

package org.jfree.chart.renderer.category;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Objects;

import org.jfree.chart.api.PublicCloneable;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.internal.HashUtils;
import org.jfree.chart.internal.SerialUtils;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.api.RectangleEdge;
import org.jfree.chart.internal.PaintUtils;
import org.jfree.chart.internal.ShapeUtils;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;

/**
 * A renderer that draws shapes for each data item, and lines between data
 * items.  Each point has a mean value and a standard deviation line. For use
 * with the {@link CategoryPlot} class.  The example shown
 * here is generated by the {@code StatisticalLineChartDemo1.java} program
 * included in the JFreeChart Demo Collection:
 * <br><br>
 * <img src="doc-files/StatisticalLineRendererSample.png"
 * alt="StatisticalLineRendererSample.png">
 */
public class StatisticalLineAndShapeRenderer extends LineAndShapeRenderer
        implements Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -3557517173697777579L;

    /** The paint used to show the error indicator. */
    private transient Paint errorIndicatorPaint;

    /** 
     * The stroke used to draw the error indicators.  If null, the renderer
     * will use the itemOutlineStroke.
     */
    private transient Stroke errorIndicatorStroke;

    /**
     * Constructs a default renderer (draws shapes and lines).
     */
    public StatisticalLineAndShapeRenderer() {
        this(true, true);
    }

    /**
     * Constructs a new renderer.
     *
     * @param linesVisible  draw lines?
     * @param shapesVisible  draw shapes?
     */
    public StatisticalLineAndShapeRenderer(boolean linesVisible,
                                           boolean shapesVisible) {
        super(linesVisible, shapesVisible);
        this.errorIndicatorPaint = null;
        this.errorIndicatorStroke = null;
    }

    /**
     * Returns the paint used for the error indicators.
     *
     * @return The paint used for the error indicators (possibly
     *         {@code null}).
     *
     * @see #setErrorIndicatorPaint(Paint)
     */
    public Paint getErrorIndicatorPaint() {
        return this.errorIndicatorPaint;
    }

    /**
     * Sets the paint used for the error indicators (if {@code null},
     * the item paint is used instead) and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param paint  the paint ({@code null} permitted).
     *
     * @see #getErrorIndicatorPaint()
     */
    public void setErrorIndicatorPaint(Paint paint) {
        this.errorIndicatorPaint = paint;
        fireChangeEvent();
    }

    /**
     * Returns the stroke used for the error indicators.
     *
     * @return The stroke used for the error indicators (possibly
     *         {@code null}).
     *
     * @see #setErrorIndicatorStroke(Stroke)
     */
    public Stroke getErrorIndicatorStroke() {
        return this.errorIndicatorStroke;
    }

    /**
     * Sets the stroke used for the error indicators (if {@code null},
     * the item outline stroke is used instead) and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param stroke  the stroke ({@code null} permitted).
     *
     * @see #getErrorIndicatorStroke()
     */
    public void setErrorIndicatorStroke(Stroke stroke) {
        this.errorIndicatorStroke = stroke;
        fireChangeEvent();
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset ({@code null} permitted).
     *
     * @return The range (or {@code null} if the dataset is
     *         {@code null} or empty).
     */
    @Override
    public Range findRangeBounds(CategoryDataset dataset) {
        return findRangeBounds(dataset, true);
    }

    /**
     * Draw a single data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area in which the data is drawn.
     * @param plot  the plot.
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset (a {@link StatisticalCategoryDataset} is
     *                 required).
     * @param row  the row index (zero-based).
     * @param column  the column index (zero-based).
     * @param pass  the pass.
     */
    @Override
    public void drawItem(Graphics2D g2, CategoryItemRendererState state,
            Rectangle2D dataArea, CategoryPlot plot, CategoryAxis domainAxis,
            ValueAxis rangeAxis, CategoryDataset dataset, int row, int column,
            int pass) {

        // do nothing if item is not visible
        if (!getItemVisible(row, column)) {
            return;
        }

        // if the dataset is not a StatisticalCategoryDataset then just revert
        // to the superclass (LineAndShapeRenderer) behaviour...
        if (!(dataset instanceof StatisticalCategoryDataset)) {
            super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis,
                    dataset, row, column, pass);
            return;
        }

        int visibleRow = state.getVisibleSeriesIndex(row);
        if (visibleRow < 0) {
            return;
        }

        StatisticalCategoryDataset statDataset
                = (StatisticalCategoryDataset) dataset;
        Number meanValue = statDataset.getMeanValue(row, column);
        if (meanValue == null) {
            return;
        }

        PlotOrientation orientation = plot.getOrientation();

        // current data point...
        double x1 = calculateX(domainAxis, dataset, row, column, state, dataArea, plot);
        double y1 = rangeAxis.valueToJava2D(meanValue.doubleValue(), dataArea,
                plot.getRangeAxisEdge());

        // draw the standard deviation lines *before* the shapes (if they're
        // visible) - it looks better if the shape fill colour is different to
        // the line colour
        Number sdv = statDataset.getStdDevValue(row, column);
        if (pass == 1 && sdv != null) {
            drawStandardDeviation(g2, plot, orientation, dataArea, rangeAxis,
                    meanValue.doubleValue(), sdv.doubleValue(), x1, row, column);
        }

        Shape hotspot = null;
        if (pass == 1 && getItemShapeVisible(row, column)) {
            hotspot = drawShape(g2, orientation, row, column, x1, y1);

            // draw the item label if there is one...
            if (isItemLabelVisible(row, column)) {
                drawItemLabel(g2, orientation, dataset, row, column, x1, y1,
                        (meanValue.doubleValue() < 0.0));
            }
        }

        if (pass == 0 && getItemLineVisible(row, column) && column != 0) {
            drawLine(g2, statDataset, domainAxis, rangeAxis, dataArea, plot,
                    orientation, row, column, visibleRow, state, x1, y1);
        }

        if (pass == 1) {
            // add an item entity, if this information is being collected
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addEntity(entities, hotspot, dataset, row, column, x1, y1);
            }
        }

    }

    private double calculateX(CategoryAxis domainAxis, CategoryDataset dataset, int row,
                              int column, CategoryItemRendererState state, Rectangle2D dataArea,
                              CategoryPlot plot) {
        double x1;
        if (getUseSeriesOffset()) {
            x1 = domainAxis.getCategorySeriesMiddle(column,
                    dataset.getColumnCount(),
                    state.getVisibleSeriesIndex(row), state.getVisibleSeriesCount(),
                    getItemMargin(), dataArea, plot.getDomainAxisEdge());
        } else {
            x1 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                    dataArea, plot.getDomainAxisEdge());
        }
        return x1;
    }

    private void drawStandardDeviation(Graphics2D g2, CategoryPlot plot,
                                      PlotOrientation orientation, Rectangle2D dataArea, ValueAxis rangeAxis,
                                      double meanValue, double valueDelta, double x1, int row, int column) {
        RectangleEdge yAxisLocation = plot.getRangeAxisEdge();
        double highVal = getHighValue(rangeAxis, dataArea, yAxisLocation,
                meanValue, valueDelta);
        double lowVal = getLowValue(rangeAxis, dataArea, yAxisLocation,
                meanValue, valueDelta);

        Stroke stroke = this.errorIndicatorStroke != null ? this.errorIndicatorStroke
                : getItemOutlineStroke(row, column);
        Paint paint = this.errorIndicatorPaint != null ? this.errorIndicatorPaint
                : getItemPaint(row, column);

        drawLine(g2, orientation, x1, highVal, lowVal, stroke, paint);
    }

    private double getLowValue(ValueAxis rangeAxis, Rectangle2D dataArea,
                              RectangleEdge yAxisLocation, double meanValue, double valueDelta) {
        if ((meanValue + valueDelta) < rangeAxis.getRange().getLowerBound()) {
            return rangeAxis.valueToJava2D(
                    rangeAxis.getRange().getLowerBound(), dataArea,
                    yAxisLocation);
        } else {
            return rangeAxis.valueToJava2D(meanValue - valueDelta, dataArea,
                    yAxisLocation);
        }
    }

    private double getHighValue(ValueAxis rangeAxis, Rectangle2D dataArea,
                               RectangleEdge yAxisLocation, double meanValue, double valueDelta) {
        if ((meanValue + valueDelta) > rangeAxis.getRange().getUpperBound()) {
            return rangeAxis.valueToJava2D(
                    rangeAxis.getRange().getUpperBound(), dataArea,
                    yAxisLocation);
        } else {
            return rangeAxis.valueToJava2D(meanValue + valueDelta, dataArea,
                    yAxisLocation);
        }
    }

    private void drawLine(Graphics2D g2, PlotOrientation orientation,
                         double x1, double highVal, double lowVal, Stroke stroke, Paint paint) {
        Line2D line = new Line2D.Double();
        if (orientation == PlotOrientation.HORIZONTAL) {
            line.setLine(lowVal, x1, highVal, x1);
            g2.draw(line);
            line.setLine(lowVal, x1 - 5.0d, lowVal, x1 + 5.0d);
            g2.draw(line);
            line.setLine(highVal, x1 - 5.0d, highVal, x1 + 5.0d);
            g2.draw(line);
        } else {  // PlotOrientation.VERTICAL
            line.setLine(x1, lowVal, x1, highVal);
            g2.draw(line);
            line.setLine(x1 - 5.0d, highVal, x1 + 5.0d, highVal);
            g2.draw(line);
            line.setLine(x1 - 5.0d, lowVal, x1 + 5.0d, lowVal);
            g2.draw(line);
        }
        g2.setPaint(paint);
        g2.setStroke(stroke);
    }

    private Shape drawShape(Graphics2D g2, PlotOrientation orientation, int row,
                            int column, double x1, double y1) {
        Shape shape = getItemShape(row, column);
        if (orientation == PlotOrientation.HORIZONTAL) {
            shape = ShapeUtils.createTranslatedShape(shape, y1, x1);
        } else if (orientation == PlotOrientation.VERTICAL) {
            shape = ShapeUtils.createTranslatedShape(shape, x1, y1);
        }
        if (getItemShapeFilled(row, column)) {
            g2.setPaint(getUseFillPaint() ? getItemFillPaint(row, column)
                    : getItemPaint(row, column));
            g2.fill(shape);
        }
        if (getDrawOutlines()) {
            g2.setPaint(getUseOutlinePaint()
                    ? getItemOutlinePaint(row, column)
                    : getItemPaint(row, column));
            g2.setStroke(getItemOutlineStroke(row, column));
            g2.draw(shape);
        }
        return shape;
    }

    private void drawLine(Graphics2D g2, StatisticalCategoryDataset statDataset,
                         CategoryAxis domainAxis, ValueAxis rangeAxis, Rectangle2D dataArea,
                         CategoryPlot plot, PlotOrientation orientation, int row, int column,
                         int visibleRow, CategoryItemRendererState state, double x1, double y1) {
        Number previousValue = statDataset.getValue(row, column - 1);
        if (previousValue != null) {

            // previous data point...
            double previous = previousValue.doubleValue();
            double x0;
            if (getUseSeriesOffset()) {
                x0 = domainAxis.getCategorySeriesMiddle(
                        column - 1, statDataset.getColumnCount(),
                        visibleRow, state.getVisibleSeriesCount(),
                        getItemMargin(), dataArea,
                        plot.getDomainAxisEdge());
            } else {
                x0 = domainAxis.getCategoryMiddle(column - 1,
                        getColumnCount(), dataArea,
                        plot.getDomainAxisEdge());
            }
            double y0 = rangeAxis.valueToJava2D(previous, dataArea,
                    plot.getRangeAxisEdge());

            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                line = new Line2D.Double(y0, x0, y1, x1);
            } else if (orientation == PlotOrientation.VERTICAL) {
                line = new Line2D.Double(x0, y0, x1, y1);
            }
            g2.setPaint(getItemPaint(row, column));
            g2.setStroke(getItemStroke(row, column));
            g2.draw(line);
        }
    }

//Refactoring end

    /**
     * Tests this renderer for equality with an arbitrary object.
     *
     * @param obj  the object ({@code null} permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof StatisticalLineAndShapeRenderer)) {
            return false;
        }
        StatisticalLineAndShapeRenderer that
                = (StatisticalLineAndShapeRenderer) obj;
        if (!PaintUtils.equal(this.errorIndicatorPaint,
                that.errorIndicatorPaint)) {
            return false;
        }
        if (!Objects.equals(this.errorIndicatorStroke, that.errorIndicatorStroke)) {
            return false;
        }
        return super.equals(obj);
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code.
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = HashUtils.hashCode(hash, this.errorIndicatorPaint);
        return hash;
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the output stream.
     *
     * @throws IOException  if there is an I/O error.
     */
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtils.writePaint(this.errorIndicatorPaint, stream);
        SerialUtils.writeStroke(this.errorIndicatorStroke, stream);
    }

    /**
     * Provides serialization support.
     *
     * @param stream  the input stream.
     *
     * @throws IOException  if there is an I/O error.
     * @throws ClassNotFoundException  if there is a classpath problem.
     */
    private void readObject(ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.errorIndicatorPaint = SerialUtils.readPaint(stream);
        this.errorIndicatorStroke = SerialUtils.readStroke(stream);
    }

}
