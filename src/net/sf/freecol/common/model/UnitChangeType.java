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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The possible changes of a unit type.
 */
public class UnitChangeType extends FreeColSpecObjectType {

    public static final String TAG = "unit-change-type";

    /** Changes due to the unit being captured. */
    public static final String CAPTURE
        = "model.unitChange.capture";

    /** Changes due to a clear-skill command. */
    public static final String CLEAR_SKILL
        = "model.unitChange.clearSkill";

    /** Changes due to a unit being converted by missionaries. */
    public static final String CONVERSION
        = "model.unitChange.conversion";

    /** Changes to be made immediately at creation of a unit. */
    public static final String CREATION
        = "model.unitChange.creation";

    /** Changes due to a unit being demoted after losing a battle. */
    public static final String DEMOTION
        = "model.unitChange.demotion";

    /** Changes due to education. */
    public static final String EDUCATION
        = "model.unitChange.education";

    /**
     * Changes when a unit begins to work in a colony.  This is not in
     * the standard spec but in the convertUpgrade mod.  However it needs
     * coding support to work.
     */
    public static final String ENTER_COLONY
        = "model.unitChange.enterColony";

    /** Changes due to experience. */
    public static final String EXPERIENCE
        = "model.unitChange.experience";

    /** Change when a founding father is elected. */
    public static final String FOUNDING_FATHER
        = "model.unitChange.foundingFather";

    /** Changes at declaration of independence. */
    public static final String INDEPENDENCE
        = "model.unitChange.independence";

    /** Changes from exploring a lost city. */
    public static final String LOST_CITY
        = "model.unitChange.lostCity";

    /** Changes from living with the natives. */
    public static final String NATIVES
        = "model.unitChange.natives";

    /** Changes due to the unit being promoted after winning a battle. */
    public static final String PROMOTION
        = "model.unitChange.promotion";

    /** Changes due to the undead claiming the unit. */
    public static final String UNDEAD
        = "model.unitChange.undead";


    /** The individual unit changes valid for this change type. */
    public final Map<UnitType, List<UnitTypeChange>> changes = new HashMap<>();

    /** True if this type of change always implies a change of owner. */
    private boolean ownerChange = false;


    /**
     * Trivial constructor.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to use.
     */
    public UnitChangeType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the owner change state.
     *
     * @return True if this type change is expected to be accompanied by
     *     an owner change.
     */
    public boolean getOwnerChange() {
        return this.ownerChange;
    }

    /**
     * Get the change map.
     *
     * @return The map of unit type to eligible changes.
     */
    protected Map<UnitType, List<UnitTypeChange>> getChanges() {
        return this.changes;
    }

    /**
     * Set the change map.
     *
     * @param changes The new map of unit type to eligible changes.
     */
    protected void setChanges(Map<UnitType, List<UnitTypeChange>> changes) {
        this.changes.clear();
        this.changes.putAll(changes);
    }

    /**
     * Add a new change.
     *
     * Public for the test suite.
     *
     * @param uc The {@code UnitTypeChange} to add.
     */
    public void addUnitTypeChange(UnitTypeChange uc) {
        appendToMapList(this.changes, uc.from, uc);
    }

    /**
     * Get the unit changes possible from a given unit type.
     *
     * @param from The source {@code UnitType}.
     * @return A list of {@code UnitChange}s.
     */
    public List<UnitTypeChange> getUnitChanges(UnitType from) {
        List<UnitTypeChange> ret = this.changes.get(from);
        return (ret == null) ? Collections.<UnitTypeChange>emptyList() : ret;
    }

    /**
     * Get a specific unit change for this unit change type, a
     * source unit type to change, and an optional destination unit type.
     *
     * @param fromType The {@code UnitType} to change from.
     * @param toType An optional {@code UnitType} to change to.
     * @return The {@code UnitChange} found, or null if the
     *     change is impossible.
     */
    public UnitTypeChange getUnitChange(UnitType fromType,
                                        final UnitType toType) {
        return find(getUnitChanges(fromType),
            ((toType == null) ? alwaysTrue() : uc -> uc.to == toType));
    }

    // @compat 0.11.6
    public void deleteUnitChanges(UnitType from) {
        this.changes.remove(from);
    }
    // end @compat 0.11.6


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        UnitChangeType o = copyInCast(other, UnitChangeType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.setChanges(o.getChanges());
        this.ownerChange = o.getOwnerChange();
        return true;
    }


    // Serialization

    private static final String OWNER_CHANGE_TAG = "owner-change";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(OWNER_CHANGE_TAG, this.ownerChange);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        for (List<UnitTypeChange> utcs : this.changes.values()) {
            for (UnitTypeChange uc : utcs) uc.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        this.ownerChange = xr.getAttribute(OWNER_CHANGE_TAG, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            this.changes.clear();
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

        if (UnitTypeChange.TAG.equals(tag)) {
            addUnitTypeChange(new UnitTypeChange(xr, spec));

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }


    // Override Object
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[').append(getId()).append(' ').append(getSuffix())
            .append(" ownerChange=").append(this.ownerChange);
        for (Map.Entry<UnitType, List<UnitTypeChange>> entry
                 : this.changes.entrySet()) {
            sb.append(' ').append(entry.getKey().getSuffix()).append('[');
            for (UnitTypeChange uc : entry.getValue()) {
                sb.append(' ').append(uc);
            }
            sb.append(" ]");
        }
        sb.append(']');
        return sb.toString();
    }
}
