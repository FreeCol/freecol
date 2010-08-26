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
import java.util.Locale;
import java.util.Map;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Settlement.SettlementType;
import net.sf.freecol.common.model.Unit.Role;
import net.sf.freecol.common.option.BooleanOption;

/**
 * Represents one of the European nations present in the game, i.e. both REFs
 * and possible human players.
 */
public class EuropeanNationType extends NationType {

    /**
     * Whether this is an REF Nation.
     */
    private boolean ref = false;

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



    public EuropeanNationType(String id, Specification specification) {
        super(id, specification);
        setTypeOfSettlement(SettlementType.SMALL_COLONY);
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
     * @param key the value of the expert-starting-units field
     * @return a list of this Nation's starting units.
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
     * Applies the difficulty level to this nation type.
     *
     * @param difficulty difficulty level to apply
     */
    @Override
    public void applyDifficultyLevel(DifficultyLevel difficulty) {
        String experts = Boolean.toString(((BooleanOption) difficulty.getOption("model.option.expertStartingUnits"))
                                          .getValue());
        startingUnits = getStartingUnits(experts);
    }


    public void readAttributes(XMLStreamReader in) throws XMLStreamException {
        String extendString = in.getAttributeValue(null, "extends");
        EuropeanNationType parent = (extendString == null) ? this :
            (EuropeanNationType) getSpecification().getNationType(extendString);
        ref = getAttribute(in, "ref", parent.ref);

        if (parent != this) {
            for (Map.Entry<String,Map<String, AbstractUnit>> entry : parent.startingUnitMap.entrySet()) {
                startingUnitMap.put(entry.getKey(), new HashMap<String, AbstractUnit>(entry.getValue()));
            }
            getFeatureContainer().add(parent.getFeatureContainer());
            if (parent.isAbstractType()) {
                getFeatureContainer().replaceSource(parent, this);
            }
        }
    }

    public void readChild(XMLStreamReader in) throws XMLStreamException {
        String childName = in.getLocalName();
        if ("unit".equals(childName)) {
            String id = in.getAttributeValue(null, ID_ATTRIBUTE_TAG);
            String type = in.getAttributeValue(null, "type");
            Role role = Enum.valueOf(Role.class, getAttribute(in, "role", "default").toUpperCase(Locale.US));
            String useExperts = in.getAttributeValue(null, "expert-starting-units");
            AbstractUnit unit = new AbstractUnit(type, role, 1);
            Map<String, AbstractUnit> units = startingUnitMap.get(useExperts);
            if (units == null) {
                units = new HashMap<String, AbstractUnit>();
                startingUnitMap.put(useExperts, units);
            }
            units.put(id, unit);
            in.nextTag();
        } else {
            super.readChild(in);
        }
    }

    /**
     * Makes an XML-representation of this object.
     * 
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXMLImpl(out, getXMLElementTagName());
    }

    public void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        out.writeAttribute("ref", Boolean.toString(ref));
    }

    protected void writeChildren(XMLStreamWriter out) throws XMLStreamException {
        super.writeChildren(out);

        if (startingUnitMap != null && !startingUnitMap.isEmpty()) {
            // default map
            for (Map.Entry<String, AbstractUnit> entry : startingUnitMap.get(null).entrySet()) {
                writeUnit(out, entry.getKey(), entry.getValue(), false);
            }
            // expert map
            for (Map.Entry<String, AbstractUnit> entry : startingUnitMap.get("true").entrySet()) {
                writeUnit(out, entry.getKey(), entry.getValue(), true);
            }
        }
    }

    protected void writeUnit(XMLStreamWriter out, String id, AbstractUnit unit, boolean expert)
        throws XMLStreamException {
        out.writeStartElement("unit");
        out.writeAttribute(ID_ATTRIBUTE_TAG, id);
        out.writeAttribute("type", unit.getId());
        out.writeAttribute("role", unit.getRole().toString().toLowerCase(Locale.US));
        //out.writeAttribute("number", String.valueOf(unit.getNumber()));
        if (expert) {
            out.writeAttribute("expert-starting-units", "true");
        }
        out.writeEndElement();
    }


    public static String getXMLElementTagName() {
        return "european-nation-type";
    }


}
