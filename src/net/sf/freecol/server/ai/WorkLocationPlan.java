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

package net.sf.freecol.server.ai;

import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.WorkLocation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link
 * net.sf.freecol.common.model.WorkLocation}.
 */
public class WorkLocationPlan extends AIObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkLocationPlan.class.getName());

    /** The work location the plan is for. */
    private WorkLocation workLocation;

    /** The goods to produce. */
    private GoodsType goodsType;


    /**
     * Creates a new <code>WorkLocationPlan</code>.
     *
     * @param aiMain The main AI-object.
     * @param workLocation The <code>WorkLocation</code> to create
     *      a plan for.
     * @param goodsType The goodsType to be produced on the
     *      <code>workLocation</code> using this plan.
     */
    public WorkLocationPlan(AIMain aiMain, WorkLocation workLocation,
                            GoodsType goodsType) {
        super(aiMain);

        this.workLocation = workLocation;
        this.goodsType = goodsType;

        uninitialized = false;
    }

    /**
     * Creates a new <code>WorkLocationPlan</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public WorkLocationPlan(AIMain aiMain, Element element) {
        super(aiMain, element);
    }


    /**
     * Gets the <code>WorkLocation</code> this
     * <code>WorkLocationPlan</code> controls.
     *
     * @return The <code>WorkLocation</code>.
     */
    public WorkLocation getWorkLocation() {
        return workLocation;
    }

    /**
     * Gets the type of goods which should be produced at the
     * <code>WorkLocation</code>.
     *
     * @return The type of goods.
     * @see net.sf.freecol.common.model.Goods
     * @see net.sf.freecol.common.model.WorkLocation
     */
    public GoodsType getGoodsType() {
        return goodsType;
    }

    /**
     * Sets the type of goods to be produced at the <code>WorkLocation</code>.
     *
     * @param goodsType The type of goods.
     * @see net.sf.freecol.common.model.Goods
     * @see net.sf.freecol.common.model.WorkLocation
     */
    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
    }


    // Serialization

    private static final String GOODS_TYPE_TAG = "goodsType";


    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // do nothing
    }

    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute(ID_ATTRIBUTE_TAG, workLocation.getId());

        element.setAttribute(GOODS_TYPE_TAG, goodsType.getId());

        return element;
    }

    /**
     * Updates this object from an XML-representation of
     * a <code>WorkLocationPlan</code>.
     *
     * @param element The XML-representation.
     */
    public void readFromXMLElement(Element element) {
        final Specification spec = getSpecification();

        String str = element.getAttribute(ID_ATTRIBUTE_TAG);
        // @compat 0.10.7
        if (str == null) str = element.getAttribute(ID_ATTRIBUTE);
        // end @compat
        workLocation = getAIMain().getGame()
            .getFreeColGameObject(str, WorkLocation.class);

        goodsType = spec.getGoodsType(element.getAttribute(GOODS_TYPE_TAG));
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "workLocationPlan"
     */
    public static String getXMLElementTagName() {
        return "workLocationPlan";
    }
}
