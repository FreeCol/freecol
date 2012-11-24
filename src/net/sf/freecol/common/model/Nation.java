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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {

    public static String UNKNOWN_NATION_ID = "model.nation.unknownEnemy";

    public static final String[] EUROPEAN_NATIONS = new String[] {
        // the original game's nations
        "english", "french", "spanish", "dutch",
        // FreeCol's additions
        "portuguese", "danish", "swedish", "russian",
        // other Europeans, used to generate Monarch messages
        "german", "austrian", "prussian", "turkish"
    };

    /**
     * Describe type here.
     */
    private NationType type;

    /**
     * Describe selectable here.
     */
    private boolean selectable;

    /**
     * Describe refNation here.
     */
    private Nation refNation;

    /**
     * Describe preferredLatitude here.
     */
    private int preferredLatitude = 0;

    /**
     * Describe startsOnEastCoast here.
     */
    private boolean startsOnEastCoast = true;


    public Nation(String id, Specification specification) {
        super(id, specification);
    }


    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>NationType</code> value
     */
    public final NationType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final NationType newType) {
        this.type = newType;
    }

    /**
     * Get the <code>RulerName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRulerNameKey() {
        return getId() + ".ruler";
    }

    /**
     * Get the <code>Selectable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isSelectable() {
        return selectable;
    }

    /**
     * Get the <code>RefNation</code> value.
     *
     * @return a <code>Nation</code> value
     */
    public final Nation getRefNation() {
        return refNation;
    }

    /**
     * Set the <code>RefNation</code> value.
     *
     * @param newRefNation The new RefNation value.
     */
    public final void setRefNation(final Nation newRefNation) {
        this.refNation = newRefNation;
    }

    /**
     * Set the <code>Selectable</code> value.
     *
     * @param newSelectable The new Selectable value.
     */
    public final void setSelectable(final boolean newSelectable) {
        this.selectable = newSelectable;
    }

    /**
     * Get the <code>PreferredLatitude</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getPreferredLatitude() {
        return preferredLatitude;
    }

    /**
     * Set the <code>PreferredLatitude</code> value.
     *
     * @param newPreferredLatitude The new PreferredLatitude value.
     */
    public final void setPreferredLatitude(final int newPreferredLatitude) {
        this.preferredLatitude = newPreferredLatitude;
    }

    /**
     * Get the <code>StartsOnEastCoast</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean startsOnEastCoast() {
        return startsOnEastCoast;
    }

    /**
     * Set the <code>StartsOnEastCoast</code> value.
     *
     * @param newStartsOnEastCoast The new StartsOnEastCoast value.
     */
    public final void setStartsOnEastCoast(final boolean newStartsOnEastCoast) {
        this.startsOnEastCoast = newStartsOnEastCoast;
    }

    /**
     * Makes an XML-representation of this object.
     *
     * @param out The output stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        super.toXML(out, getXMLElementTagName());
    }

    /**
     * Write the attributes of this object to a stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to
     *     the stream.
     */
    @Override
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("nation-type", type.getId());
        out.writeAttribute("selectable", Boolean.toString(selectable));
        out.writeAttribute("preferredLatitude", Integer.toString(preferredLatitude));
        out.writeAttribute("startsOnEastCoast", Boolean.toString(startsOnEastCoast));
        if (refNation != null) {
            out.writeAttribute("ref", refNation.getId());
        }
    }

    /**
     * Reads the attributes of this object from an XML stream.
     *
     * @param in The XML input stream.
     * @exception XMLStreamException if a problem was encountered
     *     during parsing.
     */
    @Override
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        type = getSpecification().getNationType(in.getAttributeValue(null,
                "nation-type"));
        selectable = getAttribute(in, "selectable", false);
        preferredLatitude = getAttribute(in, "preferredLatitude", 0);
        startsOnEastCoast = getAttribute(in, "startsOnEastCoast", true);
        String refId = getAttribute(in, "ref", (String)null);
        if (refId != null) {
            refNation = getSpecification().getNation(refId);
        }
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "nation".
     */
    public static String getXMLElementTagName() {
        return "nation";
    }
}
