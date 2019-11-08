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

package net.sf.freecol.client.gui.action;

import java.awt.event.ActionEvent;
import java.io.File;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * An action for starting a new game from the current map editor map.
 */
public class StartMapAction extends FreeColAction {

    public static final String id = "startMapAction";


    /**
     * Creates this action
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public StartMapAction(FreeColClient freeColClient) {
        super(freeColClient, id);
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        if (!freeColClient.isMapEditor()
            || freeColClient.getGame() == null
            || freeColClient.getGame().getMap() == null) return;
        File startFile = FreeColDirectories.getStartMapFile();
        freeColClient.getMapEditorController()
            .saveMapEditorGame(startFile);
        Game game = freeColClient.getGame();
        OptionGroup options = game.getMapGeneratorOptions();
        options.setFile(MapGeneratorOptions.IMPORT_FILE, startFile);
        File mapOptionsFile = FreeColDirectories
            .getOptionsFile(FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME);
        options.save(mapOptionsFile, null, true);
        freeColClient.getGUI().removeInGameComponents();
        freeColClient.getGUI().showNewPanel(game.getSpecification());
    }
}
