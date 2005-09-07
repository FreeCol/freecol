
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;

import net.sf.freecol.server.ai.*;
import net.sf.freecol.common.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


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
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
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
    */
    public IndianBringGiftMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
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
            if (r >= 0 && getGame().getMap().getNeighbourOrNull(r, getUnit().getTile()) == target.getTile()) { // We have arrived.
                Element deliverGiftElement = Message.createNewRootElement("deliverGift");
                deliverGiftElement.setAttribute("unit", getUnit().getID());
                deliverGiftElement.setAttribute("settlement", target.getID());
                deliverGiftElement.appendChild(((Goods) getUnit().getGoodsIterator().next()).toXMLElement(null, deliverGiftElement.getOwnerDocument()));

                try {
                    connection.send(deliverGiftElement);
                } catch (IOException e) {
                    logger.warning("Could not send \"deliverGift\"-message!");
                }
                
                giftDelivered = true;
                getUnit().getOwner().modifyTension(target.getOwner(), 1);
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
    */
    public boolean isValid() {
        // The last check is to ensure that the colony have not been burned to the ground.
        return (!giftDelivered && isValidMission(getUnit().getOwner(), target.getOwner()) 
                && target != null && target.getTile().getColony() == target);
    }


    /**
    * Checks if the player <code>owner</code> can bring a gift to the
    * <code>targetPlayer</code>.
    */
    public static boolean isValidMission(Player owner, Player targetPlayer) {
        int stance = owner.getStance(targetPlayer);
        return (stance != Player.WAR && stance != Player.CEASE_FIRE) && owner.getTension(targetPlayer) <= Player.TENSION_HAPPY;
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("target", target.getID());
        element.setAttribute("giftDelivered", Boolean.toString(giftDelivered));

        return element;
    }


    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        if (element.hasAttribute("target")) {
            target = (Colony) getGame().getFreeColGameObject(element.getAttribute("target"));
        } else {
            // For PRE-0.1.1-protocols
            target = null;
        }

        giftDelivered = Boolean.valueOf(element.getAttribute("giftDelivered")).booleanValue();
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "indianBringGiftMission".
    */
    public static String getXMLElementTagName() {
        return "indianBringGiftMission";
    }
}
