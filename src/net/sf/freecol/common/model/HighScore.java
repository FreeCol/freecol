/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.FreeCol;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.io.FreeColXMLWriter;

import org.w3c.dom.Element;


/**
 * A FreeCol high score record.
 */
public class HighScore extends FreeColObject {

    private static final Logger logger = Logger.getLogger(HighScore.class.getName());

    /** The number of high scores to allow in the high scores list. */
    public static final int NUMBER_OF_HIGH_SCORES = 10;

    /**
     * On retirement, an object will be named in honour of the
     * player.  The nature of the object depends on the player's score.
     */
    public static enum ScoreLevel {
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

        private final int minimumScore;


        ScoreLevel(int minimumScore) {
            this.minimumScore = minimumScore;
        }

        public int getMinimumScore() {
            return minimumScore;
        }
    }

    /** The format to use for dates.  Almost ISO8601. */
    private static final SimpleDateFormat dateFormat
        = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");

    /** The turn in which independence was granted. */
    private int independenceTurn = -1;

    /** The name of the human player. */
    private String playerName;

    /** The nation that retired. */
    private String nationId;

    /** The nation type that retired. */
    private String nationTypeId;

    /** The high score. */
    private int score;

    /** The ScoreLevel/title for this score. */
    private ScoreLevel level;

    /** The name given to the new independent nation. */
    private String nationName;

    /** The difficulty level of this game. */
    private String difficulty;

    /** The final number of units. */
    private int units;

    /** The final number of colonies. */
    private int colonies;

    /** The name for the New World. */
    private String newLandName;

    /** The date for this score. */
    private Date date;
    
    /** The turn when the player retired. */
    private int retirementTurn;


    /**
     * Create a new high score record.
     *
     * @param player The <code>Player</code> the score is for.
     * @param theDate The <code>Data</code> the score is created.
     */
    public HighScore(Player player, Date theDate) {
        Game game = player.getGame();
        date = theDate;
        retirementTurn = game.getTurn().getNumber();
        score = player.getScore();
        for (ScoreLevel someLevel : ScoreLevel.values()) {
            if (score >= someLevel.getMinimumScore()) {
                level = someLevel;
                break;
            }
        }
        playerName = player.getName();
        nationId = player.getNationId();
        nationTypeId = player.getNationType().getId();
        colonies = player.getColonies().size();
        units = player.getUnits().size();
        if (player.getPlayerType() == Player.PlayerType.INDEPENDENT) {
            independenceTurn = game.getTurn().getNumber();
            nationName = player.getIndependentNationName();
        } else {
            independenceTurn = -1;
        }
        difficulty = game.getSpecification().getDifficultyLevel();
        newLandName = player.getNewLandName();
    }

    /**
     * Create a new <code>HighScore</code> by reading a stream.
     *
     * @param xr The <code>FreeColXMLReader</code> to read.
     * @exception XMLStreamException if there is a problem reading the stream.
     */
    public HighScore(FreeColXMLReader xr) throws XMLStreamException {
        readFromXML(xr);
    }

    /**
     * Create a new <code>HighScore</code> by reading an element.
     *
     * @param element The <code>Element</code> to read.
     */
    public HighScore(Element element) {
        readFromXMLElement(element);
    }


    /**
     * Get the turn independence occurs.
     *
     * @return The independence turn.
     */
    public final int getIndependenceTurn() {
        return independenceTurn;
    }

    /**
     * Get the turn the player retired.
     *
     * @return The retirement turn.
     */
    public final int getRetirementTurn() {
        return retirementTurn;
    }

    /**
     * Get the player name.
     *
     * @return The player name.
     */
    public final String getPlayerName() {
        return playerName;
    }

    /**
     * Get the nation identifier.
     *
     * @return The nation identifier.
     */
    public final String getNationId() {
        return nationId;
    }

    /**
     * Get the nation type identifier.
     *
     * @return The nation type identifier.
     */
    public final String getNationTypeId() {
        return nationTypeId;
    }

    /**
     * Get the final score.
     *
     * @return The score.
     */
    public final int getScore() {
        return score;
    }

    /**
     * Get the <code>ScoreLevel</code> corresponding to the score.
     *
     * @return The score level.
     */
    public final ScoreLevel getLevel() {
        return level;
    }

    /**
     * Get the original nation localized name key.
     *
     * @return The old name key.
     */
    public final String getOldNationNameKey() {
        return Messages.nameKey(nationId);
    }

    /**
     * Get the independent nation name.
     *
     * @return The independent nation name.
     */
    public final String getNationLabel() {
        return nationName;
    }

    /**
     * Get the name given to the New World.
     *
     * @return The new land name.
     */
    public final String getNewLandName() {
        return newLandName;
    }

    /**
     * Get the game difficulty key.
     *
     * @return The game difficulty key.
     */
    public final String getDifficulty() {
        return difficulty;
    }

