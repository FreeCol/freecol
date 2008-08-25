/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Unit;

import cz.autel.dmi.HIGLayout;

public class PreCombatDialog extends FreeColDialog {

    public static final Modifier BASE_DEFENCE_MODIFIER =
        new Modifier("modifiers.baseDefence", Modifier.UNKNOWN, Modifier.Type.ADDITIVE);

    public PreCombatDialog(Unit attacker, Unit defender, Settlement settlement, Canvas parent) {

        CombatModel combatModel = attacker.getGame().getCombatModel();
        Set<Modifier> offence = sortModifiers(combatModel.getOffensiveModifiers(attacker, defender));
        Set<Modifier> defence;
        if (defender == null && settlement != null) {
            defence = new LinkedHashSet<Modifier>();
            defence.add(BASE_DEFENCE_MODIFIER);
            defence.addAll(sortModifiers(settlement.getFeatureContainer()
                                         .getModifierSet("model.modifier.defence", attacker.getType())));
        } else {
            defence = sortModifiers(combatModel.getDefensiveModifiers(attacker, defender));
        }

        int numberOfModifiers = Math.max(offence.size(), defence.size());
        int extraRows = 4; // title, icon, result, buttons
        int numberOfRows = 2 * (numberOfModifiers + extraRows) - 1;

        int[] widths = {-6, 20, -8, 0, 40, -1, 20, -3, 0};
        int[] heights = new int[numberOfRows];
        int offenceLabelColumn = 1;
        int offenceValueColumn = 3;
        int offencePercentageColumn = 4;
        int defenceLabelColumn = 6;
        int defenceValueColumn = 8;
        int defencePercentageColumn = 9;

        for (int index = 1; index < numberOfRows; index += 2) {
            heights[index] = margin;
        }


        final JButton okButton = new JButton();

        Action okAction = new AbstractAction(Messages.message("ok")) {
                public void actionPerformed( ActionEvent event ) {
                    setResponse( Boolean.TRUE );
                }
            };
        okButton.setAction(okAction);
        okButton.requestFocus();

        Action  cancelAction = new AbstractAction(Messages.message("cancel")) {
                public void actionPerformed( ActionEvent event ) {
                    setResponse( Boolean.FALSE );
                }
            };
        final JButton cancelButton = new JButton(cancelAction);

        enterPressesWhenFocused(okButton);
        enterPressesWhenFocused(cancelButton);
        
        setLayout(new HIGLayout(widths, heights));

        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    setResponse(false);
                }
            });

        //Modifier modifier;
        int row = 1;

        // left hand side: attacker
        String attackerName = Messages.message("model.unit.nationUnit",
                                               "%nation%", attacker.getOwner().getNationAsString(),
                                               "%unit%", attacker.getName());
        add(new JLabel(attackerName),
            higConst.rc(row, offenceLabelColumn));
        row += 2;
        add(new UnitLabel(attacker, parent, false, true),
            higConst.rcwh(row, offenceLabelColumn, 3, 1));
        row += 2;
        for (Modifier modifier : offence) {
            FreeColGameObjectType source = modifier.getSource();
            String sourceName;
            if (source == null) {
                sourceName = "???";
            } else {
                sourceName = source.getName();
            }
            add(new JLabel(sourceName), higConst.rc(row, offenceLabelColumn));
            String bonus = getModifierFormat().format(modifier.getValue());
            switch(modifier.getType()) {
            case ADDITIVE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                break;
            case PERCENTAGE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                add(new JLabel("%"), higConst.rc(row, offencePercentageColumn));
                break;
            case MULTIPLICATIVE:
                bonus = "\u00D7" + bonus;
                break;
            default:
            }                
            add(new JLabel(bonus), higConst.rc(row, offenceValueColumn, "r"));
            row += 2;
        }
        int finalResultRow = row;

        row = 1;
        // right hand side: defender
        if (defender != null) {
            String defenderName = Messages.message("model.unit.nationUnit",
                                                   "%nation%", defender.getOwner().getNationAsString(),
                                                   "%unit%", defender.getName());
            add(new JLabel(defenderName),
                higConst.rc(row, defenceLabelColumn));
            row += 2;
            add(new UnitLabel(defender, parent, false, true),
                higConst.rcwh(row, defenceLabelColumn, 3, 1));
            row += 2;
        } else {
            String defenderName;
            if (settlement instanceof Colony) {
                defenderName = ((Colony) settlement).getName();
            } else {
                defenderName = Messages.message("indianSettlement", 
                                                "%nation%", settlement.getOwner().getNationAsString());
            }
            add(new JLabel(defenderName),
                higConst.rc(row, defenceLabelColumn));
            row += 2;
            add(new JLabel(parent.getImageIcon(settlement, false)),
                higConst.rcwh(row, defenceLabelColumn, 3, 1));
            row += 2;
        }

        for (Modifier modifier : defence) {
            FreeColGameObjectType source = modifier.getSource();
            String sourceString = null;
            if (source == null) {
                sourceString = "???";
            } else {
                sourceString = Messages.message(source.getName());
            }
            add(new JLabel(sourceString),
                higConst.rc(row, defenceLabelColumn));
            String bonus = getModifierFormat().format(modifier.getValue());
            if (modifier.getValue() == Modifier.UNKNOWN) {
                bonus = "???";
            }
            switch(modifier.getType()) {
            case ADDITIVE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                break;
            case PERCENTAGE:
                if (modifier.getValue() > 0) {
                    bonus = "+" + bonus;
                }
                add(new JLabel("%"), higConst.rc(row, defencePercentageColumn));
                break;
            case MULTIPLICATIVE:
                bonus = "\u00D7" + bonus;
                break;
            default:
            }                
            add(new JLabel(bonus), higConst.rc(row, defenceValueColumn, "r"));
            row += 2;
        }
        if (row < finalResultRow) {
            row = finalResultRow;
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 20f);

        float offenceResult = FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(), offence);
        JLabel finalOffenceLabel = new JLabel(Messages.message("modifiers.finalResult.name"));
        finalOffenceLabel.setFont(bigFont);
        add(finalOffenceLabel, higConst.rc(row, offenceLabelColumn));
        JLabel finalOffenceResult = new JLabel(getModifierFormat().format(offenceResult));
        finalOffenceResult.setFont(bigFont);
        add(finalOffenceResult, higConst.rc(row, offenceValueColumn, "r"));

        float defenceResult = FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(), defence);
        JLabel finalDefenceLabel = new JLabel(Messages.message("modifiers.finalResult.name"));
        finalDefenceLabel.setFont(bigFont);
        add(finalDefenceLabel,
            higConst.rc(row, defenceLabelColumn));
        JLabel finalDefenceResult = new JLabel(getModifierFormat().format(defenceResult));
        if (defenceResult == Modifier.UNKNOWN) {
            finalDefenceResult.setText("???");
        }
        finalDefenceResult.setFont(bigFont);
        add(finalDefenceResult, higConst.rc(row, defenceValueColumn, "r"));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, higConst.rcwh(heights.length, 1, widths.length, 1));
        setSize(getPreferredSize());

    }


}