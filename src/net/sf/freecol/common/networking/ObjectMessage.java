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

import net.sf.freecol.common.util.DOMUtils;

import org.w3c.dom.Element;


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
     * Create a new {@code ObjectMessage} from a supplied element.
     *
     * @param game The {@code Game} this message belongs to.
     * @param element The {@code Element} to use to create the message.
     */
    public ObjectMessage(Game game, Element element) {
        this(element.getTagName());

        // Do not read the element structure, the subclasses must do that
        // because they define whether to use interning reads or not.
    }

    /**
     * Create a new {@code AttributeMessage} from a stream.
     *
     * @param type The message type.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @param attributes The attributes to read.
     * @exception XMLStreamException if the stream is corrupt.
     * @exception FreeColException if the internal message can not be read.
     */
    protected ObjectMessage(String type, FreeColXMLReader xr,
                            String... attributes) {
        super(type, xr.getAttributeMap(attributes));

        this.objects.clear();
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
    public void writeChildren(FreeColXMLWriter xw) throws XMLStreamException {
        for (FreeColObject fco : getChildren()) {
            if (fco != null) fco.toXML(xw);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Element toXMLElement() {
        return DOMUtils.createElement(getType(), getStringAttributes(),
                                      getChildren());
    }


    /**
     * Get a child object.
     *
     * @param T The actual class of {@code FreeColObject} to get.
     * @param index The index of the child to get.
     * @param returnClass The expected class of child.
     * @return The child object found, or null if the index is invalid or
     *     return class incorrect.
     */
    protected <T extends FreeColObject> T getChild(int index,
                                                   Class<T> returnClass) {
        if (index >= this.objects.size()) return (T)null;
        FreeColObject fco = this.objects.get(index);
        try {
            return returnClass.cast(fco);
        } catch (ClassCastException cce) {
            logger.log(Level.WARNING, "Cast fail", cce);
            return null;
        }
    }

    /**
     * Get the child objects.
     *
     * @param T The actual class of {@code FreeColObject} to get.
     * @param returnClass The expected class of children.
     * @return The children with the expected class.
     */
    protected <T extends FreeColObject> List<T> getChildren(Class<T> returnClass) {
        List<T> ret = new ArrayList<>();
        for (FreeColObject fco : this.objects) {
            try {
                ret.add(returnClass.cast(fco));
            } catch (ClassCastException cce) {}
        }
        return ret;
    }
            
    /**
     * Set a child object.
     *
     * @param T The actual class of {@code FreeColObject} to set.
     * @param index The index of the child to set.
     * @param fco The new child object.
     */
    protected <T extends FreeColObject> void setChild(int index, T fco) {
        if (index < this.objects.size()) this.objects.set(index, fco);
    }

    /**
     * Add another child object.
     *
     * @param T The actual class of {@code FreeColObject} to add.
     * @param fco The {@code FreeColObject} to add.
     */
    protected <T extends FreeColObject> void add1(T fco) {
        if (fco != null) this.objects.add(fco);
    }

    /**
     * Add many child objects.
     *
     * @param T The actual class of {@code FreeColObject} to add.
     * @param fcos A collection of {@code FreeColObject}s to add.
     */
    protected <T extends FreeColObject> void addAll(Collection<T> fcos) {
        this.objects.addAll(fcos);
    }

    /**
     * Complain about finding the wrong XML element.
     *
     * @param wanted The tag we wanted.
     * @param got The tag we got.
     * @exception XMLStreamException is always thrown.
     */
    protected void expected(String wanted, String got)
        throws XMLStreamException {
        throw new XMLStreamException("In " + getClass().getName()
            + ", expected " + wanted + " but read: " + got);
    }
}
