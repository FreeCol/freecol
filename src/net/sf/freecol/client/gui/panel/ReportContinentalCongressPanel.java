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

import java.awt.Image;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.debug.FreeColDebugger;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel {

    private static final String none
        = Messages.message("report.continentalCongress.none");


    /**
     * Creates the continental congress report.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     */
    public ReportContinentalCongressPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportCongressAction");

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setOpaque(false);

        final Player player = getMyPlayer();
        final FoundingFather currentFather = player.getCurrentFather();

        JPanel recruitingPanel = new MigPanel();
        recruitingPanel.setLayout(new MigLayout("center, wrap 1", "center"));
        if (currentFather == null) {
            recruitingPanel.add(new JLabel(none), "wrap 20");
        } else {
            String name = Messages.getName(currentFather);
            JButton button = Utility.getLinkButton(name, null,
                currentFather.getId());
            button.addActionListener(this);
            recruitingPanel.add(button);
            JLabel currentFatherLabel = new JLabel(new ImageIcon(
                ImageLibrary.getFoundingFatherImage(currentFather, false)));
            currentFatherLabel.setToolTipText(
                Messages.getDescription(currentFather));
            recruitingPanel.add(currentFatherLabel);
            for (GoodsType gt : getSpecification().getLibertyGoodsTypeList()) {
                int total = player.getColonies().stream()
                    .mapToInt(c -> c.getNetProductionOf(gt)).sum();
                FreeColProgressBar progressBar = new FreeColProgressBar(gt, 0,
                    player.getTotalFoundingFatherCost(), player.getLiberty(),
                    total);
                recruitingPanel.add(progressBar, "wrap 20");
            }
        }
        tabs.addTab(Messages.message("report.continentalCongress.recruiting"),
                    null, recruitingPanel, null);

        Map<FoundingFatherType, JPanel> panels
            = new EnumMap<>(FoundingFatherType.class);
        for (FoundingFatherType type : FoundingFatherType.values()) {
            JPanel panel = new MigPanel();
            panel.setLayout(new MigLayout("flowy", "[center]"));
            panels.put(type, panel);
            JScrollPane imageScrollPane = new JScrollPane(panel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tabs.addTab(Messages.message(FoundingFather.getTypeKey(type)), null,
                        imageScrollPane, null);
        }
        final int age = getGame().getAge();
        Map<String, Turn> electionTurns = getMyPlayer().getElectionTurns();
        for (FoundingFather father : getSpecification().getFoundingFathers()) {
            String name = Messages.getName(father);
            JPanel panel = panels.get(father.getType());
            Image image;
            Turn turn = null;
            if (player.hasFather(father)) {
                image = ImageLibrary.getFoundingFatherImage(father, false);
                turn = electionTurns.get(Messages.nameKey(father));
            } else {
                image = ImageLibrary.getFoundingFatherImage(father, true);
            }
            panel.add(new JLabel(new ImageIcon(image)), "newline");
            JButton button = Utility.getLinkButton(Messages.getName(father),
                                                   null, father.getId());
            button.addActionListener(this);
            panel.add(button);
            panel.add((turn != null)
                ? Utility.localizedLabel(StringTemplate
                    .template("report.continentalCongress.elected")
                    .addStringTemplate("%turn%", turn.getLabel()))
                : (father == currentFather)
                ? Utility.localizedLabel("report.continentalCongress.recruiting")
                : Utility.localizedLabel("report.continentalCongress.available"));
            if (FreeColDebugger.isInDebugMode(FreeColDebugger.DebugMode.MENUS)) {
                panel.add(new JLabel(String.valueOf(father.getWeight(age))));
            }
        }
        panels.clear();
        setMainComponent(tabs);
    }
}
