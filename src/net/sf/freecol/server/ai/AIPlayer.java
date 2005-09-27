
package net.sf.freecol.server.ai;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.StringWriter;
import java.io.PrintWriter;

import net.sf.freecol.server.ai.mission.*;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.*;
import net.sf.freecol.server.model.*;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.networking.DummyConnection;
import net.sf.freecol.common.networking.Connection;

import org.w3c.dom.*;


/**
* Objects of this class contains AI-information for a single {@link Player}.
*/
public class AIPlayer extends AIObject {
    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());

    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    
    private static final int MAX_DISTANCE_TO_BRING_GIFT = 5;
    private static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;
    

    /* Stores temporary information for sessions (trading with another player etc). */
    private HashMap sessionRegister = new HashMap();


    /**
    * The FreeColGameObject this AIObject contains AI-information for:
    */
    private ServerPlayer player;

    /** Temporary variables: */
    private ArrayList aiUnits = new ArrayList();
    private Connection debuggingConnection;


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

        //logger.info("Entering AI code for: " + player.getNationAsString());

        sessionRegister.clear();
        aiUnits.clear();

        // Determines the stance towards the other players:
        logger.fine("AI: determining stance.");
        playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();
            if ((getPlayer().getNation() == Player.SPANISH) && !p.isEuropean()) {
                // HACK: Spanish should really hate the natives at all times
                getPlayer().setTension(p, 1000);
            }
            if (getPlayer().getTension(p) >= Player.TENSION_ADD_NORMAL) {
                getPlayer().setStance(p, Player.WAR);
            } else {
                getPlayer().setStance(p, Player.PEACE);
            }
        }

        // Abort missions that are no longer valid:
        logger.fine("AI: Finding the AI-units ");
        aiUnitsIterator = getAIUnitIterator();
        logger.fine("AI: Aborting mission which are no longer valid. ");
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid() || aiUnit.getMission() instanceof UnitWanderHostileMission
                    || aiUnit.getMission() instanceof UnitWanderMission) {
                aiUnit.setMission(null);
            }
        }

        // Assign transport missions:
        if (player.isEuropean()) {
            logger.fine("AI: Assigning transport missions to naval units.");

            aiUnitsIterator = getAIUnitIterator();
            while (aiUnitsIterator.hasNext()) {
                AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
                if (aiUnit.getUnit().isNaval() && !aiUnit.hasMission()) {
                    aiUnit.setMission(new TransportMission(getAIMain(), aiUnit));
                }
            }
        }

        logger.fine("AI: Rearranges the workers in the colonies.");
        if (player.isEuropean()) {
            rearrangeWorkersInColonies();
        }

        logger.fine("AI: Securing the settlements.");
        secureSettlements();

        // Find a mission for each unit:
        if (player.isEuropean()) {
            // Create a datastructure for the unit wishes:
            ArrayList[] unitWishes = new ArrayList[Unit.UNIT_COUNT];
            for (int i=0; i<unitWishes.length; i++) {
                if (unitWishes[i] == null) {
                    unitWishes[i] = new ArrayList();
                }
            }
            Iterator aIterator = getAIColonyIterator();
            while (aIterator.hasNext()) {
                Iterator wIterator = ((AIColony) aIterator.next()).getWishIterator();
                while (wIterator.hasNext()) {
                    Wish w = (Wish) wIterator.next();
                    if (w instanceof WorkerWish && w.getTransportable() == null) {
                        unitWishes[((WorkerWish) w).getUnitType()].add(w);
                    }
                }
            }

            logger.fine("AI: Finding a mission for each unit.");
            aiUnitsIterator = getAIUnitIterator();
            while (aiUnitsIterator.hasNext()) {
                AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
                Unit unit = aiUnit.getUnit();

                //if (aiUnit.getUnit().isPioneer() && !aiUnit.hasMission()) {
                if (1==2) {
                    //aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
                } else if (aiUnit.getUnit().isColonist()
                        && !aiUnit.hasMission()
                        && (aiUnit.getUnit().getLocation() instanceof Tile
                        || aiUnit.getUnit().getLocation() instanceof Unit)) {
                    // Check if this unit is needed as an expert (using: "WorkerWish"):
                    ArrayList wishList = unitWishes[aiUnit.getUnit().getType()];
                    WorkerWish bestWish = null;
                    int bestTurns = Integer.MAX_VALUE;
                    for (int i=0; i<wishList.size(); i++) {
                        WorkerWish ww = (WorkerWish) wishList.get(i);
                        int turns = unit.getTurnsToReach(ww.getDestination().getTile());
                        if (bestWish == null
                                || turns < bestTurns
                                || turns == bestTurns && ww.getValue() > bestWish.getValue()) {
                            bestWish = ww;
                            bestTurns = turns;
                        }
                    }
                    if (bestWish != null) {
                        bestWish.setTransportable(aiUnit);
                        aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                        continue;
                    }
                    // Find a site for a new colony:
                    Tile colonyTile = findColonyLocation(aiUnit.getUnit());
                    if (colonyTile != null) {
                        bestTurns = unit.getTurnsToReach(colonyTile);
                    }
                    // Check if we can find a better site to work than a new colony:
                    for (int i=0; i<unitWishes.length; i++) {
                        wishList = unitWishes[i];
                        for (int j=0; j<wishList.size(); j++) {
                            WorkerWish ww = (WorkerWish) wishList.get(j);
                            int turns = unit.getTurnsToReach(ww.getDestination().getTile());
                            // TODO: Choose to build colony if the value of the wish is low.
                            if (bestWish == null && turns < bestTurns || bestWish != null && (
                                    turns < bestTurns
                                    || turns == bestTurns && ww.getValue() > bestWish.getValue())) {
                                bestWish = ww;
                                bestTurns = turns;
                            }
                        }
                    }
                    if (bestWish != null) {
                        bestWish.setTransportable(aiUnit);
                        aiUnit.setMission(new WishRealizationMission(getAIMain(), aiUnit, bestWish));
                        continue;
                    }

                    // Choose to build a new colony:
                    if (colonyTile != null) {
                        aiUnit.setMission(new BuildColonyMission(getAIMain(), aiUnit, colonyTile, colonyTile.getColonyValue()));
                        if (aiUnit.getUnit().getLocation() instanceof Unit) {
                            AIUnit carrier = (AIUnit) getAIMain().getAIObject((FreeColGameObject) aiUnit.getUnit().getLocation());
                            ((TransportMission) carrier.getMission()).addToTransportList(aiUnit);
                        }
                        continue;
                    }
                }
            }
        }

        // Bring gifts to nice players:
        if (!player.isEuropean()) {
            logger.fine("AI: Bringing gifts to nice players.");
            bringGifts();
        }

        // Assign a mission to every unit:
        logger.fine("AI: Assigning a simple mission to the remaining units.");
        aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (!aiUnit.hasMission() && aiUnit.getUnit().getLocation() instanceof Tile) {
                aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            }
        }

        if (player.isEuropean()) {
            createAIGoodsInColonies();
            createTransportLists();
        }

        // Make every unit perform their mission:
        logger.fine("AI: Making every unit perform their mission.");
        aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.hasMission() && (aiUnit.getUnit().getLocation() instanceof Tile
                    || aiUnit.getUnit().getLocation() instanceof Unit
                    || aiUnit.getUnit().isNaval() && aiUnit.getUnit().getLocation() instanceof Europe)
                    && aiUnit.getMission().isValid()) {
                try {
                    aiUnit.doMission(getConnection());
                } catch  (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());
                }
            }
        }
        
        aiUnits.clear();
        logger.fine("AI: Rearranges the workers in the colonies.");
        rearrangeWorkersInColonies();        
        logger.fine("AI: Leaving");
    }


    /**
    * Maps <code>Transportable</code>s to carrier's using a
    * <code>TransportMission</code>.
    */
    private void createTransportLists() {
        List transportables = new ArrayList();
        // Add units
        Iterator aui = getAIUnitIterator();
        while (aui.hasNext()) {
            AIUnit au = (AIUnit) aui.next();
            if (au.getTransportDestination() != null && au.getTransport() == null) {
                transportables.add(au);
            }
        }
        // Add goods
        Iterator aci = getAIColonyIterator();
        while (aci.hasNext()) {
            AIColony ac = (AIColony) aci.next();
            Iterator agi = ac.getAIGoodsIterator();
            while (agi.hasNext()) {
                AIGoods ag = (AIGoods) agi.next();
                if (ag.getTransportDestination() != null && ag.getTransport() == null) {
                    transportables.add(ag);
                }
            }
        }
        Collections.sort(transportables, new Comparator() {
            public int compare(Object o1, Object o2) {
                Integer i = new Integer(((Transportable) o1).getTransportPriority());
                Integer j = new Integer(((Transportable) o2).getTransportPriority());

                return j.compareTo(i);
            }
        });

        List vacantTransports = new ArrayList();
        Iterator iter = getAIUnitIterator();
        while (iter.hasNext()) {
            AIUnit au = (AIUnit) iter.next();
            if (au.hasMission() && au.getMission() instanceof TransportMission
                    && !(au.getUnit().getLocation() instanceof Europe)) {
                vacantTransports.add(au.getMission());
            }
        }

        Iterator ti = transportables.iterator();
        while (ti.hasNext()) {
            Transportable t = (Transportable) ti.next();
            t.increaseTransportPriority();
            if (t.getTransportLocatable().getLocation() instanceof Unit) {
                Mission m = ((AIUnit) getAIMain().getAIObject((FreeColGameObject) t.getTransportLocatable().getLocation())).getMission();
                if (m instanceof TransportMission) {
                    ((TransportMission) m).addToTransportList(t);
                }
                ti.remove();
            }
        }
        while (transportables.size() > 0) {
            Transportable t = (Transportable) transportables.get(0);

            TransportMission bestTransport = null;
            int bestTransportSpace = 0;
            int bestTransportTurns = Integer.MAX_VALUE;
            for (int i=0; i<vacantTransports.size(); i++) {
                TransportMission tm = (TransportMission) vacantTransports.get(i);
                if (t.getTransportSource().getTile() == tm.getUnit().getLocation().getTile()) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = 0;
                        break;
                    } else {
                        continue;
                    }
                }
                PathNode path = tm.getPath(t);
                if (path != null && path.getTotalTurns() <= bestTransportTurns) {
                    int transportSpace = tm.getAvailableSpace(t);
                    if (transportSpace > 0 && (path.getTotalTurns() < bestTransportTurns
                            || transportSpace > bestTransportSpace)) {
                        bestTransport = tm;
                        bestTransportSpace = transportSpace;
                        bestTransportTurns = path.getTotalTurns();
                    }
                }
            }
            if (bestTransport == null) {
                // No more transports available:
                break;
            }
            bestTransport.addToTransportList(t);
            transportables.remove(t);
            vacantTransports.remove(bestTransport);
            bestTransportSpace--;

            for (int i=0; i<transportables.size() && bestTransportSpace > 0; i++) {
                Transportable t2 = (Transportable) transportables.get(0);
                if (t2.getTransportLocatable().getLocation().getTile() == t.getTransportLocatable().getLocation().getTile()) {
                    bestTransport.addToTransportList(t2);
                    transportables.remove(t2);
                    bestTransportSpace--;
                }
            }
        }
    }


    /**
    * Returns an <code>Iterator</code> for all the wishes.
    * The items are sorted by the {@link Wish#getValue value},
    * with the item having the highest value appearing 
    * first in the <code>Iterator</code>.
    *
    * @return The <code>Iterator</code>.
    * @see Wish
    */
    public Iterator getWishIterator() {
        ArrayList wishList = new ArrayList();
        Iterator ai = getAIColonyIterator();
        while (ai.hasNext()) {
            AIColony ac = (AIColony) ai.next();
            Iterator wishIterator = ac.getWishIterator();
            while (wishIterator.hasNext()) {
                Wish w = (Wish) wishIterator.next();
                wishList.add(w);
            }
        }

        Collections.sort(wishList, new Comparator() {
            public int compare(Object o1, Object o2) {
                Integer a = new Integer(((Wish) o1).getValue());
                Integer b = new Integer(((Wish) o2).getValue());
                return b.compareTo(a);
            }
        });
        return wishList.iterator();
    }


    /**
    * Calls {@link Colony#rearrangeWorkersInColonies} for every colony
    * this player owns.
    */
    private void rearrangeWorkersInColonies() {
        Iterator ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = (AIColony) ci.next();
            c.rearrangeWorkers();
        }
    }


    /**
    * Calls {@link Colony#createAIGoodsInColonies} for every colony
    * this player owns.
    */
    private void createAIGoodsInColonies() {
        Iterator ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = (AIColony) ci.next();
            c.createAIGoods();
        }
    }


    /**
    * Finds a site for a new colony.
    */
    private Tile findColonyLocation(Unit unit) {
        Tile bestTile = null;
        int highestColonyValue = 0;

        Iterator it = getGame().getMap().getFloodFillIterator(unit.getTile().getPosition());
        for (int i=0; it.hasNext() && i<500; i++) {
            Tile tile = (Tile) getGame().getMap().getTile((Map.Position) it.next());
            if (tile.getColonyValue() > 0) {
                if (tile != unit.getTile()) {
                    PathNode path;
                    if (unit.getLocation() instanceof Unit) {
                        Unit carrier = (Unit) unit.getLocation();
                        path = getGame().getMap().findPath(unit, carrier.getTile(), tile, carrier);
                    } else {
                        path = getGame().getMap().findPath(unit, unit.getTile(), tile);
                    }

                    if (path != null) {
                        int newColonyValue = 10000 + tile.getColonyValue() - path.getTotalTurns() 
                                * ((unit.getGame().getTurn().getNumber() < 10 && unit.getLocation() instanceof Unit) ? 25 : 4);
                        if (newColonyValue > highestColonyValue) {
                            highestColonyValue = newColonyValue;
                            bestTile = tile;
                        }
                    }
                } else {
                    int newColonyValue = 10000 + tile.getColonyValue();
                    if (newColonyValue > highestColonyValue) {
                        highestColonyValue = newColonyValue;
                        bestTile = tile;
                    }
                }
            }
        }

        if (bestTile != null && bestTile.getColonyValue() > 0) {
            return bestTile;
        } else {
            return null;
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

            // Do not bring gifts all the time:
            if (getRandom().nextInt(10) != 1) {
                continue;
            }

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
                if (t.getColony() != null && IndianBringGiftMission.isValidMission(getPlayer(), t.getColony().getOwner())) {
                    nearbyColonies.add(t.getColony());
                }
            }
            if (nearbyColonies.size() > 0) {
                Colony target = (Colony) nearbyColonies.get(getRandom().nextInt(nearbyColonies.size()));
                Iterator it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = (AIUnit) getAIMain().getAIObject((Unit) it2.next());
                    if (!(chosenOne.getUnit().getLocation() instanceof Tile)) {
                        chosenOne = null;
                    } else if (chosenOne.getMission() == null) {
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
                    int worstThreat = 0;
                    FreeColGameObject bestTarget = null;

                    Iterator positionIterator = map.getCircleIterator(is.getTile().getPosition(), true, 2);
                    while (positionIterator.hasNext()) {
                        Tile t = map.getTile((Map.Position) positionIterator.next());
                        if (t.getFirstUnit() != null) {
                            if (t.getFirstUnit().getOwner() == player) {
                                defenders++;
                            } else {
                                if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MAJOR) {
                                    threat += 2;
                                    if (t.getUnitCount() * 2 > worstThreat) {
                                        if (t.getSettlement() != null) {
                                            bestTarget = t.getSettlement();
                                        } else {
                                            bestTarget = t.getFirstUnit();
                                        }
                                        worstThreat = t.getUnitCount() * 2;
                                    }
                                } else if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MINOR) {
                                    threat += 1;
                                    if (t.getUnitCount() > worstThreat) {
                                        if (t.getSettlement() != null) {
                                            bestTarget = t.getSettlement();
                                        } else {
                                            bestTarget = t.getFirstUnit();
                                        }
                                        worstThreat = t.getUnitCount();
                                    }
                                }
                            }
                        }
                    }

                    if (threat > defenders) {
                        Unit newDefender = is.getFirstUnit();
                        newDefender.setState(Unit.ACTIVE);                        
                        newDefender.setLocation(is.getTile());
                        AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(newDefender);
                        if (bestTarget != null) {
                            newDefenderAI.setMission(new UnitSeekAndDestroyMission(getAIMain(), newDefenderAI, bestTarget));
                        } else {
                            newDefenderAI.setMission(new UnitWanderHostileMission(getAIMain(), newDefenderAI));
                        }
                    }
                }
            }

            // This is the end of the native code.
            return;
        }

        // Ok, we are a European player. Things are about to get fun.
        Iterator it = player.getColonyIterator();

        while (it.hasNext()) {
            Colony colony = (Colony)(it.next());

            int olddefenders = 0;
            int defenders = 0;
            int threat = 0;
            int worstThreat = 0;
            FreeColGameObject bestTarget = null;

            Iterator ui = colony.getTile().getUnitIterator();
            while (ui.hasNext()) {
                if (((Unit)(ui.next())).isOffensiveUnit()) {
                    defenders++;
                }
            }

            Iterator positionIterator = map.getCircleIterator(colony.getTile().getPosition(), true, 5);
            while (positionIterator.hasNext()) {
                Tile t = map.getTile((Map.Position) positionIterator.next());
                if (t.getFirstUnit() != null) {
                    if (t.getFirstUnit().getOwner() == player) {
                        Iterator uit = t.getUnitIterator();
                        while (uit.hasNext()) {
                            if (((Unit)(uit.next())).isOffensiveUnit()) {
                                defenders++;
                            }
                        }
                    } else {
                        int thisThreat = 0;
                        if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MAJOR) {
                            Iterator uit = t.getUnitIterator();
                            while (uit.hasNext()) {
                                if (((Unit)(uit.next())).isOffensiveUnit()) {
                                    thisThreat += 2;
                                }
                            }
                        } else if (player.getTension(t.getFirstUnit().getOwner()) >= Player.TENSION_ADD_MINOR) {
                            Iterator uit = t.getUnitIterator();
                            while (uit.hasNext()) {
                                if (((Unit)(uit.next())).isOffensiveUnit()) {
                                    thisThreat++;
                                }
                            }
                        }
                        threat += thisThreat;
                        if (thisThreat > worstThreat) {
                            if (t.getSettlement() != null) {
                                bestTarget = t.getSettlement();
                            } else {
                                bestTarget = t.getFirstUnit();
                            }
                            worstThreat = thisThreat;
                        }
                    }
                }
            }

            olddefenders = defenders;

            if (colony.getBuilding(Building.STOCKADE).isBuilt()) {
                defenders += (defenders * (colony.getBuilding(Building.STOCKADE).getLevel()) / 2);
            }

            if (threat > defenders) {
                // We're under attaaaaaaaaack! Man the stockade!
                ArrayList vets = new ArrayList();
                ArrayList criminals = new ArrayList();
                ArrayList servants = new ArrayList();
                ArrayList colonists = new ArrayList();
                ArrayList experts = new ArrayList();

                int inColonyCount = 0;

                // Let's make some more soldiers, if we can.
                // First, find some people we can recruit.
                ui = colony.getUnitIterator();
                while (ui.hasNext()) {
                    Unit u = (Unit)(ui.next());
                    if (u.isOffensiveUnit()) {
                        continue; //don't bother dealing with current soldiers at the moment
                    }
                    if (u.getLocation() != colony.getTile()) {
                        // If we are not on the tile we are in the colony.
                        inColonyCount++;
                    }
                    if (u.getType() == Unit.VETERAN_SOLDIER) {
                        vets.add(u);
                    } else if (u.getType() == Unit.PETTY_CRIMINAL) {
                        criminals.add(u);
                    } else if (u.getType() == Unit.INDENTURED_SERVANT) {
                        servants.add(u);
                    } else if (u.getType() == Unit.FREE_COLONIST) {
                        colonists.add(u);
                    } else if (u.isColonist()) {
                        experts.add(u);
                    }
                }

                ArrayList recruits = new ArrayList(vets);
                recruits.addAll(criminals);
                recruits.addAll(servants);
                recruits.addAll(colonists);
                recruits.addAll(experts);
                
                // Don't overdo it - leave at least one person behind.
                int recruitCount = threat - defenders;
                if (recruitCount > recruits.size() - 1) {
                    recruitCount = recruits.size() - 1;
                }
                if (recruitCount > inColonyCount - 1) {
                    recruitCount = inColonyCount - 1;
                }

                // Actually go through and arm our people.
                boolean needMuskets = false;
                boolean needHorses = false;

                    ui = recruits.iterator();
                    while (ui.hasNext() && recruitCount > 0) {

                        Unit u = (Unit)(ui.next());
                        if (u.canArm()) {
                            recruitCount--;
                            Element equipUnitElement = Message.createNewRootElement("equipunit");
                            equipUnitElement.setAttribute("unit", u.getID());
                            equipUnitElement.setAttribute("type", Integer.toString(Goods.MUSKETS));
                            equipUnitElement.setAttribute("amount", "50");
                            // I don't think we need to do this if we are the server...
                            //unit.setArmed(true);
                            try {
                                getConnection().sendAndWait(equipUnitElement);
                            } catch (IOException e) {
                                logger.warning("Couldn't send an AI element!");
                            }

                            Element putOutsideColonyElement = Message.createNewRootElement("putOutsideColony");
                            putOutsideColonyElement.setAttribute("unit", u.getID());
                            // I don't think we need to do this if we are the server...
                            //u.putOutsideColony();
                            try {
                                getConnection().sendAndWait(putOutsideColonyElement);
                            } catch (IOException e) {
                                logger.warning("Couldn't send an AI element!");
                            }

                            Element changeStateElement = Message.createNewRootElement("changeState");
                            changeStateElement.setAttribute("unit", u.getID());
                            changeStateElement.setAttribute("state", Integer.toString(Unit.FORTIFY));
                            //u.putOutsideColony();
                            try {
                                getConnection().sendAndWait(changeStateElement);
                            } catch (IOException e) {
                                logger.warning("Couldn't send an AI element!");
                            }

                            olddefenders++;

                            if (u.canMount()) {
                                equipUnitElement = Message.createNewRootElement("equipunit");
                                equipUnitElement.setAttribute("unit", u.getID());
                                equipUnitElement.setAttribute("type", Integer.toString(Goods.HORSES));
                                equipUnitElement.setAttribute("amount", "50");
                                // I don't think we need to do this if we are the server...
                                //unit.setArmed(true);
                                try {
                                    getConnection().sendAndWait(equipUnitElement);
                                } catch (IOException e) {
                                    logger.warning("Couldn't send an AI element!");
                                }
                            } else {
                                needHorses = true;
                            }
                        } else {
                            needMuskets = true;
                            break;
                        }
                    }

                AIColony ac = null;
                if (needMuskets || needHorses) {
                    Iterator aIterator = getAIColonyIterator();
                    while (aIterator.hasNext()) {
                        AIColony temp = (AIColony)aIterator.next();
                        if (temp != null && temp.getColony() == colony) {
                            ac = temp;
                            break;
                        }
                    }
                }

                if (needMuskets && ac != null) {
                    // Check and see if we have already made a GoodsWish for here.
                    Iterator wishes = ac.getWishIterator();
                    boolean made = false;
                    while (wishes.hasNext()) {
                        Wish w = (Wish) wishes.next();
                        if (!(w instanceof GoodsWish)) {
                            continue;
                        }
                        GoodsWish gw = (GoodsWish) w;
                        if (gw == null) {
                            continue;
                        }
                        if (gw.getGoodsType() == Goods.MUSKETS) {
                            made = true;
                        }
                    }
                    if (made == false) {
                        //Add a new GoodsWish onto the stack.
                        ac.addGoodsWish(new GoodsWish(getAIMain(), colony, (threat - olddefenders) * 50, Goods.MUSKETS));
                    }
                }
                if (needHorses && ac != null) {
                    // Check and see if we have already made a GoodsWish for here.
                    Iterator wishes = ac.getWishIterator();
                    boolean made = false;
                    while (wishes.hasNext()) {
                        Wish w = (Wish) wishes.next();
                        if (!(w instanceof GoodsWish)) {
                            continue;
                        }
                        GoodsWish gw = (GoodsWish) w;
                        if (gw == null) {
                            continue;
                        }
                        if (gw.getGoodsType() == Goods.HORSES) {
                            made = true;
                        }
                    }
                    if (made == false) {
                        //Add a new GoodsWish onto the stack.
                        ac.addGoodsWish(new GoodsWish(getAIMain(), colony, (threat - defenders) * 50, Goods.HORSES));
                    }
                }

                defenders = olddefenders;
                if (colony.getBuilding(Building.STOCKADE).isBuilt()) {
                    defenders += (defenders * (colony.getBuilding(Building.STOCKADE).getLevel()) / 2);
                }
            }

            if (defenders > (threat * 2)) {
                // We're so big and tough, we can go wipe out this threat.
                // Pick someone to go make it happen.
                Unit u = null;
                Iterator uit = colony.getUnitIterator();
                while (uit.hasNext()) {
                    Unit candidate = (Unit)(uit.next());
                    if (candidate.isOffensiveUnit() && candidate.getState() == Unit.FORTIFY) {
                        u = candidate;
                        break;
                    }
                }
                if (u != null) {
                    u.setState(Unit.ACTIVE);                        
                    u.setLocation(colony.getTile());
                    AIUnit newDefenderAI = (AIUnit) getAIMain().getAIObject(u);
                    if (bestTarget != null) {
                        newDefenderAI.setMission(new UnitSeekAndDestroyMission(getAIMain(), newDefenderAI, bestTarget));
                    } else {
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

                if (getRandom().nextInt(3+haggling) <= 3) {
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
        if (aiUnits.size() == 0) {
            ArrayList au = new ArrayList();

            Iterator unitsIterator = player.getUnitIterator();
            while (unitsIterator.hasNext()) {
                Unit theUnit = (Unit) unitsIterator.next();
                AIObject a = getAIMain().getAIObject(theUnit.getID());
                if (a != null) {
                    au.add(a);
                } else {
                    logger.warning("Could not find the AIUnit for: " + theUnit);
                }
            }

            aiUnits = au;
        }

        return aiUnits.iterator();
    }


    /**
    * Returns an iterator over all the <code>AIColony</code>s
    * owned by this player.
    *
    * @return The <code>Iterator</code>.
    */
    public Iterator getAIColonyIterator() {
        ArrayList ac = new ArrayList();

        Iterator colonyIterator = player.getColonyIterator();
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            AIObject a = getAIMain().getAIObject(colony.getID());
            if (a != null) {
                ac.add(a);
            } else {
                logger.warning("Could not find the AIColony for: " + colony);
            }
        }

        return ac.iterator();
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
    public Connection getConnection() {
        if (debuggingConnection != null) {
            return debuggingConnection;
        } else {
            return ((DummyConnection) player.getConnection()).getOtherConnection();
        }
    }
    
    
    /**
    * Sets the <code>Connection</code> to be used while communicating with the server.
    * This method is only used for debugging.
    */
    public void setDebuggingConnection(Connection debuggingConnection) {
        this.debuggingConnection = debuggingConnection;
    }


    /**
    * Returns the ID for this <code>AIPlayer</code>.
    * This is the same as the ID for the {@link Player}
    * this <code>AIPlayer</code> controls.
    *
    * @return The ID.
    */
    public String getID() {
        return player.getID();
    }


    public Element toXMLElement(Document document) {
        Element element = document.createElement(getXMLElementTagName());

        element.setAttribute("ID", getID());

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
