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

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;


public class HighScore extends FreeColObject {

    /**
     * On retirement, an object will be named in honour of the
     * player. The nature of the object depends on the player's score.
     */
    public static enum Level {
        CONTINENT(40000),
        COUNTRY(35000),
        STATE(30000),
        CITY(25000),
        MOUNTAIN_RANGE(20000),
        RIVER(15000),
        INSTITUTE(12000),
        UNIVERSITY(10000),
        STREET(8000),
        SCHOOL(7000),
        BIRD_OF_PREY(6000),
        TREE(5000),
        FLOWER(4000),
        RODENT(3200),
        FOUL_SMELLING_PLANT(2400),
        POISONOUS_PLANT(1600),
        SLIME_MOLD_BEETLE(800),
        BLOOD_SUCKING_INSECT(400),
        INFECTIOUS_DISEASE(200),
        PARASITIC_WORM(0);

        private int minimumScore;

        Level(int minimumScore) {
            this.minimumScore = minimumScore;
        }

        public int getMinimumScore() {
            return minimumScore;
        }
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

    /**
     * The turn in which independence was granted.
     */
    private int independenceTurn = -1;

    /**
     * The name of the human player.
     */
    private String playerName;

    /**
     * Describe nationID here.
     */
    private String nationID;

    /**
     * Describe nationTypeID here.
     */
    private String nationTypeID;

    /**
     * Describe score here.
     */
    private int score;

    /**
     * Describe level here.
     */
    private Level level;

    /**
     * The name given to the new independent nation.
     */
    private String nationName;

    /**
     * The difficulty level of this game.
     */
    private String difficulty;

    /**
     * The final number of units.
     */
    private int units;

    /**
     * The final number of colonies.
     */
    private int colonies;

    /**
     * Describe newLandName here.
     */
    private String newLandName;

    /**
     * Describe date here.
     */
    private Date date;
    
    /**
     * Retirement Game year
     * 
     */
    private int retirementTurn;

    public HighScore(Player player, Date theDate) {
        date = theDate;
        retirementTurn = player.getGame().getTurn().getNumber();
        score = player.getScore();
        for (Level someLevel : Level.values()) {
            if (score >= someLevel.getMinimumScore()) {
                level = someLevel;
                break;
            }
        }
        playerName = player.getName();
        nationID = player.getNationID();
        nationTypeID = player.getNationType().getId();
        colonies = player.getColonies().size();
        units = player.getUnits().size();
        if (player.getPlayerType() == Player.PlayerType.INDEPENDENT) {
            independenceTurn = player.getGame().getTurn().getNumber();
            nationName = player.getIndependentNationName();
        } else {
            independenceTurn = -1;
        }
        // TODO: how difficult is a custom difficulty?
        difficulty = "model.difficulty.medium"; //player.getDifficulty().getId();
        newLandName = player.getNewLandName();
    }

    public HighScore(XMLStreamReader in) throws XMLStreamException {
        readFromXMLImpl(in);
    }

    public HighScore(Element element) throws XMLStreamException {
        readFromXMLElement(element);
    }


    /**
     * Get the <code>IndependenceTurn</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getIndependenceTurn() {
        return independenceTurn;
    }

    /**
     * Set the <code>IndependenceTurn</code> value.
     *
     * @param newIndependenceTurn The new IndependenceTurn value.
     */
    public final void setIndependenceTurn(final int newIndependenceTurn) {
        this.independenceTurn = newIndependenceTurn;
    }
    
    /**
     * Get the <code>RetirementTurn</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getRetirementTurn() {
        return retirementTurn;
    }

    /**
     * Set the <code>RetirementTurn</code> value.
     *
     * @param newRetirementTurn The new RetirementTurn value.
     */
    public final void setRetirementTurn(final int newRetirementTurn) {
        this.retirementTurn = newRetirementTurn;
    }

    /**
     * Get the <code>PlayerName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getPlayerName() {
        return playerName;
    }

    /**
     * Set the <code>PlayerName</code> value.
     *
     * @param newPlayerName The new PlayerName value.
     */
    public final void setPlayerName(final String newPlayerName) {
        this.playerName = newPlayerName;
    }

    /**
     * Get the <code>NationID</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getNationID() {
        return nationID;
    }

    /**
     * Set the <code>NationID</code> value.
     *
     * @param newNationID The new NationID value.
     */
    public final void setNationID(final String newNationID) {
        this.nationID = newNationID;
    }

    /**
     * Get the <code>NationTypeID</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getNationTypeID() {
        return nationTypeID;
    }

    /**
     * Set the <code>NationTypeID</code> value.
     *
     * @param newNationTypeID The new NationTypeID value.
     */
    public final void setNationTypeID(final String newNationTypeID) {
        this.nationTypeID = newNationTypeID;
    }

    /**
     * Get the <code>Score</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getScore() {
        return score;
    }

    /**
     * Set the <code>Score</code> value.
     *
     * @param newScore The new Score value.
     */
    public final void setScore(final int newScore) {
        this.score = newScore;
    }

