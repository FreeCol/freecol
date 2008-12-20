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

package net.sf.freecol.client.gui.panel;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.text.DateFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.HighScore;
import net.sf.freecol.common.model.Turn;

import org.w3c.dom.Element;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportHighScoresPanel extends ReportPanel implements ActionListener {


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportHighScoresPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.game.highScores"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        // Display Panel
        reportPanel.removeAll();

        FreeColClient client = getCanvas().getClient();
        ImageLibrary imageLibrary = getCanvas().getGUI().getImageLibrary();
        Element report = client.getInGameController().getHighScores();
        int number = report.getChildNodes().getLength();
        
        int[] widths1 = new int[] { 0, 30, 0 };
        int[] heights1 = new int[4 * number - 1];
        for (int index = 1; index < heights1.length; index += 2) {
            heights1[index] = margin;
        }

        reportPanel.setLayout(new HIGLayout(widths1, heights1));
        int reportRow = 1;
        int scoreColumn = 1;
        int panelColumn = 3;

        for (int i = 0; i < number; i++) {
            Element element = (Element) report.getChildNodes().item(i);
            try {
                HighScore highScore = new HighScore(element);

                JLabel scoreValue = new JLabel(String.valueOf(highScore.getScore()));
                scoreValue.setFont(smallHeaderFont);
                reportPanel.add(scoreValue, higConst.rc(reportRow, scoreColumn, "r"));
                
                String messageID = null;
                String nation = null;
                if (highScore.getIndependenceTurn() > 0) {
                    messageID = "report.highScores.president";
                    nation = highScore.getNationName();
                } else {
                    messageID = "report.highScores.governor";
                    nation = highScore.getNewLandName();
                }
                JLabel headline = new JLabel(Messages.message(messageID,
                                                              "%name%", highScore.getPlayerName(),
                                                              "%nation%", nation));
                headline.setFont(smallHeaderFont);
                reportPanel.add(headline, higConst.rc(reportRow, panelColumn));
                reportRow += 2;

                JPanel scorePanel = new JPanel();
                scorePanel.setOpaque(false);
                
                int rows = 9;
                int[] widths = new int[] { 200, 30, 200 };
                int[] heights = new int[2 * rows - 1];

                for (int index = 1; index < heights.length; index += 2) {
                    heights[index] = margin;
                }
                scorePanel.setLayout(new HIGLayout(widths, heights));
                int row = 1;
                int labelColumn = 1;
                int valueColumn = 3;

                JLabel scoreLabel = new JLabel(Messages.message("report.highScores.score"));
                scorePanel.add(scoreLabel, higConst.rc(row, labelColumn, "l"));
                JLabel scoreValue2 = new JLabel(String.valueOf(highScore.getScore()));
                scorePanel.add(scoreValue2, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel difficultyLabel = new JLabel(Messages.message("report.highScores.difficulty"));
                scorePanel.add(difficultyLabel, higConst.rc(row, labelColumn, "l"));
                JLabel difficultyValue = new JLabel(Messages.message(highScore.getDifficulty()));
                scorePanel.add(difficultyValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel independenceLabel = new JLabel(Messages.message("report.highScores.independence"));
                scorePanel.add(independenceLabel, higConst.rc(row, labelColumn, "l"));
                int independenceTurn = highScore.getIndependenceTurn();
                String independence = independenceTurn > 0 ? Turn.toString(independenceTurn) :
                    Messages.message("no");
                JLabel independenceValue = new JLabel(independence);
                scorePanel.add(independenceValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel nationLabel = new JLabel(Messages.message("report.highScores.nation"));
                scorePanel.add(nationLabel, higConst.rc(row, labelColumn, "l"));
                JLabel nationValue = new JLabel(String.valueOf(highScore.getOldNationName()));
                scorePanel.add(nationValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel nationTypeLabel = new JLabel(Messages.message("report.highScores.nationType"));
                scorePanel.add(nationTypeLabel, higConst.rc(row, labelColumn, "l"));
                JLabel nationTypeValue = new JLabel(Messages.message(highScore.getNationTypeID() + ".name"));
                scorePanel.add(nationTypeValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel unitLabel = new JLabel(Messages.message("report.highScores.units"));
                scorePanel.add(unitLabel, higConst.rc(row, labelColumn, "l"));
                JLabel unitValue = new JLabel(String.valueOf(highScore.getUnits()));
                scorePanel.add(unitValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel coloniesLabel = new JLabel(Messages.message("report.highScores.colonies"));
                scorePanel.add(coloniesLabel, higConst.rc(row, labelColumn, "l"));
                JLabel coloniesValue = new JLabel(String.valueOf(highScore.getColonies()));
                scorePanel.add(coloniesValue, higConst.rc(row, valueColumn, "r"));
                row += 2;

                JLabel dateLabel = new JLabel(Messages.message("report.highScores.retired"));
                scorePanel.add(dateLabel, higConst.rc(row, labelColumn, "l"));
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
                JLabel dateValue = new JLabel(format.format(highScore.getDate()));
                scorePanel.add(dateValue, higConst.rc(row, valueColumn, "r"));

                reportPanel.add(scorePanel, higConst.rc(reportRow, panelColumn));
                reportRow += 2;

            } catch (XMLStreamException e) {
                logger.warning(e.toString());
            }
        }

        reportPanel.doLayout();
    }

}
