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

package net.sf.freecol.client.gui.panel;

import java.awt.Font;
import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.*;
import net.sf.freecol.client.gui.label.UnitLabel;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Modifier;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.WorkLocation;
import static net.sf.freecol.common.util.CollectionUtils.*;


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
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param unit The {@code Unit} that is producing.
     */
    public WorkProductionPanel(FreeColClient freeColClient, Unit unit) {
        super(freeColClient, null,
              new MigLayout("wrap 3, insets 10 10 10 10",
                            "[]30:push[right][]", ""));

        final ImageLibrary lib = getGUI().getImageLibrary();
        final Colony colony = unit.getColony();
        final UnitType unitType = unit.getType();
        final WorkLocation wl = (WorkLocation)unit.getLocation();
        final GoodsType workType = unit.getWorkType();

        String shortName = "";
        String longName = "";
        Image image = null;
        float result = wl.getBaseProduction(wl.getProductionType(), 
                                            workType, unitType);

        // FIXME: Fix OO.
        final Tile tile = wl.getWorkTile();
        if (tile != null) { // ColonyTile
            final TileType tileType = tile.getType();
            shortName = Messages.getName(tileType);
            longName = Messages.message(wl.getLabel());
            image = getGUI().createColonyTileImage(tile, colony);

        } else { // Building
            final Building building = (Building)wl;
            shortName = Messages.getName(building.getType());
            longName = shortName;
            image = lib.getScaledBuildingImage(building);
        }

        add(new JLabel(longName), "span, align center, wrap 30");
        add(new JLabel(new ImageIcon(image)));
        add(new UnitLabel(getFreeColClient(), unit), "wrap");
        add(new JLabel(shortName));
        add(new JLabel(ModifierFormat.format(result)));

        output(sort(wl.getProductionModifiers(workType, unitType),
                    Modifier.ascendingModifierIndexComparator),
               unitType);

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

        // Is there unattended production?
        result = wl.getBaseProduction(null, workType, null);
        if (wl instanceof Building && result > 0) {
            Font boldFont = FontLibrary.createFont(FontLibrary.FontType.NORMAL,
                FontLibrary.FontSize.TINY, Font.BOLD, lib.getScaleFactor());
            JLabel unattendedLabel = Utility
                .localizedLabel("workProductionPanel.unattendedProduction");
            unattendedLabel.setFont(boldFont);
            add(unattendedLabel, "span");
            add(Utility.localizedLabel(wl.getLabel()));
            add(new JLabel(ModifierFormat.format(result)));
            output(sort(wl.getProductionModifiers(workType, null),
                        Modifier.ascendingModifierIndexComparator),
                   unitType);
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
