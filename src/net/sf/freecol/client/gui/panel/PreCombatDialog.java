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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSeparator;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObjectType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;

import net.miginfocom.swing.MigLayout;

public class PreCombatDialog extends FreeColDialog<Boolean> {

    public static final Modifier BASE_DEFENCE_MODIFIER =
        new Modifier("modifiers.baseDefence", Modifier.UNKNOWN, Modifier.Type.ADDITIVE);

    public PreCombatDialog(final Canvas parent, Unit attacker, Unit defender,
                           Settlement settlement) {
        super(parent);

        CombatModel combatModel = attacker.getGame().getCombatModel();
        Set<Modifier> offence = sortModifiers(combatModel.getOffensiveModifiers(attacker, defender));
        Set<Modifier> defence;
        if (defender == null && settlement != null) {
            defence = new LinkedHashSet<Modifier>();
            defence.add(BASE_DEFENCE_MODIFIER);
            defence.addAll(sortModifiers(settlement.getFeatureContainer()
                                         .getModifierSet(Modifier.DEFENCE, attacker.getType())));
            defence.addAll(sortModifiers(settlement.getOwner().getFeatureContainer()
                                         .getModifierSet(Modifier.SETTLEMENT_DEFENCE, attacker.getType())));
            if (settlement.isCapital()) {
                defence.addAll(sortModifiers(settlement.getOwner().getFeatureContainer()
                                             .getModifierSet(Modifier.CAPITAL_DEFENCE,
                                                             attacker.getType())));
            }
        } else {
            defence = sortModifiers(combatModel.getDefensiveModifiers(attacker, defender));
        }
        final JButton okButton = new JButton();

        Action okAction = new AbstractAction(Messages.message("ok")) {
                public void actionPerformed( ActionEvent event ) {
                    setResponse( Boolean.TRUE );
                }
            };
        okButton.setAction(okAction);
        okButton.requestFocus();

        Action cancelAction = new AbstractAction(Messages.message("cancel")) {
                public void actionPerformed( ActionEvent event ) {
                    setResponse( Boolean.FALSE );
                }
            };
        final JButton cancelButton = new JButton(cancelAction);

        enterPressesWhenFocused(okButton);
        enterPressesWhenFocused(cancelButton);
        
        okButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    setResponse(Boolean.FALSE);
                }
            });

        setLayout(new MigLayout("wrap 6", "[sg label]20[sg value, right]1px[sg percent]40" 
                                + "[sg label]20[sg value, right]1px[sg percent]", ""));

        // left hand side: attacker
        // right hand side: defender
        String attackerName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                                               .addStringTemplate("%nation%", attacker.getOwner().getNationName())
                                               .addStringTemplate("%unit%", attacker.getLabel()));
        JLabel attackerLabel = new UnitLabel(attacker, parent, false, true);

        String defenderName;
        JLabel defenderLabel;
        if (defender == null) {
            defenderName = settlement.getName();
            defenderLabel = new JLabel(parent.getImageIcon(settlement, false));
        } else {
            defenderName = Messages.message(StringTemplate.template("model.unit.nationUnit")
                                            .addStringTemplate("%nation%", defender.getOwner().getNationName())
                                            .addStringTemplate("%unit%", defender.getLabel()));
            defenderLabel = new UnitLabel(defender, parent, false, true);
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
        JLabel finalDefenceResult = new JLabel(getModifierFormat().format(defenceResult));
        if (defenceResult == Modifier.UNKNOWN) {
            finalDefenceResult.setText("???");
        }
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
        FreeColGameObjectType source = modifier.getSource();
        String sourceName = "???";
        if (source != null) {
            sourceName = Messages.message(source.getNameKey());
        }
        add(new JLabel(sourceName), constraint);
        String bonus = getModifierFormat().format(modifier.getValue());
        boolean percent = false;
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
            percent = true;
            break;
        case MULTIPLICATIVE:
            bonus = "\u00D7" + bonus;
            break;
        default:
        }                
        add(new JLabel(bonus));
        if (percent) {
            add(new JLabel("%"));
        }
        return percent;
    }


}