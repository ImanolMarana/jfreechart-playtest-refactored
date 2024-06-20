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
 * ----------------------------
 * XYBoxAndWhiskerRenderer.java
 * ----------------------------
 * (C) Copyright 2003-2021, by David Browning and Contributors.
 *
 * Original Author:  David Browning (for Australian Institute of Marine
 *                   Science);
 * Contributor(s):   David Gilbert;
 *
 */

package org.jfree.chart.renderer.xy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.event.RendererChangeEvent;
import org.jfree.chart.labels.BoxAndWhiskerXYToolTipGenerator;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.Outlier;
import org.jfree.chart.renderer.OutlierList;
import org.jfree.chart.renderer.OutlierListCollection;
import org.jfree.chart.api.RectangleEdge;
import org.jfree.chart.internal.PaintUtils;
import org.jfree.chart.internal.Args;
import org.jfree.chart.api.PublicCloneable;
import org.jfree.chart.internal.SerialUtils;
import org.jfree.data.Range;
import org.jfree.data.statistics.BoxAndWhiskerXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * A renderer that draws box-and-whisker items on an {@link XYPlot}.  This
 * renderer requires a {@link BoxAndWhiskerXYDataset}).  The example shown here
 * is generated by the{@code BoxAndWhiskerChartDemo2.java} program
 * included in the JFreeChart demo collection:
 * <br><br>
 * <img src="doc-files/XYBoxAndWhiskerRendererSample.png"
 * alt="XYBoxAndWhiskerRendererSample.png">
 * <P>
 * This renderer does not include any code to calculate the crosshair point.
 */
