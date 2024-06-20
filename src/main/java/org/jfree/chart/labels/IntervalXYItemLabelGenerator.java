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
 * ---------------------------------
 * IntervalXYItemLabelGenerator.java
 * ---------------------------------
 * (C) Copyright 2008-2022, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.chart.labels;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Date;
import org.jfree.chart.api.PublicCloneable;

import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;

/**
 * An item label generator for datasets that implement the
 * {@link IntervalXYDataset} interface.
 */
public class IntervalXYItemLabelGenerator extends AbstractXYItemLabelGenerator
        implements XYItemLabelGenerator, Cloneable, PublicCloneable,
                   Serializable {

    /** The default item label format. */
    public static final String DEFAULT_ITEM_LABEL_FORMAT = "{5} - {6}";

    /**
     * Creates an item label generator using default number formatters.
     */
    public IntervalXYItemLabelGenerator() {
        this(DEFAULT_ITEM_LABEL_FORMAT, NumberFormat.getNumberInstance(),
            NumberFormat.getNumberInstance());
    }

    /**
     * Creates an item label generator using the specified number formatters.
     *
     * @param formatString  the item label format string ({@code null} not
     *                      permitted).
     * @param xFormat  the format object for the x values ({@code null}
     *                 not permitted).
     * @param yFormat  the format object for the y values ({@code null}
     *                 not permitted).
     */
    public IntervalXYItemLabelGenerator(String formatString,
        NumberFormat xFormat, NumberFormat yFormat) {

        super(formatString, xFormat, yFormat);
    }

    /**
     * Creates an item label generator using the specified formatters.
     *
     * @param formatString  the item label format string ({@code null}
     *                      not permitted).
     * @param xFormat  the format object for the x values ({@code null}
     *                 not permitted).
     * @param yFormat  the format object for the y values ({@code null}
     *                 not permitted).
     */
    public IntervalXYItemLabelGenerator(String formatString,
        DateFormat xFormat, NumberFormat yFormat) {

        super(formatString, xFormat, yFormat);
    }

    /**
     * Creates an item label generator using the specified formatters (a
     * number formatter for the x-values and a date formatter for the
     * y-values).
     *
     * @param formatString  the item label format string ({@code null}
     *                      not permitted).
     * @param xFormat  the format object for the x values ({@code null}
     *                 permitted).
     * @param yFormat  the format object for the y values ({@code null}
     *                 not permitted).
     */
    public IntervalXYItemLabelGenerator(String formatString,
            NumberFormat xFormat, DateFormat yFormat) {

        super(formatString, xFormat, yFormat);
    }

    /**
     * Creates a label generator using the specified date formatters.
     *
     * @param formatString  the label format string ({@code null} not
     *                      permitted).
     * @param xFormat  the format object for the x values ({@code null}
     *                 not permitted).
     * @param yFormat  the format object for the y values ({@code null}
     *                 not permitted).
     */
    public IntervalXYItemLabelGenerator(String formatString,
            DateFormat xFormat, DateFormat yFormat) {

        super(formatString, xFormat, yFormat);
    }

    /**
     * Creates the array of items that can be passed to the
     * {@link MessageFormat} class for creating labels.
     *
     * @param dataset  the dataset ({@code null} not permitted).
     * @param series  the series (zero-based index).
     * @param item  the item (zero-based index).
     *
     * @return An array of seven items from the dataset formatted as
     *         {@code String} objects (never {@code null}).
     */
    @Override
    protected Object[] createItemArray(XYDataset dataset, int series,
            int item) {

        IntervalXYDataset intervalDataset = null;
        if (dataset instanceof IntervalXYDataset) {
            intervalDataset = (IntervalXYDataset) dataset;
        }
        Object[] result = new Object[7];
        result[0] = dataset.getSeriesKey(series).toString();

        double x = dataset.getXValue(series, item);
        double xs = x;
        double xe = x;
        double y = dataset.getYValue(series, item);
        double ys = y;
        double ye = y;
        if (intervalDataset != null) {
            xs = intervalDataset.getStartXValue(series, item);
            xe = intervalDataset.getEndXValue(series, item);
            ys = intervalDataset.getStartYValue(series, item);
            ye = intervalDataset.getEndYValue(series, item);
        }

        result[1] = formatXValue(x);
        result[2] = formatXValue(xs);
        result[3] = formatXValue(xe);

        result[4] = formatYValue(dataset, series, item, y);
        result[5] = formatYValue(intervalDataset, series, item, ys, true);
        result[6] = formatYValue(intervalDataset, series, item, ye, false);

        return result;
    }
    
    private String formatXValue(double value){
        DateFormat xdf = getXDateFormat();
        if (xdf != null) {
            return xdf.format(new Date((long) value));
        }
        else {
            NumberFormat xnf = getXFormat();
            return xnf.format(value);
        }
    }
    
    private String formatYValue(XYDataset dataset, int series, int item, double value){
        NumberFormat ynf = getYFormat();
        DateFormat ydf = getYDateFormat();
        if (Double.isNaN(value) && dataset.getY(series, item) == null) {
            return getNullYString();
        }
        else {
            if (ydf != null) {
                return ydf.format(new Date((long) value));
            }
            else {
                return ynf.format(value);
            }
        }
    }
    
    private String formatYValue(IntervalXYDataset dataset, int series, int item, 
            double value, boolean isStart){
        NumberFormat ynf = getYFormat();
        DateFormat ydf = getYDateFormat();
        if (Double.isNaN(value) && dataset != null
                && (isStart ? dataset.getStartY(series, item) : dataset.getEndY(series, item)) == null) {
            return getNullYString();
        }
        else {
            if (ydf != null) {
                return ydf.format(new Date((long) value));
            }
            else {
                return ynf.format(value);
            }
        }
    }

//Refactoring end
    }

    /**
     * Generates the item label text for an item in a dataset.
     *
     * @param dataset  the dataset ({@code null} not permitted).
     * @param series  the series index (zero-based).
     * @param item  the item index (zero-based).
     *
     * @return The label text (possibly {@code null}).
     */
    @Override
    public String generateLabel(XYDataset dataset, int series, int item) {
        return generateLabelString(dataset, series, item);
    }

    /**
     * Returns an independent copy of the generator.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException if cloning is not supported.
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Tests this object for equality with an arbitrary object.
     *
     * @param obj  the other object ({@code null} permitted).
     *
     * @return A boolean.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof IntervalXYItemLabelGenerator)) {
            return false;
        }
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
