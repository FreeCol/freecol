/**
 *  Copyright (C) 2002-2011  The FreeCol Team
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

import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;



/**
 * Dialog for displaying and modifying the difficulty level.
 *
 * @see OptionGroup
 */
public final class DifficultyDialog extends OptionsDialog implements ItemListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    private static final String EDIT = "EDIT";


    private JButton edit = new JButton(Messages.message("edit"));

    private String DEFAULT_LEVEL = "model.difficulty.medium";
    private String CUSTOM_LEVEL = "model.difficulty.custom";

    /**
     * We need our own copy of the specification, as the dialog is
     * used before the game has been started.
     */
    private Specification specification;

    private final JComboBox difficultyBox = new JComboBox();


    private class BoxRenderer extends FreeColComboBoxRenderer {
        public void setLabelValues(JLabel c, Object value) {
            c.setText(Messages.message((String) value));
        }
    }

    /**
     * Use this constructor to display the difficulty level of the
     * current game read-only.
     *
     * @param parent a <code>Canvas</code> value
     * @param level an <code>OptionGroup</code> value
     */
    public DifficultyDialog(GUI gui, Canvas parent, OptionGroup level) {
        super(gui, parent, false);
        difficultyBox.setRenderer(new BoxRenderer());
        specification = getSpecification();

        difficultyBox.addItem(level.getId());
        difficultyBox.setEnabled(false);

        initialize(level, Messages.message("difficulty"), difficultyBox);

    }

    /**
     * Use this constructor to allow the selection of a difficulty
     * level when starting a new game.
     *
     * @param parent The parent of this panel.
     * @param specification a <code>Specification</code> value
     */
    public DifficultyDialog(GUI gui, Canvas parent, Specification specification) {
        super(gui, parent, true);
        difficultyBox.setRenderer(new BoxRenderer());
        this.specification = specification;

        boolean customized = loadCustomOptions();

        OptionGroup group = specification.getDifficultyLevel(customized ? CUSTOM_LEVEL : DEFAULT_LEVEL);
        if (group == null) {
            // this really should not happen
            group = specification.getDifficultyLevels().get(0);
        }

        for (OptionGroup level : specification.getDifficultyLevels()) {
            String id = level.getId();
            difficultyBox.addItem(id);
        }
        difficultyBox.setSelectedItem(group.getId());

        edit.setActionCommand(EDIT);
        edit.addActionListener(this);
        edit.setEnabled(!CUSTOM_LEVEL.equals(group.getId()));
        getButtons().add(edit);

        save.setEnabled(CUSTOM_LEVEL.equals(group.getId()));

        difficultyBox.addItemListener(this);

        initialize(group, Messages.message("difficulty"), difficultyBox);

    }

    @Override
    protected boolean isGroupEditable() {
        return super.isGroupEditable() && CUSTOM_LEVEL.equals(getGroup().getId());
    }

    @Override
    public boolean isEditable() {
        return super.isEditable() && CUSTOM_LEVEL.equals(getGroup().getId());
    }

    @Override
    public String getOptionGroupId() {
        return CUSTOM_LEVEL;
    }

    /**
     * Returns this dialog's instance of the <code>Specification</code>.
     *
     * @return a <code>Specification</code> value
     */
    @Override
    public Specification getSpecification() {
        return specification;
    }

    /**
     * {@inheritDoc}
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (EDIT.equals(command)) {
            OptionGroup custom = specification.getOptionGroup(CUSTOM_LEVEL);
            custom.setValue(getGroup());
            difficultyBox.setSelectedItem(CUSTOM_LEVEL);
        } else if (LOAD.equals(command)) {
            File loadFile = getCanvas().showLoadDialog(FreeCol.getOptionsDirectory(), filters);
            if (loadFile != null) {
                load(loadFile);
                difficultyBox.setSelectedItem(CUSTOM_LEVEL);
            }
        } else {
            super.actionPerformed(event);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void itemStateChanged(ItemEvent event) {
        String id = (String) difficultyBox.getSelectedItem();
        edit.setEnabled(!CUSTOM_LEVEL.equals(id));
        save.setEnabled(CUSTOM_LEVEL.equals(id));
        updateUI(specification.getOptionGroup(id));
    }

    /**
     * Returns the default file name to save the custom difficulty
     * level.
     *
     * @return "custom.xml"
     */
    public String getDefaultFileName() {
        return "custom.xml";
    }

}
