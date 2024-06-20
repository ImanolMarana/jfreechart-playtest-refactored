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
 * [Java is a trademark or registered trademark of Sun Microsystems, Inc.
 * in the United States and other countries.]
 *
 * ---------------
 * PaintAlpha.java
 * ---------------
 * (C) Copyright 2011-2020 by DaveLaw and Contributors.
 *
 * Original Author:  DaveLaw (dave ATT davelaw D0TT de);
 * Contributor(s):   David Gilbert;
 *
 */

package org.jfree.chart.util;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

/**
 * This class contains static methods for the manipulation
 * of objects of type {@code Paint}
 * <p>
 * The intention is to honour the alpha-channel in the process.
 * {@code PaintAlpha} was originally conceived to improve the
 * rendering of 3D Shapes with transparent colours and to allow
 * invisible bars by making them completely transparent.
 * <p>
 * Previously {@link Color#darker()} was used for this,
 * which always returns an opaque colour.
 * <p>
 * Additionally there are methods to control the behaviour and
 * in particular a {@link PaintAlpha#cloneImage(BufferedImage) cloneImage(..)}
 * method which is needed to darken objects of type {@link TexturePaint}.
 *
 * @author  DaveLaw
 * 
 * @since 1.0.15
 */
public class PaintAlpha {
    // TODO Revert to SVN revision 2469 in JFreeChart 1.0.16
    //      (MultipleGradientPaint's / JDK issues)
    // TODO THEN: change visibility of ALL darker(...) Methods EXCEPT
    //      darker(Paint) to private!

    /**
     * Multiplier for the {@code darker} Methods.<br>
     * (taken from {@link java.awt.Color}.FACTOR)
     */
    private static final double FACTOR = 0.7;

    private static boolean legacyAlpha = false;

    /**
     * Per default {@code PaintAlpha} will try to honour alpha-channel
     * information.  In the past this was not the case.
     * If you wish legacy functionality for your application you can request
     * this here.
     *
     * @param legacyAlpha boolean
     *
     * @return the previous setting
     */
    public static boolean setLegacyAlpha(boolean legacyAlpha) {
        boolean old = PaintAlpha.legacyAlpha;
        PaintAlpha.legacyAlpha = legacyAlpha;
        return old;
    }

    /**
     * Create a new (if possible, darker) {@code Paint} of the same Type.
     * If the Type is not supported, the original {@code Paint} is returned.
     * <p>
     * @param paint a {@code Paint} implementation
     * (e.g. {@link Color}, {@link GradientPaint}, {@link TexturePaint},..)
     * <p>
     * @return a (usually new, see above) {@code Paint}
     */
    public static Paint darker(Paint paint) {

        if (paint instanceof Color) {
            return darker((Color) paint);
        }
        if (legacyAlpha == true) {
            /*
             * Legacy? Just return the original Paint.
             * (this corresponds EXACTLY to how Paints used to be darkened)
             */
            return paint;
        }
        if (paint instanceof GradientPaint) {
            return darker((GradientPaint) paint);
        }
        if (paint instanceof LinearGradientPaint) {
            return darkerLinearGradientPaint((LinearGradientPaint) paint);
        }
        if (paint instanceof RadialGradientPaint) {
            return darkerRadialGradientPaint((RadialGradientPaint) paint);
        }
        if (paint instanceof TexturePaint) {
            try {
                return darkerTexturePaint((TexturePaint) paint);
            }
            catch (Exception e) {
                /*
                 * Lots can go wrong while fiddling with Images, Color Models
                 * & such!  If anything at all goes awry, just return the original
                 * TexturePaint.  (TexturePaint's are immutable anyway, so no harm
                 * done)
                 */
                return paint;
            }
        }
        return paint;
    }

    /**
     * Similar to {@link Color#darker()}.
     * <p>
     * The essential difference is that this method
     * maintains the alpha-channel unchanged<br>
     *
     * @param paint a {@code Color}
     *
     * @return a darker version of the {@code Color}
     */
    private static Color darker(Color paint) {
        return new Color(
                (int)(paint.getRed  () * FACTOR),
                (int)(paint.getGreen() * FACTOR),
                (int)(paint.getBlue () * FACTOR), paint.getAlpha());
    }

    /**
     * Create a new {@code GradientPaint} with its colors darkened.
     *
     * @param paint  the gradient paint ({@code null} not permitted).
     *
     * @return a darker version of the {@code GradientPaint}
     */
    private static GradientPaint darker(GradientPaint paint) {
        return new GradientPaint(
                paint.getPoint1(), darker(paint.getColor1()),
                paint.getPoint2(), darker(paint.getColor2()),
                paint.isCyclic());
    }

    /**
     * Create a new Gradient with its colours darkened.
     *
     * @param paint a {@code LinearGradientPaint}
     *
     * @return a darker version of the {@code LinearGradientPaint}
     */
    private static Paint darkerLinearGradientPaint(LinearGradientPaint paint) {
        final Color[] paintColors = paint.getColors();
        for (int i = 0; i < paintColors.length; i++) {
            paintColors[i] = darker(paintColors[i]);
        }
        return new LinearGradientPaint(paint.getStartPoint(),
                paint.getEndPoint(), paint.getFractions(), paintColors,
                paint.getCycleMethod(), paint.getColorSpace(), 
                paint.getTransform());
    }

