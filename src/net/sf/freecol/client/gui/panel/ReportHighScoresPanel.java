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
        
        int[] widths = new int[] { 0 };
        int[] heights = new int[number == 0 ? 0 : 2 * number - 1];

        for (int index = 1; index < heights.length; index += 2) {
            heights[index] = margin;
        }

        reportPanel.setLayout(new HIGLayout(widths, heights));

        for (int i = 0; i < number; i++) {
            Element element = (Element) report.getChildNodes().item(i);
            try {
                HighScore highScore = new HighScore(element);
                String text = highScore.getScore() + "\n" + 
                    highScore.getPlayerName();
                reportPanel.add(getDefaultTextArea(text),
                                higConst.rc(2 * i + 1, 0));
            } catch (XMLStreamException e) {
                logger.warning(e.toString());
            }
        }

        reportPanel.doLayout();
    }

}
