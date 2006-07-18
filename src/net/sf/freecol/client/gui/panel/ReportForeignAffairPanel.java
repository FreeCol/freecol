package net.sf.freecol.client.gui.panel;

import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.JLabel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Player;

/**
 * This panel displays the Foreign Affairs Report.
 */
public final class ReportForeignAffairPanel extends ReportPanel implements ActionListener {
    public static final String  COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";
    public static final String  LICENSE = "http://www.gnu.org/licenses/gpl.html";
    public static final String  REVISION = "$Revision$";
    
    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public ReportForeignAffairPanel(Canvas parent) {
        super(parent, "Foreign Affairs Report");
    }

    /**
     * Prepares this panel to be displayed.
     */
    public void initialize() {
        Player player = parent.getClient().getMyPlayer();
        Iterator opponents = parent.getClient().getGame().getEuropeanPlayers().iterator();
        // Display Panel
        reportPanel.removeAll();
        reportPanel.setLayout(new GridLayout(5, 5));
        reportPanel.add(new JLabel());
        int nation;
        for (nation = 0; nation < 4; nation++) {
            reportPanel.add(new JLabel(Player.getNationAsString(nation)));
        }
        while (opponents.hasNext()) {
            Player enemy = (Player) opponents.next();
            if (enemy.isREF()) {
                continue;
            }
            buildForeignAffairLabel(player, enemy);
            for (nation = 0; nation < 4; nation++) {
                if (nation == enemy.getNation()) {
                    reportPanel.add(new JLabel());
                } else {
                    String stance = Player.getStanceAsString(enemy.getStance(nation));
                    JLabel label = new JLabel(stance);
                    label.setVerticalAlignment(JLabel.TOP);
                    label.setVerticalTextPosition(JLabel.TOP);
                    reportPanel.add(label);
                }
            }
        }
        reportPanel.doLayout();
    }


    /**
     * 
     */
    private void buildForeignAffairLabel(Player player, Player opponent) {
        String report = "<html><p align=center><b>" +
                        opponent.getName() + "'s " +
                        opponent.getNationAsString() + "</b>";
        /** We need to get this information from the server:
        int rebels = 0;
        int tories = 0;
        Iterator colonies = opponent.getColonyIterator();
        while (colonies.hasNext()) {
            Colony colony = (Colony) colonies.next();
            int sol = (int) ((colony.getSoL() * colony.getUnitCount()) / 100f);
            rebels += sol;
            tories += (colony.getUnitCount() - sol);
        }
        report += "<p>" + Messages.message("rebels") + ":" + rebels +
                  "<p>" + Messages.message("tories") + ":" + tories;
        if (player.hasFather(FoundingFather.JAN_DE_WITT)) {
          // Foreign Affair Reports become more revealing
        }
        */
        report += "</html>";
        JLabel label;
        label = new JLabel(report);
        label.setVerticalAlignment(JLabel.TOP);
        label.setVerticalTextPosition(JLabel.TOP);
        reportPanel.add(label);
    }
}
