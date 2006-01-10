
package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.MissingResourceException;

import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.FreeColException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;



/**
* Represents a player. The player can be either a human player or an AI-player.
*
* <br><br>
*
* In addition to storing the name, nation e.t.c. of the player, it also stores
* various defaults for the player. One example of this is the
* {@link #getEntryLocation entry location}.
*/
public class Player extends FreeColGameObject {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";

    /**
    * Constants for adding to the tension levels.
    */
    public static final int TENSION_ADD_TAKE_LAND = 350, // Grab land without paying
                            TENSION_ADD_MINOR = 100,    // Unit destroyed, etc
                            TENSION_ADD_NORMAL = 200,   // Unit destroyed in a Settlement, etc
                            TENSION_ADD_MAJOR = 300;    // Unit destroyed in a capital, etc

    /** The AI player is happy if <code>tension <= TENSION_HAPPY</code>. */
    public static final int TENSION_HAPPY = 100;

    /** The AI player is content if <code>tension <= TENSION_CONTENT && tension > TENSION_HAPPY</code>. */
    public static final int TENSION_CONTENT = 600;

    /** The AI player is displeased if <code>tension <= TENSION_DISPLEASED && tension > TENSION_CONTENT</code>. */
    public static final int TENSION_DISPLEASED = 700;

    /** The AI player is angry if <code>tension <= TENSION_ANGRY && tension > TENSION_DISPLEASED</code>. */
    public static final int TENSION_ANGRY = 800;

    /** The AI player is hateful if <code>tension > TENSION_ANGRY</code>. */
    public static final int TENSION_HATEFUL = 1000;

    

    /**
    * Contants for describing the stance towards a player.
    */
    public static final int WAR = -2,
                            CEASE_FIRE = -1,
                            PEACE = 0,
                            ALLIANCE = 1;

    public static final int NO_NATION = -1;

    /** The nations a player can play. */
    public static final int DUTCH = 0,
                            ENGLISH = 1,
                            FRENCH = 2,
                            SPANISH = 3;

    // WARNING: do not make the nations or tribes overlap!! ie: no DUTCH=0 && INCA=0

    /** The Indian tribes. Note that these values differ from IndianSettlement's by a value of 4.*/
    public static final int INCA = 4,
                            AZTEC = 5,
                            ARAWAK = 6,
                            CHEROKEE = 7,
                            IROQUOIS = 8,
                            SIOUX = 9,
                            APACHE = 10,
                            TUPI = 11;

    /** For future reference - the REF forces. */
    public static final int REF_DUTCH = 12,
                            REF_ENGLISH = 13,
                            REF_FRENCH = 14,
                            REF_SPANISH = 15;

    /** An array holding all the European nations in String form. */
    public static final String[] NATIONS = {"Dutch", "French", "English", "Spanish"};

    /** An array holding all the Native American tribes in String form. */
    public static final String[] TRIBES = {"Apache", "Arawak", "Aztec", "Cherokee",
                                        "Inca", "Iroquois", "Sioux", "Tupi"};

    public static final int NUMBER_OF_NATIONS = TRIBES.length + NATIONS.length;

    /** The maximum line of sight a unit can have in the game. */
    public static final int MAX_LINE_OF_SIGHT = 2;

    /** Constants for describing difficulty level. */
    public static final int VERY_EASY = 0,
	EASY = 1,
	MEDIUM = 2,
	HARD = 3,
	VERY_HARD = 4;

    /** 5000 in the original game, IIRC. */
    public static final int DEFAULT_ARREARS = 5000;
    
    private int difficulty = 2;
    
    /**
    * Contains booleans to see which tribes this player has met.
    */
    private boolean[] contacted = new boolean[NUMBER_OF_NATIONS];

    /**
    * Only used by AI - stores the tension levels,
    * 0-1000 with 1000 maximum hostility.
    */
    private int[] tension = new int[NUMBER_OF_NATIONS];

    /**
    * Stores the stance towards the other players. One of:
    * WAR, CEASE_FIRE, PEACE and ALLIANCE.
    */
    private int[] stance = new int[NUMBER_OF_NATIONS];


    private static final Color defaultNationColors[] = {
        Color.ORANGE,
        Color.RED,
        Color.BLUE,
        Color.YELLOW,
        new Color(244, 240, 196),
        new Color(196, 160,  32),
        new Color(104, 136, 192),
        new Color(108,  60,  24),
        new Color(116, 164,  76),
        new Color(192, 172, 132),
        new Color(144,   0,   0),
        new Color(  4,  92,   4)
    };


    private String          name;
    private int             nation;
    private String          newLandName = null;

    // Represented on the network as "color.getRGB()":
    private Color           color;

    private boolean         admin;
    private int             gold;
    private Europe          europe;
    private Monarch         monarch;
    private boolean         ready;

    /** True if this is an AI player. */
    private boolean         ai;

    private int             crosses;
    private int             bells;
    /** Bells bonus granted by Thomas Paine and Thomas Jefferson. */
    private int             bellsBonus = 0;
    private boolean         dead = false;

    // any founding fathers in this Player's congress
    private boolean[]       fathers = new boolean[FoundingFather.FATHER_COUNT];
    private int             currentFather;

    /** Market data */
    private int             tax = 0;
    private int[]           arrears = new int[Goods.NUMBER_OF_TYPES];
    private int[]           sales = new int[Goods.NUMBER_OF_TYPES];
    private int[]           incomeBeforeTaxes = new int[Goods.NUMBER_OF_TYPES];
    private int[]           incomeAfterTaxes = new int[Goods.NUMBER_OF_TYPES];

    // 0 = pre-rebels; 1 = in rebellion; 2 = independence granted
    private int             rebellionState;

    public static final int REBELLION_PRE_WAR = 0;
    public static final int REBELLION_IN_WAR = 1;
    public static final int REBELLION_POST_WAR = 2;

    private int             crossesRequired = -1;
    
    // No need for a persistent storage of this variable:
    private int colonyNameIndex = 0;


    private Location entryLocation;

    private Iterator nextActiveUnitIterator = new UnitIterator(this, new ActivePredicate());
    private Iterator nextGoingToUnitIterator = new UnitIterator(this, new GoingToPredicate());


    // Temporary variables:
    protected boolean[][] canSeeTiles = null;


    /**
    * This constructor should only be used by subclasses.
    */
    protected Player() {

    }


    /**
    * Creates a new non-admin <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    */
    public Player(Game game, String name) {
        this(game, name, false, game.getVacantNation());
    }

    /**
    * Creates an new AI <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin Whether or not this AI player shall be considered an Admin.
    * @param ai Whether or not this AI player shall be considered an AI player (usually true here).
    */
    public Player(Game game, String name, boolean admin, boolean ai, int nation) {
        this(game, name, admin, nation);

        this.ai = ai;
    }

    
    /**
    * Creates a new <code>Player</code> with specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin 'true' if this Player is an admin,
    * 'false' otherwise.
    */
    public Player(Game game, String name, boolean admin) {
        this(game, name, admin, game.getVacantNation());
    }

    /**
    * Creates a new <code>Player</code> with specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin 'true' if this Player is an admin,
    * 'false' otherwise.
    */
    public Player(Game game, String name, boolean admin, int nation) {
        super(game);

        this.name = name;
        this.admin = admin;
        this.nation = nation;

        color = getDefaultNationColor(nation);
        europe = new Europe(game, this);
        monarch = new Monarch(game, this, "");
        /** No initial arrears. */
        for ( int i = 0; i < arrears.length; i++ ) {
            arrears[i] = 0;
        }

        if (isEuropean(nation)) {
            /*
              Setting the amount of gold to "getGameOptions().getInteger(GameOptions.STARTING_MONEY)"
              just before starting the game. See "net.sf.freecol.server.control.PreGameController".
            */
            gold = 0;
	    
        } else {
            gold = 1500;
        }


        crosses = 0;
        bells = 0;

        currentFather = FoundingFather.NONE;
        rebellionState = 0;
    }


