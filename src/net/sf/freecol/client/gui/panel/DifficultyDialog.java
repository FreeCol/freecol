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

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionGroupUI;
import net.sf.freecol.client.gui.plaf.FreeColComboBoxRenderer;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.resources.ResourceManager;

import net.miginfocom.swing.MigLayout;


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
        specification = getSpecification();
        List<OptionGroup> levels = new ArrayList<OptionGroup>(1);
        levels.add(level);
        initialize(levels);
    }

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public DifficultyDialog(Canvas parent, Specification specification) {
        super(parent, false);
        this.specification = specification;
        initialize(specification.getDifficultyLevels());

    }

    private void initialize(List<OptionGroup> levels) {

        setLayout(new MigLayout("wrap 1, fill", "[center]"));

        difficultyBox.setRenderer(new BoxRenderer());

        edit.setActionCommand(EDIT);
        edit.addActionListener(this);

        // Header:
        add(getDefaultHeader(Messages.message("difficulty")), "wrap 20");

        for (OptionGroup dLevel : levels) {
            String id = dLevel.getId();
            difficultyBox.addItem(id);
            if (DEFAULT_LEVEL.equals(id)) {
                group = dLevel;
                difficultyBox.setSelectedIndex(difficultyBox.getItemCount() - 1);
            }
        }

        if (levels.size() == 1) {
            difficultyBox.setEnabled(false);
            group = levels.get(0);
        } else {
            difficultyBox.addItemListener(this);
        }
        add(difficultyBox);

        // Options:
        OptionGroupUI ui = new OptionGroupUI(group, false);
        setOptionUI(ui);
        ui.setOpaque(false);
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
        if (levels.size() == 1) {
            add(okButton, "newline 20, tag ok");
        } else {
            add(okButton, "newline 20, split 6, tag ok");
            add(cancelButton, "tag cancel");
            add(reset);
            add(load);
            add(save);
            add(edit);
        }

        setSize(780, 540);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(780, 540);
    }

    @Override
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }

    public void initialize() {
        removeAll();

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
            custom.setValue(group);
            difficultyBox.setSelectedItem(CUSTOM_LEVEL);
        } else {
            super.actionPerformed(event);
        }
    }

    public void itemStateChanged(ItemEvent event) {
        String id = (String) difficultyBox.getSelectedItem();
        group = specification.getOptionGroup(id);
        OptionGroupUI ui = new OptionGroupUI(group, (CUSTOM_LEVEL.equals(id)));
        setOptionUI(ui);
        optionPanel.removeAll();
        optionPanel.add(ui);
        revalidate();
        repaint();
    }
}
