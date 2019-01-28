/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.common.networking;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;


/**
 * The basic message with optional objects.
 */
public abstract class ObjectMessage extends AttributeMessage {

    /** The attached FreeColObjects. */
    private final List<FreeColObject> objects = new ArrayList<>();


    /**
     * Create a new {@code ObjectMessage} of a given type.
     *
     * @param type The message type.
     */
    public ObjectMessage(String type) {
        super(type);

        this.objects.clear();
    }

    /**
     * Create a new {@code ObjectMessage} of a given type, with
     * attributes and optional objects.
     *
     * @param type The message type.
     * @param attributes A list of key,value pairs.
     */
    public ObjectMessage(String type, String... attributes) {
        super(type, attributes);

        this.objects.clear();
    }

    /**
     * Create a new {@code AttributeMessage} from a stream.
     *
     * @param type The message type.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param attributes The attributes to read.
     */
    protected ObjectMessage(String type, FreeColXMLReader xr,
                            String... attributes) {
        super(type, xr.getAttributeMap(attributes));

        this.objects.clear();
    }


    /**
     * {@inheritDoc}
     */
    protected int getChildCount() {
        return this.objects.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<FreeColObject> getChildren() {
        return this.objects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setChildren(List<? extends FreeColObject> fcos) {
        this.objects.clear();
        this.objects.addAll(fcos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T extends FreeColObject> void appendChild(T fco) {
        if (fco != null) this.objects.add(fco);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected <T extends FreeColObject> void appendChildren(Collection<T> fcos) {
        if (fcos != null) this.objects.addAll(fcos);
    }
}
