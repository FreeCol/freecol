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
import java.awt.Image;
import java.util.Collections;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Display the production of a unit.
 */
public class WorkProductionPanel extends FreeColPanel {

    private final Turn turn = getGame().getTurn();


    /**
     * Create a new production display.
     *
     * FIXME: expand display to handle several outputs
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param unit The <code>Unit</code> that is producing.
     */
    public WorkProductionPanel(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, new MigLayout("wrap 3, insets 10 10 10 10",
                                           "[]30:push[right][]", ""));

        final ImageLibrary lib = getGUI().getImageLibrary();
        final Colony colony = unit.getColony();
        final UnitType unitType = unit.getType();
        final WorkLocation wl = (WorkLocation)unit.getLocation();
        final GoodsType workType = unit.getWorkType();

        List<Modifier> attendedModifiers
            = wl.getProductionModifiers(workType, unitType);
        List<Modifier> unattendedModifiers
            = wl.getProductionModifiers(workType, null);
        String shortName = "";
        String longName = "";
        Image image = null;
        float result = wl.getBaseProduction(wl.getProductionType(), 
                                            workType, unitType);

        // FIXME: Fix OO.
        if (wl instanceof ColonyTile) {
            final ColonyTile colonyTile = (ColonyTile)wl;
            final Tile tile = colonyTile.getWorkTile();
            final TileType tileType = tile.getType();
            shortName = Messages.getName(tileType);
            longName = Messages.message(colonyTile.getLabel());
            image = getGUI().createColonyTileImage(tile, colony);

        } else if (wl instanceof Building) {
            final Building building = (Building)wl;
            shortName = Messages.getName(building.getType());
            longName = shortName;
            image = lib.getBuildingImage(building);

        } else {
            throw new IllegalStateException("WorkLocation OO fail.");
        }

        add(new JLabel(longName), "span, align center, wrap 30");
        add(new JLabel(new ImageIcon(image)));
        add(new UnitLabel(getFreeColClient(), unit, false, false), "wrap");
        add(new JLabel(shortName));
        add(new JLabel(ModifierFormat.format(result)));

        Collections.sort(attendedModifiers);
        output(attendedModifiers, unitType);

        result = wl.getPotentialProduction(workType, unitType);
        if (result < 0.0f) {
            add(Utility.localizedLabel("workProductionPanel.zeroThreshold"), "newline");
            add(new JLabel(ModifierFormat.format(-result)), "wrap 30");
            result = 0.0f;
        }

        Font bigFont = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
            FontLibrary.FontSize.SMALLER, Font.BOLD, lib.getScaleFactor());
        JLabel finalLabel = Utility.localizedLabel("finalResult");
        finalLabel.setFont(bigFont);
        add(finalLabel, "newline");

        JLabel finalResult = new JLabel(ModifierFormat.format(result));
        finalResult.setFont(bigFont);
        finalResult.setBorder(Utility.PRODUCTION_BORDER);
        add(finalResult, "wrap 30");

        if (wl instanceof Building) { // Unattended production also applies.
            result = wl.getBaseProduction(null, workType, null);
            if (result > 0) {
                add(Utility.localizedLabel(wl.getLabel()));
                add(new JLabel(ModifierFormat.format(result)), "wrap 30");
                Collections.sort(unattendedModifiers);
                output(unattendedModifiers, unitType);
            }
        }

        add(okButton, "newline, span, tag ok");
        setSize(getPreferredSize());
    }

    private void output(List<Modifier> modifiers, UnitType unitType) {
        for (Modifier m : modifiers) {
            JLabel[] mLabels
                = ModifierFormat.getModifierLabels(m, unitType, turn);
            for (int i = 0; i < mLabels.length; i++) {
                if (mLabels[i] == null) continue;
                if (i == 0) {
                    add(mLabels[i], "newline");
                } else {
                    add(mLabels[i]);
                }
            }
        }
    }
}
