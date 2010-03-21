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
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import net.sf.freecol.client.gui.Canvas;
import net.sf.freecol.client.gui.FreeColMenuBar;
import net.sf.freecol.client.gui.action.MapControlsAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.option.OptionMapUI;
import net.sf.freecol.common.model.DifficultyLevel;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.option.AbstractOption;
import net.sf.freecol.common.option.OptionMap;

import net.miginfocom.swing.MigLayout;

/**
* Dialog for changing the {@link net.sf.freecol.common.model.DifficultyLevel}.
*/
public final class DifficultyDialog extends FreeColDialog<DifficultyLevel> implements ItemListener {

    private static final Logger logger = Logger.getLogger(DifficultyDialog.class.getName());

    private static final String RESET = "RESET";

    private OptionMapUI ui;
    private DifficultyLevel level;
    private JPanel optionPanel;

    // TODO: read these items from specification
    private static final String[] difficulties = new String[] {
        "model.difficulty.veryEasy",
        "model.difficulty.easy",
        "model.difficulty.medium",
        "model.difficulty.hard",
        "model.difficulty.veryHard",
        "model.difficulty.custom"
    };

    private static final int DEFAULT_INDEX = 2;
    private static final int CUSTOM_INDEX = difficulties.length - 1;

    private final JComboBox difficultyBox = new JComboBox();

    /**
    * The constructor that will add the items to this panel.
    * @param parent The parent of this panel.
    */
    public DifficultyDialog(Canvas parent) {
        super(parent);
        setLayout(new MigLayout("wrap 1, fill"));

        // Header:
        JLabel header = localizedLabel("gameOptions.difficultySettings.name");
        header.setFont(((Font) UIManager.get("HeaderFont")).deriveFont(0, 48));
        add(header, "center, wrap 20");

        for (String key : difficulties) {
            difficultyBox.addItem(Messages.message(key));
        }
        difficultyBox.setSelectedIndex(DEFAULT_INDEX);
        difficultyBox.addItemListener(this);
        add(difficultyBox);

        // Options:
        level = Specification.getSpecification().getDifficultyLevel(0);
        ui = new OptionMapUI(new DifficultyOptionMap(level), false);
        ui.setOpaque(false);
        optionPanel = new JPanel();
        optionPanel.setOpaque(true);
        optionPanel.add(ui);
        JScrollPane scrollPane = new JScrollPane(optionPanel,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
        add(scrollPane, "height 100%, width 100%");

        // Buttons:
        add(okButton, "newline 20, split 3, tag ok");

        JButton reset = new JButton(Messages.message("reset"));
        reset.setActionCommand(RESET);
        reset.addActionListener(this);
        reset.setMnemonic('R');
        add(reset);
        
        add(cancelButton, "tag cancel");

        //setCancelComponent(cancelButton);

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
        if (OK.equals(command)) {
            ui.unregister();
            ui.updateOption();
            getCanvas().remove(this);
            JMenuBar menuBar = getClient().getFrame().getJMenuBar();
            if (menuBar != null) {
                ((FreeColMenuBar) menuBar).reset();
            }
            setResponse(level);
                    
            // Immediately redraw the minimap if that was updated.
            MapControlsAction mca = (MapControlsAction) getClient()
                .getActionManager().getFreeColAction(MapControlsAction.id);
            if(mca.getMapControls() != null) {
                mca.getMapControls().update();                        
            }
        } else if (CANCEL.equals(command)) {
            ui.rollback();
            ui.unregister();
            getCanvas().remove(this);
            setResponse(null);
        } else if (RESET.equals(command)) {
            ui.reset();
        } else {
            logger.warning("Invalid ActionCommand: invalid number.");
        }
    }

    public void itemStateChanged(ItemEvent event) {
        int index = difficultyBox.getSelectedIndex();
        level = Specification.getSpecification().getDifficultyLevel(index);
        ui = new OptionMapUI(new DifficultyOptionMap(level), (index == CUSTOM_INDEX));
        optionPanel.removeAll();
        optionPanel.add(ui);
        revalidate();
        repaint();
    }


    private class DifficultyOptionMap extends OptionMap {

        private DifficultyLevel level;

        public DifficultyOptionMap(DifficultyLevel level) {
            super("difficultySettings");
            this.level = level;
            for (AbstractOption option: level.getOptions().values()) {
                option.setGroup("difficultySettings");
                add(option);
            }
        }

        public DifficultyLevel getLevel() {
            return level;
        }

        protected void addDefaultOptions() {
            /*
            for (AbstractOption option: level.getOptions().values()) {
                add(option);
            }
            */
        }

        protected boolean isCorrectTagName(String tagName) {
            return true;
        }
    }

}
