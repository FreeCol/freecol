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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.Specification;

import org.w3c.dom.Element;

/**
 * The <code>BuildQueue</code> class is intended for use as a
 * <code>ListModel</code> suitable for the <code>JList</code>
 * class. It wraps an <code>ArrayList</code> of
 * <code>BuildableType</code>s.
 *
 * @see BuildableType
 */
public class BuildQueue extends FreeColObject implements ListModel {

    private static final Logger logger = Logger.getLogger(BuildQueue.class.getName());

    public static enum Type { MIXED, UNITS, BUILDINGS }

    private final List<BuildableType> model = new ArrayList<BuildableType>();

    private final List<ListDataListener> dataListeners = new ArrayList<ListDataListener>();

    private Type type = Type.MIXED;
    private int units = 0;
    private int buildings = 0;

    /**
     * The Colony this BuildQueue belongs to.
     */
    private Colony colony;

    public BuildQueue() {
        // empty constructor
    }

    public BuildQueue(Colony colony, Object[] values) {
        this.colony = colony;
        for (Object value : values) {
            BuildableType item = (BuildableType) value;
            model.add(item);
            if (item instanceof UnitType) {
                units++;
            } else if (item instanceof BuildingType) {
                buildings++;
            }
        }
    }

    public BuildQueue(Colony colony, Iterable<BuildableType> buildableTypes) {
        this.colony = colony;
        for (BuildableType buildableType : buildableTypes) {
            model.add(buildableType);
            if (buildableType instanceof UnitType) {
                units++;
            } else if (buildableType instanceof BuildingType) {
                buildings++;
            }
        }
    }

    public BuildQueue(XMLStreamReader in) throws XMLStreamException {
        readFromXML(in);
    }
    
    public BuildQueue(Element e) {
        readFromXMLElement(e);
    }


    public BuildQueue(Type type) {
        this.type = type;
    }

    /**
     * Get the <code>Colony</code> value.
     *
     * @return a <code>Colony</code> value
     */
    public final Colony getColony() {
        return colony;
    }

    /**
     * Set the <code>Colony</code> value.
     *
     * @param newColony The new Colony value.
     */
    public final void setColony(final Colony newColony) {
        this.colony = newColony;
    }

    public Type getType() {
        return type;
    }

    public boolean acceptsUnits() {
        return (type != Type.BUILDINGS);
    }

    public boolean acceptsBuildings() {
        return (type != Type.UNITS);
    }

    public boolean hasUnits() {
        return (units > 0);
    }

    public boolean hasBuildings() {
        return (buildings > 0);
    }

    public boolean ignoresPreferredIndex() {
        return (type != Type.MIXED);
    }

    public boolean isReadOnly() {
        return (type == Type.UNITS);
    }

    public boolean isEmpty() {
        return model.isEmpty();
    }

    public Iterator<BuildableType> iterator() {
        return model.iterator();
    }

    public List<BuildableType> getBuildableTypes() {
        return model;
    }

    // ListModel
    public int getSize() {
        return model.size();
    }

    // Collection
    public int size() {
        return model.size();
    }

    // ListModel
    public Object getElementAt(int index) {
        return model.get(index);
    }

    // Collection
    public BuildableType get(int index) {
        return model.get(index);
    }

    // Other methods
    public boolean add(BuildableType item) {
        return add(model.size(), item);
    }

    public boolean addUnchecked(BuildableType item) {
        return model.add(item);
    }

    public void addUnchecked(int index, BuildableType item) {
        model.add(index, item);
    }

