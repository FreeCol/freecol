/**
 *  Copyright (C) 2002-2007  The FreeCol Team
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
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileFilter;
import java.util.Properties;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JPanel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.BooleanOptionUI;
import net.sf.freecol.client.gui.option.FileOptionUI;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.common.io.FreeColSavegameFile;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.server.generator.MapGeneratorOptions;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog for changing the
 * {@link net.sf.freecol.server.generator.OptionGroup}.
 */
public final class MapGeneratorOptionsDialog extends FreeColDialog<Boolean> implements ActionListener {

    private static final Logger logger = Logger.getLogger(MapGeneratorOptionsDialog.class.getName());

    private final OptionGroupUI ui;

    /**
     * The constructor that will add the items to this panel.
     *
     * @param parent The parent of this panel.
     */
    public MapGeneratorOptionsDialog(Canvas parent, OptionGroup mgo, boolean editable) {
        super(parent);
        setLayout(new MigLayout("wrap 1"));

        ui = new OptionGroupUI(mgo, editable);

        JButton reset = new JButton(Messages.message("reset"));
        reset.setActionCommand("RESET");
        reset.addActionListener(this);
        reset.setMnemonic('R');

        setCancelComponent(cancelButton);

        setSize(750, 500);

        // Header:
        add(getDefaultHeader(mgo.getName()), "align center");

        JScrollPane scrollPane;

        if (editable) {
            JPanel mapPanel = new JPanel();
            mapPanel.setLayout(new MigLayout("", "", "[nogrid][]"));
            /*
             * TODO: The update should be solved by PropertyEvent.
             */
            File mapDirectory = new File(FreeCol.getDataDirectory(), "maps");
            if (mapDirectory.isDirectory()) {
                for (final File file : mapDirectory.listFiles(new FileFilter() {
                        public boolean accept(File file) {
                            return file.isFile() && file.getName().endsWith(".fsg");
                        }
                    })) {
                    String mapName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                    JButton mapButton = new JButton(mapName);
                    try {
                        FreeColSavegameFile savegame = new FreeColSavegameFile(file);
                        Image thumbnail = ImageIO.read(savegame.getInputStream("thumbnail.png"));
                        mapButton.setIcon(new ImageIcon(thumbnail));
                        try {
                            Properties properties = new Properties();
                            properties.load(savegame.getInputStream("savegame.properties"));
                            mapButton.setToolTipText(properties.getProperty("map.width")
                                                     + "\u00D7"
                                                     + properties.getProperty("map.height"));
                        } catch(Exception e) {
                            logger.fine("Unable to load savegame properties.");
                        }
                        mapButton.setHorizontalTextPosition(JButton.CENTER);
                        mapButton.setVerticalTextPosition(JButton.BOTTOM);
                    } catch(Exception e) {
                        logger.warning("Failed to read thumbnail.");
                    }

                    mapButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                ui.reset();
                                FileOptionUI fou = (FileOptionUI) ui.getOptionUI(MapGeneratorOptions.IMPORT_FILE);
                                fou.setValue(file);

                                ((BooleanOptionUI) ui.getOptionUI(MapGeneratorOptions.IMPORT_RUMOURS)).setValue(false);
                                ((BooleanOptionUI) ui.getOptionUI(MapGeneratorOptions.IMPORT_TERRAIN)).setValue(true);
                                ((BooleanOptionUI) ui.getOptionUI(MapGeneratorOptions.IMPORT_BONUSES)).setValue(false);
                            }
                        });
                    mapPanel.add(mapButton);
                }
            }

            // Options:
            mapPanel.add(ui, "newline 20");

            scrollPane = new JScrollPane(mapPanel,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        } else {
            scrollPane = new JScrollPane(ui,
                                         JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        }
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, "height 100%, width 100%");

        // Buttons:
        if (editable) {
            add(okButton, "newline 20, split 3, tag ok");
            add(reset);
            add(cancelButton, "tag cancel");
        } else {
            add(okButton, "newline 20, tag ok");
        }

    }

    public void requestFocus() {
        if (okButton.isEnabled()) {
            okButton.requestFocus();
        } else {
            cancelButton.requestFocus();
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(750, 500);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    /**
     * This function analyses an event and calls the right methods to take care
     * of the user's requests.
     *
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.unregister();
            ui.updateOption();
            getCanvas().remove(this);
            if (!getClient().isMapEditor()) {
                getClient().getPreGameController().sendMapGeneratorOptions();
                getClient().getCanvas().getStartGamePanel().updateMapGeneratorOptions();
            }
            setResponse(new Boolean(true));
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(new Boolean(false));
        } else if ("RESET".equals(command)) {
            ui.reset();
        } else {
            logger.warning("Invalid ActionCommand: invalid number.");
        }
    }
}
