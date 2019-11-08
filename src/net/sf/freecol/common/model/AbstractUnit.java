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

import java.util.List;
import java.util.function.Predicate;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Contains the information necessary to create a new unit.
 */
public class AbstractUnit extends FreeColObject {

    public static final String TAG = "abstractUnit";

    /** The role identifier of this AbstractUnit. */
    private String roleId = Specification.DEFAULT_ROLE_ID;

    /** The number of units. */
    private int number = 1;


    /**
     * Deliberately empty constructor, for Game.newInstance.
     */
    public AbstractUnit() {}

    /**
     * Create a new AbstractUnit.
     *
     * @param id The object identifier.
     * @param roleId The unit role identifier.
     * @param number A number of units.
     */
    public AbstractUnit(String id, String roleId, int number) {
        setId(id);
        this.roleId = roleId;
        this.number = number;
    }

    /**
     * Create a new AbstractUnit.
     *
     * @param unitType The type of unit to create.
     * @param roleId The unit role identifier.
     * @param number The number of units.
     */
    public AbstractUnit(UnitType unitType, String roleId, int number) {
        this(unitType.getId(), roleId, number);
    }

    /**
     * Creates a new {@code AbstractUnit} instance.
     *
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if an error occurs
     */
    public AbstractUnit(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }


    /**
     * Get the role identifier.
     *
     * @return The role identifier.
     */
    public final String getRoleId() {
        return this.roleId;
    }

    /**
     * Set the role identifier.
     *
     * @param roleId The new role identifier.
     */
    public final void setRoleId(final String roleId) {
        this.roleId = roleId;
    }

    /**
     * Get the number of units.
     *
     * @return The number of units.
     */
    public final int getNumber() {
        return this.number;
    }

    /**
     * Set the number of units.
     *
     * @param newNumber The new number of units.
     */
    public final void setNumber(final int newNumber) {
        this.number = newNumber;
    }

    /**
     * Add to the number.
     *
     * @param diff The amount to add.
     */
    public final void addToNumber(int diff) {
        this.number += diff;
    }

    /**
     * Gets a template describing this abstract unit with a fixed count of 1.
     *
     * @return A {@code StringTemplate} describing the abstract unit.
     */
    public StringTemplate getSingleLabel() {
        return Messages.getUnitLabel(null, getId(), 1, null, getRoleId(), null);
    }

    /**
     * Gets a template describing this abstract unit.
     *
     * @return A {@code StringTemplate} describing the abstract unit.
     */
    public StringTemplate getLabel() {
        return getLabelInternal(getId(), getRoleId(), getNumber());
    }
       
    /**
     * Gets a template describing an arbitrary abstract unit.
     *
     * @param typeId The unit type identifier.
     * @param roleId The role identifier.
     * @param number The number of units.
     * @return A {@code StringTemplate} describing the abstract unit.
     */
    private static StringTemplate getLabelInternal(String typeId,
                                                   String roleId,
                                                   int number) {
        StringTemplate tmpl = Messages.getUnitLabel(null, typeId, number,
                                                    null, roleId, null);
        return StringTemplate.template("model.abstractUnit.label")
                             .addAmount("%number%", number)
                             .addStringTemplate("%unit%", tmpl);
    }
        
    /**
     * Get a description of this abstract unit.
     *
     * @return A {@code String} describing this abstract unit.
     */
    public String getDescription() {
        return Messages.message(getLabel());
    }

    /**
     * Convenience accessor for the unit type.
     *
     * @param spec A {@code Specification} to look up the type in.
     * @return The {@code UnitType} of this abstract unit.
     */
    public UnitType getType(Specification spec) {
        return spec.getUnitType(getId());
    }

    /**
     * Convenience accessor for the role.
     *
     * @param spec A {@code Specification} to look up the role in.
     * @return The {@code Role} of this abstract unit.
     */
    public Role getRole(Specification spec) {
        return spec.getRole(getRoleId());
    }

    /**
     * Get the approximate offence power that an instantiated unit
     * corresponding to this abstract form would have.
     *
     * @param spec A {@code Specification} to look up.
     * @return The approximate offence power.
     */
    public double getOffence(Specification spec) {
        int n = getNumber();
        Role role = getRole(spec);
        UnitType type = spec.getUnitType(getId());
        return n * (type.getOffence() + role.getOffence());
    }

