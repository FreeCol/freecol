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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
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

    /** Whether to apply one, many or all applicable disasters. */
    public static enum Effects { ONE, SEVERAL, ALL };

    /** Whether this disaster is natural.  Defaults to false. */
    private boolean natural = false;

    /** The number of effects of this disaster. Defaults to <code>ONE</code>. */
    private Effects numberOfEffects = Effects.ONE;

    /** The effects of this disaster. */
    private List<RandomChoice<Effect>> effects = null;


    /**
     * Create a new disaster.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
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
     * @return The <code>Effects</code> to apply.
     */
    public final Effects getNumberOfEffects() {
        return numberOfEffects;
    }

    /**
     * Get the random choice list of effects.
     *
     * @return A list of random <code>Effect</code> choices.
     */
    public final List<RandomChoice<Effect>> getEffects() {
        return (effects == null)
            ? Collections.<RandomChoice<Effect>>emptyList()
            : effects;
    }

    /**
     * Add an effect.
     *
     * @param effect The <code>Effect</code> to add.
     */
    private void addEffect(Effect effect) {
        if (effects == null) effects = new ArrayList<>();
        effects.add(new RandomChoice<>(effect, effect.getProbability()));
    }


    // Serialization

    private static final String EFFECT_TAG = "effect";
    private static final String EFFECTS_TAG = "effects";
    private static final String NATURAL_TAG = "natural";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(NATURAL_TAG, natural);

        xw.writeAttribute(EFFECTS_TAG, numberOfEffects);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (RandomChoice<Effect> choice : getEffects()) {
            choice.getObject().toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        Disaster parent = xr.getType(spec, EXTENDS_TAG, Disaster.class, this);

        natural = xr.getAttribute(NATURAL_TAG, parent.natural);

        numberOfEffects = (xr.hasAttribute(EFFECTS_TAG))
            ? xr.getAttribute(EFFECTS_TAG, Effects.class, Effects.ONE)
            : parent.numberOfEffects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            effects = null;
        }

        final Specification spec = getSpecification();
        Disaster parent = xr.getType(spec, EXTENDS_TAG, Disaster.class, this);

        if (parent != this && !parent.getEffects().isEmpty()) {
            if (effects == null) effects = new ArrayList<>();
            for (RandomChoice<Effect> choice : parent.getEffects()) {
                Effect effect = new Effect(choice.getObject());
                effect.getFeatureContainer().replaceSource(parent, this);
                addEffect(effect);
            }
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final Specification spec = getSpecification();
        final String tag = xr.getLocalName();

        if (EFFECT_TAG.equals(tag)) {
            Effect effect = new Effect(xr, spec);
            effect.getFeatureContainer().replaceSource(null, this);
            addEffect(effect);

        } else {
            super.readChild(xr);
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append("[").append(getId());
        for (RandomChoice<Effect> choice : getEffects()) {
            sb.append(" ").append(choice.getObject());
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "disaster".
     */
    public static String getXMLElementTagName() {
        return "disaster";
    }
}
