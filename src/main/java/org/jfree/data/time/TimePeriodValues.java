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
 * ---------------------
 * TimePeriodValues.java
 * ---------------------
 * (C) Copyright 2003-2022, by David Gilbert.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.data.time;

import org.jfree.chart.internal.Args;
import org.jfree.data.general.Series;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A structure containing zero, one or many {@link TimePeriodValue} instances.  
 * The time periods can overlap, and are maintained in the order that they are 
 * added to the collection.
 * <p>
 * This is similar to the {@link TimeSeries} class, except that the time 
 * periods can have irregular lengths.
 */
public class TimePeriodValues<S extends Comparable<S>> extends Series<S> 
        implements Serializable {

    /** For serialization. */
    static final long serialVersionUID = -2210593619794989709L;

    /** The list of data pairs in the series. */
    private List<TimePeriodValue> data;

    /** Index of the time period with the minimum start milliseconds. */
    private int minStartIndex = -1;
    
    /** Index of the time period with the maximum start milliseconds. */
    private int maxStartIndex = -1;
    
    /** Index of the time period with the minimum middle milliseconds. */
    private int minMiddleIndex = -1;
    
    /** Index of the time period with the maximum middle milliseconds. */
    private int maxMiddleIndex = -1;
    
    /** Index of the time period with the minimum end milliseconds. */
    private int minEndIndex = -1;
    
    /** Index of the time period with the maximum end milliseconds. */
    private int maxEndIndex = -1;

    /**
     * Creates a new (empty) collection of time period values.
     *
     * @param name  the name of the series ({@code null} not permitted).
     */
    public TimePeriodValues(S name) {
        super(name);
        this.data = new ArrayList<>();
    }

    /**
     * Returns the number of items in the series.
     *
     * @return The item count.
     */
    @Override
    public int getItemCount() {
        return this.data.size();
    }

    /**
     * Returns one data item for the series.
     *
     * @param index  the item index (in the range {@code 0} to 
     *     {@code getItemCount() -1}).
     *
     * @return One data item for the series.
     */
    public TimePeriodValue getDataItem(int index) {
        return this.data.get(index);
    }

    /**
     * Returns the time period at the specified index.
     *
     * @param index  the item index (in the range {@code 0} to 
     *     {@code getItemCount() -1}).
     *
     * @return The time period at the specified index.
     * 
     * @see #getDataItem(int)
     */
    public TimePeriod getTimePeriod(int index) {
        return getDataItem(index).getPeriod();
    }

    /**
     * Returns the value at the specified index.
     *
     * @param index  the item index (in the range {@code 0} to 
     *     {@code getItemCount() -1}).
     *
     * @return The value at the specified index (possibly {@code null}).
     * 
     * @see #getDataItem(int)
     */
    public Number getValue(int index) {
        return getDataItem(index).getValue();
    }

    /**
     * Adds a data item to the series and sends a {@link SeriesChangeEvent} to
     * all registered listeners.
     *
     * @param item  the item ({@code null} not permitted).
     */
    public void add(TimePeriodValue item) {
        Args.nullNotPermitted(item, "item");
        this.data.add(item);
        updateBounds(item.getPeriod(), this.data.size() - 1);
        fireSeriesChanged();
    }
    
    /**
     * Update the index values for the maximum and minimum bounds.
     * 
     * @param period  the time period.
     * @param index  the index of the time period.
     */
    private void updateBounds(TimePeriod period, int index) {

        long start = period.getStart().getTime();
        long end = period.getEnd().getTime();
        long middle = start + ((end - start) / 2);

        this.minStartIndex = updateBoundIndex(this.minStartIndex, start, index,
                (i) -> getDataItem(i).getPeriod().getStart().getTime());

        this.maxStartIndex = updateBoundIndex(this.maxStartIndex, start, index,
                (i) -> getDataItem(i).getPeriod().getStart().getTime());

        this.minMiddleIndex = updateBoundIndex(this.minMiddleIndex, middle, index,
                (i) -> {
                    TimePeriod p = getDataItem(i).getPeriod();
                    long s = p.getStart().getTime();
                    long e = p.getEnd().getTime();
                    return s + (e - s) / 2;
                });

        this.maxMiddleIndex = updateBoundIndex(this.maxMiddleIndex, middle, index,
                (i) -> {
                    TimePeriod p = getDataItem(i).getPeriod();
                    long s = p.getStart().getTime();
                    long e = p.getEnd().getTime();
                    return s + (e - s) / 2;
                });

        this.minEndIndex = updateBoundIndex(this.minEndIndex, end, index,
                (i) -> getDataItem(i).getPeriod().getEnd().getTime());

        this.maxEndIndex = updateBoundIndex(this.maxEndIndex, end, index,
                (i) -> getDataItem(i).getPeriod().getEnd().getTime());
      }

      private int updateBoundIndex(int currentIndex, long value, int newIndex,
                                  IndexValueExtractor extractor) {
        if (currentIndex >= 0) {
          long currentValue = extractor.getValue(currentIndex);
          if (value < currentValue) {
            return newIndex;
          }
        } else {
          return newIndex;
        }
        return currentIndex;
      }

      private interface IndexValueExtractor {
        long getValue(int index);
      }
//Refactoring end