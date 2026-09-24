/**
 *  Copyright (C) 2002-2024   The FreeCol Team
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

import static net.sf.freecol.common.model.Constants.INFINITY;
import static net.sf.freecol.common.util.RandomUtils.randomInt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.Random;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * A group of units with a common origin and purpose.
 */
public class Force extends FreeColSpecObject {

    public static final String TAG = "force";

    private static final double UNDERPROVISION_FACTOR = 1.1;

    /** The number of land units in the REF. */
    private final List<AbstractUnit> landUnits = new ArrayList<>();

    /** The number of naval units in the REF. */
    private final List<AbstractUnit> navalUnits = new ArrayList<>();

    // Internal variables that do not need serialization.
    /** The space required to transport all land units. */
    private int spaceRequired;

    /** The current naval transport capacity. */
    private int capacity;


    /**
     * Basic constructor.
     *
     * @param specification The {@code Specification} for this object.
     */
    public Force(Specification specification) {
        super(specification);

        this.capacity = this.spaceRequired = 0;
    }

    /**
     * Create a new Force.
     *
     * @param spec The {@code Specification} for this object.
     * @param units A list of {@code AbstractUnit}s defining the force.
     * @param ability An optional ability name required of the units
     *     in the force.
     */
    public Force(Specification spec, List<AbstractUnit> units, String ability) {
        this(spec);

        for (AbstractUnit au : units) {
            if (ability == null || au.getType(spec).hasAbility(ability)) {
                add(au);
            } else {
                logger.warning("Found unit lacking required ability \""
                    + ability + "\": " + au);
            }
        }
    }


    /**
     * Get the cargo space required for the land units of this force.
     *
     * @return The required cargo space.
     */
    public final int getSpaceRequired() {
        return this.spaceRequired;
    }

    /**
     * Get the cargo space provided by the naval units of this force.
     *
     * @return The provided cargo space.
     */
    public final int getCapacity() {
        return this.capacity;
    }

    /**
     * Gets all units.
     *
     * @return A copy of the list of all units.
     */
    public final List<AbstractUnit> getUnitList() {
        List<AbstractUnit> result = new ArrayList<>();
        result.addAll(this.landUnits);
        result.addAll(this.navalUnits);
        return result;
    }

    /**
     * Clear the land unit list.
     */
    public final void clearLandUnits() {
        this.landUnits.clear();
        this.spaceRequired = 0;
    }

    /**
     * Clear the naval unit list.
     */
    public final void clearNavalUnits() {
        this.navalUnits.clear();
        this.capacity = 0;
    }

    /**
     * Gets the land units.
     *
     * @return A list of the  land units.
     */
    public final List<AbstractUnit> getLandUnitsList() {
        return AbstractUnit.deepCopy(this.landUnits);
    }

    /**
     * Set the land unit list.
     *
     * @param landUnits Set the new land {@code AbstractUnit}s.
     */
    protected void setLandUnitList(List<AbstractUnit> landUnits) {
        clearLandUnits();
        this.landUnits.addAll(landUnits);
    }
    
    /**
     * Gets the naval units.
     *
     * @return A copy of the list of the naval units.
     */
    public final List<AbstractUnit> getNavalUnitsList() {
        return AbstractUnit.deepCopy(this.navalUnits);
    }

    /**
     * Set the naval unit list.
     *
     * @param navalUnits Set the new land {@code AbstractUnit}s.
     */
    protected void setNavalUnitList(List<AbstractUnit> navalUnits) {
        clearNavalUnits();
        this.navalUnits.addAll(navalUnits);
    }
    
    /**
     * Is this Force empty?
     *
     * @return True if there are no land or naval units.
     */
    public final boolean isEmpty() {
        return this.landUnits.isEmpty() && this.navalUnits.isEmpty();
    }

    /**
     * Add abstract units to this Force.
     *
     * @param au The addition to this {@code Force}.
     */
    public final void add(AbstractUnit au) {
        final Specification spec = getSpecification();
        final UnitType unitType = au.getType(spec);
        final int n = au.getNumber();
        final Predicate<AbstractUnit> matchPred = AbstractUnit.matcher(au);

        if (unitType.hasAbility(Ability.NAVAL_UNIT)) {
            AbstractUnit refUnit = find(this.navalUnits, matchPred);
            if (refUnit != null) {
                refUnit.addToNumber(n);
            } else {
                this.navalUnits.add(au);
            }
            if (unitType.canCarryUnits()) {
                this.capacity += unitType.getSpace() * n;
            }
        } else {
            AbstractUnit refUnit = find(this.landUnits, matchPred);
            if (refUnit != null) {
                refUnit.addToNumber(n);
            } else {
                this.landUnits.add(au);
            }
            this.spaceRequired += unitType.getSpaceTaken() * n;
        }
    }

