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
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.server.ai.mission.PioneeringMission;

import org.w3c.dom.Element;


/**
 * Represents a plan to improve a <code>Tile</code> in some way.
 * For instance by plowing or by building a road.
 *
 * @see Tile
 */
public class TileImprovementPlan extends ValuedAIObject {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(TileImprovementPlan.class.getName());

    /**
     * The type of improvement, from TileImprovementTypes.
     */
    private TileImprovementType type;
    
    /**
     * The pioneer which should make the improvement (if a
     * <code>Unit</code> has been assigned).
     */
    private AIUnit pioneer = null;
    
    /**
     * The <code>Tile</code> to be improved.
     */
    private Tile target;


    /**
     * Creates a new <code>TileImprovementPlan</code>.
     *
     * @param aiMain The main AI-object.
     * @param target The target <code>Tile</code> for the improvement.
     * @param type The type of improvement.
     * @param value The value identifying the importance of
     *        this <code>TileImprovementPlan</code> - a higher value 
     *        signals a higher importance.
     */
    public TileImprovementPlan(AIMain aiMain, Tile target,
        TileImprovementType type, int value) {
        super(aiMain, getXMLElementTagName() + ":" + aiMain.getNextId());
        
        this.target = target;
        this.type = type;
        setValue(value);
    }

    /**
     * Creates a new <code>TileImprovementPlan</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param element The root element for the XML-representation 
     *       of a <code>Wish</code>.
     */
    public TileImprovementPlan(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute(ID_ATTRIBUTE));
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>TileImprovementPlan</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public TileImprovementPlan(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, ID_ATTRIBUTE));
        readFromXML(in);
    }
    
    /**
     * Creates a new <code>TileImprovementPlan</code> from the given
     * XML-representation.
     *
     * @param aiMain The main AI-object.
     * @param id The ID.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public TileImprovementPlan(AIMain aiMain, String id)
        throws XMLStreamException {
        super(aiMain, id);
    }


    /**
     * Disposes this <code>TileImprovementPlan</code>.
     *
     * If a pioneer has been assigned to making this improvement, then
     * abort its mission.
     */
    public void dispose() {
        if (pioneer != null
            && pioneer.getMission() instanceof PioneeringMission) {
            pioneer.abortMission("disposing plan");
        }
        pioneer = null;
        super.dispose();
    }    

    /**
     * Gets the pioneer who have been assigned to making the
     * improvement described by this object.
     * 
     * @return The pioneer which should make the improvement, if 
     *      such a <code>AIUnit</code> has been assigned, and 
     *      <code>null</code> if nobody has been assigned this
     *      mission.
     */
    public AIUnit getPioneer() {
        return pioneer;
    }
    
    /**
     * Sets the pioneer who have been assigned to making the
     * improvement described by this object.
     * 
     * @param pioneer The pioneer which should make the improvement, if 
     *      such a <code>Unit</code> has been assigned, and 
     *      <code>null</code> if nobody has been assigned this
     *      mission.
     */    
    public void setPioneer(AIUnit pioneer) {
        this.pioneer = pioneer;    
    }
    
    /**
     * Gets the <code>TileImprovementType</code> of this plan.
     * 
     * @return The type of the improvement.
     */
    public TileImprovementType getType() {
        return type;
    }
    
    /**
     * Sets the type of this <code>TileImprovementPlan</code>.
     *
     * @param type The <code>TileImprovementType</code>.
     * @see #getType
     */
    public void setType(TileImprovementType type) {
        this.type = type;
    }
    
    /**
     * Gets the target of this <code>TileImprovementPlan</code>.
     *
     * @return The <code>Tile</code> where
     *       {@link #getPioneer pioneer} should make the
     *       given {@link #getType improvement}.
     */
    public Tile getTarget() {
        return target;
    }

    /**
     * Gets the 'most effective' TileImprovementType allowed for a
     * given tile and goods type.  Useful for AI in deciding the
     * improvements to prioritize.
     *
     * @param tile The <code>Tile</code> that will be improved.
     * @param goodsType The <code>GoodsType</code> to be prioritized.
     * @return The best TileImprovementType available to be done.
     */
    public static TileImprovementType getBestTileImprovementType(Tile tile,
        GoodsType goodsType) {
        int bestValue = 0;
        TileImprovementType bestType = null;
        for (TileImprovementType impType
                 : tile.getSpecification().getTileImprovementTypeList()) {
            if (!impType.isNatural()
                && impType.isTileTypeAllowed(tile.getType())
                && tile.findTileImprovementType(impType) == null) {
                int value = impType.getImprovementValue(tile, goodsType);
                if (value > bestValue) {
                    bestValue = value;
                    bestType = impType;
                }
            }
        }
        return bestType;
    }

    /**
     * Updates this tile improvement plan to the best available for its
     * tile and the specified goods type.
     *
     * @param goodsType The <code>GoodsType</code> to be prioritized.
     * @return True if the plan is still viable.
     */
    public boolean update(GoodsType goodsType) {
        TileImprovementType type
            = getBestTileImprovementType(target, goodsType);
        if (type == null) return false;
        setType(type);
        setValue(type.getImprovementValue(target, goodsType));
        return true;
    }

    /**
     * Checks the integrity of a this TileImprovementPlan.
     *
     * @return True if the plan is valid.
     */
    public boolean checkIntegrity() {
        return super.checkIntegrity()
            && (pioneer == null || pioneer.checkIntegrity());
    }


    // Serialization

    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute(ID_ATTRIBUTE, getId());

        out.writeAttribute("type", type.getId());

        out.writeAttribute("value", Integer.toString(getValue()));

        if (pioneer != null && pioneer.checkIntegrity()) {
            out.writeAttribute("pioneer", pioneer.getId());
        }

        out.writeAttribute("target", target.getId());

        out.writeEndElement();
    }

    /**
     * Reads information for this object from an XML stream.
     *
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        String str = in.getAttributeValue(null, "type");
        type = getSpecification().getTileImprovementType(str);

        setValue(getAttribute(in, "value", -1));
        
        str = in.getAttributeValue(null, "pioneer");
        if (str != null) {
            if ((pioneer = (AIUnit) getAIMain().getAIObject(str)) == null) {
                pioneer = new AIUnit(getAIMain(), str);
            }
        } else {
            pioneer = null;
        }

        str = in.getAttributeValue(null, "target");
        target = (Tile) getAIMain().getFreeColGameObject(str);

        in.nextTag();
    }

    /**
     * {@inherit-doc}
     */
    @Override
    public String toString() {
        return "[" + getId() + " " + type.getNameKey()
            + " at " + target + " /" + getValue() + "]";
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "tileImprovementPlan"
     */
    public static String getXMLElementTagName() {
        return "tileImprovementPlan";
    }    
}
