package net.sf.freecol.server;

import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.server.FreeColServer.GameState;
import net.sf.freecol.server.control.Controller;
import net.sf.freecol.server.control.InGameController;
import net.sf.freecol.server.control.PreGameController;
import net.sf.freecol.server.control.PreGameInputHandler;
import net.sf.freecol.server.control.UserConnectionHandler;
import net.sf.freecol.server.model.ServerPlayer;

public class FreeColServerControllers {
    /**
     * Stores the current state of the game.
     */
    private FreeColServer.GameState gameState = GameState.STARTING_GAME;
    private final UserConnectionHandler userConnectionHandler;
    private final PreGameController preGameController;
    private final PreGameInputHandler preGameInputHandler;
    private final InGameController inGameController;
    private String owner;

    /**
     * @param userConnectionHandler
     * @param preGameController
     * @param preGameInputHandler
     * @param inGameController
     */
    public FreeColServerControllers(
                                    UserConnectionHandler userConnectionHandler,
                                    PreGameController preGameController,
                                    PreGameInputHandler preGameInputHandler,
                                    InGameController inGameController) {
        super();
        this.userConnectionHandler = userConnectionHandler;
        this.preGameController = preGameController;
        this.preGameInputHandler = preGameInputHandler;
        this.inGameController = inGameController;
    }

    /**
     * Gets the owner of the <code>Game</code>.
     *
     * @return The owner of the game. THis is the player that has loaded the
     *         game (if any).
     * @see #loadGame
     */
    public String getOwner() {
        return owner;
    }

    /**
     * Gets the <code>UserConnectionHandler</code>.
     *
     * @return The <code>UserConnectionHandler</code> that is beeing used when
     *         new client connect.
     */
    public UserConnectionHandler getUserConnectionHandler() {
        return userConnectionHandler;
    }

    /**
     * Gets the <code>Controller</code>.
     *
     * @return The <code>Controller</code>.
     */
    public Controller getController() {
        if (getGameState() == GameState.IN_GAME) {
            return inGameController;
        } else {
            return preGameController;
        }
    }

    /**
     * Gets the <code>PreGameInputHandler</code>.
     *
     * @return The <code>PreGameInputHandler</code>.
     */
    public PreGameInputHandler getPreGameInputHandler() {
        return preGameInputHandler;
    }

    /**
     * Gets the controller being used while the game is running.
     *
     * @return The controller from making a new turn etc.
     */
    public InGameController getInGameController() {
        return inGameController;
    }

    /**
     * Gets the current state of the game.
     *
     * @return One of: {@link net.sf.freecol.server.FreeColServer.GameState#STARTING_GAME},
     *                 {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME} and
     *                 {@link net.sf.freecol.server.FreeColServer.GameState#ENDING_GAME}.
     */
    public FreeColServer.GameState getGameState() {
        return gameState;
    }

    /**
     * Sets the current state of the game.
     *
     * @param state The new state to be set. One of:
     *        {@link net.sf.freecol.server.FreeColServer.GameState#STARTING_GAME},
     *        {@link net.sf.freecol.server.FreeColServer.GameState#IN_GAME} and
     *        {@link net.sf.freecol.server.FreeColServer.GameState#ENDING_GAME}.
     */
    public void setGameState(FreeColServer.GameState state) {
        gameState = state;
    }

    /**
     * Get a unit by ID, validating the ID as much as possible.  Designed for
     * message unpacking where the ID should not be trusted.
     *
     * @param unitId       The ID of the unit to be found.
     * @param serverPlayer The <code>ServerPlayer</code> to whom the unit must belong.
     * @return The unit corresponding to the unitId argument.
     * @throws IllegalStateException on failure to validate the unitId
     *                               in any way.
     *                               In the worst case this may be indicative of a malign client.
     */
    public Unit getUnitSafely(String unitId, ServerPlayer serverPlayer)
        throws IllegalStateException {
        Game game = serverPlayer.getGame();
        FreeColGameObject obj;
        Unit unit;

        if (unitId == null || unitId.length() == 0) {
            throw new IllegalStateException("ID must not be empty.");
        }
        obj = game.getFreeColGameObjectSafely(unitId);
        if (obj == null) {
            throw new IllegalStateException("Not an object: " + unitId);
        } else if (!(obj instanceof Unit)) {
            throw new IllegalStateException("Unit expected, "
                                            + " got " + obj.getClass()
                                            + ": " + unitId);
        }
        unit = (Unit) obj;
        if (unit.getOwner() != serverPlayer) {
            throw new IllegalStateException("Not the owner of unit: " + unitId);
        }
        return unit;
    }

    /**
     * Get a settlement by ID, validating the ID as much as possible.
     * Designed for message unpacking where the ID should not be trusted.
     *
     * @param settlementId The ID of the <code>Settlement</code> to be found.
     * @param unit         A <code>Unit</code> which must be adjacent
     *                     to the <code>Settlement</code>.
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *                               in any way.
     *                               In the worst case this may be indicative of a malign client.
     */
    public Settlement getAdjacentSettlementSafely(String settlementId, Unit unit)
        throws IllegalStateException {
        Game game = unit.getOwner().getGame();
        Settlement settlement;

        if (settlementId == null || settlementId.length() == 0) {
            throw new IllegalStateException("ID must not be empty.");
        } else if (!(game.getFreeColGameObject(settlementId) instanceof Settlement)) {
            throw new IllegalStateException("Not a settlement ID: " + settlementId);
        }
        settlement = (Settlement) game.getFreeColGameObject(settlementId);
        if (settlement.getTile() == null) {
            throw new IllegalStateException("Settlement is not on the map: "
                                            + settlementId);
        }
        if (unit.getTile() == null) {
            throw new IllegalStateException("Unit is not on the map: "
                                            + unit.getId());
        }
        if (unit.getTile().getDistanceTo(settlement.getTile()) > 1) {
            throw new IllegalStateException("Unit " + unit.getId()
                                            + " is not adjacent to settlement: " + settlementId);
        }
        if (unit.getOwner() == settlement.getOwner()) {
            throw new IllegalStateException("Unit: " + unit.getId()
                                            + " and settlement: " + settlementId
                                            + " are both owned by player: "
                                            + unit.getOwner().getId());
        }
        return settlement;
    }

    /**
     * Get an adjacent Indian settlement by ID, validating as much as possible,
     * including checking whether the nation involved has been contacted.
     * Designed for message unpacking where the ID should not be trusted.
     *
     * @param settlementId The ID of the <code>Settlement</code> to be found.
     * @param unit         A <code>Unit</code> which must be adjacent
     *                     to the <code>Settlement</code>.
     * @return The settlement corresponding to the settlementId argument.
     * @throws IllegalStateException on failure to validate the settlementId
     *                               in any way.
     *                               In the worst case this may be indicative of a malign client.
     */
    public IndianSettlement getAdjacentIndianSettlementSafely(String settlementId, Unit unit)
        throws IllegalStateException {
        Settlement settlement = getAdjacentSettlementSafely(settlementId, unit);
        if (!(settlement instanceof IndianSettlement)) {
            throw new IllegalStateException("Not an indianSettlement: " + settlementId);
        }
        if (!unit.getOwner().hasContacted(settlement.getOwner())) {
            throw new IllegalStateException("Player has not established contact with the "
                                            + settlement.getOwner().getNation());
        }
        return (IndianSettlement) settlement;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
}