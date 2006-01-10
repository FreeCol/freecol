
package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Locatable;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIGoods;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.GoodsWish;
import net.sf.freecol.server.ai.Transportable;
import net.sf.freecol.server.ai.Wish;
import net.sf.freecol.server.ai.WorkerWish;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
* Mission for transporting units and goods on a carrier.
* @see net.sf.freecol.common.model.Unit Unit
*/
public class TransportMission extends Mission {
    private static final Logger logger = Logger.getLogger(TransportMission.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    private static final String ELEMENT_TRANSPORTABLE = "transportable";
    private static final int MINIMUM_GOLD_TO_STAY_IN_EUROPE = 600;

    List transportList = new ArrayList();


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    * 
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public TransportMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);


        if (!getUnit().isCarrier()) {
            logger.warning("Only carriers can transport unit/goods.");
            throw new IllegalArgumentException("Only carriers can transport unit/goods.");
        }
    }


    /**
     * Loads a <code>TransportMission</code> from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public TransportMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Adds every <code>Goods</code> and <code>Unit</code>
    * onboard the carrier to the transport list.
    *
    * @see Goods
    * @see Unit
    */
    private void updateTransportList() {
        Unit carrier = getUnit();

        Iterator ui = carrier.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = (Unit) ui.next();
            AIUnit aiUnit = (AIUnit) getAIMain().getAIObject(u);
            addToTransportList(aiUnit);
        }
    }


    /**
    * Checks if the carrier using this mission is carrying the
    * given <code>Transportable</code>.
    *
    * @param t The <code>Transportable</code>.
    * @return <code>true</code> if the given <code>Transportable</code>
    *         is {@link Unit#getLocation located} in the carrier.
    */
    private boolean isCarrying(Transportable t) {
        // TODO: Proper code for checking if the goods is onboard the carrier.
        return t.getTransportLocatable().getLocation() == getUnit();
    }


    /**
    * Disposes this <code>Mission</code>.
    */
    public void dispose() {
        Iterator ti = transportList.iterator();
        while (ti.hasNext()) {
            Transportable t = (Transportable) ti.next();
            if (isCarrying(t)) {
                ((AIObject) t).dispose();
            } else {
                t.setTransport(null);
            }
        }
    }


    /**
    * Checks if the given <code>Transportable</code> is on the
    * transport list.
    *
    * @param newTransportable The <code>Transportable</code> to
    *         be checked
    * @return <code>true</code> if the given <code>Transportable</code>
    *         was on the transport list, and <code>false</code>
    *         otherwise.
    */
    public boolean isOnTransportList(Transportable newTransportable) {
        for (int i=0; i<transportList.size(); i++) {
            if (transportList.get(i) == newTransportable) {
                return true;
            }
        }
        return false;
    }


    /**
    * Removes the given <code>Transportable</code> from the transport list.
    * @param transportable The <code>Transportable</code>.
    */
    public void removeFromTransportList(Transportable transportable) {
        Iterator ti = transportList.iterator();
        while (ti.hasNext()) {
            Transportable t = (Transportable) ti.next();
            if (t == transportable) {
                ti.remove();
            }
        }

    }


    /**
    * Adds the given <code>Transportable</code> to the transport list.
    * The method returns immediately if the {@link Transportable}
    * has already be added.
    *
    * <br><br>
    *
    * Both the source and destination {@link Location} for the <code>Transportable</code>
    * is entered into the transport list if the <code>Transportable</code> is not
    * already loaded onto the transport. If the <code>Transportable</code> is onboard
    * the transport, then only the destination is put on the transport list.
    *
    * @param newTransportable The <code>Transportable</code>.
    */
    public void addToTransportList(Transportable newTransportable) {
        Unit carrier = getUnit();
        if (newTransportable.getTransportLocatable() instanceof Unit
                && ((Unit) newTransportable.getTransportLocatable()).isCarrier()) {
            throw new IllegalArgumentException("You cannot add a carrier to the transport list.");
        }
        Location newSource = newTransportable.getTransportSource();
        Location newDestination = newTransportable.getTransportDestination();
        if (newDestination == null || (newSource == null && !isCarrying(newTransportable))) {
            logger.warning("No source/destination for: " + newTransportable.getTransportLocatable().getName());
            return;
        }

        if (isOnTransportList(newTransportable)) {
            return;
        }

        int bestSourceIndex = -1;
        if (!isCarrying(newTransportable)) {
            int bestSourceDistance;
            if (carrier.getLocation().getTile() == newSource.getTile()) {
                bestSourceIndex = 0;
                bestSourceDistance = 0;
            } else {
                bestSourceIndex = 0;
                bestSourceDistance = getDistanceTo(newTransportable,
                                        ((carrier.getTile() != null) ? carrier.getTile() : carrier.getEntryLocation().getTile()),
                                        true);
            }
            for (int i=1; i<transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = (Transportable) transportList.get(i-1);
                if (t1.getTransportSource() != null && 
                        t1.getTransportSource().getTile() == newSource.getTile()
                        || t1.getTransportDestination() != null && 
                        t1.getTransportDestination().getTile() == newSource.getTile()) {
                    bestSourceIndex = i;
                    bestSourceDistance = 0;
                }

            }
            for (int i=1; i<transportList.size() && bestSourceDistance > 0; i++) {
                Transportable t1 = (Transportable) transportList.get(i-1);
                if (isCarrying(t1) && getDistanceTo(newTransportable, t1.getTransportDestination(), true) <= bestSourceDistance) {
                    bestSourceIndex = i;
                    bestSourceDistance = getDistanceTo(newTransportable, t1.getTransportDestination(), true);
                } else if (!isCarrying(t1)
                        && getDistanceTo(newTransportable, t1.getTransportSource(), true) <= bestSourceDistance) {
                    bestSourceIndex = i;
                    bestSourceDistance = getDistanceTo(newTransportable, t1.getTransportSource(), true);
                }
            }
            transportList.add(bestSourceIndex, newTransportable);
        }

        int bestDestinationIndex = bestSourceIndex + 1;
        int bestDestinationDistance = Integer.MAX_VALUE;
        if (bestSourceIndex == -1) {
            bestDestinationIndex = 0;
            if (carrier.getTile() == newSource.getTile()) {
                bestDestinationDistance = 0;
            } else {
                bestDestinationDistance = getDistanceTo(newTransportable, carrier.getTile(), false);
            }
        }
        for (int i=Math.max(bestSourceIndex, 1); i<transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = (Transportable) transportList.get(i-1);
            if (t1.getTransportSource().getTile() == newDestination.getTile()
                    || t1.getTransportDestination().getTile() == newDestination.getTile()) {
                bestDestinationIndex = i;
                bestDestinationDistance = 0;
            }

        }
        for (int i=Math.max(bestSourceIndex, 1); i<transportList.size() && bestDestinationDistance > 0; i++) {
            Transportable t1 = (Transportable) transportList.get(i-1);
            if (isCarrying(t1) && getDistanceTo(newTransportable, t1.getTransportDestination(), false) <= bestDestinationDistance) {
                bestDestinationIndex = i;
                bestDestinationDistance = getDistanceTo(newTransportable, t1.getTransportDestination(), false);
            } else if (!isCarrying(t1)
                    && getDistanceTo(newTransportable, t1.getTransportSource(), false) <= bestDestinationDistance) {
                bestDestinationIndex = i;
                bestDestinationDistance = getDistanceTo(newTransportable, t1.getTransportSource(), false);
            }
        }
        transportList.add(bestDestinationIndex, newTransportable);
        
        if (newTransportable.getTransport() != getAIUnit()) {
            newTransportable.setTransport(getAIUnit());
        }
    }


    /**
    * Gets the distance to the given <code>Transportable</code>.
    *
    * @param start The <code>Location</code> to check the distance from.
    *           <code>Europe</code> is used instead of this location
    *           if <code>start.getTile() == null</code>.
    * @param source Sets wether the <code>Transportable</code>'s 
    *           {@link Transportable#getTransportSource source}
    *           or {@link Transportable#getTransportDestination destination}
    *           should be used.
    * @return The distance from the given <code>Location</code> to the
    *           source or destination of the given <code>Transportable</code>.
    */
    private int getDistanceTo(Transportable t, Location start, boolean source) {
        // TODO: This takes to much resources - find another method:
        return getPath(t, start, source).getTotalTurns();
    }


    /**
    * Performs the mission.
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
        Unit carrier = getUnit();

        if (carrier.getLocation() instanceof Europe) {
            if (carrier.getState() == Unit.TO_EUROPE || carrier.getState() == Unit.TO_AMERICA) {
                return;
            } else {
                restockCargoAtDestination(connection);
                buyCargo(connection);
                restockCargoAtDestination(connection);

                // Move back to america:
                if (carrier.getOwner().getGold() < MINIMUM_GOLD_TO_STAY_IN_EUROPE
                        || transportList.size() > 0) {
                    Element moveToAmericaElement = Message.createNewRootElement("moveToAmerica");
                    moveToAmericaElement.setAttribute("unit", carrier.getID());
                    try {
                        connection.sendAndWait(moveToAmericaElement);
                    } catch (IOException e) {
                        logger.warning("Could not send \"moveToAmericaElement\"-message!");
                    }
                }
                return;
            }
        }

        if (transportList == null || transportList.size() <= 0) {
            updateTransportList();
        }

        restockCargoAtDestination(connection);

        boolean transportListChanged = false;
        boolean moreWork = true;
        for (int i=0; i<transportList.size() && moreWork || i == 0; i++) {
            moreWork = false;

            if (transportListChanged) {
                i = 0;
                transportListChanged = false;
            }

            boolean moveToEurope = false;

            // Determine the path to the next target:
            PathNode path = null;
            if (i == 0 && transportList.size() == 0) {
                // Send to Europe if the transport list is empty:
                path = findPathToEurope(carrier.getTile());
                moveToEurope = true;
            } else {
                Transportable transportable = (Transportable) transportList.get(i);
                path = getPath(transportable);
                moveToEurope = (transportable.getTransportDestination() instanceof Europe);
            }

            // Move towards the next target:
            if (path != null) {
                //Tile oldTile = carrier.getTile();

                int r = moveTowards(connection, path);
                if (r >= 0 && (carrier.getMoveType(r) == Unit.MOVE || carrier.getMoveType(r) == Unit.MOVE_HIGH_SEAS)) {
                    //Tile target = getGame().getMap().getNeighbourOrNull(r, carrier.getTile());
                    if (carrier.getMoveType(r) == Unit.MOVE_HIGH_SEAS && moveToEurope) {
                        Element moveToEuropeElement = Message.createNewRootElement("moveToEurope");
                        moveToEuropeElement.setAttribute("unit", carrier.getID());
                        try {
                            connection.sendAndWait(moveToEuropeElement);
                        } catch (IOException e) {
                            logger.warning("Could not send \"moveToEuropeElement\"-message!");
                        }
                    } else {
                        move(connection, r);
                    }

                    if (!(carrier.getLocation() instanceof Europe)) {
                        moreWork = true;
                    }
                }

                transportListChanged = restockCargoAtDestination(connection);
            } else {
                //moreWork = true;
                //continue;
            }
        }
    }


    /**
    * Buys cargo (units and goods) when the carrier is in <code>Europe</code>.
    *
    * <br><br>
    *
    * <b>Warning:</b> This method can only be called
    * when the carrier is located in {@link Europe}.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    private void buyCargo(Connection connection) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());       

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        /* 
         * Quick fix for forcing the AI to build more colonies.
         * This fix should be removed after a proper implementation
         * has been created.
         */
        if (aiPlayer.hasFewColonies()) {
            int space = getAvailableSpace();
            while (space > 0) {
                AIUnit newUnit = getCheapestUnitInEurope(connection);
                if (newUnit != null) {
                    addToTransportList(newUnit);
                    space--;
                } else {
                    return;
                }
            }
        }        
        
        /* Add colonies containing wishes with the same destination as
           an item in the transport list to the "aiColonies"-list: */
        ArrayList aiColonies = new ArrayList();
        for (int i=0; i<transportList.size(); i++) {
            Transportable t = (Transportable) transportList.get(i);
            if (t.getTransportDestination() != null 
                    && t.getTransportDestination().getTile() != null
                    && t.getTransportDestination().getTile().getColony() != null
                    && t.getTransportDestination().getTile().getColony().getOwner() == getUnit().getOwner()) {
                AIColony ac = (AIColony) getAIMain().getAIObject(t.getTransportDestination().getTile().getColony().getID());
                aiColonies.add(ac);
            }
        }

        /* Add the colony containing the wish with the highest value
           to the "aiColonies"-list: */
        Iterator highValueWishIterator = ((AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID())).getWishIterator();
        while (highValueWishIterator.hasNext()) {
            Wish w = (Wish) highValueWishIterator.next();
            if (w.getTransportable() != null) {
                continue;
            }
            if (w instanceof WorkerWish) {
                WorkerWish ww = (WorkerWish) w;
                Colony c = (Colony) ww.getDestination();
                AIColony ac = (AIColony) getAIMain().getAIObject(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else if (w instanceof GoodsWish) {
                GoodsWish gw = (GoodsWish) w;
                Colony c = (Colony) gw.getDestination();
                AIColony ac = (AIColony) getAIMain().getAIObject(c);
                if (!aiColonies.contains(ac)) {
                    aiColonies.add(ac);
                }
            } else {
                logger.warning("Unknown type of wish: " + w);
            }
        }

        for (int i=0; i<aiColonies.size(); i++) {
            AIColony ac = (AIColony) aiColonies.get(i);
            // Assuming that all colonists which can be bought in Europe take the same space:
            int space = getAvailableSpace(Unit.FREE_COLONIST, getUnit().getOwner().getEurope(), ac.getColony());
            Iterator wishIterator = ac.getWishIterator();
            while (space > 0 && wishIterator.hasNext()) {
                Wish w = (Wish) wishIterator.next();
                if (w.getTransportable() != null) {
                    continue;
                }
                if (w instanceof WorkerWish) {
                    WorkerWish ww = (WorkerWish) w;
                    AIUnit newUnit = getUnitInEurope(connection, ww.getUnitType());
                    if (newUnit != null) {
                        newUnit.setMission(new WishRealizationMission(getAIMain(), newUnit, ww));
                        ww.setTransportable(newUnit);
                        addToTransportList(newUnit);
                        space--;
                    }
                } else if (w instanceof GoodsWish) {
                    GoodsWish gw = (GoodsWish) w;
                    AIGoods ag = buyGoodsInEurope(connection, gw.getGoodsType(), 100, gw.getDestination());
                    if (ag != null) {
                        gw.setTransportable(ag);
                        addToTransportList(ag);
                        space--;
                    }
                } else {
                    logger.warning("Unknown type of wish: " + w);
                }
            }
        }

        // Fill the transport with cheap colonists:
        int space = getAvailableSpace();
        while (space > 0) {
            AIUnit newUnit = getCheapestUnitInEurope(connection);
            if (newUnit != null) {
                addToTransportList(newUnit);
                space--;
            } else {
                break;
            }
        }
    }


    /**
    * Buys the given cargo.
    *
    * <br><br>
    *
    * <b>Warning:</b> This method can only be called
    * when the carrier is located in {@link Europe}.
    *
    * @param connection The <code>Connection</code> to use
    *       when communicating with the server.
    * @param type The type of goods to buy.
    * @param amount The amount of goods to buy.
    * @param destination The <code>Location</code> to which the goods should be
    *       transported.
    * @return The goods.
    */
    public AIGoods buyGoodsInEurope(Connection connection, int type, int amount, Location destination) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();
        Market market = getAIMain().getGame().getMarket();

        if (player.getGold() >= market.getBidPrice(type, amount)) {
            Element buyGoodsElement = Message.createNewRootElement("buyGoods");
            buyGoodsElement.setAttribute("carrier", getUnit().getID());
            buyGoodsElement.setAttribute("type", Integer.toString(type));
            buyGoodsElement.setAttribute("amount", Integer.toString(amount));
            try {
                connection.sendAndWait(buyGoodsElement);
            } catch (IOException e) {
                logger.warning("Could not send \"trainUnitInEurope\"-message to the server.");
                return null;
            }
            AIGoods ag = new AIGoods(getAIMain(), getUnit(), type, amount, destination);
            return ag;
        } else {
            return null;
        }
    }


    /**
    * Returns the given type of <code>Unit</code>.
    *
    * <br><br>
    *
    * <b>Warning:</b> This method can only be called
    * when the carrier is located in {@link Europe}.
    *
    * <br><br>
    *
    * This sequence is used when trying to get the unit:
    * <br><br>
    * <ol>
    *   <li>Getting the unit from the docks.
    *   <li>Recruiting the unit.
    *   <li>Training the unit.
    * </ol>
    *
    * @param connection The <code>Connection</code> to the server.
    * @param unitType The type of {@link Unit} to be
    *       found/recuited/trained.
    * @return The <code>AIUnit</code>.
    */
    private AIUnit getUnitInEurope(Connection connection, int unitType) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        // Check if the given type of unit appear on the docks:
        Iterator ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = (Unit) ui.next();
            if (unitType == -1 || unitType == u.getType()) {
                return (AIUnit) getAIMain().getAIObject(u.getID());
            }
        }

        // Try recruiting the unit:
        // TODO: Check if it will be cheaper to train the unit instead.
        if (player.getGold() >= player.getRecruitPrice()) {
            for (int i=1; i<=3; i++) {
                if (europe.getRecruitable(i) == unitType) {
                    Element recruitUnitInEuropeElement = Message.createNewRootElement("recruitUnitInEurope");
                    recruitUnitInEuropeElement.setAttribute("slot", Integer.toString(i));
                    try {
                        Element reply = connection.ask(recruitUnitInEuropeElement);
                        if (reply.getTagName().equals("recruitUnitInEuropeConfirmed")) {
                            return (AIUnit) getAIMain().getAIObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
                        } else {
                            logger.warning("Could not recruit the specified unit in europe.");
                            continue;
                        }
                    } catch (IOException e) {
                        logger.warning("Could not send \"recruitUnitInEurope\"-message to the server.");
                    }
                }
            }
        }

        // Try training the unit:
        if (unitType != Unit.ARTILLERY && Unit.getPrice(unitType) >= 0 && player.getGold() >= Unit.getPrice(unitType)
                || unitType == Unit.ARTILLERY && player.getGold() >= europe.getArtilleryPrice()) {
            Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
            trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(unitType));
            try {
                Element reply = connection.ask(trainUnitInEuropeElement);
                if (reply.getTagName().equals("trainUnitInEuropeConfirmed")) {
                    return (AIUnit) getAIMain().getAIObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
                } else {
                    logger.warning("Could not train the specified unit in europe.");
                }
            } catch (IOException e) {
                logger.warning("Could not send \"trainUnitInEurope\"-message to the server.");
            }
        }

        return null;
    }


    /**
    * Returns the cheapest unit which can be bought in <code>Europe</code>.
    * @return The <code>AIUnit</code>.
    */
    private AIUnit getCheapestUnitInEurope(Connection connection) {
        AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getID());
        Player player = aiPlayer.getPlayer();
        Europe europe = player.getEurope();

        if (!(getUnit().getLocation() instanceof Europe)) {
            throw new IllegalStateException("Carrier not in Europe");
        }

        // Check if there are any units on the docks:
        Iterator ui = europe.getUnitIterator();
        while (ui.hasNext()) {
            Unit u = (Unit) ui.next();
            if (!u.isCarrier()) {
                return (AIUnit) getAIMain().getAIObject(u.getID());
            }
        }

        // Try recruiting the unit:
        if (player.getGold() >= player.getRecruitPrice() && player.getRecruitPrice() < Unit.getPrice(Unit.EXPERT_ORE_MINER)) {
            Element recruitUnitInEuropeElement = Message.createNewRootElement("recruitUnitInEurope");
            // TODO: Take the best unit (Seasoned scout, pioneer, soldier etc):
            recruitUnitInEuropeElement.setAttribute("slot", Integer.toString(1));
            try {
                Element reply = connection.ask(recruitUnitInEuropeElement);
                if (reply.getTagName().equals("recruitUnitInEuropeConfirmed")) {
                    return (AIUnit) getAIMain().getAIObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
                } else {
                    logger.warning("Could not recruit the specified unit in europe.");
                }
            } catch (IOException e) {
                logger.warning("Could not send \"recruitUnitInEurope\"-message to the server.");
            }
        }

        // Try training the unit:
        if (player.getGold() >= Unit.getPrice(Unit.EXPERT_ORE_MINER)) {
            Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
            trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(Unit.EXPERT_ORE_MINER));
            try {
                Element reply = connection.ask(trainUnitInEuropeElement);
                if (reply.getTagName().equals("trainUnitInEuropeConfirmed")) {
                    return (AIUnit) getAIMain().getAIObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
                } else {
                    logger.warning("Could not train the specified unit in europe.");
                }
            } catch (IOException e) {
                logger.warning("Could not send \"trainUnitInEurope\"-message to the server.");
            }
        }

        return null;
    }


    /**
    * Returns the path the carrier should use to get/drop the given <code>Transportable</code>.
    *
    * @param transportable The <code>Transportable</code>.
    * @return The path.
    */
    public PathNode getPath(Transportable transportable) {
        return getPath(transportable, getUnit().getTile(), !isCarrying(transportable));
    }


    /**
    * Returns the path the carrier should use to get/drop the given <code>Transportable</code>.
    *
    * @param transportable The <code>Transportable</code>.
    * @param start The <code>Tile</code> to search from. If <code>start == null</code>
    *       or <code>start.getTile() == null</code> then the carrier's
    *       {@link Unit#getEntryLocation entry location} is used instead.
    * @param source
    * @return The path.
    */
    private PathNode getPath(Transportable transportable, Location start, boolean source) {
        Unit carrier = getUnit();

        if (isCarrying(transportable) && source) {
            throw new IllegalStateException("Cannot find the path to the source while the transportable is on the carrier.");
        }

        PathNode path;
        Locatable locatable = transportable.getTransportLocatable();

        if (start == null || start.getTile() == null) {
            start = getUnit().getEntryLocation();
        }

        Location destination;
        if (source) {
            destination = locatable.getLocation();
        } else {
            destination = transportable.getTransportDestination();
        }

        if (destination == null) {
            return null;
        }

        if (destination instanceof Europe) {
            path = findPathToEurope(start.getTile());
        } else if (locatable instanceof Unit && isCarrying(transportable)) {
            path = getGame().getMap().findPath((Unit) locatable, start.getTile(), destination.getTile(), carrier);
            if (path.getTransportDropNode().previous == null) {
                path = null;
            } else {
                path.getTransportDropNode().previous.next = null;
            }
        } else {
            path = getGame().getMap().findPath(carrier, start.getTile(), destination.getTile());

        }

        return path;
    }


    /**
    * Returns the available space for the given <code>Transportable</code>.
    *
    * @param t The <code>Transportable</code>
    * @return The space available for <code>Transportable</code>s with the
    *         same source and {@link Transportable#getTransportDestination destination}.
    */
    public int getAvailableSpace(Transportable t) {
        if (t.getTransportLocatable() instanceof Unit) {
            Unit u = (Unit) t.getTransportLocatable();
            return getAvailableSpace(u.getType(), t.getTransportSource(), t.getTransportDestination());
        } else {
            return getAvailableSpace(-1, t.getTransportSource(), t.getTransportDestination());
        }
    }


    /**
    * Returns the available space for the given type of <code>Unit</code>
    * at the given <code>Location</code>.
    *
    * @param unitType The type of {@link Unit} or <code>-1</code> for
    *           {@link Goods}
    * @param source The source for the unit. This is where the unit
    *       is presently located.
    * @param destination The destination for the unit.
    * @return The space available
    */
    public int getAvailableSpace(int unitType, Location source, Location destination) {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }


    /**
    * Returns the available space for any type of unit going to any type of location.
    * @return The space available
    */
    public int getAvailableSpace() {
        // TODO: Implement this method properly:
        return Math.max(0, getUnit().getSpaceLeft() - transportList.size());
    }


    /**
    * Loads and unloads any <code>Transportable</code>.
    *
    * @param connection The <code>Connection</code> to the server.
    * @return <code>true</code> if something has been loaded/unloaded and
    *       <code>false</code>otherwise.
    */
    private boolean restockCargoAtDestination(Connection connection) {
        return unloadCargoAtDestination(connection) | loadCargoAtDestination(connection);
    }


    /**
    * Unloads any <code>Transportable</code>s which have reached
    * their destination.
    *
    * @param connection The <code>Connection</code> to the server.
    * @return <code>true</code> if something has been unloaded and
    *       <code>false</code>otherwise.
    */
    private boolean unloadCargoAtDestination(Connection connection) {
        Unit carrier = getUnit();

        boolean transportListChanged = false;

        Iterator tli = transportList.iterator();
        while (tli.hasNext()) {
            Transportable t = (Transportable) tli.next();
            if (!isCarrying(t)) {
                continue;
            }
            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                Mission mission = au.getMission();
                if (mission != null && mission.isValid()) {
                    if (au.getTransportDestination() != null && au.getTransportDestination().getTile() == carrier.getTile()) {
                        if (u.getLocation() instanceof Europe || u.getTile() != null && u.getTile().getColony() != null) {
                            Element leaveShipElement = Message.createNewRootElement("leaveShip");
                            leaveShipElement.setAttribute("unit", u.getID());
                            try {
                                connection.sendAndWait(leaveShipElement);
                            } catch (IOException e) {
                                logger.warning("Could not send \"leaveShipElement\"-message!");
                            }
                        }
                        mission.doMission(connection);
                        if (u.getLocation() != getUnit()) {
                            tli.remove();
                            transportListChanged = true;
                        }
                    } else if (!(carrier.getLocation() instanceof Europe)
                            && au.getTransportDestination() != null && au.getTransportDestination().getTile() != null) {
                        PathNode p = getGame().getMap().findPath(u, carrier.getTile(), au.getTransportDestination().getTile());
                        if (p != null && p.getTransportDropNode().getTurns() <= 2) {
                            mission.doMission(connection);
                            if (u.getLocation() != getUnit()) {
                                tli.remove();
                                transportListChanged = true;
                            }
                        }
                    }
                }
            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                if (ag.getTransportDestination() != null && ag.getTransportDestination().getTile() == carrier.getLocation().getTile()) {
                    if (carrier.getLocation() instanceof Europe) {
                        Element sellGoodsElement = Message.createNewRootElement("sellGoods");
                        sellGoodsElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(), sellGoodsElement.getOwnerDocument()));
                        try {
                            connection.sendAndWait(sellGoodsElement);
                            tli.remove();
                            ag.dispose();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"sellGoodsElement\"-message!");
                        }
                    } else {
                        Element unloadCargoElement = Message.createNewRootElement("unloadCargo");
                        unloadCargoElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(), unloadCargoElement.getOwnerDocument()));
                        try {
                            connection.sendAndWait(unloadCargoElement);
                            tli.remove();
                            ag.dispose();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"unloadCargoElement\"-message!");
                        }
                    }
                }
            } else {
                logger.warning("Unknown Transportable.");
            }
        }

        return transportListChanged;
    }


    /**
    * Loads any <code>Transportable</code>s being in range
    * of the carrier.
    *
    * @param connection The <code>Connection</code> to the server.
    * @return <code>true</code> if something has been unloaded and
    *       <code>false</code>otherwise.
    */
    private boolean loadCargoAtDestination(Connection connection) {
        Unit carrier = getUnit();

        // TODO: Add code for rendez-vous.

        boolean transportListChanged = false;

        Iterator tli = transportList.iterator();
        while (tli.hasNext()) {
            Transportable t = (Transportable) tli.next();
            if (isCarrying(t)) {
                continue;
            }
            if (t instanceof AIUnit) {
                AIUnit au = (AIUnit) t;
                Unit u = au.getUnit();
                if (u.getTile() == carrier.getTile()) {
                    Element boardShipElement = Message.createNewRootElement("boardShip");
                    boardShipElement.setAttribute("unit", u.getID());
                    boardShipElement.setAttribute("carrier", carrier.getID());
                    try {
                        connection.sendAndWait(boardShipElement);
                        tli.remove();
                        transportListChanged = true;
                    } catch (IOException e) {
                        logger.warning("Could not send \"boardShipElement\"-message!");
                    }
                }
            } else if (t instanceof AIGoods) {
                AIGoods ag = (AIGoods) t;
                if (ag.getGoods().getTile() == carrier.getTile()) {
                    if (carrier.getLocation() instanceof Europe) {
                        Element buyGoodsElement = Message.createNewRootElement("buyGoods");
                        buyGoodsElement.setAttribute("carrier", carrier.getID());
                        buyGoodsElement.setAttribute("type", Integer.toString(ag.getGoods().getType()));
                        buyGoodsElement.setAttribute("amount", Integer.toString(ag.getGoods().getAmount()));
                        try {
                            connection.sendAndWait(buyGoodsElement);
                            tli.remove();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"buyGoodsElement\"-message!");
                        }
                        ag.setGoods(new Goods(getGame(), carrier, ag.getGoods().getType(), ag.getGoods().getAmount()));
                    } else {
                        Element loadCargoElement = Message.createNewRootElement("loadCargo");
                        loadCargoElement.setAttribute("carrier", carrier.getID());
                        loadCargoElement.appendChild(ag.getGoods().toXMLElement(carrier.getOwner(), loadCargoElement.getOwnerDocument()));

                        try {
                            connection.sendAndWait(loadCargoElement);
                            tli.remove();
                            transportListChanged = true;
                        } catch (IOException e) {
                            logger.warning("Could not send \"loadCargoElement\"-message!");
                        }
                        ag.setGoods(new Goods(getGame(), carrier, ag.getGoods().getType(), ag.getGoods().getAmount()));
                    }
                }
            } else {
                logger.warning("Unknown Transportable.");
            }
        }

        return transportListChanged;
    }


    /**
    * Checks if this mission is still valid to perform.
    * @return <code>true</code>
    */
    public boolean isValid() {
        return true;
    }


    /**
    * Finds the best path to <code>Europe</code>.
    *
    * @param start The starting <code>Tile</code>.
    * @return The path to the target or <code>null</code> if no target can
    *         be found.
    * @see Europe
    */
    protected PathNode findPathToEurope(Tile start) {
        return getGame().getMap().findPathToEurope(getUnit(), start);
    }
    
    /**
     * Creates an XML-representation of this object.
     * @param document The <code>Document</code> in which
     *      the XML-representation should be created.
     * @return The XML-representation.
     */    
    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("unit", getUnit().getID());

        Iterator tli = transportList.iterator();
        while (tli.hasNext()) {
            Transportable t = (Transportable) tli.next();
            Element tElement = document.createElement(ELEMENT_TRANSPORTABLE);
            tElement.setAttribute("ID", ((AIObject) t).getID());
            element.appendChild(tElement);

        }
        return element;
    }


    /**
     * Updates this object from an XML-representation of
     * a <code>TransportMission</code>.
     * 
     * @param element The XML-representation.
     */    
    public void readFromXMLElement(Element element) {
        setAIUnit((AIUnit) getAIMain().getAIObject(element.getAttribute("unit")));

        transportList.clear();

        NodeList nl = element.getChildNodes();
        for (int i=0; i<nl.getLength(); i++) {
            if (!(nl.item(i) instanceof Element)) {
                continue;
            }
            Element e = (Element) nl.item(i);
            if (e.getTagName().equals(ELEMENT_TRANSPORTABLE)) {
                AIObject ao = getAIMain().getAIObject(e.getAttribute("ID"));
                if (ao == null) {
                    logger.warning("ao == null for: " + e.getAttribute("ID"));
                } else if (!(ao instanceof Transportable)) {
                    logger.warning("AIObject not Transportable, ID: " + e.getAttribute("ID"));
                } else if (ao != null) {
                    transportList.add((Transportable) ao);
                } else {
                    logger.warning("Could not find unit with ID: " + e.getAttribute("ID"));
                }
            } else {
                logger.warning("Unknown tag.");
            }
        }
    }


    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "transportMission".
    */
    public static String getXMLElementTagName() {
        return "transportMission";
    }
}