    /**
     * Get number of units.
     *
     * @return The number of units.
     */
    public final int getUnits() {
        return units;
    }

    /**
     * Get the number of colonies.
     *
     * @return The number of colonies.
     */
    public final int getColonies() {
        return colonies;
    }

    /**
     * Get the <code>Date</code> the score was achieved.
     *
     * @return The score <code>Date</code>.
     */
    public final Date getDate() {
        return date;
    }


    // Utilities for manipulating lists of high scores, and serialization
    // with the high scores file.

    private static final String HIGH_SCORES_TAG = "highScores";

    /**
     * Tidy a list of scores into canonical form.  That is, sorted and
     * with no more that NUMBER_OF_HIGH_SCORES members.
     *
     * @param scores The list of <code>HighScore</code>s to operate on.
     */
    private static void tidyScores(List<HighScore> scores) {
        if (scores.size() > NUMBER_OF_HIGH_SCORES) {
            scores = scores.subList(0, NUMBER_OF_HIGH_SCORES - 1);
        }
        Collections.sort(scores);
    }

    /**
     * Can a given score be added to a high score list.
     *
     * @param score The score to check.
     * @param scores A list of <code>HighScore</code> to check against.
     * @return True if the given score can be added to the list.
     */
    public static boolean checkHighScore(int score, List<HighScore> scores) {
        return /*!FreeColDebugger.isInDebugMode()
                 && */score >= 0
            && (scores.size() < NUMBER_OF_HIGH_SCORES
                || score > scores.get(scores.size()-1).getScore());
    }

    /**
     * Tries to adds a new high score for player.
     *
     * @param player The <code>Player</code> to add a high score for.
     * @return True if the score was high enough to be added to the
     *     high score list.
     */
    public static boolean newHighScore(Player player) {
        List<HighScore> scores = loadHighScores();
        if (!checkHighScore(player.getScore(), scores)) return false;
        HighScore hs = new HighScore(player, new Date());
        scores.add(hs);
        tidyScores(scores);
        return saveHighScores(scores);
    }

    /**
     * Load the high scores.
     *
     * @return A list of <code>HighScore</code>s from the high score file.
     */
    public static List<HighScore> loadHighScores() {
        List<HighScore> scores = new ArrayList<>();
        File hsf = FreeColDirectories.getHighScoreFile();
        if (!hsf.exists()) {
            try {
                hsf.createNewFile();
                saveHighScores(scores);
                logger.info("Created empty high score file: " + hsf.getPath());
            } catch (IOException ioe) {
                scores = null;
                logger.log(Level.WARNING, "Unable to create high score file: "
                           + hsf.getPath(), ioe);
            }
            return scores;
        }

        try (
            FileInputStream fis = new FileInputStream(hsf);
            FreeColXMLReader xr = new FreeColXMLReader(fis);
        ) {
            xr.nextTag();

            while (xr.nextTag() != XMLStreamConstants.END_ELEMENT) {
                final String tag = xr.getLocalName();
                if (HighScore.getXMLElementTagName().equals(tag)) {
                    scores.add(new HighScore(xr));
                }
            }
        } catch (Exception e) { // Do not crash on high score fail.
            logger.log(Level.WARNING, "Error loading high scores.", e);
        }
        tidyScores(scores);
        return scores;
    }

    /**
     * Saves high scores.
     *
     * @param scores The list of <code>HighScore</code>s to save.
     * @return True if the high scores were saved to the high score file.
     */
    public static boolean saveHighScores(List<HighScore> scores) {
        boolean ret = false;
        if (scores == null) return false;
        tidyScores(scores);

        File hsf = FreeColDirectories.getHighScoreFile();
        try (
            FileOutputStream fos = new FileOutputStream(hsf);
            FreeColXMLWriter xw = new FreeColXMLWriter(fos,
                FreeColXMLWriter.WriteScope.toSave(), true);
        ) {
            ret = true;
            xw.writeStartDocument("UTF-8", "1.0");
            xw.writeStartElement(HIGH_SCORES_TAG);
            int count = 0;
            for (HighScore score : scores) {
                score.toXML(xw);
                count++;
            }
            xw.writeEndElement();
            xw.writeEndDocument();
            xw.flush();
        } catch (FileNotFoundException fnfe) {
            logger.log(Level.WARNING, "Failed to open high scores file.", fnfe);
            ret = false;
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error creating FreeColXMLWriter.", ioe);
            ret = false;
        } catch (XMLStreamException xse) {
            logger.log(Level.WARNING, "Failed to write high scores file.", xse);
            ret = false;
        }
        return ret;
    }


    // Override FreeColObject

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(FreeColObject other) {
        int cmp = 0;
        if (other instanceof HighScore) {
            HighScore hs = (HighScore)other;
            cmp = hs.getScore() - getScore();
        }
        if (cmp == 0) cmp = super.compareTo(other);
        return cmp;
    }


