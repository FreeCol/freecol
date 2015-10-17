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

import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.client.gui.option.FileOptionUI;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.option.OptionUI;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.io.FreeColSavegameFile;
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
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param frame The owner frame.
     * @param editable Whether the options may be edited.
     */
    public MapGeneratorOptionsDialog(FreeColClient freeColClient, JFrame frame,
            boolean editable) {
        super(freeColClient, frame, editable,
            freeColClient.getGame().getMapGeneratorOptions(),
            MapGeneratorOptions.getXMLElementTagName(),
            FreeColDirectories.MAP_GENERATOR_OPTIONS_FILE_NAME,
            MapGeneratorOptions.getXMLElementTagName());

        if (isEditable()) {
            loadDefaultOptions();
            // FIXME: The update should be solved by PropertyEvent.
            File mapDirectory = FreeColDirectories.getMapsDirectory();
            if (mapDirectory.isDirectory()) {
                File[] files = mapDirectory.listFiles(FreeColSavegameFile.getFileFilter());
                Arrays.sort(files, new Comparator<File>() {
                        @Override
                        public int compare(File f1, File f2) {
                            return f1.getName().compareTo(f2.getName());
                        }
                    });
                JPanel mapPanel = new JPanel();
                for (final File file : files) {
                    JButton mapButton = makeMapButton(file);
                    if (mapButton == null) continue;
                    mapButton.addActionListener((ActionEvent ae) -> {
                            updateFile(file);
                        });
                    mapPanel.add(mapButton);
                }

                JScrollPane scrollPane = new JScrollPane(mapPanel,
                    JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
                scrollPane.getVerticalScrollBar().setUnitIncrement(16);
                scrollPane.getViewport().setOpaque(false);
                // FIXME: find out how to do this properly
                scrollPane.setMinimumSize(new Dimension(400, 110));

                panel.add(scrollPane);
            }
        }
        initialize(frame);
    }


    /**
     * Update the selected map file.
     *
     * The option UI may not have been created if we just click on the
     * map, because the text field is under mapGeneratorOptions.import.
     * Hence the null tests against the OptionUIs.
     *
     * @param file The new map <code>File</code>.
     */
    private void updateFile(File file) {
        final OptionGroup mgo = getGroup();
        final OptionGroupUI mgoUI = getOptionUI();
        final GUI gui = freeColClient.getGUI();

        FileOptionUI foui = (FileOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_FILE);
        if (foui == null)
            foui = (FileOptionUI)OptionUI.getOptionUI(gui,
                mgo.getOption(MapGeneratorOptions.IMPORT_FILE), true);
        foui.setValue(file);
        
        BooleanOptionUI terrainUI = (BooleanOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_TERRAIN);
        if (terrainUI == null)
            terrainUI = (BooleanOptionUI)OptionUI.getOptionUI(gui,
                mgo.getOption(MapGeneratorOptions.IMPORT_TERRAIN), true);
        terrainUI.setValue(true);

        BooleanOptionUI bonusesUI = (BooleanOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_BONUSES);
        if (bonusesUI == null)
            bonusesUI = (BooleanOptionUI)OptionUI.getOptionUI(gui,
                mgo.getOption(MapGeneratorOptions.IMPORT_BONUSES), true);
        bonusesUI.setValue(false);

        BooleanOptionUI rumourUI = (BooleanOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_RUMOURS);
        if (rumourUI == null)
            rumourUI = (BooleanOptionUI)OptionUI.getOptionUI(gui,
                mgo.getOption(MapGeneratorOptions.IMPORT_RUMOURS), true);
        rumourUI.setValue(false);

        BooleanOptionUI settlementsUI = (BooleanOptionUI)mgoUI
            .getOptionUI(MapGeneratorOptions.IMPORT_SETTLEMENTS);
        if (settlementsUI == null)
            settlementsUI = (BooleanOptionUI)OptionUI.getOptionUI(gui,
                mgo.getOption(MapGeneratorOptions.IMPORT_SETTLEMENTS), true);
        settlementsUI.setValue(false);
    }

    /**
     * Make a map button for a given map file.
     *
     * @param file The map <code>File</code>.
     * @return A <code>JButton</code> if the map is readable, or null
     *     on failure.
     */
    private JButton makeMapButton(File file) {
        String mapName = file.getName().substring(0, file.getName()
                                                         .lastIndexOf('.'));
        JButton mapButton = Utility.localizedButton("freecol.map." + mapName);
        try {
            FreeColSavegameFile savegame = new FreeColSavegameFile(file);
            Image thumbnail = ImageIO.read(savegame
                .getInputStream(FreeColSavegameFile.THUMBNAIL_FILE));
            mapButton.setIcon(new ImageIcon(thumbnail));
            try {
                Properties properties = new Properties();
                properties.load(savegame
                    .getInputStream(FreeColSavegameFile.SAVEGAME_PROPERTIES));
                mapButton.setToolTipText(properties.getProperty("map.width")
                    + "\u00D7"
                    + properties.getProperty("map.height"));
            } catch (Exception e) {
                logger.log(Level.WARNING, "Unable to load savegame.", e);
                return null;
            }
            mapButton.setHorizontalTextPosition(JButton.CENTER);
            mapButton.setVerticalTextPosition(JButton.BOTTOM);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read thumbnail.", e);
        }
        return mapButton;
    }


    // Override OptionsDialog

    /**
     * {@inheritDoc}
     */
    @Override
    public OptionGroup getResponse() {
        OptionGroup value = super.getResponse();
        if (value != null) {
            if (isEditable()) saveDefaultOptions();
        }
        return value;
    }
}
