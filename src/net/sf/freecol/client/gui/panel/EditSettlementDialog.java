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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.IndianNationType;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Nation;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.SettlementType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.util.RandomChoice;
import net.sf.freecol.server.model.ServerUnit;


/**
 * This dialog is used to edit an Indian settlement (map editor only).
 */
public final class EditSettlementDialog extends FreeColDialog<IndianSettlement>
    implements ItemListener {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(EditSettlementDialog.class.getName());

    /** The settlement to edit. */
    private final IndianSettlement settlement;

    /** The settlement name. */
    private final JTextField name;

    /** The selected settlement owner. */
    private final JComboBox<Nation> owner;

    /** Is this settlement the capital? */
    private final JCheckBox capital;

    /** The skill to learn at the settlement. */
    private final JComboBox<UnitType> skill;

    /** The number of units. */
    private final JSpinner units;


    /**
     * Create an EditSettlementDialog.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param settlement The <code>IndianSettlement</code> to edit.
     */
    public EditSettlementDialog(FreeColClient freeColClient,
                                final IndianSettlement settlement) {
        super(freeColClient);

        this.settlement = settlement;

        this.name = new JTextField(settlement.getName(), 30);

        DefaultComboBoxModel<Nation> nationModel
            = new DefaultComboBoxModel<Nation>();
        for (Nation n : getSpecification().getIndianNations()) {
            nationModel.addElement(n);
        }
        this.owner = new JComboBox<Nation>(nationModel);
        this.owner.setSelectedItem(settlement.getOwner().getNation());
        this.owner.addItemListener(this);
        this.owner.setRenderer(new FreeColComboBoxRenderer<Nation>());

        this.capital = new JCheckBox();
        this.capital.setSelected(settlement.isCapital());

        this.skill = new JComboBox<UnitType>(getSkillModel());
        this.skill.setSelectedItem(settlement.getLearnableSkill());
        this.skill.setRenderer(new FreeColComboBoxRenderer<UnitType>());

        int unitCount = settlement.getUnitCount();
        SpinnerNumberModel spinnerModel
            = new SpinnerNumberModel(unitCount, 1, 20, 1);
        this.units = new JSpinner(spinnerModel);
        spinnerModel.setValue(unitCount);

        MigPanel panel = new MigPanel(new MigLayout("wrap 2, gapx 20"));
        panel.add(GUI.localizedLabel("name"));
        panel.add(this.name);
        panel.add(GUI.localizedLabel("nation"));
        panel.add(this.owner);
        panel.add(GUI.localizedLabel("capital"));
        panel.add(this.capital);
        panel.add(GUI.localizedLabel("report.indian.skillTaught"));
        panel.add(this.skill);
        panel.add(GUI.localizedLabel("report.units"));
        panel.add(this.units);

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
        return (Nation)this.owner.getSelectedItem();
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
        return getOwnerNationType().getSettlementType(this.capital.isSelected());
    }
        
    private int getAverageSize() {
        SettlementType t = getSettlementType();
        return (t.getMinimumSize() + t.getMaximumSize()) / 2;
    }

    private DefaultComboBoxModel<UnitType> getSkillModel() {
        IndianNationType ownerType = getOwnerNationType();
        DefaultComboBoxModel<UnitType> skillModel
            = new DefaultComboBoxModel<UnitType>();
        for (RandomChoice<UnitType> skill : ownerType.getSkills()) {
            skillModel.addElement(skill.getObject());
        }
        return skillModel;
    }


    // Interface ItemListener

    /**
     * {@inheritDoc}
     */
    public void itemStateChanged(ItemEvent e) {
        Player player = getOwnerPlayer();
        if (player != null) {
            this.name.setText((this.capital.isSelected())
                ? player.getCapitalName(null)
                : player.getSettlementName(null));
        }
        this.skill.setModel(getSkillModel());
        this.skill.setSelectedItem(settlement.getLearnableSkill());
        this.units.getModel().setValue(settlement.getUnitList().size());
    }


    // Override FreeColDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public IndianSettlement getResponse() {
        final Specification spec = freeColClient.getGame().getSpecification();
        final GUI gui = freeColClient.getGUI();
        IndianSettlement ret = null;
        Set<Tile> tiles = settlement.getOwnedTiles();
        Object value = getValue();
        if (options.get(0).equals(value)) { // OK
            settlement.setName(this.name.getText());
            Nation newNation = getOwnerNation();
            if (newNation != settlement.getOwner().getNation()) {
                Player newPlayer = getOwnerPlayer();
                // FIXME: recalculate tile ownership properly, taking
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
            if (this.capital.isSelected() && !settlement.isCapital()) {
                // make sure we downgrade the old capital
                for (IndianSettlement indianSettlement
                         : settlement.getOwner().getIndianSettlements()) {
                    indianSettlement.setCapital(false);
                }
                settlement.setCapital(true);
            } else if (!this.capital.isSelected() && settlement.isCapital()) {
                settlement.setCapital(false);
            }
            settlement.setLearnableSkill((UnitType)this.skill.getSelectedItem());
            int numberOfUnits = (Integer)this.units.getValue()
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
            if (!getGUI().confirm("editor.removeSettlement.text", 
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
