/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.Role;

/**
 * Represents one of the European nations present in the game, i.e. both REFs
 * and possible human players.
 */
public class EuropeanNationType extends NationType {


    /**
     * Whether this is an REF Nation.
     */
    private boolean ref;

    /**
     * Stores the starting units of this Nation.
     */
    private List<AbstractUnit> startingUnits;

    /**
     * Stores the starting units of this Nation at various
     * difficulties.
     */
    private Map<String, Map<String, AbstractUnit>> startingUnitMap =
        new HashMap<String, Map<String, AbstractUnit>>();

    /**
     * Constructor.
     */
    public EuropeanNationType() {
        setTypeOfSettlement(SettlementType.SMALL_COLONY);
    }

    /**
     * Returns the name of this Nation's Home Port.
     *
     * @return a <code>String</code> value
     */
    public String getEuropeName() {
        return Messages.message(getId() + ".europe");
    }

    /**
     * Returns the name of this Nation's REF.
     *
     * @return a <code>String</code> value
     */
    public String getREFName() {
        return Messages.message(getId() + ".ref");
    }

    /**
     * Get the <code>REF</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isREF() {
        return ref;
    }

    /**
     * Set the <code>REF</code> value.
     *
     * @param newREF The new REF value.
     */
    public final void setREF(final boolean newREF) {
        this.ref = newREF;
    }

    /**
     * Returns true.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isEuropean() {
        return true;
    }

    /**
     * Returns a list of this Nation's starting units.
     *
     * @return a list of this Nation's starting units.
     */
    public List<AbstractUnit> getStartingUnits() {
        return startingUnits;
    }

    /**
     * Returns a list of this Nation's starting units at the given
     * difficulty.
     *
     * @param difficulty the ID of a difficulty level.
     * @return a list of this Nation's starting units.
     */
    public List<AbstractUnit> getStartingUnits(String difficulty) {
        Map<String, AbstractUnit> result = new HashMap<String, AbstractUnit>();
        Map<String, AbstractUnit> defaultMap = startingUnitMap.get(null);
        Map<String, AbstractUnit> difficultyMap = startingUnitMap.get(difficulty);
        if (defaultMap != null) {
            result.putAll(defaultMap);
        }
        if (difficultyMap != null) {
            result.putAll(difficultyMap);
        }
        return new ArrayList<AbstractUnit>(result.values());
    }

    /**
     * Applies the difficulty level to this nation type.
     *
     * @param difficultyLevel difficulty level to apply
     */
    public void applyDifficultyLevel(String difficulty) {
        startingUnits = getStartingUnits(difficulty);
    }


    public void readAttributes(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        ref = getAttribute(in, "ref", false);
    }

    public void readChildren(XMLStreamReader in, Specification specification)
            throws XMLStreamException {
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            String childName = in.getLocalName();
            if ("unit".equals(childName)) {
                String id = in.getAttributeValue(null, "id");
                String type = in.getAttributeValue(null, "type");
                Role role = Enum.valueOf(Role.class, getAttribute(in, "role", "default").toUpperCase());
                String difficulty = in.getAttributeValue(null, "difficulty");
                AbstractUnit unit = new AbstractUnit(type, role, 1);
                Map<String, AbstractUnit> units = startingUnitMap.get(difficulty);
                if (units == null) {
                    units = new HashMap<String, AbstractUnit>();
                    startingUnitMap.put(difficulty, units);
                }
                units.put(id, unit);
                in.nextTag();
            } else {
                super.readChild(in, specification);
            }
        }
    }

}
