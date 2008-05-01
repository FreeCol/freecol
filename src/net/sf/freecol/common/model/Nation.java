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

import java.awt.Color;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.Specification;
import net.sf.freecol.client.gui.i18n.Messages;

/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {


    /**
     * The default color to use for this nation. Can be changed by the
     * Player.
     */
    private Color color;

    /**
     * Describe type here.
     */
    private NationType type;

    /**
     * Describe selectable here.
     */
    private boolean selectable;

    /**
     * Describe classic here.
     */
    private boolean classic;

    /**
     * Describe refID here.
     */
    private String refID;

    /**
     * Describe coatOfArms here.
     */
    private String coatOfArms;

    /**
     * Describe monarch-art here.
     */
    private String monarchArt;

    /**
     * Describe anthem here.
     */
    private String anthem;


    /**
     * Sole constructor.
     */
    public Nation(int index) {
        setIndex(index);
    }


    /**
     * Get the <code>Color</code> value.
     *
     * @return a <code>Color</code> value
     */
    public final Color getColor() {
        return color;
    }

    /**
     * Set the <code>Color</code> value.
     *
     * @param newColor The new Color value.
     */
    public final void setColor(final Color newColor) {
        this.color = newColor;
    }

    /**
     * Get the <code>CoatOfArms</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getCoatOfArms() {
        return coatOfArms;
    }

    /**
     * Set the <code>CoatOfArms</code> value.
     *
     * @param newCoatOfArms The new CoatOfArms value.
     */
    public final void setCoatOfArms(final String newCoatOfArms) {
        this.coatOfArms = newCoatOfArms;
    }

    /**
     * Get the <code>Monarch-Art</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getMonarchArt() {
        return monarchArt;
    }

    /**
     * Set the <code>MonarchArt</code> value.
     *
     * @param newMonarchArt The new Monarch art value.
     */
    public final void setMonarchArt(final String newMonarchArt) {
        this.monarchArt = newMonarchArt;
    }

    /**
     * Get the <code>Anthem</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getAnthem() {
        return anthem;
    }

    /**
     * Set the <code>Anthem</code> value.
     *
     * @param newAnthem The new Anthem value.
     */
    public final void setAnthem(final String newAnthem) {
        this.anthem = newAnthem;
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
    public final String getRulerName() {
        return Messages.message(getId() + ".ruler");
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
     * Get the <code>RefID</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRefID() {
        return refID;
    }

    /**
     * Set the <code>RefID</code> value.
     *
     * @param newRefID The new RefID value.
     */
    public final void setRefID(final String newRefID) {
        this.refID = newRefID;
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
     * Get the <code>Classic</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isClassic() {
        return classic;
    }

    /**
     * Set the <code>Classic</code> value.
     *
     * @param newClassic The new Classic value.
     */
    public final void setClassic(final boolean newClassic) {
        this.classic = newClassic;
    }

    public String toString() {
        return getName();
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        throw new UnsupportedOperationException("Call 'readFromXML' instead.");
    }

    public void readFromXML(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        setId(in.getAttributeValue(null, "id"));
        setColor(new Color(Integer.decode(in.getAttributeValue(null, "color"))));
        type = specification.getNationType(in.getAttributeValue(null, "nation-type"));
        selectable = getAttribute(in, "selectable", false);
        classic = getAttribute(in, "classic", false);
        refID = getAttribute(in, "ref-of", null);
        coatOfArms = getAttribute(in, "coat-of-arms", "native.png");
        monarchArt = getAttribute(in, "monarch-art", "louis.png");
        anthem = in.getAttributeValue(null, "anthem");
        in.nextTag();
   }


}
