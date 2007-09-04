package net.sf.freecol.client.gui.panel;

import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Player;

import org.w3c.dom.Element;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportForeignAffairPanel extends ReportPanel implements ActionListener {
    public static final String COPYRIGHT = "Copyright (C) 2003-2006 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";


    /**
     * The constructor that will add the items to this panel.
     * 
     * @param parent The parent of this panel.
     */
    public ReportForeignAffairPanel(Canvas parent) {
        super(parent, Messages.message("menuBar.report.foreign"));
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(0, 2));

        int[] widths = new int[] { 0, 12, 0, 12, 0, 0 };
        int[] heights = new int[] { 0, 12, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0 };
        int coatColumn = 1;
        int labelColumn = 3;
        int valueColumn = 5;
        int percentColumn = 6;
        HIGConstraints higConst = new HIGConstraints();

        Element report = getCanvas().getClient().getInGameController().getForeignAffairsReport();
        int number = report.getChildNodes().getLength();
        for (int i = 0; i < number; i++) {
            Element enemyElement = (Element) report.getChildNodes().item(i);
            JPanel enemyPanel = new JPanel(new HIGLayout(widths, heights));
            enemyPanel.setOpaque(false);
            int row = 1;
            Player enemy = (Player) getCanvas().getClient().getGame().getFreeColGameObject(enemyElement.getAttribute("nation"));
            final Image coatOfArms = (Image) UIManager.get(enemy.getNation().getID() + ".coatOfArms.image");
            enemyPanel.add(new JLabel(new ImageIcon(coatOfArms)),
                           higConst.rcwh(row, coatColumn, 1, heights.length, "t"));
            enemyPanel.add(new JLabel(enemy.getNation().getName()), higConst.rc(row, labelColumn));
            row += 2;
            enemyPanel.add(new JLabel(Messages.message("report.stance")), higConst.rc(row, labelColumn));
            int stance = Integer.parseInt(enemyElement.getAttribute("stance"));
            enemyPanel.add(new JLabel(Player.getStanceAsString(stance)),
                           higConst.rc(row, valueColumn, "r"));
            row++;
            enemyPanel.add(new JLabel(Messages.message("report.numberOfColonies")), higConst.rc(row, labelColumn));
            enemyPanel.add(new JLabel(enemyElement.getAttribute("numberOfColonies"), JLabel.TRAILING), higConst.rc(row,
                    valueColumn));
            row++;
            enemyPanel.add(new JLabel(Messages.message("report.numberOfUnits")), higConst.rc(row, labelColumn));
            enemyPanel.add(new JLabel(enemyElement.getAttribute("numberOfUnits"), JLabel.TRAILING), higConst.rc(row,
                    valueColumn));
            row++;
            enemyPanel.add(new JLabel(Messages.message("report.militaryStrength")), higConst.rc(row, labelColumn));
            enemyPanel.add(new JLabel(enemyElement.getAttribute("militaryStrength"), JLabel.TRAILING), higConst.rc(row,
                    valueColumn));
            row++;
            enemyPanel.add(new JLabel(Messages.message("report.navalStrength")), higConst.rc(row, labelColumn));
            enemyPanel.add(new JLabel(enemyElement.getAttribute("navalStrength"), JLabel.TRAILING), higConst.rc(row,
                    valueColumn));
            row++;
            enemyPanel.add(new JLabel(Messages.message("goldTitle")), higConst.rc(row, labelColumn));
            enemyPanel.add(new JLabel(enemyElement.getAttribute("gold"), JLabel.TRAILING), 
                           higConst.rc(row, valueColumn));
            row += 2;

            if (enemyElement.hasAttribute("tax")) {
                enemyPanel.add(new JLabel(Messages.message("menuBar.colopedia.father")), higConst.rc(row, labelColumn));
                enemyPanel.add(new JLabel(enemyElement.getAttribute("foundingFathers"), JLabel.TRAILING), higConst.rc(
                        row, valueColumn));
                row++;
                enemyPanel.add(new JLabel(Messages.message("tax")), higConst.rc(row, labelColumn));
                enemyPanel.add(new JLabel(enemyElement.getAttribute("tax"), JLabel.TRAILING), higConst.rc(row,
                        valueColumn));
                enemyPanel.add(new JLabel("%"), higConst.rc(row, percentColumn));
                row++;
                enemyPanel.add(new JLabel(Messages.message("report.sonsOfLiberty")), higConst.rc(row, labelColumn));
                enemyPanel.add(new JLabel(enemyElement.getAttribute("SoL"), JLabel.TRAILING), higConst.rc(row,
                        valueColumn));
                enemyPanel.add(new JLabel("%"), higConst.rc(row, percentColumn));
                row++;

            }
            reportPanel.add(enemyPanel);
        }

        reportPanel.doLayout();
    }

}