    /**
     * Calculate the approximate offence power of this force.
     *
     * @param naval If true, consider only naval units, otherwise
     *     consider the land units.
     * @return The approximate offence power.
     */
    public double calculateStrength(boolean naval) {
        return AbstractUnit.calculateStrength(getSpecification(),
            (naval) ? this.navalUnits : this.landUnits);
    }

    /**
     * Ensure this force has enough naval capacity to carry all land units.
     * If not, add enough ships of the given type (or an existing naval type)
     * to cover the shortfall.
     *
     * @param shipType Optional ship type to add if more capacity is needed.
     * @return Remaining capacity after boarding (may be negative).
     */
    public int prepareToBoard(UnitType shipType) {
        // Already enough capacity
        if (spaceRequired <= capacity) {
            return capacity - spaceRequired;
        }

        // Determine which ship type to use
        UnitType type = shipType;
        if (type == null) {
            final Specification spec = getSpecification();
            AbstractUnit existing = find(navalUnits,
                au -> au.getType(spec).getSpace() > 0);
            if (existing == null) {
                // No ship type available; return deficit
                return capacity - spaceRequired;
            }
            type = existing.getType(spec);
        }

        // Compute how many ships are needed
        int shipSpace = type.getSpace();
        int needed = (spaceRequired - capacity) / shipSpace + 1;

        // Add the ships
        if (needed > 0) {
            add(new AbstractUnit(type, Specification.DEFAULT_ROLE_ID, needed));
        }

        return capacity - spaceRequired;
    }

    /**
     * Does another force match?
     *
     * @param other The other <code>Force</code> to test.
     * @return True if the other force contains the same units.
     */
    public boolean matchAll(Force other) {
        return AbstractUnit.matchUnits(this.landUnits, other.landUnits)
            && AbstractUnit.matchUnits(this.navalUnits, other.navalUnits);
    }

    /**
     * Checks if the naval capacity is sufficient to transport the land units,
     * including a 10% safety margin.
     *
     * @return True if more naval units are needed.
     */
    public boolean isUnderprovisioned() {
        // Moved logic from Monarch.shouldAddNavalUnit
        return (double)this.capacity < this.spaceRequired * UNDERPROVISION_FACTOR;
    }
    
    /**
     * Scales the size of the land units in this force by a given amount.
     * * @param addition The number of units to add to each existing abstract unit group.
     */
    public void scaleLandUnits(int addition) {
        if (addition <= 0) return;
        for (AbstractUnit au : this.landUnits) {
            au.addToNumber(addition);
            // Update spaceRequired as we go
            this.spaceRequired += au.getType(getSpecification()).getSpaceTaken() * addition;
        }
    }

    /**
     * Calculate the total land strength of this force.
     *
     * @return The total land strength.
     */
    public double getLandStrength() {
        return calculateStrength(false);
    }

    /**
     * Reduce land unit counts until the total strength falls below a limit.
     *
     * @param limit The maximum allowed strength.
     * @return The resulting strength after downsizing.
     */
    public double downsizeToLimit(double limit) {
        double strength = getLandStrength();
        if (strength < limit) return strength;

        for (AbstractUnit au : landUnits) {
            int current = au.getNumber();

            // Try reducing this unit type step by step
            for (int n = current - 1; n >= 1; n--) {
                au.setNumber(n);
                strength = getLandStrength();
                if (strength < limit) {
                    return strength;
                }
            }
        }

        return strength;
    }

