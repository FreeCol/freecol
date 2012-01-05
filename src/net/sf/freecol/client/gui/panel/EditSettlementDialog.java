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
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.common.util.RandomChoice;

/**
 * This dialog is used to edit an Indian settlement (map editor only).
 */
public final class EditSettlementDialog extends FreeColDialog<IndianSettlement>
    implements ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EditSettlementDialog.class.getName());

    private static final String REMOVE = "REMOVE";

    private final IndianSettlement settlement;

    private final JCheckBox capital;
    private final JComboBox owner;
    private final JComboBox skill;
    private final JSpinner units;
    private final JTextField name;

    /**
     * The constructor that will add the items to this panel.
     * @param freeColClient the client
     * @param gui the GUI
     * @param settlement the IndianSettlement to edit
     */
    public EditSettlementDialog(FreeColClient freeColClient, GUI gui, final IndianSettlement settlement) {

        super(freeColClient, gui);
        this.settlement = settlement;

        okButton.addActionListener(this);

        setLayout(new MigLayout("wrap 2, gapx 20"));

        add(new JLabel(Messages.message("name")));
        name = new JTextField(settlement.getName(), 30);
        add(name);

        add(new JLabel(Messages.message("nation")));
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
        add(owner);

        add(new JLabel(Messages.message("capital")));
        capital = new JCheckBox();
        capital.setSelected(settlement.isCapital());
        add(capital);

        add(new JLabel(Messages.message("report.indian.skillTaught")));
        skill = new JComboBox(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
        skill.setRenderer(new FreeColComboBoxRenderer());
        add(skill);

        add(new JLabel(Messages.message("report.units")));
        int unitCount = settlement.getUnitCount();
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(unitCount, 1, 20, 1);
        units = new JSpinner(spinnerModel);
        add(units);

        JButton remove = new JButton(Messages.message("editor.removeSettlement"));
        remove.setActionCommand(REMOVE);
        remove.addActionListener(this);

        add(okButton, "newline 20, span, split 3, tag ok");
        add(remove, "");
        add(cancelButton, "tag cancel");

        setSize(getPreferredSize());
    }

    private DefaultComboBoxModel getSkillModel() {
        IndianNationType ownerType = (IndianNationType)
            ((Nation) owner.getSelectedItem()).getType();
        DefaultComboBoxModel skillModel = new DefaultComboBoxModel();
        for (RandomChoice<UnitType> skill : ownerType.getSkills()) {
            skillModel.addElement(skill.getObject());
        }
        return skillModel;
    }

    public void itemStateChanged(ItemEvent e) {
        skill.setModel(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            settlement.setName(name.getText());
            Nation newNation = (Nation) owner.getSelectedItem();
            if (newNation != settlement.getOwner().getNation()) {
                Player newPlayer = settlement.getGame().getPlayer(newNation.getId());
                settlement.changeOwner(newPlayer);
                MapEditorTransformPanel.setNativePlayer(newPlayer);
            }
            if (capital.isSelected() && !settlement.isCapital()) {
                // make sure we downgrade the old capital
                for (IndianSettlement indianSettlement : settlement.getOwner().getIndianSettlements()) {
                    indianSettlement.setCapital(false);
                }
                settlement.setCapital(true);
            } else if (!capital.isSelected() && settlement.isCapital()) {
                settlement.setCapital(false);
            }
            settlement.setLearnableSkill((UnitType) skill.getSelectedItem());
            int numberOfUnits = (Integer) units.getValue() - settlement.getUnitCount();
            if (numberOfUnits > 0) {
                UnitType BRAVE = getSpecification().getUnitType("model.unit.brave");
                for (int index = 0; index < numberOfUnits; index++) {
                    settlement.add(new ServerUnit(settlement.getGame(),
                                                  settlement, settlement.getOwner(), BRAVE));
                }
            } else if (numberOfUnits < 0) {
                List<Unit> unitList = settlement.getUnitList().subList(0, -numberOfUnits);
                for (Unit unit : unitList) {
                    unit.dispose();
                }
            }
        } else if (REMOVE.equals(command)) {
            if (!getGUI().showConfirmDialog("editor.removeSettlement.text",
                                            "ok", "cancel")) {
                return;
            }
            // Dispose of units and settlement on tile
            Tile t = settlement.getTile();
            for (Unit unit : t.getUnitList()) {
                unit.dispose();
            }
            t.setSettlement(null);
            settlement.dispose();
        }
        getGUI().removeFromCanvas(this);
    }


}
