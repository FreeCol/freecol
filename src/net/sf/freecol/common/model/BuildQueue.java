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
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
//import net.sf.freecol.client.gui.i18n.Messages;

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

    public BuildQueue() {
    }

    public BuildQueue(Object[] values) {
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

    public BuildQueue(BuildableType... buildableTypes) {
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

    public Iterator iterator() {
        return model.iterator();
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
	    int index = -1;
            /*
	    if (type == Type.MIXED) {
		index = indexOf(item.getRequiredType(),
				item.getRequiredLevel()) + 1;
		if (preferredIndex > index) {
		    index = preferredIndex;
		}
	    } else if (type == Type.BUILDINGS) {
		index = findIndex(item.getRequiredType(),
				  item.getRequiredLevel());
	    }
            */
	    try {
		model.add(index, item);
	    } catch(Exception e) {
		System.out.println(e);
	    }
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
            BuildableType item = (BuildableType) buildQueue.get(index);
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
     * Finds a suitable index for inserting the given
     * <code>BuildableType</code>.
     * @param newItem The item to be inserted.
     * @return A suitable index for inserting the given
     * <code>BuildableType</code>.
     */
    /*
    public int findIndex(int type, int level) {
	for (int index = 0; index < model.size(); index++) {
	    BuildableType item = model.get(index);
	    if (type < item.getType() ||
		type == item.getType() && level < item.getLevel()) {
		return index;
	    }
	}
	return model.size();
    }
    */

    /**
     * Returns <code>true</code> if all requirements of the
     * <code>BuildQueue</code> given are satisfied by this build
     * queue.
     * @param buildQueue The build queue to check.
     * @return Whether all requirements of the <code>BuildQueue</code>
     * given are satisfied by this build queue.
     */
    /*
    public boolean canAdd(BuildQueue buildQueue) {
	Iterator iterator = buildQueue.iterator();
	while (iterator.hasNext()) {
	    BuildableType item = (BuildableType) iterator.next();
	    if (item instanceof UnitType && !acceptsUnits()  ||
		item.isBuilding() && !acceptsBuildings()) {
		return false;
	    } else if (type == MIXED) {
		int type = item.getRequiredType();
		int level = item.getRequiredLevel();
		if (!hasBuilding(type, level) &&
		    !buildQueue.hasBuilding(type, level)) {
		    return false;
		}
	    }
	}
	return true;
    }
    */

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
        itemLoop: for (BuildableType item : model) {
            if (item instanceof BuildingType) {
                if (hasBuildingType((BuildingType) item)) {
                    continue itemLoop;
                } else {
                    for (BuildQueue buildQueue : queues) {
                        if (buildQueue.hasBuildingType((BuildingType) item)) {
                            continue itemLoop;
                        }
                    }
                    return false;
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
            addUnchecked((BuildableType) FreeCol.getSpecification()
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
}
