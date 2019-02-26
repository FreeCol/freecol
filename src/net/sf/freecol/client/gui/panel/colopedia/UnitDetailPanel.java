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

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;
import javax.swing.tree.DefaultMutableTreeNode;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.action.ColopediaAction.PanelType;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.UnitType;
import static net.sf.freecol.common.util.CollectionUtils.*;
import net.sf.freecol.common.util.RandomChoice;


/**
 * This panel displays details of units in the Colopedia.
 */
public class UnitDetailPanel extends ColopediaGameObjectTypePanel<UnitType> {

    /** Layout of production modifier panel. */
    private static final int MODIFIERS_PER_ROW = 5;


    /**
     * Creates a new instance of this colopedia subpanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colopediaPanel The parent ColopediaPanel.
     */
    public UnitDetailPanel(FreeColClient freeColClient,
                           ColopediaPanel colopediaPanel) {
        super(freeColClient, colopediaPanel, PanelType.UNITS.getKey());
    }


    // Implement ColopediaDetailPanel

    /**
     * {@inheritDoc}
     */
    public void addSubTrees(DefaultMutableTreeNode root) {
        super.addSubTrees(root, getId(),
            new ArrayList<UnitType>(getSpecification().getUnitTypeList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void buildDetail(String id, JPanel panel) {
        if (getId().equals(id)) return;

        final Specification spec = getSpecification();
        final UnitType type = spec.getUnitType(id);
        panel.setLayout(new MigLayout("wrap 4", "[]20[]40[]20[]"));

        JLabel name = Utility.localizedHeaderLabel(type, FontLibrary.FontSize.SMALL);
        panel.add(name, "span, align center, wrap 40");

        panel.add(Utility.localizedLabel("colopedia.unit.offensivePower"));
        panel.add(new JLabel(Integer.toString((int)type.getOffence())), "right");

        panel.add(Utility.localizedLabel("colopedia.unit.defensivePower"));
        panel.add(new JLabel(Integer.toString((int)type.getDefence())), "right");

        panel.add(Utility.localizedLabel("colopedia.unit.movement"));
        panel.add(new JLabel(String.valueOf(type.getMovement()/3)), "right");

        if (type.canCarryGoods() || type.canCarryUnits()) {
            panel.add(Utility.localizedLabel("colopedia.unit.capacity"));
            panel.add(new JLabel(Integer.toString(type.getSpace())), "right");
        }

        Player player = getMyPlayer();
        // player can be null when using the map editor
        Europe europe = (player == null) ? null : player.getEurope();

        String price = null;
        if (europe != null && europe.getUnitPrice(type) > 0) {
            price = Integer.toString(europe.getUnitPrice(type));
        } else if (type.getPrice() > 0) {
            price = Integer.toString(type.getPrice());
        }
        if (price != null) {
            panel.add(Utility.localizedLabel("colopedia.unit.price"));
            panel.add(new JLabel(price), "right");
        }


        if (type.hasSkill()) {
            panel.add(Utility.localizedLabel("colopedia.unit.skill"));
            panel.add(new JLabel(Integer.toString(type.getSkill())), "right");

            List<BuildingType> schools = transform(spec.getBuildingTypeList(),
                bt -> bt.hasAbility(Ability.TEACH) && bt.canAdd(type));
            if (!schools.isEmpty()) {
                panel.add(Utility.localizedLabel("colopedia.unit.school"), "newline");
                int count = 0;
                for (BuildingType school : schools) {
                    JButton label = getButton(school);
                    if (count > 0 && count % 3 == 0) {
                        panel.add(label, "skip");
                    } else {
                        panel.add(label);
                    }
                    count++;
                }
            }

            List<IndianNationType> nations
                = transform(spec.getIndianNationTypes(),
                            nt -> any(nt.getSkills(),
                                      matchKey(type, RandomChoice::getObject)));
            if (!nations.isEmpty()) {
                panel.add(Utility.localizedLabel("colopedia.unit.natives"), "newline");
                int count = 0;
                for (IndianNationType nation : nations) {
                    JButton label = getButton(nation);
                    if (count > 0 && count % 3 == 0) {
                        panel.add(label, "skip");
                    } else {
                        panel.add(label);
                    }
                    count++;
                }
            }

        }

        // Requires - prerequisites to build
        Map<String, Boolean> abilities = type.getRequiredAbilities();
        if (!abilities.isEmpty()) {
            panel.add(Utility.localizedLabel("colopedia.unit.requirements"), "newline, top");
            JTextPane textPane = Utility.getDefaultTextPane();
            StyledDocument doc = textPane.getStyledDocument();
            forEachMapEntry(type.getRequiredAbilities(),
                e -> appendRequiredAbility(doc, e.getKey(), e.getValue()));
            panel.add(textPane, "span, width 60%");
        }

        final Function<GoodsType, Stream<Modifier>> goodsMapper = gt ->
            type.getModifiers(gt.getId());
        List<Modifier> bonusList
            = sort(flatten(spec.getGoodsTypeList(), goodsMapper),
                   Modifier.ascendingModifierIndexComparator);
        int bonusNumber = bonusList.size();
        if (bonusNumber > 0) {
            StringTemplate template = StringTemplate
                .template("colopedia.unit.productionBonus")
                .addAmount("%number%", bonusNumber);
            panel.add(Utility.localizedLabel(template), "newline 20, top");
            JPanel productionPanel = new JPanel(new GridLayout(0, MODIFIERS_PER_ROW));
            productionPanel.setOpaque(false);
            for (Modifier productionBonus : bonusList) {
                GoodsType goodsType = spec.getGoodsType(productionBonus.getId());
                String bonus = ModifierFormat.getModifierAsString(productionBonus);
                productionPanel.add(getGoodsButton(goodsType, bonus));
            }
            panel.add(productionPanel, "span");
        }

        if (type.needsGoodsToBuild()) {
            panel.add(Utility.localizedLabel("colopedia.unit.goodsRequired"),
                            "newline 20");
            List<AbstractGoods> required = type.getRequiredGoodsList();
            AbstractGoods goods = first(required);
            if (required.size() > 1) {
                panel.add(getGoodsButton(goods.getType(), goods.getAmount()),
                                "span, split " + required.size());
                for (int index = 1; index < required.size(); index++) {
                    goods = required.get(index);
                    panel.add(getGoodsButton(goods.getType(), goods.getAmount()));
                }
            } else {
                panel.add(getGoodsButton(goods.getType(), goods.getAmount()));
            }
        }

        panel.add(Utility.localizedLabel("colopedia.unit.description"),
                  "newline 20");
        panel.add(Utility.localizedTextArea(Messages.descriptionKey(type), 30),
                  "span");
    }
}
