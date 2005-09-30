
package net.sf.freecol.server.ai.mission;

import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.Wish;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
* Mission for realizing a <code>Wish</code>.
* @see Wish
*/
public class WishRealizationMission extends Mission {
    private static final Logger logger = Logger.getLogger(WishRealizationMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private Wish wish;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public WishRealizationMission(AIMain aiMain, AIUnit aiUnit, Wish wish) {
        super(aiMain, aiUnit);
        this.wish = wish;
    }


    /**
    * Loads a mission from the given element.
    */
    public WishRealizationMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Disposes this <code>Mission</code>.
    */
    public void dispose() {
        if (wish != null) {
            wish.setTransportable(null);
            wish = null;
        }
        super.dispose();
    }


    /**
    * Performs this mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit unit = getUnit();

        if (!isValid()) {
            return;
        }

        // Move towards the target.
        if (getUnit().getTile() != null) {
            if (wish.getDestination().getTile() != getUnit().getTile()) {
                int r = moveTowards(connection, wish.getDestination().getTile());
                if (r >= 0 && (unit.getMoveType(r) == Unit.MOVE || unit.getMoveType(r) == Unit.DISEMBARK)) {
                    move(connection, r);
                }
            }
            if (wish.getDestination().getTile() == getUnit().getTile()) {
                if (wish.getDestination() instanceof Colony) {
                    Colony colony = (Colony) wish.getDestination();
                    // TODO: Do this by sending a message to the server:
                    getUnit().setLocation(colony);
                } else {
                    logger.warning("Unknown type of destination for: " + wish);
                }
            }
        }
    }



    /**
    * Returns the destination for this <code>Transportable</code>.
    * This can either be the target {@link Tile} of the transport
    * or the target for the entire <code>Transportable</code>'s
    * mission. The target for the tansport is determined by
    * {@link TransportMission} in the latter case.
    *
    * @return The destination for this <code>Transportable</code>.
    */    
    public Tile getTransportDestination() {
        if (getUnit().getLocation() instanceof Unit) {
            return wish.getDestination().getTile();
        } else if (getUnit().getLocation().getTile() == wish.getDestination().getTile()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(wish.getDestination().getTile()) == null) {
            return wish.getDestination().getTile();
        } else {
            return null;
        }
    }


    /**
    * Returns the priority of getting the unit to the
    * transport destination.
    *
    * @return The priority.
    */
    public int getTransportPriority() {
        if (getUnit().getLocation() instanceof Unit) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else if (getUnit().getLocation().getTile() == wish.getDestination().getTile()) {
            return 0;
        } else if (getUnit().getTile() == null || getUnit().findPath(wish.getDestination().getTile()) == null) {
            return NORMAL_TRANSPORT_PRIORITY;
        } else {
            return 0;
        }
    }


    /**
    * Checks if this mission is still valid to perform.
    *
    * @return <code>true</code> if this mission is still valid to perform
    *         and <code>false</code> otherwise.
    */
    public boolean isValid() {
        Location l = wish.getDestination();
        if (l instanceof Ownable && ((Ownable) l).getOwner() != getUnit().getOwner()) {
            return false;
        } else {
            return true;
        }
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());
        element.setAttribute("wish", wish.getID());

        return element;
    }


    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));
        
        wish = (Wish) getAIMain().getAIObject(element.getAttribute("wish"));
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "wishRealizationMission".
    */
    public static String getXMLElementTagName() {
        return "wishRealizationMission";
    }
}
