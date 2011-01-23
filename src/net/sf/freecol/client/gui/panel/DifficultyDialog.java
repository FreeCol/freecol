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
import java.io.FileInputStream;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.OptionGroup;



/**
* Dialog for changing the {@link net.sf.freecol.common.model.DifficultyLevel}.
*/
public final class DifficultyDialog extends OptionsDialog implements ItemListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    private static final String EDIT = "EDIT";

    private JPanel optionPanel;

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

    public DifficultyDialog(Canvas parent, OptionGroup level) {
        super(parent, false);
        difficultyBox.setRenderer(new BoxRenderer());
        specification = getSpecification();

        difficultyBox.addItem(level.getId());
        difficultyBox.setEnabled(false);

        initialize(level, Messages.message("difficulty"), difficultyBox);

    }

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public DifficultyDialog(Canvas parent, Specification specification) {
        super(parent, true);
        difficultyBox.setRenderer(new BoxRenderer());
        this.specification = specification;

        // try to load saved custom difficulty
        File customFile = new File(FreeCol.getSaveDirectory(), getDefaultFileName());
        if (customFile.exists()) {
            load(customFile);
        }

        OptionGroup group = null;
        for (OptionGroup level : specification.getDifficultyLevels()) {
            String id = level.getId();
            difficultyBox.addItem(id);
            if (DEFAULT_LEVEL.equals(id)) {
                group = level;
                difficultyBox.setSelectedIndex(difficultyBox.getItemCount() - 1);
            }
        }

        if (group == null) {
            group = specification.getDifficultyLevels().get(0);
        }

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
        return CUSTOM_LEVEL.equals(getGroup().getId());
    }

    /**
     * This function analyses an event and calls the right methods to take
     * care of the user's requests.
     * @param event The incoming ActionEvent.
     */
    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (EDIT.equals(command)) {
            OptionGroup custom = specification.getOptionGroup(CUSTOM_LEVEL);
            custom.setValue(getGroup());
            difficultyBox.setSelectedItem(CUSTOM_LEVEL);
        } else if (LOAD.equals(command)) {
            File loadFile = getCanvas().showLoadDialog(FreeCol.getSaveDirectory(), filters);
            if (loadFile != null) {
                load(loadFile);
                difficultyBox.setSelectedItem(CUSTOM_LEVEL);
            }
        } else {
            super.actionPerformed(event);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        String id = (String) difficultyBox.getSelectedItem();
        edit.setEnabled(!CUSTOM_LEVEL.equals(id));
        save.setEnabled(CUSTOM_LEVEL.equals(id));
        updateUI(specification.getOptionGroup(id));
    }

    /**
     * Load custom difficulty level from given File.
     *
     * @param file a <code>File</code> value
     */
    private void load(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            XMLStreamReader xsr = XMLInputFactory.newInstance().createXMLStreamReader(in);
            xsr.nextTag();
            specification.getOptionGroup(CUSTOM_LEVEL).setValue(new OptionGroup(xsr));
            in.close();
        } catch(Exception e) {
            logger.warning("Failed to load custom difficulty level from " + file.getName());
        }
    }

    public String getDefaultFileName() {
        return "custom.xml";
    }

}
