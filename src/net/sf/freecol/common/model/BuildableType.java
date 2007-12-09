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

import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Contains information on buildable types.
 */
public class BuildableType extends FreeColGameObjectType {

    public static final int UNDEFINED = Integer.MIN_VALUE;

    public static final BuildableType NOTHING = new BuildableType("model.buildableType.nothing");
    
    private int hammersRequired = UNDEFINED;
    private int toolsRequired = UNDEFINED;
    private int populationRequired = 1;
    
    /**
     * Stores the abilities required by this Type.
     */
    private HashMap<String, Boolean> requiredAbilities = new HashMap<String, Boolean>();
    
    public BuildableType() {}

    private BuildableType(String id) {
        setId(id);
    }

    /**
     * Get the <code>HammersRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getHammersRequired() {
        return hammersRequired;
    }

    /**
     * Set the <code>HammersRequired</code> value.
     *
     * @param newHammersRequired The new HammersRequired value.
     */
    public void setHammersRequired(final int newHammersRequired) {
        this.hammersRequired = newHammersRequired;
    }

    /**
     * Get the <code>ToolsRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getToolsRequired() {
        return toolsRequired;
    }

    /**
     * Set the <code>ToolsRequired</code> value.
     *
     * @param newToolsRequired The new ToolsRequired value.
     */
    public void setToolsRequired(final int newToolsRequired) {
        this.toolsRequired = newToolsRequired;
    }

    /**
     * Get the <code>PopulationRequired</code> value.
     *
     * @return an <code>int</code> value
     */
    public int getPopulationRequired() {
        return populationRequired;
    }

    /**
     * Set the <code>PopulationRequired</code> value.
     *
     * @param newPopulationRequired The new PopulationRequired value.
     */
    public void setPopulationRequired(final int newPopulationRequired) {
        this.populationRequired = newPopulationRequired;
    }

    /**
     * Returns the abilities required by this Type.
     *
     * @return the abilities required by this Type.
     */
    public Map<String, Boolean> getAbilitiesRequired() {
        return requiredAbilities;
    }

    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
    }
}
