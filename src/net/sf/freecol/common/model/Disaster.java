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
     * Whether this disaster is natural.  Defaults to
     * <code>false</code>.
     */
    private boolean natural = false;

    /**
     * The number of effects of this disaster. Defaults to
     * <code>ONE</code>.
     */
    private Effects numberOfEffects = Effects.ONE;

    /**
     * The effects of this disaster.
     */
    private List<RandomChoice<Effect>> effects = null;



    /**
     * Create a new disaster.
     *
     * @param id The object identifier.
     * @param specification The enclosing <code>Specification</code>.
     */
    public Disaster(String id, Specification specification) {
        super(id, specification);
    }

    /**
     * Is this a natural disaster?
     *
     * @return True if this is a natural disaster.
     */
    public final boolean isNatural() {
        return natural;
    }

    /**
     * Get the number of effects.
     *
     * @return an <code>Effects</code> value
     */
    public final Effects getNumberOfEffects() {
        return numberOfEffects;
    }

    /**
     * Get the random choice list of effects.
     *
     * @return A list of <code>RandomChoice\<Effect\></code>s.
     */
    public final List<RandomChoice<Effect>> getEffects() {
        return effects;
    }


    // Serialization

    private static final String EFFECT_TAG = "effect";
    private static final String EFFECTS_TAG = "effects";
    private static final String NATURAL_TAG = "natural";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        writeAttribute(out, NATURAL_TAG, natural);

        writeAttribute(out, EFFECTS_TAG, numberOfEffects.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (RandomChoice<Effect> choice : effects) {
            choice.getObject().toXMLImpl(out);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();

        super.readAttributes(in);

        Disaster parent = spec.getType(in, EXTENDS_TAG, Disaster.class, this);

        natural = getAttribute(in, NATURAL_TAG, parent.natural);

        String str = getAttribute(in, EFFECTS_TAG, (String)null);
        numberOfEffects = (str == null) ? parent.numberOfEffects
            : Effects.valueOf(str);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        if (readShouldClearContainers(in)) {
            effects = null;
        }

        final Specification spec = getSpecification();

        Disaster parent = spec.getType(in, EXTENDS_TAG, Disaster.class, this);

        if (parent != this && !parent.effects.isEmpty()) {
            if (effects == null) {
                effects = new ArrayList<RandomChoice<Effect>>();
            }
            for (RandomChoice<Effect> choice : parent.effects) {
                Effect effect = new Effect(choice.getObject());
                effect.getFeatureContainer().replaceSource(parent, this);
                effects.add(new RandomChoice<Effect>(effect, effect.getProbability()));
            }
        }

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = in.getLocalName();

        if (EFFECT_TAG.equals(tag)) {
            if (effects == null) {
                effects = new ArrayList<RandomChoice<Effect>>();
            }
            Effect effect = new Effect(in, spec);
            effect.getFeatureContainer().replaceSource(null, this);
            effects.add(new RandomChoice<Effect>(effect, effect.getProbability()));

        } else {
            super.readChild(in);
        }
    }


    /**
     * {@inheritDoc}
     */
    public String toString() {
        String result = getId();
        for (RandomChoice<Effect> choice : effects) {
            result += " " + choice.getObject();
        }
        return result;
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "disaster".
     */
    public static String getXMLElementTagName() {
        return "disaster";
    }
}
