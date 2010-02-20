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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

/**
 * Represents a lost city rumour.
 */
public class LostCityRumour extends TileItem {

    /**
     * The type of the rumour. A RumourType, or null if the type has
     * not yet been determined.
     */
    private RumourType type = null;

    /**
     * The name of this rumour, or null, if it has none. Rumours such
     * as the Seven Cities of Gold and Fountains of Youth may have
     * individual names.
     */
    private String name = null;

    /** Constants describing types of Lost City Rumours. */
    public static enum RumourType {
        NO_SUCH_RUMOUR,
        BURIAL_GROUND,
        EXPEDITION_VANISHES,
        NOTHING,
        LEARN,
        TRIBAL_CHIEF,
        COLONIST,
        RUINS,
        CIBOLA,
        FOUNTAIN_OF_YOUTH
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param tile a <code>Tile</code> value
     */
    public LostCityRumour(Game game, Tile tile) {
        super(game, tile);
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param tile a <code>Tile</code> value
     * @param type a <code>RumourType</code> value
     * @param name a <code>String</code> value
     */
    public LostCityRumour(Game game, Tile tile, RumourType type, String name) {
        super(game, tile);
        this.type = type;
        this.name = name;
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param in a <code>XMLStreamReader</code> value
     * @exception XMLStreamException if an error occurs
     */
    public LostCityRumour(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Creates a new <code>LostCityRumour</code> instance.
     *
     * @param game a <code>Game</code> value
     * @param e an <code>Element</code> value
     */
    public LostCityRumour(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Get the <code>Type</code> value.
     *
     * @return a <code>RumourType</code> value
     */
    public final RumourType getType() {
        return type;
    }

    /**
     * Set the <code>Type</code> value.
     *
     * @param newType The new Type value.
     */
    public final void setType(final RumourType newType) {
        this.type = newType;
    }

    /**
     * Get the <code>Name</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getName() {
        return name;
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public final void setName(final String newName) {
        this.name = newName;
    }

    /**
     * Get the <code>ZIndex</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getZIndex() {
        return RUMOUR_ZINDEX;
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream.
     *
     * <br>
     * <br>
     *
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    @Override
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
        throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        // Add attributes:
        out.writeAttribute("ID", getId());
        out.writeAttribute("tile", getTile().getId());
        if (type != null && (showAll || toSavedGame)) {
            out.writeAttribute("type", getType().toString());
        }
        if (name != null && (showAll || toSavedGame)) {
            out.writeAttribute("name", name);
        }

        // End element:
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    @Override
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));

        tile = getFreeColGameObject(in, "tile", Tile.class);
        String typeString = getAttribute(in, "type", null);
        if (typeString != null) {
            type = Enum.valueOf(RumourType.class, typeString);
        }
        name = getAttribute(in, "name", null);

        in.nextTag();
    }

    public static String getXMLElementTagName() {
        return "lostCityRumour";
    }

}
