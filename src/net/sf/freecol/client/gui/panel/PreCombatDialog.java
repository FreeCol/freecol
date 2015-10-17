/**
 *  Copyright (C) 2002-2015   The FreeCol Team
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;


/**
 * The dialog that is shown prior to a possible combat.
 */
public class PreCombatDialog extends FreeColConfirmDialog {

    /**
     * Create a new pre-combat dialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param attacker The attacker <code>Unit</code>.
     * @param defender The defender (either a <code>Unit</code> or
     *     a <code>Settlement</code>).
     */
    public PreCombatDialog(FreeColClient freeColClient, JFrame frame,
            Unit attacker, FreeColGameObject defender) {
        super(freeColClient, frame);
        
        final ImageLibrary lib = freeColClient.getGUI().getImageLibrary();
        final Game game = attacker.getGame();
        final CombatModel combatModel = game.getCombatModel();
        final Turn turn = game.getTurn();
        final Set<Modifier> attackModifiers
            = combatModel.getOffensiveModifiers(attacker, defender);
        final Set<Modifier> defenceModifiers
            = combatModel.getDefensiveModifiers(attacker, defender);
        final List<Modifier> offence = new ArrayList<>(attackModifiers);
        Collections.sort(offence);
        final List<Modifier> defence = new ArrayList<>(defenceModifiers);
        Collections.sort(defence);

        MigPanel panel = new MigPanel(new MigLayout("wrap 6",
                "[sg label]20[sg value, right]1px[sg percent]40"
                + "[sg label]20[sg value, right]1px[sg percent]", ""));
        // left hand side: attacker
        // right hand side: defender
        String attackerName
            = attacker.getDescription(Unit.UnitLabelType.NATIONAL);
        JLabel attackerLabel = new UnitLabel(freeColClient, attacker,
                                             false, true);
        String defenderName = null;
        JLabel defenderLabel = null;
        if (combatModel.combatIsAttack(attacker, defender)) {
            Unit defenderUnit = (Unit)defender;
            defenderName
                = defenderUnit.getDescription(Unit.UnitLabelType.NATIONAL);
            defenderLabel = new UnitLabel(freeColClient, defenderUnit,
                                          false, true);

        } else if (combatModel.combatIsSettlementAttack(attacker, defender)) {
            Settlement settlement = (Settlement) defender;
            defenderName = settlement.getName();
            defenderLabel = new JLabel(new ImageIcon(
                lib.getSettlementImage(settlement)));

        } else {
            throw new IllegalStateException("Bogus attack");
        }

        panel.add(new JLabel(attackerName), "span 3, align center");
        panel.add(new JLabel(defenderName), "span 3, align center");
        panel.add(attackerLabel, "span 3, align center");
        panel.add(defenderLabel, "span 3, align center");
        panel.add(new JSeparator(JSeparator.HORIZONTAL),
                  "newline, span 3, growx");
        panel.add(new JSeparator(JSeparator.HORIZONTAL),
                  "span 3, growx");

        Iterator<Modifier> offenceI = offence.iterator();
        Iterator<Modifier> defenceI = defence.iterator();
        while (offenceI.hasNext() || defenceI.hasNext()) {
            int skip = 0;
            boolean hasOffence = offenceI.hasNext();
            if (hasOffence) {
                JLabel[] labels = ModifierFormat
                    .getModifierLabels(offenceI.next(), null, turn);
                skip = addLabels(panel, labels, true, 0);
            } else {
                skip = 3;
            }
            if (defenceI.hasNext()) {
                JLabel[] labels = ModifierFormat
                    .getModifierLabels(defenceI.next(), null, turn);
                addLabels(panel, labels, !hasOffence, skip);
            }
        }

        Font bigFont = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALLER, Font.BOLD, lib.getScaleFactor());
        float offenceResult
            = FeatureContainer.applyModifiers(0f, turn, attackModifiers);
        JLabel finalOffenceLabel = Utility.localizedLabel("finalResult");
        finalOffenceLabel.setFont(bigFont);
        panel.add(new JSeparator(JSeparator.HORIZONTAL),
                  "newline, span 3, growx");
        panel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, growx");
        panel.add(finalOffenceLabel);
        JLabel finalOffenceResult
            = new JLabel(ModifierFormat.format(offenceResult));
        finalOffenceResult.setFont(bigFont);
        panel.add(finalOffenceResult);

        float defenceResult
            = FeatureContainer.applyModifiers(0f, turn, defenceModifiers);
        JLabel finalDefenceLabel = Utility.localizedLabel("finalResult");
        finalDefenceLabel.setFont(bigFont);
        panel.add(finalDefenceLabel, "skip");
        JLabel finalDefenceResult
            = new JLabel(ModifierFormat.format(defenceResult));
        finalDefenceResult.setFont(bigFont);
        panel.add(finalDefenceResult);
        panel.setSize(panel.getPreferredSize());

        initializeConfirmDialog(frame, true, panel, null, "ok", "cancel");
    }

    private int addLabels(JPanel panel, JLabel[] labels, boolean newline,
                          int skip) {
        int len = labels.length;
        for (int i = 0; i < len; i++) if (labels[i] == null) len = i;
 
        String constraint = (newline) ? "newline" : null;
        if (skip > 0) {
            if (constraint == null) {
                constraint = "skip " + skip;
            } else {
                constraint += ", skip " + skip;
            }
        }
        for (int i = 0; i < len; i++) {
            if (constraint != null) {
                panel.add(labels[i], constraint);
                constraint = null;
            } else {
                panel.add(labels[i]);
            }
        }
        return 3 - len;
    }
}