    public boolean add(int preferredIndex, BuildableType item) {
        if (type == Type.UNITS && item instanceof UnitType) {
            return true;
        } else if (item instanceof UnitType && acceptsUnits()  ||
                   item instanceof BuildingType && acceptsBuildings()) {
            int index = preferredIndex;
            if (item instanceof BuildingType) {
                int minimumIndex = findMinimumIndex((BuildingType) item);
                if (minimumIndex > index) {
                    index = minimumIndex;
                }
            } else if (item instanceof UnitType) {
                int minimumIndex = findMinimumIndex((UnitType) item);
                if (minimumIndex > index) {
                    index = minimumIndex;
                }
            }
            model.add(index, item);

            if (item instanceof UnitType) {
                units++;
            } else {
                buildings++;
            }
            fireContentsChanged(0, getSize());
            return true;
        } else {
            return false;
        }
    }

    public void addAll(int preferredIndex, BuildQueue buildQueue) {
        for (int index = 0; index < buildQueue.size(); index++) {
            BuildableType item = buildQueue.get(index);
            add(preferredIndex + index, item);
        }
    }

    public void clear() {
        model.clear();
        units = 0;
        buildings = 0;
        fireContentsChanged(0, getSize());
    }

    public boolean contains(BuildableType item) {
        return model.contains(item);
    }

    public BuildableType firstItem() {
        // Return the appropriate item
        return model.get(0);
    }

    public Object lastItem() {
        // Return the appropriate item
        return model.get(model.size() - 1);
    }

    public boolean remove(BuildableType item) {
        boolean removed = model.remove(item);
        if (removed) {
            fireContentsChanged(0, getSize());
            if (item instanceof UnitType) {
                units--;
            } else {
                buildings--;
            }
        }
        return removed;   
    }

    public void remove(int index) {
        BuildableType item = model.get(index);
        model.remove(index);
        if (item instanceof UnitType) {
            units--;
        } else {
            buildings--;
        }
        fireContentsChanged(0, getSize());
    }

    /**
     * Returns <code>true</code> if this build queue contains the
     * Building given.
     *
     * @param buildingType The BuildingType to search for.
     * @return Whether this build queue contains the
     * BuildingType given.
     */
    private boolean hasBuildingType(BuildingType buildingType) {
        if (model.contains(buildingType)) {
            return true;
        } else if (buildingType.getUpgradesTo() != null) {
            return hasBuildingType(buildingType.getUpgradesTo());
        } else {
            return false;
        }
    }

    /**
     * Returns the smallest index for inserting the given BuildingType.
     *
     * @param buildingType a <code>BuildingType</code> value
     * @return an <code>int</code> value
     */
    public int findMinimumIndex(BuildingType buildingType) {
        BuildingType upgradeFrom = buildingType.getUpgradesFrom();
        if (upgradeFrom == null) {
            return 0;
        } else if (colony != null) {
            Building building = colony.getBuilding(buildingType);
            if (building != null && 
                upgradeFrom.equals(building.getType())) {
                return 0;
            } else {
                return -1;
            }
        } else {
            int index = model.indexOf(upgradeFrom);
            if (index < 0) {
                return index;
            } else {
                return index + 1;
            }
        }
    }


    /**
     * Returns the smallest index for inserting the given UnitType.
     *
     * @param unitType an <code>UnitType</code> value
     * @return an <code>int</code> value
     */
    public int findMinimumIndex(UnitType unitType) {

        int index = -1;
        if (colony.hasAbility("model.ability.build", unitType)) {
            index = 0;
        } else {
            for (int newIndex = 0; newIndex < model.size(); newIndex++) {
                BuildableType buildableType = model.get(newIndex);
                if (buildableType.hasAbility("model.ability.build", unitType)) {
                    index = newIndex + 1;
                    break;
                }
            }
        }

        if (index < 0 || unitType.getAbilitiesRequired().isEmpty()) {
            return index;
        } else {
            loop: for (Entry<String, Boolean> entry : unitType.getAbilitiesRequired().entrySet()) {
                if (colony != null &&
                    colony.hasAbility(entry.getKey()) == entry.getValue()) {
                    continue loop;
                } else if (definedOnlyByBuildingType(entry.getKey())) {
                    for (int newIndex = 0; newIndex < model.size(); newIndex++) {
                        BuildableType buildableType = model.get(newIndex);
                        if (buildableType.hasAbility(entry.getKey()) == entry.getValue()) {
                            if (index < newIndex) {
                                index = newIndex;
                                continue loop;
                            }
                        }
                    }
                    System.out.println("returning index: " + index);
                    // none of the buildings has the required ability
                    return -1;
                }
            }
            if (index < 0) {
                return index;
            } else {
                return index + 1;
            }
        }
    }

