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
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.client.gui.i18n.Messages;

/**
 * A named region on the map.
 */
public class Region extends FreeColObject implements Nameable {

    /**
     * The name of this Region.
     */
    private String name;

    /**
     * The size of this Region (number of Tiles).
     */
    private int size;

    /**
     * The parent Region of this Region.
     */
    private Region parent;

    /**
     * Whether this Region is claimable. Ocean Regions and non-leaf
     * Regions should not be claimable.
     */
    private boolean claimable;

    /**
     * Whether this Region is discoverable. The Eastern Ocean regions
     * should not be discoverable.
     */
    private boolean discoverable;

    /**
     * The children Regions of this Region.
     */
    private List<Region> children;


    /**
     * Creates a new <code>Region</code> instance.
     *
     * @param id a <code>String</code> value
     * @param name a <code>String</code> value
     * @param parent a <code>Region</code> value
     */
    public Region(String id, String name, Region parent) {
        setId(id);
        this.name = name;
        this.parent = parent;
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
     * Returns the name or default name of this Region.
     *
     * @return a <code>String</code> value
     */
    public String getDisplayName() {
        if (name == null) {
            return Messages.message(getId());
        } else {
            return name;
        }
    }

    /**
     * Get the <code>Size</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getSize() {
        return size;
    }

    /**
     * Set the <code>Size</code> value.
     *
     * @param newSize The new Size value.
     */
    public final void setSize(final int newSize) {
        this.size = newSize;
    }

    /**
     * Get the <code>Parent</code> value.
     *
     * @return a <code>Region</code> value
     */
    public final Region getParent() {
        return parent;
    }

    /**
     * Set the <code>Parent</code> value.
     *
     * @param newParent The new Parent value.
     */
    public final void setParent(final Region newParent) {
        this.parent = newParent;
    }

    /**
     * Get the <code>Children</code> value.
     *
     * @return a <code>List<Region></code> value
     */
    public final List<Region> getChildren() {
        return children;
    }

    /**
     * Set the <code>Children</code> value.
     *
     * @param newChildren The new Children value.
     */
    public final void setChildren(final List<Region> newChildren) {
        this.children = newChildren;
    }

    /**
     * Get the <code>Claimable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isClaimable() {
        return claimable;
    }

    /**
     * Set the <code>Claimable</code> value.
     *
     * @param newClaimable The new Claimable value.
     */
    public final void setClaimable(final boolean newClaimable) {
        this.claimable = newClaimable;
    }

    /**
     * Get the <code>Discoverable</code> value.
     *
     * @return a <code>boolean</code> value
     */
    public final boolean isDiscoverable() {
        return discoverable;
    }

    /**
     * Set the <code>Discoverable</code> value.
     *
     * @param newDiscoverable The new Discoverable value.
     */
    public final void setDiscoverable(final boolean newDiscoverable) {
        this.discoverable = newDiscoverable;
    }

    /**
     * Returns true if this is the whole map Region.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Returns true if this is a leaf node.
     *
     * @return a <code>boolean</code> value
     */
    public boolean isLeaf() {
        return children == null;
    }

    /**
     * Returns a discoverable Region or null. If this region is
     * discoverable, it is returned. If not, a discoverable parent is
     * returned, unless there is none. This is intended for
     * discovering the Pacific Ocean when discovering one of its
     * sub-Regions.
     *
     * @return a <code>Region</code> value
     */
    public Region getDiscoverableRegion() {
        if (isDiscoverable()) {
            return this;
        } else if (parent != null) {
            return parent.getDiscoverableRegion();
        } else {
            return null;
        }
    }

    /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */    
    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("ID", getId());
        if (name != null) {
            out.writeAttribute("name", name);
        }
        out.writeAttribute("size", Integer.toString(size));
        out.writeAttribute("claimable", Boolean.toString(claimable));
        out.writeAttribute("discoverable", Boolean.toString(discoverable));
        if (parent != null) {
            out.writeAttribute("parent", parent.getId());
        }
        if (children != null) {
            String[] childArray = new String[children.size()];
            for (int index = 0; index < childArray.length; index++) {
                childArray[index] = children.get(index).getId();
            }
            toArrayElement("children", childArray, out);
        }
        out.writeEndElement();
    }
    
    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        setId(in.getAttributeValue(null, "ID"));
        name = in.getAttributeValue(null, "name");
        size = getAttribute(in, "size", 0);
        claimable = getAttribute(in, "claimable", false);
        discoverable = getAttribute(in, "discoverable", false);
        in.nextTag();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    protected void readFromXMLImpl(XMLStreamReader in, Map map) throws XMLStreamException {        
        setId(in.getAttributeValue(null, "ID"));
        name = in.getAttributeValue(null, "name");
        size = getAttribute(in, "size", 0);
        claimable = getAttribute(in, "claimable", false);
        discoverable = getAttribute(in, "discoverable", false);
        String parentString = in.getAttributeValue(null, "parent");
        if (parentString != null) {
            parent = map.getRegion(parentString);
        }
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("children")) {
                String[] childArray = readFromArrayElement("children", in, new String[0]);
                children = new ArrayList<Region>();
                for (String child : childArray) {
                    children.add(map.getRegion(child));
                }
            }
        }

    }            

    /**
    * Gets the tag name of the root element representing this object.
    * @return "region".
    */
    public static String getXMLElementTagName() {
        return "region";
    }

}
