/**
 *  Copyright (C) 2002-2019   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.common.model;

import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A range of numbers, and support routines to make a random choice therefrom.
 */
public final class RandomRange {

    private static final Logger logger = Logger.getLogger(RandomRange.class.getName());

    /** Percentage probability that the result is not zero. */
    private int probability = 0;

    /** The inclusive lower bound of the range. */
    private int minimum = 0;

    /** The inclusive upper bound of the range. */
    private int maximum = 0;

    /** Factor to multiply the final value with. */
    private int factor = 1;


    /**
     * Creates a new {@code RandomRange} instance.
     *
     * @param probability The probability of this result.
     * @param minimum The range inclusive minimum.
     * @param maximum The range inclusive maximum.
     * @param factor The result multiplier.
     */
    public RandomRange(int probability, int minimum, int maximum, int factor) {
        if (probability < 0) {
            throw new IllegalArgumentException("Negative probability "
                + probability);
        }
        if (minimum > maximum) {
            throw new IllegalArgumentException("Min " + minimum
                + " > Max " + maximum);
        }
        this.probability = probability;
        this.minimum = minimum;
        this.maximum = maximum;
        this.factor = factor;
    }

    /**
     * Read a new {@code RandomRange} instance from a stream.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if there is a problem reading the
     *     stream.
     */
    public RandomRange(FreeColXMLReader xr) throws XMLStreamException {
        readAttributes(xr);
    }


    /**
     * Get the result probability.
     *
     * @return The probability.
     */
    public final int getProbability() {
        return probability;
    }

    /**
     * Get the range lower bound.
     *
     * @return The lower bound.
     */
    public final int getMinimum() {
        return minimum;
    }

    /**
     * Get the range upper bound.
     *
     * @return The upper bound.
     */
    public final int getMaximum() {
        return maximum;
    }

    /**
     * Get the multiplication factor.
     *
     * @return The factor.
     */
    public final int getFactor() {
        return factor;
    }

    /**
     * Gets a random value from this range.
     *
     * @param prefix A logger prefix.
     * @param random A pseudo-random number source.
     * @param continuous Choose a continuous or discrete result.
     * @return A random amount of plunder as defined by this
     *     {@code RandomRange}.
     */
    public int getAmount(String prefix, Random random, boolean continuous) {
        if (probability >= 100
            || (probability > 0
                && randomInt(logger, prefix + " check-probability",
                             random, 100) < probability)) {
            int range = maximum - minimum + 1;
            if (continuous) {
                int r = randomInt(logger, prefix + " random-range",
                                  random, range * factor);
                return r + minimum * factor;
            } else {
                int r = randomInt(logger, prefix + " random-range",
                                  random, range);
                return (r + minimum) * factor;
            }
        }
        return 0;
    }


    // Serialization
    // Note: not a FreeColObject but using the same method signatures.

    private static final String FACTOR_TAG = "factor";
    private static final String MAXIMUM_TAG = "maximum";
    private static final String MINIMUM_TAG = "minimum";
    private static final String PROBABILITY_TAG = "probability";


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param xw The {@code FreeColXMLWriter} to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        xw.writeAttribute(PROBABILITY_TAG, probability);

        xw.writeAttribute(MINIMUM_TAG, minimum);

        xw.writeAttribute(MAXIMUM_TAG, maximum);

        xw.writeAttribute(FACTOR_TAG, factor);
    }

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param xr The input stream with the XML.
     */
    public void readAttributes(FreeColXMLReader xr) {
        probability = xr.getAttribute(PROBABILITY_TAG, 0);

        minimum = xr.getAttribute(MINIMUM_TAG, 0);

        maximum = xr.getAttribute(MAXIMUM_TAG, 0);

        factor = xr.getAttribute(FACTOR_TAG, 0);
    }
}
