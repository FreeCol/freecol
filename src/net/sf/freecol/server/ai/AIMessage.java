/**
 *  Copyright (C) 2002-2016   The FreeCol Team
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
import java.util.List;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Goods;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Location;
import net.sf.freecol.common.model.NationSummary;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileImprovementType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Wrapper class for AI message handling.
 */
public class AIMessage {

    /**
     * An AIUnit attacks in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to attack with.
     * @param direction The <code>Direction</code> to attack in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askAttack(AIUnit aiUnit, Direction direction) {
        return aiUnit.getAIOwner().askServer()
            .attack(aiUnit.getUnit(), direction);
    }

    /**
     * An AIUnit builds a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to build the colony.
     * @param name The name of the colony.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askBuildColony(AIUnit aiUnit, String name) {
        return aiUnit.getAIOwner().askServer()
            .buildColony(name, aiUnit.getUnit());
    }

    /**
     * An AIUnit cashes in.
     *
     * @param aiUnit The <code>AIUnit</code> cashing in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCashInTreasureTrain(AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .cashInTreasureTrain(aiUnit.getUnit());
    }

    /**
     * An AIUnit changes state.
     *
     * @param aiUnit The <code>AIUnit</code> to change the state of.
     * @param state The new <code>UnitState</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeState(AIUnit aiUnit, UnitState state) {
        return aiUnit.getAIOwner().askServer()
            .changeState(aiUnit.getUnit(), state);
    }

    /**
     * An AIUnit changes its work type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>GoodsType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkType(AIUnit aiUnit, GoodsType type) {
        return aiUnit.getAIOwner().askServer()
            .changeWorkType(aiUnit.getUnit(), type);
    }

   /**
     * An AIUnit changes its work improvement type.
     *
     * @param aiUnit The <code>AIUnit</code> to change the work type of.
     * @param type The <code>TileImprovementType</code> to produce.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askChangeWorkImprovementType(AIUnit aiUnit,
        TileImprovementType type) {
        return aiUnit.getAIOwner().askServer()
            .changeWorkImprovementType(aiUnit.getUnit(), type);
    }

    /**
     * Claims a tile for a colony.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param aic The <code>AIColony</code> that is claiming.
     * @param price The price to pay.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClaimLand(Tile tile, AIColony aic, int price) {
        return aic.getAIOwner().askServer()
            .claimTile(tile, aic.getColony(), price);
    }

    /**
     * Claims a tile.
     *
     * @param tile The <code>Tile</code> to claim.
     * @param aiUnit The <code>AIUnit</code> that is claiming.
     * @param price The price to pay.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClaimLand(Tile tile, AIUnit aiUnit, int price) {
        return aiUnit.getAIOwner().askServer()
            .claimTile(tile, aiUnit.getUnit(), price);
    }

    /**
     * Clears the speciality of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to clear.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askClearSpeciality(AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .clearSpeciality(aiUnit.getUnit());
    }

    /**
     * An AIUnit closes a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that closes the transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askCloseTransaction(AIUnit aiUnit,
                                              Settlement settlement) {
        return aiUnit.getAIOwner().askServer()
            .closeTransactionSession(aiUnit.getUnit(), settlement);
    }

    /**
     * An AIUnit delivers a gift.
     *
     * @param aiUnit The <code>AIUnit</code> delivering the gift.
     * @param settlement The <code>Settlement</code> to give to.
     * @param goods The <code>Goods</code> to give.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDeliverGift(AIUnit aiUnit, Settlement settlement,
                                         Goods goods) {
        return aiUnit.getAIOwner().askServer()
            .deliverGiftToSettlement(aiUnit.getUnit(), settlement, goods);
    }

    /**
     * An AIUnit disbands.
     *
     * @param aiUnit The <code>AIUnit</code> to disband.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDisband(AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .disbandUnit(aiUnit.getUnit());
    }

    /**
     * An AIUnit disembarks.
     *
     * @param aiUnit The <code>AIUnit</code> disembarking.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askDisembark(AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .disembark(aiUnit.getUnit());
    }

    /**
     * An AIUnit embarks.
     *
     * @param aiUnit The <code>AIUnit</code> carrier.
     * @param unit The <code>Unit</code> that is embarking.
     * @param direction The <code>Direction</code> to embark in (may be null).
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmbark(AIUnit aiUnit, Unit unit,
                                    Direction direction) {
        return aiUnit.getAIOwner().askServer()
            .embark(unit, aiUnit.getUnit(), direction);
    }

    /**
     * A unit in Europe emigrates.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring emigration.
     * @param slot The slot to emigrate from.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEmigrate(AIPlayer aiPlayer, int slot) {
        return aiPlayer.askServer()
            .emigrate(aiPlayer.getGame(), slot);
    }

    /**
     * Ends the player turn.
     *
     * @param aiPlayer The <code>AIPlayer</code> ending the turn.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEndTurn(AIPlayer aiPlayer) {
        return aiPlayer.askServer()
            .endTurn(aiPlayer.getGame());
    }

    /**
     * Change the role of a unit.
     *
     * @param aiUnit The <code>AIUnit</code> to equip.
     * @param role The <code>Role</code> to equip for.
     * @param roleCount The role count.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEquipForRole(AIUnit aiUnit, Role role,
                                          int roleCount) {
        return aiUnit.getAIOwner().askServer()
            .equipUnitForRole(aiUnit.getUnit(), role, roleCount);
    }

    /**
     * Establishes a mission in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> establishing the mission.
     * @param direction The <code>Direction</code> to move the unit.
     * @param denounce Is this a denunciation?
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askEstablishMission(AIUnit aiUnit,
                                              Direction direction,
                                              boolean denounce) {
        return aiUnit.getAIOwner().askServer()
            .missionary(aiUnit.getUnit(), direction, denounce);
    }

    /**
     * Gets a nation summary for a player.
     *
     * @param owner The <code>AIPlayer</code> making the inquiry.
     * @param player The <code>Player</code> to summarize.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askNationSummary(AIPlayer owner, Player player) {
        return owner.askServer()
            .nationSummary(owner.getPlayer(), player);
    }

    /**
     * An AIUnit gets a transaction.
     *
     * @param aiUnit The <code>AIUnit</code> that gets a transaction.
     * @param settlement The target <code>Settlement</code>.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askGetTransaction(AIUnit aiUnit,
                                            Settlement settlement) {
        return aiUnit.getAIOwner().askServer()
            .openTransactionSession(aiUnit.getUnit(), settlement) != null;
    }

    /**
     * Makes demands to a colony.  One and only one of goods or gold is valid.
     *
     * @param aiUnit The <code>AIUnit</code> that is demanding.
     * @param colony The <code>Colony</code> to demand of.
     * @param type The <code>GoodsType</code> to demand.
     * @param amount The amount of goods to demand.
     * @return True if the message was sent, a non-error reply returned, and
     *     the demand was accepted.
     */
    public static boolean askIndianDemand(AIUnit aiUnit, Colony colony,
                                          GoodsType type, int amount) {
        return aiUnit.getAIOwner().askServer()
            .indianDemand(aiUnit.getUnit(), colony, type, amount);
    }