    /**
     * Calculate the approximate offence power of a list of units.
     *
     * @param spec A {@code Specification} to look up the type in.
     * @param units A list of {@code AbstractUnit}s.
     * @return The approximate offence power.
     */
    public static double calculateStrength(Specification spec,
                                           List<AbstractUnit> units) { 
        return sumDouble(units, au -> au.getOffence(spec));
    }

    /**
     * Get a deep copy of a list of abstract units.
     *
     * @param units The list of {@code AbstractUnit}s to copy.
     * @return A list of {@code AbstractUnit}s.
     */
    public static List<AbstractUnit> deepCopy(List<AbstractUnit> units) {
        return transform(units, alwaysTrue(), au ->
                new AbstractUnit(au.getId(), au.getRoleId(), au.getNumber()));
    }

    /**
     * Get a template for a list of abstract units.
     *
     * @param base The label template base.
     * @param units The list of {@code AbstractUnit}s to use.
     * @return A suitable {@code StringTemplate}.
     */
    public static StringTemplate getListLabel(String base,
                                              List<AbstractUnit> units) {
        StringTemplate template = StringTemplate.label(base);
        for (AbstractUnit au : units) {
            template.addStringTemplate(au.getLabel());
        }
        return template;
    }

    /**
     * Does another AbstractUnit match in all fields.
     *
     * @param other The other <code>AbstractUnit</code> to test.
     * @return True if all fields match.
     */
    public boolean matchAll(AbstractUnit other) {
        return getId().equals(other.getId())
            && this.getRoleId().equals(other.getRoleId())
            && this.getNumber() == other.getNumber();
    }

    /**
     * Create a predicate to match the type+role of an abstract unit.
     *
     * @param ut The {@code UnitType} to match.
     * @param roleId The role identifier to match.
     * @return A suitable {@code Predicate}.
     */
    public static Predicate<AbstractUnit> matcher(UnitType ut, String roleId) {
        return (AbstractUnit a) ->
            a.getId().equals(ut.getId()) && a.roleId.equals(roleId);
    }

    /**
     * Create a predicate to match the type+role of an abstract unit.
     *
     * @param au The {@code AbstractUnit} to match.
     * @return A suitable {@code Predicate}.
     */
    public static Predicate<AbstractUnit> matcher(AbstractUnit au) {
        return (AbstractUnit a) ->
            a.getId().equals(au.getId())
                && a.getRoleId().equals(au.getRoleId());
    }

    /**
     * Create a predicate to match the type+role of a unit.
     *
     * @param unit The {@code Unit} to match.
     * @return A suitable {@code Predicate}.
     */
    public static Predicate<AbstractUnit> matcher(Unit unit) {
        return matcher(unit.getType(), unit.getRole().getId());
    }

    /**
     * Do two lists of abstract units match completely.
     *
     * @param l1 The first <code>AbstractUnit</code>.
     * @param l2 The first <code>AbstractUnit</code>.
     * @return True if the lists match.
     */
    public static boolean matchUnits(List<AbstractUnit> l1,
                                     List<AbstractUnit> l2) {
        if (l1.size() != l2.size()) return false;
        for (AbstractUnit au : l1) {
            if (none(l2, a -> a.matchAll(au))) return false;
        }
        return true;
    }


    // Overide FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        AbstractUnit o = copyInCast(other, AbstractUnit.class);
        if (o == null || !super.copyIn(o)) return false;
        this.roleId = o.getRoleId();
        this.number = o.getNumber();
        return true;
    }


    // Serialization

    private static final String ROLE_TAG = "role";
    private static final String NUMBER_TAG = "number";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(ROLE_TAG, roleId);

        xw.writeAttribute(NUMBER_TAG, number);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        roleId = xr.getAttribute(ROLE_TAG, Specification.DEFAULT_ROLE_ID);

        number = xr.getAttribute(NUMBER_TAG, 1);
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
        StringBuilder sb = new StringBuilder(16);
        sb.append(number).append(' ').append(getId())
            .append(" (").append(roleId).append(')');
        return sb.toString();
    }
}
