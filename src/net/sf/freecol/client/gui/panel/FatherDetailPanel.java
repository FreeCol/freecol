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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.resources.ResourceManager;


/**
 * This panel displays the Colopedia.
 */
public class FatherDetailPanel extends ColopediaGameObjectTypePanel<FoundingFather> {

    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public FatherDetailPanel(ColopediaPanel colopediaPanel) {
        super(colopediaPanel, PanelType.FATHERS.toString(), 0.75);
    }



    /**
     * Adds one or several subtrees for all the objects for which this
     * ColopediaDetailPanel could build a detail panel to the given
     * root node.
     *
     * @param root a <code>DefaultMutableTreeNode</code>
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode parent =
            new DefaultMutableTreeNode(new ColopediaTreeItem(this, getId(), getName(), null));

        EnumMap<FoundingFatherType, List<FoundingFather>> fathersByType =
            new EnumMap<FoundingFatherType, List<FoundingFather>>(FoundingFatherType.class);
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            fathersByType.put(fatherType, new ArrayList<FoundingFather>());
        }
        for (FoundingFather foundingFather : getSpecification().getFoundingFathers()) {
            fathersByType.get(foundingFather.getType()).add(foundingFather);
        }
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            String id = FoundingFather.getTypeKey(fatherType);
            String typeName = Messages.message(id);
            DefaultMutableTreeNode node =
                new DefaultMutableTreeNode(new ColopediaTreeItem(this, id, typeName, null));

            parent.add(node);
            for (FoundingFather father : fathersByType.get(fatherType)) {
                ImageIcon icon = new ImageIcon(ResourceManager.getImage("model.goods.bells.image", getScale()));
                node.add(buildItem(father, icon));
            }
        }
        root.add(parent);
    }

    /**
     * Builds the details panel for the FoundingFather with the given ID.
     *
     * @param id the ID of a FoundingFather
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        FoundingFather father = getSpecification().getFoundingFather(id);
        panel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        JLabel name = new JLabel(Messages.message(father.getNameKey())
                                 + " (" + Messages.message(father.getTypeKey()) + ")");
        name.setFont(smallHeaderFont);
        panel.add(name, "span, align center, wrap 40");

        Image image = getLibrary().getFoundingFatherImage(father);

        JLabel imageLabel;
        if (image != null) {
            imageLabel = new JLabel(new ImageIcon(image));
        } else {
            imageLabel = new JLabel();
        }
        panel.add(imageLabel, "top");

        String text = Messages.message(father.getDescriptionKey()) + "\n\n" + "["
            + Messages.message(father.getId() + ".birthAndDeath") + "] "
            + Messages.message(father.getId() + ".text");
        JTextArea description = getDefaultTextArea(text, 20);
        panel.add(description, "top, growx");
    }

}