    /**
     * An AI unit loads some cargo.
     *
     * @param loc The <code>Location</code> where the goods are.
     * @param type The <code>GoodsType</code> to load.
     * @param amount The amount of goods to load.
     * @param aiUnit The <code>AIUnit</code> that is loading.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoadGoods(Location loc, GoodsType type,
                                       int amount, AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .loadGoods(loc, type, amount, aiUnit.getUnit());
    }

    /**
     * An AI unit loots some cargo.
     *
     * @param aiUnit The <code>AIUnit</code> that is looting.
     * @param defenderId The object identifier of the defending unit.
     * @param goods A list of <code>Goods</code> to loot.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askLoot(AIUnit aiUnit, String defenderId,
                                  List<Goods> goods) {
        return aiUnit.getAIOwner().askServer()
            .loot(aiUnit.getUnit(), defenderId, goods);
    }

    /**
     * Moves an AIUnit in the given direction.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param direction The <code>Direction</code> to move the unit.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMove(AIUnit aiUnit, Direction direction) {
        return aiUnit.getAIOwner().askServer()
            .move(aiUnit.getUnit(), direction);
    }

    /**
     * Moves an AIUnit across the high seas.
     *
     * @param aiUnit The <code>AIUnit</code> to move.
     * @param destination The <code>Location</code> to move to.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askMoveTo(AIUnit aiUnit, Location destination) {
        return aiUnit.getAIOwner().askServer()
            .moveTo(aiUnit.getUnit(), destination);
    }

    /**
     * An AIUnit is put outside a colony.
     *
     * @param aiUnit The <code>AIUnit</code> to put out.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askPutOutsideColony(AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .putOutsideColony(aiUnit.getUnit());
    }

    /**
     * Rearrange an AI colony.
     *
     * @param aiColony The <code>AIColony</code> to rearrange.
     * @param workers A list of worker <code>Unit</code>s that may change.
     * @param scratch A copy of the underlying <code>Colony</code> with the
     *     workers arranged as required.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askRearrangeColony(AIColony aiColony,
                                             List<Unit> workers,
                                             Colony scratch) {
        return aiColony.getAIOwner().askServer()
            .rearrangeColony(aiColony.getColony(), workers, scratch);
    }

    /**
     * An AI unit speaks to the chief of a native settlement.
     *
     * @param aiUnit The <code>AIUnit</code> that is scouting.
     * @param settlement The <code>IndianSettlement</code> to scout.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askScoutSpeakToChief(AIUnit aiUnit,
                                               IndianSettlement settlement) {
        return aiUnit.getAIOwner().askServer()
            .scoutSpeakToChief(aiUnit.getUnit(), settlement);
    }

    /**
     * Set the build queue in a colony.
     *
     * @param aiColony The <code>AIColony</code> that is building.
     * @param queue The list of <code>BuildableType</code>s to build.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askSetBuildQueue(AIColony aiColony,
                                           List<BuildableType> queue) {
        return aiColony.getAIOwner().askServer()
            .setBuildQueue(aiColony.getColony(), queue);
    }

    /**
     * Train unit in Europe.
     *
     * @param aiPlayer The <code>AIPlayer</code> requiring training.
     * @param type The <code>UnitType</code> to train.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askTrainUnitInEurope(AIPlayer aiPlayer,
                                               UnitType type) {
        return aiPlayer.askServer()
            .trainUnitInEurope(aiPlayer.getGame(), type);
    }


    /**
     * An AI unit unloads some cargo.
     *
     * @param type The <code>GoodsType</code> to unload.
     * @param amount The amount of goods to unload.
     * @param aiUnit The <code>AIUnit</code> that is unloading.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askUnloadGoods(GoodsType type, int amount,
                                         AIUnit aiUnit) {
        return aiUnit.getAIOwner().askServer()
            .unloadGoods(type, amount, aiUnit.getUnit());
    }

    /**
     * Set a unit to work in a work location.
     *
     * @param aiUnit The <code>AIUnit</code> to work.
     * @param workLocation The <code>WorkLocation</code> to work in.
     * @return True if the message was sent, and a non-error reply returned.
     */
    public static boolean askWork(AIUnit aiUnit, WorkLocation workLocation) {
        return aiUnit.getAIOwner().askServer()
            .work(aiUnit.getUnit(), workLocation);
    }
}
