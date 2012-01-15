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

import java.util.List;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.networking.Connection;
import net.sf.freecol.server.ai.AIMain;
import net.sf.freecol.server.ai.AIMessage;
import net.sf.freecol.server.ai.AIUnit;

import org.w3c.dom.Element;


/**
 * Mission for defending a <code>Settlement</code>.
 * @see net.sf.freecol.common.model.Settlement Settlement
 */
public class DefendSettlementMission extends Mission {
    /*
     * TODO: This Mission should later use sub-missions for
     *       eliminating threats etc.
     */
    private static final Logger logger = Logger.getLogger(DefendSettlementMission.class.getName());


    /** The <code>Settlement</code> to be protected. */
    private Settlement settlement;

    //private Mission subMission;


    /**
     * Creates a mission for the given <code>AIUnit</code>.
     * @param aiMain The main AI-object.
     * @param aiUnit The <code>AIUnit</code> this mission
     *        is created for.
     * @param settlement The <code>Settlement</code> to defend.
     * @exception NullPointerException if <code>aiUnit == null</code> or
     *        <code>settlement == null</code>.
     */
    public DefendSettlementMission(AIMain aiMain, AIUnit aiUnit, Settlement settlement) {
        super(aiMain, aiUnit);

        this.settlement = settlement;
    }

    /**
     * Creates a new <code>DefendSettlementMission</code>.
     *
     * @param aiMain The main AI-object.
     * @param element An <code>Element</code> containing an
     *      XML-representation of this object.
     */
    public DefendSettlementMission(AIMain aiMain, Element element) {
        super(aiMain);
        readFromXMLElement(element);
    }

    /**
     * Creates a new <code>DefendSettlementMission</code> and reads
     * the given element.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered
     *      during parsing.
     * @see net.sf.freecol.server.ai.AIObject#readFromXML
     */
     public DefendSettlementMission(AIMain aiMain, XMLStreamReader in) throws XMLStreamException {
         super(aiMain);
         readFromXML(in);
     }

    /**
     * Performs this mission.
     *
     * @param connection The <code>Connection</code> to the server.
     */
    public void doMission(Connection connection) {
        final Unit unit = getUnit();
        if (!isValid() || unit.getTile() == null) return;
        final Tile settlementTile = settlement.getTile();

        if (unit.getTile() != settlementTile) { // Go home!
            Direction r = moveTowards(settlementTile);
            if (r == null || !moveButDontAttack(r)) return;
        }

        int defenderCount = 0, fortifiedCount = 0;
        List<Unit> units = settlement.getUnitList();
        units.addAll(settlementTile.getUnitList());
        for (Unit u : units) {
            if (unit.isOffensiveUnit()) {
                defenderCount++;
                if (unit.getState() == UnitState.FORTIFIED) {
                    fortifiedCount++;
                }
            }
        }
        if (defenderCount <= 2 || fortifiedCount <= 1) {
            // The settlement is badly defended, go straight home if not
            // there, otherwise fortify if not already.
            if (unit.getTile() == settlementTile
                && unit.getState() != UnitState.FORTIFIED
                && unit.getState() != UnitState.FORTIFYING
                && unit.checkSetState(UnitState.FORTIFYING)) {
                AIMessage.askChangeState(getAIUnit(), UnitState.FORTIFYING);
            }
            return;
        }

        // The settlement is well enough defended.  (must prevent a
        // sole unit losing an offensive combat because the combat
        // model does not understand that).
        if (!unit.isOffensiveUnit()) return;
        CombatModel combatModel = unit.getGame().getCombatModel();
        Unit bestTarget = null;
        float bestDifference = Float.MIN_VALUE;
        Direction bestDirection = null;
        for (Direction direction : Direction.getRandomDirectionArray(getAIRandom())) {
            Tile t = unit.getTile().getNeighbourOrNull(direction);
            if (t == null) continue;
            Unit defender = t.getFirstUnit();
            if (defender != null
                && defender.getOwner().atWarWith(unit.getOwner())
                && unit.getMoveType(direction).isAttack()) {
                Unit enemyUnit = t.getDefendingUnit(unit);
                float enemyAttack = combatModel.getOffencePower(enemyUnit, unit);
                float weAttack = combatModel.getOffencePower(unit, enemyUnit);
                float enemyDefend = combatModel.getDefencePower(unit, enemyUnit);
                float weDefend = combatModel.getDefencePower(enemyUnit, unit);
                
                float difference = weAttack / (weAttack + enemyDefend) - enemyAttack / (enemyAttack + weDefend);
                if (difference > bestDifference) {
                    if (difference > 0 || weAttack > enemyDefend) {
                        bestDifference = difference;
                        bestTarget = enemyUnit;
                        bestDirection = direction;
                    }
                }
            }
        }

        if (bestTarget != null) {
            AIMessage.askAttack(getAIUnit(), bestDirection);
        }
    }

    /**
     * Returns the destination for this <code>Transportable</code>.
     * This can either be the target {@link Tile} of the transport
     * or the target for the entire <code>Transportable</code>'s
     * mission. The target for the transport is determined by
     * {@link TransportMission} in the latter case.
     *
     * @return The destination for this <code>Transportable</code>.
     */
     public Tile getTransportDestination() {
         if (settlement == null) {
             return null;
         } else if (getUnit().isOnCarrier()) {
             return settlement.getTile();
         } else if (getUnit().getLocation().getTile() == settlement.getTile()) {
             return null;
         } else if (getUnit().getTile() == null || getUnit().findPath(settlement.getTile()) == null) {
             return settlement.getTile();
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
            return NORMAL_TRANSPORT_PRIORITY + 5;
        } else {
            return 0;
        }
     }

     /**
      * Gets the settlement.
      * @return The <code>Settlement</code> to be defended by
      *         this <code>Mission</code>.
      */
     public Settlement getSettlement() {
         return settlement;
     }

    /**
     * Checks if this mission is still valid to perform.
     *
     * @return True if this mission is still valid to perform.
     */
    public boolean isValid() {
        return super.isValid()
            && settlement != null && !settlement.isDisposed()
            && settlement.getOwner() == getUnit().getOwner()
            && getUnit().isDefensiveUnit();
    }

    /**
     * Gets debugging information about this mission.
     * This string is a short representation of this
     * object's state.
     *
     * @return The <code>String</code>:
     *      "(x, y) ColonyName"
     *      where <code>x</code> and <code>y</code> is the
     *      coordinates of the settlement for this mission,
     *      and <code>ColonyName</code> is the name
     *      (if available).
     */
    public String getDebuggingInfo() {
        String name = (settlement instanceof Colony) ? ((Colony) settlement).getName() : "";
        return settlement.getTile().getPosition().toString() + " " + name;
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
        toXML(out, getXMLElementTagName());
    }

    protected void writeAttributes(XMLStreamWriter out) throws XMLStreamException {
        super.writeAttributes(out);
        if (settlement != null) {
            out.writeAttribute("settlement", settlement.getId());
        }
    }

    /**
     * Reads all the <code>AIObject</code>s and other AI-related information
     * from XML data.
     *
     * @param in The input stream with the XML.
     */
    protected void readAttributes(XMLStreamReader in) throws XMLStreamException {
        super.readAttributes(in);
        settlement = (Settlement) getGame()
            .getFreeColGameObject(in.getAttributeValue(null, "settlement"));
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return "defendSettlementMission".
     */
    public static String getXMLElementTagName() {
        return "defendSettlementMission";
    }
}
