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

import java.awt.Dimension;
import java.awt.Image;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This panel displays details of founding fathers in the Colopedia.
 */
public class FatherDetailPanel
    extends ColopediaGameObjectTypePanel<FoundingFather> {


    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public FatherDetailPanel(FreeColClient freeColClient,
                             ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.FATHERS.getKey());
    }


    // Implelement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubTrees(DefaultMutableTreeNode root) {
        final Specification spec = getSpecification();
        DefaultMutableTreeNode parent
            = new DefaultMutableTreeNode(new ColopediaTreeItem(this, getId(),
                    getName(), null));

        EnumMap<FoundingFatherType, List<FoundingFather>> fathersByType
            = new EnumMap<>(FoundingFatherType.class);
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            fathersByType.put(fatherType, new ArrayList<FoundingFather>());
        }
        for (FoundingFather foundingFather : spec.getFoundingFathers()) {
            fathersByType.get(foundingFather.getType()).add(foundingFather);
        }
        ImageIcon icon = new ImageIcon(ImageLibrary.getLibertyImage());
        for (FoundingFatherType fatherType : FoundingFatherType.values()) {
            String id = fatherType.getTypeKey();
            String typeName = Messages.message(id);
            DefaultMutableTreeNode node
                = new DefaultMutableTreeNode(new ColopediaTreeItem(this, id,
                        typeName, null));
            parent.add(node);
            for (FoundingFather father : fathersByType.get(fatherType)) {
                node.add(buildItem(father, icon));
            }
        }
        root.add(parent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        FoundingFather father = getSpecification().getFoundingFather(id);
        buildDetail(father, panel);
    }

    /**
     * Builds the details panel for the given FoundingFather.
     *
     * @param father The {@code FoundingFather} to build a panel for.
     * @param panel The detail panel to build.
     */
    public void buildDetail(FoundingFather father, JPanel panel) {
        panel.setLayout(new MigLayout("wrap 2, fillx, gapx 20", "", ""));

        String name = Messages.getName(father);
        String type = Messages.message(father.getTypeKey());
        String text = name + " (" + type + ")";
        JLabel header = new JLabel(text);
        header.setFont(FontLibrary.createCompatibleFont(text,
            FontLibrary.FontType.HEADER, FontLibrary.FontSize.SMALL));

        Image image = ImageLibrary.getFoundingFatherImage(father, false);
        JLabel label = new JLabel(new ImageIcon(image));

        StringTemplate template = StringTemplate.label("")
            .add(Messages.descriptionKey(father))
            .addName("\n\n[")
            .add(father.getId() + ".birthAndDeath")
            .addName("] ")
            .add(father.getId() + ".text");
        final Turn turn = getMyPlayer().getElectionTurns().get(name);
        if (turn != null) {
            template
                .addName("\n\n")
                .add("report.continentalCongress.elected")
                .addName(" ")
                .addStringTemplate(turn.getLabel());
        }

        panel.add(header, "span, align center, wrap 40");
        panel.add(label, "top");
        JTextArea description = Utility.localizedTextArea(template, 20);
        panel.add(description, "top, growx");

        Dimension hSize = header.getPreferredSize(),
            lSize = label.getPreferredSize(),
            dSize = description.getPreferredSize(), size = new Dimension();
        size.setSize(lSize.getWidth() + dSize.getWidth() + 20,
            hSize.getHeight() + lSize.getHeight() + 10);
        panel.setPreferredSize(size);            
    }
}