    // Serialization.
    // High scores are only FreeColObjects so that they can be c-s serialized,
    // they do not have ids.


    private static final String COLONIES_TAG = "colonies";
    private static final String DATE_TAG = "date";
    private static final String DIFFICULTY_TAG = "difficulty";
    private static final String INDEPENDENCE_TURN_TAG = "independenceTurn";
    private static final String LEVEL_TAG = "level";
    private static final String NATION_ID_TAG = "nationId";
    private static final String NATION_NAME_TAG = "nationName";
    private static final String NATION_TYPE_ID_TAG = "nationTypeId";
    private static final String NEW_LAND_NAME_TAG = "newLandName";
    private static final String PLAYER_NAME_TAG = "playerName";
    private static final String RETIREMENT_TURN_TAG = "retirementTurn";
    private static final String SCORE_TAG = "score";
    private static final String UNITS_TAG = "units";
    // @compat 0.10.7
    private static final String OLD_NATION_ID_TAG = "nationID";
    private static final String OLD_NATION_TYPE_ID_TAG = "nationTypeID";
    // end @compat


    /**
     * {@inheritDoc}
     */
    @Override
    protected void writeAttributes(FreeColXMLWriter xw) throws XMLStreamException {
        // HighScores do not have ids, no super.writeAttributes().

        long l = date.getTime();
        xw.writeAttribute(DATE_TAG, l);

        xw.writeAttribute(RETIREMENT_TURN_TAG, retirementTurn);

        xw.writeAttribute(INDEPENDENCE_TURN_TAG, independenceTurn);

        xw.writeAttribute(PLAYER_NAME_TAG, playerName);

        xw.writeAttribute(NATION_ID_TAG, nationId);

        xw.writeAttribute(NATION_TYPE_ID_TAG, nationTypeId);

        xw.writeAttribute(SCORE_TAG, score);

        xw.writeAttribute(LEVEL_TAG, level.toString());

        if (nationName != null) {
            xw.writeAttribute(NATION_NAME_TAG, nationName);
        }

        if (newLandName != null) {
            xw.writeAttribute(NEW_LAND_NAME_TAG, newLandName);
        }

        xw.writeAttribute(DIFFICULTY_TAG, difficulty);

        xw.writeAttribute(UNITS_TAG, units);

        xw.writeAttribute(COLONIES_TAG, colonies);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void readAttributes(FreeColXMLReader xr) throws XMLStreamException {
        // HighScores do not have ids, no super.readAttributes().

        date = null;
        try {
            long l = xr.getAttribute(DATE_TAG, -1L);
            if (l >= 0) date = new Date(l);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Bad long date", e);
        }
        // Early 0.11.x had a bug that wrote date as a float
        if (date == null) {
            try {
                float f = xr.getAttribute(DATE_TAG, -1.0f);
                if (f >= 0.0 && f < Long.MAX_VALUE) {
                    date = new Date(new Float(f).longValue());
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Bad float date", e);
            }
        } 
        // @compat 0.10.x
        // Serializing the long as of 0.11.x
        if (date == null) {
            String str = xr.getAttribute(DATE_TAG, "2014-07-01 00:00:00+0000");
            try {
                date = dateFormat.parse(str);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Bad date: " + str, e);
            }
        }
        // end @compat
        if (date == null) date = new Date(); // Give up
        
        retirementTurn = xr.getAttribute(RETIREMENT_TURN_TAG, 0);

        independenceTurn = xr.getAttribute(INDEPENDENCE_TURN_TAG, 0);

        playerName = xr.getAttribute(PLAYER_NAME_TAG, "anonymous");

        nationId = xr.getAttribute(NATION_ID_TAG,
            // @compat 0.10.7
            xr.getAttribute(OLD_NATION_ID_TAG,
            // end @compat
                "model.nation.dutch"));

        nationTypeId = xr.getAttribute(NATION_TYPE_ID_TAG,
            // @compat 0.10.7
            xr.getAttribute(OLD_NATION_TYPE_ID_TAG,
            // end @compat
                "model.nationType.trade"));

        score = xr.getAttribute(SCORE_TAG, 0);

        level = xr.getAttribute(LEVEL_TAG, ScoreLevel.class,
                                ScoreLevel.PARASITIC_WORM);

        nationName = xr.getAttribute(NATION_NAME_TAG, "Freedonia");

        newLandName = xr.getAttribute(NEW_LAND_NAME_TAG, "New World");
        
        difficulty = xr.getAttribute(DIFFICULTY_TAG,
                                     FreeCol.getDifficulty());

        units = xr.getAttribute(UNITS_TAG, 0);

        colonies = xr.getAttribute(COLONIES_TAG, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getXMLTagName() { return getXMLElementTagName(); }

    /**
     * Gets the tag name of the root element representing this object.
     *
     * @return "highScore".
     */
    public static String getXMLElementTagName() {
        return "highScore";
    }
}
