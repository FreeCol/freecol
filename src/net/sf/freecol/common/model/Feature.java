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
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * The <code>Feature</code> class encapsulates a bonus or penalty
 * that can be applied to any action within the game, most obviously
 * combat.
 */
public abstract class Feature extends FreeColObject {

    /**
     * The category of this Feature, e.g. "offenseModifier".
     */
    private String source;

    /**
     * Describe scopes here.
     */
    private List<Scope> scopes = new ArrayList<Scope>();


    public Feature() {
    }

    /**
     * Get the <code>Scopes</code> value.
     *
     * @return a <code>List<Scope></code> value
     */
    public final List<Scope> getScopes() {
        return scopes;
    }

    /**
     * Set the <code>Scopes</code> value.
     *
     * @param newScopes The new Scopes value.
     */
    public final void setScopes(final List<Scope> newScopes) {
        this.scopes = newScopes;
    }

    /**
     * Get the <code>Source</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getSource() {
        return source;
    }

    /**
     * Set the <code>Source</code> value.
     *
     * @param newSource The new Source value.
     */
    public final void setSource(final String newSource) {
        this.source = newSource;
    }

    /**
     * Returns true if the <code>appliesTo</code> method of at least
     * one <code>Scope</code> object returns true.
     *
     * @param objectType a <code>FreeColGameObjectType</code> value
     * @return a <code>boolean</code> value
     */
    public boolean appliesTo(FreeColGameObjectType objectType) {
        if (scopes.size() == 0) {
            return true;
        }
        for (Scope scope : scopes) {
            if (scope.appliesTo(objectType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     *
     * Classes extending this class must write attributes before this method
     * is called, and must write children after this method is called.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeAttribute("id", getId());
        if (getSource() != null) {
            out.writeAttribute("source", source);
        }
        for (Scope scope : scopes) {
            scope.toXMLImpl(out);
        }
    }
}
