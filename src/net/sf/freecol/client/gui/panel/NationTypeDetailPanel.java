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

import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.EuropeanNationType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.NationType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.resources.ResourceManager;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.common.util.Utils;


/**
 * This panel displays the Colopedia.
 */
public class NationTypeDetailPanel extends ColopediaGameObjectTypePanel<NationType> {

    /**
     * Creates a new instance of this ColopediaDetailPanel.
     *
     * @param colopediaPanel the ColopediaPanel
     */
    public NationTypeDetailPanel(ColopediaPanel colopediaPanel) {
        super(colopediaPanel, PanelType.NATION_TYPES.toString(), 0.75);
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

        List<NationType> nations = new ArrayList<NationType>();
        nations.addAll(getSpecification().getEuropeanNationTypes());
        nations.addAll(getSpecification().getREFNationTypes());
        nations.addAll(getSpecification().getIndianNationTypes());
        for (NationType type : nations) {
            ImageIcon icon = new ImageIcon(ResourceManager.getImage("model.goods.bells.image", getScale()));
            parent.add(buildItem(type, icon));
        }
        root.add(parent);
    }

    /**
     * Builds the details panel for the NationType with the given ID.
     *
     * @param id the ID of the NationType to display
     * @param panel the detail panel to build
     */
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) {
            return;
        }

        NationType nationType = getSpecification().getNationType(id);
        if (nationType instanceof EuropeanNationType) {
            buildEuropeanNationTypeDetail((EuropeanNationType) nationType, panel);
        } else if (nationType instanceof IndianNationType) {
            buildIndianNationTypeDetail((IndianNationType) nationType, panel);
        }
    }


    /**
     * Builds the details panel for the given nation type.
     *
     * @param nationType - the EuropeanNationType
     * @param panel the panel to use
     */
    private void buildEuropeanNationTypeDetail(EuropeanNationType nationType, JPanel panel) {

        Font boldFont = ResourceManager.getFont("SimpleFont", Font.BOLD, 16f);

        Set<Ability> abilities = nationType.getFeatureContainer().getAbilities();
        Set<Modifier> modifiers = nationType.getFeatureContainer().getModifiers();

        panel.setLayout(new MigLayout("wrap 2, gapx 20"));

        JLabel label = localizedLabel(nationType.getNameKey());
        label.setFont(smallHeaderFont);
        panel.add(label, "span, align center, wrap 40");

        label = localizedLabel("colopedia.nationType.units");
        label.setFont(boldFont);
        panel.add(label, "wrap");

        List<AbstractUnit> startingUnits = nationType.getStartingUnits();
        if (!startingUnits.isEmpty()) {
            AbstractUnit startingUnit = startingUnits.get(0);
            if (startingUnits.size() > 1) {
                panel.add(getUnitButton(startingUnit),
                          "span, split " + startingUnits.size());
                for (int index = 1; index < startingUnits.size(); index++) {
                    startingUnit = startingUnits.get(index);
                    panel.add(getUnitButton(startingUnit));
                }
            } else {
                panel.add(getUnitButton(startingUnit));
            }
        }

        if (!abilities.isEmpty()) {
            label = localizedLabel("abilities");
            label.setFont(boldFont);
            panel.add(label, "newline 20, span");
            for (Ability ability : abilities) {
                panel.add(getAbilityComponent(ability));
            }
        }

        if (!modifiers.isEmpty()) {
            label = localizedLabel("modifiers");
            label.setFont(boldFont);
            panel.add(label, "newline 20, span");
            for (Modifier modifier : modifiers) {
                panel.add(getModifierComponent(modifier));
            }
        }
    }


    /**
     * Builds the details panel for the given nation type.
     *
     * @param nationType - the IndianNationType
     * @param panel the panel to use
     */
    private void buildIndianNationTypeDetail(IndianNationType nationType, JPanel panel) {

        List<RandomChoice<UnitType>> skills = nationType.getSkills();

        panel.setLayout(new MigLayout("wrap 2, gapx 20", "", ""));

        JLabel name = localizedLabel(nationType.getNameKey());
        name.setFont(smallHeaderFont);
        panel.add(name, "span, align center, wrap 40");

        panel.add(localizedLabel("colopedia.nationType.aggression"));
        panel.add(new JLabel(Messages.message("colopedia.nationType.aggression." +
                                              nationType.getAggression().toString().toLowerCase())));

        panel.add(localizedLabel("colopedia.nationType.numberOfSettlements"));
        panel.add(new JLabel(Messages.message("colopedia.nationType.numberOfSettlements." +
                                              nationType.getNumberOfSettlements().toString()
                                              .toLowerCase())));

        panel.add(localizedLabel("colopedia.nationType.typeOfSettlements"));
        panel.add(new JLabel(Messages.message(nationType.getCapitalType().getId() + ".name"),
                             new ImageIcon(getLibrary().getSettlementImage(nationType.getCapitalType())),
                             SwingConstants.CENTER));

        List<String> regionNames = new ArrayList<String>();
        for (String regionName : nationType.getRegionNames()) {
            regionNames.add(Messages.message(regionName + ".name"));
        }
        panel.add(localizedLabel("colopedia.nationType.regions"));
        panel.add(new JLabel(Utils.join(", ", regionNames)));

        panel.add(localizedLabel("colopedia.nationType.skills"), "top, newline 20");
        GridLayout gridLayout = new GridLayout(0, 2);
        gridLayout.setHgap(10);
        JPanel unitPanel = new JPanel(gridLayout);
        unitPanel.setOpaque(false);
        for (RandomChoice<UnitType> choice : skills) {
            unitPanel.add(getUnitButton(choice.getObject()));
        }
        panel.add(unitPanel);
    }

}
