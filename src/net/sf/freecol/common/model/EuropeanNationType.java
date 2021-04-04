/**
 *  Copyright (C) 2002-2021   The FreeCol Team
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

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.option.GameOptions;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Represents one of the European nations present in the game,
 * i.e. both REFs and possible human players.
 */
public class EuropeanNationType extends NationType {

    public static final String TAG = "european-nation-type";
    private static final String DEFAULT_MAP_KEY = "default";
    private static final String EXPERT_MAP_KEY = "expert";

    /** Whether this is an REF Nation. */
    private boolean ref = false;

    /**
     * Stores the starting units of this Nation at various
     * difficulties.
     */
    private final Map<String, Map<String, AbstractUnit>> startingUnitMap = new HashMap<>();


    /**
     * Create a new European nation type.
     *
     * @param id The object identifier.
     * @param specification The {@code Specification} to refer to.
     */
    public EuropeanNationType(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the starting unit map.
     *
     * @return The map of national starting units by difficulty.
     */
    protected Map<String, Map<String, AbstractUnit>> getStartingUnitMap() {
        return this.startingUnitMap;
    }

    /**
     * Set the starting unit map.
     *
     * @param startingUnitMap The new map of national starting units.
     */
    protected void setStartingUnitMap(Map<String, Map<String, AbstractUnit>> startingUnitMap) {
        this.startingUnitMap.clear();
        this.startingUnitMap.putAll(startingUnitMap);
    }

    /**
     * Get the map key, either expert or default, according to a boolean.
     *
     * @param b The boolean to test.
     * @return The map key.
     */
    private String getMapKey(boolean b) {
        return (b) ? EXPERT_MAP_KEY : DEFAULT_MAP_KEY;
    }
    
    /**
     * Gets the starting units for this nation type.
     *
     * @return A list of {@code AbstractUnit}s to start with.
     */
    public List<AbstractUnit> getStartingUnits() {
        boolean ex = getSpecification().getBoolean(GameOptions.EXPERT_STARTING_UNITS);
        return getStartingUnits(getMapKey(ex));
    }

    /**
     * Gets a list of this Nation's starting units at the given
     * difficulty.
     *
     * @param key The value of the expert-starting-units field.
     * @return A list of {@code AbstractUnit}s to start with.
     */
    public List<AbstractUnit> getStartingUnits(String key) {
        Map<String, AbstractUnit> result = new HashMap<>();
        Map<String, AbstractUnit> defaultMap
            = startingUnitMap.get(DEFAULT_MAP_KEY);
        Map<String, AbstractUnit> difficultyMap = startingUnitMap.get(key);
        if (defaultMap != null) {
            result.putAll(defaultMap);
        }
        if (difficultyMap != null) {
            result.putAll(difficultyMap);
        }
        return new ArrayList<>(result.values());
    }

    /**
     * Add a starting unit.
     *
     * @param id The unit identifier.
     * @param unit The {@code AbstractUnit} to add.
     * @param expert Is this an expert unit?
     */
    private void addStartingUnit(String id, AbstractUnit unit, boolean expert) {
        String mapTag = getMapKey(expert);
        Map<String, AbstractUnit> units = startingUnitMap.get(mapTag);
        if (units == null) {
            units = new HashMap<>();
            startingUnitMap.put(mapTag, units);
        }
        units.put(id, unit);
    }


    // Override NationType

    /**
     * Is this a REF nation type?
     *
     * @return True if this is a REF nation type.
     */
    @Override
    public final boolean isREF() {
        return ref;
    }

    /**
     * Is this a European nation type?
     *
     * @return True.
     */
    @Override
    public boolean isEuropean() {
        return true;
    }

    /**
     * Is this a native nation type?
     *
     * @return False.
     */
    @Override
    public boolean isIndian() {
        return false;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends FreeColObject> boolean copyIn(T other) {
        EuropeanNationType o = copyInCast(other, EuropeanNationType.class);
        if (o == null || !super.copyIn(o)) return false;
        this.setStartingUnitMap(o.getStartingUnitMap());
        return true;
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
            if ((map = startingUnitMap.get(DEFAULT_MAP_KEY)) != null) {
                for (Map.Entry<String, AbstractUnit> entry : map.entrySet()) {
                    writeUnit(xw, entry.getKey(), entry.getValue(), false);
                }
            }
            // expert map
            if ((map = startingUnitMap.get(EXPERT_MAP_KEY)) != null) {
                for (Map.Entry<String, AbstractUnit> entry : map.entrySet()) {
                    writeUnit(xw, entry.getKey(), entry.getValue(), true);
                }
            }
        }
    }

    private void writeUnit(FreeColXMLWriter xw, String id,
                           AbstractUnit au,
                           boolean expert) throws XMLStreamException {
        xw.writeStartElement(UNIT_TAG);

        xw.writeAttribute(ID_ATTRIBUTE_TAG, id);

        xw.writeAttribute(TYPE_TAG, au);

        xw.writeAttribute(ROLE_TAG, au.getRoleId());

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
            forEachMapEntry(parent.startingUnitMap, e ->
                startingUnitMap.put(e.getKey(), new HashMap<>(e.getValue())));
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

            String roleId = xr.getAttribute(ROLE_TAG,
                                            Specification.DEFAULT_ROLE_ID);

            boolean ex = xr.getAttribute(EXPERT_STARTING_UNITS_TAG, false);

            addStartingUnit(id, new AbstractUnit(type, roleId, 1), ex);
            xr.closeTag(UNIT_TAG);

        } else {
            super.readChild(xr);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getXMLTagName() { return TAG; }
}
