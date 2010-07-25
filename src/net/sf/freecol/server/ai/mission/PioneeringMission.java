/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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


package net.sf.freecol.server.ai.mission;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.EquipmentType;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovement;
import net.sf.freecol.common.model.TileItemContainer;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.ClaimLandMessage;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.common.networking.Message;
import net.sf.freecol.server.ai.AIColony;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;
import net.sf.freecol.server.ai.TileImprovementPlan;

import org.w3c.dom.Element;


/**
 * Mission for controlling a pioneer.
 * 
 * @see net.sf.freecol.common.model.Unit.Role#PIONEER
 */
public class PioneeringMission extends Mission {
    /* 
     * TODO-LATER: "updateTileImprovementPlan" should be called
     *             only once (in the beginning of the turn).
     */
    
    private static final Logger logger = Logger.getLogger(PioneeringMission.class.getName());

    private static final EquipmentType toolsType = Specification.getSpecification().getEquipmentType("model.equipment.tools");

    private static enum PioneeringMissionState {GET_TOOLS,IMPROVING};
    
    private PioneeringMissionState state = PioneeringMissionState.GET_TOOLS;
    
    private TileImprovementPlan tileImprovementPlan = null;
    
    private Colony colonyWithTools = null;

    private boolean invalidateMission = false;

    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * 
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     */
    public PioneeringMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);

        boolean hasTools = getUnit().hasAbility("model.ability.improveTerrain");
        if(hasTools){
            state = PioneeringMissionState.IMPROVING;
        }
        else{
            state = PioneeringMissionState.GET_TOOLS;
        }
    }


    /**
     * Loads a mission from the given element.
     * 
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public PioneeringMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>PioneeringMission</code> and reads the given element.
     * 
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public PioneeringMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }
    
    
    /**
     * Disposes this <code>Mission</code>.
     */
    public void dispose() {
        if (tileImprovementPlan != null) {
            tileImprovementPlan.setPioneer(null);
            tileImprovementPlan = null;
        }
        super.dispose();
    }

    /**
     * Sets the <code>TileImprovementPlan</code> which should
     * be the next target.
     * 
     * @param tileImprovementPlan The <code>TileImprovementPlan</code>.
     */
    public void setTileImprovementPlan(TileImprovementPlan tileImprovementPlan) {
        this.tileImprovementPlan = tileImprovementPlan;
    }

    private void updateTileImprovementPlan() {
        final AIPlayer aiPlayer = (AIPlayer) getAIMain().getAIObject(getUnit().getOwner().getId());
        final Unit carrier = (getUnit().isOnCarrier()) ? (Unit) getUnit().getLocation() : null;
        
        Tile improvementTarget = (tileImprovementPlan != null)? tileImprovementPlan.getTarget():null;
        // invalid tileImprovementPlan, remove and get a new valid one
        if (tileImprovementPlan != null && improvementTarget == null) {
            logger.finest("Found invalid TileImprovementPlan, removing it and assigning a new one");
            aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
            tileImprovementPlan.dispose();
            tileImprovementPlan = null;
        }
        
        // Verify if the improvement has been applied already
        // If it has, remove this improvement
        if (tileImprovementPlan != null &&
            improvementTarget != null &&
            improvementTarget.hasImprovement(tileImprovementPlan.getType())){
            aiPlayer.removeTileImprovementPlan(tileImprovementPlan);
            tileImprovementPlan.dispose();
            tileImprovementPlan = null;
        }
        
        // mission still valid, no update needed
        if (tileImprovementPlan != null && improvementTarget != null) {
            return;
        }
        
        final Tile startTile;
        if (getUnit().getTile() == null) {
            if (getUnit().isOnCarrier()) {
                startTile = (Tile) ((Unit) getUnit().getLocation()).getEntryLocation();
            } else {
                startTile = (Tile) getUnit().getOwner().getEntryLocation();
            }
        } else {
            startTile = getUnit().getTile();
        }
                
        TileImprovementPlan bestChoice = null;
        int bestValue = 0;
        Iterator<TileImprovementPlan> tiIterator = aiPlayer.getTileImprovementPlanIterator();            
        while (tiIterator.hasNext()) {
            TileImprovementPlan ti = tiIterator.next();
            if (ti.getPioneer() == null) {
                // invalid tileImprovementPlan, remove and get a new valid one
                if (ti.getTarget() == null) {
                    logger.finest("Found invalid TileImprovementPlan, removing it and finding a new one");
                    aiPlayer.removeTileImprovementPlan(ti);
                    ti.dispose();
                    continue;
                }
                
                PathNode path = null;
                int value;
                if (startTile != ti.getTarget()) {
                    path = getGame().getMap().findPath(getUnit(), startTile, ti.getTarget(), carrier);
                    if (path != null) {
                        value = ti.getValue() + 10000 - (path.getTotalTurns()*5);
                        
                        /*
                         * Avoid picking a TileImprovementPlan with a path being blocked 
                         * by an enemy unit (apply a penalty to the value):
                         */
                        PathNode pn = path;
                        while (pn != null) {
                            if (pn.getTile().getFirstUnit() != null
                                && pn.getTile().getFirstUnit().getOwner() != getUnit().getOwner()) {
                                value -= 1000;
                            }
                            pn = pn.next;
                        }
                    } else {
                        value = ti.getValue();
                    }
                } else {
                    value = ti.getValue() + 10000;
                }                
                if (value > bestValue) {
                    bestChoice = ti;
                    bestValue = value;
                }
            }
        }
        
        if (bestChoice != null) {
            tileImprovementPlan = bestChoice;
            bestChoice.setPioneer(getAIUnit());
        }
        
        if(tileImprovementPlan == null){
            invalidateMission = true;
        }
    }
    
    
    /**
     * Performs this mission.
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        logger.finest("Entering PioneeringMission.doMission()");
        
        Unit unit = getUnit();
        
        boolean hasTools = getUnit().hasAbility("model.ability.improveTerrain");
        if(unit.getState() == UnitState.IMPROVING || hasTools){
            state = PioneeringMissionState.IMPROVING;
        }
        else{
            state = PioneeringMissionState.GET_TOOLS;
        }
        
        while(isValid() && unit.getMovesLeft() > 0){
            switch(state){
                case GET_TOOLS:
                    getTools(connection);
                    break;
                case IMPROVING:
                    processImprovementPlan(connection);
                    break;
                default:
                    logger.warning("Unknown state");
                    invalidateMission = true;
            }        
        }
    }

    private void processImprovementPlan(Connection connection) {
        if (tileImprovementPlan == null) {
            updateTileImprovementPlan();
            if (tileImprovementPlan == null) {
                invalidateMission = true;
                return;
            }
        }

        Unit unit = getUnit();
        // Sanitation
        if (unit.getTile() == null) {
            logger.warning("Unit is in unknown location, cannot proceed with mission");
            invalidateMission = true;
            return;
        }
                
        // move toward the target tile
        if (getUnit().getTile() != tileImprovementPlan.getTarget()) {
            PathNode pathToTarget = getUnit().findPath(tileImprovementPlan.getTarget());
            if (pathToTarget == null) {
                invalidateMission = true;
                return; 
            }

            Direction direction = moveTowards(connection, pathToTarget);
            if (direction != null
                    && unit.getMoveType(direction).isProgress()) {
                AIMessage.askMove(getAIUnit(), direction);
            }
            
            if(unit.getTile() != tileImprovementPlan.getTarget()){
                unit.setMovesLeft(0);
            }
            if(unit.getMovesLeft() == 0){
                return;
            }
        }

        // Sanitation
        if (unit.getTile() != tileImprovementPlan.getTarget()){
            String errMsg = "Something is wrong, pioneer should be on the tile to improve, but isnt";
            logger.warning(errMsg);
            invalidateMission = true;
            return;
        }            

        Tile target = tileImprovementPlan.getTarget();
        Player player = getUnit().getOwner();
        if (target.getOwner() != player) {
            // Take control of land before proceeding with mission.
            // Decide whether to pay or steal.
            // Currently always pay if we can, steal if we can not.
            int price = player.getLandPrice(target);
            if (price > 0 && player.getGold() < price) {
                price = ClaimLandMessage.STEAL_LAND;
            }
            ClaimLandMessage message = new ClaimLandMessage(target, null, price);
            try {
                connection.sendAndWait(message.toXMLElement());
            } catch (IOException e) {
                logger.warning("Could not send \""
                               + ClaimLandMessage.getXMLElementTagName()
                               + "\"-message:" + e.getMessage());
            }
        }
        if (target.getOwner() != player) {
            // Failed to take ownership
            invalidateMission = true;
            return;
        }

        makeImprovement(connection);
    }

    private void makeImprovement(Connection connection) {
        if(getUnit().getState() == UnitState.IMPROVING){
            getUnit().setMovesLeft(0);
            return;
        }
        
        if (getUnit().checkSetState(UnitState.IMPROVING)) {
            // start improving now
            int price = getUnit().getOwner().getLandPrice(getUnit().getTile());
            // Buy the land from the Indians first?
            if (price > 0) {
                // TODO: the AI should buy the land, to avoid indian wars
            }
            // ask to create the TileImprovement
            Element changeWorkTypeElement = Message.createNewRootElement("workImprovement");
            changeWorkTypeElement.setAttribute("unit", getUnit().getId());
            changeWorkTypeElement.setAttribute("improvementType", tileImprovementPlan.getType().getId());
            Element reply = null;
            try {
                reply = connection.ask(changeWorkTypeElement);
            } catch (IOException e) {
                logger.warning("Could not send message!");
            }
            if (reply==null || !reply.getTagName().equals("workImprovementConfirmed")) {
                throw new IllegalStateException("Failed to make improvement");
            }
            
            // get the TileItemContainer
            Element containerElement = (Element)reply.getElementsByTagName(TileItemContainer.getXMLElementTagName()).item(0);
            if (containerElement != null) {
                TileItemContainer container = (TileItemContainer) getGame().getFreeColGameObject(containerElement.getAttribute("ID"));
                if (container == null) {
                    container = new TileItemContainer(getGame(), getUnit().getTile(), containerElement);
                    getUnit().getTile().setTileItemContainer(container);
                } else {
                    container.readFromXMLElement(containerElement);
                }
            }
            
            // get the TileImprovement
            Element improvementElement = (Element)reply.getElementsByTagName(TileImprovement.getXMLElementTagName()).item(0);
            if (improvementElement==null) {
                throw new IllegalStateException("Failed to make improvement");
            }
 
            TileImprovement improvement = (TileImprovement) getGame().getFreeColGameObject(improvementElement.getAttribute("ID"));
            if (improvement == null) {
                improvement = new TileImprovement(getGame(), improvementElement);
                getUnit().getTile().add(improvement);
            } else {
                improvement.readFromXMLElement(improvementElement);
            }
            getUnit().work(improvement);
        }
    }

    private void getTools(Connection connection) {
        validateColonyWithTools();
        if(invalidateMission){
            return;
        }
          
        Unit unit = getUnit();
        
        // Not there yet
        if(unit.getTile() != colonyWithTools.getTile()){
            PathNode path = unit.findPath(colonyWithTools.getTile());

            if(path == null){
                invalidateMission = true;
                colonyWithTools = null;
                return;
            }

            Direction direction = moveTowards(connection, path);
            moveButDontAttack(connection, direction);

            // not there yet, remove any moves left 
            if(unit.getTile() != colonyWithTools.getTile()){
                unit.setMovesLeft(0);
                return;
            }
        }
        // reached colony with tools, equip unit
        equipUnitWithTools(connection);
    }


    private void equipUnitWithTools(Connection connection) {
        Unit unit = getUnit();
        logger.finest("About to equip " + unit + " in " + colonyWithTools.getName());
        AIColony ac = (AIColony) getAIMain().getAIObject(colonyWithTools);
        int amount = toolsType.getMaximumCount();
        for (AbstractGoods materials : toolsType.getGoodsRequired()) {
            int availableAmount = ac.getAvailableGoods(materials.getType());
            int requiredAmount = materials.getAmount();
            if (availableAmount < requiredAmount) {
                invalidateMission = true;
                return;
            } 
            amount = Math.min(amount, availableAmount / requiredAmount);
        }

        logger.finest("Equipping " + unit + " at=" + colonyWithTools.getName() + " amount=" + amount);
        Element equipUnitElement = Message.createNewRootElement("equipUnit");
        equipUnitElement.setAttribute("unit", unit.getId());
        equipUnitElement.setAttribute("type", toolsType.getId());
        equipUnitElement.setAttribute("amount", Integer.toString(amount));
        try {
            connection.sendAndWait(equipUnitElement);
        } catch (Exception e) {
            logger.warning("Could not send equip message.");
        }
        
        // Unit is now equipped, get to work
        if(unit.getEquipmentCount(toolsType) > 0){
            state = PioneeringMissionState.IMPROVING;
        }
    }


    private boolean validateColonyWithTools() {
        if(colonyWithTools != null){
            if(colonyWithTools.isDisposed() 
                    || colonyWithTools.getOwner() != getUnit().getOwner()
                    || !colonyWithTools.canBuildEquipment(toolsType)){
                colonyWithTools = null;
            }
        }
        if(colonyWithTools == null){
            // find a new colony with tools
            colonyWithTools = findColonyWithTools(getAIUnit());
            if(colonyWithTools == null){
                logger.finest("No tools found");
                invalidateMission = true;
                return false;
            }
            logger.finest("Colony found=" + colonyWithTools.getName());            
        }
        return true;
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
        updateTileImprovementPlan();
        if (tileImprovementPlan == null) {
            return null;
        }
        if (getUnit().isOnCarrier()) {
            return tileImprovementPlan.getTarget();
        } else if (getUnit().getTile() == tileImprovementPlan.getTarget()) {
            return null;
        } else if (getUnit().getTile() == null || getUnit().findPath(tileImprovementPlan.getTarget()) == null) {
            return tileImprovementPlan.getTarget();
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
        if (getTransportDestination() != null) {
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
        if(getUnit().getTile() == null){
            return false;
        }
        
        switch(state){
            case GET_TOOLS:
                validateColonyWithTools();
                break;
            case IMPROVING:
                updateTileImprovementPlan();
                break;
        }        
        return !invalidateMission;
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The unit.
     * @return <code>true</code> if this mission is still valid to perform
     *         and <code>false</code> otherwise.
     */    
    public static boolean isValid(AIUnit aiUnit) {
        if(!aiUnit.getUnit().isColonist()){
            return false;
        }
        
        if(aiUnit.getUnit().getTile() == null){
            return false;
        }
        
        AIPlayer aiPlayer = (AIPlayer) aiUnit.getAIMain().getAIObject(aiUnit.getUnit().getOwner().getId());
        Iterator<TileImprovementPlan> tiIterator = aiPlayer.getTileImprovementPlanIterator();            
        
        
        boolean foundImprovementPlan = false;
        while (tiIterator.hasNext()) {
            TileImprovementPlan ti = tiIterator.next();
            if (ti.getPioneer() == null) {
                foundImprovementPlan = true;
            }
        }
        if(!foundImprovementPlan){
            logger.finest("No Improvement plan found, PioneeringMission not valid");
            return false;
        }
        
        boolean unitHasToolsAvail = aiUnit.getUnit().getEquipmentCount(toolsType) > 0;
        if(unitHasToolsAvail){
            logger.finest("Tools equipped, PioneeringMission valid");
           return true; 
        }
        
        // Search colony with tools to equip the unit with
        Colony colonyWithTools = findColonyWithTools(aiUnit);
        if(colonyWithTools != null){
            logger.finest("Tools found, PioneeringMission valid");
            return true;
        }
        
        logger.finest("Tools not found, PioneeringMission not valid");
        return false;
    }
    
    public static Colony findColonyWithTools(AIUnit aiu) {
        final int MAX_TURN_DISTANCE = 10;
        Colony best = null;
        int bestValue = Integer.MIN_VALUE;
        
        Unit unit = aiu.getUnit();
        // Sanitation
        if(unit == null){
            return null;
        }
        
        for(Colony colony : unit.getOwner().getColonies()){
            if(!colony.canBuildEquipment(toolsType)) {
                continue;
            }

            AIColony ac = (AIColony) aiu.getAIMain().getAIObject(colony);
            // Sanitation
            if(ac == null){
                continue;
            }
            
            // check if it possible for the unit to reach the colony
            PathNode pathNode = null;
            if(unit.getTile() != colony.getTile()){
                pathNode = unit.findPath(colony.getTile());
                // no path found
                if(pathNode == null){
                    continue;
                }
                // colony too far
                if(pathNode.getTotalTurns() > MAX_TURN_DISTANCE){
                    continue;
                }
            }
            
            int value = 100;
            // Prefer units with plenty of tools
            for(AbstractGoods goods : toolsType.getGoodsRequired()){
                value += colony.getGoodsCount(goods.getType());
            }
            
            if(pathNode != null){
                value -= pathNode.getTotalTurns() * 10;
            }
                
            if(best == null || value > bestValue){
                best = colony;
                bestValue = value;
            }
        }        
        return best;
    }
    
    public static List<AIUnit>getPlayerPioneers(AIPlayer aiPlayer){
        List<AIUnit> list = new ArrayList<AIUnit>();
        
        AIMain aiMain = aiPlayer.getAIMain();
        for(Unit u : aiPlayer.getPlayer().getUnits()){
            AIUnit aiu =  (AIUnit) aiMain.getAIObject(u);
            if(aiu == null){
                continue;
            }
            if(aiu.getMission() instanceof PioneeringMission){
                list.add(aiu);
            }
        }
        return list;
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
        
        out.writeAttribute("unit", getUnit().getId());
        if (tileImprovementPlan != null) {
            out.writeAttribute("tileImprovementPlan", tileImprovementPlan.getId());
        }

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        
        final String tileImprovementPlanStr = in.getAttributeValue(null, "tileImprovementPlan");
        if (tileImprovementPlanStr != null) {
            tileImprovementPlan = (TileImprovementPlan) getAIMain().getAIObject(tileImprovementPlanStr);
            if (tileImprovementPlan == null) {
                tileImprovementPlan = new TileImprovementPlan(getAIMain(), tileImprovementPlanStr);
            }
        } else {
            tileImprovementPlan = null;
        }
        
        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     * @return The <code>String</code> "wishRealizationMission".
     */
    public static String getXMLElementTagName() {
        return "tileImprovementPlanMission";
    }
    
    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     * 
     * @return The <code>String</code>: 
     *      <ul>
     *          <li>"(x, y) P" (for plowing)</li>
     *          <li>"(x, y) R" (for building road)</li>
     *          <li>"(x, y) Getting tools: (x, y)"</li>
     *      </ul>
     */
    public String getDebuggingInfo() {
        switch(state){
            case IMPROVING:
                if(tileImprovementPlan == null){
                    return "No target";
                }
                final String action = tileImprovementPlan.getType().getNameKey();
                return tileImprovementPlan.getTarget().getPosition().toString() + " " + action;
            case GET_TOOLS:
                if (colonyWithTools == null) {
                    return "No target";
                }
                return "Getting tools from " + colonyWithTools.getName();
            default:
                logger.warning("Unknown state");
                return "";
        }
    }
}
