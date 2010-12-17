/**
 *  Copyright (C) 2002-2010  The FreeCol Team
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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.filechooser.FileFilter;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.common.option.OptionGroup;

import net.miginfocom.swing.MigLayout;

/**
 * Dialog for changing the {@link net.sf.freecol.client.ClientOptions}.
 */
public class OptionsDialog extends FreeColDialog<OptionGroup>  {

    private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());

    private OptionGroupUI ui;
    protected OptionGroup group;
    protected JButton reset = new JButton(Messages.message("reset"));
    protected JButton load = new JButton(Messages.message("load"));
    protected JButton save = new JButton(Messages.message("save"));
    private boolean editable = true;

    /**
     * The constructor that will add the items to this panel.
     * @param parent The parent of this panel.
     */
    public OptionsDialog(Canvas parent, boolean editable) {
        super(parent);
        this.editable = editable;
        setLayout(new MigLayout("wrap 1, fill", "[center]"));

        reset.setActionCommand(RESET);
        reset.addActionListener(this);

        load.setActionCommand(LOAD);
        load.addActionListener(this);

        save.setActionCommand(SAVE);
        save.addActionListener(this);

        setCancelComponent(cancelButton);

        setSize(850, 600);

    }

    protected void initialize(OptionGroup group, String header, Component component) {

        this.group = group;

        removeAll();

        // Header:
        add(getDefaultHeader(header));

        // Additional component, if any
        if (component != null) {
            add(component);
        }

        // Options:
        ui = new OptionGroupUI(group, editable);
        add(ui, "grow");

        // Buttons:
        if (editable) {
            add(okButton, "newline 20, split 5, tag ok");
            add(cancelButton);
            add(reset);
            add(load);
            add(save);
        } else {
            add(okButton, "newline 20, tag ok");
        }
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(850, 700);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    protected OptionGroupUI getOptionUI() {
        return ui;
    }

    protected void setOptionUI(OptionGroupUI ui) {
        this.ui = ui;
    }

    protected void setEditable(boolean value) {
        editable = value;
    }


    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.updateOption();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(group);
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(null);
        } else if (RESET.equals(command)) {
            ui.reset();
        } else if (SAVE.equals(command)) {
            FileFilter[] filters = new FileFilter[] {
                FreeColDialog.getFGOFileFilter(),
                FreeColDialog.getFSGFileFilter(),
                FreeColDialog.getGameOptionsFileFilter()
            };
            File saveFile = getCanvas().showSaveDialog(FreeCol.getSaveDirectory(), ".fgo", filters, "");
            if (saveFile != null) {
                ui.updateOption();
                group.save(saveFile);
            }
        } else if (LOAD.equals(command)) {
            File loadFile = getCanvas()
                .showLoadDialog(FreeCol.getSaveDirectory(),
                                new FileFilter[] {
                                    FreeColDialog.getFGOFileFilter(),
                                    FreeColDialog.getFSGFileFilter(),
                                    FreeColDialog.getGameOptionsFileFilter()
                                });
            if (loadFile != null) {
                try {
                    FileInputStream in = new FileInputStream(loadFile);
                    getGame().getSpecification().loadFragment(in);
                    in.close();
                } catch(Exception e) {
                    logger.warning("Failed to load game options from " + loadFile.getName());
                }
            }
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }
}
