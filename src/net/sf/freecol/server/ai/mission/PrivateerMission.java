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


package net.sf.freecol.server.ai.mission;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIObject;
import net.sf.freecol.server.ai.AIPlayer;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


public class PrivateerMission extends Mission {
    private static final Logger logger = Logger.getLogger(PrivateerMission.class.getName());

	private static enum PrivateerMissionState {HUNTING,TRANSPORTING};
	private PrivateerMissionState state = PrivateerMissionState.HUNTING;
	private Location nearestPort = null;
	private Tile target = null;


    /**
    * Creates a mission for the given <code>AIUnit</code>.
    *
    * @param aiMain The main AI-object.
    * @param aiUnit The <code>AIUnit</code> this mission
    *        is created for.
    */
    public PrivateerMission(AIMain aiMain, AIUnit aiUnit) {
        super(aiMain, aiUnit);
        Unit unit = aiUnit.getUnit();
        logger.finest("Assigning PrivateerMission to unit=" + unit + " at " + unit.getTile());
    }


    /**
     * Loads a mission from the given element.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public PrivateerMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see AIObject#readFromXML
     */
    public PrivateerMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
        super(aiMain);
        readFromXML(in);
    }


    /**
    * Performs the mission. This is done by searching for hostile units
    * that are located within one tile and attacking them. If no such units
    * are found, then wander in a random direction.
    *
    * @param connection The <code>Connection</code> to the server.
    */
    public void doMission(Connection connection) {
    	logger.finest("Entering doMission");
    	Unit unit = getUnit();
        while(isValid() && unit.getMovesLeft() > 0){
        	// Unit is between Europe and America, nothing to do
        	if(unit.isBetweenEuropeAndNewWorld()){
        		unit.setMovesLeft(0);
        		return;
        	}
            switch(state){
                case HUNTING:
                    hunt4Target(connection);
                    break;
                case TRANSPORTING:
                    gotoNearestPort(connection);
                    break;
            }
        }
    }

    private void hunt4Target(Connection  connection){
        Unit unit = getUnit();

        if(unit.getLocation() instanceof Europe){
            moveUnitToAmerica();
            unit.setMovesLeft(0);
            return;
        }
        logger.finest("Privateer (" + unit.getId() + ") at " + unit.getTile() + " hunting");

        // has captured goods, must get them to port
        if(unit.getGoodsCount() > 0){
            state = PrivateerMissionState.TRANSPORTING;
            return;
        }

        final int MAX_TURNS_TO_TARGET = 1;
        PathNode pathToTarget = findTarget(MAX_TURNS_TO_TARGET);
        // Found a target
        if (pathToTarget != null) {
        	target = pathToTarget.getLastNode().getTile();
            logger.finest("Privateer (" + unit.getId() + ") at " + unit.getTile() + " found target at " + target);
            // We need to find an updated path to target
            pathToTarget = unit.findPath(target);
            Direction direction = moveTowards(pathToTarget);
            if (direction == null) {
                // some movement points may still remain due to some
                // block or just not enough points for next node we need
                // to make sure the unit has no points left, so the game
                // can move to next unit
                logger.finest("Ending privateer (" + unit.getId() + ") turn, moves=" + unit.getMovesLeft());
                unit.setMovesLeft(0);
                return;
            }
            // catch up with the prey
            if (unit.getMoveType(direction) == MoveType.ATTACK) {
                logger.finest("Privateer (" + unit.getId() + ") at " + unit.getTile() + " attacking target");
                AIMessage.askAttack(getAIUnit(), direction);
            }
        } else {
            // No target found, just make a random move
            target = null;
            logger.finest("Privateer at " + unit.getTile() + " without target, wandering");
        	moveRandomly(connection);
        }
        // some movement points may still remain
    	//due to some block or just not enough points for next node
    	// we need to make sure the unit has no points left,
    	//so the game can move to next unit
    	unit.setMovesLeft(0);
    }

    private void gotoNearestPort(Connection connection){
        Unit unit = getUnit();

        if(isUnitInPort()){
            dumpCargoInPort(connection);
            state = PrivateerMissionState.HUNTING;
            return;
        }

        PathNode path = getValidPathForNearestPort();
        if(path == null){
            findNearestPort();
            if(nearestPort == null){
            	logger.finest("Failed to find port for goods");
                return;
            }
            path = getValidPathForNearestPort();
            if(path == null){
            	logger.finest("Failed to deliver goods to " + nearestPort + ", no path");
                return;
            }
        }

        boolean moveToEurope = nearestPort instanceof Europe;
        Direction direction = moveTowards(path);
        if (direction == null) {
            unit.setMovesLeft(0);
            return;
        }

        if (moveToEurope && unit.getMoveType(direction) == MoveType.MOVE_HIGH_SEAS) {
        	moveUnitToEurope();
        	unit.setMovesLeft(0);
        	return;
        }

        if(unit.getMoveType(direction) == MoveType.MOVE){
        	Position unitPos = unit.getTile().getPosition();
        	Position ColPos = unitPos.getAdjacent(direction);
        	Colony colony = getGame().getMap().getTile(ColPos).getColony();
        	if(colony == nearestPort){
              AIMessage.askMove(getAIUnit(), direction);
              return;
        	}
        	else{
        		String errMsg = "Privateer (" + unit.getId() + ") with PrivateerMission trying to enter settlement";
        		throw new IllegalStateException(errMsg);
        	}
        }

        // some movement points may still remain
    	//due to some block or just not enough points for next node
    	// we need to make sure the unit has no points left,
    	//so the game can move to next unit
    	unit.setMovesLeft(0);
    }

    private PathNode getValidPathForNearestPort(){
        Unit unit = getUnit();
        Player player = unit.getOwner();

        if(nearestPort == null){
        	return null;
        }

        if(nearestPort instanceof Europe){
            if(player.getEurope() == null){
                nearestPort = null;
                return null;
            }
            return unit.findPathToEurope();
        }

        Colony nearestColony = (Colony) nearestPort;
        if(nearestColony == null
        		|| nearestColony.isDisposed()
        		|| nearestColony.getOwner() != player){
            nearestPort = null;
            return null;
        }

        return unit.findPath(nearestColony.getTile());
    }

    private void findNearestPort(){
        nearestPort = null;
        Unit unit = getUnit();

        PathNode path = findNearestColony(unit);
        if(path != null){
            nearestPort = path.getLastNode().getTile().getColony();
        }
        else{
            Europe europe = unit.getOwner().getEurope();
            if(europe != null){
                nearestPort = europe;
            }
        }
    }

    private boolean isUnitInPort(){
        if(nearestPort == null){
            return false;
        }

        Unit unit = getUnit();

        if(nearestPort instanceof Europe){
            return unit.getLocation() == nearestPort;
        }

        return unit.getTile() == nearestPort.getTile();
    }

    private void dumpCargoInPort(Connection connection){
    	logger.finest("Dumping goods");
        Unit unit = getUnit();
        boolean inEurope = unit.getLocation() instanceof Europe;

        List<Goods> goodsLst = new ArrayList<Goods>(unit.getGoodsList());
        for(Goods goods : goodsLst){
            if(inEurope){
            	logger.finest("Before dumping: money=" + unit.getOwner().getGold());
                sellCargoInEurope(goods);
            	logger.finest("After dumping: money=" + unit.getOwner().getGold());
            } else{
            	Colony colony = unit.getTile().getColony();
            	logger.finest("Before dumping: " +  colony.getGoodsCount(goods.getType()) + " " + goods.getType());
                unloadCargoInColony(goods);
            	logger.finest("After dumping: " +  colony.getGoodsCount(goods.getType()) + " " + goods.getType());
            }
        }

        List<Unit> unitLst = new ArrayList<Unit>(unit.getUnitList());
        for(Unit u : unitLst){
            unitLeavesShip((AIUnit) getAIMain().getAIObject(u));
        }
    }

    /**
     * Checks if this mission is valid for the given unit.
     *
     * @param aiUnit The unit.
     * @return <code>true</code> if this mission is valid to perform
     *         and <code>false</code> otherwise.
     */
    public static boolean isValid(AIUnit aiUnit) {
        Unit unit = aiUnit.getUnit();
        AIPlayer aiPlayer = (AIPlayer) aiUnit.getAIMain().getAIObject(unit.getOwner().getId());
        return unit != null
            && unit.isNaval() && unit.hasAbility("model.ability.piracy")
            && !unit.isUnderRepair()
        		&& unit.getGoodsCount() == 0
            && unit.getUnitCount() == 0
            && TransportMission.getPlayerNavalTransportMissionCount(aiPlayer, unit) != 0;
    }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if the mission is still valid.
     */
    public boolean isValid() {
        return super.isValid() && !isValid(getAIUnit());
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
        out.writeAttribute("state", state.toString());

        out.writeEndElement();
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setAIUnit((AIUnit) getAIMain().getAIObject(in.getAttributeValue(null, "unit")));
        state = PrivateerMissionState.valueOf(in.getAttributeValue(null, "state"));
        in.nextTag();
    }

    /**
    * Returns the tag name of the root element representing this object.
    * @return The <code>String</code> "unitWanderHostileMission".
    */
    public static String getXMLElementTagName() {
        return "privateerMission";
    }

    /**
     * Gets debugging information about this mission. This string is a short
     * representation of this object's state.
     *
     */
    public String getDebuggingInfo() {
    	StringBuffer sb = new StringBuffer("State: " + state.name());
    	if(state == PrivateerMissionState.HUNTING && target != null){
    		Unit targetUnit = target.getDefendingUnit(getUnit());
    		if(targetUnit != null){
    			String coord = " (" + target.getX() + "," + target.getY() + ")";
    			sb.append(" target=" + targetUnit + coord);
    		}
    	}
        return sb.toString();
    }

    /**
     * Calculates the modifier used when assessing the value of a target to a privateer
     * Note: it gives a modifier value, other parameters should be considered as well
     * Note: we assume the unit given is a privateer, no test is made
     * @param combatModel The <code>Combat Model</code> used.
     * @param attacker The <code>Unit</code> attacking, should be a privateer.
     * @param defender The <code>Unit</code> the attacker is considering as a target.
     * @return The modifier value the defender is worth as a target to the privateer
     */
    public static int getModifierValueForTarget(CombatModel combatModel, Unit attacker, Unit defender){
    	// pirates are greedy ;)
    	int modifier = 100;
    	modifier += defender.getGoodsCount() * 200;
    	modifier += defender.getUnitCount() * 100;

        // they are also coward
        if(defender.isOffensiveUnit()){
        	modifier -= combatModel.getDefencePower(attacker, defender) * 100;
        }

        return modifier;
    }
}
