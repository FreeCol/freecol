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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.ResourceType;
import net.sf.freecol.common.model.Scope;
import net.sf.freecol.common.util.Utils;


/**
 * This panel displays the Colopedia.
 */
public class ResourcesDetailPanel extends ColopediaGameObjectTypePanel<ResourceType> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     * @param freeColClient 
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public ResourcesDetailPanel(FreeColClient freeColClient, ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.RESOURCES.toString(), 0.75);
    }


    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        super.addSubTrees(root, getSpecification().getResourceTypeList());
    }

    /**
     * Builds the details panel for the ResourceType with the given ID.
     *
     * @param id the ID of a ResourceType
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        ResourceType type = getSpecification().getResourceType(id);
        panel.setLayout(new MigLayout("wrap 2", "[]20[]"));

        JLabel name = localizedLabel(type.getNameKey());
        name.setFont(smallHeaderFont);
        panel.add(name, "span, align center, wrap 40");

        Set<Modifier> modifiers = type.getFeatureContainer().getModifiers();

        panel.add(localizedLabel("colopedia.resource.bonusProduction"));
        JPanel goodsPanel = new JPanel();
        goodsPanel.setOpaque(false);
        for (Modifier modifier : modifiers) {
            String text = getModifierAsString(modifier);
            if (modifier.hasScope()) {
                List<String> scopeStrings = new ArrayList<String>();
                for (Scope scope : modifier.getScopes()) {
                    if (scope.getType() != null) {
                        FreeColGameObjectType objectType = getSpecification()
                            .getType(scope.getType());
                        scopeStrings.add(Messages.message(objectType.getNameKey()));
                    }
                }
                if (!scopeStrings.isEmpty()) {
                    text += " (" + Utils.join(", ", scopeStrings) + ")";
                }
            }

            GoodsType goodsType = getSpecification().getGoodsType(modifier.getId());
            JButton goodsButton = getGoodsButton(goodsType, text);
            goodsPanel.add(goodsButton);
        }
        panel.add(goodsPanel);

        panel.add(localizedLabel("colopedia.resource.description"), "newline 20");
        panel.add(getDefaultTextArea(Messages.message(type.getDescriptionKey()), 30), "growx");
    }

}
