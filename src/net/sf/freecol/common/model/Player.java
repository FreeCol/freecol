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

package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.Specification;
import net.sf.freecol.common.model.Map.Direction;
import net.sf.freecol.common.model.Map.Position;
import net.sf.freecol.common.model.Region.RegionType;
import net.sf.freecol.common.model.Unit.UnitState;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;

import org.w3c.dom.Element;

/**
 * Represents a player. The player can be either a human player or an AI-player.
 *
 * <br>
 * <br>
 *
 * In addition to storing the name, nation e.t.c. of the player, it also stores
 * various defaults for the player. One example of this is the
 * {@link #getEntryLocation entry location}.
 */
public class Player extends FreeColGameObject implements Nameable {

    private static final Logger logger = Logger.getLogger(Player.class.getName());

    public static final int SCORE_SETTLEMENT_DESTROYED = -40;
    public static final int SCORE_INDEPENDENCE_DECLARED = 100;
    public static final int SCORE_INDEPENDENCE_GRANTED = 1000;

    /**
     * The XML tag name for the set of founding fathers.
     */
    private static final String FOUNDING_FATHER_TAG = "foundingFathers";

    /**
     * The XML tag name for the stance array.
     */
    private static final String STANCE_TAG = "stance";

    /**
     * The XML tag name for the tension array.
     */
    private static final String TENSION_TAG = "tension";

    /**
     * The index of this player
     */
    private int index;


    /**
     * Constants for describing the stance towards a player.
     */
    public static enum Stance {
        WAR, CEASE_FIRE, PEACE, ALLIANCE
    }


    /**
     * Only used by AI - stores the tension levels, 0-1000 with 1000 maximum
     * hostility.
     */
    // TODO: move this to AIPlayer
    private java.util.Map<Player, Tension> tension = new HashMap<Player, Tension>();

    /**
     * Stores the stance towards the other players. One of: WAR, CEASE_FIRE,
     * PEACE and ALLIANCE.
     */
    private java.util.Map<String, Stance> stance = new HashMap<String, Stance>();

    private static final Color noNationColor = Color.BLACK;

    /**
     * Nation/NationType related variables.
     */

    /**
     * The name of this player. This defaults to the user name in case of a
     * human player and the rulerName of the NationType in case of an AI player.
     */
    private String name;

    public static final String UNKNOWN_ENEMY = "unknown enemy";

    /**
     * The NationType of this player.
     */
    private NationType nationType;

    /**
     * The nation ID of this player, e.g. "model.nation.dutch".
     */
    private String nationID;

    /**
     * The name this player uses for the New World.
     */
    private String newLandName = null;

    // Represented on the network as "color.getRGB()":
    private Color color = Color.BLACK;

    private boolean admin;

    /**
     * The current score of this player.
     */
    private int score;

    private int gold;

    // only used to store the number of native settlements
    private int numberOfSettlements;

    /** The market for Europe. */
    private Market market;

    private Europe europe;

    private Monarch monarch;

    private boolean ready;

    /** True if this is an AI player. */
    private boolean ai;

    /** True if player has been attacked by privateers. */
    private boolean attackedByPrivateers = false;

    private int oldSoL;

    private int crosses;

    private int bells;

    private boolean dead = false;

    // any founding fathers in this Player's congress
    final private Set<FoundingFather> allFathers = new HashSet<FoundingFather>();

    private FoundingFather currentFather;

    /** The current tax rate for this player. */
    private int tax = 0;

    private PlayerType playerType;


    public static enum PlayerType {
        NATIVE, COLONIAL, REBEL, INDEPENDENT, ROYAL, UNDEAD
    }


    private int crossesRequired = 12;

    // No need for a persistent storage of these variables:
    private int colonyNameIndex = 0;
    private EnumMap<RegionType, Integer> regionNameIndex = new EnumMap<RegionType, Integer>(RegionType.class);

    private Location entryLocation;

    /**
     * The Units this player owns.
     */
    private final java.util.Map<String, Unit> units = new HashMap<String, Unit>();

    private final Iterator<Unit> nextActiveUnitIterator = new UnitIterator(this, new ActivePredicate());

    private final Iterator<Unit> nextGoingToUnitIterator = new UnitIterator(this, new GoingToPredicate());

    // Settlements this player owns
    final private List<Settlement> settlements = new ArrayList<Settlement>();

    // Trade routes of this player
    private List<TradeRoute> tradeRoutes = new ArrayList<TradeRoute>();

    // Model messages for this player
    private final List<ModelMessage> modelMessages = new ArrayList<ModelMessage>();                                                                            

    // Temporary variables:
    protected boolean[][] canSeeTiles = null;

    /**
     * Contains the abilities and modifiers of this type.
     */
    private FeatureContainer featureContainer = new FeatureContainer();

    /**
     * Describe history here.
     */
    private List<HistoryEvent> history = new ArrayList<HistoryEvent>();


    /**
     *
     * This constructor should only be used by subclasses.
     *
     */
    protected Player() {
        // empty constructor
    }

    /**
     *
     * Creates an new AI <code>Player</code> with the specified name.
     *
     *
     *
     * @param game The <code>Game</code> this <code>Player</code> belongs
     *            to.
     *
     * @param name The name that this player will use.
     *
     * @param admin Whether or not this AI player shall be considered an Admin.
     *
     * @param ai Whether or not this AI player shall be considered an AI player
     *            (usually true here).
     *
     * @param nation The nation of the <code>Player</code>.
     *
     */
    public Player(Game game, String name, boolean admin, boolean ai, Nation nation) {
        this(game, name, admin, nation);
        this.ai = ai;
    }

    /**
     *
     * Creates a new <code>Player</code> with specified name.
     *
     *
     *
     * @param game The <code>Game</code> this <code>Player</code> belongs
     *            to.
     *
     * @param name The name that this player will use.
     *
     * @param admin 'true' if this Player is an admin,
     *
     * 'false' otherwise.
     *
     */
    public Player(Game game, String name, boolean admin) {
        this(game, name, admin, game.getVacantNation());
    }

    /**
     *
     * Creates a new (human) <code>Player</code> with specified name.
     *
     * @param game The <code>Game</code> this <code>Player</code> belongs
     *            to.
     *
     * @param name The name that this player will use.
     *
     * @param admin 'true' if this Player is an admin,
     *
     * 'false' otherwise.
     *
     * @param newNation The nation of the <code>Player</code>.
     *
     */
    public Player(Game game, String name, boolean admin, Nation newNation) {
        super(game);

        this.name = name;
        this.admin = admin;
        if (newNation != null && newNation.getType() != null) {
            this.nationType = newNation.getType();
            this.color = newNation.getColor();
            this.nationID = newNation.getId();
            try {
                featureContainer.add(nationType.getFeatureContainer());
            } catch (Throwable error) {
                error.printStackTrace();
            }
            if (nationType.isEuropean()) {
                /*
                 * Setting the amount of gold to
                 * "getGameOptions().getInteger(GameOptions.STARTING_MONEY)"
                 *
                 * just before starting the game. See
                 * "net.sf.freecol.server.control.PreGameController".
                 */
                gold = 0;
                europe = new Europe(game, this);
                if (!nationType.isREF()) {
                    // colonial nation
                    monarch = new Monarch(game, this, "");
                    playerType = PlayerType.COLONIAL;
                } else {
                    // Royal expeditionary force
                    playerType = PlayerType.ROYAL;
                }
            } else {
                // indians
                gold = 1500;
                playerType = PlayerType.NATIVE;
            }
        } else {
            // virtual "enemy privateer" player
            // or undead ?
            this.nationID = "model.nation.unknownEnemy";
            this.color = noNationColor;
            this.playerType = PlayerType.COLONIAL;
        }
        market = new Market(getGame(), this);
        crosses = 0;
        bells = 0;
        currentFather = null;
    }

    /**
     *
     * Initiates a new <code>Player</code> from an <code>Element</code> and
     * registers this <code>Player</code> at the specified game.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param in The input stream containing the XML.
     * @throws XMLStreamException if a problem was encountered during parsing.
     */
    public Player(Game game, XMLStreamReader in) throws XMLStreamException {
        super(game, in);
        readFromXML(in);
    }

    /**
     * Initiates a new <code>Player</code> from an <code>Element</code> and
     * registers this <code>Player</code> at the specified game.
     *
     * @param game The <code>Game</code> this object belongs to.
     * @param e An XML-element that will be used to initialize this object.
     *
     */
    public Player(Game game, Element e) {
        super(game, e);
        readFromXMLElement(e);
    }

    /**
     * Initiates a new <code>Player</code> with the given ID. The object
     * should later be initialized by calling either {@link
     * #readFromXML(XMLStreamReader)} or {@link #readFromXMLElement(Element)}.
     *
     * @param game The <code>Game</code> in which this object belong.
     * @param id The unique identifier for this object.
     */
    public Player(Game game, String id) {
        super(game, id);
    }

    /**
     * Returns the index of this Player.
     *
     * @return an <code>int</code> value
     */
    public int getIndex() {
        return index;
    }

    /**
     * Get the <code>FeatureContainer</code> value.
     *
     * @return a <code>FeatureContainer</code> value
     */
    public final FeatureContainer getFeatureContainer() {
        return featureContainer;
    }

    /**
     * Set the <code>FeatureContainer</code> value.
     *
     * @param newFeatureContainer The new FeatureContainer value.
     */
    public final void setFeatureContainer(final FeatureContainer newFeatureContainer) {
        this.featureContainer = newFeatureContainer;
    }

    public boolean hasAbility(String id) {
        return featureContainer.hasAbility(id);
    }

    /**
     * Returns this Player's Market.
     *
     * @return This Player's Market.
     */
    public Market getMarket() {
        return market;
    }

    /**
     * Resets this Player's Market.
     */
    public void reinitialiseMarket() {
        market = new Market(getGame(), this);
    }

    /**
     * Checks if this player owns the given <code>Settlement</code>.
     *
     * @param s The <code>Settlement</code>.
     * @return <code>true</code> if this <code>Player</code> owns the given
     *         <code>Settlement</code>.
     */
    public boolean hasSettlement(Settlement s) {
        return settlements.contains(s);
    }

    /**
     * Adds the given <code>Settlement</code> to this <code>Player</code>'s
     * list of settlements.
     *
     * @param s
     */
    public void addSettlement(Settlement s) {
        if (!settlements.contains(s)) {
            settlements.add(s);
            if (s.getOwner() != this) {
                s.setOwner(this);
            }
        }
    }

    /**
     * Removes the given <code>Settlement</code> from this <code>Player</code>'s
     * list of settlements.
     *
     * @param s The <code>Settlement</code> to remove.
     */
    public void removeSettlement(Settlement s) {
        if (settlements.contains(s)) {
            if (s.getOwner() == this) {
                throw new IllegalStateException(
                        "Cannot remove the ownership of the given settlement before it has been given to another player.");
            }
            settlements.remove(s);
        }
    }

    public int getNumberOfSettlements() {
        return numberOfSettlements;
    }

    public void setNumberOfSettlements(int number) {
        numberOfSettlements = number;
    }

