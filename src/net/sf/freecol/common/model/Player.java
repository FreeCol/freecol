/*
 *  Player.java - Holds all the information about one player.
 *
 *  Copyright (C) 2002  The FreeCol Team
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Library General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package net.sf.freecol.common.model;

import java.awt.Color;
import java.util.Vector;
import java.util.Iterator;
import java.util.ArrayList;

import net.sf.freecol.common.FreeColException;
import net.sf.freecol.client.gui.i18n.Messages;

import org.w3c.dom.Element;
import org.w3c.dom.Document;



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

    /** The nations a player can play. */
    public static final int DUTCH = 0,
                            ENGLISH = 1,
                            FRENCH = 2,
                            SPANISH = 3;

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

    /** The maximum line of sight a unit can have in the game. */
    public static final int MAX_LINE_OF_SIGHT = 2;

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

    // Represented on the network as "color.getRGB()":
    private Color           color;

    private boolean         admin;
    private int             gold;
    private Europe          europe;
    private boolean         ready;

    /** True if this is an AI player. */
    private boolean         ai;

    private int             crosses;
    private int             bells;
    private boolean         dead = false;

    // any founding fathers in this Player's congress
    private boolean[]       fathers = new boolean[FoundingFather.FATHER_COUNT];
    private int             currentFather;

    // 0 = pre-rebels; 1 = in rebellion; 2 = independence granted
    private int             rebellionState;
    
    private int             crossesRequired = 1000000000; // Not allowed to recruit unit in the first round. ;-)


    private Location entryLocation;

    private Iterator nextActiveUnitIterator = new NextActiveUnitIterator(this);




    /**
    * Creates a new non-admin <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    */
    public Player(Game game, String name) {
        this(game, name, false);
    }

    /**
    * Creates an new AI <code>Player</code> with the specified name.
    *
    * @param game The <code>Game</code> this <code>Player</code> belongs to.
    * @param name The name that this player will use.
    * @param admin Whether or not this AI player shall be considered an Admin.
    * @param ai Whether or not this AI player shall be considered an AI player (usually true here).
    */
    public Player(Game game, String name, boolean admin, boolean ai) {
        this(game, name, admin);

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
        super(game);

        this.name = name;
        this.admin = admin;

        nation = game.getVacantNation();
        color = getDefaultNationColor(nation);
        europe = new Europe(game, this);

        // TODO (this is for testing only): Set to 0
        gold = 10000;

        crosses = 0;
        bells = 0;

        currentFather = -1;
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
    * Checks if this player is european. This includes the
    * "Royal Expeditionay Force".
    *
    * @return <i>true</i> if this player is european and <i>false</i> otherwise.
    */
    public boolean isEuropean() {
        if (nation == DUTCH || nation == ENGLISH || nation == FRENCH || nation == SPANISH ||
                nation == REF_DUTCH || nation == REF_ENGLISH || nation == REF_FRENCH || nation == REF_SPANISH) {
            return true;
        } else {
            return false;
        }
    }


    /**
    * Returns the default color for the given <code>nation</code>.
    */
    public static Color getDefaultNationColor(int nation) {
        return defaultNationColors[nation];
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
    * @return The <code>Location</code>.
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
    * Checks if this <code>Player</code> can see the given
    * <code>Tile</code>. The <code>Tile</code> can be seen if
    * it is in a {@link Unit}'s line of sight.
    *
    * @param The given <code>Tile</code>.
    * @return <i>true</i> if the <code>Player</code> can see
    *         the given <code>Tile</code> and <i>false</i>
    *         otherwise.
    */
    public boolean canSee(Tile tile) {
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
            } else if (t != null && t.getColony() != null && t.getColony().getOwner().equals(this) && t.getColony().getLineOfSight() >= t.getDistanceTo(tile)) {
                return true;
            }
        }

        return false;
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
    * Sets the rebellion status.
    * @param state The state of this player's rebellion
    * @see #getRebellionState
    */
    public void setRebellionState(int state) {
        rebellionState = state;
    }


    /**
    * Adds a founding father to this players continental congress
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
    * to its congress
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
    * Gets called when this player's turn has ended.
    */
    public void endTurn() {
        // Will be implemented later.
    }


    /**
    * Returns the europe object that this player has.
    * @return The europe object that this player has.
    */
    public Europe getEurope() {
        return europe;
    }


    /**
    * Returns the amount of gold that this player has.
    * @return The amount of gold that this player has.
    */
    public int getGold() {
        return gold;
    }

    /**
    * Determines whether this player is an AI player.
    * @return Whether this player is an AI player.
    */
    public boolean isAI() {
        return ai;
    }

    /**
    * Modifies the amount of gold that this player has. The argument
    * can be both positive and negative.
    *
    * @param amount The amount of gold that should be added to this
    *               player's gold amount (can be negative!).
    * @exception IllegalArgumentException if the player gets a negativ
    *            amount of gold after adding <code>amount</code>.
    */
    public void modifyGold(int amount) {
        if ((gold + amount) >= 0) {
            gold += amount;
        } else {
            throw new IllegalArgumentException();
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
        switch (nation) {
            case DUTCH:
                return "Dutch";
            case ENGLISH:
                return "English";
            case FRENCH:
                return "French";
            case SPANISH:
                return "Spanish";
            case INCA:
                return "Inca";
            case AZTEC:
                return "Aztec";
            case ARAWAK:
                return "Arawak";
            case CHEROKEE:
                return "Cherokee";
            case IROQUOIS:
                return "Iroquois";
            case SIOUX:
                return "Sioux";
            case APACHE:
                return "Apache";
            case TUPI:
                return "Tupi";
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
        }

        return units.iterator();
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
    * @see setCrosses
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
    * @return The number of crosses required to cause a new colonist to emigrate.
    */
    public void setCrossesRequired(int crossesRequired) {
        this.crossesRequired = crossesRequired;
    }

    
    /**
    * Updates the amount of crosses needed to emigrate a <code>Unit</code> 
    * from <code>Europe</code>.
    */
    private void updateCrossesRequired() {
        // The book I have tells me the crosses needed is:
        // [(colonist count in colonies + total colonist count) * 2] + 8.
        // So every unit counts as 2 unless they're in a colony,
        // wherein they count as 4.
        // This does that, I think. -sjm
        int count = 8;

        //ArrayList units = new ArrayList();
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
                        count += 2;
                    }

                    count += 2;
                }
            } else if (t != null && t.getSettlement() != null && (t.getSettlement() instanceof Colony)) {
              count += (((Colony)t.getSettlement()).getUnitCount()) * 4; // Units in colonies count doubly. -sjm
            }
        }

        Iterator europeUnitIterator = getEurope().getUnitIterator();
        while (europeUnitIterator.hasNext()) {
            europeUnitIterator.next();
            count += 2;
        }

        if (nation == ENGLISH) count = (count * 2) / 3;

        setCrossesRequired(count);
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
    * Prepares this <code>Player</code> for a new turn.
    */
    public void newTurn() {
        // TODO: founding fathers - need real formula to calculate req. number of bells for next father
        if (isEuropean() && bells >= (getFatherCount() * 100) + 200 ) {
            fathers[currentFather] = true;
            
            if (currentFather == FoundingFather.JOHN_PAUL_JONES) {
                getGame().getModelController().createUnit(getID() + "newTurnJohnPaulJones", getEurope(), this, Unit.FRIGATE);
            } else if (currentFather == FoundingFather.BARTOLOME_DE_LAS_CASAS) {
                for(Iterator iter = getUnitIterator(); iter.hasNext(); ) {
                    Unit u = (Unit)iter.next();
                    if (u.getType() == Unit.INDIAN_CONVERT) {
                        u.setType(Unit.FREE_COLONIST);
                    }
                }
            } else if (currentFather == FoundingFather.FRANSICO_DE_CORONADO) {
                ArrayList tiles = new ArrayList();
                
                Iterator tileIterator = getGame().getMap().getWholeMapIterator();
                while (tileIterator.hasNext()) {
                    Tile tile = (Tile) tileIterator.next();
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
            } else if (currentFather == FoundingFather.LA_SALLE) {
                Iterator tileIterator = getGame().getMap().getWholeMapIterator();
                while (tileIterator.hasNext()) {
                    Tile tile = (Tile) tileIterator.next();
                    if (tile.getColony() != null) {
                        tile.getColony().updatePopulation();
                    }
                }
            } else if (currentFather == FoundingFather.SIMON_BOLIVAR) {
                Iterator tileIterator = getGame().getMap().getWholeMapIterator();
                while (tileIterator.hasNext()) {
                    Tile tile = (Tile) tileIterator.next();
                    if (tile.getColony() != null) {
                        tile.getColony().addSoL(20);
                    }
                }
            }

            addModelMessage(this, "model.player.foundingFatherJoinedCongress", new String[][] {{"%foundingFather%", Messages.message(FoundingFather.getName(currentFather))}});
            currentFather = -1;
            bells = 0;
        }

        if (crossesRequired != -1) {
            updateCrossesRequired();
        }
    }


    /**
    * Makes an XML-representation of this object.
    *
    * @param document The document to use when creating new componenets.
    * @return The DOM-element ("Document Object Model") made to represent this "Player".
    */
    public Element toXMLElement(Player player, Document document) {
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

        if (equals(player)) {
            playerElement.setAttribute("gold", Integer.toString(gold));
            playerElement.setAttribute("crosses", Integer.toString(crosses));
            playerElement.setAttribute("bells", Integer.toString(bells));
            playerElement.setAttribute("currentFather", Integer.toString(currentFather));
            playerElement.setAttribute("crossesRequired", Integer.toString(crossesRequired));
            
            char[] fatherCharArray = new char[FoundingFather.FATHER_COUNT];
            for(int i = 0; i < fathers.length; i++)
                fatherCharArray[i] = (fathers[i] ? '1' : '0');
            String fatherStr = new String(fatherCharArray);
            playerElement.setAttribute("foundingFathers", fatherStr);
        } else {
            playerElement.setAttribute("gold", Integer.toString(-1));
            playerElement.setAttribute("crosses", Integer.toString(-1));
            playerElement.setAttribute("bells", Integer.toString(-1));
            playerElement.setAttribute("currentFather", Integer.toString(-1));
            playerElement.setAttribute("crossesRequired", Integer.toString(-1));
        }

        if (entryLocation != null) {
            playerElement.setAttribute("entryLocation", entryLocation.getID());
        }

        if (equals(player)) {
            playerElement.appendChild(europe.toXMLElement(player, document));
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
        ready = (new Boolean(playerElement.getAttribute("ready"))).booleanValue();
        ai = (new Boolean(playerElement.getAttribute("ai"))).booleanValue();
        dead = (new Boolean(playerElement.getAttribute("dead"))).booleanValue();
        rebellionState = Integer.parseInt(playerElement.getAttribute("rebellionState"));
        currentFather = Integer.parseInt(playerElement.getAttribute("currentFather"));
        crossesRequired = Integer.parseInt(playerElement.getAttribute("crossesRequired"));

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
            // Here's the old way of doing this:
            /*int random = (int)(Math.random() * UNIT_COUNT);
            while (!Unit.isRecruitable(random)) {
                random = (int)(Math.random() * UNIT_COUNT);
            }*/
            //...and here's my way of doing this: -sjm
            int chance = (int)(Math.random() * 100);
            int random = Unit.PETTY_CRIMINAL;
            if (chance < 20 && !hasFather(FoundingFather.WILLIAM_BREWSTER)) {
                 random = Unit.PETTY_CRIMINAL;
            } else if (chance < 45 && !hasFather(FoundingFather.WILLIAM_BREWSTER)) {
                 random = Unit.INDENTURED_SERVANT;
            } else if (chance < 67 && !hasFather(FoundingFather.WILLIAM_BREWSTER)) {
                 random = Unit.FREE_COLONIST;
            } else {
                switch ((int) ((chance - 67) / 2)) {
                default:
                case 0:
                    random = Unit.EXPERT_ORE_MINER;
                    break;
                case 1:
                    random = Unit.EXPERT_LUMBER_JACK;
                    break;
                case 2:
                    random = Unit.MASTER_GUNSMITH;
                    break;
                case 3:
                    random = Unit.EXPERT_SILVER_MINER;
                    break;
                case 4:
                    random = Unit.MASTER_FUR_TRADER;
                    break;
                case 5:
                    random = Unit.MASTER_CARPENTER;
                    break;
                case 6:
                    random = Unit.EXPERT_FISHERMAN;
                    break;
                case 7:
                    random = Unit.MASTER_BLACKSMITH;
                    break;
                case 8:
                    random = Unit.EXPERT_FARMER;
                    break;
                case 9:
                    random = Unit.MASTER_DISTILLER;
                    break;
                case 10:
                    random = Unit.HARDY_PIONEER;
                    break;
                case 11:
                    random = Unit.MASTER_TOBACCONIST;
                    break;
                case 12:
                    random = Unit.MASTER_WEAVER;
                    break;
                case 13:
                    random = Unit.JESUIT_MISSIONARY;
                    break;
                case 14:
                    random = Unit.FIREBRAND_PREACHER;
                    break;
                case 15:
                    random = Unit.ELDER_STATESMAN;
                    break;
                case 16:
                    random = Unit.VETERAN_SOLDIER;
                    break;
                }
            }
            return random;
        }





    /**
    * An <code>Iterator</code> of {@link Unit}s that can be made active.
    */
    public class NextActiveUnitIterator implements Iterator {

        private Iterator unitIterator = null;
        private Player owner;



        /**
        * Creates a new <code>NextActiveUnitIterator</code>.
        * @param owner The <code>Player</code> that needs an iterator of it's units.
        */
        public NextActiveUnitIterator(Player owner) {
            this.owner = owner;
        }






        public boolean hasNext() {
            if (unitIterator != null && unitIterator.hasNext()) {
                return true;
            } else {
                unitIterator = createUnitIterator();

                if (unitIterator.hasNext()) {
                    return true;
                } else {
                    return false;
                }
            }
        }


        public Object next() {
            if (unitIterator != null && unitIterator.hasNext()) {
                Unit u = (Unit) unitIterator.next();

                if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && !(u.getLocation() instanceof WorkLocation) && u.getTile() != null) {
                    return u;
                } else {
                    return next();
                }
            } else {
                unitIterator = createUnitIterator();

                if (unitIterator.hasNext()) {
                    Unit u = (Unit) unitIterator.next();

                    if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && !(u.getLocation() instanceof WorkLocation)) {
                        return u;
                    } else {
                        return next();
                    }
                } else {
                    return null;
                }
            }
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

                            if ((childUnit.getMovesLeft() > 0) && (childUnit.getState() == Unit.ACTIVE) && u.getTile() != null) {
                                units.add(childUnit);
                            }
                        }

                        if ((u.getMovesLeft() > 0) && (u.getState() == Unit.ACTIVE) && u.getTile() != null) {
                            units.add(u);
                        }
                    }
                }
            }

            return units.iterator();
        }
    }
}

