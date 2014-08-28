/**
 *  Copyright (C) 2002-2014   The FreeCol Team
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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Settlement;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.server.control.ChangeSet;
import net.sf.freecol.server.model.ServerPlayer;

import net.miginfocom.swing.MigLayout;


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
        for (Nation n : getSpecification().getIndianNations()) {
            nationModel.addElement(n.getId());
        }
        owner = new JComboBox(nationModel);
        owner.setSelectedItem(settlement.getOwner().getNation().getId());
        owner.addItemListener(this);
        owner.setRenderer(new DefaultListCellRenderer() {
                public Component getListCellRendererComponent(JList list,
                    Object value, int index, boolean selected, boolean focus) {
                    Component ret = super.getListCellRendererComponent(list,
                        value, index, selected, focus);
                    String name = Messages.getName((String)value);
                    setText(name);
                    return ret;
                }
            });

        capital = new JCheckBox();
        capital.setSelected(settlement.isCapital());

        skill = new JComboBox(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
        skill.setRenderer(new FreeColComboBoxRenderer());

        int unitCount = settlement.getUnitCount();
        SpinnerNumberModel spinnerModel
            = new SpinnerNumberModel(unitCount, 1, 20, 1);
        units = new JSpinner(spinnerModel);
        spinnerModel.setValue(unitCount);

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

        final IndianSettlement fake = null;
        List<ChoiceItem<IndianSettlement>> c = choices();
        c.add(new ChoiceItem<IndianSettlement>(Messages.message("ok"), 
                settlement).okOption());
        c.add(new ChoiceItem<IndianSettlement>(Messages.message("editor.removeSettlement"),
                fake));
        c.add(new ChoiceItem<IndianSettlement>(Messages.message("cancel"),
                fake).cancelOption().defaultOption());
        initializeDialog(DialogType.QUESTION, true, panel,
            getGUI().getImageLibrary().getImageIcon(settlement, true), c);
    }

    private Nation getOwnerNation() {
        String id = (String)owner.getSelectedItem();
        return getSpecification().getNation(id);
    }

    private IndianNationType getOwnerNationType() {
        Nation n = getOwnerNation();
        return (n == null) ? null : (IndianNationType)n.getType();
    }

    private Player getOwnerPlayer() {
        Nation n = getOwnerNation();
        for (Player player : settlement.getGame().getLivePlayers(null)) {
            if (player.getNationId().equals(n.getId())) return player;
        }
        return null;
    }

    private SettlementType getSettlementType() {
        return getOwnerNationType().getSettlementType(capital.isSelected());
    }
        
    private int getAverageSize() {
        SettlementType t = getSettlementType();
        return (t.getMinimumSize() + t.getMaximumSize()) / 2;
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    private DefaultComboBoxModel getSkillModel() {
        IndianNationType ownerType = getOwnerNationType();
        DefaultComboBoxModel skillModel = new DefaultComboBoxModel();
        for (RandomChoice<UnitType> skill : ownerType.getSkills()) {
            skillModel.addElement(skill.getObject());
        }
        return skillModel;
    }

    @SuppressWarnings("unchecked") // FIXME in Java7
    public void itemStateChanged(ItemEvent e) {
        Player player = getOwnerPlayer();
        if (player != null) {
            name.setText((capital.isSelected()) ? player.getCapitalName(null)
                : player.getSettlementName(null));
        }
        skill.setModel(getSkillModel());
        skill.setSelectedItem(settlement.getLearnableSkill());
        units.getModel().setValue(settlement.getUnitList().size());
    }


    /**
     * {@inheritDoc}
     */
    public IndianSettlement getResponse() {
        final Specification spec = freeColClient.getGame().getSpecification();
        final GUI gui = freeColClient.getGUI();
        IndianSettlement ret = null;
        Set<Tile> tiles = settlement.getOwnedTiles();
        Object value = getValue();
        if (options.get(0).equals(value)) { // OK
            settlement.setName(name.getText());
            Nation newNation = getOwnerNation();
            if (newNation != settlement.getOwner().getNation()) {
                Player newPlayer = getOwnerPlayer();
                // TODO: recalculate tile ownership properly, taking
                // settlement radius into account
                settlement.setOwner(newPlayer);
                List<Unit> ul = settlement.getUnitList();
                ul.addAll(settlement.getTile().getUnitList());
                for (Unit u : ul) {
                    u.setOwner(newPlayer);
                    u.setEthnicity(newNation.getId());
                    u.setNationality(newNation.getId());
                }
                for (Tile t : settlement.getOwnedTiles()) {
                    t.setOwner(newPlayer);//-til
                }
                MapEditorTransformPanel.setNativeNation(newNation);
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
            SettlementType oldType = settlement.getType();
            SettlementType type = getSettlementType();
            settlement.setType(type);
            settlement.getFeatureContainer().replaceSource(oldType, type);
            ret = settlement;

        } else if (options.get(1).equals(value)) {
            if (!getGUI().showSimpleConfirmDialog("editor.removeSettlement.text",
                                                  "ok", "cancel")) {
                return settlement;
            }
            // Dispose of units and settlement on tile
            Tile tile = settlement.getTile();
            for (Unit unit : tile.getUnitList()) unit.dispose();
            settlement.exciseSettlement();
        }
        for (Tile t : tiles) gui.refreshTile(t);
        return ret;
    }
}
