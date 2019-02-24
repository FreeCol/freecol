/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.panel.colopedia;

import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.model.Specification;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * This panel displays details of resources in the Colopedia.
 */
public class ResourcesDetailPanel
    extends ColopediaGameObjectTypePanel<ResourceType> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
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

        final Specification spec = getSpecification();
        ResourceType type = spec.getResourceType(id);
        panel.setLayout(new MigLayout("wrap 2", "[]20[]"));

        JLabel name = Utility.localizedHeaderLabel(type, FontLibrary.FontSize.SMALL);
        panel.add(name, "span, align center, wrap 40");

        panel.add(Utility.localizedLabel("colopedia.resource.bonusProduction"));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        List<Modifier> mods = sort(type.getModifiers(),
                                   Modifier.ascendingModifierIndexComparator);
        for (Modifier modifier : mods) {
            String text = ModifierFormat.getModifierAsString(modifier);
            if (modifier.hasScope()) {
                String scopes = transform(modifier.getScopes(),
                                          isNotNull(Scope::getType),
                                          s -> Messages.getName(spec.getType(s.getType())),
                                          Collectors.joining(", "));
                if (!scopes.isEmpty()) text += " (" + scopes + ")";
            }

            GoodsType goodsType = spec.getGoodsType(modifier.getId());
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
