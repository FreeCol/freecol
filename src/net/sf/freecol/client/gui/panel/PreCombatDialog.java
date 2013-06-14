/**
 *  Copyright (C) 2002-2013   The FreeCol Team
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
import java.awt.event.ActionEvent;
import java.util.Iterator;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.FreeColObject;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * The dialog that is shown prior to a possible combat.
 */
public class PreCombatDialog extends FreeColDialog<Boolean> {

    public PreCombatDialog(FreeColClient freeColClient,
                           FreeColGameObject attacker,
                           FreeColGameObject defender) {
        super(freeColClient);

        CombatModel combatModel = attacker.getGame().getCombatModel();
        Set<Modifier> offence = sortModifiers(combatModel
                .getOffensiveModifiers(attacker, defender));
        Set<Modifier> defence = sortModifiers(combatModel
                .getDefensiveModifiers(attacker, defender));

        setLayout(new MigLayout("wrap 6", "[sg label]20[sg value, right]1px[sg percent]40"
                                + "[sg label]20[sg value, right]1px[sg percent]", ""));

        // left hand side: attacker
        // right hand side: defender
        String attackerName;
        JLabel attackerLabel;
        String defenderName;
        JLabel defenderLabel;
        if (combatModel.combatIsAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            Unit defenderUnit = (Unit) defender;
            attackerName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                    .addStringTemplate("%nation%", attackerUnit.getOwner().getNationName())
                    .addStringTemplate("%unit%", attackerUnit.getLabel()));
            attackerLabel = new UnitLabel(freeColClient, attackerUnit,
                                          false, true);
            defenderName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                    .addStringTemplate("%nation%", defenderUnit.getOwner().getNationName())
                    .addStringTemplate("%unit%", defenderUnit.getLabel()));
            defenderLabel = new UnitLabel(freeColClient, defenderUnit,
                                          false, true);
        } else if (combatModel.combatIsSettlementAttack(attacker, defender)) {
            Unit attackerUnit = (Unit) attacker;
            Settlement settlement = (Settlement) defender;
            attackerName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                    .addStringTemplate("%nation%", attackerUnit.getOwner().getNationName())
                    .addStringTemplate("%unit%", attackerUnit.getLabel()));
            attackerLabel = new UnitLabel(freeColClient, attackerUnit,
                                          false, true);
            defenderName = settlement.getName();
            defenderLabel = new JLabel(getGUI().getImageIcon(settlement, false));
        } else {
            throw new IllegalStateException("Bogus attack");
        }

        add(new JLabel(attackerName), "span 3, align center");
        add(new JLabel(defenderName), "span 3, align center");
        add(attackerLabel, "span 3, align center");
        add(defenderLabel, "span 3, align center");
        add(new JSeparator(JSeparator.HORIZONTAL), "newline, span 3, growx");
        add(new JSeparator(JSeparator.HORIZONTAL), "span 3, growx");

        Iterator<Modifier> offenceModifiers = offence.iterator();
        Iterator<Modifier> defenceModifiers = defence.iterator();

        while (offenceModifiers.hasNext() || defenceModifiers.hasNext()) {
            int skip = 0;
            boolean hasOffence = offenceModifiers.hasNext();
            if (hasOffence) {
                if (!addModifier(offenceModifiers.next(), true, 0)) {
                    skip = 1;
                }
            } else {
                skip = 3;
            }
            if (defenceModifiers.hasNext()) {
                addModifier(defenceModifiers.next(), !hasOffence, skip);
            }
        }

        Font bigFont = getFont().deriveFont(Font.BOLD, 20f);

        float offenceResult = FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(), offence);
        JLabel finalOffenceLabel = new JLabel(Messages.message("model.source.finalResult.name"));
        finalOffenceLabel.setFont(bigFont);

        add(new JSeparator(JSeparator.HORIZONTAL), "newline, span 3, growx");
        add(new JSeparator(JSeparator.HORIZONTAL), "span 3, growx");
        add(finalOffenceLabel);
        JLabel finalOffenceResult = new JLabel(getModifierFormat().format(offenceResult));
        finalOffenceResult.setFont(bigFont);
        add(finalOffenceResult);

        float defenceResult = FeatureContainer.applyModifierSet(0, attacker.getGame().getTurn(), defence);
        JLabel finalDefenceLabel = new JLabel(Messages.message("model.source.finalResult.name"));
        finalDefenceLabel.setFont(bigFont);
        add(finalDefenceLabel, "skip");
        JLabel finalDefenceResult = (defenceResult == Modifier.UNKNOWN)
            ? new JLabel("???")
            : new JLabel(getModifierFormat().format(defenceResult));
        finalDefenceResult.setFont(bigFont);
        add(finalDefenceResult);

        add(okButton, "newline 20, span, split 2, tag ok");
        add(cancelButton, "tag cancel");

        setSize(getPreferredSize());
    }

    private boolean addModifier(Modifier modifier, boolean newline, int skip) {
        String constraint = null;
        if (newline) {
            constraint = "newline";
        }
        if (skip > 0) {
            if (constraint == null) {
                constraint = "skip " + skip;
            } else {
                constraint += ", skip " + skip;
            }
        }
        FreeColObject source = modifier.getSource();
        String sourceName = "???";
        if (source != null) {
            sourceName = Messages.getName(source);
        }
        add(new JLabel(sourceName), constraint);
        String[] bonus = getModifierStrings(modifier.getValue(), modifier.getType());
        add(new JLabel(bonus[0] + bonus[1]));
        if (bonus[2] == null) {
            return false;
        } else {
            add(new JLabel(bonus[2]));
            return true;
        }
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            setResponse(Boolean.TRUE);
        } else if (CANCEL.equals(command)) {
            setResponse(Boolean.FALSE);
        } else {
            super.actionPerformed(event);
        }
    }

}
