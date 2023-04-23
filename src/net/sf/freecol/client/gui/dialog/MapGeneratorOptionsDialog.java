/**
 *  Copyright (C) 2002-2022   The FreeCol Team
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

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.FreeCol;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.client.gui.option.FileOptionUI;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.client.gui.panel.FreeColButton;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.MapGeneratorOptions;
import net.sf.freecol.common.option.OptionGroup;


/**
 * Dialog for changing the map generator options.
 *
 * @see MapGeneratorOptions
 * @see OptionGroup
 */
public final class MapGeneratorOptionsDialog extends OptionsDialog {

    private static final Logger logger = Logger.getLogger(MapGeneratorOptionsDialog.class.getName());


    /**
     * Creates a dialog to set the map generator options.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param editable Whether the options may be edited.
     */
    public MapGeneratorOptionsDialog(FreeColClient freeColClient, JFrame frame,
                                     boolean editable) {
        super(freeColClient,
              freeColClient.getGame().getMapGeneratorOptions(),
              MapGeneratorOptions.TAG,
              FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME,
              MapGeneratorOptions.TAG,
              editable);
        
        if (isEditable()) {
            // FIXME: The update should be solved by PropertyEvent.
            
            final List<File> mapFiles = FreeColDirectories.getMapFileList();
            final JPanel mapPanel = new JPanel(new MigLayout("wrap 4, fill"));
            final JButton randomMap = new FreeColButton(Messages.message("mapGeneratorOptions.landGenerator.name"));
            randomMap.addActionListener((ActionEvent ae) -> {
                        updateFile(null);
                    });
            mapPanel.add(randomMap, "sizegroup button, grow");
            for (File f : mapFiles) {
                JButton mapButton = makeMapButton(f);
                if (mapButton == null) continue;
                mapButton.addActionListener((ActionEvent ae) -> {
                        updateFile(f);
                    });
                mapPanel.add(mapButton, "sizegroup button, grow");
            }

            final JScrollPane scrollPane = new JScrollPane(mapPanel);
            scrollPane.getViewport().setOpaque(false);
            panel.add(scrollPane, "height 80%, width 100%");
        }
        initialize(frame, List.of());
    }

    /**
     * Update the selected map file.
     *
     * @param file The new map {@code File}.
     */
    private void updateFile(File file) {
        final OptionGroupUI mgoUI = getOptionUI();

        /*
         * This needs to be selected first to initialize the OptionUIs we're
         * asking for below.
         */
        mgoUI.selectOption(MapGeneratorOptions.MAPGENERATOROPTIONS_IMPORT);
        
        mgoUI.getOptionUI(MapGeneratorOptions.IMPORT_FILE, FileOptionUI.class).setValue(file);
        mgoUI.getOptionUI(MapGeneratorOptions.IMPORT_TERRAIN, BooleanOptionUI.class).setValue(true);
        mgoUI.getOptionUI(MapGeneratorOptions.IMPORT_BONUSES, BooleanOptionUI.class).setValue(false);
        mgoUI.getOptionUI(MapGeneratorOptions.IMPORT_RUMOURS, BooleanOptionUI.class).setValue(false);
        mgoUI.getOptionUI(MapGeneratorOptions.IMPORT_SETTLEMENTS, BooleanOptionUI.class).setValue(false);
        
        if (file == null) {
            mgoUI.selectOption(MapGeneratorOptions.MAPGENERATOROPTIONS_LAND_GENERATOR);
        }
    }

    /**
     * Make a map button for a given map file.
     *
     * @param file The map {@code File}.
     * @return A {@code JButton} if the map is readable, or null
     *     on failure.
     */
    private JButton makeMapButton(File file) {
        String mapName = file.getName();
        mapName = mapName.substring(0, mapName.lastIndexOf('.'));
        JButton mapButton = null;

        FreeColSavegameFile savegame;
        try {
            savegame = new FreeColSavegameFile(file);
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Failed to make save game for: "
                + mapName, ioe);
            return null;
        }

        Image thumbnail;
        try {
            thumbnail = ImageIO.read(savegame.getThumbnailInputStream());
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Failed to read thumbnail for: "
                + mapName, ioe);
            thumbnail = null;
        }

        if (thumbnail != null) {
            mapButton = new FreeColButton(Messages.message("freecol.map." + mapName));
            mapButton.setIcon(new ImageIcon(thumbnail));
            mapButton.setHorizontalTextPosition(JButton.CENTER);
            mapButton.setVerticalTextPosition(JButton.BOTTOM);
            try {
                Properties properties = savegame.getProperties();
                mapButton.setToolTipText(properties.getProperty("map.width")
                    + "\u00D7"
                    + properties.getProperty("map.height"));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Failed to read properties for: "
                    + mapName, ioe);
            }
        }
        return mapButton;
    }


    // Override OptionsDialog

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean save(File file) {
        final String EDGE = MapGeneratorOptions.MAXIMUM_DISTANCE_TO_EDGE;
        boolean ok = false;
        try {
            int edge = getGroup().getInteger(EDGE),
                width = getGroup().getInteger(MapGeneratorOptions.MAP_WIDTH);
            if (width >= 4 * edge) {
                ok = true;
            } else {
                getGUI().showErrorPanel(StringTemplate
                    .template("mapGeneratorOptionsDialog.badWidth")
                    .addAmount("%width%", width)
                    .addAmount("%edge%", edge));
                getGroup().setInteger(EDGE, getGroup().getIntegerMinimum(EDGE));
                return false;
            }
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Options in disarray", ex);
        }           
        if (!ok) {
            getGUI().showErrorPanel(FreeCol.badFile("error.couldNotSave", file));
            return false;
        }
        return super.save(file);
    }
}
