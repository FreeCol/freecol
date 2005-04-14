
package net.sf.freecol.server.ai;

import java.util.*;

import net.sf.freecol.server.ai.mission.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.*;
import net.sf.freecol.server.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.*;


/**
* Objects of this class contains AI-information for a single {@link Player}.
*/
public class AIPlayer extends AIObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2004 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    private static final int MAX_DISTANCE_TO_BRING_GIFT = 10;
    private static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;
    

    /* Stores temporary information for sessions (trading with another player etc). */
    private HashMap sessionRegister = new HashMap();
    
    private Random random = new Random();

    /**
    * The FreeColGameObject this AIObject contains AI-information for:
    */
    private ServerPlayer player;


    /**
    * Creates a new <code>AIPlayer</code>.
    *
    * @param aiMain The main AI-class.
    * @param player The player that should be associated with this
    *        <code>AIPlayer</code>.
    */
    public AIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain);

        this.player = player;
    }


    /**
    * Creates a new <code>AIPlayer</code> and reads the information
    * from the given <code>Element</code>.
    *
    * @param aiMain The main AI-class.
    * @param element The XML-element containing information.
    */
    public AIPlayer(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }


    /**
    * Tells this <code>AIPlayer</code> to make decisions.
    * The <code>AIPlayer</code> is done doing work this turn
    * when this method returns.
    */
    public void startWorking() {
        Iterator aiUnitsIterator, playerIterator;

        sessionRegister.clear();

        // Determines the stance towards the other players:
        playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();
            if (getPlayer().getTension(p) >= Player.TENSION_ADD_NORMAL) {
                getPlayer().setStance(p, Player.WAR);
            } else {
                getPlayer().setStance(p, Player.PEACE);
            }
        }

        // Abort missions that are no longer valid:
        aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (!aiUnit.getMission().isValid() || aiUnit.getMission() instanceof UnitWanderHostileMission
                    || aiUnit.getMission() instanceof UnitWanderMission) {
                aiUnit.setMission(null);
            }
        }

        secureSettlements();

        // Bring gifts to nice players:
        if (!player.isEuropean()) {
            //bringGifts();
        }

        // Assign a mission to every unit:
        aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (!aiUnit.hasMission()) {
                aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            }
        }

        // Make every unit perform their mission:
        aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            aiUnit.doMission(getConnection());
        }
    }


    /**
    * Brings gifts to nice players with nearby colonies.
    * Should only be called for an indian player.
    */
    private void bringGifts() {
        Iterator indianSettlementIterator = player.getIndianSettlementIterator();
        while (indianSettlementIterator.hasNext()) {
            IndianSettlement indianSettlement = (IndianSettlement) indianSettlementIterator.next();

            int alreadyAssignedUnits = 0;
            Iterator ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (((AIUnit) getAIMain().getAIObject((Unit) ownedUnits.next())).getMission() instanceof IndianBringGiftMission) {
                    alreadyAssignedUnits++;
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_GIFTS_BEING_DELIVERED) {
                continue;
            }

            // Creates a list of nearby colonies:
            List nearbyColonies = new ArrayList();
            Iterator it = getGame().getMap().getCircleIterator(indianSettlement.getTile().getPosition(), true, MAX_DISTANCE_TO_BRING_GIFT);
            while (it.hasNext()) {
                Tile t = getGame().getMap().getTile((Map.Position) it.next());
                if (t.getColony() != null && IndianBringGiftMission.isValidMission(getPlayer(), t.getColony().getOwner())
                        && getPlayer().getTension(t.getColony().getOwner()) <= Player.TENSION_HAPPY) {
                    nearbyColonies.add(t.getColony());
                }
            }
            if (nearbyColonies.size() > 0) {
                Colony target = (Colony) nearbyColonies.get(random.nextInt(nearbyColonies.size()));
                Iterator it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = (AIUnit) getAIMain().getAIObject((Unit) it2.next());
                    if (chosenOne.getMission() == null) {
                        break;
                    }
                }
                if (chosenOne != null) {
                    // Check that the colony can be reached:
                    PathNode pn = getGame().getMap().findPath(chosenOne.getUnit(), indianSettlement.getTile(), target.getTile());
                    if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_BRING_GIFT) {
                        chosenOne.setMission(new IndianBringGiftMission(getAIMain(), chosenOne, target));
                    }
                }

            }
        }
    }
    

    /**
    * Takes the necessary actions to secure the settlements.
    * This is done by making new military units or to give
    * existing units new missions.
    */
    private void secureSettlements() {
        Map map = player.getGame().getMap();

        if (!player.isEuropean()) {
            // Determines if we need to move a brave out of the settlement.
            Iterator it = player.getIndianSettlementIterator();
            while (it.hasNext()) {
                IndianSettlement is = (IndianSettlement) it.next();
                
                if (is.getUnitCount() > 2) {
                    int defenders = is.getTile().getUnitCount();
                    int threat = 0;

                    Iterator positionIterator = map.getCircleIterator(is.getTile().getPosition(), true, 2);
                    while (positionIterator.hasNext()) {
                        Tile t = map.getTile((Map.Position) positionIterator.next());
                        if (t.getFirstUnit() != null) {
                            if (t.getFirstUnit().getOwner() == player) {
                                defenders++;
                            } else {
                                if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MAJOR) {
                                    threat = 2;
                                } else if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MINOR){
                                    threat = 1;
                                }
                            }
                        }
                    }

                    if (threat > defenders) {
                        Unit newDefender = is.getFirstUnit();
                        newDefender.setState(Unit.ACTIVE);                        
                        newDefender.setLocation(is.getTile());
                        AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(newDefender);
                        // TODO: Use a mission like; InterceptUnitMission, SeekAndDestroyMission....
                        newDefenderAI.setMission(new UnitWanderHostileMission(getAIMain(), newDefenderAI));
                    }
                }
            }
        }
    }


    /**
    * Called when another <code>Player</code> proposes a trade.
    *
    * @param unit The foreign <code>Unit</code> trying to trade.
    * @param settlement The <code>Settlement</code> this player owns
    *             and which the given <code>Unit</code> if trying to sell
    *             goods.
    * @param goods The goods the given <code>Unit</code> is trying to sell.
    * @param gold The suggested price.
    */
    public int tradeProposition(Unit unit, Settlement settlement, Goods goods, int gold) {
        if (settlement instanceof IndianSettlement) {
            int price;
            if (sessionRegister.containsKey("tradeGold#"+unit.getID())) {
                price = ((Integer) sessionRegister.get("tradeGold#"+unit.getID())).intValue();

                if (price <= 0) {
                    return price;
                }
            } else {
                price = ((IndianSettlement) settlement).getPrice(goods) - player.getTension(unit.getOwner());
                price = Math.min(price, player.getGold()/2);
                if (price <= 0) {
                    return 0;
                }
                sessionRegister.put("tradeGold#"+unit.getID(), new Integer(price));
            }

            if (gold < 0 || price == gold) {
                return price;
            } else if (gold > (player.getGold()*3)/4) {
                sessionRegister.put("tradeGold#"+unit.getID(), new Integer(-1));
                return NetworkConstants.NO_TRADE;
            } else {
                int haggling = 1;
                if (sessionRegister.containsKey("tradeHaggling#"+unit.getID())) {
                    haggling = ((Integer) sessionRegister.get("tradeHaggling#"+unit.getID())).intValue();
                }

                if (random.nextInt(3+haggling) <= 3) {
                    sessionRegister.put("tradeGold#"+unit.getID(), new Integer(gold));
                    sessionRegister.put("tradeHaggling#"+unit.getID(), new Integer(haggling+1));
                    return gold;
                } else {
                    sessionRegister.put("tradeGold#"+unit.getID(), new Integer(-1));
                    return NetworkConstants.NO_TRADE;
                }
            }
        } else {
            throw new IllegalStateException("Trade with colonies not yet implemented!");
        }
    }


    /**
    * Returns an iterator over all the <code>AIUnit</code>s
    * owned by this player.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getAIUnitIterator() {
        ArrayList au = new ArrayList();

        Iterator unitsIterator = player.getUnitIterator();
        while (unitsIterator.hasNext()) {
            Unit theUnit = (Unit) unitsIterator.next();
            au.add(getAIMain().getAIObject(theUnit.getID()));
        }

        return au.iterator();
    }

    
    /**
    * Returns the <code>Player</code> this <code>AIPlayer</code> is controlling.
    */
    public Player getPlayer() {
        return player;
    }


    /**
    * Gets the connection to the server.
    *
    * @return The connection that can be used when communication
    *         with the server.
    */
    public DummyConnection getConnection() {
        return ((DummyConnection) player.getConnection()).getOtherConnection();
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", player.getID());

        return element;
    }

    
    public void readFromXMLElement(Element element) {
        player = (ServerPlayer) getAIMain().getFreeColGameObject(element.getAttribute("ID"));
    }    
    
    
    /**
    * Returns the tag name of the root element representing this object.
    * @return the tag name.
    */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }
}
