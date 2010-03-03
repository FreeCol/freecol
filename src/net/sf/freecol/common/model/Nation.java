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

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;


/**
 * Represents one of the nations present in the game.
 */
public class Nation extends FreeColGameObjectType {
	
	static public String UNKNOWN_NATION_ID = "model.nation.unknownEnemy";

    private static int nextIndex = 0;

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
     * Describe anthem here.
     */
    private String anthem;

    /**
     * Describe refNation here.
     */
    private Nation refNation;

    /**
     * Creates a new Nation instance.
     */
    public Nation() {
        setIndex(nextIndex++);
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

    public void readAttributes(XMLStreamReader in, Specification specification)
        throws XMLStreamException {
        setColor(new Color(Integer.decode(in.getAttributeValue(null, "color"))));
        type = specification.getNationType(in.getAttributeValue(null, "nation-type"));
        selectable = getAttribute(in, "selectable", false);
        String refId = getAttribute(in, "ref", null);
        if (refId != null) {
            refNation = specification.getNation(refId);
        }
        anthem = in.getAttributeValue(null, "anthem");
   }


}