    /**
     * Adds a <code>ModelMessage</code> for this player.
     *
     * @param modelMessage The <code>ModelMessage</code>.
     */
    public void addModelMessage(ModelMessage modelMessage) {
        modelMessages.add(modelMessage);
    }

    /**
     * Returns all ModelMessages for this player.
     *
     * @return all ModelMessages for this player.
     */
    public List<ModelMessage> getModelMessages() {
        return modelMessages;
    }

    /**
     * Returns all new ModelMessages for this player.
     *
     * @return all new ModelMessages for this player.
     */
    public List<ModelMessage> getNewModelMessages() {

        ArrayList<ModelMessage> out = new ArrayList<ModelMessage>();

        for (ModelMessage message : modelMessages) {
            if (message.hasBeenDisplayed()) {
                continue;
            } else {
                out.add(message); // preserve message order
            }
        }

        return out;
    }

    /**
     * Removes all undisplayed model messages for this player.
     */
    public void removeModelMessages() {
        Iterator<ModelMessage> messageIterator = modelMessages.iterator();
        while (messageIterator.hasNext()) {
            ModelMessage message = messageIterator.next();
            if (message.hasBeenDisplayed()) {
                messageIterator.remove();
            }
        }
    }

    /**
     * Removes all the model messages for this player.
     */
    public void clearModelMessages() {
        modelMessages.clear();
    }

    /**
     * Checks if this player is a "royal expeditionary force.
     *
     * @return <code>true</code> is the given nation is a royal expeditionary
     *         force and <code>false</code> otherwise.
     */
    public boolean isREF() {
        return nationType != null && nationType.isREF();
    }

    /**
     * Returns the current score of the player.
     *
     * @return an <code>int</code> value
     */
    public int getScore() {
        return score;
    }

    /**
     * Modifies the score of the player by the given value.
     *
     * @param value an <code>int</code> value
     */
    public void modifyScore(int value) {
        score += value;
    }

    /**
     * Get the <code>Unit</code> value.
     *
     * @return a <code>List<Unit></code> value
     */
    public final Unit getUnit(String id) {
        return units.get(id);
    }

    /**
     * Set the <code>Unit</code> value.
     *
     * @param newUnit The new Units value.
     */
    public final void setUnit(final Unit newUnit) {
        if (newUnit != null) {
            units.put(newUnit.getId(), newUnit);
        }
    }

    /**
     * Remove Unit.
     *
     * @param oldUnit an <code>Unit</code> value
     */
    public void removeUnit(final Unit oldUnit) {
        if (oldUnit != null) {
            units.remove(oldUnit.getId());
        }
    }

    /**
     * Gets the total percentage of rebels in all this player's colonies.
     *
     * @return The total percentage of rebels in all this player's colonies.
     */
    public int getSoL() {
        int sum = 0;
        int number = 0;
        for (Colony c : getColonies()) {
            sum += c.getSoL();
            number++;
        }
        if (number > 0) {
            return sum / number;
        } else {
            return 0;
        }
    }

    /**
     * Declares independence.
     */
    public void declareIndependence() {
        if (getSoL() < 50) {
            throw new IllegalStateException("Cannot declare independence. SoL is only: " + getSoL());
        }
        if (playerType != PlayerType.COLONIAL) {
            throw new IllegalStateException("Independence has already been declared.");
        }
        setPlayerType(PlayerType.REBEL);
        featureContainer.addAbility(new Ability("model.ability.independenceDeclared"));

        changeRelationWithPlayer(getREFPlayer(), Stance.WAR);

        setTax(0);
        // Reset all arrears
        for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
            resetArrears(goodsType);
        }

        // Dispose all units in Europe.
        ArrayList<String> unitNames = new ArrayList<String>();
        for (Unit unit : europe.getUnitList()) {
            unitNames.add(unit.getName());
        }
        europe.disposeUnitList();
        if (!unitNames.isEmpty()) {
            addModelMessage(this, ModelMessage.MessageType.UNIT_LOST, "model.player.independence.unitsSeized",
                    "%units%", Utils.join(", ", unitNames));
        }
        for (Colony colony : getColonies()) {
            int sol = colony.getSoL();
            if (sol > 50) {
                List<Unit> veterans = new ArrayList<Unit>();
                for (Unit unit : colony.getTile().getUnitList()) {
                    if (unit.hasAbility("model.ability.expertSoldier")) {
                        veterans.add(unit);
                    }
                }
                int limit = (veterans.size() + 2) * (sol - 50) / 100;
                if (limit > 0) {
                    for (int index = 0; index < limit && limit < veterans.size(); index++) {
                        Unit unit = veterans.get(index);
                        // TODO: use a new upgrade type?
                        unit.setType(unit.getType().getPromotion());
                    }
                    addModelMessage(colony, ModelMessage.MessageType.DEFAULT, "model.player.continentalArmyMuster",
                            "%colony%", colony.getName(), "%number%", String.valueOf(limit));
                }
            }
        }
        // Drop the price{Inc,Dec}reases already prepared but now irrelevant
        divertModelMessages(europe, null);