public class XYBoxAndWhiskerRenderer extends AbstractXYItemRenderer
        implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {

    /** For serialization. */
    private static final long serialVersionUID = -8020170108532232324L;

    /** The box width. */
    private double boxWidth;

    /** The paint used to fill the box. */
    private transient Paint boxPaint;

    /** A flag that controls whether or not the box is filled. */
    private boolean fillBox;

    /**
     * The paint used to draw various artifacts such as outliers, farout
     * symbol, average ellipse and median line.
     */
    private transient Paint artifactPaint = Color.BLACK;

    /**
     * Creates a new renderer for box and whisker charts.
     */
    public XYBoxAndWhiskerRenderer() {
        this(-1.0);
    }

    /**
     * Creates a new renderer for box and whisker charts.
     * <P>
     * Use -1 for the box width if you prefer the width to be calculated
     * automatically.
     *
     * @param boxWidth  the box width.
     */
    public XYBoxAndWhiskerRenderer(double boxWidth) {
        super();
        this.boxWidth = boxWidth;
        this.boxPaint = Color.GREEN;
        this.fillBox = true;
        setDefaultToolTipGenerator(new BoxAndWhiskerXYToolTipGenerator());
    }

    /**
     * Returns the width of each box.
     *
     * @return The box width.
     *
     * @see #setBoxWidth(double)
     */
    public double getBoxWidth() {
        return this.boxWidth;
    }

    /**
     * Sets the box width and sends a {@link RendererChangeEvent} to all
     * registered listeners.
     * <P>
     * If you set the width to a negative value, the renderer will calculate
     * the box width automatically based on the space available on the chart.
     *
     * @param width  the width.
     *
     * @see #getBoxWidth()
     */
    public void setBoxWidth(double width) {
        if (width != this.boxWidth) {
            this.boxWidth = width;
            fireChangeEvent();
        }
    }

    /**
     * Returns the paint used to fill boxes.
     *
     * @return The paint (possibly {@code null}).
     *
     * @see #setBoxPaint(Paint)
     */
    public Paint getBoxPaint() {
        return this.boxPaint;
    }

    /**
     * Sets the paint used to fill boxes and sends a {@link RendererChangeEvent}
     * to all registered listeners.
     *
     * @param paint  the paint ({@code null} permitted).
     *
     * @see #getBoxPaint()
     */
    public void setBoxPaint(Paint paint) {
        this.boxPaint = paint;
        fireChangeEvent();
    }

    /**
     * Returns the flag that controls whether or not the box is filled.
     *
     * @return A boolean.
     *
     * @see #setFillBox(boolean)
     */
    public boolean getFillBox() {
        return this.fillBox;
    }

    /**
     * Sets the flag that controls whether or not the box is filled and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param flag  the flag.
     *
     * @see #setFillBox(boolean)
     */
    public void setFillBox(boolean flag) {
        this.fillBox = flag;
        fireChangeEvent();
    }

    /**
     * Returns the paint used to paint the various artifacts such as outliers,
     * farout symbol, median line and the averages ellipse.
     *
     * @return The paint (never {@code null}).
     *
     * @see #setArtifactPaint(Paint)
     */
    public Paint getArtifactPaint() {
        return this.artifactPaint;
    }

    /**
     * Sets the paint used to paint the various artifacts such as outliers,
     * farout symbol, median line and the averages ellipse, and sends a
     * {@link RendererChangeEvent} to all registered listeners.
     *
     * @param paint  the paint ({@code null} not permitted).
     *
     * @see #getArtifactPaint()
     */
    public void setArtifactPaint(Paint paint) {
        Args.nullNotPermitted(paint, "paint");
        this.artifactPaint = paint;
        fireChangeEvent();
    }

    /**
     * Returns the range of values the renderer requires to display all the
     * items from the specified dataset.
     *
     * @param dataset  the dataset ({@code null} permitted).
     *
     * @return The range ({@code null} if the dataset is {@code null}
     *         or empty).
     *
     * @see #findDomainBounds(XYDataset)
     */
    @Override
    public Range findRangeBounds(XYDataset dataset) {
        return findRangeBounds(dataset, true);
    }

    /**
     * Returns the box paint or, if this is {@code null}, the item
     * paint.
     *
     * @param series  the series index.
     * @param item  the item index.
     *
     * @return The paint used to fill the box for the specified item (never
     *         {@code null}).
     */
    protected Paint lookupBoxPaint(int series, int item) {
        Paint p = getBoxPaint();
        if (p != null) {
            return p;
        }
        else {
            // TODO: could change this to itemFillPaint().  For backwards
            // compatibility, it might require a useFillPaint flag.
            return getItemPaint(series, item);
        }
    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param state  the renderer state.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects info about the drawing.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset (must be an instance of
     *                 {@link BoxAndWhiskerXYDataset}).
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairState  crosshair information for the plot
     *                        ({@code null} permitted).
     * @param pass  the pass index.
     */
    @Override
    public void drawItem(Graphics2D g2, XYItemRendererState state,
            Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot,
            ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset,
            int series, int item, CrosshairState crosshairState, int pass) {

        PlotOrientation orientation = plot.getOrientation();

        if (orientation == PlotOrientation.HORIZONTAL) {
            drawHorizontalItem(g2, dataArea, info, plot, domainAxis, rangeAxis,
                    dataset, series, item, crosshairState, pass);
        }
        else if (orientation == PlotOrientation.VERTICAL) {
            drawVerticalItem(g2, dataArea, info, plot, domainAxis, rangeAxis,
                    dataset, series, item, crosshairState, pass);
        }

    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects info about the drawing.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset (must be an instance of
     *                 {@link BoxAndWhiskerXYDataset}).
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairState  crosshair information for the plot
     *                        ({@code null} permitted).
     * @param pass  the pass index.
     */
    public void drawHorizontalItem(Graphics2D g2, Rectangle2D dataArea,
            PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
            ValueAxis rangeAxis, XYDataset dataset, int series,
            int item, CrosshairState crosshairState, int pass) {

        // setup for collecting optional entity info...
        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        BoxAndWhiskerXYDataset boxAndWhiskerData
                = (BoxAndWhiskerXYDataset) dataset;

        Number x = boxAndWhiskerData.getX(series, item);
        Number yMax = boxAndWhiskerData.getMaxRegularValue(series, item);
        Number yMin = boxAndWhiskerData.getMinRegularValue(series, item);
        Number yMedian = boxAndWhiskerData.getMedianValue(series, item);
        Number yAverage = boxAndWhiskerData.getMeanValue(series, item);
        Number yQ1Median = boxAndWhiskerData.getQ1Value(series, item);
        Number yQ3Median = boxAndWhiskerData.getQ3Value(series, item);

        double xx = domainAxis.valueToJava2D(x.doubleValue(), dataArea,
                plot.getDomainAxisEdge());

        RectangleEdge location = plot.getRangeAxisEdge();
        double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea,
                location);
        double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea,
                location);
        double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(),
                dataArea, location);
        double yyAverage = 0.0;
        if (yAverage != null) {
            yyAverage = rangeAxis.valueToJava2D(yAverage.doubleValue(),
                    dataArea, location);
        }
        double yyQ1Median = rangeAxis.valueToJava2D(yQ1Median.doubleValue(),
                dataArea, location);
        double yyQ3Median = rangeAxis.valueToJava2D(yQ3Median.doubleValue(),
                dataArea, location);

        double exactBoxWidth = getBoxWidth();
        double width = exactBoxWidth;
        double dataAreaX = dataArea.getHeight();
        double maxBoxPercent = 0.1;
        double maxBoxWidth = dataAreaX * maxBoxPercent;
        if (exactBoxWidth <= 0.0) {
            int itemCount = boxAndWhiskerData.getItemCount(series);
            exactBoxWidth = dataAreaX / itemCount * 4.5 / 7;
            if (exactBoxWidth < 3) {
                width = 3;
            }
            else if (exactBoxWidth > maxBoxWidth) {
                width = maxBoxWidth;
            }
            else {
                width = exactBoxWidth;
            }
        }

        g2.setPaint(getItemPaint(series, item));
        Stroke s = getItemStroke(series, item);
        g2.setStroke(s);

        // draw the upper shadow
        g2.draw(new Line2D.Double(yyMax, xx, yyQ3Median, xx));
        g2.draw(new Line2D.Double(yyMax, xx - width / 2, yyMax,
                xx + width / 2));

        // draw the lower shadow
        g2.draw(new Line2D.Double(yyMin, xx, yyQ1Median, xx));
        g2.draw(new Line2D.Double(yyMin, xx - width / 2, yyMin,
                xx + width / 2));

        // draw the body
        Shape box;
        if (yyQ1Median < yyQ3Median) {
            box = new Rectangle2D.Double(yyQ1Median, xx - width / 2,
                    yyQ3Median - yyQ1Median, width);
        }
        else {
            box = new Rectangle2D.Double(yyQ3Median, xx - width / 2,
                    yyQ1Median - yyQ3Median, width);
        }
        if (this.fillBox) {
            g2.setPaint(lookupBoxPaint(series, item));
            g2.fill(box);
        }
        g2.setStroke(getItemOutlineStroke(series, item));
        g2.setPaint(getItemOutlinePaint(series, item));
        g2.draw(box);

        // draw median
        g2.setPaint(getArtifactPaint());
        g2.draw(new Line2D.Double(yyMedian,
                xx - width / 2, yyMedian, xx + width / 2));

        // draw average - SPECIAL AIMS REQUIREMENT
        if (yAverage != null) {
            double aRadius = width / 4;
            // here we check that the average marker will in fact be visible
            // before drawing it...
            if ((yyAverage > (dataArea.getMinX() - aRadius))
                    && (yyAverage < (dataArea.getMaxX() + aRadius))) {
                Ellipse2D.Double avgEllipse = new Ellipse2D.Double(
                        yyAverage - aRadius, xx - aRadius, aRadius * 2,
                        aRadius * 2);
                g2.fill(avgEllipse);
                g2.draw(avgEllipse);
            }
        }

        // FIXME: draw outliers

        // add an entity for the item...
        if (entities != null && box.intersects(dataArea)) {
            addEntity(entities, box, dataset, series, item, yyAverage, xx);
        }

    }

    /**
     * Draws the visual representation of a single data item.
     *
     * @param g2  the graphics device.
     * @param dataArea  the area within which the plot is being drawn.
     * @param info  collects info about the drawing.
     * @param plot  the plot (can be used to obtain standard color
     *              information etc).
     * @param domainAxis  the domain axis.
     * @param rangeAxis  the range axis.
     * @param dataset  the dataset (must be an instance of
     *                 {@link BoxAndWhiskerXYDataset}).
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     * @param crosshairState  crosshair information for the plot
     *                        ({@code null} permitted).
     * @param pass  the pass index.
     */
    public void drawVerticalItem(Graphics2D g2, Rectangle2D dataArea,
            PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis,
            ValueAxis rangeAxis, XYDataset dataset, int series,
            int item, CrosshairState crosshairState, int pass) {

        EntityCollection entities = null;
        if (info != null) {
            entities = info.getOwner().getEntityCollection();
        }

        BoxAndWhiskerXYDataset boxAndWhiskerData
                = (BoxAndWhiskerXYDataset) dataset;

        Number x = boxAndWhiskerData.getX(series, item);
        Number yMax = boxAndWhiskerData.getMaxRegularValue(series, item);
        Number yMin = boxAndWhiskerData.getMinRegularValue(series, item);
        Number yMedian = boxAndWhiskerData.getMedianValue(series, item);
        Number yAverage = boxAndWhiskerData.getMeanValue(series, item);
        Number yQ1Median = boxAndWhiskerData.getQ1Value(series, item);
        Number yQ3Median = boxAndWhiskerData.getQ3Value(series, item);
        List yOutliers = boxAndWhiskerData.getOutliers(series, item);

        if (yOutliers == null) {
            yOutliers = Collections.EMPTY_LIST;
        }

        double xx = domainAxis.valueToJava2D(x.doubleValue(), dataArea,
                plot.getDomainAxisEdge());

        RectangleEdge location = plot.getRangeAxisEdge();
        double yyMax = rangeAxis.valueToJava2D(yMax.doubleValue(), dataArea,
                location);
        double yyMin = rangeAxis.valueToJava2D(yMin.doubleValue(), dataArea,
                location);
        double yyMedian = rangeAxis.valueToJava2D(yMedian.doubleValue(),
                dataArea, location);
        double yyAverage = 0.0;
        if (yAverage != null) {
            yyAverage = rangeAxis.valueToJava2D(yAverage.doubleValue(),
                    dataArea, location);
        }
        double yyQ1Median = rangeAxis.valueToJava2D(yQ1Median.doubleValue(),
                dataArea, location);
        double yyQ3Median = rangeAxis.valueToJava2D(yQ3Median.doubleValue(),
                dataArea, location);

        double width = calculateBoxWidth(dataArea, boxAndWhiskerData, series);

        g2.setPaint(getItemPaint(series, item));
        Stroke s = getItemStroke(series, item);
        g2.setStroke(s);

        drawVerticalBoxAndWhisker(g2, xx, yyMax, yyQ3Median, yyMin, yyQ1Median,
                width, series, item);

        drawMedian(g2, xx, yyMedian, width);

        double aRadius = drawAverage(g2, dataArea, xx, yyAverage, width);

        OutlierListCollection outlierListCollection = processOutliers(
                boxAndWhiskerData, series, item, rangeAxis, dataArea, location,
                xx, yOutliers, width / 3
        );

        drawOutliers(g2, outlierListCollection, width, rangeAxis, dataArea,
                location, aRadius);

        addEntityIfNecessary(entities, dataArea, boxAndWhiskerData, series,
                item, xx, yyAverage);
    }

    private double calculateBoxWidth(Rectangle2D dataArea,
                                    BoxAndWhiskerXYDataset boxAndWhiskerData,
                                    int series) {
        double exactBoxWidth = getBoxWidth();
        double width = exactBoxWidth;
        double dataAreaX = dataArea.getMaxX() - dataArea.getMinX();
        double maxBoxPercent = 0.1;
        double maxBoxWidth = dataAreaX * maxBoxPercent;
        if (exactBoxWidth <= 0.0) {
            int itemCount = boxAndWhiskerData.getItemCount(series);
            exactBoxWidth = dataAreaX / itemCount * 4.5 / 7;
            if (exactBoxWidth < 3) {
                width = 3;
            } else if (exactBoxWidth > maxBoxWidth) {
                width = maxBoxWidth;
            } else {
                width = exactBoxWidth;
            }
        }
        return width;
    }

    private void drawVerticalBoxAndWhisker(Graphics2D g2, double xx,
                                          double yyMax, double yyQ3Median,
                                          double yyMin, double yyQ1Median,
                                          double width, int series, int item) {
        g2.draw(new Line2D.Double(xx, yyMax, xx, yyQ3Median));
        g2.draw(new Line2D.Double(xx - width / 2, yyMax, xx + width / 2,
                yyMax));

        g2.draw(new Line2D.Double(xx, yyMin, xx, yyQ1Median));
        g2.draw(new Line2D.Double(xx - width / 2, yyMin, xx + width / 2,
                yyMin));

        Shape box;
        if (yyQ1Median > yyQ3Median) {
            box = new Rectangle2D.Double(xx - width / 2, yyQ3Median, width,
                    yyQ1Median - yyQ3Median);
        } else {
            box = new Rectangle2D.Double(xx - width / 2, yyQ1Median, width,
                    yyQ3Median - yyQ1Median);
        }
        if (this.fillBox) {
            g2.setPaint(lookupBoxPaint(series, item));
            g2.fill(box);
        }
        g2.setStroke(getItemOutlineStroke(series, item));
        g2.setPaint(getItemOutlinePaint(series, item));
        g2.draw(box);
    }

    private void drawMedian(Graphics2D g2, double xx, double yyMedian,
                            double width) {
        g2.setPaint(getArtifactPaint());
        g2.draw(new Line2D.Double(xx - width / 2, yyMedian, xx + width / 2,
                yyMedian));
    }

    private double drawAverage(Graphics2D g2, Rectangle2D dataArea, double xx,
                              double yyAverage, double width) {
        double aRadius = 0;
        if (yyAverage != 0.0) {
            aRadius = width / 4;
            if ((yyAverage > (dataArea.getMinY() - aRadius))
                    && (yyAverage < (dataArea.getMaxY() + aRadius))) {
                Ellipse2D.Double avgEllipse = new Ellipse2D.Double(
                        xx - aRadius, yyAverage - aRadius, aRadius * 2,
                        aRadius * 2);
                g2.fill(avgEllipse);
                g2.draw(avgEllipse);
            }
        }
        return aRadius;
    }

    private OutlierListCollection processOutliers(
            BoxAndWhiskerXYDataset boxAndWhiskerData, int series, int item,
            ValueAxis rangeAxis, Rectangle2D dataArea, RectangleEdge location,
            double xx, List yOutliers, double oRadius
    ) {
        OutlierListCollection outlierListCollection
                = new OutlierListCollection();
        List outliers = new ArrayList();
        for (int i = 0; i < yOutliers.size(); i++) {
            double outlier = ((Number) yOutliers.get(i)).doubleValue();
            processOutlier(boxAndWhiskerData, series, item, outlier,
                    outlierListCollection, outliers, rangeAxis, dataArea,
                    location, xx, oRadius);
        }
        Collections.sort(outliers);
        for (Iterator iterator = outliers.iterator(); iterator.hasNext();) {
            Outlier outlier = (Outlier) iterator.next();
            outlierListCollection.add(outlier);
        }
        return outlierListCollection;
    }

    private void processOutlier(BoxAndWhiskerXYDataset boxAndWhiskerData,
                               int series, int item, double outlier,
                               OutlierListCollection outlierListCollection,
                               List outliers, ValueAxis rangeAxis,
                               Rectangle2D dataArea, RectangleEdge location,
                               double xx, double oRadius) {
        if (outlier > boxAndWhiskerData.getMaxOutlier(series,
                item).doubleValue()) {
            outlierListCollection.setHighFarOut(true);
        } else if (outlier < boxAndWhiskerData.getMinOutlier(series,
                item).doubleValue()) {
            outlierListCollection.setLowFarOut(true);
        } else if (outlier > boxAndWhiskerData.getMaxRegularValue(series,
                item).doubleValue()) {
            double yyOutlier = rangeAxis.valueToJava2D(outlier, dataArea,
                    location);
            outliers.add(new Outlier(xx, yyOutlier, oRadius));
        } else if (outlier < boxAndWhiskerData.getMinRegularValue(series,
                item).doubleValue()) {
            double yyOutlier = rangeAxis.valueToJava2D(outlier, dataArea,
                    location);
            outliers.add(new Outlier(xx, yyOutlier, oRadius));
        }
    }

    private void drawOutliers(Graphics2D g2,
                             OutlierListCollection outlierListCollection,
                             double width, ValueAxis rangeAxis,
                             Rectangle2D dataArea, RectangleEdge location,
                             double aRadius) {
        double maxAxisValue = rangeAxis.valueToJava2D(
                rangeAxis.getUpperBound(), dataArea, location) + aRadius;
        double minAxisValue = rangeAxis.valueToJava2D(
                rangeAxis.getLowerBound(), dataArea, location) - aRadius;
        for (Iterator iterator = outlierListCollection.iterator();
             iterator.hasNext();) {
            OutlierList list = (OutlierList) iterator.next();
            Outlier outlier = list.getAveragedOutlier();
            Point2D point = outlier.getPoint();

            if (list.isMultiple()) {
                drawMultipleEllipse(point, width,
                        outlier.getRadius(), g2);
            } else {
                drawEllipse(point, outlier.getRadius(), g2);
            }
        }
        if (outlierListCollection.isHighFarOut()) {
            drawHighFarOut(aRadius, g2,
                    outlierListCollection.getHighFarOut().getX(), maxAxisValue);
        }
        if (outlierListCollection.isLowFarOut()) {
            drawLowFarOut(aRadius, g2,
                    outlierListCollection.getLowFarOut().getX(), minAxisValue);
        }
    }

    private void addEntityIfNecessary(EntityCollection entities,
                                     Rectangle2D dataArea,
                                     BoxAndWhiskerXYDataset boxAndWhiskerData,
                                     int series, int item, double xx,
                                     double yyAverage) {
        if (entities != null) {
            addEntity(entities, createEntityArea(xx, boxAndWhiskerData, series,
                            item), dataset, series, item, xx,
                    yyAverage);
        }
    }

    private Shape createEntityArea(double xx,
                                 BoxAndWhiskerXYDataset boxAndWhiskerData,
                                 int series, int item) {
        double yyQ1Median = rangeAxis.valueToJava2D(
                boxAndWhiskerData.getQ1Value(series, item).doubleValue(),
                dataArea, location);
        double yyQ3Median = rangeAxis.valueToJava2D(
                boxAndWhiskerData.getQ3Value(series, item).doubleValue(),
                dataArea, location);
        double width = calculateBoxWidth(dataArea, boxAndWhiskerData, series);
        return new Rectangle2D.Double(xx - width / 2, yyQ1Median, width,
                yyQ3Median - yyQ1Median);
    }

//Refactoring end