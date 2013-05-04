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
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * The <code>Feature</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public abstract class Feature extends FreeColObject {

    /** The source of this Feature, e.g. a UnitType. */
    private FreeColObject source;

    /** The first Turn in which this Feature applies. */
    private Turn firstTurn;

    /** The last Turn in which this Feature applies. */
    private Turn lastTurn;

    /** The duration of this Feature. By default, the duration is unlimited. */
    private int duration = 0;

    /**
     * Transient features are provided by events such as disasters and
     * goods parties, and need to be serialized by the
     * FreeColGameObject they apply to.
     */
    private boolean temporary;

    /**
     * A list of Scopes limiting the applicability of this Feature.
  	 * Allocated on demand.
     */
    private List<Scope> scopes = null;


    protected void copy(Feature other) {
        setId(other.getId());
        this.source = other.source;
        this.firstTurn = other.firstTurn;
        this.lastTurn = other.lastTurn;
        this.duration = other.duration;
        this.temporary = other.temporary;
        setScopes(other.getScopes());
    }

    /**
     * Gets a name key for this feature.
     *
     * @return A name key.
     */
    public String getNameKey() {
        return getId() + ".name";
    }

    /**
     * Does this feature have a time limit?
     *
     * @return True if the feature is time limited.
     */
    public final boolean hasTimeLimit() {
        return (firstTurn != null || lastTurn != null);
    }

    /**
     * Get the first turn of a time limit.
     *
     * @return The first turn, or null if none.
     */
    public final Turn getFirstTurn() {
        return firstTurn;
    }

    /**
     * Set the first turn of a time limit.
     *
     * @param newFirstTurn The new first turn value.
     */
    public final void setFirstTurn(final Turn newFirstTurn) {
        this.firstTurn = newFirstTurn;
    }

    /**
     * Get the last turn of a time limit.
     *
     * @return The last turn, or null if none.
     */
    public final Turn getLastTurn() {
        return lastTurn;
    }

    /**
     * Set the last turn of a time limit.
     *
     * @param newLastTurn The new last turn value.
     */
    public final void setLastTurn(final Turn newLastTurn) {
        this.lastTurn = newLastTurn;
    }

    /**
     * Does this feature have a scope?
     *
     * @return True if there are any scopes attached to this feature.
     */
    public final boolean hasScope() {
        return scopes != null && !scopes.isEmpty();
    }

    /**
     * Get the scopes for this feature.
     *
     * @return A list of <code>Scope</code>s.
     */
    public final List<Scope> getScopes() {
        if (scopes == null) return Collections.emptyList();
        return scopes;
    }

    /**
     * Set the scopes for this feature.
     *
     * @param scopes A list of new <code>Scope</code>s.
     */
    public final void setScopes(List<Scope> scopes) {
        this.scopes = scopes;
    }

    /**
     * Add a scope.
     *
     * @param scope The <code>Scope</code> to add.
     */
    private void addScope(Scope scope) {
        if (scopes == null) scopes = new ArrayList<Scope>();
        scopes.add(scope);
    }

    /**
     * Get the source of this feature.
     *
     * @return The source object.
     */
    public final FreeColObject getSource() {
        return source;
    }

    /**
     * Set the source of this feature.
     *
     * @param newSource The new source.
     */
    public final void setSource(final FreeColObject newSource) {
        this.source = newSource;
    }

    /**
     * Get the duration of this feature.
     *
     * @return The number of turns this feature lasts.
     */
    public final int getDuration() {
        return duration;
    }

    /**
     * Set the duration of this feature.
     *
     * @param newDuration The new duration.
     */
    public final void setDuration(final int newDuration) {
        this.duration = newDuration;
    }

    /**
     * Is this a temporary feature?
     *
     * True if this is a temporary feature.
     */
    public final boolean isTemporary() {
        return temporary;
    }

    /**
     * Set the temporary status.
     *
     * @param newTemporary The new temporary status.
     */
    public final void setTemporary(final boolean newTemporary) {
        this.temporary = newTemporary;
    }

    /**
     * Does this feature apply to a given object type?
     *
     * @param objectType The <code>FreeColGameObjectType</code> to test.
     * @return True if there are no scopes, or at least one scope is
     *     applicable to the object.
     */
    public boolean appliesTo(final FreeColGameObjectType objectType) {
        if (!hasScope()) return true;
        for (Scope scope : scopes) {
            if (scope.appliesTo(objectType)) return true;
        }
        return false;
    }

    /**
     * Does this feature apply to a given turn?
     *
     * @param turn The <code>Turn</code> to test.
     * @return True if the turn is not outside a valid time limit.
     */
    protected boolean appliesTo(final Turn turn) {
        return !(turn != null
            && (firstTurn != null && turn.getNumber() < firstTurn.getNumber()
                || lastTurn != null && turn.getNumber() > lastTurn.getNumber()));
    }

    /**
     * Does this feature apply to a given object type and turn.
     *
     * @param objectType The <code>FreeColGameObjectType</code> to test.
     * @param turn The <code>Turn</code> to test.
     * @return True if the feature applies.
     */
    protected boolean appliesTo(final FreeColGameObjectType objectType,
                                final Turn turn) {
        return appliesTo(turn) && appliesTo(objectType);
    }

    /**
     * Is this feature out of date with respect to a given turn?
     *
     * @param turn The <code>Turn</code> to compare to.
     * @return True if the Feature has an lastTurn turn smaller than the
     *     given turn.
     */
    public boolean isOutOfDate(Turn turn) {
        return turn != null && lastTurn != null
            && turn.getNumber() > lastTurn.getNumber();
    }


    // Override Object

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int hash = 7;
        hash += 31 * hash + (getId() == null ? 0 : getId().hashCode());
        hash += 31 * hash + (source == null ? 0 : source.hashCode());
        hash += 31 * hash + (firstTurn == null ? 0 : firstTurn.getNumber());
        hash += 31 * hash + (lastTurn == null ? 0 : lastTurn.getNumber());
        hash += 31 * hash + duration;
        hash += 31 * (temporary ? 1 : 0);
        if (scopes != null) {
            for (Scope scope : scopes) {
                // TODO: is this safe? It is an easy way to ignore the order
                // of scope elements.
                hash += scope.hashCode();
            }
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (o instanceof Feature) {
            Feature feature = (Feature) o;
            if (getId() == null) {
                if (feature.getId() != null) {
                    return false;
                }
            } else if (feature.getId() == null) {
                return false;
            } else if (!getId().equals(feature.getId())) {
                return false;
            }
            if (source != feature.source) {
                return false;
            }
            if (firstTurn == null) {
                if (feature.firstTurn != null) {
                    return false;
                }
            } else if (feature.firstTurn == null) {
                return false;
            } else if (firstTurn.getNumber() != feature.firstTurn.getNumber()) {
                return false;
            }
            if (duration != feature.duration) {
                return false;
            }
            if (temporary != feature.temporary) {
                return false;
            }
            if (scopes == null) {
                if (feature.scopes != null) {
                    return false;
                }
            } else if (feature.scopes == null) {
                return false;
            } else {
                // not very efficient, but we do not expect many
                // scopes
                for (Scope scope : scopes) {
                    if (!feature.scopes.contains(scope)) {
                        return false;
                    }
                }
                for (Scope scope : feature.scopes) {
                    if (!scopes.contains(scope)) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }


    // Serialization

    private static final String DURATION_TAG = "duration";
    private static final String FIRST_TURN_TAG = "firstTurn";
    private static final String LAST_TURN_TAG = "lastTurn";
    private static final String SOURCE_TAG = "source";
    private static final String TEMPORARY_TAG = "temporary";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);

        if (getSource() != null) {
            writeAttribute(out, SOURCE_TAG, getSource());
        }

        if (getFirstTurn() != null) {
            writeAttribute(out, FIRST_TURN_TAG, getFirstTurn().getNumber());
        }

        if (getLastTurn() != null) {
            writeAttribute(out, LAST_TURN_TAG, getLastTurn().getNumber());
        }

        if (duration != 0) {
            writeAttribute(out, DURATION_TAG, duration);
        }

        if (temporary) {
            writeAttribute(out, TEMPORARY_TAG, temporary);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        for (Scope scope : getScopes()) scope.toXML(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);

        final Specification spec = getSpecification();

        String str = getAttribute(in, SOURCE_TAG, (String)null);
        // @compat 0.9.x
        if (!hasAttribute(in, ID_ATTRIBUTE_TAG)
            && "model.colony.colonyGoodsParty".equals(str)) {
            setId("model.modifier.colonyGoodsParty");
            setSource(spec.getType("model.source.colonyGoodsParty"));
        // @end compatibility code
        } else {
            if (str == null) {
                setSource(null);
            // @compat 0.9.x
            } else if ("model.monarch.colonyGoodsParty".equals(str)) {
                setSource(spec.getType("model.source.colonyGoodsParty"));
            // @end compatibility code
            } else if (spec != null) {
                setSource(spec.getType(str));
            }
        }

        int firstTurn = getAttribute(in, FIRST_TURN_TAG, UNDEFINED);
        if (firstTurn != UNDEFINED) {
            setFirstTurn(new Turn(firstTurn));
        }

        int lastTurn = getAttribute(in, LAST_TURN_TAG, UNDEFINED);
        if (lastTurn != UNDEFINED) {
            setLastTurn(new Turn(lastTurn));
        }

        duration = getAttribute(in, DURATION_TAG, 0);

        temporary = getAttribute(in, TEMPORARY_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(XMLStreamReader in) throws XMLStreamException {
        // Clear container
        scopes = null;

        super.readChildren(in);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(XMLStreamReader in) throws XMLStreamException {
        final String tag = in.getLocalName();

        if (Scope.getXMLElementTagName().equals(tag)) {
            addScope(new Scope(in));

        } else {
            super.readChild(in);
        }
    }
}
