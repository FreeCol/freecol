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

import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.Player;


/**
 * This panel displays details of nations in the Colopedia.
 */
public class NationDetailPanel extends ColopediaGameObjectTypePanel<Nation> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public NationDetailPanel(FreeColClient freeColClient,
                             ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.NATIONS.getKey());
    }


    // Implement ColopediaDetailPanel
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        List<Nation> nations = new ArrayList<>();
        nations.addAll(getSpecification().getEuropeanNations());
        nations.addAll(getSpecification().getIndianNations());
        super.addSubTrees(root, nations);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) return;

        Nation nation = getSpecification().getNation(id);
        NationType currentNationType = nation.getType();
        for (Player player : getGame().getLivePlayers(null)) {
            if (player.getNation() == nation) {
                currentNationType = player.getNationType();
                break;
            }
        }

        panel.setLayout(new MigLayout("wrap 3, fillx, gapx 20", "", ""));

        JLabel name = Utility.localizedHeaderLabel(nation, FontLibrary.FontSize.SMALL);
        panel.add(name, "span, align center, wrap 40");

        JLabel artLabel = new JLabel(new ImageIcon(ImageLibrary.getMonarchImage(nation)));
        panel.add(artLabel, "spany, gap 40, top");

        panel.add(Utility.localizedLabel("colopedia.nation.ruler"));
        panel.add(new JLabel(nation.getRulerName()));

        panel.add(Utility.localizedLabel("colopedia.nation.defaultAdvantage"));
        panel.add(getButton(nation.getType()));

        panel.add(Utility.localizedLabel("colopedia.nation.currentAdvantage"));
        panel.add(getButton(currentNationType), "wrap push");
    }
}