    /**
     * Create a new Gradient with its colours darkened.
     *
     * @param paint a {@code RadialGradientPaint}
     *
     * @return a darker version of the {@code RadialGradientPaint}
     */
    private static Paint darkerRadialGradientPaint(RadialGradientPaint paint) {
        final Color[] paintColors = paint.getColors();
        for (int i = 0; i < paintColors.length; i++) {
            paintColors[i] = darker(paintColors[i]);
        }
        return new RadialGradientPaint(paint.getCenterPoint(), 
                paint.getRadius(), paint.getFocusPoint(), 
                paint.getFractions(), paintColors, paint.getCycleMethod(),
                paint.getColorSpace(), paint.getTransform());
    }

    /**
     * Create a new {@code TexturePaint} with its colors darkened.
     * <p>
     * This entails cloning the underlying {@code BufferedImage},
     * then darkening each color-pixel individually!
     *
     * @param paint a {@code TexturePaint}
     *
     * @return a darker version of the {@code TexturePaint}
     */
    private static TexturePaint darkerTexturePaint(TexturePaint paint) {
        if (paint.getImage().getColorModel().isAlphaPremultiplied()) {
            // Placeholder
        }

        BufferedImage img = cloneImage(paint.getImage());

        WritableRaster ras = img.copyData(null);

        final int miX = ras.getMinX();
        final int miY = ras.getMinY();
        final int maY = ras.getMinY() + ras.getHeight();

        final int wid = ras.getWidth();

        int[] pix = new int[wid * img.getSampleModel().getNumBands()];

        if (img.getColorModel() instanceof IndexColorModel) {
            return darkenIndexedColorModel(paint, img, ras, miX, maY, wid, pix);
        }

        return darkenOtherColorModels(paint, img, ras, miX, miY, maY, wid, pix);
    }
    
    private static TexturePaint darkenIndexedColorModel(TexturePaint paint, BufferedImage img, WritableRaster ras, int miX, int maY, int wid, int[] pix) {
        int[] nco = new int[4];
        for (int y = miY; y < maY; y++) {
            pix = ras.getPixels(miX, y, wid, 1, pix);

            for (int p = 0; p < pix.length; p++) {
                nco = img.getColorModel().getComponents(pix[p], nco, 0);
                nco[0] *= FACTOR;
                nco[1] *= FACTOR;
                nco[2] *= FACTOR;
                pix[p] = img.getColorModel().getDataElement(nco, 0);
            }
            ras.setPixels(miX, y, wid, 1, pix);
        }
        img.setData(ras);

        return new TexturePaint(img, paint.getAnchorRect());
    }

    private static TexturePaint darkenOtherColorModels(TexturePaint paint, BufferedImage img, WritableRaster ras, int miX, int miY, int maY, int wid, int[] pix) {
        if (img.getSampleModel().getNumBands() == 4) {
            for (int y = miY; y < maY; y++) {
                pix = ras.getPixels(miX, y, wid, 1, pix);

                for (int p = 0; p < pix.length;) {
                    pix[p] = (int) (pix[p++] * FACTOR);
                    pix[p] = (int) (pix[p++] * FACTOR);
                    pix[p] = (int) (pix[p++] * FACTOR);
                    p++;
                }
                ras.setPixels(miX, y, wid, 1, pix);
            }
            img.setData(ras);
            return new TexturePaint(img, paint.getAnchorRect());
        } else {
            for (int y = miY; y < maY; y++) {
                pix = ras.getPixels(miX, y, wid, 1, pix);

                for (int p = 0; p < pix.length; p++) {
                    pix[p] = (int) (pix[p] * FACTOR);
                }
                ras.setPixels(miX, y, wid, 1, pix);
            }
            img.setData(ras);
            return new TexturePaint(img, paint.getAnchorRect());
        }
    }

//Refactoring end
    }

    /**
     * Clone a {@link BufferedImage}.
     * <p>
     * Note: when constructing the clone, the original Color Model Object is
     * reused.<br>  That keeps things simple and should not be a problem, as all
     * known Color Models<br>
     * ({@link java.awt.image.IndexColorModel     IndexColorModel},
     *  {@link java.awt.image.DirectColorModel    DirectColorModel},
     *  {@link java.awt.image.ComponentColorModel ComponentColorModel}) are
     * immutable.
     *
     * @param image original BufferedImage to clone
     *
     * @return a new BufferedImage reusing the original's Color Model and
     *         containing a clone of its pixels
     */
    public static BufferedImage cloneImage(BufferedImage image) {

        WritableRaster rin = image.getRaster();
        WritableRaster ras = rin.createCompatibleWritableRaster();
        /**/ ras.setRect(rin); // <- this is the code that actually COPIES the pixels

        /*
         * Buffered Images may have properties, but NEVER disclose them!
         * Nevertheless, just in case someone implements getPropertyNames()
         * one day...
         */
        Hashtable props = null;
        String[] propNames = image.getPropertyNames();
        if (propNames != null) { // ALWAYS null
            props = new Hashtable();
            for (int i = 0; i < propNames.length; i++) {
                props.put(propNames[i], image.getProperty(propNames[i]));
            }
        }
        return new BufferedImage(image.getColorModel(), ras,
                image.isAlphaPremultiplied(), props);
    }
}
