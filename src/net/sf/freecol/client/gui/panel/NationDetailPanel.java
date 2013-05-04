/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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

import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;


/**
 * This panel displays the Colopedia.
 */
public class NationDetailPanel extends ColopediaGameObjectTypePanel<Nation> {

    /**
     * Creates a new instance of this ColopediaDetailPanel.
     * @param freeColClient 
     * @param gui 
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public NationDetailPanel(FreeColClient freeColClient, GUI gui, ColopediaPanel colopediaPanel) {
        super(freeColClient, gui, colopediaPanel, PanelType.NATIONS.toString(), 0.5);
    }

    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        List<Nation> nations = new ArrayList<Nation>();
        nations.addAll(getSpecification().getEuropeanNations());
        nations.addAll(getSpecification().getIndianNations());
        super.addSubTrees(root, nations);
    }

    /**
     * Builds the details panel for the Nation with the given identifier.
     *
     * @param id The object identifier.
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        Nation nation = getSpecification().getNation(id);
        NationType currentNationType = nation.getType();
        for (Player player : getGame().getPlayers()) {
            if (player.getNation() == nation) {
                currentNationType = player.getNationType();
                break;
            }
        }

        panel.setLayout(new MigLayout("wrap 3, fillx, gapx 20", "", ""));

        JLabel name = localizedLabel(nation.getNameKey());
        name.setFont(smallHeaderFont);
        panel.add(name, "span, align center, wrap 40");

        JLabel artLabel = new JLabel(getLibrary().getMonarchImageIcon(nation));
        panel.add(artLabel, "spany, gap 40, top");

        panel.add(localizedLabel("colopedia.nation.ruler"));
        panel.add(localizedLabel(nation.getRulerNameKey()));

        panel.add(localizedLabel("colopedia.nation.defaultAdvantage"));
        panel.add(getButton(nation.getType()));

        panel.add(localizedLabel("colopedia.nation.currentAdvantage"));
        panel.add(getButton(currentNationType), "wrap push");
    }


}