        europe.dispose();
        europe = null;
        monarch = null;
        modifyScore(SCORE_INDEPENDENCE_DECLARED);
        history.add(new HistoryEvent(getGame().getTurn().getNumber(),
                                     HistoryEvent.Type.DECLARE_INDEPENDENCE));
                                     
    }

    /*
     * Sometimes an event causes the source (and display) fields in an
     * accumulated model message to become invalid (e.g. Europe disappears
     * on independence.  This routine is for cleaning up such cases.
     * @param source the source field that has become invalid
     * @param newSource a new source field to replace the old with, or
     *   if null then remove the message
     */
    public void divertModelMessages(FreeColGameObject source, FreeColGameObject newSource) {
        for (ModelMessage modelMessage : getModelMessages()) {
            if (modelMessage.getSource() == source) {
                if (newSource == null) {
                    modelMessages.remove(modelMessage);
                } else {
                    modelMessage.setSource(newSource);
                    if (modelMessage.getDisplay() == source) {
                        modelMessage.setDisplay(newSource);
                    }
                }
            }
        }
    }        

    /**
     * Gives independence to this <code>Player</code>.
     */
    public void giveIndependence() {
        if (!isEuropean()) {
            throw new IllegalStateException("The player \"" + getName() + "\" is not European.");
        }
        if (playerType != PlayerType.REBEL) {
            throw new IllegalStateException("The player \"" + getName() + "\" is not a Rebel.");
        }
        setPlayerType(PlayerType.INDEPENDENT);
        changeRelationWithPlayer(getREFPlayer(), Stance.PEACE);
        modifyScore(SCORE_INDEPENDENCE_GRANTED - getGame().getTurn().getNumber());
        addModelMessage(this, ModelMessage.MessageType.DEFAULT, "model.player.independence");
        history.add(new HistoryEvent(getGame().getTurn().getNumber(),
                                     HistoryEvent.Type.INDEPENDENCE));
                                     
    }

    /**
     * Gets the <code>Player</code> controlling the "Royal Expeditionary
     * Force" for this player.
     *
     * @return The player, or <code>null</code> if this player does not have a
     *         royal expeditionary force.
     */
    public Player getREFPlayer() {
        return getGame().getPlayer(getNation().getRefId());
    }

    /**
     * Gets the name this player has chosen for the new land.
     *
     * @return The name of the new world as chosen by the <code>Player</code>.
     *         If no land name was chosen, the default name is returned.
     */
    public String getNewLandName() {
        if (newLandName == null) {
            return Messages.message(nationID + ".newLandName");
        } else {
            return newLandName;
        }
    }

    /**
     * Returns true if the player already selected a new name for the discovered
     * land.
     *
     * @return true if the player already set a name for the newly discovered
     *         land, otherwise false.
     */
    public boolean isNewLandNamed() {
        return newLandName != null;
    }

    /**
     * Returns the <code>Colony</code> with the given name.
     *
     * @param name The name of the <code>Colony</code>.
     * @return The <code>Colony</code> or <code>null</code> if this player
     *         does not have a <code>Colony</code> with the specified name.
     */
    public Colony getColony(String name) {
        for (Colony colony : getColonies()) {
            if (colony.getName().equals(name)) {
                return colony;
            }
        }
        return null;
    }

    /**
     * Creates a unique colony name. This is done by fetching a new default
     * colony name from the list of default names.
     *
     * @return A <code>String</code> containing a new unused colony name from
     *         the list, if any is available, and otherwise an automatically
     *         generated name.
     */
    public String getDefaultColonyName() {
        String prefix = nationID + ".newColonyName.";
        String name;

        while (Messages.containsKey(prefix + Integer.toString(colonyNameIndex))) {
            name = Messages.message(prefix + Integer.toString(colonyNameIndex));
            colonyNameIndex++;
            if (getGame().getColony(name) == null) return name;
        }
        do {
            name = Messages.message("Colony") + colonyNameIndex;
            colonyNameIndex++;
        } while (getColony(name) != null);
        return name;
    }

    /**
     * Creates a unique region name by fetching a new default name
     * from the list of default names if possible.
     *
     * @param regionType a <code>RegionType</code> value
     * @return a <code>String</code> value
     */
    public String getDefaultRegionName(RegionType regionType) {
        String prefix = nationID + ".region." + regionType.toString().toLowerCase() + ".";
        String name = null;
        int index = 1;
        Integer newIndex = regionNameIndex.get(regionType);
        if (newIndex != null) {
            index = newIndex.intValue();
        }
        do {
            name = null;
            if (Messages.containsKey(prefix + Integer.toString(index))) {
                name = Messages.message(prefix + Integer.toString(index));
                index++;
            }
        } while (name != null && getGame().getMap().getRegionByName(name) != null);
        if (name == null) {
            do {
                String type = Messages.message("model.region." + regionType.toString().toLowerCase() + ".name");
                name = Messages.message("model.region.default",
                                        "%nation%", getNationAsString(),
                                        "%type%", type,
                                        "%index%", Integer.toString(index));
                index++;
            } while (getGame().getMap().getRegionByName(name) != null);
        }
        regionNameIndex.put(regionType, index);
        return name;
    }

    /**
     * Sets the name this player uses for the new land.
     *
     * @param newLandName This <code>Player</code>'s name for the new world.
     */
    public void setNewLandName(String newLandName) {
        this.newLandName = newLandName;
    }

    /**
     * Checks if this player is european. This includes the "Royal Expeditionay
     * Force".
     *
     * @return <i>true</i> if this player is european and <i>false</i>
     *         otherwise.
     */
    public boolean isEuropean() {
        return nationType != null && nationType.isEuropean();
    }

    /**
     * Checks if this player is indian. This method returns the opposite of
     * {@link #isEuropean()}.
     *
     * @return <i>true</i> if this player is indian and <i>false</i>
     *         otherwise.
     */
    public boolean isIndian() {
        return playerType == PlayerType.NATIVE;
    }

    /**
     * Checks whether this player is at war with any other player.
     *
     * @return <i>true</i> if this player is at war with any other.
     */
    public boolean isAtWar() {
        for (Player player : getGame().getPlayers()) {
            if (getStance(player) == Stance.WAR) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the price of the given land.
     *
     * @param tile The <code>Tile</code> to get the price for.
     * @return The price of the land.
     */
    public int getLandPrice(Tile tile) {
        Player nationOwner = tile.getOwner();
        if (nationOwner == null || nationOwner == this || nationOwner.isEuropean() || tile.getSettlement() != null) {
            return 0;
        }
        int price = 0;
        for (GoodsType type : FreeCol.getSpecification().getGoodsTypeList()) {
            price += tile.potential(type);
        }
        price = price * Specification.getSpecification().getIntegerOption("model.option.landPriceFactor").getValue()
                + 100;
        return (int) featureContainer.applyModifier(price, "model.modifier.landPaymentModifier", null, getGame()
                .getTurn());
    }

    /**
     * Buys the given land.
     *
     * @param tile The <code>Tile</code> to buy.
     */
    public void buyLand(Tile tile) {
        Player owner = tile.getOwner();
        if (owner == null) {
            throw new IllegalStateException("The Tile is not owned by any nation!");
        }
        if (owner == this) {
            throw new IllegalStateException("The Player already owns the Tile.");
        }
        if (owner.isEuropean()) {
            throw new IllegalStateException("The owner is an european player");
        }
        int price = getLandPrice(tile);
        modifyGold(-price);
        owner.modifyGold(price);
        tile.setOwner(this);
        tile.setOwningSettlement(null);
    }

    /**
     * Returns whether this player has met with the <code>Player</code> if the
     * given <code>nation</code>.
     *
     * @param player The Player.
     * @return <code>true</code> if this <code>Player</code> has contacted
     *         the given nation.
     */
    public boolean hasContacted(Player player) {
        if (player == null) {
            return true;
        } else {
            return stance.containsKey(player.getId());
        }
    }

    /**
     * Sets whether this player has contacted the given player.
     *
     * @param player The <code>Player</code>.
     * @param contacted <code>true</code> if this <code>Player</code> has
     *            contacted the given <code>Player</code>.
     */
    public void setContacted(Player player, boolean contacted) {

        if (player == null || player == this || player == getGame().getUnknownEnemy()) {
            return;
        }

        if (contacted && !hasContacted(player)) {
            stance.put(player.getId(), Stance.PEACE);
            history.add(new HistoryEvent(getGame().getTurn().getNumber(),
                                         HistoryEvent.Type.MEET_NATION,
                                         "%nation%", player.getNationAsString()));

            if (isEuropean() && !isAI()) {
                boolean contactedIndians = false;
                boolean contactedEuro = false;
                for (Player player1 : getGame().getPlayers()) {
                    if (hasContacted(player1)) {
                        if (player1.isEuropean()) {
                            contactedEuro = true;
                            if (contactedIndians) {
                                break;
                            }
                        } else {
                            contactedIndians = true;
                            if (contactedEuro) {
                                break;
                            }
                        }
                    }
                }
                // these dialogs should only appear on the first event
                if (player.isEuropean()) {
                    if (!contactedEuro) {
                        addModelMessage(this, ModelMessage.MessageType.FOREIGN_DIPLOMACY, player,
                                "EventPanel.MEETING_EUROPEANS");
                    }
                } else {
                    if (!contactedIndians) {
                        addModelMessage(this, ModelMessage.MessageType.FOREIGN_DIPLOMACY, player,
                                "EventPanel.MEETING_NATIVES");
                    }
                    // special cases for Aztec/Inca
                    if (player.getNationType() == FreeCol.getSpecification().getNationType("model.nationType.aztec")) {
                        addModelMessage(this, ModelMessage.MessageType.FOREIGN_DIPLOMACY, player,
                                "EventPanel.MEETING_AZTEC");
                    } else if (player.getNationType() == FreeCol.getSpecification().getNationType(
                            "model.nationType.inca")) {
                        addModelMessage(this, ModelMessage.MessageType.FOREIGN_DIPLOMACY, player,
                                "EventPanel.MEETING_INCA");
                    }
                }
            } else if (!isEuropean()) {
                tension.put(player, new Tension(0));
            }

        }
        if (!contacted) {
            stance.remove(player.getId());
        }
    }

    /**
     * Returns whether this player has been attacked by privateers.
     *
     * @return <code>true</code> if this <code>Player</code> has been
     *         attacked by privateers.
     */
    public boolean hasBeenAttackedByPrivateers() {
        return attackedByPrivateers;
    }

    /** Sets the variable attackedByPrivateers to true. */
    public void setAttackedByPrivateers() {
        attackedByPrivateers = true;
    }

    /**
     * Gets the default <code>Location</code> where the units arriving from
     * {@link Europe} will be put.
     *
     * @return The <code>Location</code>.
     * @see Unit#getEntryLocation
     */
    public Location getEntryLocation() {
        return entryLocation;
    }

    /**
     * Sets the <code>Location</code> where the units arriving from
     * {@link Europe} will be put as a default.
     *
     * @param entryLocation The <code>Location</code>.
     * @see #getEntryLocation
     */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }

    /**
     * Checks if this <code>Player</code> has explored the given
     * <code>Tile</code>.
     *
     * @param tile The <code>Tile</code>.
     * @return <i>true</i> if the <code>Tile</code> has been explored and
     *         <i>false</i> otherwise.
     */
    public boolean hasExplored(Tile tile) {
        return tile.isExplored();
    }

    /**
     * Sets the given tile to be explored by this player and updates the
     * player's information about the tile.
     *
     * @param tile The <code>Tile</code> to set explored.
     * @see Tile#updatePlayerExploredTile(Player)
     */
    public void setExplored(Tile tile) {
        logger.warning("Implemented by ServerPlayer");
    }

    /**
     * Sets the tiles within the given <code>Unit</code>'s line of sight to
     * be explored by this player.
     *
     * @param unit The <code>Unit</code>.
     * @see #setExplored(Tile)
     * @see #hasExplored
     */
    public void setExplored(Unit unit) {
        if (getGame() == null || getGame().getMap() == null || unit == null || unit.getLocation() == null
                || unit.getTile() == null || isIndian()) {
            return;
        }
        if (canSeeTiles == null) {
            resetCanSeeTiles();
        }
        Iterator<Position> positionIterator = getGame().getMap().getCircleIterator(unit.getTile().getPosition(), true,
                unit.getLineOfSight());
        while (positionIterator.hasNext()) {
            Map.Position p = positionIterator.next();
            canSeeTiles[p.getX()][p.getY()] = true;
        }
    }

    /**
     * Forces an update of the <code>canSeeTiles</code>. This method should
     * be used to invalidate the current <code>canSeeTiles</code>. The method
     * {@link #resetCanSeeTiles} will be called whenever it is needed.
     */
    public void invalidateCanSeeTiles() {
        canSeeTiles = null;
    }

    /**
     * Resets this player's "can see"-tiles. This is done by setting all the
     * tiles within a {@link Unit}s line of sight visible. The other tiles are
     * made unvisible. <br>
     * <br>
     * Use {@link #invalidateCanSeeTiles} whenever possible.
     */
    public void resetCanSeeTiles() {
        Map map = getGame().getMap();
        if (map != null) {
            canSeeTiles = new boolean[map.getWidth()][map.getHeight()];
            if (!getGameOptions().getBoolean(GameOptions.FOG_OF_WAR)) {
                Iterator<Position> positionIterator = getGame().getMap().getWholeMapIterator();
                while (positionIterator.hasNext()) {
                    Map.Position p = positionIterator.next();
                    canSeeTiles[p.getX()][p.getY()] = hasExplored(getGame().getMap().getTile(p));
                }
            } else {
                Iterator<Unit> unitIterator = getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = unitIterator.next();
                    if (!(unit.getLocation() instanceof Tile)) {
                        continue;
                    }
                    Map.Position position = unit.getTile().getPosition();
                    if (position == null) {
                        logger.warning("position == null");
                    }
                    canSeeTiles[position.getX()][position.getY()] = true;
                    /*
                     * if (getGame().getViewOwner() == null &&
                     * !hasExplored(map.getTile(position))) {
                     *
                     * logger.warning("Trying to set a non-explored Tile to be
                     * visible (1). Unit: " + unit.getName() + ", Tile: " +
                     * position);
                     *
                     * throw new IllegalStateException("Trying to set a
                     * non-explored Tile to be visible. Unit: " + unit.getName() + ",
                     * Tile: " + position); }
                     */
                    Iterator<Position> positionIterator = map.getCircleIterator(position, true, unit.getLineOfSight());
                    while (positionIterator.hasNext()) {
                        Map.Position p = positionIterator.next();
                        canSeeTiles[p.getX()][p.getY()] = true;
                        /*
                         * if (getGame().getViewOwner() == null &&
                         * !hasExplored(map.getTile(p))) {
                         *
                         * logger.warning("Trying to set a non-explored Tile to
                         * be visible (2). Unit: " + unit.getName() + ", Tile: " +
                         * p);
                         *
                         * throw new IllegalStateException("Trying to set a
                         * non-explored Tile to be visible. Unit: " +
                         * unit.getName() + ", Tile: " + p); }
                         */
                    }
                }
                for (Settlement settlement : getSettlements()) {
                    Map.Position position = settlement.getTile().getPosition();
                    canSeeTiles[position.getX()][position.getY()] = true;
                    /*
                     * if (getGame().getViewOwner() == null &&
                     * !hasExplored(map.getTile(position))) {
                     *
                     * logger.warning("Trying to set a non-explored Tile to be
                     * visible (3). Settlement: " + settlement + "(" +
                     * settlement.getTile().getPosition() + "), Tile: " +
                     * position);
                     *
                     * throw new IllegalStateException("Trying to set a
                     * non-explored Tile to be visible. Settlement: " +
                     * settlement + "(" + settlement.getTile().getPosition() +
                     * "), Tile: " + position); }
                     */
                    Iterator<Position> positionIterator = map.getCircleIterator(position, true, settlement
                            .getLineOfSight());
                    while (positionIterator.hasNext()) {
                        Map.Position p = positionIterator.next();
                        canSeeTiles[p.getX()][p.getY()] = true;
                        /*
                         * if (getGame().getViewOwner() == null &&
                         * !hasExplored(map.getTile(p))) {
                         *
                         * logger.warning("Trying to set a non-explored Tile to
                         * be visible (4). Settlement: " + settlement + "(" +
                         * settlement.getTile().getPosition() + "), Tile: " +
                         * p);
                         *
                         * throw new IllegalStateException("Trying to set a
                         * non-explored Tile to be visible. Settlement: " +
                         * settlement + "(" + settlement.getTile().getPosition() +
                         * "), Tile: " + p); }
                         */
                    }
                }
            }
        }
    }

    /**
     * Checks if this <code>Player</code> can see the given <code>Tile</code>.
     * The <code>Tile</code> can be seen if it is in a {@link Unit}'s line of
     * sight.
     *
     * @param tile The given <code>Tile</code>.
     * @return <i>true</i> if the <code>Player</code> can see the given
     *         <code>Tile</code> and <i>false</i> otherwise.
     */
    public boolean canSee(Tile tile) {
        if (tile == null) {
            return false;
        }
        if (canSeeTiles == null) {
            resetCanSeeTiles();
            if (canSeeTiles == null) {
                return false;
            }
        }
        return canSeeTiles[tile.getX()][tile.getY()];
    }

    /**
     * Returns the type of this player.
     *
     * @return The player type.
     */
    public PlayerType getPlayerType() {
        return playerType;
    }

    /**
     * Checks if this <code>Player</code> can build colonies.
     *
     * @return <code>true</code> if this player is european, not the royal
     *         expeditionary force and not currently fighting the war of
     *         independence.
     */
    public boolean canBuildColonies() {
        return nationType.hasAbility("model.ability.foundColony");
    }

    /**
     * Checks if this <code>Player</code> can get founding fathers.
     *
     * @return <code>true</code> if this player is european, not the royal
     *         expeditionary force and not currently fighting the war of
     *         independence.
     */
    public boolean canHaveFoundingFathers() {
        return nationType.hasAbility("model.ability.electFoundingFather");
    }

    /**
     * Sets the player type.
     *
     * @param type The new player type.
     * @see #getPlayerType
     */
    public void setPlayerType(PlayerType type) {
        playerType = type;
    }

    /**
     * Determines whether this player has a certain Founding father.
     *
     * @param someFather a <code>FoundingFather</code> value
     * @return Whether this player has this Founding father
     * @see FoundingFather
     */
    public boolean hasFather(FoundingFather someFather) {
        return allFathers.contains(someFather);
    }

    /**
     * Returns the number of founding fathers in this players congress. Used to
     * calculate number of bells needed to recruit new fathers.
     *
     * @return The number of founding fathers in this players congress
     */
    public int getFatherCount() {
        return allFathers.size();
    }

    /**
     * Sets this players liberty bell production to work towards recruiting
     * <code>father</code> to its congress.
     *
     * @param someFather a <code>FoundingFather</code> value
     * @see FoundingFather
     */
    public void setCurrentFather(FoundingFather someFather) {
        currentFather = someFather;
    }

    /**
     * Gets the {@link FoundingFather founding father} this player is working
     * towards.
     *
     * @return The current FoundingFather or null if there is none
     * @see #setCurrentFather
     * @see FoundingFather
     */
    public FoundingFather getCurrentFather() {
        return currentFather;
    }

    /**
     * Gets called when this player's turn has ended.
     */
    public void endTurn() {
        removeModelMessages();
        resetCanSeeTiles();
    }

    /**
     * Checks if this <code>Player</code> can move units to
     * <code>Europe</code>.
     *
     * @return <code>true</code> if this <code>Player</code> has an instance
     *         of <code>Europe</code>.
     */
    public boolean canMoveToEurope() {
        return getEurope() != null;
    }

    /**
     * Returns the europe object that this player has.
     *
     * @return The europe object that this player has or <code>null</code> if
     *         this <code>Player</code> does not have an instance
     *         <code>Europe</code>.
     */
    public Europe getEurope() {
        return europe;
    }

    /**
     * Describe <code>getEuropeName</code> method here.
     *
     * @return a <code>String</code> value
     */
    public String getEuropeName() {
        if (europe == null) {
            return null;
        } else {
            return Messages.message(nationID + ".europe");
        }
    }

    /**
     * Returns the monarch object this player has.
     *
     * @return The monarch object this player has or <code>null</code> if this
     *         <code>Player</code> does not have an instance
     *         <code>Monarch</code>.
     */
    public Monarch getMonarch() {
        return monarch;
    }

    /**
     * Sets the monarch object this player has.
     *
     * @param monarch The monarch object this player should have.
     */
    public void setMonarch(Monarch monarch) {
        this.monarch = monarch;
    }

    /**
     * Returns the amount of gold that this player has.
     *
     * @return The amount of gold that this player has or <code>-1</code> if
     *         the amount of gold is unknown.
     */
    public int getGold() {
        return gold;
    }

    /**
     * Modifies the amount of gold that this player has. The argument can be
     * both positive and negative.
     *
     * @param amount The amount of gold that should be added to this player's
     *            gold amount (can be negative!).
     * @exception IllegalArgumentException if the player gets a negative amount
     *                of gold after adding <code>amount</code>.
     */
    public void modifyGold(int amount) {
        if (this.gold == -1) {
            return;
        }
        if ((gold + amount) >= 0) {
            modifyScore((gold + amount) / 1000 - gold / 1000);
            gold += amount;
        } else {
            // This can happen if the server and the client get out of synch.
            // Perhaps it can also happen if the client tries to adjust gold
            // for another player, where the balance is unknown. Just keep
            // going and do the best thing possible, we don't want to crash
            // the game here.
            logger.warning("Cannot add " + amount + " gold for " + this + ": would be negative!");
            gold = 0;
        }
    }

    /**
     * Determines whether this player is an AI player.
     *
     * @return Whether this player is an AI player.
     */
    public boolean isAI() {
        return ai;
    }

    /**
     * Sets whether this player is an AI player.
     *
     * @param ai <code>true</code> if this <code>Player</code> is controlled
     *            by the computer.
     */
    public void setAI(boolean ai) {
        this.ai = ai;
    }

    /**
     * Gets a new active unit.
     *
     * @return A <code>Unit</code> that can be made active.
     */
    public Unit getNextActiveUnit() {
        return nextActiveUnitIterator.next();
    }

    /**
     * Gets a new going_to unit.
     *
     * @return A <code>Unit</code> that can be made active.
     */
    public Unit getNextGoingToUnit() {
        return nextGoingToUnitIterator.next();
    }

    /**
     * Checks if a new active unit can be made active.
     *
     * @return <i>true</i> if this is the case and <i>false</i> otherwise.
     */
    public boolean hasNextActiveUnit() {
        return nextActiveUnitIterator.hasNext();
    }

    /**
     * Checks if a new active unit can be made active.
     *
     * @return <i>true</i> if this is the case and <i>false</i> otherwise.
     */
    public boolean hasNextGoingToUnit() {
        return nextGoingToUnitIterator.hasNext();
    }

    /**
     * Checks if this player is an admin.
     *
     * @return <i>true</i> if the player is an admin and <i>false</i>
     *         otherwise.
     */
    public boolean isAdmin() {
        return admin;
    }

    /**
     * Checks if this player is dead. A <code>Player</code> dies when it
     * looses the game.
     *
     * @return <code>true</code> if this <code>Player</code> is dead.
     */
    public boolean isDead() {
        return dead;
    }

    /**
     * Sets this player to be dead or not.
     *
     * @param dead Should be set to <code>true</code> when this
     *            <code>Player</code> dies.
     * @see #isDead
     */
    public void setDead(boolean dead) {
        this.dead = dead;
    }

    /**
     * Returns the name of this player.
     *
     * @return The name of this player.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the name of this player.
     *
     * @return The name of this player.
     */
    public String toString() {
        return getName();
    }

    /**
     * Set the <code>Name</code> value.
     *
     * @param newName The new Name value.
     */
    public void setName(String newName) {
        this.name = newName;
    }

    /**
     * Returns the nation type of this player.
     *
     * @return The nation type of this player.
     */
    public NationType getNationType() {
        return nationType;
    }

    /**
     * Sets the nation type of this player.
     *
     * @param newNationType a <code>NationType</code> value
     */
    public void setNationType(NationType newNationType) {
        if (nationType != null) {
            featureContainer.remove(nationType.getFeatureContainer());
        }
        nationType = newNationType;
        featureContainer.add(newNationType.getFeatureContainer());
    }

    /**
     * Return this Player's nation.
     *
     * @return a <code>String</code> value
     */
    public Nation getNation() {
        return FreeCol.getSpecification().getNation(nationID);
    }

    /**
     * Sets the nation for this player.
     *
     * @param newNation The new nation for this player.
     */
    public void setNation(Nation newNation) {
        final String oldNationID = nationID;
        nationID = newNation.getId();
        firePropertyChange("nationID", oldNationID, nationID);
    }

    /**
     * Return the ID of this Player's nation.
     *
     * @return a <code>String</code> value
     */
    public String getNationID() {
        return nationID;
    }

    /**
     * Returns the nation of this player as a String.
     *
     * @return The nation of this player as a String.
     */
    public String getNationAsString() {
        return Messages.message(nationID + ".name");
    }

    /**
     * Get the <code>RulerName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getRulerName() {
        return Messages.message(nationID + ".ruler");
    }

    /**
     * Returns the color of this player.
     *
     * @return The color of this player.
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color for this player.
     *
     * @param c The new color for this player.
     */
    public void setColor(Color c) {
        color = c;
    }

    /**
     * Checks if this <code>Player</code> is ready to start the game.
     *
     * @return <code>true</code> if this <code>Player</code> is ready to
     *         start the game.
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Sets this <code>Player</code> to be ready/not ready for starting the
     * game.
     *
     * @param ready This indicates if the player is ready to start the game.
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * Gets an <code>Iterator</code> containing all the units this player
     * owns.
     *
     * @return The <code>Iterator</code>.
     * @see Unit
     */
    public Iterator<Unit> getUnitIterator() {
        return units.values().iterator();
    }

    public List<Unit> getUnits() {
        return new ArrayList<Unit>(units.values());
    }

    /**
     * Returns a list of all Settlements this player owns.
     *
     * @return The settlements this player owns.
     */
    public List<Settlement> getSettlements() {
        return settlements;
    }

    /**
     * Returns a list of all Colonies this player owns.
     *
     * @return The colonies this player owns.
     */
    public List<Colony> getColonies() {
        ArrayList<Colony> colonies = new ArrayList<Colony>();
        for (Settlement s : settlements) {
            if (s instanceof Colony) {
                colonies.add((Colony) s);
            } else {
                throw new RuntimeException("getColonies can only be called for players whose settlements are colonies.");
            }
        }
        return colonies;
    }

    /**
     * Returns a list of all IndianSettlements this player owns.
     *
     * @return The indian settlements this player owns.
     */
    public List<IndianSettlement> getIndianSettlements() {
        ArrayList<IndianSettlement> indianSettlements = new ArrayList<IndianSettlement>();
        for (Settlement s : settlements) {
            if (s instanceof IndianSettlement) {
                indianSettlements.add((IndianSettlement) s);
            } else {
                throw new RuntimeException(
                        "getIndianSettlements can only be called for players whose settlements are IndianSettlements.");
            }
        }
        return indianSettlements;
    }

    /**
     * Returns the closest <code>Location</code> in which the given ship can
     * get repaired. This is the closest {@link Colony} with a drydock, or
     * {@link Europe} if this player has no colonies with a drydock.
     *
     * @param unit The ship that needs a location to be repaired.
     * @return The closest <code>Location</code> in which the ship can be
     *         repaired.
     * @exception IllegalArgumentException if the <code>unit</code> is not a
     *                ship.
     */
    public Location getRepairLocation(Unit unit) {
        if (!unit.isNaval()) {
            throw new IllegalArgumentException();
        }
        Location closestLocation = null;
        int shortestDistance = Integer.MAX_VALUE;
        for (Colony colony : getColonies()) {
            if (colony == null || colony.getTile() == unit.getTile()) {
                // This happens when is called from damageAllShips because
                // the colony is being captured and can't be repaired in that
                // colony
                continue;
            }
            int distance;
            if (colony.hasAbility("model.ability.repairUnits")
                    && (distance = unit.getTile().getDistanceTo(colony.getTile())) < shortestDistance) {
                closestLocation = colony;
                shortestDistance = distance;
            }
        }
        if (closestLocation != null) {
            return closestLocation;
        }
        return getEurope();
    }

    /**
     * Describe <code>increment</code> method here.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param amount an <code>int</code> value
     */
    // TODO: make this more general
    public void increment(GoodsType goodsType, int amount) {
        if (canRecruitUnits()) {
            if (goodsType == Goods.CROSSES) {
                crosses += amount;
            } else if (goodsType == Goods.BELLS) {
                bells += amount;
            }
        }
    }

    /**
     * Sets the number of crosses this player possess.
     *
     * @see #incrementCrosses(int)
     */
    public void reduceCrosses() {
        if (!canRecruitUnits()) {
            return;
        }

        int cost = getGameOptions().getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW)
            ? crossesRequired : crosses;

        if (cost > crosses) {
            crosses = 0;
        } else {
            crosses -= cost;
        }
    }

    /**
     * Gets the number of crosses this player possess.
     *
     * @return The number.
     * @see #reduceCrosses
     */
    public int getCrosses() {
        if (!canRecruitUnits()) {
            return 0;
        }
        return crosses;
    }

    /**
     * Get the <code>TradeRoutes</code> value.
     *
     * @return a <code>List<TradeRoute></code> value
     */
    public final List<TradeRoute> getTradeRoutes() {
        return tradeRoutes;
    }

    /**
     *
     * Set the <code>TradeRoutes</code> value.
     *
     *
     *
     * @param newTradeRoutes The new TradeRoutes value.
     *
     */
    public final void setTradeRoutes(final List<TradeRoute> newTradeRoutes) {
        this.tradeRoutes = newTradeRoutes;
    }

    /**
     * Checks to see whether or not a colonist can emigrate, and does so if
     * possible.
     *
     * @return Whether a new colonist should immigrate.
     */
    public boolean checkEmigrate() {
        if (!canRecruitUnits()) {
            return false;
        }
        return getCrossesRequired() <= crosses;
    }

    /**
     * Gets the number of crosses required to cause a new colonist to emigrate.
     *
     * @return The number of crosses required to cause a new colonist to
     *         emigrate.
     */
    public int getCrossesRequired() {
        if (!canRecruitUnits()) {
            return 0;
        }
        return crossesRequired;
    }

    /**
     * Sets the number of crosses required to cause a new colonist to emigrate.
     *
     * @param crossesRequired The number of crosses required to cause a new
     *            colonist to emigrate.
     */
    public void setCrossesRequired(int crossesRequired) {
        if (!canRecruitUnits()) {
            return;
        }
        this.crossesRequired = crossesRequired;
    }

    /**
     * Checks if this <code>Player</code> can recruit units by producing
     * crosses.
     *
     * @return <code>true</code> if units can be recruited by this
     *         <code>Player</code>.
     */
    public boolean canRecruitUnits() {
        return playerType == PlayerType.COLONIAL;
    }

    /**
     * Updates the amount of crosses needed to emigrate a <code>Unit</code>
     * from <code>Europe</code>.
     */
    public void updateCrossesRequired() {
        if (!canRecruitUnits()) {
            return;
        }
        crossesRequired += (int) featureContainer.applyModifier(Specification.getSpecification().getIntegerOption(
                "model.option.crossesIncrement").getValue(), "model.modifier.religiousUnrestBonus");
        // The book I have tells me the crosses needed is:
        // [(colonist count in colonies + total colonist count) * 2] + 8.
        // So every unit counts as 2 unless they're in a colony,
        // wherein they count as 4.
        /*
         * int count = 8; Map map = getGame().getMap(); Iterator<Position>
         * tileIterator = map.getWholeMapIterator(); while
         * (tileIterator.hasNext()) { Tile t = map.getTile(tileIterator.next());
         * if (t != null && t.getFirstUnit() != null &&
         * t.getFirstUnit().getOwner().equals(this)) { Iterator<Unit>
         * unitIterator = t.getUnitIterator(); while (unitIterator.hasNext()) {
         * Unit u = unitIterator.next(); Iterator<Unit> childUnitIterator =
         * u.getUnitIterator(); while (childUnitIterator.hasNext()) { // Unit
         * childUnit = (Unit) childUnitIterator.next();
         * childUnitIterator.next(); count += 2; } count += 2; } } if (t != null &&
         * t.getColony() != null && t.getColony().getOwner() == this) { count +=
         * t.getColony().getUnitCount() * 4; // Units in colonies // count
         * doubly. // -sjm } } Iterator<Unit> europeUnitIterator =
         * getEurope().getUnitIterator(); while (europeUnitIterator.hasNext()) {
         * europeUnitIterator.next(); count += 2; } if (nation == ENGLISH) {
         * count = (count * 2) / 3; } setCrossesRequired(count);
         */
    }

    /**
     * Modifies the hostiliy against the given player.
     *
     * @param player The <code>Player</code>.
     * @param addToTension The amount to add to the current tension level.
     */
    public void modifyTension(Player player, int addToTension) {
        modifyTension(player, addToTension, null);
    }

    public void modifyTension(Player player, int addToTension, IndianSettlement origin) {
        if (player == this || player == null) {
            return;
        }
        if (tension.get(player) == null) {
            tension.put(player, new Tension(addToTension));
        } else {
            tension.get(player).modify(addToTension);
        }

        if (origin != null && isIndian() && origin.getOwner() == this) {
            for (Settlement settlement : settlements) {
                if (settlement instanceof IndianSettlement && !origin.equals(settlement)) {
                    ((IndianSettlement) settlement).propagatedAlarm(player, addToTension);
                }
            }
        }
    }

    /**
     * Sets the hostility against the given player.
     *
     * @param player The <code>Player</code>.
     * @param newTension The <code>Tension</code>.
     */
    public void setTension(Player player, Tension newTension) {
        if (player == this || player == null) {
            return;
        }
        tension.put(player, newTension);
    }

    /**
     * Gets the hostility this player has against the given player.
     *
     * @param player The <code>Player</code>.
     * @return An object representing the tension level.
     */
    public Tension getTension(Player player) {
        if (player == null) {
            return new Tension();
        } else {
            return tension.get(player);
        }
    }
    
    /**
     * Indian player surrenders
     * @param player The <code>Player</code> he surrenders to.
     * 
     */
    public void surrenderTo(Player player) {
    	if(!isIndian()){
    		logger.warning("Only indians should surrender");
    		return;
    	}
    	changeRelationWithPlayer(player, Stance.PEACE);
    	getTension(player).setValue(Tension.SURRENDED);
    }

    /**
     * Get the <code>History</code> value.
     *
     * @return a <code>List<HistoryEvent></code> value
     */
    public final List<HistoryEvent> getHistory() {
        return history;
    }

    /**
     * Set the <code>History</code> value.
     *
     * @param newHistory The new History value.
     */
    public final void setHistory(final List<HistoryEvent> newHistory) {
        this.history = newHistory;
    }

    private static int getNearbyColonyBonus(Player owner, Tile tile) {
        Game game = tile.getGame();
        Map map = game.getMap();
        Iterator<Position> it = map.getCircleIterator(tile.getPosition(), false, 3);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 45;
            }
        }
        it = map.getCircleIterator(tile.getPosition(), false, 4);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 25;
            }
        }
        it = map.getCircleIterator(tile.getPosition(), false, 5);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 20;
            }
        }
        it = map.getCircleIterator(tile.getPosition(), false, 6);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 30;
            }
        }
        it = map.getCircleIterator(tile.getPosition(), false, 7);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 15;
            }
        }
        it = map.getCircleIterator(tile.getPosition(), false, 8);
        while (it.hasNext()) {
            Tile ct = map.getTile(it.next());
            if (ct.getColony() != null && ct.getColony().getOwner() == owner) {
                return 5;
            }
        }
        return 0;
    }

    /**
     * Gets the value of building a <code>Colony</code> on the given tile.
     * This method adds bonuses to the colony value if the tile is close to (but
     * not overlapping with) another friendly colony. Penalties for enemy
     * units/colonies are added as well.
     *
     * @param tile The <code>Tile</code>
     * @return The value of building a colony on the given tile.
     * @see Tile#getColonyValue()
     */
    public int getColonyValue(Tile tile) {
        int value = tile.getColonyValue();
        if (value == 0) {
            return 0;
        } else {
            Iterator<Position> it = getGame().getMap().getCircleIterator(tile.getPosition(), true, 4);
            while (it.hasNext()) {
                Tile ct = getGame().getMap().getTile(it.next());
                if (ct.getColony() != null && ct.getColony().getOwner() != this) {
                    if (getStance(ct.getColony().getOwner()) == Stance.WAR) {
                        value -= Math.max(0, 20 - tile.getDistanceTo(tile) * 4);
                    } else {
                        value -= Math.max(0, 8 - tile.getDistanceTo(tile) * 2);
                    }
                }
                Iterator<Unit> ui = ct.getUnitIterator();
                while (ui.hasNext()) {
                    Unit u = ui.next();
                    if (u.getOwner() != this && u.isOffensiveUnit() && u.getOwner().isEuropean()
                            && getStance(u.getOwner()) == Stance.WAR) {
                        value -= Math.max(0, 40 - tile.getDistanceTo(tile) * 9);
                    }
                }
            }
            return Math.max(0, value + getNearbyColonyBonus(this, tile));
        }
    }

    /**
     * Returns the stance towards a given player. <BR>
     * <BR>
     * One of: WAR, CEASE_FIRE, PEACE and ALLIANCE.
     *
     * @param player The <code>Player</code>.
     * @return The stance.
     */
    public Stance getStance(Player player) {
        if (player == null) {
            return Stance.PEACE;
        } else {
            return stance.get(player.getId());
        }
    }

    /**
     * Returns a string describing the given stance.
     *
     * @param stance The stance.
     * @return A matching string.
     */
    public static String getStanceAsString(Stance stance) {
        return Messages.message("model.stance." + stance.toString().toLowerCase());
    }

    /**
     * Sets the stance towards a given player. <BR>
     * <BR>
     * One of: WAR, CEASE_FIRE, PEACE and ALLIANCE.
     * This only sets this player stance,
     *<code>changeRelationsWithPlayer</code> should be used to set both players
     *
     * @param player The <code>Player</code>.
     * @param newStance The stance.
     */
    public void setStance(Player player, Stance newStance) {
        if (player == null) {
            throw new IllegalArgumentException("Player must not be 'null'.");
        }
        if (player == this) {
            throw new IllegalArgumentException("Cannot set the stance towards ourselves.");
        }
        Stance oldStance = stance.get(player.getId());
        if (newStance.equals(oldStance)) {
            return;
        }
        if (newStance == Stance.CEASE_FIRE && oldStance != Stance.WAR) {
        	throw new IllegalStateException("Cease fire can only be declared when at war.");
        }
        stance.put(player.getId(), newStance);
    }

    public void changeRelationWithPlayer(Player player,Stance newStance){
    	Stance oldStance = getStance(player);
    	
    	// Sanitation
    	if(newStance == oldStance){
    		return;
    	}
    	
    	// Set stance
    	setStance(player, newStance);
    
    	// For now on, consider null as PEACE
    	// This may happen with the REF
    	if(oldStance == null){
    	    oldStance = Stance.PEACE;
    	}
    	
    	// Update tension
    	int modifier = 0;
    	switch(newStance){
    		case PEACE:
    			if(oldStance == Stance.WAR){
    				modifier = Tension.CEASE_FIRE_MODIFIER + Tension.PEACE_TREATY_MODIFIER;
    			}
    			if(oldStance == Stance.CEASE_FIRE){
    				modifier = Tension.PEACE_TREATY_MODIFIER;
    			}
    			break;
    		case CEASE_FIRE:
    			if(oldStance == Stance.WAR){
    				modifier = Tension.CEASE_FIRE_MODIFIER;
    			}
    	}
    	modifyTension(player,modifier);
    	
        if (player.getStance(this) != newStance) {
            getGame().getModelController().setStance(this, player, newStance);
            player.setStance(this, newStance);
        
            if(newStance == Stance.WAR){
            	switch(oldStance){
            		case PEACE:
            			modifier = Tension.TENSION_ADD_DECLARE_WAR_FROM_PEACE;
            			break;
            		case CEASE_FIRE:
            			modifier = Tension.TENSION_ADD_DECLARE_WAR_FROM_CEASE_FIRE;
            	}
            }
            player.modifyTension(this, modifier);
        }
    }

    /**
     * Gets the price for a recruit in europe.
     *
     * @return The price of a single recruit in {@link Europe}.
     */
    public int getRecruitPrice() {
        // return Math.max(0, (getCrossesRequired() - crosses) * 10);
        return getEurope().getRecruitPrice();
    }

    /**
     * Gets the current amount of bells this <code>Player</code> has.
     *
     * @return This player's number of bells earned towards the current Founding
     *         Father.
     * @see Goods#BELLS
     * @see #incrementBells
     */
    public int getBells() {
        if (!canHaveFoundingFathers()) {
            return 0;
        }
        return bells;
    }

    /**
     * Adds a founding father to this player's continental congress.
     *
     * @param father a <code>FoundingFather</code> value
     * @see FoundingFather
     */
    public void addFather(FoundingFather father) {

        allFathers.add(father);

        addModelMessage(this, ModelMessage.MessageType.DEFAULT,
                        "model.player.foundingFatherJoinedCongress",
                        "%foundingFather%", father.getName(),
                        "%description%", father.getDescription());
        history.add(new HistoryEvent(getGame().getTurn().getNumber(),
                                     HistoryEvent.Type.FOUNDING_FATHER,
                                     "%father%", father.getName()));
        featureContainer.add(father.getFeatureContainer());

        List<AbstractUnit> units = father.getUnits();
        if (units != null) {
            // TODO: make use of armed, mounted, etc.
            for (int index = 0; index < units.size(); index++) {
                AbstractUnit unit = units.get(index);
                String uniqueID = getId() + "newTurn" + father.getId() + index;
                getGame().getModelController().createUnit(uniqueID, getEurope(), this, unit.getUnitType());
            }
        }

        java.util.Map<UnitType, UnitType> upgrades = father.getUpgrades();
        if (upgrades != null) {
            Iterator<Unit> unitIterator = getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit unit = unitIterator.next();
                UnitType newType = upgrades.get(unit.getType());
                if (newType != null) {
                    unit.setType(newType);
                }
            }
        }

        for (Ability ability : father.getFeatureContainer().getAbilities()) {
            if ("model.ability.addTaxToBells".equals(ability.getId())) {
                updateAddTaxToBells();
            }
        }

        for (String event : father.getEvents().keySet()) {
            if (event.equals("model.event.resetNativeAlarm")) {
                // reduce indian tension and alarm
                for (Player player : getGame().getPlayers()) {
                    if (!player.isEuropean() && player.getTension(this) != null) {
                        player.getTension(this).setValue(0);
                        for (IndianSettlement is : player.getIndianSettlements()) {
                            if (is.getAlarm(this) != null) {
                                is.getAlarm(this).setValue(0);
                            }
                        }
                    }
                }
            } else if (event.equals("model.event.boycottsLifted")) {
                for (GoodsType goodsType : FreeCol.getSpecification().getGoodsTypeList()) {
                    resetArrears(goodsType);
                }
            } else if (event.equals("model.event.freeBuilding")) {
                BuildingType type = FreeCol.getSpecification().getBuildingType(father.getEvents().get(event));
                for (Colony colony : getColonies()) {
                    if (colony.canBuild(type)) {
                        // Need to use a `special' taskID to avoid collision
                        // with other building tasks that complete this turn.
                        String taskIDplus = colony.getId() + "buildBuilding" + father.getId();
                        Building building = getGame().getModelController().createBuilding(taskIDplus, colony, type);
                        colony.addBuilding(building);
                    }
                }
            } else if (event.equals("model.event.seeAllColonies")) {
                exploreAllColonies();
            } else if (event.equals("model.event.increaseSonsOfLiberty")) {
                int value = Integer.parseInt(father.getEvents().get(event));
                for (Colony colony : getColonies()) {
                    /*
                     * The number of bells to be generated in order to get the
                     * appropriate SoL is determined by the formula: int
                     * membership = ... in "colony.updateSoL()":
                     */
                    int requiredBells = ((colony.getSoL() + value) * Colony.BELLS_PER_REBEL * colony.getUnitCount()) / 100;
                    colony.addGoods(Goods.BELLS, requiredBells - colony.getGoodsCount(Goods.BELLS));
                }
            } else if (event.equals("model.event.newRecruits")) {
                for (int index = 0; index < 3; index++) {
                    UnitType recruitable = getEurope().getRecruitable(index);
                    if (featureContainer.hasAbility("model.ability.canNotRecruitUnit", recruitable)) {
                        getEurope().setRecruitable(index, generateRecruitable("newRecruits" + index));
                    }
                }
            }
        }
    }

    /**
     * Prepares this <code>Player</code> for a new turn.
     */
    public void newTurn() {

        int newSoL = 0;

        // reducing tension levels if nation is native
        if (isIndian()) {
            for (Tension tension1 : tension.values()) {
                if (tension1.getValue() > 0) {
                    tension1.modify(-(4 + tension1.getValue() / 100));
                }
            }
        }

        // settlements
        ArrayList<Settlement> settlements = new ArrayList<Settlement>(getSettlements());
        for (Settlement settlement : settlements) {
            logger.finest("Calling newTurn for settlement " + settlement.toString());
            settlement.newTurn();
            if (isEuropean()) {
                Colony colony = (Colony) settlement;
                newSoL += colony.getSoL();
            }
        }

        /*
         * Moved founding fathers infront of units so that naval units will get
         * their Magellan bonus the turn Magellan joins the congress.
         */
        if (isEuropean()) {
            if (!hasAbility("model.ability.independenceDeclared") &&
                getBells() >= getTotalFoundingFatherCost() &&
                currentFather != null) {
                addFather(currentFather);
                currentFather = null;
                bells -= getGameOptions().getBoolean(GameOptions.SAVE_PRODUCTION_OVERFLOW) ?
                    getTotalFoundingFatherCost() : bells;
            }

            // CO: since the pioneer already finishes faster, changing
            // it at both locations would double the bonus.
            /**
             * Create new collection to avoid to concurrent modification.
             */
            for (Unit unit : new ArrayList<Unit>(units.values())) {
                logger.finest("Calling newTurn for unit " + unit.getName() + " " + unit.getId());
                unit.newTurn();
            }

            if (getEurope() != null) {
                logger.finest("Calling newTurn for player " + getName() + "'s Europe");
                getEurope().newTurn();
            }

            if (getMarket() != null) {
                logger.finest("Calling newTurn for player " + getName() + "'s Market");
                getMarket().newTurn();
            }

            int numberOfColonies = settlements.size();
            if (numberOfColonies > 0) {
                newSoL = newSoL / numberOfColonies;
                if (oldSoL / 10 != newSoL / 10) {
                    if (newSoL > oldSoL) {
                        addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY, "model.player.SoLIncrease",
                                "%oldSoL%", String.valueOf(oldSoL), "%newSoL%", String.valueOf(newSoL));
                    } else {
                        addModelMessage(this, ModelMessage.MessageType.SONS_OF_LIBERTY, "model.player.SoLDecrease",
                                "%oldSoL%", String.valueOf(oldSoL), "%newSoL%", String.valueOf(newSoL));
                    }
                }
            }
            // remember SoL for check changes at next turn
            oldSoL = newSoL;
        } else {
            for (Iterator<Unit> unitIterator = getUnitIterator(); unitIterator.hasNext();) {
                Unit unit = unitIterator.next();
                if (logger.isLoggable(Level.FINEST)) {
                    logger.finest("Calling newTurn for unit " + unit.getName() + " " + unit.getId());
                }
                unit.newTurn();
            }
        }
    }

    private void exploreAllColonies() {
        // explore all tiles surrounding colonies
        ArrayList<Tile> tiles = new ArrayList<Tile>();
        Iterator<Position> tileIterator = getGame().getMap().getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile tile = getGame().getMap().getTile((tileIterator.next()));
            if (tile.getColony() != null) {
                tiles.add(tile);
                for (Direction direction : Direction.values()) {
                    Tile addTile = getGame().getMap().getNeighbourOrNull(direction, tile);
                    if (addTile != null) {
                        tiles.add(addTile);
                    }
                }
            }
        }
        getGame().getModelController().exploreTiles(this, tiles);
    }

    /**
     * Generates a random unit type. The unit type that is returned represents
     * the type of a unit that is recruitable in Europe.
     *
     * @param unique a <code>String</code> value
     * @return A random unit type of a unit that is recruitable in Europe.
     */
    public UnitType generateRecruitable(String unique) {
        ArrayList<RandomChoice<UnitType>> recruitableUnits = new ArrayList<RandomChoice<UnitType>>();
        for (UnitType unitType : FreeCol.getSpecification().getUnitTypeList()) {
            if (unitType.isRecruitable() &&
                !featureContainer.hasAbility("model.ability.canNotRecruitUnit", unitType)) {
                recruitableUnits.add(new RandomChoice<UnitType>(unitType, unitType.getRecruitProbability()));
            }
        }
        int totalProbability = RandomChoice.getTotalProbability(recruitableUnits);
        int random = getGame().getModelController().getRandom(getId() + "newRecruitableUnit" + unique,
                                                              totalProbability);
        return RandomChoice.select(recruitableUnits, random);
    }

    /**
     * Gets the number of bells needed for recruiting the next founding father.
     *
     * @return How many more bells the <code>Player</code> needs in order to
     *         recruit the next founding father.
     * @see Goods#BELLS
     * @see #incrementBells
     */
    public int getRemainingFoundingFatherCost() {
        return getTotalFoundingFatherCost() - getBells();
    }

    /**
     * Returns how many bells in total are needed to earn the Founding Father we
     * are trying to recruit.
     *
     * @return Total number of bells the <code>Player</code> needs to recruit
     *         the next founding father.
     * @see Goods#BELLS
     * @see #incrementBells
     */
    public int getTotalFoundingFatherCost() {
        return (getFatherCount() * getFatherCount() * Specification.getSpecification()
                .getIntegerOption("model.option.foundingFatherFactor").getValue() + 50);
    }

    /**
     * Returns how many total bells will be produced if no colonies are lost and
     * nothing unexpected happens.
     *
     * @return Total number of bells this <code>Player</code>'s
     *         <code>Colony</code>s will make.
     * @see Goods#BELLS
     * @see #incrementBells
     */
    public int getBellsProductionNextTurn() {
        int bellsNextTurn = 0;
        for (Colony colony : getColonies()) {
            bellsNextTurn += colony.getProductionOf(Goods.BELLS);
        }
        return bellsNextTurn;
    }

    /**
     * Returns the arrears due for a type of goods.
     *
     * @param type a <code>GoodsType</code> value
     * @return The arrears due for this type of goods.
     */
    public int getArrears(GoodsType type) {
        MarketData data = getMarket().getMarketData(type);
        return (data == null) ? 0 : data.getArrears();
    }

    /**
     * Returns the arrears due for a type of goods.
     *
     * @param goods The goods.
     * @return The arrears due for this type of goods.
     */
    public int getArrears(Goods goods) {
        return getArrears(goods.getType());
    }

    /**
     * Sets the arrears for a type of goods.
     *
     * @param goodsType a <code>GoodsType</code> value
     */
    public void setArrears(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            getMarket().putMarketData(goodsType, data);
        }
        Specification spec = Specification.getSpecification();
        data.setArrears(spec.getIntegerOption("model.option.arrearsFactor").getValue()
                        * data.getPaidForSale());
    }

    /**
     * Sets the arrears for these goods.
     *
     * @param goods The goods.
     */
    public void setArrears(Goods goods) {
        setArrears(goods.getType());
    }

    /**
     * Resets the arrears for this type of goods to zero.
     *
     * @param goodsType a <code>GoodsType</code> value
     */
    public void resetArrears(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            getMarket().putMarketData(goodsType, data);
        }
        data.setArrears(0);
    }

    /**
     * Resets the arrears for these goods to zero. This is the same as calling:
     * <br>
     * <br>
     * <code>resetArrears(goods.getType());</code>
     *
     * @param goods The goods to reset the arrears for.
     * @see #resetArrears(GoodsType)
     */
    public void resetArrears(Goods goods) {
        resetArrears(goods.getType());
    }

    /**
     * Returns true if type of goods can be traded in Europe.
     *
     * @param type The goods type.
     * @return True if there are no arrears due for this type of goods.
     */
    public boolean canTrade(GoodsType type) {
        return canTrade(type, Market.EUROPE);
    }

    /**
     * Returns true if type of goods can be traded at specified place.
     *
     * @param type The GoodsType.
     * @param marketAccess Way the goods are traded (Europe OR Custom)
     * @return <code>true</code> if type of goods can be traded.
     */
    public boolean canTrade(GoodsType type, int marketAccess) {
        MarketData data = getMarket().getMarketData(type);
        if (data == null) {
            return true;
        } else {
            return (data.getArrears() == 0 || (marketAccess == Market.CUSTOM_HOUSE && getGameOptions().getBoolean(
                    GameOptions.CUSTOM_IGNORE_BOYCOTT)));
        }
    }

    /**
     * Returns true if type of goods can be traded at specified place
     *
     * @param goods The goods.
     * @param marketAccess Place where the goods are traded (Europe OR Custom)
     * @return True if type of goods can be traded.
     */
    public boolean canTrade(Goods goods, int marketAccess) {
        return canTrade(goods.getType(), marketAccess);
    }

    /**
     * Returns true if type of goods can be traded in Europe.
     *
     * @param goods The goods.
     * @return True if there are no arrears due for this type of goods.
     */
    public boolean canTrade(Goods goods) {
        return canTrade(goods, Market.EUROPE);
    }

    /**
     * Returns the current tax.
     *
     * @return The current tax.
     */
    public int getTax() {
        return tax;
    }

    /**
     * Sets the current tax. If Thomas Paine has already joined the Continental
     * Congress, the bellsBonus is adjusted accordingly.
     *
     * @param amount The new tax.
     */
    public void setTax(int amount) {
        if (amount != tax) {
            tax = amount;
            updateAddTaxToBells();
        }
    }

    private void updateAddTaxToBells() {
        Set<Modifier> bellsBonus = featureContainer.getModifierSet("model.goods.bells");
        for (Ability ability : featureContainer.getAbilitySet("model.ability.addTaxToBells")) {
            FreeColGameObjectType source = ability.getSource();
            if (source != null) {
                for (Modifier modifier : bellsBonus) {
                    if (source.equals(modifier.getSource())) {
                        modifier.setValue(tax);
                        return;
                    }
                }
            }
        }
    }


    /**
     * Returns the current sales.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return The current sales.
     */
    public int getSales(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            return 0;
        } else {
            return data.getSales();
        }
    }

    /**
     * Modifies the current sales.
     *
     * @param goodsType a <code>GoodsType</code> value
     * @param amount The new sales.
     */
    public void modifySales(GoodsType goodsType, int amount) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            getMarket().putMarketData(goodsType, data);
        }
        int oldSales = data.getSales();
        data.setSales(oldSales + amount);
    }

    /**
     * Returns the current incomeBeforeTaxes.
     *
     * @param goodsType The GoodsType.
     * @return The current incomeBeforeTaxes.
     */
    public int getIncomeBeforeTaxes(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            return 0;
        } else {
            return data.getIncomeBeforeTaxes();
        }
    }

    /**
     * Modifies the current incomeBeforeTaxes.
     *
     * @param goodsType The GoodsType.
     * @param amount The new incomeBeforeTaxes.
     */
    public void modifyIncomeBeforeTaxes(GoodsType goodsType, int amount) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            getMarket().putMarketData(goodsType, data);
        }
        int oldAmount = data.getIncomeBeforeTaxes();
        data.setIncomeBeforeTaxes(oldAmount += amount);
    }

    /**
     * Returns the current incomeAfterTaxes.
     *
     * @param goodsType The GoodsType.
     * @return The current incomeAfterTaxes.
     */
    public int getIncomeAfterTaxes(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            return 0;
        } else {
            return data.getIncomeAfterTaxes();
        }
    }

    /**
     * Modifies the current incomeAfterTaxes.
     *
     * @param goodsType The GoodsType.
     * @param amount The new incomeAfterTaxes.
     */
    public void modifyIncomeAfterTaxes(GoodsType goodsType, int amount) {
        MarketData data = getMarket().getMarketData(goodsType);
        if (data == null) {
            data = new MarketData(goodsType);
            getMarket().putMarketData(goodsType, data);
        }
        int oldAmount = data.getIncomeAfterTaxes();
        data.setIncomeAfterTaxes(oldAmount + amount);
    }

    /**
     * Returns the difficulty level.
     *
     * @return The difficulty level.
     */
    public DifficultyLevel getDifficulty() {
        int level = getGame().getGameOptions().getInteger(GameOptions.DIFFICULTY);
        return FreeCol.getSpecification().getDifficultyLevel(level);
    }

    /**
     * Has a type of goods been traded?
     *
     * @param goodsType a <code>GoodsType</code> value
     * @return Whether these goods have been traded.
     */
    public boolean hasTraded(GoodsType goodsType) {
        MarketData data = getMarket().getMarketData(goodsType);
        return data != null && data.getTraded();
    }

    /**
     * Returns the most valuable goods available in one of the player's
     * colonies for the purposes of choosing a threat-to-boycott.
     * The goods must not currently be boycotted, the player must have traded in
     * it, and the amount will not exceed 100.
     *
     * @return A goods object, or null.
     */
    public Goods getMostValuableGoods() {
        Goods goods = null;
        if (!isEuropean()) {
            return goods;
        }
        int value = 0;
        for (Colony colony : getColonies()) {
            List<Goods> colonyGoods = colony.getCompactGoods();
            for (Goods currentGoods : colonyGoods) {
                if (getArrears(currentGoods) == 0
                    && hasTraded(currentGoods.getType())) {
                    // never discard more than 100 units
                    if (currentGoods.getAmount() > 100) {
                        currentGoods.setAmount(100);
                    }
                    int goodsValue = market.getSalePrice(currentGoods);
                    if (goodsValue > value) {
                        value = goodsValue;
                        goods = currentGoods;
                    }
                }
            }
        }
        return goods;
    }

    /**
     * Checks if the given <code>Player</code> equals this object.
     *
     * @param o The <code>Player</code> to compare against this object.
     * @return <i>true</i> if the two <code>Player</code> are equal and none
     *         of both have <code>nation == null</code> and <i>false</i>
     *         otherwise.
     */
    public boolean equals(Player o) {
        if (o == null) {
            return false;
        } else if (getId() == null || o.getId() == null) {
            // This only happens in the client code with the virtual "enemy
            // privateer" player
            // This special player is not properly associated to the Game and
            // therefore has no ID
            // TODO: remove this hack when the virtual "enemy privateer" player
            // is better implemented
            return false;
        } else {
            return getId().equals(o.getId());
        }
    }


    /**
     * A predicate that can be applied to a unit.
     */
    public abstract class UnitPredicate {
        public abstract boolean obtains(Unit unit);
    }

    /**
     * A predicate for determining active units.
     */
    public class ActivePredicate extends UnitPredicate {
        /**
         * Returns true if the unit is active (and going nowhere).
         */
        public boolean obtains(Unit unit) {
            return (!unit.isDisposed() && (unit.getMovesLeft() > 0) && (unit.getState() == UnitState.ACTIVE)
                    && (unit.getDestination() == null) && !(unit.getLocation() instanceof WorkLocation) && unit
                    .getTile() != null);
        }
    }

    /**
     * A predicate for determining units going somewhere.
     */
    public class GoingToPredicate extends UnitPredicate {
        /**
         * Returns true if the unit has order to go somewhere.
         */
        public boolean obtains(Unit unit) {
            return (!unit.isDisposed() && (unit.getMovesLeft() > 0) && (unit.getDestination() != null)
                    && !(unit.getLocation() instanceof WorkLocation) && unit.getTile() != null);
        }
    }

    /**
     * An <code>Iterator</code> of {@link Unit}s that can be made active.
     */
    public class UnitIterator implements Iterator<Unit> {

        private Iterator<Unit> unitIterator = null;

        private Player owner;

        private Unit nextUnit = null;

        private UnitPredicate predicate;


        /**
         * Creates a new <code>NextActiveUnitIterator</code>.
         *
         * @param owner The <code>Player</code> that needs an iterator of it's
         *            units.
         * @param predicate An object for deciding whether a <code>Unit</code>
         *            should be included in the <code>Iterator</code> or not.
         */
        public UnitIterator(Player owner, UnitPredicate predicate) {
            this.owner = owner;
            this.predicate = predicate;
        }

        public boolean hasNext() {
            if (nextUnit != null && predicate.obtains(nextUnit)) {
                return true;
            }
            if (unitIterator == null) {
                unitIterator = createUnitIterator();
            }
            while (unitIterator.hasNext()) {
                nextUnit = unitIterator.next();
                if (predicate.obtains(nextUnit)) {
                    return true;
                }
            }
            unitIterator = createUnitIterator();
            while (unitIterator.hasNext()) {
                nextUnit = unitIterator.next();
                if (predicate.obtains(nextUnit)) {
                    return true;
                }
            }
            nextUnit = null;
            return false;
        }

        public Unit next() {
            if (nextUnit == null || !predicate.obtains(nextUnit)) {
                hasNext();
            }
            Unit temp = nextUnit;
            nextUnit = null;
            return temp;
        }

        /**
         * Removes from the underlying collection the last element returned by
         * the iterator (optional operation).
         *
         * @exception UnsupportedOperationException no matter what.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Returns an <code>Iterator</code> for the units of this player that
         * can be active.
         */
        private Iterator<Unit> createUnitIterator() {
            ArrayList<Unit> units = new ArrayList<Unit>();
            Map map = getGame().getMap();
            Iterator<Position> tileIterator = map.getWholeMapIterator();
            while (tileIterator.hasNext()) {
                Tile t = map.getTile(tileIterator.next());
                if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(owner)) {
                    Iterator<Unit> unitIterator = t.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = unitIterator.next();
                        Iterator<Unit> childUnitIterator = u.getUnitIterator();
                        while (childUnitIterator.hasNext()) {
                            Unit childUnit = childUnitIterator.next();
                            if (predicate.obtains(childUnit)) {
                                units.add(childUnit);
                            }
                        }
                        if (predicate.obtains(u)) {
                            units.add(u);
                        }
                    }
                }
            }
            return units.iterator();
        }
    }

    /**
     * This method writes an XML-representation of this object to the given
     * stream. <br>
     * <br>
     * Only attributes visible to the given <code>Player</code> will be added
     * to that representation if <code>showAll</code> is set to
     * <code>false</code>.
     *
     * @param out The target stream.
     * @param player The <code>Player</code> this XML-representation should be
     *            made for, or <code>null</code> if
     *            <code>showAll == true</code>.
     * @param showAll Only attributes visible to <code>player</code> will be
     *            added to the representation if <code>showAll</code> is set
     *            to <i>false</i>.
     * @param toSavedGame If <code>true</code> then information that is only
     *            needed when saving a game is added.
     * @throws XMLStreamException if there are any problems writing to the
     *             stream.
     */
    protected void toXMLImpl(XMLStreamWriter out, Player player, boolean showAll, boolean toSavedGame)
            throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());
        out.writeAttribute("ID", getId());
        out.writeAttribute("index", String.valueOf(index));
        out.writeAttribute("username", name);
        out.writeAttribute("nationID", nationID);
        if (nationType != null) {
            out.writeAttribute("nationType", nationType.getId());
        }
        out.writeAttribute("color", Integer.toString(color.getRGB()));
        out.writeAttribute("admin", Boolean.toString(admin));
        out.writeAttribute("ready", Boolean.toString(ready));
        out.writeAttribute("dead", Boolean.toString(dead));
        out.writeAttribute("playerType", playerType.toString());
        out.writeAttribute("ai", Boolean.toString(ai));
        out.writeAttribute("tax", Integer.toString(tax));
        out.writeAttribute("numberOfSettlements", Integer.toString(numberOfSettlements));

        if (getGame().isClientTrusted() || showAll || equals(player)) {
            out.writeAttribute("gold", Integer.toString(gold));
            out.writeAttribute("crosses", Integer.toString(crosses));
            out.writeAttribute("bells", Integer.toString(bells));
            if (currentFather != null) {
                out.writeAttribute("currentFather", currentFather.getId());
            }
            out.writeAttribute("crossesRequired", Integer.toString(crossesRequired));
            out.writeAttribute("attackedByPrivateers", Boolean.toString(attackedByPrivateers));
            out.writeAttribute("oldSoL", Integer.toString(oldSoL));
            out.writeAttribute("score", Integer.toString(score));
        } else {
            out.writeAttribute("gold", Integer.toString(-1));
            out.writeAttribute("crosses", Integer.toString(-1));
            out.writeAttribute("bells", Integer.toString(-1));
            out.writeAttribute("crossesRequired", Integer.toString(-1));
        }
        if (newLandName != null) {
            out.writeAttribute("newLandName", newLandName);
        }
        if (entryLocation != null) {
            out.writeAttribute("entryLocation", entryLocation.getId());
        }
        // attributes end here

        for (Entry<Player, Tension> entry : tension.entrySet()) {
            out.writeStartElement(TENSION_TAG);
            out.writeAttribute("player", entry.getKey().getId());
            out.writeAttribute("value", String.valueOf(entry.getValue().getValue()));
            out.writeEndElement();
        }

        for (Entry<String, Stance> entry : stance.entrySet()) {
            out.writeStartElement(STANCE_TAG);
            out.writeAttribute("player", entry.getKey());
            out.writeAttribute("value", entry.getValue().toString());
            out.writeEndElement();
        }

        for (HistoryEvent event : history) {
            event.toXML(out, this);
        }

        for (TradeRoute route : getTradeRoutes()) {
            route.toXML(out, this);
        }

        if (market != null) {
            market.toXML(out, player, showAll, toSavedGame);
        }

        if (getGame().isClientTrusted() || showAll || equals(player)) {
            out.writeStartElement(FOUNDING_FATHER_TAG);
            out.writeAttribute(ARRAY_SIZE, Integer.toString(allFathers.size()));
            int index = 0;
            for (FoundingFather father : allFathers) {
                out.writeAttribute("x" + Integer.toString(index), father.getId());
                index++;
            }
            out.writeEndElement();

            if (europe != null) {
                europe.toXML(out, player, showAll, toSavedGame);
            }
            if (monarch != null) {
                monarch.toXML(out, player, showAll, toSavedGame);
            }
        }

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     *
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {
        setId(in.getAttributeValue(null, "ID"));
        index = Integer.parseInt(in.getAttributeValue(null, "index"));
        name = in.getAttributeValue(null, "username");
        nationID = in.getAttributeValue(null, "nationID");
        if (!name.equals(UNKNOWN_ENEMY)) {
            nationType = FreeCol.getSpecification().getNationType(in.getAttributeValue(null, "nationType"));
        }
        color = new Color(Integer.parseInt(in.getAttributeValue(null, "color")));
        admin = getAttribute(in, "admin", false);
        gold = Integer.parseInt(in.getAttributeValue(null, "gold"));
        crosses = Integer.parseInt(in.getAttributeValue(null, "crosses"));
        bells = Integer.parseInt(in.getAttributeValue(null, "bells"));
        oldSoL = getAttribute(in, "oldSoL", 0);
        score = getAttribute(in, "score", 0);
        ready = getAttribute(in, "ready", false);
        ai = getAttribute(in, "ai", false);
        dead = getAttribute(in, "dead", false);
        tax = Integer.parseInt(in.getAttributeValue(null, "tax"));
        numberOfSettlements = getAttribute(in, "numberOfSettlements", 0);
        playerType = Enum.valueOf(PlayerType.class, in.getAttributeValue(null, "playerType"));
        currentFather = FreeCol.getSpecification().getType(in, "currentFather", FoundingFather.class, null);
        crossesRequired = getAttribute(in, "crossesRequired", 12);
        newLandName = getAttribute(in, "newLandName", null);

        attackedByPrivateers = getAttribute(in, "attackedByPrivateers", false);
        final String entryLocationStr = in.getAttributeValue(null, "entryLocation");
        if (entryLocationStr != null) {
            entryLocation = (Location) getGame().getFreeColGameObject(entryLocationStr);
            if (entryLocation == null) {
                entryLocation = new Tile(getGame(), entryLocationStr);
            }
        }

        featureContainer = new FeatureContainer();
        if (nationType != null) {
            featureContainer.add(nationType.getFeatureContainer());
        }
        switch (playerType) {
        case REBEL:
        case INDEPENDENT:
            featureContainer.addAbility(new Ability("model.ability.independenceDeclared"));
            break;
        }

        tension = new HashMap<Player, Tension>();
        stance = new HashMap<String, Stance>();
        while (in.nextTag() != XMLStreamConstants.END_ELEMENT) {
            if (in.getLocalName().equals(TENSION_TAG)) {
                Player player = (Player) getGame().getFreeColGameObject(in.getAttributeValue(null, "player"));
                tension.put(player, new Tension(getAttribute(in, "value", 0)));
                in.nextTag(); // close element
            } else if (in.getLocalName().equals(FOUNDING_FATHER_TAG)) {
                int length = Integer.parseInt(in.getAttributeValue(null, ARRAY_SIZE));
                for (int index = 0; index < length; index++) {
                    String fatherId = in.getAttributeValue(null, "x" + String.valueOf(index));
                    FoundingFather father = FreeCol.getSpecification().getFoundingFather(fatherId);
                    allFathers.add(father);
                    // add only features, no other effects
                    featureContainer.add(father.getFeatureContainer());
                }
                in.nextTag();
            } else if (in.getLocalName().equals(STANCE_TAG)) {
            	String playerId = in.getAttributeValue(null, "player");
                stance.put(playerId, Enum.valueOf(Stance.class, in.getAttributeValue(null, "value")));
                in.nextTag(); // close element
            } else if (in.getLocalName().equals(Europe.getXMLElementTagName())) {
                europe = updateFreeColGameObject(in, Europe.class);
            } else if (in.getLocalName().equals(Monarch.getXMLElementTagName())) {
                monarch = updateFreeColGameObject(in, Monarch.class);
            } else if (in.getLocalName().equals(HistoryEvent.getXMLElementTagName())) {
                HistoryEvent event = new HistoryEvent();
                event.readFromXMLImpl(in);
                getHistory().add(event);
            } else if (in.getLocalName().equals(TradeRoute.getXMLElementTagName())) {
                TradeRoute route = new TradeRoute(getGame(), in);
                getTradeRoutes().add(route);
            } else if (in.getLocalName().equals(Market.getXMLElementTagName())) {
                market = updateFreeColGameObject(in, Market.class);
            } else {
                logger.warning("Unknown tag: " + in.getLocalName() + " loading player");
                in.nextTag();
            }
        }

        // sanity check: we should be on the closing tag
        if (!in.getLocalName().equals(Player.getXMLElementTagName())) {
            logger.warning("Error parsing xml: expecting closing tag </" + Player.getXMLElementTagName() + "> "
                    + "found instead: " + in.getLocalName());
        }

        if (market == null) {
            market = new Market(getGame(), this);
        }
        invalidateCanSeeTiles();
    }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "player"
     */
    public static String getXMLElementTagName() {
        return "player";
    }


}