    public int findMinimumIndex(BuildableType buildableType) {
        if (buildableType instanceof BuildingType) {
            return findMinimumIndex((BuildingType) buildableType);
        } else if (buildableType instanceof UnitType) {
            return findMinimumIndex((UnitType) buildableType);
        } else {
            return -1;
        }
    }


    public boolean definedOnlyByBuildingType(String id) {
        List<Ability> definedAbilities = 
            Specification.getSpecification().getAbilities(id);
        if (definedAbilities != null) {
            for (Ability ability : definedAbilities) {
                if (ability.getSource() != null &&
                    !(ability.getSource() instanceof BuildingType)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }        


    /**
     * Returns <code>true</code> if the <code>BuildQueue</code>s given
     * satisfy all dependencies of this <code>BuildQueue</code>.
     *
     * @param queues a <code>BuildQueue[]</code> value
     * @return Whether the <code>BuildQueue</code>s given satisfy all
     * dependencies of this <code>BuildQueue</code>.
     * @return a <code>boolean</code> value
     */
    public boolean dependenciesSatisfiedBy(BuildQueue... queues) {
        Set<BuildableType> items = new HashSet<BuildableType>(model);
        for (BuildQueue buildQueue : queues) {
            items.addAll(buildQueue.model);
        }

        FeatureContainer featureContainer = new FeatureContainer();
        for (BuildableType item : items) {
            featureContainer.add(item.getFeatureContainer());
        }

        for (BuildableType item : model) {
            if (item instanceof BuildingType) {
                BuildingType upgradesFrom = ((BuildingType) item).getUpgradesFrom();
                if (upgradesFrom != null && !items.contains(upgradesFrom)) {
                    return false;
                }
            } else if (item instanceof UnitType) {
                if (!featureContainer.hasAbility("model.ability.build", item)) {
                    return false;
                }
                for (Entry<String, Boolean> entry : ((UnitType) item).getAbilitiesRequired().entrySet()) {
                    if (definedOnlyByBuildingType(entry.getKey()) && 
                        featureContainer.hasAbility(entry.getKey()) != entry.getValue()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }


    public void addListDataListener(ListDataListener l) {
        dataListeners.add(l);
    }

    public void removeListDataListener(ListDataListener l) {
        dataListeners.remove(l);
    }

    public void fireContentsChanged(int firstIndex, int lastIndex) {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED,
                                                firstIndex, lastIndex);
        for (ListDataListener listener : dataListeners) {
            listener.contentsChanged(event);
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("type", type.toString());
        out.writeAttribute(ARRAY_SIZE, String.valueOf(model.size()));
        for (int index = 0; index < model.size(); index++) {
            out.writeAttribute("x" + index, model.get(index).getId());
        }
        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        type = Enum.valueOf(Type.class, in.getAttributeValue(null, "type"));
        int size = Integer.parseInt(in.getAttributeValue(null, "size"));
        for (int index = 0; index < size; index++) {
            addUnchecked((BuildableType) Specification.getSpecification()
                         .getType(in.getAttributeValue(null, "element" + index)));
        }
    }


    /**
     * Gets the tag name of the root element representing this object.
     * @return "buildQueue".
     */
    public static String getXMLElementTagName() {
        return "buildQueue";
    }

    public String toString() {
        String text = "";
        for (BuildableType item : model) {
            text += item.getName() + " ";
        }
        return text;
    }


}
