/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Objects of this class contains AI-information for a single {@link WorkLocation}.
*/
public class WorkLocationPlan extends ValuedAIObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(WorkLocationPlan.class.getName());

    /**
    * The FreeColGameObject this AIObject contains AI-information for.
    */
    private WorkLocation workLocation;
    private int priority;
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
    public WorkLocationPlan(AIMain aiMain, WorkLocation workLocation, GoodsType goodsType) {
        super(aiMain);
        this.workLocation = workLocation;
        this.goodsType = goodsType;
        setValue(getProductionOf(goodsType));
    }


    /**
     * Creates a new <code>WorkLocationPlan</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public WorkLocationPlan(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
        setValue(getProductionOf(goodsType));
    }


    /**
     * Gets a <code>TileImprovementPlan</code> which will improve
     * the production of the goods type specified by this
     * <code>WorkLocationPlan</code>.
     * 
     * @return The <code>TileImprovementPlan</code> if there is an
     *      improvement, plow or build road, which will increase
     *      the production of the goods type specified by this 
     *      plan. <code>null</code> gets returned if this plan is 
     *      for a <code>Building</code> or that the <code>Tile</code> 
     *      does not have an improvement.
     */
    public TileImprovementPlan createTileImprovementPlan() {   
        return updateTileImprovementPlan(null);
    }
        
    /**
     * Updates the given <code>TileImprovementPlan</code>.
     * 
     * @param tip The <code>TileImprovementPlan</code> to update.
     * @return The same <code>TileImprovementPlan</code>-object
     *      as provided to the method or <code>null</code> if 
     *      there is no more need for the improvement. 
     */
    public TileImprovementPlan updateTileImprovementPlan(TileImprovementPlan tip) {
        if (workLocation instanceof ColonyTile) {
            Tile tile = ((ColonyTile) workLocation).getWorkTile();
            
            if (tip != null && tip.getTarget() != tile) {
                throw new IllegalArgumentException("The given TileImprovementPlan was not created for this Tile.");
            }
            
            // Update to find the best thing to do now
            TileImprovementType impType = TileImprovement.findBestTileImprovementType(tile, goodsType);
            if (impType != null) {
                int value = impType.getValue(tile.getType(), goodsType);
                if (tip == null) {
                    return new TileImprovementPlan(getAIMain(), tile, impType, value);
                } else {
                    tip.setType(impType);
                    tip.setValue(value);
                    return tip;
                }
            }
        }
        return null;
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
     * Gets the production of the given type of goods according to this
     * <code>WorkLocationPlan</code>. The plan has been created for either
     * a {@link ColonyTile} or a {@link Building}. If this is a plan for a
     * <code>ColonyTile</code> then the maximum possible production of the
     * tile gets returned, while the <code>Building</code>-plans only returns
     * a number used for identifying the value of the goods produced.
     *
     * @param goodsType The type of goods to get the production for.
     * @return The production.
     */
    public int getProductionOf(GoodsType goodsType) {
        if (goodsType == null || goodsType != this.goodsType) {
            return 0;
        }
        
        if (workLocation instanceof ColonyTile) {
            if (!goodsType.isFarmed()) {
                return 0;
            }

            ColonyTile ct = (ColonyTile) workLocation;
            Tile t = ct.getWorkTile();
            UnitType expertUnitType = getAIMain().getGame().getSpecification().getExpertForProducing(goodsType);

            int base = t.getMaximumPotential(goodsType, expertUnitType);

            if (t.isLand() && base != 0) {
                base++;
            }
            /**
             * What's this supposed to be? Are we checking for the
             * possible production bonus granted by Henry Hudson? If
             * so, we should check all possible production bonuses instead.
             *
             * return expertUnitType.getProductionFor(goodsType, base) * ((goodsType == Goods.FURS) ? 2 : 1);
             */
            return expertUnitType.getProductionFor(goodsType, base);
        } else {
            if (goodsType.isFarmed()) {
                return 0;
            } else {
                /* These values are not really the production, but are
                   being used while sorting the WorkLocationPlans:
                */

                if (goodsType == getAIMain().getGame().getSpecification().getGoodsType("model.goods.hammers")) {
                    return 16;
                } else if (goodsType == getAIMain().getGame().getSpecification().getGoodsType("model.goods.bells")) {
                    return 12;
                } else if (goodsType == getAIMain().getGame().getSpecification().getGoodsType("model.goods.crosses")) {
                    return 10;
                } else {
                    return workLocation.getColony().getOwner().getMarket().getSalePrice(goodsType, 1);
                }
            }
        }
    }

    /**
    * Gets the type of goods which should be produced at the <code>WorkLocation</code>.
    *
    * @return The type of goods.
    * @see Goods
    * @see WorkLocation
    */
    public GoodsType getGoodsType() {
        return goodsType;
    }
    
    
    /**
    * Sets the type of goods to be produced at the <code>WorkLocation</code>.
    *
    * @param goodsType The type of goods.
    * @see Goods
    * @see WorkLocation
    */
    public void setGoodsType(GoodsType goodsType) {
        this.goodsType = goodsType;
        setValue(getProductionOf(goodsType));
    }

    
    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", workLocation.getId());
        element.setAttribute("priority", Integer.toString(priority));
        element.setAttribute("goodsType", goodsType.getId());

        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>WorkLocationPlan</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        workLocation = (WorkLocation) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
        priority = Integer.parseInt(element.getAttribute("priority"));
        goodsType = getAIMain().getGame().getSpecification().getGoodsType(element.getAttribute("goodsType"));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return "workLocationPlan"
    */
    public static String getXMLElementTagName() {
        return "workLocationPlan";
    }
}
