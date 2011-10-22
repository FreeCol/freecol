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
import net.sf.freecol.common.model.Turn;
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
        try {
            FoundingFather father = getSpecification().getFoundingFather(id);
            buildDetail(father, panel);
        } catch(IllegalArgumentException e) {
            // this is not a founding father
            panel.setLayout(new MigLayout("wrap 1, align center", "align center"));
            JLabel header = localizedLabel(id + ".name");
            header.setFont(smallHeaderFont);
            panel.add(header, "align center, wrap 20");
            if (getId().equals(id)) {
                panel.add(getDefaultTextArea(Messages.message("colopedia.foundingFather.description"), 40));
            } else {
                Image image = ResourceManager.getImage(id + ".image");
                if (image != null) {
                    header.setText(Messages.message(id));
                    panel.add(new JLabel(new ImageIcon(image)));
                }
            }
        }
    }

    /**
     * Builds the details panel for the given FoundingFather.
     *
     * @param father a FoundingFather
     * @param panel the detail panel to build
     */
    public void buildDetail(FoundingFather father, JPanel panel) {
        panel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        String name = Messages.message(father.getNameKey());
        String type = Messages.message(father.getTypeKey());
        JLabel header = new JLabel(name + " (" + type + ")");
        header.setFont(smallHeaderFont);
        panel.add(header, "span, align center, wrap 40");

        Image image = getLibrary().getFoundingFatherImage(father);

        panel.add(new JLabel(new ImageIcon(image)), "top");

        StringBuilder text = new StringBuilder();
        text.append(Messages.message(father.getDescriptionKey()));
        text.append("\n\n[");
        text.append(Messages.message(father.getId() + ".birthAndDeath"));
        text.append("] ");
        text.append(Messages.message(father.getId() + ".text"));

        Turn turn = getElectionTurns().get(name);
        if (turn != null) {
            text.append("\n\n");
            text.append(Messages.message("report.continentalCongress.elected"));
            text.append(" ");
            text.append(Messages.message(turn.getLabel()));
        }

        JTextArea description = getDefaultTextArea(text.toString(), 20);
        panel.add(description, "top, growx");
    }

}
