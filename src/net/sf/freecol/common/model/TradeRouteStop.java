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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;


public class TradeRouteStop {

    private static final Logger logger = Logger.getLogger(TradeRouteStop.class.getName());

    /**
     * The location of the stop.
     */
    private Location location;

    /**
     * Whether the stop has been modified. This is of interest only to the
     * client and can be ignored for XML serialization.
     */
    private boolean modified = false;

    /**
     * The AbstractGoods to unload in this Location.
     */
    private List<AbstractGoods> goodsToUnload;

    /**
     * The AbstractGoods to load in this Location.
     */
    private List<AbstractGoods> goodsToLoad;


    /**
     * Creates a new <code>TradeRouteStop</code> instance.
     *
     * @param location a <code>Location</code> value
     */
    public TradeRouteStop(Location location) {
        this.location = location;
    }

    /**
     * Get the <code>GoodsToLoad</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getGoodsToLoad() {
        return goodsToLoad;
    }

    /**
     * Set the <code>GoodsToLoad</code> value.
     *
     * @param newGoodsToLoad The new GoodsToLoad value.
     */
    public final void setGoodsToLoad(final List<AbstractGoods> newGoodsToLoad) {
        this.goodsToLoad = newGoodsToLoad;
    }

    /**
     * Get the <code>GoodsToUnload</code> value.
     *
     * @return a <code>List<AbstractGoods></code> value
     */
    public final List<AbstractGoods> getGoodsToUnload() {
        return goodsToUnload;
    }

    /**
     * Set the <code>GoodsToUnload</code> value.
     *
     * @param newGoodsToUnload The new GoodsToUnload value.
     */
    public final void setGoodsToUnload(final List<AbstractGoods> newGoodsToUnload) {
        this.goodsToUnload = newGoodsToUnload;
    }

    /**
     * Get the <code>Modified</code> value.
     * 
     * @return a <code>boolean</code> value
     */
    public final boolean isModified() {
        return modified;
    }

    /**
     * Set the <code>Modified</code> value.
     * 
     * @param newModified The new Modified value.
     */
    public final void setModified(final boolean newModified) {
        this.modified = newModified;
    }

    /**
     * Get the <code>Location</code> value.
     * 
     * @return a <code>Location</code> value
     */
    public final Location getLocation() {
        return location;
    }

    /**
     * Set the <code>Location</code> value.
     *
     * @param newLocation a <code>Location</code> value
     */
    public void setLocation(Location newLocation) {
        this.location = newLocation;
    }

    public void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("location", this.location.getId());
        if (goodsToUnload != null) {
            out.writeStartElement("goodsToUnload");
            for (AbstractGoods goods : goodsToUnload) {
                goods.toXML(out);
            }
            out.writeEndElement();
        }
        if (goodsToLoad != null) {
            out.writeStartElement("goodsToLoad");
            for (AbstractGoods goods : goodsToLoad) {
                goods.toXML(out);
            }
            out.writeEndElement();
        }
        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException is thrown if something goes wrong.
     */
    public void readFromXML(XMLStreamReader in)
        throws XMLStreamException {
        readFromXML(in, null);
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * 
     * @param in The input stream with the XML.
     * @throws XMLStreamException is thrown if something goes wrong.
     */
    public void readFromXML(XMLStreamReader in, Game game)
        throws XMLStreamException {
        if (game != null) {
            String str = in.getAttributeValue(null, "location");
            location = game.makeFreeColLocation(str);
        }

        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals("goodsToUnload")) {
                goodsToUnload = new ArrayList<AbstractGoods>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(AbstractGoods.getXMLElementTagName())) {
                        AbstractGoods goods = new AbstractGoods();
                        goods.readFromXML(in);
                        goodsToUnload.add(goods);
                    }
                }
            } else if (in.getLocalName().equals("goodsToLoad")) {
                goodsToLoad = new ArrayList<AbstractGoods>();
                while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    if (in.getLocalName().equals(AbstractGoods.getXMLElementTagName())) {
                        AbstractGoods goods = new AbstractGoods();
                        goods.readFromXML(in);
                        goodsToLoad.add(goods);
                    }
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return (location == null) ? "" : location.getLocationName().getId();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * 
     * @return "tradeRoute".
     */
    public static String getXMLElementTagName() {
        return "tradeRouteStop";
    }
}