    /**
     * Reduces this force until its total hire price fits within the player's gold.
     * This reproduces the exact behavior of Monarch.loadMercenaryForce().
     *
     * @param player The player attempting to hire the force.
     * @param random Random source for downsizing selection.
     * @return The final total price, or -1 if nothing can be afforded.
     */
    public int downsizeToPrice(Player player, Random random) {

        // Work on a mutable list that mirrors the original mercs list
        List<AbstractUnit> units = new ArrayList<>(getUnitList());
        List<Integer> prices = new ArrayList<>(units.size());

        // Build price list and remove invalid entries
        int totalPrice = 0;
        for (int i = 0; i < units.size(); ) {
            AbstractUnit au = units.get(i);
            int unitPrice = player.getMercenaryHirePrice(au) / au.getNumber();

            if (unitPrice <= 0 || unitPrice == INFINITY) {
                units.remove(i);
            } else {
                prices.add(unitPrice);
                totalPrice += unitPrice * au.getNumber();
                i++;
            }
        }

        // Downsizing loop (identical to original)
        while (!units.isEmpty()) {

            // If affordable, write back and return
            if (player.checkGold(totalPrice)) {

                // Reset internal state to avoid ghost values
                this.landUnits.clear();
                this.navalUnits.clear();
                this.spaceRequired = 0;
                this.capacity = 0;

                // Rebuild the force cleanly
                for (AbstractUnit au : units) {
                    add(au); // Force.add() handles land/naval sorting + counters
                }

                return totalPrice;
            }

            // Pick a random entry to reduce
            int r = randomInt(null, "merc downsize", random, units.size());
            AbstractUnit au = units.get(r);

            // Reduce price
            totalPrice -= prices.get(r);

            if (au.getNumber() > 1) {
                au.addToNumber(-1);
            } else {
                // Remove unit entirely
                units.remove(r);
                prices.remove(r);
            }
        }

        return -1;
    }

    /**
     * Creates a deep copy of this Force.
     *
     * @return A new Force with identical units and fresh internal counters.
     */
    public Force copy() {
        final Specification spec = getSpecification();
        Force f = new Force(spec);

        // Copy land units
        for (AbstractUnit au : this.landUnits) {
            UnitType type = au.getType(spec);
            f.add(new AbstractUnit(type, au.getRoleId(), au.getNumber()));
        }

        // Copy naval units
        for (AbstractUnit au : this.navalUnits) {
            UnitType type = au.getType(spec);
            f.add(new AbstractUnit(type, au.getRoleId(), au.getNumber()));
        }

        return f;
    }

    /**
     * Calculates the total hire price for all units in this force using a 
     * player-specific base price and a mercenary multiplier.
     *
     * @param player The {@code Player} attempting to hire the units.
     * @param multiplier The mercenary price percentage multiplier (e.g., 75 for 75%).
     * @return The total price of all units, or -1 if any unit in the force 
     * has an invalid price (<= 0 or INFINITY).
     */
    public int getCustomPrice(Player player, int multiplier) {
        int total = 0;

        for (AbstractUnit au : getUnitList()) {
            int base = player.getEuropeanPurchasePrice(au);

            if (base <= 0 || base == INFINITY) {
                return -1; // same behavior as loadMercenaries()
            }

            int unitPrice = base * multiplier / 100;
            total += unitPrice * au.getNumber();
        }

        return total;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        Force o = copyInCast(other, Force.class);
        if (o == null || !super.copyIn(o)) return false;
        this.setLandUnitList(o.getLandUnitsList());
        this.setNavalUnitList(o.getNavalUnitsList());
        this.spaceRequired = o.getSpaceRequired();
        this.capacity = o.getCapacity();
        return true;
    }


    // Serialization

    private static final String LAND_UNITS_TAG = "landUnits";
    private static final String NAVAL_UNITS_TAG = "navalUnits";


    /**
     * {@inheritDoc}
     */
    @Override
    public void toXML(FreeColXMLWriter xw, String tag) throws XMLStreamException {
        xw.writeStartElement(tag);

        xw.writeStartElement(NAVAL_UNITS_TAG);

        for (AbstractUnit unit : this.navalUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeStartElement(LAND_UNITS_TAG);

        for (AbstractUnit unit : this.landUnits) unit.toXML(xw);

        xw.writeEndElement();

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readFromXML(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        clearLandUnits();
        clearNavalUnits();

        while (xr.moreTags()) {
            final String tag = xr.getLocalName();

            if (LAND_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    add(new AbstractUnit(xr));
                }
            } else if (NAVAL_UNITS_TAG.equals(tag)) {
                while (xr.moreTags()) {
                    add(new AbstractUnit(xr));
                }
            } else {
                logger.warning("Bogus Force tag: " + tag);
            }
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
        StringBuilder sb = new StringBuilder(32);
        sb.append("<Force ").append(this.spaceRequired)
            .append('/').append(this.capacity);
        for (AbstractUnit au : this.landUnits) sb.append(' ').append(au);
        for (AbstractUnit au : this.navalUnits) sb.append(' ').append(au);
        sb.append('>');
        return sb.toString();
    }
}