    /**
     * Get the <code>Level</code> value.
     *
     * @return a <code>Level</code> value
     */
    public final Level getLevel() {
        return level;
    }

    /**
     * Set the <code>Level</code> value.
     *
     * @param newLevel The new Level value.
     */
    public final void setLevel(final Level newLevel) {
        this.level = newLevel;
    }

    /**
     * Get the <code>Nation's</code> localized name.
     *
     * @return a <code>String</code> value
     */
    public final String getOldNationNameKey() {
        return nationID + ".name";
    }

    /**
     * Get the <code>NationName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getNationName() {
        return nationName;
    }

    /**
     * Set the <code>NationName</code> value.
     *
     * @param newNationName The new NationName value.
     */
    public final void setNationName(final String newNationName) {
        this.nationName = newNationName;
    }

    /**
     * Get the <code>NewLandName</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getNewLandName() {
        return newLandName;
    }

    /**
     * Set the <code>NewLandName</code> value.
     *
     * @param newNewLandName The new NewLandName value.
     */
    public final void setNewLandName(final String newNewLandName) {
        this.newLandName = newNewLandName;
    }

    /**
     * Get the <code>Difficulty</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getDifficulty() {
        return difficulty;
    }

    /**
     * Set the <code>Difficulty</code> value.
     *
     * @param newDifficulty The new Difficulty value.
     */
    public final void setDifficulty(final String newDifficulty) {
        this.difficulty = newDifficulty;
    }

    /**
     * Get the <code>Units</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getUnits() {
        return units;
    }

    /**
     * Set the <code>Units</code> value.
     *
     * @param newUnits The new Units value.
     */
    public final void setUnits(final int newUnits) {
        this.units = newUnits;
    }

    /**
     * Get the <code>Colonies</code> value.
     *
     * @return an <code>int</code> value
     */
    public final int getColonies() {
        return colonies;
    }

    /**
     * Set the <code>Colonies</code> value.
     *
     * @param newColonies The new Colonies value.
     */
    public final void setColonies(final int newColonies) {
        this.colonies = newColonies;
    }

    /**
     * Get the <code>Date</code> value.
     *
     * @return a <code>Date</code> value
     */
    public final Date getDate() {
        return date;
    }

    /**
     * Set the <code>Date</code> value.
     *
     * @param newDate The new Date value.
     */
    public final void setDate(final Date newDate) {
        this.date = newDate;
    }

   /**
     * This method writes an XML-representation of this object to
     * the given stream.
     * 
     * <br><br>
     * 
     * Only attributes visible to the given <code>Player</code> will 
     * be added to that representation if <code>showAll</code> is
     * set to <code>false</code>.
     *  
     * @param out The target stream.
     * @exception XMLStreamException if there are any problems writing
     *      to the stream.
     */
    protected void toXMLImpl(XMLStreamWriter out) throws XMLStreamException {
        // Start element:
        out.writeStartElement(getXMLElementTagName());

        out.writeAttribute("date", dateFormat.format(date));
        out.writeAttribute("retirementTurn", Integer.toString(retirementTurn));
        out.writeAttribute("independenceTurn", Integer.toString(independenceTurn));
        out.writeAttribute("playerName", playerName);
        out.writeAttribute("nationID", nationID);
        out.writeAttribute("nationTypeID", nationTypeID);
        out.writeAttribute("score", Integer.toString(score));
        out.writeAttribute("level", level.toString());
        if (nationName != null) {
            out.writeAttribute("nationName", nationName);
        }
        out.writeAttribute("newLandName", newLandName);
        out.writeAttribute("difficulty", difficulty);
        out.writeAttribute("units", Integer.toString(units));
        out.writeAttribute("colonies", Integer.toString(colonies));

        out.writeEndElement();
    }

    /**
     * Initialize this object from an XML-representation of this object.
     * @param in The input stream with the XML.
     */
    protected void readFromXMLImpl(XMLStreamReader in) throws XMLStreamException {

        try {
            date = dateFormat.parse(getAttribute(in, "date", "2008-01-01 00:00:00+0000"));
        } catch (Exception e) {
            logger.warning(e.toString());
            date = new Date();
        }
        retirementTurn = getAttribute(in, "retirementTurn", 0);
        independenceTurn = getAttribute(in, "independenceTurn", 0);
        playerName = getAttribute(in, "playerName", "");
        nationID = getAttribute(in, "nationID", "model.nation.dutch");
        nationTypeID = getAttribute(in, "nationTypeID", "model.nationType.trade");
        score = getAttribute(in, "score", 0);
        level = Enum.valueOf(Level.class, getAttribute(in, "level", "PARASITIC_WORM"));
        nationName = getAttribute(in, "nationName", null);
        newLandName = getAttribute(in, "nationName", "New World");
        difficulty = getAttribute(in, "difficulty", "model.difficulty.medium");
        units = getAttribute(in, "units", 0);
        colonies = getAttribute(in, "colonies", 0);

        in.nextTag();
    }

    /**
     * Returns the tag name of the root element representing this object.
     *
     * @return the tag name.
     */
    public static String getXMLElementTagName() {
        return "highScore";
    }

}