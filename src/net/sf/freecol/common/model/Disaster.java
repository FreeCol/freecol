/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import net.sf.freecol.common.util.RandomChoice;

/**
 * This class describes disasters that can happen to a Colony, such as
 * flooding, disease or Indian raids.
 */
public class Disaster extends FreeColGameObjectType {

    /**
     * Bankruptcy occurs if upkeep is enabled and a player is unable
     * to pay for the maintenance of all buildings.
     */
    public static final String BANKRUPTCY = "model.disaster.bankruptcy";

    public static enum Effects { ONE, SEVERAL, ALL };

    /**
     * Whether this disaster is natural. Defaults to
     * <code>false</code>.
     */
    private boolean natural = false;

    /**
     * The number of effects of this disaster. Defaults to
     * <code>ONE</code>.
     */
    private Effects numberOfEffects = Effects.ONE;

    /**
     * Describe effects here.
     */
    private List<RandomChoice<Effect>> effects;



    public Disaster(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Get the <code>Natural</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isNatural() {
        return natural;
    }

    /**
     * Set the <code>Natural</code> value.
     *
     * @param newNatural The new Natural value.
     */
    public final void setNatural(final boolean newNatural) {
        this.natural = newNatural;
    }

    /**
     * Get the <code>NumberOfEffects</code> value.
     *
     * @return an <code>Effects</code> value
     */
    public final Effects getNumberOfEffects() {
        return numberOfEffects;
    }

    /**
     * Set the <code>NumberOfEffects</code> value.
     *
     * @param newNumberOfEffects The new NumberOfEffects value.
     */
    public final void setNumberOfEffects(final Effects newNumberOfEffects) {
        this.numberOfEffects = newNumberOfEffects;
    }

    /**
     * Get the <code>Effects</code> value.
     *
     * @return a <code>List<RandomChoice<Effect>></code> value
     */
    public final List<RandomChoice<Effect>> getEffects() {
        return effects;
    }

    /**
     * Set the <code>Effects</code> value.
     *
     * @param newEffects The new Effects value.
     */
    public final void setEffects(final List<RandomChoice<Effect>> newEffects) {
        this.effects = newEffects;
    }


    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);
        String extendString = in.getAttributeValue(null, "extends");
        Disaster parent = (extendString == null) ? this :
            getSpecification().getDisaster(extendString);

        natural = getAttribute(in, "natural", parent.natural);
        String effectString = in.getAttributeValue(null, "effects");
        numberOfEffects = (effectString == null)
            ? parent.numberOfEffects
            : Effects.valueOf(effectString);

        effects = new ArrayList<RandomChoice<Effect>>();
        if (parent != this) {
            effects.addAll(parent.effects);
        }

    }

    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        String nodeName = in.getLocalName();
        if ("effect".equals(nodeName)) {
            Effect effect = new Effect(in, getSpecification());
            effects.add(new RandomChoice<Effect>(effect, effect.getProbability()));
        } else {
            super.readChild(in);
        }
    }


    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("natural", Boolean.toString(natural));
        out.writeAttribute("effects", numberOfEffects.toString());
    }

    /**
     * Write the children of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *     to the stream.
     */
    @Override
    protected void writeChildren(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeChildren(out);

        for (RandomChoice<Effect> choice : effects) {
            choice.getObject().toXMLImpl(out);
        }
    }

}


