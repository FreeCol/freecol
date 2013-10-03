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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * This dialog is used to edit an Indian settlement (map editor only).
 */
public final class EditSettlementDialog extends FreeColDialog<IndianSettlement>
    implements ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EditSettlementDialog.class.getName());

    private final IndianSettlement settlement;

    private final JTextField name;
    private final JComboBox owner;
    private final JCheckBox capital;
    private final JComboBox skill;
    private final JSpinner units;


    /**
     * Create an EditSettlementDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param settlement The <code>IndianSettlement</code> to edit.
     */
    @SuppressWarnings("unchecked") // FIXME in Java7
    public EditSettlementDialog(FreeColClient freeColClient,
                                final IndianSettlement settlement) {
        super(freeColClient);

        this.settlement = settlement;

        MigPanel panel = new MigPanel(new MigLayout("wrap 2, gapx 20"));

        name = new JTextField(settlement.getName(), 30);

        DefaultComboBoxModel nationModel = new DefaultComboBoxModel();
        for (Player player : settlement.getGame().getPlayers()) {
            if (player.isAI() && player.isIndian()) {
                nationModel.addElement(player.getNation());
            }
        }
        owner = new JComboBox(nationModel);
        owner.setSelectedItem(settlement.getOwner().getNation());
        owner.addItemListener(this);
        owner.setRenderer(new FreeColComboBoxRenderer());

        capital = new JCheckBox();
        capital.setSelected(settlement.isCapital());

        skill = new JComboBox(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
        skill.setRenderer(new FreeColComboBoxRenderer());

        int unitCount = settlement.getUnitCount();
        SpinnerNumberModel spinnerModel
            = new SpinnerNumberModel(unitCount, 1, 20, 1);
        units = new JSpinner(spinnerModel);

        panel.add(new JLabel(Messages.message("name")));
        panel.add(name);
        panel.add(new JLabel(Messages.message("nation")));
        panel.add(owner);
        panel.add(new JLabel(Messages.message("capital")));
        panel.add(capital);
        panel.add(new JLabel(Messages.message("report.indian.skillTaught")));
        panel.add(skill);
        panel.add(new JLabel(Messages.message("report.units")));
        panel.add(units);

        initialize(DialogType.QUESTION, true, panel, getGUI().getImageLibrary()
            .getImageIcon(settlement, true),
            new String[] {
                Messages.message("ok"),
                Messages.message("editor.removeSettlement"),
                Messages.message("cancel")
            });
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private DefaultComboBoxModel getSkillModel() {
        IndianNationType ownerType = (IndianNationType)
            ((Nation) owner.getSelectedItem()).getType();
        DefaultComboBoxModel skillModel = new DefaultComboBoxModel();
        for (RandomChoice<UnitType> skill : ownerType.getSkills()) {
            skillModel.addElement(skill.getObject());
        }
        return skillModel;
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    public void itemStateChanged(ItemEvent e) {
        skill.setModel(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
    }


    /**
     * {@inheritDoc}
     */
    public IndianSettlement getResponse() {
        final Specification spec = freeColClient.getGame().getSpecification();
        Object value = getValue();
        if (options[0].equals(value)) { // OK
            settlement.setName(name.getText());
            Nation newNation = (Nation) owner.getSelectedItem();
            if (newNation != settlement.getOwner().getNation()) {
                Player newPlayer = settlement.getGame()
                    .getPlayer(newNation.getId());
                // TODO: recalculate tile ownership properly, taking
                // settlement radius into account
                settlement.changeOwner(newPlayer);
                MapEditorTransformPanel.setNativePlayer(newPlayer);
            }
            if (capital.isSelected() && !settlement.isCapital()) {
                // make sure we downgrade the old capital
                for (IndianSettlement indianSettlement
                         : settlement.getOwner().getIndianSettlements()) {
                    indianSettlement.setCapital(false);
                }
                settlement.setCapital(true);
            } else if (!capital.isSelected() && settlement.isCapital()) {
                settlement.setCapital(false);
            }
            settlement.setLearnableSkill((UnitType)skill.getSelectedItem());
            int numberOfUnits = (Integer)units.getValue()
                - settlement.getUnitCount();
            if (numberOfUnits > 0) {
                UnitType brave = spec.getUnitType("model.unit.brave");
                for (int index = 0; index < numberOfUnits; index++) {
                    settlement.add(new ServerUnit(settlement.getGame(),
                            settlement, settlement.getOwner(), brave));
                }
            } else if (numberOfUnits < 0) {
                List<Unit> unitList
                    = settlement.getUnitList().subList(0, -numberOfUnits);
                for (Unit unit : unitList) {
                    unit.dispose();
                }
            }
            return settlement;

        } else if (options[1].equals(value)) {
            if (!getGUI().showModalConfirmDialog("editor.removeSettlement.text",
                                                 "ok", "cancel")) {
                return settlement;
            }
            // Dispose of units and settlement on tile
            Tile tile = settlement.getTile();
            for (Unit unit : tile.getUnitList()) {
                unit.dispose();
            }
            // TODO: improve recalculation of tile ownership
            ((ServerPlayer)settlement.getOwner())
                .csDisposeSettlement(settlement, new ChangeSet());
        }
        return null;
    }
}
