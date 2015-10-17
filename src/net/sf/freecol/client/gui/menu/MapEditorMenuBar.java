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

package net.sf.freecol.client.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Logger;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.action.*;
import net.sf.freecol.client.gui.action.DisplayTileTextAction.DisplayText;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * The menu bar used when running in editor mode.
 *
 * <br><br>
 *
 * The menu bar that is displayed on the top left corner of the
 * <code>Canvas</code>.
 *
 * @see InGameMenuBar
 */
public class MapEditorMenuBar extends FreeColMenuBar {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(MapEditorMenuBar.class.getName());


    /**
     * Creates a new <code>MapEditorMenuBar</code>. This menu bar will include
     * all of the submenus and items.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param listener An optional mouse motion listener.
     */
    public MapEditorMenuBar(final FreeColClient freeColClient, MouseMotionListener listener) {
        super(freeColClient);

        // Add a mouse listener so that autoscrolling can happen in this menubar
        this.addMouseMotionListener(listener);
        reset();
    }


    /**
     * Resets this menu bar.
     */
    @Override
    public final void reset() {
        removeAll();

        buildGameMenu();
        buildViewMenu();
        buildToolsMenu();
        buildColopediaMenu();

        update();
    }

    private void buildGameMenu() {
        // --> Game
        JMenu menu = Utility.localizedMenu("menuBar.game");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_G);

        menu.add(getMenuItem(NewAction.id));
        menu.add(getMenuItem(NewEmptyMapAction.id));

        menu.addSeparator();

        menu.add(getMenuItem(OpenAction.id));
        menu.add(getMenuItem(SaveAction.id));
        JMenuItem playItem = Utility.localizedMenuItem("startGame");
        playItem.addActionListener((ActionEvent ae) -> {
                File startFile = FreeColDirectories.getStartMapFile();
                freeColClient.getMapEditorController().saveGame(startFile);
                OptionGroup options = freeColClient.getGame()
                    .getMapGeneratorOptions();
                FileOption fileOption = (FileOption)options
                    .getOption(MapGeneratorOptions.IMPORT_FILE);
                fileOption.setValue(startFile);
                File mapOptionsFile = FreeColDirectories
                    .getOptionsFile(FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME);
                try {
                    options.save(mapOptionsFile);
                } catch (FileNotFoundException fnfe) {}
                freeColClient.newGame(true);
            });
        menu.add(playItem);
        menu.addSeparator();

        menu.add(getMenuItem(PreferencesAction.id));

        menu.addSeparator();

        menu.add(getMenuItem(ShowMainAction.id));
        menu.add(getMenuItem(QuitAction.id));

        add(menu);
    }

    private void buildViewMenu() {
        // --> View
        JMenu menu = Utility.localizedMenu("menuBar.view");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_V);

        menu.add(getCheckBoxMenuItem(MapControlsAction.id));
        menu.add(getCheckBoxMenuItem(DisplayGridAction.id));
        menu.add(getCheckBoxMenuItem(ChangeWindowedModeAction.id));

        menu.addSeparator();
        ButtonGroup tileTextGroup = new ButtonGroup();
        for (DisplayText type : DisplayText.values()) {
            menu.add(getRadioButtonMenuItem(DisplayTileTextAction.id + type.getKey(),
                                            tileTextGroup));
        }

        menu.addSeparator();
        menu.add(getMenuItem(ZoomInAction.id));
        menu.add(getMenuItem(ZoomOutAction.id));

        add(menu);
    }

    private void buildToolsMenu() {
        // --> Tools
        JMenu menu = Utility.localizedMenu("menuBar.tools");
        menu.setOpaque(false);
        menu.setMnemonic(KeyEvent.VK_T);

        menu.add(getMenuItem(ScaleMapAction.id));
        menu.add(getMenuItem(DetermineHighSeasAction.id));

        add(menu);
    }

}