    /**
    * Initiates a new <code>Player</code> from an <code>Element</code>
    * and registers this <code>Player</code> at the specified game.
    *
    * @param game The <code>Game</code> this object belongs to.
    * @param element The <code>Element</code> in a DOM-parsed XML-tree that describes
    *                this object.
    */
    public Player(Game game, Element element) {
        super(game, element);

        readFromXMLElement(element);
    }
   

    /**
    * Checks if this player is a "royal expeditionary force.
    */
    public boolean isREF() {
        return nation == REF_DUTCH || nation == REF_ENGLISH || nation == REF_FRENCH || nation == REF_SPANISH;
    }


    /**
    * Gets the name this player has choosen for the new land.
    */
    public String getNewLandName() {
        return newLandName;
    }


    /**
    * Gets the default name this player has choosen for the new land.
    */
    public String getDefaultNewLandName() {
        return Messages.message("newLandName." + Integer.toString(getNation()));
    }

    
    /**
    * Returns the <code>Colony</code> with the given name.
    *
    * @param name The name of the <code>Colony</code>.
    * @return The <code>Colony</code> or <code>null</code>
    *         if this player does not have a <code>Colony</code>
    *         with the specified name.
    */
    public Colony getColony(String name) {
        Iterator it = getColonyIterator();
        while (it.hasNext()) {
            Colony colony = (Colony) it.next();
            if (colony.getName().equals(name)) {
                return colony;
            }
        }
        return null;
    }


    /**
    * Creates a unique colony name.
    * This is done by fetching a new default colony name from the 
    * list of default names.
    *
    * @return A <code>String</code> containing a new unused colony name
    *         from the list, if any is available, and otherwise 
    *         an automatically generated name.
    */
    public String getDefaultColonyName() {
        try {
            String name = "";
            do {
                name = Messages.message("newColonyName."
                        + Integer.toString(getNation()) + "."
                        + Integer.toString(colonyNameIndex));
                colonyNameIndex++;
            } while (getColony(name) != null);

            return name;
        } catch (MissingResourceException e) {
            String name = null;
            do {
                name = Messages.message("Colony") + colonyNameIndex;
                colonyNameIndex++;
            } while (getColony(name) != null);
            return name;
        }
    }


    /**
    * Sets the name this player uses for the new land.
    */
    public void setNewLandName(String newLandName) {
        this.newLandName = newLandName;
    }


    /**
    * Checks if this player is european. This includes the
    * "Royal Expeditionay Force".
    *
    * @return <i>true</i> if this player is european and <i>false</i> otherwise.
    */
    public boolean isEuropean() {
        return isEuropean(nation);
    }

    /**
     * Checks if this player is indian. This method returns
     * the opposite of {@link #isEuropean}.
     *
     * @return <i>true</i> if this player is indian and <i>false</i> otherwise.
     */
    public boolean isIndian() {
        return !isEuropean();
    }

    /**
    * Checks if this player is european. This includes the
    * "Royal Expeditionay Force".
    *
    * @return <i>true</i> if this player is european and <i>false</i> otherwise.
    */
    public static boolean isEuropean(int nation) {
        if (nation == DUTCH || nation == ENGLISH || nation == FRENCH || nation == SPANISH ||
                nation == REF_DUTCH || nation == REF_ENGLISH || nation == REF_FRENCH || nation == REF_SPANISH) {
            return true;
        } else {
            return false;
        }
    }
    
    
    /**
    * Returns the price of the given land.
    */
    public int getLandPrice(Tile tile)  {
        int price = 0;
        for (int i=0; i<Goods.NUMBER_OF_TYPES; i++) {
            price += tile.potential(i);
        }
        return price*40+100;
    }


    /**
    * Buys the given land.
    */
    public void buyLand(Tile tile) {
        int nation = tile.getNationOwner();
        if (nation == NO_NATION) {
            throw new IllegalStateException("The Tile is not owned by any nation!");
        }        
        if (nation == getNation()) {
            throw new IllegalStateException("The Player already owns the Tile.");
        }
        Player owner = getGame().getPlayer(nation);
        if (owner.isEuropean()) {
            throw new IllegalStateException("The owner is an european player");
        }

        int price = owner.getLandPrice(tile);
        modifyGold(-price);
        owner.modifyGold(price);
        tile.setNationOwner(getNation());
    }


    /**
    * Returns the default color for the given <code>nation</code>.
    */
    public static Color getDefaultNationColor(int nation) {
        return defaultNationColors[nation];
    }

    /**
     * Returns whether this player has met with <code>type</code>.
     */
    public boolean hasContacted(int type) {
        return contacted[type];
    }


    /**
     * Sets whether this player has contacted <code>type</code>.
     */
    public void setContacted(Player player, boolean b) {
        int type = player.getNation();

        if (type == getNation()) {
            return;
        }

        if (b == true && b != contacted[type]) {

            boolean contactedIndians = false;
            boolean contactedEuro = false;
            for (int i=INCA; i<=TUPI; i++) {
                if (contacted[i] == true) {
                    contactedIndians = true;
                }
            }
            for (int i=DUTCH; i<=SPANISH; i++) {
                if (contacted[i] == true) {
                    contactedEuro = true;
                }
            }

            // these dialogs should only appear on the first event
            if (player.isEuropean()) {
                if(!contactedEuro) {
                    addModelMessage(this, "EventPanel.MEETING_EUROPEANS", null);
                }
            } else {
                if(!contactedIndians) {
                    addModelMessage(this, "EventPanel.MEETING_NATIVES", null);
                }

                // special cases for Aztec/Inca
                if(player.getNation() == Player.AZTEC) {
                    addModelMessage(this, "EventPanel.MEETING_AZTEC", null);
                } else if(player.getNation() == Player.INCA) {
                    addModelMessage(this, "EventPanel.MEETING_INCA", null);
                }
            }
        }

        setContacted(type, b);
    }


    /**
     * Sets whether this player has contacted <code>type</code>.
     */
    public void setContacted(int type, boolean b) {
        if (type == getNation()) {
            return;
        }

        contacted[type] = b;
    }


    /**
    * Gets the default <code>Location</code> where the units
    * arriving from {@link Europe} will be put.
    *
    * @return The <code>Location</code>.
    * @see Unit#getEntryLocation
    */
    public Location getEntryLocation() {
        return entryLocation;
    }


    /**
    * Sets the <code>Location</code> where the units
    * arriving from {@link Europe} will be put as a default.
    *
    * @param entryLocation The <code>Location</code>.
    * @see #getEntryLocation
    */
    public void setEntryLocation(Location entryLocation) {
        this.entryLocation = entryLocation;
    }


    /**
    * Checks if this <code>Player</code> has explored the given <code>Tile</code>.
    * @param tile The <code>Tile</code>.
    * @return <i>true</i> if the <code>Tile</code> has been explored and
    *         <i>false</i> otherwise.
    */
    public boolean hasExplored(Tile tile) {
        return tile.isExplored();
    }


    /**
    * Sets the given tile to be explored by this player and updates the player's
    * information about the tile.
    *
    * @see Tile#updatePlayerExploredTile(Player)
    */
    public void setExplored(Tile tile) {
        // Implemented by ServerPlayer.
    }


    /**
    * Sets the tiles within the given <code>Unit</code>'s line of
    * sight to be explored by this player.
    *
    * @param unit The <code>Unit</code>.
    * @see #setExplored(Tile)
    * @see #hasExplored
    */
    public void setExplored(Unit unit) {
        if (getGame() == null || getGame().getMap() == null || unit == null || unit.getLocation() == null || unit.getTile() == null) {
            return;
        }

        if (canSeeTiles == null) {
            resetCanSeeTiles();
        }

        Iterator positionIterator = getGame().getMap().getCircleIterator(unit.getTile().getPosition(), true, unit.getLineOfSight());
        while (positionIterator.hasNext()) {
            Map.Position p = (Map.Position) positionIterator.next();
            canSeeTiles[p.getX()][p.getY()] = true;
        }
    }

    
    /**
    * Forces an update of the <code>canSeeTiles</code>. This method should be used
    * to invalidate the current <code>canSeeTiles</code>. The method
    * {@link #resetCanSeeTiles} will be called whenever it is needed.
    */
    public void invalidateCanSeeTiles() {
        canSeeTiles = null;
    }


