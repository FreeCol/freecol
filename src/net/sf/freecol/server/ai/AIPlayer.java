
package net.sf.freecol.server.ai;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.GoalDecider;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Monarch;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.common.networking.NetworkConstants;
import net.sf.freecol.server.ai.mission.BuildColonyMission;
import net.sf.freecol.server.ai.mission.CashInTreasureTrainMission;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.IndianBringGiftMission;
import net.sf.freecol.server.ai.mission.IndianDemandMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.PioneeringMission;
import net.sf.freecol.server.ai.mission.ScoutingMission;
import net.sf.freecol.server.ai.mission.TransportMission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.ai.mission.UnitWanderHostileMission;
import net.sf.freecol.server.ai.mission.UnitWanderMission;
import net.sf.freecol.server.ai.mission.WishRealizationMission;
import net.sf.freecol.server.ai.mission.WorkInsideColonyMission;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.networking.DummyConnection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single {@link Player}
 * and is used for controlling this player.
 * 
 * <br /><br />
 * 
 * The method {@link #startWorking} gets called by the 
 * {@link AIInGameInputHandler} when it is this player's turn.
 */
public class AIPlayer extends AIObject {
    private static final Logger logger = Logger.getLogger(AIPlayer.class.getName());
    
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    private static final int MAX_DISTANCE_TO_BRING_GIFT = 5;
    private static final int MAX_NUMBER_OF_GIFTS_BEING_DELIVERED = 1;
    
    private static final int MAX_DISTANCE_TO_MAKE_DEMANDS = 5;
    private static final int MAX_NUMBER_OF_DEMANDS = 1;
    
    public static final int STRATEGY_NONE = 0,
        STRATEGY_TRADE = 1,
        STRATEGY_IMMIGRATION = 2,
        STRATEGY_COOPERATION = 3,
        STRATEGY_CONQUEST = 4;
    
    /** The strategy of this player. */
    private int strategy = STRATEGY_NONE;
    
    /* Stores temporary information for sessions (trading with another player etc). */
    private HashMap sessionRegister = new HashMap();
        
    
    /**
     * The FreeColGameObject this AIObject contains AI-information for.
     */
    private ServerPlayer player;
    
    /** Temporary variable. */
    private ArrayList aiUnits = new ArrayList();
    
    /** Temporary variable. */
    private Connection debuggingConnection;
    
    
    /**
     * Creates a new <code>AIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *        <code>AIPlayer</code>.
     */
    public AIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player.getID());
         
        this.player = player;        
        switch (player.getNation()) {
        case Player.DUTCH:
            this.strategy = STRATEGY_TRADE; break;
        case Player.ENGLISH:
            this.strategy = STRATEGY_IMMIGRATION; break;
        case Player.FRENCH:
            this.strategy = STRATEGY_COOPERATION; break;
        case Player.SPANISH:
            this.strategy = STRATEGY_CONQUEST; break;
        }
    }
    
    
    /**
     * Creates a new <code>AIPlayer</code> and reads the information
     * from the given <code>Element</code>.
     *
     * @param aiMain The main AI-class.
     * @param element The XML-element containing information.
     */
    public AIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element.getAttribute("ID"));
        readFromXMLElement(element);
    }
    
    /**
     * Creates a new <code>AIPlayer</code>.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     */
    public AIPlayer(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain, in.getAttributeValue(null, "ID"));
        readFromXML(in);
    }
    
    
    /**
     * Tells this <code>AIPlayer</code> to make decisions.
     * The <code>AIPlayer</code> is done doing work this turn
     * when this method returns.
     */
    public void startWorking() {
        //logger.info("Entering AI code for: " + player.getNationAsString());

        sessionRegister.clear();
        aiUnits.clear();
        
        if (getPlayer().isREF()) {
            checkForREFDefeat();
            
            if (!isWorkForREF()) {
                return;
            }
        }        

        cheat();        
        determineStances();
        moveREFToDocks();
        rearrangeWorkersInColonies();  
        abortInvalidAndOneTimeMissions();        
        ensureCorrectMissions();        
        giveNavalMissions();      
        secureSettlements();        
        giveNormalMissions(); 
        bringGifts();
        demandTribute();       
        createAIGoodsInColonies();
        createTransportLists();
        doMissions();
        rearrangeWorkersInColonies();
        abortInvalidMissions();        
        ensureCorrectMissions();   
        
        aiUnits.clear();
    }
    
    /**
     * For REF-players: Checks if we have lost the war of independence.
     */
    private void checkForREFDefeat() {
        if (!getPlayer().isREF()) {
            return;
        }
        
        boolean defeated = true;
        Iterator it = getPlayer().getUnitIterator();
        while (it.hasNext()) {
            Unit u = (Unit) it.next();             
            if (u.getType() != Unit.MAN_O_WAR) {
                defeated = false;
                break;
            }
        }          
        if (defeated) {
            Iterator it2 = getGame().getPlayerIterator();
            while (it2.hasNext()) {
                Player p = (Player) it2.next();
                if (p.getREFPlayer() == getPlayer() 
                        && p.getRebellionState() == Player.REBELLION_IN_WAR
                        && p.getMonarch() == null) {           
                    Element giveIndependenceElement = Message.createNewRootElement("giveIndependence");
                    giveIndependenceElement.setAttribute("player", p.getID());
                    try {
                        getConnection().sendAndWait(giveIndependenceElement);
                    } catch (IOException e) {
                        logger.warning("Could not send \"giveIndependence\"-message.");                            
                    }
                }                    
            }
        }
    }

    /**
     * Cheats for the AI :-)
     */
    private void cheat() {
        // TODO-AI-CHEATING: REMOVE WHEN THE AI IS GOOD ENOUGH:
        if (getAIMain().getFreeColServer().isSingleplayer()
                && player.isEuropean() 
                && !player.isREF() 
                && player.isAI()
                && player.getRebellionState() == Player.REBELLION_PRE_WAR) {
            if (getRandom().nextInt(40) == 21) {
                player.modifyGold(Unit.getPrice(Unit.EXPERT_ORE_MINER));
                player.modifyGold(getGame().getMarket().getBidPrice(Goods.MUSKETS, 50));
                player.modifyGold(getGame().getMarket().getBidPrice(Goods.HORSES, 50));
                
                try {
                    Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
                    trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(Unit.EXPERT_ORE_MINER));                    
                    Element reply = getConnection().ask(trainUnitInEuropeElement);
                    Unit unit = (Unit) getGame().getFreeColGameObject(((Element) reply.getChildNodes().item(0)).getAttribute("ID"));
                    
                    Element clearSpecialityElement = Message.createNewRootElement("clearSpeciality");
                    clearSpecialityElement.setAttribute("unit", unit.getID());
                    getConnection().sendAndWait(clearSpecialityElement);
                    
                    Element equipMusketsElement = Message.createNewRootElement("equipunit");
                    equipMusketsElement.setAttribute("unit", unit.getID());
                    equipMusketsElement.setAttribute("type", Integer.toString(Goods.MUSKETS));
                    equipMusketsElement.setAttribute("amount", Integer.toString(50));  
                    getConnection().sendAndWait(equipMusketsElement);
                    
                    Element equipHorsesElement = Message.createNewRootElement("equipunit");
                    equipHorsesElement.setAttribute("unit", unit.getID());
                    equipHorsesElement.setAttribute("type", Integer.toString(Goods.HORSES));
                    equipHorsesElement.setAttribute("amount", Integer.toString(50));  
                    getConnection().sendAndWait(equipHorsesElement);                    
                } catch (IOException e) {
                    logger.warning("Could not create dragoon.");
                }                
            }
            if (getRandom().nextInt(100) == 42) {                
                int unitType = Unit.CARAVEL;
                switch (getRandom().nextInt(10)) {
                case 1:
                case 2: 
                case 3: 
                case 4: 
                    unitType = Unit.CARAVEL;
                    break;
                case 5:
                case 6:
                case 7:
                    unitType = Unit.MERCHANTMAN;
                    break;
                case 8: 
                    unitType = Unit.GALLEON;
                    break;
                case 9: 
                    unitType = Unit.PRIVATEER;
                    break;
                case 10: 
                    unitType = Unit.FRIGATE;
                    break;
                }
                player.modifyGold(Unit.getPrice(unitType));
                Element trainUnitInEuropeElement = Message.createNewRootElement("trainUnitInEurope");
                trainUnitInEuropeElement.setAttribute("unitType", Integer.toString(unitType));
                
                try {
                    getConnection().ask(trainUnitInEuropeElement);
                } catch (IOException e) {
                    logger.warning("Could not buy the ship.");
                }
            }
        }
    }

    /**
     * Ensures that all workers inside a colony gets a
     * {@link WorkInsideColonyMission}.
     */
    private void ensureCorrectMissions() {
        if (player.isIndian()) {
            return;
        }
        Iterator it = getAIUnitIterator();
        while (it.hasNext()) {
            AIUnit au = (AIUnit) it.next();
            if (!au.hasMission() &&
                    (au.getUnit().getLocation() instanceof ColonyTile 
                    || au.getUnit().getLocation() instanceof Building)) {
                AIColony ac = (AIColony) getAIMain().getAIObject(au.getUnit().getTile().getColony());
                au.setMission(new WorkInsideColonyMission(getAIMain(), au, ac));               
            }
        }
    }
    
    /**
     * Checks if this player has work to do (provided it is an REF-player).
     * @return <code>true</code> if any of our units are located in the new world
     *      or if a puppet-nation has declared independence.
     */
    private boolean isWorkForREF() {
        Iterator it = getPlayer().getUnitIterator();
        while (it.hasNext()) {
            if (((Unit) it.next()).getTile() != null) {
                return true;
            }
        }
        
        Iterator it2 = getGame().getPlayerIterator();
        while (it2.hasNext()) {
            Player p = (Player) it2.next();
            if (p.getREFPlayer() == getPlayer() 
                    && p.getRebellionState() == Player.REBELLION_IN_WAR) {
                return true;
            }
                
        }
        
        return false;
    }
    
    /**
     * Determines the stances towards each player.
     * That is: should we declare war? 
     */
    private void determineStances() {
        Iterator playerIterator = getGame().getPlayerIterator();
        while (playerIterator.hasNext()) {
            Player p = (Player) playerIterator.next();
            if (p.getREFPlayer() == getPlayer() 
                    && p.getRebellionState() == Player.REBELLION_IN_WAR) {
                getPlayer().getTension(p).modify(1000);
            }            
            if (strategy == STRATEGY_CONQUEST && p.isEuropean()) {
                getPlayer().getTension(p).modify(10);
            }
            if (getPlayer().getStance(p) != Player.WAR
                    && getPlayer().getTension(p).getLevel() >= Tension.HATEFUL) {
                getPlayer().setStance(p, Player.WAR);
            } else if (getPlayer().getStance(p) == Player.WAR
                    && getPlayer().getTension(p).getLevel() <= Tension.CONTENT) {
                getPlayer().setStance(p, Player.CEASE_FIRE);
            } else if (getPlayer().getStance(p) == Player.CEASE_FIRE
                    && getPlayer().getTension(p).getLevel() <= Tension.HAPPY) {
                getPlayer().setStance(p, Player.PEACE);
            }
        }
    }
    
    /**
     * Moves the "Royal Expeditionary Force" to the docks.
     * This method will just return if this player is
     * not a REF-player.
     */
    private void moveREFToDocks() {        
        Iterator it = getGame().getPlayerIterator();
        while (it.hasNext()) {
            Player p = (Player) it.next();
            if (p.getREFPlayer() == getPlayer() 
                    && p.getRebellionState() == Player.REBELLION_IN_WAR) {
                Monarch m = p.getMonarch();
                if (m == null) {
                    continue;
                }
                int[] ref = m.getREF();
                int totalNumber = 0;
                for (int i=0; i<ref.length; i++) {
                    totalNumber += ref[i];
                }
                while (totalNumber > 0) {
                    int i = getRandom().nextInt(ref.length);
                    if (ref[i] <= 0) {
                        continue;
                    }
                    int unitType;
                    boolean armed = false;
                    boolean mounted = false;
                    if (i == Monarch.INFANTRY) {
                        unitType = Unit.KINGS_REGULAR;
                        armed = true;
                    } else if (i == Monarch.DRAGOON) {
                        unitType = Unit.KINGS_REGULAR;                        
                        armed = true;
                        mounted = true;
                    } else if (i == Monarch.ARTILLERY) {
                        unitType = Unit.ARTILLERY;
                    } else if (i == Monarch.MAN_OF_WAR) {
                        unitType = Unit.MAN_O_WAR;
                    } else {
                        logger.warning("Unsupported REF-unit.");
                        continue;
                    }
                    Unit newUnit = new Unit(getGame(), 
                            getPlayer().getEurope(), 
                            getPlayer(), 
                            unitType,
                            Unit.ACTIVE,
                            armed,
                            mounted,
                            0,
                            false);                    
                    ref[i]--;
                    totalNumber--;
                }
                p.setMonarch(null);
            }
        }        
    }
    
    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidMissions() {
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid()) {
                aiUnit.setMission(null);
            }
        }
    }
    
    /**
     * Aborts all the missions which are no longer valid.
     */
    private void abortInvalidAndOneTimeMissions() {
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.getMission() == null) {
                continue;
            }
            if (!aiUnit.getMission().isValid() 
                    || aiUnit.getMission() instanceof UnitWanderHostileMission
                    || aiUnit.getMission() instanceof UnitWanderMission
                    //|| aiUnit.getMission() instanceof DefendSettlementMission
                    //|| aiUnit.getMission() instanceof UnitSeekAndDestroyMission
                    ) {
                aiUnit.setMission(null);
            }
        }
    }
    
    /**
     * Gives missions to all the naval units this player owns.
     */
    private void giveNavalMissions() {
        if (!player.isEuropean()) {
            return;
        }
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.getUnit().isNaval() && !aiUnit.hasMission()) {
                aiUnit.setMission(new TransportMission(getAIMain(), aiUnit));
            }
        }
    }
    
    /**
     * Calls {@link AIColony#rearrangeWorkers} for every colony
     * this player owns.
     */
    private void rearrangeWorkersInColonies() {
        if (!player.isEuropean()) {
            return;
        }
        Iterator ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = (AIColony) ci.next();
            c.rearrangeWorkers(getConnection());
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
                    Location bestTarget = null;
                    
                    Iterator positionIterator = map.getCircleIterator(is.getTile().getPosition(), true, 2);
                    while (positionIterator.hasNext()) {
                        Tile t = map.getTile((Map.Position) positionIterator.next());
                        if (t.getFirstUnit() != null) {
                            if (t.getFirstUnit().getOwner() == player) {
                                defenders++;
                            } else {
                                if (player.getTension(t.getFirstUnit().getOwner()).getValue() >=
                                    Tension.TENSION_ADD_MAJOR) {
                                    threat += 2;
                                    if (t.getUnitCount() * 2 > worstThreat) {
                                        if (t.getSettlement() != null) {
                                            bestTarget = t.getSettlement();
                                        } else {
                                            bestTarget = t.getFirstUnit();
                                        }
                                        worstThreat = t.getUnitCount() * 2;
                                    }
                                } else if (player.getTension(t.getFirstUnit().getOwner()).getValue() >=
                                    Tension.TENSION_ADD_MINOR) {
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
            Location bestTarget = null;
            
            Iterator ui = colony.getTile().getUnitIterator();
            while (ui.hasNext()) {
                if (((Unit)(ui.next())).isDefensiveUnit()) {
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
                        if (player.getTension(t.getFirstUnit().getOwner()).getValue() >=
                            Tension.TENSION_ADD_MAJOR) {
                            Iterator uit = t.getUnitIterator();
                            while (uit.hasNext()) {
                                if (((Unit)(uit.next())).isOffensiveUnit()) {
                                    thisThreat += 2;
                                }
                            }
                        } else if (player.getTension(t.getFirstUnit().getOwner()).getValue() >= 
                            Tension.TENSION_ADD_MINOR) {
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
                    if (!u.isArmed() && u.canBeArmed()) {
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
                        
                        if (!u.isMounted() && u.canBeMounted()) {
                            equipUnitElement = Message.createNewRootElement("equipunit");
                            equipUnitElement.setAttribute("unit", u.getID());
                            equipUnitElement.setAttribute("type", Integer.toString(Goods.HORSES));
                            equipUnitElement.setAttribute("amount", "50");
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
     * Gives a mission to non-nava) units. 
     */
    private void giveNormalMissions() {
        // Create a datastructure for the worker wishes:
        ArrayList[] workerWishes = new ArrayList[Unit.UNIT_COUNT];
        for (int i=0; i<workerWishes.length; i++) {
            if (workerWishes[i] == null) {
                workerWishes[i] = new ArrayList();
            }
        }
        if (player.isEuropean()) {
            Iterator aIterator = getAIColonyIterator();
            while (aIterator.hasNext()) {
                Iterator wIterator = ((AIColony) aIterator.next()).getWishIterator();
                while (wIterator.hasNext()) {
                    Wish w = (Wish) wIterator.next();
                    if (w instanceof WorkerWish && w.getTransportable() == null) {                        
                        workerWishes[((WorkerWish) w).getUnitType()].add(w);
                    }
                }
            }
        }
        
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            
            if (aiUnit.hasMission()) {
                continue;
            }
            
            Unit unit = aiUnit.getUnit();
            
            if (unit.getType() == Unit.TREASURE_TRAIN) {
                aiUnit.setMission(new CashInTreasureTrainMission(getAIMain(), aiUnit));
            } else if (unit.isScout() && ScoutingMission.isValid(aiUnit)) {
                aiUnit.setMission(new ScoutingMission(getAIMain(), aiUnit));
            } else if ((unit.isOffensiveUnit() || unit.isDefensiveUnit()) && 
                    (!unit.isColonist() 
                            || unit.getType() == Unit.VETERAN_SOLDIER
                            || getGame().getTurn().getNumber() > 5)) {
                giveMilitaryMission(aiUnit);
            } else if (unit.isPioneer() && PioneeringMission.isValid(aiUnit)) {
                aiUnit.setMission(new PioneeringMission(getAIMain(), aiUnit));
            } else if (unit.isColonist()) {
                // Check if this unit is needed as an expert (using: "WorkerWish"):
                ArrayList wishList = workerWishes[unit.getType()];
                WorkerWish bestWish = null;
                int bestTurns = Integer.MAX_VALUE;
                for (int i=0; i<wishList.size(); i++) {
                    WorkerWish ww = (WorkerWish) wishList.get(i);
                    int turns;
                    if (unit.getLocation().getTile() == null ) {
                        // TODO-MUCH-LATER: This can be done better:
                        turns = ((Tile) player.getEntryLocation()).getDistanceTo(ww.getDestination().getTile()) * 3;
                    } else if (unit.getLocation() instanceof Tile || unit.getLocation() instanceof Unit) {
                        turns = unit.getTurnsToReach(ww.getDestination().getTile());
                    } else {
                        turns = Integer.MAX_VALUE;
                    }
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
                Tile colonyTile = null;
                if (getPlayer().canBuildColonies()) {
                    colonyTile = BuildColonyMission.findColonyLocation(aiUnit.getUnit());
                }
                if (colonyTile != null) {
                    bestTurns = unit.getTurnsToReach(colonyTile);
                }
                // Check if we can find a better site to work than a new colony:
                if (!hasFewColonies() || colonyTile == null || bestTurns > 10) {
                    for (int i=0; i<workerWishes.length; i++) {
                        wishList = workerWishes[i];
                        for (int j=0; j<wishList.size(); j++) {
                            WorkerWish ww = (WorkerWish) wishList.get(j);
                            Tile source = unit.getTile();
                            if (source == null) {
                                if (unit.getLocation() instanceof Unit) {
                                    source = (Tile) ((Unit) unit.getLocation()).getEntryLocation();
                                } else {
                                    source = (Tile) unit.getOwner().getEntryLocation();
                                }
                            }
                            int turns = unit.getTurnsToReach(source, ww.getDestination().getTile());
                            // TODO: Choose to build colony if the value of the wish is low.
                            if (bestWish == null && turns < bestTurns
                                    || bestWish != null && (turns < bestTurns || 
                                            turns == bestTurns && ww.getValue() > bestWish.getValue())) {
                                bestWish = ww;
                                bestTurns = turns;
                            }
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
            
            if (!aiUnit.hasMission()) {
                aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            }           
        }
    }
    
    /**
     * Brings gifts to nice players with nearby colonies.
     */
    private void bringGifts() {
        if (!player.isIndian()) {
            return;
        }
        
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
                    } else if (chosenOne.getMission() == null || chosenOne.getMission() instanceof UnitWanderHostileMission) {
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
     * Demands goods from players with nearby colonies.
     */
    private void demandTribute() {
        if (!player.isIndian()) {
            return;
        }
        Iterator indianSettlementIterator = player.getIndianSettlementIterator();
        while (indianSettlementIterator.hasNext()) {
            IndianSettlement indianSettlement = (IndianSettlement) indianSettlementIterator.next();
            
            // Do not demand goods all the time:
            if (getRandom().nextInt(10) != 1) {
                continue;
            }
            
            int alreadyAssignedUnits = 0;
            Iterator ownedUnits = indianSettlement.getOwnedUnitsIterator();
            while (ownedUnits.hasNext()) {
                if (((AIUnit) getAIMain().getAIObject((Unit) ownedUnits.next())).getMission()
                        instanceof IndianDemandMission) {
                    alreadyAssignedUnits++;
                }
            }
            if (alreadyAssignedUnits > MAX_NUMBER_OF_DEMANDS) {
                continue;
            }
            
            // Creates a list of nearby colonies:
            List nearbyColonies = new ArrayList();
            Iterator it = getGame().getMap().
            getCircleIterator(indianSettlement.getTile().getPosition(),
                    true,
                    MAX_DISTANCE_TO_MAKE_DEMANDS);
            while (it.hasNext()) {
                Tile t = getGame().getMap().getTile((Map.Position) it.next());
                if (t.getColony() != null ) {
                    nearbyColonies.add(t.getColony());
                }
            }
            if (nearbyColonies.size() > 0) {
                int targetTension = Integer.MIN_VALUE;
                Colony target = null;
                for (int i=0; i<nearbyColonies.size(); i++) {
                    Colony t = (Colony) nearbyColonies.get(i);
                    Player to = t.getOwner();
                    int tension = 1 + getPlayer().getTension(to).getValue() + indianSettlement.getAlarm(to).getValue();
                    tension = getRandom().nextInt(tension);
                    if (tension > targetTension) {
                        targetTension = tension;
                        target = t;
                    }
                }                
                Iterator it2 = indianSettlement.getOwnedUnitsIterator();
                AIUnit chosenOne = null;
                while (it2.hasNext()) {
                    chosenOne = (AIUnit) getAIMain().getAIObject((Unit) it2.next());
                    if (!(chosenOne.getUnit().getLocation() instanceof Tile)) {
                        chosenOne = null;
                    } else if (chosenOne.getMission() == null || chosenOne.getMission() instanceof UnitWanderHostileMission) {
                        break;
                    }
                }
                if (chosenOne != null) {
                    // Check that the colony can be reached:
                    PathNode pn = getGame().getMap().findPath(chosenOne.getUnit(),
                            indianSettlement.getTile(),
                            target.getTile());
                    if (pn != null && pn.getTotalTurns() <= MAX_DISTANCE_TO_MAKE_DEMANDS) {
                        // Make it less probable that nice players get targeted for a demand misson:
                        Player tp = target.getOwner();                        
                        int tension = 1 + getPlayer().getTension(tp).getValue() + indianSettlement.getAlarm(tp).getValue();
                        if (getRandom().nextInt(tension) > Tension.HAPPY) {
                            chosenOne.setMission(new IndianDemandMission(getAIMain(), chosenOne, target));
                        }
                    }
                }
                
            }
        }
    }
    
    /**
     * Calls {@link AIColony#createAIGoods()} for every colony
     * this player owns.
     */
    private void createAIGoodsInColonies() {
        if (!player.isEuropean()) {
            return;
        }
        Iterator ci = getAIColonyIterator();
        while (ci.hasNext()) {
            AIColony c = (AIColony) ci.next();
            c.createAIGoods();
        }
    }
    
    /**
     * Makes every unit perform their mission.
     */
    private void doMissions() {
        Iterator aiUnitsIterator = getAIUnitIterator();
        while (aiUnitsIterator.hasNext()) {
            AIUnit aiUnit = (AIUnit) aiUnitsIterator.next();
            if (aiUnit.hasMission() && aiUnit.getMission().isValid()) {
                try {
                    aiUnit.doMission(getConnection());
                } catch  (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    logger.warning(sw.toString());
                }
            }
        }       
    }
    
    private int getDefendColonyMissionValue(Unit u, Colony colony, int turns) {
        // Temporary helper method for: giveMilitaryMission
        int value = 10025 - turns;            
        
        int numberOfDefendingUnits = 0;
        /*Iterator ui = colony.getTile().getUnitIterator();
        while (ui.hasNext()) {
            Unit tu = (Unit) ui.next();
            if (tu.isDefensiveUnit()) {
                value -= 6;
                numberOfDefendingUnits++;
            }                       
        }*/
        Iterator aui = getAIUnitIterator();
        while (aui.hasNext()) {
            Mission m = ((AIUnit) aui.next()).getMission();
            if (m != null && m instanceof DefendSettlementMission) {
                if (((DefendSettlementMission) m).getSettlement() == colony) {
                    value -= 6;
                    numberOfDefendingUnits++;
                }
            }
        }
        
        if (u.getOwner().isREF()) {
            value -= 19;
            if (numberOfDefendingUnits > 0) {
                return 0;
            }
        }
        
        if (colony != null 
                && numberOfDefendingUnits > colony.getBuilding(Building.STOCKADE).getLevel() + 1) {
            return Math.max(0, value - 9000);
        }
        
        return value;
    }
    
    private int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile, int turns) {
        if (newTile.isLand() 
                && !unit.isNaval() 
                && newTile.getDefendingUnit(unit) != null 
                && newTile.getDefendingUnit(unit).getOwner() != unit.getOwner()
                && unit.getOwner().getStance(newTile.getDefendingUnit(unit).getOwner()) == Player.WAR) {
            
            int value = 10020;                    
            if (newTile.getBestTreasureTrain() != null) {
                value += Math.min(newTile.getBestTreasureTrain().getTreasureAmount() / 10, 50);
            }
            if (newTile.getDefendingUnit(unit).getType() == Unit.ARTILLERY && newTile.getSettlement() == null) {
                value += 200 - newTile.getDefendingUnit(unit).getDefensePower(unit) * 2 - turns * 50;
            }
            if (newTile.getDefendingUnit(unit).getType() == Unit.VETERAN_SOLDIER && !newTile.getDefendingUnit(unit).isArmed()) {
                value += 10 - newTile.getDefendingUnit(unit).getDefensePower(unit) * 2 - turns * 25;
            }
            if (newTile.getSettlement() != null) {
                value += 300;
                Iterator dp = newTile.getUnitIterator();
                while (dp.hasNext()) {
                    Unit u = (Unit) dp.next();
                    if (u.isDefensiveUnit()) {
                        if (u.getDefensePower(unit) > unit.getOffensePower(u)) {
                            value -= 100 * (u.getDefensePower(unit) - unit.getOffensePower(u));
                        } else {
                            value -= u.getDefensePower(unit);
                        }
                    }
                }
            }
            value += newTile.getDefendingUnit(unit).getOffensePower(unit) - newTile.getDefendingUnit(unit).getDefensePower(unit);
             
            value -= turns * 10;

            return Math.max(0, value);                                    
        } else {
            return 0;            
        }
    }
    
    /**
     * Gives a military <code>Mission</code> to the given
     * unit.
     *  
     * <br><br>
     *  
     * <b>This method should only be used on units owned by
     * european players.</b>
     *  
     * @param aiUnit The unit.
     */
    private void giveMilitaryMission(AIUnit aiUnit) {
        /*
         *  Temporary method for giving a military mission.
         *  This method will be removed when "MilitaryStrategy" and
         *  the "Tactic"-classes has been implemented.
         */
        
        if (player.isIndian()) {
            aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));
            return;
        }

        Unit unit = aiUnit.getUnit();
        Unit carrier = (unit.getLocation() instanceof Unit) ? (Unit) unit.getLocation() : null;
        Map map = unit.getGame().getMap();
        
        // Initialize variables:
        Ownable bestTarget = null;              // The best target for a mission.
        int bestValue = Integer.MIN_VALUE;      // The value of the target above.

        // Determine starting tile:
        Tile startTile = unit.getTile();
        if (startTile == null) {
            if (unit.getLocation() instanceof Unit) {
                startTile = (Tile) ((Unit) unit.getLocation()).getEntryLocation();
            } else {
                startTile = (Tile) unit.getOwner().getEntryLocation();
            }
        }               

        /* 
         * Checks if we are currently located on a Tile with a Settlement
         * which requires defenders:
         */
        if (unit.getTile() != null
                && unit.getTile().getColony() != null) {
            bestTarget = unit.getTile().getColony();            
            bestValue = getDefendColonyMissionValue(unit, (Colony) bestTarget, 0);
        }        
        
        // Checks if a nearby colony requires additional defence:
        int theBestValueSoFar1 = bestValue;
        GoalDecider gd = new GoalDecider() {
            private PathNode best = null;
            private int bestValue = Integer.MIN_VALUE;
            
            public PathNode getGoal() {
                return best;
            }
            
            public boolean hasSubGoals() {
                return true;
            }
            
            public boolean check(Unit u, PathNode pathNode) {
                Tile t = pathNode.getTile();
                if (t.getColony() != null
                        && t.getColony().getOwner() == u.getOwner()) {
                    int value = getDefendColonyMissionValue(u, t.getColony(), pathNode.getTurns());
                    if (value > 0 && value > bestValue) {
                        bestValue = value;
                        best = pathNode;
                    }
                    return true;
                } else {
                    return false;
                }
            }
        };
        final int MAXIMUM_DISTANCE_TO_SETTLEMENT = 10; // Given in number of turns.
        PathNode bestPath = map.search(unit, startTile, gd, map.getDefaultCostDecider(), MAXIMUM_DISTANCE_TO_SETTLEMENT, carrier);
        if (bestPath != null) {      
            PathNode ln = bestPath.getLastNode();
            int value = getDefendColonyMissionValue(unit, ln.getTile().getColony(), ln.getTurns());
            if (value > bestValue) {
                bestTarget = ln.getTile().getColony();
                bestValue = value;
            }
        }        
                
        // Searches for the closest target for an existing "UnitSeekAndDestroyMission":
        Location bestExistingTarget = null;
        int smallestDifference = Integer.MAX_VALUE;
        Iterator aui = getAIUnitIterator();
        while (aui.hasNext()) {
            AIUnit coAIUnit = (AIUnit) aui.next();
            Unit coUnit = coAIUnit.getUnit();
            if (coUnit.getTile() != null
                    && coAIUnit.getMission() instanceof UnitSeekAndDestroyMission) {
                Location target = ((UnitSeekAndDestroyMission) coAIUnit.getMission()).getTarget();
                int ourDistance = unit.getTurnsToReach(startTile, target.getTile());
                int coUnitDistance = coUnit.getTurnsToReach(target.getTile());
                if (ourDistance != Integer.MAX_VALUE) {
                    int difference = Math.abs(ourDistance - coUnitDistance);
                    if (difference < smallestDifference) {
                        smallestDifference = difference;
                        bestExistingTarget = target;
                    }
                }
            }
        }
        if (bestExistingTarget != null) {
            int value = getUnitSeekAndDestroyMissionValue(unit, bestExistingTarget.getTile(), smallestDifference);
            if (value > bestValue) {
                bestValue = value;
                bestTarget = (Ownable) bestExistingTarget;
            }
        }
        
        // Checks if there is a better target than the existing one:
        GoalDecider targetDecider = new GoalDecider() {
            private PathNode bestTarget = null;
            private int bestNewTargetValue = Integer.MIN_VALUE;
            
            public PathNode getGoal() {
                return bestTarget;              
            }
            
            public boolean hasSubGoals() {
                return true;
            }
            
            public boolean check(Unit unit, PathNode pathNode) {
                Tile newTile = pathNode.getTile();
                Map map = newTile.getMap();
                                
                if (newTile.isLand() 
                        && !unit.isNaval() 
                        && newTile.getDefendingUnit(unit) != null 
                        && newTile.getDefendingUnit(unit).getOwner() != unit.getOwner()
                        && unit.getOwner().getStance(newTile.getDefendingUnit(unit).getOwner()) == Player.WAR) {
                    
                    int value = getUnitSeekAndDestroyMissionValue(unit, pathNode.getTile(), pathNode.getTurns());
                    if (value > bestNewTargetValue) {
                        bestTarget = pathNode;                           
                        bestNewTargetValue = value;
                        return true;
                    }                                    
                }            
                return false;
            }
        };        
        PathNode newTarget = map.search(unit, startTile, targetDecider, map.getDefaultCostDecider(), Integer.MAX_VALUE, carrier);
        if (newTarget != null) {
            Tile targetTile = newTarget.getLastNode().getTile();
            int value = getUnitSeekAndDestroyMissionValue(unit, targetTile, newTarget.getTotalTurns());
            if (value > bestValue) {
                bestValue = value;                
                if (targetTile.getSettlement() != null) {
                    bestTarget = targetTile.getSettlement();
                } else if (targetTile.getBestTreasureTrain() != null) {
                    bestTarget = targetTile.getBestTreasureTrain();
                } else {
                    bestTarget = targetTile.getDefendingUnit(unit);
                }
            }
        }                       

        // Use the best target:
        if (bestTarget != null && bestValue > 0) {
            if (bestTarget.getOwner() == unit.getOwner()) {
                aiUnit.setMission(new DefendSettlementMission(getAIMain(), aiUnit, (Colony) bestTarget));
            } else {
                aiUnit.setMission(new UnitSeekAndDestroyMission(getAIMain(), aiUnit, (Location) bestTarget));
            }
        } else {
            // Just give a simple mission if we could not find a better one:
            aiUnit.setMission(new UnitWanderHostileMission(getAIMain(), aiUnit));            
        }
    }
    
    /**
     * Returns an <code>Iterator</code> over all the 
     * <code>TileImprovement</code>s needed by all
     * of this player's colonies.
     * 
     * @return The <code>Iterator</code>.
     * @see TileImprovement
     */
    public Iterator getTileImprovementIterator() {
        List tileImprovements = new ArrayList();
        
        Iterator acIterator = getAIColonyIterator();
        while (acIterator.hasNext()) {
            AIColony ac = (AIColony) acIterator.next();
            Iterator it = ac.getTileImprovementIterator();
            while (it.hasNext()) {
                tileImprovements.add(it.next());
            }
        }
        
        return tileImprovements.iterator();
    }    
    
    
    /**
     * This is a temporary method which are used for 
     * forcing the computer players into building more 
     * colonies. The method will be removed after the
     * proper code for deciding wether a colony should 
     * be built or not has been implemented.
     * 
     * @return <code>true</code> if the AI should build
     *         more colonies.
     */
    public boolean hasFewColonies() {        
        if (!getPlayer().canBuildColonies()) {
            return false;
        }
        Iterator it = getPlayer().getColonyIterator();
        int numberOfColonies = 0;
        int numberOfWorkers = 0;
        while (it.hasNext()) {
            Colony c = (Colony) it.next();
            numberOfColonies++;
            numberOfWorkers += c.getUnitCount();
        }
        return numberOfColonies <= 2  || numberOfColonies >= 3 
        && numberOfWorkers / numberOfColonies > numberOfColonies - 2;
    }
    
    
    /**
     * Maps <code>Transportable</code>s to carrier's using a
     * <code>TransportMission</code>.
     */
    private void createTransportLists() {
        if (!player.isEuropean()) {
            return;
        }
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
                if (t2.getTransportLocatable().getLocation() == t.getTransportLocatable().getLocation()) {
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
     * Selects the most useful founding father offered.
     *
     * @param foundingFathers The founding fathers on offer.
     * @return The founding father selected.
     */
    public int selectFoundingFather(int[] foundingFathers) {
        int choice = -1;
        int bestValue = -1;
        for (int i = 0; i < FoundingFather.TYPE_COUNT; i++) {
            int value = -1;
            switch (foundingFathers[i]) {
            case FoundingFather.ADAM_SMITH:
                if (strategy == STRATEGY_TRADE) {
                    value = 10;
                } else {
                    value = 5;
                }
                break;
            case FoundingFather.JACOB_FUGGER:
                if (strategy == STRATEGY_TRADE) {
                    value = 6;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.PETER_MINUIT:
                if (strategy == STRATEGY_CONQUEST) {
                    value = 1;
                } else {
                    value = 6;
                }
                break;
            case FoundingFather.PETER_STUYVESANT:
                if (strategy == STRATEGY_TRADE) {
                    value = 8;
                } else {
                    value = 4;
                }
                break;
            case FoundingFather.JAN_DE_WITT:
                if (strategy == STRATEGY_TRADE) {
                    value = 6;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.FERDINAND_MAGELLAN:
                value = 5;
                break;
            case FoundingFather.FRANSISCO_DE_CORONADO:
                value = 4;
                break;
            case FoundingFather.HERNANDO_DE_SOTO:
                value = 7;
                break;
            case FoundingFather.HENRY_HUDSON:
                if (strategy == STRATEGY_TRADE) {
                    value = 8;
                } else {
                    value = 4;
                }
                break;
            case FoundingFather.LA_SALLE:
                value = 3;
                break;
            case FoundingFather.HERNAN_CORTES: 
                if (strategy == STRATEGY_CONQUEST) {
                    value = 10;
                } else if (strategy == STRATEGY_COOPERATION) {
                    value = 1;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.GEORGE_WASHINGTON:
                value = 5;
                break;
            case FoundingFather.PAUL_REVERE:
                value = 2;
                break;
            case FoundingFather.FRANCIS_DRAKE:
                value = 3;
                break;
            case FoundingFather.JOHN_PAUL_JONES:
                value = 3;
                break;
            case FoundingFather.THOMAS_JEFFERSON:
                value = 10;
                break;
            case FoundingFather.POCAHONTAS:
                if (strategy == STRATEGY_CONQUEST) {
                    value = 1;
                } else if (strategy == STRATEGY_COOPERATION) {
                    value = 7;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.THOMAS_PAINE:
                value = Math.max(1, player.getTax()/10);
                break;
            case FoundingFather.SIMON_BOLIVAR:
                value = 6;
                break;
            case FoundingFather.BENJAMIN_FRANKLIN:
                value = 5;
                break;
            case FoundingFather.WILLIAM_BREWSTER:
                if (strategy == STRATEGY_IMMIGRATION) {
                    value = 7;
                } else {
                    value = 5;
                }
                break;
            case FoundingFather.WILLIAM_PENN:
                if (strategy == STRATEGY_IMMIGRATION) {
                    value = 5;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.FATHER_JEAN_DE_BREBEUF: 
                if (strategy == STRATEGY_CONQUEST) {
                    value = 2;
                } else {
                    value = 4;
                }
                break;
            case FoundingFather.JUAN_DE_SEPULVEDA:
                if (strategy == STRATEGY_CONQUEST) {
                    value = 7;
                } else {
                    value = 3;
                }
                break;
            case FoundingFather.BARTOLOME_DE_LAS_CASAS: 
                if (strategy == STRATEGY_CONQUEST) {
                    value = 6;
                } else {
                    value = 5;
                }
                break;
            case FoundingFather.NONE:
                break;
            default:
                throw new IllegalArgumentException("FoundingFather has invalid type.");
            }
            if (value > bestValue) {
                bestValue = value;
                choice = foundingFathers[i];
            }
        }
        return choice;
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
     * @return The price this <code>AIPlayer</code> suggests or
     *       {@link NetworkConstants#NO_TRADE}.
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
                price = ((IndianSettlement) settlement).getPrice(goods) -
                player.getTension(unit.getOwner()).getValue();
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
        } else if (settlement instanceof Colony) {
            Colony colony = (Colony) settlement;
            Player otherPlayer = unit.getOwner();
            // the client should have prevented this
            if (player.getStance(otherPlayer) == Player.WAR) {
                return NetworkConstants.NO_TRADE;
            }
            // don't pay for more than fits in the warehouse
            int amount = colony.getWarehouseCapacity() - colony.getGoodsCount(goods.getType());
            amount = Math.min(amount, goods.getAmount());
            // get a good price
            int tensionLevel = player.getTension(otherPlayer).getLevel();
            int percentage = (9 - tensionLevel) * 10;
            // what we could get for the goods in Europe (minus taxes)
            int netProfits = ((100 - player.getTax()) * getGame().getMarket().getSalePrice(goods.getType(), amount)) / 100;
            int price = (netProfits * percentage) / 100;
            return price;
        } else {
            throw new IllegalArgumentException("Unknown type of settlement.");
        }
    }
    
    
    /**
     * Decides whether to accept the monarch's tax raise or not.
     *
     * @param tax The new tax rate to be considered.
     * @return <code>true</code> if the tax raise should be accepted.
     */
    public boolean acceptTax(int tax) {
        Goods toBeDestroyed = player.getMostValuableGoods();
        if (toBeDestroyed == null) {
            return false;
        }
        switch (toBeDestroyed.getType()) {
        case Goods.FOOD:
        case Goods.HORSES:
            // we should be able to produce goods and horses
            // ourselves
            return false;
        case Goods.TRADE_GOODS:
        case Goods.TOOLS:
        case Goods.MUSKETS:
            if (getGame().getTurn().getAge() == 3) {
                // by this time, we should be able to produce
                // enough ourselves
                return false;
            } else {
                return true;
            }
        default:
            int averageIncome = 0;
            for (int i = 0; i < Goods.NUMBER_OF_TYPES; i++) {
                averageIncome += player.getIncomeAfterTaxes(i);
            }
            averageIncome = averageIncome / Goods.NUMBER_OF_TYPES;
            if (player.getIncomeAfterTaxes(toBeDestroyed.getType()) > averageIncome) {
                // this is a more valuable type of goods
                return false;
            } else {
                return true;
            }
        }
    }
    
    /**
     * Decides whether to accept an Indian demand, or not.
     *
     * @param unit The unit making demands.
     * @param colony The colony where demands are being made.
     * @param goods The goods demanded.
     * @param gold The amount of gold demanded.
     * @return <code>true</code> if this <code>AIPlayer</code>
     *      accepts the indian demand and <code>false</code>
     *      otherwise.
     */
    public boolean acceptIndianDemand(Unit unit, Colony colony, Goods goods, int gold) {
        // TODO: make a better choice
        if (strategy == STRATEGY_CONQUEST) {
            return false;
        } else {
            return true;
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
                    logger.warning("Could not find the AIUnit for: " + theUnit + " (" + theUnit.getID() + ") - " + (getGame().getFreeColGameObject(theUnit.getID()) != null));
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
     * @return The <code>Player</code>.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Returns the strategy of this <code>AIPlayer</code>.
     * @return the strategy of this <code>AIPlayer</code>.
     */
    public int getStrategy() {
        return strategy;
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
     * 
     * @param debuggingConnection The connection to be used for debugging.
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
    
    /**
     * Writes this object to an XML stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement(getXMLElementTagName());
        
        out.writeAttribute("ID", getID());
        
        out.writeEndElement();
    }
    
    
    /**
     * Reads information for this object from an XML stream.
     * @param in The input stream with the XML.
     * @throws XMLStreamException if there are any problems reading
     *      from the stream.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {        
        player = (ServerPlayer) getAIMain().getFreeColGameObject(in.getAttributeValue(null, "ID"));
        in.nextTag();
    }
    
    
    /**
     * Returns the tag name of the root element representing this object.
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "aiPlayer";
    }
}
