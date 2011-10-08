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

import java.awt.Image;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


/**
 * This panel displays the ContinentalCongress Report.
 */
public final class ReportContinentalCongressPanel extends ReportPanel {

    static final String title = Messages.message("reportCongressAction.name");

    static final String none = Messages.message("report.continentalCongress.none");

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public ReportContinentalCongressPanel(Canvas parent) {
        super(parent, title);

        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setOpaque(false);

        Player player = getMyPlayer();

        JPanel recruitingPanel = new JPanel(new MigLayout("center, wrap 1", "center"));
        if (player.getCurrentFather() == null) {
            recruitingPanel.add(new JLabel(none), "wrap 20");
        } else {
            FoundingFather father = player.getCurrentFather();
            String name = Messages.message(father.getNameKey());
            JButton button = getLinkButton(name, null, father.getId());
            button.addActionListener(this);
            recruitingPanel.add(button);
            JLabel currentFatherLabel = new JLabel(new ImageIcon(getLibrary().getFoundingFatherImage(father)));
            currentFatherLabel.setToolTipText(Messages.message(father.getDescriptionKey()));
            recruitingPanel.add(currentFatherLabel);
            GoodsType bellsType = getSpecification().getGoodsType("model.goods.bells");
            FreeColProgressBar progressBar = new FreeColProgressBar(getCanvas(), bellsType);
            int total = 0;
            for (Colony colony : player.getColonies()) {
                total += colony.getNetProductionOf(bellsType);
            }
            int bells = player.getLiberty();
            int required = player.getTotalFoundingFatherCost();
            progressBar.update(0, required, bells, total);
            recruitingPanel.add(progressBar, "wrap 20");
        }
        tabs.addTab(Messages.message("report.continentalCongress.recruiting"), null,
                    recruitingPanel, null);

        Map<FoundingFatherType, JPanel> panels =
            new EnumMap<FoundingFatherType, JPanel>(FoundingFatherType.class);
        for (FoundingFatherType type : FoundingFatherType.values()) {
            JPanel panel = new JPanel(new MigLayout("flowy", "[center]"));
            panels.put(type, panel);
            JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                     JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tabs.addTab(Messages.message(FoundingFather.getTypeKey(type)), null,
                        scrollPane, null);
        }
        for (FoundingFather father : getSpecification().getFoundingFathers()) {
            String name = Messages.message(father.getNameKey());
            JPanel panel = panels.get(father.getType());
            Image image;
            Turn turn = null;
            if (player.hasFather(father)) {
                image = getLibrary().getFoundingFatherImage(father);
                turn = player.getElectionTurn(name);
            } else {
                image = ResourceManager.getGrayscaleImage(father.getId() + ".image", 1);
            }
            panel.add(new JLabel(new ImageIcon(image)), "newline");
            JButton button = getLinkButton(name, null, father.getId());
            button.addActionListener(this);
            panel.add(button);
            if (turn != null) {
                panel.add(localizedLabel("report.continentalCongress.elected"));
                panel.add(localizedLabel(turn.getLabel()));
            }
        }

        setMainComponent(tabs);
    }




}
