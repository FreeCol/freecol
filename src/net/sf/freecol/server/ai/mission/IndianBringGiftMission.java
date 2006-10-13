
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsContainer;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
* Mission for bringing a gift to a specified player.
*
* <BR><BR>
*
* The mission has three different tasks to perform:
* <ol>
*     <li>Get the gift (goods) from the {@link IndianSettlement} that owns the unit.
*     <li>Transport this gift to the given {@link Colony}.
*     <li>Complete the mission by delivering the gift.
* </ol>
*/
public class IndianBringGiftMission extends Mission {
    private static final Logger logger = Logger.getLogger(IndianBringGiftMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /** The <code>Colony</code> receiving the gift. */
    private Colony target;
    
    /** Desides wether this mission has been completed or not. */
    private boolean giftDelivered;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    * @param target The <code>Colony</code> receiving the gift.    
    */
    public IndianBringGiftMission(AIMain aiMain, AIUnit aiUnit, Colony target) {
        super(aiMain, aiUnit);
        
        this.target = target;

        if (getUnit().getType() != Unit.BRAVE) {
            logger.warning("Only an indian brave can be given the mission: IndianBringGiftMission");
            throw new IllegalArgumentException("Only an indian brave can be given the mission: IndianBringGiftMission");
        }
    }

    
    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */    
    public IndianBringGiftMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>IndianBringGiftMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public IndianBringGiftMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }
    
    /**
    * Performs the mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        if (!hasGift()) {
            if (getUnit().getTile() != getUnit().getIndianSettlement().getTile()) {
                // Move to the owning settlement:
                int r = moveTowards(connection, getUnit().getIndianSettlement().getTile());
                if (r >= 0 && getUnit().getMoveType(r) == Unit.MOVE) {
                    move(connection, r);
                }
            } else {
                // Load the goods:
                List goodsList = new ArrayList();
                GoodsContainer gc = getUnit().getIndianSettlement().getGoodsContainer();
                for (int i=1; i<=4; i++) {
                    if (gc.getGoodsCount(i) >= IndianSettlement.KEEP_RAW_MATERIAL + 25) {
                        goodsList.add(new Goods(getGame(), getUnit().getIndianSettlement(), i, getRandom().nextInt(15)+10));
                    }
                }
                
                if (goodsList.size() > 0) {
                    Goods goods = (Goods) goodsList.get(getRandom().nextInt(goodsList.size()));
                    goods.setLocation(getUnit());
                }
            }
        } else {
            // Move to the target's colony and deliver
            int r = moveTowards(connection, target.getTile());
            if (r >= 0
                    && getGame().getMap().getNeighbourOrNull(r, getUnit().getTile()) == target.getTile()) { 
                // We have arrived.
                Element deliverGiftElement = Message.createNewRootElement("deliverGift");
                deliverGiftElement.setAttribute("unit", getUnit().getID());
                deliverGiftElement.setAttribute("settlement", target.getID());
                deliverGiftElement.appendChild(((Goods) getUnit().getGoodsIterator().next()).toXMLElement(null, deliverGiftElement.getOwnerDocument()));

                try {
                    connection.sendAndWait(deliverGiftElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"deliverGift\"-message!");
                }
                
                giftDelivered = true;
                getUnit().getOwner().modifyTension(target.getOwner(), 1);
            }
        }
        
        // Walk in a random direction if we have any moves left:
        Tile thisTile = getUnit().getTile();
        Unit unit = getUnit();
        while(unit.getMovesLeft() > 0) {
            int direction = (int) (Math.random() * 8);
            int j;
            for (j = 8; j > 0 && 
                    ((unit.getGame().getMap().getNeighbourOrNull(direction, thisTile) == null)
                            || (unit.getMoveType(direction) != Unit.MOVE)); j--) {
                direction = (int) (Math.random() * 8);
            }
            if (j == 0) break;
            thisTile = unit.getGame().getMap().getNeighbourOrNull(direction, thisTile);

            Element moveElement = Message.createNewRootElement("move");
            moveElement.setAttribute("unit", unit.getID());
            moveElement.setAttribute("direction", Integer.toString(direction));
            
            try {
                connection.sendAndWait(moveElement);
            } catch (IOException e) {
                logger.warning("Could not send \"move\"-message!");
            }
        }        
    }


    /**
    * Checks if the unit is carrying a gift (goods).
    * @return <i>true</i> if <code>getUnit().getSpaceLeft() == 0</code> and false otherwise.
    */
    private boolean hasGift() {
        return (getUnit().getSpaceLeft() == 0);
    }

    
    /**
    * Checks if this mission is still valid to perform.
    *
    * <BR><BR>
    *
    * This mission will be invalidated when the gift has been delivered.
    * In case of the stances {@link Player#WAR WAR} or {@link Player#CEASE_FIRE CEASE_FIRE} towards the target player,
    * the mission would be invalidated as well.
    * 
    * @return <code>true</code> if this mission is still valid.
    */
    public boolean isValid() {
        return target != null 
                && !target.isDisposed()
                && target.getTile().getColony() == target
                && !giftDelivered
                && isValidMission(getUnit().getOwner(), target.getOwner());
    }


    /**
    * Checks if the player <code>owner</code> can bring a gift to the
    * <code>targetPlayer</code>.
    * 
    * @param owner The owner of this mission.
    * @param targetPlayer The target of the gift.
    * @return <code>true</code> if this mission is still valid to perform
    *       with regard to the tension towards the target player.
    */
    public static boolean isValidMission(Player owner, Player targetPlayer) {
        int stance = owner.getStance(targetPlayer);
        return (stance != Player.WAR && stance != Player.CEASE_FIRE) &&
            owner.getTension(targetPlayer).getLevel() <= Tension.HAPPY;
    }

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related 
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("unit", getUnit().getID());
        out.writeAttribute("target", target.getID());
        out.writeAttribute("giftDelivered", Boolean.toString(giftDelivered));

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        
        target = (Colony) getGame().getFreeColGameObject(in.getAttributeValue(null, "target"));
        giftDelivered = Boolean.valueOf(in.getAttributeValue(null, "giftDelivered")).booleanValue();
        
        in.nextTag();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "indianBringGiftMission".
    */
    public static String getXMLElementTagName() {
        return "indianBringGiftMission";
    }
}
