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

package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.text.DateFormat;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This panel displays the high scores.
 */
public final class ReportHighScoresPanel extends ReportPanel {

    /**
     * Creates the high scores report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param prefix An optional message to add at the top of the panel.
     * @param highScores The list of <code>HighScore</code>s.
     */
    public ReportHighScoresPanel(FreeColClient freeColClient, String prefix,
                                 List<HighScore> highScores) {
        super(freeColClient, "reportHighScoresAction");

        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 3, gapx 30",
                                            "[][][align right]", ""));
        if (prefix != null) {
            reportPanel.add(Utility.localizedLabel(prefix),
                            "span, wrap 10");
        }

        Font font = FontLibrary.createFont(FontLibrary.FontType.HEADER,
            FontLibrary.FontSize.SMALL);
        for (HighScore highScore : highScores) {
            JLabel scoreValue
                = new JLabel(String.valueOf(highScore.getScore()));
            scoreValue.setFont(font);
            reportPanel.add(scoreValue);

            String messageId = (highScore.getIndependenceTurn() > 0)
                ? "report.highScores.president"
                : "report.highScores.governor";
            StringTemplate template = StringTemplate.template(messageId)
                .addName("%name%", highScore.getPlayerName())
                .addName("%nation%", highScore.getNewLandName());
            JLabel headline = Utility.localizedHeaderLabel(template,
                SwingConstants.LEADING, FontLibrary.FontSize.SMALL);
            reportPanel.add(headline,
                            "span, wrap 10");

            reportPanel.add(Utility.localizedLabel("report.highScores.turn"),
                            "skip");
            int retirementTurn = highScore.getRetirementTurn();
            reportPanel.add((retirementTurn <= 0)
                ? Utility.localizedLabel("notApplicable")
                : Utility.localizedLabel(Turn.getLabel(retirementTurn)));

            reportPanel.add(Utility.localizedLabel("report.highScores.score"),
                            "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getScore())));

            reportPanel.add(Utility.localizedLabel("report.highScores.difficulty"),
                            "skip");
            reportPanel.add(Utility.localizedLabel(Messages.nameKey(highScore.getDifficulty())));

            reportPanel.add(Utility.localizedLabel("report.highScores.independence"),
                            "skip");
            int independenceTurn = highScore.getIndependenceTurn();
            reportPanel.add((independenceTurn <= 0)
                ? Utility.localizedLabel("no")
                : Utility.localizedLabel(Turn.getLabel(independenceTurn)));

            reportPanel.add(Utility.localizedLabel("report.highScores.nation"),
                            "skip");
            reportPanel.add(Utility.localizedLabel((highScore.getIndependenceTurn() > 0)
                    ? StringTemplate.name(highScore.getNationLabel())
                    : StringTemplate.key(highScore.getOldNationNameKey())));

            reportPanel.add(Utility.localizedLabel("report.highScores.nationType"),
                            "skip");
            reportPanel.add(Utility.localizedLabel(Messages.nameKey(highScore.getNationTypeId())));

            reportPanel.add(Utility.localizedLabel("report.highScores.units"),
                            "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getUnits())));

            reportPanel.add(Utility.localizedLabel("report.highScores.colonies"),
                            "skip");
            reportPanel.add(new JLabel(String.valueOf(highScore.getColonies())));

            reportPanel.add(Utility.localizedLabel("report.highScores.retired"),
                            "skip");
            DateFormat format = DateFormat
                .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
            reportPanel.add(new JLabel(format.format(highScore.getDate())),
                            "wrap 20");
        }
        reportPanel.doLayout();
    }
}
