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

package net.sf.freecol.client.gui.dialog;

import java.awt.Font;
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
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.FeatureContainer;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * The dialog that is shown prior to a possible combat.
 */
public class PreCombatDialog extends FreeColConfirmDialog {

    /**
     * Create a new pre-combat dialog.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param attacker The attacker {@code Unit}.
     * @param defender The defender (either a {@code Unit} or
     *     a {@code Settlement}).
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
        final List<Modifier> offence
            = sort(attackModifiers, Modifier.ascendingModifierIndexComparator);
        final List<Modifier> defence
            = sort(defenceModifiers, Modifier.ascendingModifierIndexComparator);

        JPanel panel = new MigPanel(new MigLayout("wrap 6",
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
                lib.getScaledSettlementImage(settlement)));

        } else {
            throw new RuntimeException("Bogus attack: " + attacker
                + " v " + defender);
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
        JLabel finalLabel = Utility.localizedLabel("finalResult");
        finalLabel.setFont(bigFont);
        panel.add(new JSeparator(JSeparator.HORIZONTAL),
                  "newline, span 3, growx");
        panel.add(new JSeparator(JSeparator.HORIZONTAL), "span 3, growx");
        panel.add(finalLabel);
        JLabel finalOffenceResult
            = new JLabel(ModifierFormat.format(offenceResult));
        finalOffenceResult.setFont(bigFont);
        panel.add(finalOffenceResult);

        float defenceResult
            = FeatureContainer.applyModifiers(0f, turn, defenceModifiers);
        panel.add(finalLabel, "skip");
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
