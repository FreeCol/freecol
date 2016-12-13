/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.server.FreeColServer;

import net.sf.freecol.common.util.DOMUtils;

import org.w3c.dom.Element;


/**
 * The basic message with optional objects.
 */
public class ObjectMessage extends AttributeMessage {

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
     * @param attributes A map of key,value pairs.
     */
    public ObjectMessage(String type, String... attributes) {
        super(type, attributes);
    }

    /**
     * Create a new {@code ObjectMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ObjectMessage(Game game, Element element) {
        this(element.getTagName());

        this.objects.addAll(DOMUtils.getChildren(game, element));
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<FreeColObject> getChildren() {
        return this.objects;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setChildren(List<? extends FreeColObject> fcos) {
        this.objects.clear();
        if (objects != null) this.objects.addAll(fcos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return DOMUtils.createElement(getType(), getStringAttributes(),
                                      getChildren());
    }
}