    /**
    * Resets this player's "can see"-tiles. This is done by setting
    * all the tiles within a {@link Unit}s line of sight visible.
    * The other tiles are made unvisible.
    * <br><br>
    * Use {@link #invalidateCanSeeTiles} whenever possible.
    */
    public void resetCanSeeTiles() {
        Map map = getGame().getMap();

        if (map != null) {
            canSeeTiles = new boolean[map.getWidth()][map.getHeight()];

            if (!getGameOptions().getBoolean(GameOptions.FOG_OF_WAR)) {
                Iterator positionIterator = getGame().getMap().getWholeMapIterator();
                while (positionIterator.hasNext()) {
                    Map.Position p = (Map.Position) positionIterator.next();
                    canSeeTiles[p.getX()][p.getY()] = hasExplored(getGame().getMap().getTile(p));
                }
            } else {
                Iterator unitIterator = getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    if (unit.getLocation() == null || !(unit.getLocation() instanceof Tile)) {
                        continue;
                    }

                    Map.Position position = unit.getTile().getPosition();
                    canSeeTiles[position.getX()][position.getY()] = true;

                    Iterator positionIterator = map.getCircleIterator(position, true, unit.getLineOfSight());
                    while (positionIterator.hasNext()) {
                        Map.Position p = (Map.Position) positionIterator.next();
                        canSeeTiles[p.getX()][p.getY()] = true;
                    }
                }

                Iterator colonyIterator = getColonyIterator();
                while (colonyIterator.hasNext()) {
                    Colony colony = (Colony) colonyIterator.next();

                    Map.Position position = colony.getTile().getPosition();
                    canSeeTiles[position.getX()][position.getY()] = true;

                    Iterator positionIterator = map.getCircleIterator(position, true, colony.getLineOfSight());
                    while (positionIterator.hasNext()) {
                        Map.Position p = (Map.Position) positionIterator.next();
                        canSeeTiles[p.getX()][p.getY()] = true;
                    }
                }
            }
        }
    }


    /**
    * Checks if this <code>Player</code> can see the given
    * <code>Tile</code>. The <code>Tile</code> can be seen if
    * it is in a {@link Unit}'s line of sight.
    *
    * @param The given <code>Tile</code>.
    * @return <i>true</i> if the <code>Player</code> can see
    *         the given <code>Tile</code> and <i>false</i>
    *         otherwise.
    */
    /*public boolean canSee(Tile tile) {
        if (tile == null) {
            return false;
        }

        // First check this tile:
        if (tile.getFirstUnit() != null && tile.getFirstUnit().getOwner().equals(this)) {
            return true;
        }

        if (tile != null && tile.getColony() != null && tile.getColony().getOwner().equals(this)) {
            return true;
        }

        // Check the tiles in a MAX_LINE_OF_SIGHT radius around the given tile:
        Vector surroundingTiles = getGame().getMap().getSurroundingTiles(tile, MAX_LINE_OF_SIGHT);

        for (int i=0; i<surroundingTiles.size(); i++) {
            Tile t = (Tile) surroundingTiles.get(i);

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit unit = (Unit) unitIterator.next();
                    if (unit.getLineOfSight() >= t.getDistanceTo(tile)) {
                        return true;
                    }
                }
            }

            if (t != null && t.getColony() != null && t.getColony().getOwner().equals(this) && t.getColony().getLineOfSight() >= t.getDistanceTo(tile)) {
                return true;
            }
        }

        return false;
    }*/


    /**
    * Checks if this <code>Player</code> can see the given
    * <code>Tile</code>. The <code>Tile</code> can be seen if
    * it is in a {@link Unit}'s line of sight.
    *
    * @param tile The given <code>Tile</code>.
    * @return <i>true</i> if the <code>Player</code> can see
    *         the given <code>Tile</code> and <i>false</i>
    *         otherwise.
    */
    public boolean canSee(Tile tile) {
        if (tile == null) {
            return false;
        }

        if (canSeeTiles == null) {
            resetCanSeeTiles();
        }

        return canSeeTiles[tile.getX()][tile.getY()];
    }


    /**
    * Returns the state of this players rebellion status.
    * <pre>0 = Have not declared independence
    * 1 = Declared independence, at war with king
    * 2 = Independence granted</pre>
    * @return The rebellion state.
    */
    public int getRebellionState() {
        return rebellionState;
    }
    
    
    /**
    * Checks if this <code>Player</code> can build colonies.
    * @return <code>true</code> if this player is european, not the
    *         royal expeditionary force and not currently fighting
    *         the war of independence.
    */
    public boolean canBuildColonies() {
        return isEuropean() && getRebellionState() != REBELLION_IN_WAR && !isREF();
    }


    /**
    * Sets the rebellion status.
    * @param state The state of this player's rebellion
    * @see #getRebellionState
    */
    public void setRebellionState(int state) {
        rebellionState = state;
    }


    /**
    * Adds a founding father to this players continental congress.
    * @param type The type of Founding Father to add
    * @see FoundingFather
    */
    public void addFather(int type) {
        fathers[type] = true;
    }


    /**
    * Determines whether this player has a certain Founding father.
    * @return Whether this player has a Founding father of <code>type</code>
    * @see FoundingFather
    */
    public boolean hasFather(int type) {
        return fathers[type];
    }


    /**
    * Returns the number of founding fathers in this players congress. Used to calculate number
    * of bells needed to recruit new fathers.
    * @return The number of founding fathers in this players congress
    */
    public int getFatherCount() {
        int count = 0;
        for(int i = 0; i < fathers.length; i++) {
            if(fathers[i] == true) {
                count++;
            }
        }

        return count;
    }


    /**
    * Sets this players liberty bell production to work towards recruiting <code>father</code>
    * to its congress.
    * @param father The type of FoundingFather to recruit
    * @see FoundingFather
    */
    public void setCurrentFather(int father) {
        currentFather = father;
    }

    /**
    * Gets the {@link FoundingFather founding father} this player is working towards.
    * @return The ID of the founding father or <code>-1</code> if none.
    * @see #setCurrentFather
    * @see FoundingFather
    */
    public int getCurrentFather() {
        return currentFather;
    }

    /**
     * Returns the bell production bonus.
     */
    public int getBellsBonus() {
        return bellsBonus;
    }
    

    /**
    * Gets called when this player's turn has ended.
    */
    public void endTurn() {
        getGame().removeModelMessagesFor(this);
        resetCanSeeTiles();
    }


    /**
    * Returns the europe object that this player has.
    * @return The europe object that this player has.
    */
    public Europe getEurope() {
        return europe;
    }

    /**
     * Returns the monarch object this player has.
     *
     * @return The monarch object this player has.
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
    * @return The amount of gold that this player has or 
    *        <code>-1</code> if the amount of gold is unknown.
    */
    public int getGold() {
        return gold;
    }

    
    /**
    * Sets the amount of gold that this player has.
    * @param gold The new amount of gold.
    * @exception IllegalArgumentException if the new amount is negative.
    * @see #modifyGold
    */
    public void setGold(int gold) {
        if (this.gold == -1) {
            return;
        }
        
        this.gold = gold;
    }


    /**
    * Determines whether this player is an AI player.
    * @return Whether this player is an AI player.
    */
    public boolean isAI() {
        return ai;
    }


    /**
    * Sets whether this player is an AI player.
    */
    public void setAI(boolean ai) {
        this.ai = ai;
    }


    /**
    * Modifies the amount of gold that this player has. The argument
    * can be both positive and negative.
    *
    * @param amount The amount of gold that should be added to this
    *               player's gold amount (can be negative!).
    * @exception IllegalArgumentException if the player gets a negative
    *            amount of gold after adding <code>amount</code>.
    */
    public void modifyGold(int amount) {
        if (this.gold == -1) {
            return;
        }
            
        if ((gold + amount) >= 0) {
            gold += amount;
        } else {
            /* REMEMBER: The opponents' amount of gold is hidden for the clients */
            throw new IllegalArgumentException("The resulting amount would be negative.");
        }
    }


    /**
    * Gets a new active unit.
    * @return A <code>Unit</code> that can be made active.
    */
    public Unit getNextActiveUnit() {
        return (Unit) nextActiveUnitIterator.next();
    }

    /**
    * Gets a new going_to unit.
    * @return A <code>Unit</code> that can be made active.
    */
    public Unit getNextGoingToUnit() {
        return (Unit) nextGoingToUnitIterator.next();
    }
    
    /**
    * Checks if a new active unit can be made active.
    * @return <i>true</i> if this is the case and <i>false</i> otherwise.
    */
    public boolean hasNextActiveUnit() {
        return nextActiveUnitIterator.hasNext();
    }

    
    /**
    * Checks if a new active unit can be made active.
    * @return <i>true</i> if this is the case and <i>false</i> otherwise.
    */
    public boolean hasNextGoingToUnit() {
        return nextGoingToUnitIterator.hasNext();
    }


    /**
    * Checks if this player is an admin.
    * @return <i>true</i> if the player is an admin and <i>false</i> otherwise.
    */
    public boolean isAdmin() {
        return admin;
    }


    /**
    * Checks if this player is dead.
    * A <code>Player</code> dies when it looses the game.
    */
    public boolean isDead() {
        return dead;
    }


    /**
    * Sets this player to be dead or not.
    * @see #isDead
    */
    public void setDead(boolean dead) {
        this.dead = dead;
    }


    /**
    * Returns the name of this player.
    * @return The name of this player.
    */
    public String getName() {
        return name;
    }


    /**
    * Returns the name of this player.
    * @return The name of this player.
    */
    public String getUsername() {
        return name;
    }


    /**
    * Returns the nation of this player.
    * @return The nation of this player.
    */
    public int getNation() {
        return nation;
    }


    /**
    * Returns the nation of this player as a String.
    * @return The nation of this player as a String.
    */
    public String getNationAsString() {
        return getNationAsString(getNation());
    }


    /**
    * Returns the given nation as a String.
    * @return The given nation as a String.
    */
    public static String getNationAsString(int nation) {
        switch (nation) {
            case DUTCH:
                return Messages.message("model.nation.Dutch");
            case ENGLISH:
                return Messages.message("model.nation.English");
            case FRENCH:
                return Messages.message("model.nation.French");
            case SPANISH:
                return Messages.message("model.nation.Spanish");
            case INCA:
                return Messages.message("model.nation.Inca");
            case AZTEC:
                return Messages.message("model.nation.Aztec");
            case ARAWAK:
                return Messages.message("model.nation.Arawak");
            case CHEROKEE:
                return Messages.message("model.nation.Cherokee");
            case IROQUOIS:
                return Messages.message("model.nation.Iroquois");
            case SIOUX:
                return Messages.message("model.nation.Sioux");
            case APACHE:
                return Messages.message("model.nation.Apache");
            case TUPI:
                return Messages.message("model.nation.Tupi");
            default:
                return "INVALID";
        }
    }


    /**
    * Returns the color of this player.
    * @return The color of this player.
    */
    public Color getColor() {
        return color;
    }


    /**
    * Returns the String representation of the given Color.
    * The result is something that looks like this example: "R:23;G:230;B:89".
    * @return The String representation of the given Color.
    */
    public static String convertColorToString(Color c) {
        return "R:" + c.getRed() + ";G:" + c.getGreen() + ";B:" + c.getBlue();
    }


    /**
    * Sets the nation for this player.
    * @param n The new nation for this player.
    */
    public void setNation(int n) {
        nation = n;
    }

    /**
    * Sets the nation for this player.
    * @param n The new nation for this player.
    * @throws FreeColException In case the given nation is invalid.
    */
    public void setNation(String n) throws FreeColException {
        final String[] nationNames = {"dutch", "english", "french", "spanish", "apache",
                "arawak", "aztec", "cherokee", "inca", "iroquois", "sioux", "tupi"};
        final int[] nations = {DUTCH, ENGLISH, FRENCH, SPANISH, APACHE, ARAWAK, AZTEC,
                CHEROKEE, INCA, IROQUOIS, SIOUX, TUPI};

        for (int i = 0; i < nationNames.length; i++) {
            if (n.toLowerCase().equals(nationNames[i])) {
                setNation(nations[i]);
                return;
            }
        }

        throw new FreeColException("Invalid nation '" + n + "'.");
    }

    /**
    * Sets the color for this player.
    * @param c The new color for this player.
    */
    public void setColor(Color c) {
        color = c;
    }

    /**
    * Sets the color for this player.
    * @param c The new color for this player.
    */
    public void setColor(String c) {
        final String red,
                     green,
                     blue;
        red = c.substring(c.indexOf(':') + 1, c.indexOf(';'));
        c = c.substring(c.indexOf(';') + 1);
        green = c.substring(c.indexOf(':') + 1, c.indexOf(';'));
        c = c.substring(c.indexOf(';') + 1);
        blue = c.substring(c.indexOf(':') + 1);

        Color myColor = new Color(Integer.valueOf(red).intValue(),
                                  Integer.valueOf(green).intValue(),
                                  Integer.valueOf(blue).intValue());
        setColor(myColor);
    }


    /**
    * Checks if this <code>Player</code> is ready to start the game.
    */
    public boolean isReady() {
        return ready;
    }


    /**
    * Sets this <code>Player</code> to be ready/not ready for
    * starting the game.
    */
    public void setReady(boolean ready) {
        this.ready = ready;
    }


    /**
    * Gets an <code>Iterator</code> containing all the units this player owns.
    *
    * @return The <code>Iterator</code>.
    * @see Unit
    */
    public Iterator getUnitIterator() {
        ArrayList units = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    Iterator childUnitIterator = u.getUnitIterator();
                    while (childUnitIterator.hasNext()) {
                        Unit childUnit = (Unit) childUnitIterator.next();
                        units.add(childUnit);
                    }

                    units.add(u);
                }
            }
            if (t.getSettlement() != null && t.getSettlement().getOwner().equals(this)) {
                Iterator unitIterator = t.getSettlement().getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();
                    units.add(u);
                }
            }
        }
        
        if (getEurope() != null) {
            Iterator unitIterator = getEurope().getUnitIterator();
            while (unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();
                units.add(u);
            }
        }

        return units.iterator();
    }


    /**
    * Gets an <code>Iterator</code> containing all the colonies this player owns.
    *
    * @return The <code>Iterator</code>.
    * @see Colony
    */
    public Iterator getColonyIterator() {
        ArrayList colonies = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getColony() != null && t.getColony().getOwner() == this) {
                colonies.add(t.getColony());
            }
        }

        return colonies.iterator();
    }


    /**
    * Returns the closest <code>Location</code> in which the given ship can get repaired.
    * This is the closest {@link Colony} with a drydock, or {@link Europe} if this
    * player has no colonies with a drydock.
    *
    * @param unit The ship that needs a location to be repaired.
    * @return The closest <code>Location</code> in which the ship can be repaired.
    * @exception IllegalArgumentException if the <code>unit</code> is not a ship.
    */
    public Location getRepairLocation(Unit unit) {
        if (!unit.isNaval()) {
            throw new IllegalArgumentException();
        }

        Location closestLocation = null;
        int shortestDistance = Integer.MAX_VALUE;

        Iterator colonyIterator = getColonyIterator();
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            if (colony == null || colony.getBuilding(Building.DOCK) == null) {
                continue; // This has happened before, oddly ~ smelenchuk
            } 
            int distance;
            if (colony.getBuilding(Building.DOCK).getLevel() >= Building.SHOP &&
                    (distance = unit.getTile().getDistanceTo(colony.getTile())) < shortestDistance) {
                closestLocation = colony;
                shortestDistance = distance;
            }
        }

        if (closestLocation != null) {
            return closestLocation;
        } else {
            return getEurope();
        }
    }


    /**
    * Gets an <code>Iterator</code> containing all the indian settlements this player owns.
    *
    * @return The <code>Iterator</code>.
    * @see IndianSettlement
    */
    public Iterator getIndianSettlementIterator() {
        ArrayList indianSettlements = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getSettlement() != null && t.getSettlement().getOwner() == this && t.getSettlement() instanceof IndianSettlement) {
                indianSettlements.add(t.getSettlement());
            }
        }

        return indianSettlements.iterator();
    }
    
    
    /**
    * Returns a random settlement, or <i>null</i> if this
    * player does not own a single settlement.
    */
    public Settlement getRandomSettlement() {
        ArrayList settlements = new ArrayList();
        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getSettlement() != null && t.getSettlement().getOwner() == this) {
                settlements.add(t.getSettlement());
            }
        }
        
        if (settlements.size() == 0) {
            return null;
        }
        
        return (Settlement) settlements.get((int) (Math.random() * settlements.size()));
    }


    /**
    * Increments the player's cross count, with benefits thereof.
    * @param num The number of crosses to add.
    * @see #setCrosses
    */
    public void incrementCrosses(int num) {
        crosses += num;
    }


    /**
    * Sets the number of crosses this player possess.
    * @param crosses The number.
    * @see #incrementCrosses
    */
    public void setCrosses(int crosses) {
        this.crosses = crosses;
    }


    /**
    * Gets the number of crosses this player possess.
    * @return The number.
    * @see #setCrosses
    */
    public int getCrosses() {
        return crosses;
    }


    /**
    * Checks to see whether or not a colonist can emigrate, and does so if possible.
    * @return Whether a new colonist should immigrate.
    */
    public boolean checkEmigrate() {
        if (crosses >= getCrossesRequired()) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Gets the number of crosses required to cause a new colonist to emigrate.
    * @return The number of crosses required to cause a new colonist to emigrate.
    */
    public int getCrossesRequired() {
        return crossesRequired;
    }


    /**
    * Sets the number of crosses required to cause a new colonist to emigrate.
    * @param crossesRequired The number of crosses required to cause a new colonist to emigrate.
    */
    public void setCrossesRequired(int crossesRequired) {
        this.crossesRequired = crossesRequired;
    }


    /**
    * Updates the amount of crosses needed to emigrate a <code>Unit</code>
    * from <code>Europe</code>.
    */
    public void updateCrossesRequired() {
        // The book I have tells me the crosses needed is:
        // [(colonist count in colonies + total colonist count) * 2] + 8.
        // So every unit counts as 2 unless they're in a colony,
        // wherein they count as 4.
        int count = 8;

        Map map = getGame().getMap();

        Iterator tileIterator = map.getWholeMapIterator();
        while (tileIterator.hasNext()) {
            Tile t = map.getTile((Map.Position) tileIterator.next());

            if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(this)) {
                Iterator unitIterator = t.getUnitIterator();
                while (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    Iterator childUnitIterator = u.getUnitIterator();
                    while (childUnitIterator.hasNext()) {
                        //Unit childUnit = (Unit) childUnitIterator.next();
                        childUnitIterator.next();
                        count += 2;
                    }

                    count += 2;
                }
            } else if (t != null && t.getColony() != null && t.getColony().getOwner() == this) {
                count += t.getColony().getUnitCount() * 4; // Units in colonies count doubly. -sjm
            }
        }
        Iterator europeUnitIterator = getEurope().getUnitIterator();
        while (europeUnitIterator.hasNext()) {
            europeUnitIterator.next();
            count += 2;
        }

        if (nation == ENGLISH) {
            count = (count * 2) / 3;
        }

        setCrossesRequired(count);
    }


    /**
    * Modifies the hostiliy against the given player.
    *
    * @param player The <code>Player</code>.
    * @param addToTension The amount to add to the current tension level.
    */
    public void modifyTension(Player player, int addToTension) {
        modifyTension(player.getNation(), addToTension);
    }

    public void modifyTension(int nation, int addToTension) {        
        tension[nation] += addToTension;

        if (tension[nation]>1000) {
            tension[nation] = 1000;
        }
    }

    
    /**
    * Sets the hostiliy against the given player.
    *
    * @param player The <code>Player</code>.
    * @param tensionLevel The tension level.
    */
    public void setTension(Player player, int tensionLevel) {
        tension[player.getNation()] = tensionLevel;
        
        if (tension[player.getNation()]>1000) {
            tension[player.getNation()] = 1000;
        }
    }


    /**
    * Gets the hostility this player has against the given player.
    */
    public int getTension(Player player) {
        return tension[player.getNation()];
    }


    /**
    * Returns the stance towards a given player.
    *
    * <BR><BR>
    *
    * One of: WAR, CEASE_FIRE, PEACE and ALLIANCE.
    */
    public int getStance(Player player) {
        return stance[player.getNation()];
    }

    public int getStance(int nation) {
        return stance[nation];
    }
    
    
    /**
    * Sets the stance towards a given player.
    *
    * <BR><BR>
    *
    * One of: WAR, CEASE_FIRE, PEACE and ALLIANCE.
    */
    public void setStance(Player player, int theStance) {
        stance[player.getNation()] = theStance;
    }

    public void setStance(int nation, int theStance) {
        stance[nation] = theStance;
    }
    
    /**
     * Declares war on this player.
     *
     * @param enemy The player who declares war on this one.
     */
    public void declareWar(Player enemy) {
        declareWar(enemy.getNation());
    }

    public void declareWar(int nation) {
        setStance(nation, WAR);
        modifyTension(nation, TENSION_HATEFUL);
        if (!isEuropean()) {
            Iterator settlementIterator = getIndianSettlementIterator();
            while (settlementIterator.hasNext()) {
                IndianSettlement settlement = (IndianSettlement) settlementIterator.next();
                settlement.modifyAlarm(nation, IndianSettlement.ALARM_HATEFUL);
            }
        }
    }    

    /**
    * Gets the price for a recruit in europe.
    */
    public int getRecruitPrice() {
        return (getCrossesRequired() - crosses) * 10;
    }


    /**
    * Increments the player's bell count, with benefits thereof.
    * @param num The number of bells to add.
    */
    public void incrementBells(int num) {
        bells += num;
    }
    
    /**
    * Gets the current amount of bells this <code>Player</code> has.
    *
    * @return This player's number of bells earned towards the current Founding Father.
    * @see Goods#BELLS
    * @see #incrementBells
    */
    public int getBells() {
        return bells;
    }


    /**
    * Prepares this <code>Player</code> for a new turn.
    */
    public void newTurn() {
        if (isEuropean() && getBells() >= getTotalFoundingFatherCost()
                && currentFather != FoundingFather.NONE) {
            fathers[currentFather] = true;

            switch (currentFather) {
            case FoundingFather.JOHN_PAUL_JONES:
                // get new frigate
                getGame().getModelController().createUnit(getID() + "newTurnJohnPaulJones",
                                                          getEurope(), this, Unit.FRIGATE);
                break;
            case FoundingFather.BARTOLOME_DE_LAS_CASAS:
                // make all converts free colonists
                for(Iterator iter = getUnitIterator(); iter.hasNext(); ) {
                    Unit u = (Unit)iter.next();
                    if (u.getType() == Unit.INDIAN_CONVERT) {
                        u.setType(Unit.FREE_COLONIST);
                    }
                }
                break;
            case FoundingFather.FRANSICO_DE_CORONADO:
                // explore all tiles surrounding colonies
                ArrayList tiles = new ArrayList();

                Iterator tileIterator = getGame().getMap().getWholeMapIterator();
                while (tileIterator.hasNext()) {
                    Tile tile = getGame().getMap().getTile(((Map.Position) tileIterator.next()));
                    if (tile.getColony() != null) {
                        tiles.add(tile);
                        for (int i=0; i<8; i++) {
                            Tile addTile = getGame().getMap().getNeighbourOrNull(i, tile);
                            if (addTile != null) {
                                tiles.add(addTile);
                            }
                        }
                    }
                }

                getGame().getModelController().exploreTiles(this, tiles);
                break;
            case FoundingFather.LA_SALLE:
                // all colonies get a stockade for free
                Iterator colonyIterator = getColonyIterator();
                while (colonyIterator.hasNext()) {
                    ((Colony) colonyIterator.next()).updatePopulation();
                }
                break;
            case FoundingFather.SIMON_BOLIVAR:
                // SoL increase by 20 %
                Iterator colonyIterator2 = getColonyIterator();
                while (colonyIterator2.hasNext()) {
                    ((Colony) colonyIterator2.next()).addSoL(20);
                }
                break;
            case FoundingFather.POCAHONTAS:
                // reduce indian tension and alarm
                Iterator pi = getGame().getPlayerIterator();
                while (pi.hasNext()) {
                    Player p = (Player) pi.next();
                    if (!p.isEuropean()) {
                        p.setTension(this, 0);
                        Iterator isi = p.getIndianSettlementIterator();
                        while (isi.hasNext()) {
                            IndianSettlement is = (IndianSettlement) isi.next();
                            is.setAlarm(this, 0);
                        }
                    }
                }
                break;
            case FoundingFather.WILLIAM_BREWSTER:
                // don't recruit any more criminals or servants
                for (int i=1; i<=3; i++) {
                    if (getEurope().getRecruitable(i) == Unit.PETTY_CRIMINAL
                            || getEurope().getRecruitable(i) == Unit.INDENTURED_SERVANT) {
                        getEurope().setRecruitable(i, Unit.FREE_COLONIST);
                    }
                }
                break;
            case FoundingFather.THOMAS_JEFFERSON:
                // increase bells production by 50 %
                bellsBonus += 50;
                break;
            case FoundingFather.THOMAS_PAINE:
                // increase bell production by current tax rate
                bellsBonus += tax;
                break;
            case FoundingFather.JACOB_FUGGER:
                // lift all current boycotts
                for (int goods = 0; goods < Goods.NUMBER_OF_TYPES; goods++) {
                    setArrears(goods, 0);
                }
                break;
            }

            addModelMessage(this, "model.player.foundingFatherJoinedCongress",
                            new String[][] {{"%foundingFather%",
                            Messages.message(FoundingFather.getName(currentFather))}});

            currentFather = FoundingFather.NONE;
            bells = 0;
        }

        if (crossesRequired != -1) {
            updateCrossesRequired();
        }

        int oldSoL = 0;
        int newSoL = 0;
        int numberOfColonies = 0;
        Iterator iterator = getColonyIterator();
        while (iterator.hasNext()) {
            Colony colony = (Colony) iterator.next();
            colony.updateSoL();
            numberOfColonies++;
            oldSoL += colony.getOldSoL();
            newSoL += colony.getSoL();
        }
        if (numberOfColonies > 0) {
            oldSoL = oldSoL / numberOfColonies;
            newSoL = newSoL / numberOfColonies;
            if (oldSoL/10 != newSoL/10) {
                if (newSoL > oldSoL) {
                    addModelMessage(this, "model.player.SoLIncrease",
                                    new String [][] {{"%oldSoL%", String.valueOf(oldSoL)},
                                                     {"%newSoL%", String.valueOf(newSoL)}});
                } else {
                    addModelMessage(this, "model.player.SoLDecrease",
                                    new String [][] {{"%oldSoL%", String.valueOf(oldSoL)},
                                                     {"%newSoL%", String.valueOf(newSoL)}});
                }
            }
        }

    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Player".
    */
    public Element toXMLElement(Player player, Document document, boolean showAll, boolean toSavedGame) {
        Element playerElement = document.createElement(getXMLElementTagName());

        playerElement.setAttribute("ID", getID());
        playerElement.setAttribute("username", name);
        playerElement.setAttribute("nation", Integer.toString(nation));
        playerElement.setAttribute("color", Integer.toString(color.getRGB()));
        playerElement.setAttribute("admin", Boolean.toString(admin));
        playerElement.setAttribute("ready", Boolean.toString(ready));
        playerElement.setAttribute("dead", Boolean.toString(dead));
        playerElement.setAttribute("rebellionState", Integer.toString(rebellionState));
        playerElement.setAttribute("ai", Boolean.toString(ai));
        playerElement.setAttribute("tax", Integer.toString(tax));
        playerElement.setAttribute("difficulty", Integer.toString(difficulty));
        playerElement.setAttribute("bellsBonus", Integer.toString(bellsBonus));
        playerElement.appendChild(toArrayElement("tension", tension, document));
        playerElement.appendChild(toArrayElement("stance", stance, document));
        playerElement.appendChild(toArrayElement("arrears", arrears, document));
        playerElement.appendChild(toArrayElement("sales", sales, document));
        playerElement.appendChild(toArrayElement("incomeBeforeTaxes", incomeBeforeTaxes, document));
        playerElement.appendChild(toArrayElement("incomeAfterTaxes", incomeAfterTaxes, document));

        if (showAll || equals(player)) {
            playerElement.setAttribute("gold", Integer.toString(gold));
            playerElement.setAttribute("crosses", Integer.toString(crosses));
            playerElement.setAttribute("bells", Integer.toString(bells));
            playerElement.setAttribute("currentFather", Integer.toString(currentFather));
            playerElement.setAttribute("crossesRequired", Integer.toString(crossesRequired));

            char[] fatherCharArray = new char[FoundingFather.FATHER_COUNT];
            for(int i = 0; i < fathers.length; i++) {
                fatherCharArray[i] = (fathers[i] ? '1' : '0');
            }
            playerElement.setAttribute("foundingFathers", new String(fatherCharArray));

            StringBuffer sb = new StringBuffer(contacted.length);
            for(int i = 0; i < contacted.length; i++) {
                if(contacted[i]) {
                    sb.append('1');
                } else {
                    sb.append('0');
                }
            }
            playerElement.setAttribute("contacted", sb.toString());
        } else {
            playerElement.setAttribute("gold", Integer.toString(-1));
            playerElement.setAttribute("crosses", Integer.toString(-1));
            playerElement.setAttribute("bells", Integer.toString(-1));
            playerElement.setAttribute("currentFather", Integer.toString(-1));
            playerElement.setAttribute("crossesRequired", Integer.toString(-1));
        }

        if (newLandName != null) {
            playerElement.setAttribute("newLandName", newLandName);
        }

        if (entryLocation != null) {
            playerElement.setAttribute("entryLocation", entryLocation.getID());
        }

        if (showAll || equals(player)) {
            playerElement.appendChild(europe.toXMLElement(player, document, showAll, toSavedGame));
            playerElement.appendChild(monarch.toXMLElement(player, document, showAll, toSavedGame));
        }

        return playerElement;
    }


    /**
    * Initialize this object from an XML-representation of this object.
    * @param playerElement The DOM-element ("Document Object Model") made to represent this "Player".
    */
    public void readFromXMLElement(Element playerElement) {
        setID(playerElement.getAttribute("ID"));

        name = playerElement.getAttribute("username");
        nation = Integer.parseInt(playerElement.getAttribute("nation"));
        color = new Color(Integer.parseInt(playerElement.getAttribute("color")));
        admin = (new Boolean(playerElement.getAttribute("admin"))).booleanValue();
        gold = Integer.parseInt(playerElement.getAttribute("gold"));
        crosses = Integer.parseInt(playerElement.getAttribute("crosses"));
        bells = Integer.parseInt(playerElement.getAttribute("bells"));
        if (playerElement.hasAttribute("bellsBonus")) {
        	bellsBonus = Integer.parseInt(playerElement.getAttribute("bellsBonus"));
        } else {
        	bellsBonus = 0;
        }
        ready = (new Boolean(playerElement.getAttribute("ready"))).booleanValue();
        ai = (new Boolean(playerElement.getAttribute("ai"))).booleanValue();
        dead = (new Boolean(playerElement.getAttribute("dead"))).booleanValue();
        tax = Integer.parseInt(playerElement.getAttribute("tax"));
        difficulty = Integer.parseInt(playerElement.getAttribute("difficulty"));
        rebellionState = Integer.parseInt(playerElement.getAttribute("rebellionState"));
        currentFather = Integer.parseInt(playerElement.getAttribute("currentFather"));
        crossesRequired = Integer.parseInt(playerElement.getAttribute("crossesRequired"));

        if (getChildElement(playerElement, "tension") != null) {
            tension = readFromArrayElement("tension", getChildElement(playerElement, "tension"), new int[0]);
        } else {
            tension = new int[TRIBES.length + NATIONS.length];
        }
        
        if (getChildElement(playerElement, "stance") != null) {
            stance = readFromArrayElement("stance", getChildElement(playerElement, "stance"), new int[0]);
        } else {
            stance = new int[TRIBES.length + NATIONS.length];
        }

        if (getChildElement(playerElement, "arrears") != null) {
            arrears = readFromArrayElement("arrears", getChildElement(playerElement, "arrears"), new int[0]);
        } else {
            arrears = new int[Goods.NUMBER_OF_TYPES];
        }

        if (getChildElement(playerElement, "sales") != null) {
            sales = readFromArrayElement("sales", getChildElement(playerElement, "sales"), new int[0]);
        } else {
            sales = new int[Goods.NUMBER_OF_TYPES];
        }

        if (getChildElement(playerElement, "incomeBeforeTaxes") != null) {
            incomeBeforeTaxes = readFromArrayElement("incomeBeforeTaxes",
                                                     getChildElement(playerElement,
                                                                     "incomeBeforeTaxes"), new int[0]);
        } else {
            incomeBeforeTaxes = new int[Goods.NUMBER_OF_TYPES];
        }

        if (getChildElement(playerElement, "incomeAfterTaxes") != null) {
            incomeAfterTaxes = readFromArrayElement("incomeAfterTaxes",
                                                    getChildElement(playerElement,
                                                                    "incomeAfterTaxes"), new int[0]);
        } else {
            incomeAfterTaxes = new int[Goods.NUMBER_OF_TYPES];
        }

        if (playerElement.hasAttribute("contacted")) {
            String contacts = playerElement.getAttribute("contacted");
            for(int i = 0; i < contacts.length(); i++) {
                if(contacts.charAt(i) == '1') {
                    contacted[i] = true;
                } else {
                    contacted[i] = false;
                }
            }
        }

        if (playerElement.hasAttribute("newLandName")) {
            newLandName = playerElement.getAttribute("newLandName");
        }

        if (playerElement.hasAttribute("foundingFathers")) {
            String fatherStr = playerElement.getAttribute("foundingFathers");
            for(int i = 0; i < fatherStr.length(); i++) {
                fathers[i] = ( (fatherStr.charAt(i) == '1') ? true : false );
            }
        }

        if (playerElement.hasAttribute("entryLocation")) {
            entryLocation = (Location) getGame().getFreeColGameObject(playerElement.getAttribute("entryLocation"));
        }

        Element europeElement = getChildElement(playerElement, Europe.getXMLElementTagName());
        if (europeElement != null) {
            if (europe != null) {
                europe.readFromXMLElement(europeElement);
            } else {
                europe = new Europe(getGame(), europeElement);
            }
        }
        
        Element monarchElement = getChildElement(playerElement, Monarch.getXMLElementTagName());
        if (monarchElement != null) {
            if (monarch != null) {
                monarch.readFromXMLElement(monarchElement);
            } else {
                monarch = new Monarch(getGame(), monarchElement);
            }
        }
        resetCanSeeTiles();        
    }


    /**
    * Gets the tag name of the root element representing this object.
    * @return "player"
    */
    public static String getXMLElementTagName() {
        return "player";
    }





    /**
    * Generates a random unit type. The unit type that is returned represents
    * the type of a unit that is recruitable in Europe.
    * @return A random unit type of a unit that is recruitable in Europe.
    */
    public int generateRecruitable() {
        int random;

        if (hasFather(FoundingFather.WILLIAM_BREWSTER)) {
            // Make sure random is a number from 0 to 18:
            random = (int)(Math.random() * 19);
        } else {
            // Chance will be a number from 0 to 99 (never 100!):
            int chance = (int)(Math.random() * 100);

            if (chance < 21) {
                return Unit.PETTY_CRIMINAL;
            } else if (chance < 42) {
                return Unit.INDENTURED_SERVANT;
            } else if (chance < 62) {
                return Unit.FREE_COLONIST;
            } else {
                // Make sure random is a number from 0 to 17:
                random = (int) ((chance - 62) / 2);
            }
        }

        switch (random) {
            default:
            case 0:
                return Unit.FREE_COLONIST;
            case 1:
                return Unit.EXPERT_ORE_MINER;
            case 2:
                return Unit.EXPERT_LUMBER_JACK;
            case 3:
                return Unit.MASTER_GUNSMITH;
            case 4:
                return Unit.EXPERT_SILVER_MINER;
            case 5:
                return Unit.MASTER_FUR_TRADER;
            case 6:
                return Unit.MASTER_CARPENTER;
            case 7:
                return Unit.EXPERT_FISHERMAN;
            case 8:
                return Unit.MASTER_BLACKSMITH;
            case 9:
                return Unit.EXPERT_FARMER;
            case 10:
                return Unit.MASTER_DISTILLER;
            case 11:
                return Unit.HARDY_PIONEER;
            case 12:
                return Unit.MASTER_TOBACCONIST;
            case 13:
                return Unit.MASTER_WEAVER;
            case 14:
                return Unit.JESUIT_MISSIONARY;
            case 15:
                return Unit.FIREBRAND_PREACHER;
            case 16:
                return Unit.ELDER_STATESMAN;
            case 17:
                return Unit.VETERAN_SOLDIER;
            case 18:
                return Unit.SEASONED_SCOUT;
        }
    }

    /**
    * Gets the number of bells needed for recruiting the next
    * founding father.
    *
    * @return How many more bells the <code>Player</code> needs in order
    *       to recruit the next founding father.
    * @see Goods#BELLS
    * @see #incrementBells
    */
    public int getRemainingFoundingFatherCost() {
        return getTotalFoundingFatherCost() - getBells();
    }


    /**
    * Returns how many bells in total are needed to earn the Founding Father
    * we are trying to recruit.
    *
    * @return Total number of bells the <code>Player</code> needs to recruit the
    *       next founding father.
    * @see Goods#BELLS
    * @see #incrementBells
    */
    public int getTotalFoundingFatherCost() {
        return (getFatherCount() * getFatherCount() * 5) + 25;
    }

    /**
    * Returns how many total bells will be produced if no colonies are lost and nothing unexpected happens.
    *
    * @return          Total number of bells this <code>Player</code>'s <code>Colony</code>s will make.
    * @see Goods#BELLS
    * @see #incrementBells
    */
    public int getBellsProductionNextTurn() {
        int bellsNextTurn = 0;
        for (Iterator colonies = this.getColonyIterator(); colonies.hasNext();) {
            Colony colony = (Colony) colonies.next();
            bellsNextTurn += colony.getProductionOf(Goods.BELLS);
        }
        return bellsNextTurn;
    }

    /**
     * Returns the arrears due for a type of goods.
     *
     * @param typeOfGoods The type of goods.
     * @return The arrears due for this type of goods.
     */
    public int getArrears(int typeOfGoods) {
    	return arrears[typeOfGoods];
    }

    /**
     * Returns the arrears due for a type of goods.
     *
     * @param goods The goods.
     * @return The arrears due for this type of goods.
     */
    public int getArrears(Goods goods) {
    	return arrears[goods.getType()];
    }

    /**
     * Sets the arrears for a type of goods.
     *
     * @param typeOfGoods The type of goods.
     * @param amount The arrears due for this type of goods.
     */
    public void setArrears(int typeOfGoods, int amount) {
		if ( amount < 0 ) {
		    amount = 0;
		}
		arrears[typeOfGoods] = amount;
    }

    /**
     * Sets the default arrears for a type of goods.
     *
     * @param typeOfGoods The type of goods.
     */
    public void setArrears(int typeOfGoods) {
    	arrears[typeOfGoods] = (difficulty + 3) * 1000;
    }

    /**
     * Sets the arrears for these goods.
     *
     * @param goods The goods.
     * @param amount The arrears due for this type of goods.
     */
    public void setArrears(Goods goods, int amount) {
        setArrears(goods.getType(), amount);
    }
    
    /**
     * Sets the default arrears for these goods.
     *
     * @param goods The goods.
     */
    public void setArrears(Goods goods) {
        setArrears(goods.getType());
    }
    
    /**
     * Returns true if there are no arrears due for a type of goods.
     *
     * @param typeOfGoods The type of goods.
     * @return True if there are no arrears due for this type of goods.
     */
    public boolean canTrade(int typeOfGoods) {
		return (arrears[typeOfGoods] == 0);
    }

    /**
     * Returns true if there are no arrears due for a type of goods.
     *
     * @param goods The goods.
     * @return True if there are no arrears due for this type of goods.
     */
    public boolean canTrade(Goods goods) {
    	return (arrears[goods.getType()] == 0);
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
     * Sets the current tax.
     *
     * @param amount The new tax.
     */
    public void setTax(int amount) {
        tax = amount;
    }

    /**
     * Returns the current sales.
     *
     * @param type The type of goods.
     * @return The current sales.
     */
    public int getSales(int type) {
    	return sales[type];
    }

    /**
     * Modifies the current sales.
     *
     * @param type The type of goods.
     * @param amount The new sales.
     */
    public void modifySales(int type, int amount) {
        sales[type] += amount;
    }

    /**
     * Returns the current incomeBeforeTaxes.
     *
     * @param type The type of goods.
     * @return The current incomeBeforeTaxes.
     */
    public int getIncomeBeforeTaxes(int type) {
    	return incomeBeforeTaxes[type];
    }

    /**
     * Modifies the current incomeBeforeTaxes.
     *
     * @param type The type of goods.
     * @param amount The new incomeBeforeTaxes.
     */
    public void modifyIncomeBeforeTaxes(int type, int amount) {
        incomeBeforeTaxes[type] += amount;
    }

    /**
     * Returns the current incomeAfterTaxes.
     *
     * @param type The type of goods.
     * @return The current incomeAfterTaxes.
     */
    public int getIncomeAfterTaxes(int type) {
    	return incomeAfterTaxes[type];
    }

    /**
     * Modifies the current incomeAfterTaxes.
     *
     * @param type The type of goods.
     * @param amount The new incomeAfterTaxes.
     */
    public void modifyIncomeAfterTaxes(int type, int amount) {
        incomeAfterTaxes[type] += amount;
    }

    /**
     * Returns the difficulty level.
     *
     * @return The difficulty level.
     */
    public int getDifficulty() {
    	return difficulty;
    }

    /**
     * Sets the difficulty level.
     *
     * @param value The difficulty level.
     */
    public void setDifficulty(int value) {
    	if ( value == VERY_EASY ||
    			value == EASY ||
    			value == MEDIUM ||
    			value == HARD ||
    			value == VERY_HARD ) {
    		difficulty = value;
    	}
    }


    /**
     * Returns the most valuable goods available in one of the
     * player's colonies. The goods must not be boycotted, and the
     * amount will not exceed 100.
     *
     * @return A goods object, or null.
     */
    public Goods getMostValuableGoods() {
        Goods goods = null;

        if (!isEuropean()) {
            return goods;
        }
        
        Market market = getGame().getMarket();
        int value = 0;

        Iterator colonyIterator = getColonyIterator();
        while (colonyIterator.hasNext()) {
            Colony colony = (Colony) colonyIterator.next();
            Iterator goodsIterator = colony.getCompactGoodsIterator();
            while (goodsIterator.hasNext()) {
                Goods currentGoods = (Goods) goodsIterator.next();
                if (getArrears(currentGoods) == 0) {
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
     * A predicate that can be applied to a unit.
     *
     */
    public abstract class UnitPredicate {

        public abstract boolean obtains(Unit unit);

    }

    /**
     * A predicate for determining active units.
     */
    public class ActivePredicate extends UnitPredicate {

        /**
         * Returns true if the unit is active.
         */
        public boolean obtains(Unit unit) {
            return (!unit.isDisposed() &&
                    (unit.getMovesLeft() > 0) &&
                    (unit.getState() == Unit.ACTIVE) &&
                    !(unit.getLocation() instanceof WorkLocation) &&
                    unit.getTile() != null);
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
            return (!unit.isDisposed() &&
                    (unit.getMovesLeft() > 0) &&
                    (unit.getState() == Unit.GOING_TO) &&
                    !(unit.getLocation() instanceof WorkLocation) &&
                    unit.getTile() != null);
        }
    }


    /**
    * An <code>Iterator</code> of {@link Unit}s that can be made active.
    */
    public class UnitIterator implements Iterator {

        private Iterator unitIterator = null;
        private Player owner;
        private Unit nextUnit = null;
        private UnitPredicate predicate;


        /**
        * Creates a new <code>NextActiveUnitIterator</code>.
        * @param owner The <code>Player</code> that needs an iterator of it's units.
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
                nextUnit = (Unit) unitIterator.next();
                if (predicate.obtains(nextUnit)) {
                    return true;
                }
            }
            unitIterator = createUnitIterator();
            while (unitIterator.hasNext()) {
                nextUnit = (Unit) unitIterator.next();
                if (predicate.obtains(nextUnit)) {
                    return true;
                }
            }

            nextUnit = null;
            return false;
        }


        public Object next() {
            if (nextUnit == null || !predicate.obtains(nextUnit)) {
                hasNext();
            }

            Unit temp = nextUnit;
            nextUnit = null;
            return temp;
        }


        /**
        * Removes from the underlying collection the last element returned by the
        * iterator (optional operation).
        *
        * @exception UnsupportedOperationException no matter what.
        */
        public void remove() {
            throw new UnsupportedOperationException();
        }


        /**
        * Returns an <code>Iterator</code> for the units of this player that can be active.
        */
        private Iterator createUnitIterator() {
            ArrayList units = new ArrayList();
            Map map = getGame().getMap();

            Iterator tileIterator = map.getWholeMapIterator();
            while (tileIterator.hasNext()) {
                Tile t = map.getTile((Map.Position) tileIterator.next());

                if (t != null && t.getFirstUnit() != null && t.getFirstUnit().getOwner().equals(owner)) {
                    Iterator unitIterator = t.getUnitIterator();
                    while (unitIterator.hasNext()) {
                        Unit u = (Unit) unitIterator.next();

                        Iterator childUnitIterator = u.getUnitIterator();
                        while (childUnitIterator.hasNext()) {
                            Unit childUnit = (Unit) childUnitIterator.next();

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

}

