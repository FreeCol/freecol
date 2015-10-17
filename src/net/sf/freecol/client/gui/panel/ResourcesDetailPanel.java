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
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.StringUtils.*;


/**
 * This panel displays details of resources in the Colopedia.
 */
public class ResourcesDetailPanel
    extends ColopediaGameObjectTypePanel<ResourceType> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public ResourcesDetailPanel(FreeColClient freeColClient,
                                ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.RESOURCES.getKey());
    }


    // Implement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        super.addSubTrees(root, getSpecification().getResourceTypeList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) return;

        ResourceType type = getSpecification().getResourceType(id);
        panel.setLayout(new MigLayout("wrap 2", "[]20[]"));

        JLabel name = Utility.localizedHeaderLabel(type, FontLibrary.FontSize.SMALL);
        panel.add(name, "span, align center, wrap 40");

        panel.add(Utility.localizedLabel("colopedia.resource.bonusProduction"));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        for (Modifier modifier : type.getModifiers()) {
            String text = ModifierFormat.getModifierAsString(modifier);
            if (modifier.hasScope()) {
                final Specification spec = getSpecification();
                String scopeStrings = modifier.getScopes().stream()
                    .filter(s -> s.getType() != null)
                    .map(s -> Messages.getName(spec.findType(s.getType())))
                    .collect(Collectors.joining(", "));
                if (!scopeStrings.isEmpty()) text += " (" + scopeStrings + ")";
            }

            GoodsType goodsType = getSpecification().getGoodsType(modifier.getId());
            JButton goodsButton = getGoodsButton(goodsType, text);
            goodsPanel.add(goodsButton);
        }
        panel.add(goodsPanel);

        panel.add(Utility.localizedLabel("colopedia.resource.description"),
                  "newline 20");
        panel.add(Utility.localizedTextArea(Messages.descriptionKey(type), 30),
                  "growx");
    }
}
