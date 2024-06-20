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
 * --------------
 * LineUtils.java
 * --------------
 * (C) Copyright 2008-2022, by David Gilbert and Contributors.
 *
 * Original Author:  David Gilbert;
 * Contributor(s):   -;
 *
 */

package org.jfree.chart.internal;

import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Some utility methods for {@link Line2D} objects.
 */
public class LineUtils {

    /**
     * Clips the specified line to the given rectangle.  If any of the line
     * coordinates is not finite, then the method returns {@code false} and
     * the line is not modified.
     *
     * @param line  the line ({@code null} not permitted).
     * @param rect  the clipping rectangle ({@code null} not permitted).
     *
     * @return {@code true} if the clipped line is visible, and
     *     {@code false} otherwise.
     */
    public static boolean clipLine(Line2D line, Rectangle2D rect) {

          double x1 = line.getX1();
          double y1 = line.getY1();
          double x2 = line.getX2();
          double y2 = line.getY2();
  
          // check that the line can be worked with (bug#223)
          if ((!Double.isFinite(x1) || !Double.isFinite(y1))
                  || !Double.isFinite(x2) || !Double.isFinite(y2)) {
              return false;
          }
  
          double minX = rect.getMinX();
          double maxX = rect.getMaxX();
          double minY = rect.getMinY();
          double maxY = rect.getMaxY();
  
          int f1 = rect.outcode(x1, y1);
          int f2 = rect.outcode(x2, y2);
  
          while ((f1 | f2) != 0) {
              if ((f1 & f2) != 0) {
                  return false;
              }
              double dx = (x2 - x1);
              double dy = (y2 - y1);
              // update (x1, y1), (x2, y2) and f1 and f2 using intersections
              // then recheck
              if (f1 != 0) {
                  // first point is outside, so we update it against one of the
                  // four sides then continue
                  updateCoordinates(rect, f1, dx, dy, minX, maxX, minY, maxY, x1, y1);
                  f1 = rect.outcode(x1, y1);
              } else if (f2 != 0) {
                  // second point is outside, so we update it against one of the
                  // four sides then continue
                  updateCoordinates(rect, f2, dx, dy, minX, maxX, minY, maxY, x2, y2);
                  f2 = rect.outcode(x2, y2);
              }
          }
  
          line.setLine(x1, y1, x2, y2);
          return true;  // the line is visible - if it wasn't, we'd have
          // returned false from within the while loop above
  
      }
  
      private static void updateCoordinates(Rectangle2D rect, int f, double dx, double dy, double minX, double maxX, double minY, double maxY, double x, double y) {
          if ((f & Rectangle2D.OUT_LEFT) == Rectangle2D.OUT_LEFT
                  && dx != 0.0) {
              y = y + (minX - x) * dy / dx;
              x = minX;
          } else if ((f & Rectangle2D.OUT_RIGHT) == Rectangle2D.OUT_RIGHT
                  && dx != 0.0) {
              y = y + (maxX - x) * dy / dx;
              x = maxX;
          } else if ((f & Rectangle2D.OUT_BOTTOM) == Rectangle2D.OUT_BOTTOM
                  && dy != 0.0) {
              x = x + (maxY - y) * dx / dy;
              y = maxY;
          } else if ((f & Rectangle2D.OUT_TOP) == Rectangle2D.OUT_TOP
                  && dy != 0.0) {
              x = x + (minY - y) * dx / dy;
              y = minY;
          }
      }

//Refactoring end
    
    /**
     * Creates a new line by extending an existing line.
     *
     * @param line  the line ({@code null} not permitted).
     * @param startPercent  the amount to extend the line at the start point
     *                      end.
     * @param endPercent  the amount to extend the line at the end point end.
     *
     * @return A new line.
     */
    public static Line2D extendLine(Line2D line, double startPercent,
                              double endPercent) {
        Args.nullNotPermitted(line, "line");
        double x1 = line.getX1();
        double x2 = line.getX2();
        double deltaX = x2 - x1;
        double y1 = line.getY1();
        double y2 = line.getY2();
        double deltaY = y2 - y1;
        x1 = x1 - (startPercent * deltaX);
        y1 = y1 - (startPercent * deltaY);
        x2 = x2 + (endPercent * deltaX);
        y2 = y2 + (endPercent * deltaY);
        return new Line2D.Double(x1, y1, x2, y2);
    }

}
