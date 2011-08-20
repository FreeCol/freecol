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

package net.sf.freecol.server.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Ownable;
import net.sf.freecol.common.model.PathNode;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Player.PlayerType;
import net.sf.freecol.common.model.Player.Stance;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.ai.mission.DefendSettlementMission;
import net.sf.freecol.server.ai.mission.Mission;
import net.sf.freecol.server.ai.mission.UnitSeekAndDestroyMission;
import net.sf.freecol.server.model.ServerPlayer;

import org.w3c.dom.Element;


/**
 * Objects of this class contains AI-information for a single REF player.
 *
 * For now, mostly just the EuropeanAIPlayer, with a few tweaks.
 */
public class REFAIPlayer extends EuropeanAIPlayer {

    private static final Logger logger = Logger.getLogger(REFAIPlayer.class.getName());


    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-class.
     * @param player The player that should be associated with this
     *            <code>REFAIPlayer</code>.
     */
    public REFAIPlayer(AIMain aiMain, ServerPlayer player) {
        super(aiMain, player);
    }

    /**
     * Creates a new <code>REFAIPlayer</code> and reads the
     * information from the given <code>Element</code>.
     *
     * @param aiMain The main AI-class.
     * @param element The XML-element containing information.
     */
    public REFAIPlayer(AIMain aiMain, Element element) {
        super(aiMain, element);
    }

    /**
     * Creates a new <code>REFAIPlayer</code>.
     *
     * @param aiMain The main AI-object.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public REFAIPlayer(AIMain aiMain, XMLStreamReader in)
        throws XMLStreamException {
        super(aiMain, in);
    }


    /**
     * Tells this <code>REFAIPlayer</code> to make decisions.
     */
    public void startWorking() {
        final Player player = getPlayer();
        logger.finest("Entering method startWorking: "
            + player + ", year " + getGame().getTurn());
        if (!player.isWorkForREF()) {
            logger.warning("No work for REF: " + player);
            return;
        }
        super.startWorking();
    }


    /**
     * Evaluate allocating a unit to the defence of a colony.
     * Temporary helper method for giveMilitaryMission.
     *
     * Only public for testAssignDefendSettlementMission.
     *
     * @param unit The <code>Unit</code> that is to defend.
     * @param colony The <code>Colony</code> to defend.
     * @param turns The turns for the unit to reach the colony.
     * @return A value for such a mission.
     */
    @Override
    public int getDefendColonyMissionValue(Unit unit, Colony colony,
                                           int turns) {
        int value = super.getDefendColonyMissionValue(unit, colony, turns);

        // The REF garrisons thinly.
        return (getColonyDefenders(colony) > 0) ? 0 : value;
    }

    /**
     * Evaluate a potential seek and destroy mission for a given unit
     * to a given tile.
     * TODO: revisit and rebalance the mass of magic numbers.
     *
     * @param unit The <code>Unit</code> to do the mission.
     * @param newTile The <code>Tile</code> to go to.
     * @param turns How long to travel to the tile.
     * @return A score for the proposed mission.
     */
    @Override
    public int getUnitSeekAndDestroyMissionValue(Unit unit, Tile newTile,
                                                 int turns) {
        int value = super.getUnitSeekAndDestroyMissionValue(unit, newTile,
                                                            turns);
        if (value <= 0) return value;

        Settlement settlement = newTile.getSettlement();
        Unit defender = newTile.getDefendingUnit(unit);
        if (settlement == null) {
            // Do not all chase the one unit!
            if (alreadySeeking(defender)) return 0;

            // The REF is more interested in colonies.
            value /= 2;
        } else {
            if (settlement.isConnected()) value += 1000;
        }
        return value;
    }

    /**
     * Checks if there is already a seek and destroy mission active on
     * for a target unit.
     *
     * @param unit The <code>Unit</code> to check if there is a mission for.
     * @return True if there is a mission for the unit.
     */
    private boolean alreadySeeking(Unit unit) {
        for (AIUnit au : getAIUnits()) {
            Mission m = au.getMission();
            Location target;
            if (m != null
                && m instanceof UnitSeekAndDestroyMission
                && (target = ((UnitSeekAndDestroyMission) m).getTarget()) != null
                && target instanceof Unit
                && ((Unit)target) == unit) return true;
        }
        return false;
    }

    /**
     * Gives a mission to non-naval units.
     */
    @Override
    public void giveNormalMissions() {
        // Give military missions to all offensive units.
        for (AIUnit aiu : getAIUnits()) {
            Unit u = aiu.getUnit();
            if (u.isNaval()) continue;
            if (u.isOffensiveUnit()) giveMilitaryMission(aiu);
        }

        // Fall back to the normal EuropeanAI behaviour for non-army.
        super.giveNormalMissions();
    }
}
