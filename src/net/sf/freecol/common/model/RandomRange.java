/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;
import static net.sf.freecol.common.util.RandomUtils.*;


/**
 * A range of numbers, and support routines to make a random choice therefrom.
 */
public class RandomRange {

    private static final Logger logger = Logger.getLogger(RandomRange.class.getName());

    /** Percentage probability that the result is not zero. */
    private int probability = 0;

    /** The inclusive lower bound of the range. */
    private int minimum = 0;

    /** The inclusive upper bound of the range. */
    private int maximum = 0;

    /** Factor to multiply the final value with. */
    private int factor = 1;

    /** A list of Scopes limiting the applicability of this Feature. */
    private List<Scope> scopes = null;


    /**
     * Creates a new <code>RandomRange</code> instance.
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
     * Read a new <code>RandomRange</code> instance from a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read from.
     * @exception XMLStreamException if there is a problem reading the
     *     stream.
     */
    public RandomRange(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
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
     * Get the scopes of this random range.
     *
     * @return The scopes of this <code>RandomRange</code>.
     */
    public List<Scope> getScopes() {
        return (scopes == null) ? Collections.<Scope>emptyList()
            : scopes;
    }

    /**
     * Add a scope.
     *
     * @param scope The <code>Scope</code> to add.
     */
    private void addScope(Scope scope) {
        if (scopes == null) scopes = new ArrayList<>();
        scopes.add(scope);
    }

    /**
     * Does an object satisfy the scopes?
     *
     * @param fco The <code>FreeColObject</code> to test.
     * @return True if the scopes are satisfied.
     */
    public boolean appliesTo(FreeColObject fco) {
        List<Scope> scs = getScopes();
        return scs.isEmpty() || any(scs, s -> s.appliesTo(fco));
    }
    
    /**
     * Gets a random value from this range.
     *
     * @param prefix A logger prefix.
     * @param random A pseudo-random number source.
     * @param continuous Choose a continuous or discrete result.
     * @return A random amount of plunder as defined by this
     *     <code>RandomRange</code>.
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

    private static final String FACTOR_TAG = "factor";
    private static final String MAXIMUM_TAG = "maximum";
    private static final String MINIMUM_TAG = "minimum";
    private static final String PROBABILITY_TAG = "probability";
    private static final String SCOPE_TAG = "scope";


    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * @param xw The <code>FreeColXMLWriter</code> to write to.
     * @exception XMLStreamException if there are any problems writing
     *     to the stream.
     */
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        xw.writeAttribute(PROBABILITY_TAG, probability);

        xw.writeAttribute(MINIMUM_TAG, minimum);

        xw.writeAttribute(MAXIMUM_TAG, maximum);

        xw.writeAttribute(FACTOR_TAG, factor);

        for (Scope scope : getScopes()) scope.toXML(xw);

        xw.writeEndElement();
    }

    /**
     * Initializes this object from an XML-representation of this object.
     *
     * @param xr The input stream with the XML.
     * @exception XMLStreamException if there are any problems reading
     *     from the stream.
     */
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        probability = xr.getAttribute(PROBABILITY_TAG, 0);

        minimum = xr.getAttribute(MINIMUM_TAG, 0);

        maximum = xr.getAttribute(MAXIMUM_TAG, 0);

        factor = xr.getAttribute(FACTOR_TAG, 0);

        // Clear containers
        if (scopes != null) scopes.clear();

        while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
            final String tag = xr.getLocalName();

            if (SCOPE_TAG.equals(tag)) {
                addScope(new Scope(xr));

            } else {
                throw new XMLStreamException("Bogus RandomRange tag: " + tag);
            }
        }
    }
}
