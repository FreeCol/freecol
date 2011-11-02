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

package net.sf.freecol.client.gui.panel;

import java.awt.event.ActionEvent;
import java.text.DateFormat;
import java.util.List;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportHighScoresPanel extends ReportPanel {


    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     * @param prefix An optional message to add at the top of the panel.
     */
    public ReportHighScoresPanel(Canvas parent, String prefix) {
        super(parent.getFreeColClient(), parent, Messages.message("reportHighScoresAction.name"));
        // Display Panel
        reportPanel.removeAll();

        List<HighScore> highScores = getController().getHighScores();

        reportPanel.setLayout(new MigLayout("wrap 3, gapx 30", "[][][align right]", ""));

        if (prefix != null) {
            reportPanel.add(new JLabel(Messages.message(prefix)),
                "span, wrap 10");
        }

        for (HighScore highScore : highScores) {
            JLabel scoreValue = new JLabel(String.valueOf(highScore.getScore()));
            scoreValue.setFont(smallHeaderFont);
            reportPanel.add(scoreValue);

            String messageID = null;
            if (highScore.getIndependenceTurn() > 0) {
                messageID = "report.highScores.president";
            } else {
                messageID = "report.highScores.governor";
            }
            String country = highScore.getNewLandName();
            JLabel headline = localizedLabel(Messages.message(StringTemplate.template(messageID)
                                                              .addName("%name%", highScore.getPlayerName())
                                                              .addName("%nation%", country)));
            headline.setFont(smallHeaderFont);
            reportPanel.add(headline, "span, wrap 10");

            reportPanel.add(new JLabel(Messages.message("report.highScores.turn")), "skip");
            int retirementTurn = highScore.getRetirementTurn();
            String retirementTurnStr = (retirementTurn <= 0)
                ? Messages.message("N/A")
                : Messages.message(Turn.getLabel(retirementTurn));
            reportPanel.add(new JLabel(retirementTurnStr));

            reportPanel.add(new JLabel(Messages.message("report.highScores.score")), "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getScore())));

            reportPanel.add(new JLabel(Messages.message("report.highScores.difficulty")), "skip");
            reportPanel.add(new JLabel(Messages.message(highScore.getDifficulty())));

            reportPanel.add(new JLabel(Messages.message("report.highScores.independence")), "skip");
            int independenceTurn = highScore.getIndependenceTurn();
            String independence = (independenceTurn <= 0)
                ? Messages.message("no")
                : Messages.message(Turn.getLabel(independenceTurn));
            reportPanel.add(new JLabel(independence));

            reportPanel.add(new JLabel(Messages.message("report.highScores.nation")), "skip");
            if (highScore.getIndependenceTurn() > 0) {
                reportPanel.add(new JLabel(highScore.getNationName()));
            } else {
                reportPanel.add(new JLabel(Messages.message(highScore.getOldNationNameKey())));
            }

            reportPanel.add(new JLabel(Messages.message("report.highScores.nationType")), "skip");
            reportPanel.add(new JLabel(Messages.message(highScore.getNationTypeID() + ".name")));

            reportPanel.add(new JLabel(Messages.message("report.highScores.units")), "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getUnits())));

            reportPanel.add(new JLabel(Messages.message("report.highScores.colonies")), "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getColonies())));

            reportPanel.add(new JLabel(Messages.message("report.highScores.retired")), "skip");
            DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            reportPanel.add(new JLabel(format.format(highScore.getDate())), "wrap 20");
        }

        reportPanel.doLayout();
    }

    /**
     * Just drop the panel.  Retired players quitting is handled in
     * Canvas.retire().
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        getCanvas().remove(this);
    }
}
