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

package net.sf.freecol.server.ai.mission;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.MoveType;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;


public class PrivateerMission extends Mission {

    private static final Logger logger = Logger.getLogger(PrivateerMission.class.getName());

    private static String tag = "AI privateer";

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
        logger.finest(tag + " begins at " + unit.getLocation() + ": " + this);
        uninitialized = false;
    }

    /**
     * Creates a new <code>UnitWanderHostileMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
    public PrivateerMission(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain);

        readFromXML(in);
        uninitialized = getAIUnit() == null;
    }


    // Mission interface

    /**
     * Gets the target for this mission.
     *
     * @return The target for this mission.
     */
    public Location getTarget() {
        return target;
    }

    /**
     * Why would a PrivateeringMission be invalid with the given unit.
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason why the mission would be invalid with the unit,
     *     or null if none found.
     */
    private static String invalidMissionReason(AIUnit aiUnit) {
        String reason = invalidAIUnitReason(aiUnit);
        if (reason != null) return reason;
        final Unit unit = aiUnit.getUnit();
        return (!unit.isCarrier()) ? "unit-not-a-carrier"
            : (!unit.isOffensiveUnit()) ? Mission.UNITNOTOFFENSIVE
            : (!unit.hasAbility(Ability.PIRACY)) ? "unit-not-a-pirate"
            : (unit.getGoodsCount() > 0) ? "unit-has-goods"
            : (unit.getUnitCount() > 0) ? "unit-has-units"
            : null;
    }

    /**
     * Why is this mission invalid?
     *
     * @return A reason for the mission invalidity, or null if still valid.
     */
    public String invalidReason() {
        return invalidReason(getAIUnit(), target);
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit) {
        return invalidMissionReason(aiUnit);
    }

    /**
     * Why would this mission be invalid with the given AI unit?
     *
     * @param aiUnit The <code>AIUnit</code> to check.
     * @param loc The <code>Location</code> to check.
     * @return A reason for mission invalidity, or null if none found.
     */
    public static String invalidReason(AIUnit aiUnit, Location loc) {
        return invalidMissionReason(aiUnit);
    }

    // Not a one-time mission, omit isOneTime().

    /**
     * Performs the mission. This is done by searching for hostile units
     * that are located within one tile and attacking them. If no such units
     * are found, then wander in a random direction.
     */
    public void doMission() {
        Unit unit = getUnit();
        while(isValid() && unit.getMovesLeft() > 0){
            // Unit is between Europe and America, nothing to do
            if (unit.isAtSea()){
                unit.setMovesLeft(0);
                return;
            }
            switch(state){
            case HUNTING:
                hunt4Target();
                break;
            case TRANSPORTING:
                gotoNearestPort();
                break;
            }
        }
    }

    private void hunt4Target() {
        Unit unit = getUnit();

        if (unit.getLocation() instanceof Europe){
            getAIUnit().moveToAmerica();
            unit.setMovesLeft(0);
            return;
        }

        // has captured goods, must get them to port
        if (unit.getGoodsCount() > 0) {
            state = PrivateerMissionState.TRANSPORTING;
            return;
        }

        final int MAX_TURNS_TO_TARGET = 1;
        PathNode pathToTarget = findTarget(MAX_TURNS_TO_TARGET);
        // Found a target
        if (pathToTarget != null) {
            target = pathToTarget.getLastNode().getTile();
            logger.finest(tag + " at " + unit.getTile()
                + " found target at " + target + ": " + this);
            // We need to find an updated path to target
            Direction direction;
            if (travelToTarget(tag, target) == MoveType.ATTACK_UNIT
                && (direction = unit.getTile().getDirection(target)) != null) {
                logger.finest(tag + " at " + unit.getTile()
                    + " attacking " + target.getDefendingUnit(unit)
                    + ": " + this);
                AIMessage.askAttack(getAIUnit(), direction);
            }
        } else {
            // No target found, just make a random move
            target = null;
            logger.finest(tag + " at " + unit.getTile()
                + " without target, wandering");
            moveRandomly(tag, null);
        }
    }

    private void gotoNearestPort() {
        final Unit unit = getUnit();

        if (isUnitInPort()) {
            dumpCargoInPort();
            state = PrivateerMissionState.HUNTING;
            return;
        }

        PathNode path = unit.findFullPath(nearestPort);
        if (path == null) {
            if ((path = unit.findOurNearestPort()) == null) {
                logger.finest(tag + " failed to find port for goods: " + this);
                return;
            }
            nearestPort = path.getLastNode().getLocation();
        }

        if (followPath(tag, path) != MoveType.MOVE) return;

        if (isUnitInPort()) {
            dumpCargoInPort();
            state = PrivateerMissionState.HUNTING;
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

    private void dumpCargoInPort() {
        logger.finest(tag + " dumping goods: " + this);
        Unit unit = getUnit();
        boolean inEurope = unit.getLocation() instanceof Europe;

        List<Goods> goodsLst = new ArrayList<Goods>(unit.getGoodsList());
        for(Goods goods : goodsLst){
            if(inEurope){
                goodsLeavesTransport(goods.getType(), goods.getAmount());
            } else {
                Colony colony = unit.getTile().getColony();
                unloadCargoInColony(goods);
            }
        }

        for (Unit u : unit.getUnitList()) {
            unitLeavesTransport(getAIMain().getAIUnit(u), null);
        }
    }

    /**
     * Calculates the modifier used when assessing the value of a
     * target to a privateer.
     * Note: it gives a modifier value, other parameters should be
     * considered as well
     * Note: we assume the unit given is a privateer, no test is made
     *
     * @param combatModel The <code>Combat Model</code> used.
     * @param attacker The <code>Unit</code> attacking, should be a privateer.
     * @param defender The <code>Unit</code> the attacker is considering
     *            as a target.
     * @return The modifier value the defender is worth as a target to
     *     the privateer
     */
    public static int getModifierValueForTarget(CombatModel combatModel,
        Unit attacker, Unit defender) {
        // pirates are greedy ;)
        int modifier = 100;
        modifier += defender.getGoodsCount() * 200;
        modifier += defender.getUnitCount() * 100;

        // they are also coward
        if (defender.isOffensiveUnit()) {
            modifier -= combatModel.getDefencePower(attacker, defender) * 100;
        }

        return modifier;
    }


    // Serialization

    /**
     * Writes all of the <code>AIObject</code>s and other AI-related
     * information to an XML-stream.
     *
     * @param out The target stream.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        toXML(out, getXMLElementTagName());
    }

    /**
     * {@inheritDoc}
     */
    protected void writeAttributes(XMLStreamWriter out)
        throws XMLStreamException {
        super.writeAttributes(out);

        out.writeAttribute("state", state.toString());
    }

    /**
     * {@inheritDoc}
     */
    protected void readAttributes(XMLStreamReader in)
        throws XMLStreamException {
        super.readAttributes(in);

        state = PrivateerMissionState.valueOf(in.getAttributeValue(null,
                "state"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "privateerMission"
     */
    public static String getXMLElementTagName() {
        return "privateerMission";
    }
}
