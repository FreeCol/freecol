/**
 *  Copyright (C) 2002-2012   The FreeCol Team
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
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;
import javax.xml.stream.XMLStreamReader;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.util.XMLStream;


/**
 * Dialog for changing the options of an {@link OptionGroup}.
 */
public abstract class OptionsDialog extends FreeColDialog<OptionGroup>  {

    private static final Logger logger = Logger.getLogger(OptionsDialog.class.getName());

    private static final String RESET = "RESET";
    private static final String SAVE = "SAVE";
    protected static final String LOAD = "LOAD";

    private OptionGroupUI ui;
    private OptionGroup group;
    private JButton reset = new JButton(Messages.message("reset"));
    private JButton load = new JButton(Messages.message("load"));
    protected JButton save = new JButton(Messages.message("save"));
    private JPanel optionPanel;

    private List<JButton> buttons = new ArrayList<JButton>();


    protected static final FileFilter[] filters = new FileFilter[] {
        new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory() || file.getName().endsWith(".xml");
            }
            public String getDescription() {
                return Messages.message("filter.xml");
            }
        }
    };


    /**
     * The constructor that will add the items to this panel.
     *
     * @param freeColClient The <code>FreeColClient</code> for the game.
     * @param gui The <code>GUI</code> to display on.
     * @param editable Whether the dialog is editable.
     */
    public OptionsDialog(FreeColClient freeColClient, GUI gui,
                         boolean editable) {
        super(freeColClient, gui);
        this.editable = editable;
        setLayout(new MigLayout("wrap 1, fill"));

        reset.setActionCommand(RESET);
        reset.addActionListener(this);

        load.setActionCommand(LOAD);
        load.addActionListener(this);

        save.setActionCommand(SAVE);
        save.addActionListener(this);

        buttons.add(reset);
        buttons.add(load);
        buttons.add(save);

        setCancelComponent(cancelButton);

        setSize(850, 600);
    }


    protected void initialize(OptionGroup group, String header, Component component) {

        this.group = group;

        removeAll();

        // Header:
        add(getDefaultHeader(header), "center");

        // Additional component, if any
        if (component != null) {
            add(component);
        }

        // Options:
        ui = new OptionGroupUI(getGUI(), group, editable);
        optionPanel = new JPanel() {
            @Override
            public String getUIClassID() {
                return "ReportPanelUI";
            }
        };
        optionPanel.setOpaque(true);
        optionPanel.add(ui);
        JScrollPane scrollPane = new JScrollPane(optionPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        add(scrollPane, "height 100%, width 100%");

        // Buttons:
        if ( isEditable() ) {
            int cells = buttons.size() + 2;
            add(okButton, "newline 20, tag ok, split " + cells);
            add(cancelButton, "tag cancel");
            for (JButton button : buttons) {
                add(button);
            }
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

    protected List<JButton> getButtons() {
        return buttons;
    }

    protected void updateUI(OptionGroup group) {
        this.group = group;
        optionPanel.removeAll();
        ui = new OptionGroupUI(getGUI(), group, editable);
        optionPanel.add(ui);
        revalidate();
        repaint();
    }

    protected OptionGroup getGroup() {
        return group;
    }

    /**
     * Returns the default name of the file to save the
     * <code>OptionGroup</code>.
     * @return String
     */
    public abstract String getDefaultFileName();

    /**
     * Returns the identifier of the <code>OptionGroup</code>.
     * @return String
     */
    public abstract String getOptionGroupId();

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event <code>ActionEvent</code>, the incoming action event
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (OK.equals(command)) {
            ui.updateOption();
            getGUI().removeFromCanvas(this);
            setResponse(group);
        } else if (CANCEL.equals(command)) {
            getGUI().removeFromCanvas(this);
            setResponse(null);
        } else if (RESET.equals(command)) {
            ui.reset();
            revalidate();
            repaint();
        } else if (SAVE.equals(command)) {
            File saveFile = getGUI().showSaveDialog(FreeColDirectories.getOptionsDirectory(), ".xml",
                                                       filters, getDefaultFileName());
            if (saveFile != null) {
                ui.updateOption();
                try {
                    group.save(saveFile);
                } catch (FileNotFoundException e) {
                    logger.log(Level.WARNING, "Save failed", e);
                    StringTemplate t = StringTemplate.template("failedToSave")
                        .addName("%name%", saveFile.getPath());
                    getGUI().showInformationMessage(t);
                }
            }
        } else if (LOAD.equals(command)) {
            File loadFile = getGUI().showLoadDialog(FreeColDirectories.getOptionsDirectory(), filters);
            if (loadFile != null) {
                load(loadFile);
            }
        } else {
            logger.warning("Invalid ActionCommand: " + command);
        }
    }

    /**
     * Load OptionGroup from given File.
     *
     * @param file a <code>File</code> value
     */
    protected void load(File file) {
        XMLStream xr = null;
        try {
            xr = new XMLStream(new FileInputStream(file));
            xr.nextTag();
            // TODO: read into group rather than specification
            OptionGroup group = new OptionGroup(getSpecification());
            group.readFromXML(xr.getXMLStreamReader());
            getSpecification().getOptionGroup(getOptionGroupId()).setValue(group);
            logger.info("Loaded custom options from file " + file.getPath());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to load OptionGroup "
                + getOptionGroupId() + " from " + file.getName(), e);
        } finally {
            if (xr != null) xr.close();
        }
    }

    /**
     * Load custom OptionGroup from default file.
     *
     * @return true if custom options were loaded
     */
    protected boolean loadCustomOptions() {
        File customFile = new File(FreeColDirectories.getOptionsDirectory(), getDefaultFileName());
        if (customFile.exists()) {
            load(customFile);
            return true;
        } else {
            return false;
        }
    }
}
