/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Represents one of the European nations present in the game,
 * i.e. both REFs and possible human players.
 */
public class EuropeanNationType extends NationType {

    /** Whether this is an REF Nation. */
    private boolean ref = false;

    /** Stores the starting units of this Nation. */
    private List<AbstractUnit> startingUnits = null;

    /**
     * Stores the starting units of this Nation at various
     * difficulties.
     */
    private Map<String, Map<String, AbstractUnit>> startingUnitMap
        = new HashMap<String, Map<String, AbstractUnit>>();

    /** Always using expert starting units. */
    private final boolean expert = true;


    /**
     * Create a new European nation type.
     *
     * @param id The object identifier.
     * @param specification The <code>Specification</code> to refer to.
     */
    public EuropeanNationType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Is this a REF nation type?
     *
     * @return True if this is a REF nation type.
     */
    public final boolean isREF() {
        return ref;
    }

    /**
     * Is this a European nation type?
     *
     * @return True.
     */
    public boolean isEuropean() {
        return true;
    }

    /**
     * Is this a native nation type?
     *
     * @return False.
     */
    public boolean isIndian() {
        return false;
    }

    /**
     * Gets the starting units for this nation type.
     *
     * @return A list of <code>AbstractUnit</code>s to start with.
     */
    public List<AbstractUnit> getStartingUnits() {
        if (startingUnits == null) return Collections.emptyList();
        return startingUnits;
    }

    /**
     * Gets a list of this Nation's starting units at the given
     * difficulty.
     *
     * @param key The value of the expert-starting-units field.
     * @return A list of <code>AbstractUnit</code>s to start with.
     */
    public List<AbstractUnit> getStartingUnits(String key) {
        Map<String, AbstractUnit> result = new HashMap<String, AbstractUnit>();
        Map<String, AbstractUnit> defaultMap = startingUnitMap.get(null);
        Map<String, AbstractUnit> difficultyMap = startingUnitMap.get(key);
        if (defaultMap != null) {
            result.putAll(defaultMap);
        }
        if (difficultyMap != null) {
            result.putAll(difficultyMap);
        }
        return new ArrayList<AbstractUnit>(result.values());
    }

    /**
     * Add a starting unit.
     *
     * @param id The unit identifier.
     * @param unit The <code>AbstractUnit</code> to add.
     * @param expert Is this an expert unit?
     */
    private void addStartingUnit(String id, AbstractUnit unit, boolean expert) {
        String exTag = (expert) ? Boolean.TRUE.toString() : null;
        Map<String, AbstractUnit> units = startingUnitMap.get(exTag);
        if (units == null) {
            units = new HashMap<String, AbstractUnit>();
            startingUnitMap.put(exTag, units);
        }
        units.put(id, unit);
    }
        
    /**
     * Applies the difficulty level to this nation type.
     *
     * @param difficulty difficulty level to apply
     */
    @Override
    public void applyDifficultyLevel(OptionGroup difficulty) {
        boolean ex = difficulty.getBoolean("model.option.expertStartingUnits");
        startingUnits = getStartingUnits(String.valueOf(ex));
    }


    // Serialization

    private static final String EXPERT_STARTING_UNITS_TAG = "expert-starting-units";
    private static final String REF_TAG = "ref";
    private static final String ROLE_TAG = "role";
    private static final String TYPE_TAG = "type";
    private static final String UNIT_TAG = "unit";


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeAttributes(xw);

        xw.writeAttribute(REF_TAG, ref);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        super.writeChildren(xw);

        if (startingUnitMap != null && !startingUnitMap.isEmpty()) {
            Map<String, AbstractUnit> map;
            // default map
            if ((map = startingUnitMap.get(null)) != null) {
                for (Map.Entry<String, AbstractUnit> entry : map.entrySet()) {
                    writeUnit(xw, entry.getKey(), entry.getValue(), false);
                }
            }
            // expert map
            if ((map = startingUnitMap.get(Boolean.TRUE.toString())) != null) {
                for (Map.Entry<String, AbstractUnit> entry : map.entrySet()) {
                    writeUnit(xw, entry.getKey(), entry.getValue(), true);
                }
            }
        }
    }

    private void writeUnit(FreeColXMLWriter xw, String id,
                           AbstractUnit unit,
                           boolean expert) throws XMLStreamException {
        xw.writeStartElement(UNIT_TAG);

        xw.writeAttribute(ID_ATTRIBUTE_TAG, id);

        xw.writeAttribute(TYPE_TAG, unit);

        xw.writeAttribute(ROLE_TAG, unit.getRole());

        //xw.writeAttribute("number", unit.getNumber());

        if (expert) xw.writeAttribute(EXPERT_STARTING_UNITS_TAG, expert);

        xw.writeEndElement();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        super.readAttributes(xr);

        final Specification spec = getSpecification();

        EuropeanNationType parent = xr.getType(spec, EXTENDS_TAG,
                                               EuropeanNationType.class, this);
        
        ref = xr.getAttribute(REF_TAG, parent.ref);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChildren(FreeColXMLReader xr) throws XMLStreamException {
        // Clear containers.
        if (xr.shouldClearContainers()) {
            startingUnitMap.clear();
        }

        final Specification spec = getSpecification();
        EuropeanNationType parent = xr.getType(spec, EXTENDS_TAG,
                                               EuropeanNationType.class, this);
        if (parent != this) {
            for (Map.Entry<String, Map<String, AbstractUnit>> entry
                     : parent.startingUnitMap.entrySet()) {
                startingUnitMap.put(entry.getKey(),
                    new HashMap<String, AbstractUnit>(entry.getValue()));
            }
        }

        super.readChildren(xr);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readChild(FreeColXMLReader xr) throws XMLStreamException {
        final String tag = xr.getLocalName();

        if (UNIT_TAG.equals(tag)) {
            String id = xr.readId();

            String type = xr.getAttribute(TYPE_TAG, (String)null);

            Role role = xr.getAttribute(ROLE_TAG, Role.class, Role.DEFAULT);
            
            boolean ex = xr.getAttribute(EXPERT_STARTING_UNITS_TAG, false);

            addStartingUnit(id, new AbstractUnit(type, role, 1), ex);
            xr.closeTag(UNIT_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "european-nation-type".
     */
    public static String getXMLElementTagName() {
        return "european-nation-type";
    }
}
