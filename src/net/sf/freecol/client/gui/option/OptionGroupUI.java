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

package net.sf.freecol.client.gui.option;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.common.option.AudioMixerOption;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.LanguageOption;
import net.sf.freecol.common.option.ListOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.PercentageOption;
import net.sf.freecol.common.option.RangeOption;
import net.sf.freecol.common.option.SelectOption;

import net.miginfocom.swing.MigLayout;

/**
 * This class provides visualization for an {@link OptionGroup}. In order to
 * enable values to be both seen and changed.
 */
public final class OptionGroupUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());




    /** The horizontal gap between components in this <code>OptionGroupUI</code>. */
    public static final int H_GAP = 10;

    private final OptionUpdater[] optionUpdaters;

    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionGroup</code>.
     * 
     * @param option The <code>OptionGroup</code> to make a user interface
     *            for.
     */
    public OptionGroupUI(OptionGroup option, boolean editable, int level, HashMap<String, JComponent> optionUIs) {
        
        setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));
        
        JPanel horizontalPanel = null;
        boolean buttonAdded = false;
        
        ArrayList<OptionUpdater> ou = new ArrayList<OptionUpdater>();
        Iterator<Option> it = option.iterator();
        while (it.hasNext()) {
            Option o = it.next();

            if (o instanceof OptionGroup) {
                if (level == 2) {
                    final OptionGroupUI groupUI = new OptionGroupUI((OptionGroup) o, editable, 1, optionUIs);
                    final OptionGroupButton ogb = new OptionGroupButton(o.getName(), groupUI);
                    ou.add(ogb);
                    if ((horizontalPanel == null) || !buttonAdded) {
                        horizontalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        horizontalPanel.setOpaque(false);
                        add(horizontalPanel, "newline, span");
                    }
                    horizontalPanel.add(ogb);
                    buttonAdded = true;
                } else {
                    final OptionGroupUI groupUI = new OptionGroupUI((OptionGroup) o, editable, level+1, optionUIs);
                    add(groupUI, "newline, span");
                    ou.add(groupUI);
                    buttonAdded = false;
                }
            } else if (o instanceof BooleanOption) {                
                final BooleanOptionUI boi = new BooleanOptionUI((BooleanOption) o, editable);
                ou.add(boi);
                if (boi.getLabel().length() > 40) {
                    add(boi, "newline, span");
                } else {
                    add(boi, "span 2");
                }
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), boi);
                }
            } else if (o instanceof PercentageOption) {
                final PercentageOptionUI soi = new PercentageOptionUI((PercentageOption) o, editable);
                add(soi, "newline, span");
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof ListOption) {
                @SuppressWarnings("unchecked")
                final ListOptionUI soi = new ListOptionUI((ListOption) o, editable);
                add(soi);
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof IntegerOption) {
                final IntegerOptionUI iou = new IntegerOptionUI((IntegerOption) o, editable);
                if (iou.getLabel().getText().length() > 30) {
                    add(iou.getLabel(), "newline, span 3, right");
                } else {
                    add(iou.getLabel(), "right");
                }
                add(iou);
                ou.add(iou);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), iou);
                }
            } else if (o instanceof FileOption) {
                final FileOptionUI iou = new FileOptionUI((FileOption) o, editable);
                add(iou, "newline, span");
                ou.add(iou);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), iou);
                }
            } else if (o instanceof RangeOption) {
                final RangeOptionUI soi = new RangeOptionUI((RangeOption) o, editable);
                add(soi, "newline, span");
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof SelectOption) {
                final SelectOptionUI soi = new SelectOptionUI((SelectOption) o, editable);
                if (soi.getLabel().getText().length() > 30) {
                    add(soi.getLabel(), "newline, span 3, right");
                } else {
                    add(soi.getLabel(), "right");
                }
                add(soi);
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof LanguageOption) {
                final LanguageOptionUI soi = new LanguageOptionUI((LanguageOption) o, editable);
                add(soi, "span 2");
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof AudioMixerOption) {
                final AudioMixerOptionUI soi = new AudioMixerOptionUI((AudioMixerOption) o, editable);
                add(soi, "newline, span");
                ou.add(soi);
                buttonAdded = false;
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), soi);
                }
            } else if (o instanceof FreeColAction) {
                final FreeColActionUI fau = new FreeColActionUI((FreeColAction) o, this);
                ou.add(fau);
                add(fau, "newline, span");
                if (!o.getId().equals(Option.NO_ID)) {
                    optionUIs.put(o.getId(), fau);
                }
            } else {
                logger.warning("Unknown option.");
            }
        }
        optionUpdaters = ou.toArray(new OptionUpdater[0]);

        setBorder(BorderFactory.createTitledBorder(option.getName()));
        setOpaque(false);
    }

    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        for (int i = 0; i < optionUpdaters.length; i++) {
            optionUpdaters[i].rollback();
        }
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        for (int i = 0; i < optionUpdaters.length; i++) {
            optionUpdaters[i].unregister();
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        for (int i = 0; i < optionUpdaters.length; i++) {
            optionUpdaters[i].updateOption();
        }
    }
    
    /**
     * Reset with the value from the option.
     */
    public void reset() {
        for (int i = 0; i < optionUpdaters.length; i++) {
            optionUpdaters[i].reset();
        }
    }

    /**
     * Removes the given <code>KeyStroke</code> from all of this
     * <code>OptionGroupUI</code>'s children.
     * 
     * @param keyStroke The <code>KeyStroke</code> to be removed.
     */
    public void removeKeyStroke(KeyStroke keyStroke) {
        for (int i = 0; i < optionUpdaters.length; i++) {
            if (optionUpdaters[i] instanceof FreeColActionUI) {
                ((FreeColActionUI) optionUpdaters[i]).removeKeyStroke(keyStroke);
            }
        }
    }

    /**
     * A button for displaying an <code>OptionGroupUI</code>. The 
     * <code>OptionGroupUI</code> is displayed inside a panel when
     * the button is clicked.
     */
    private class OptionGroupButton extends JButton implements OptionUpdater {
        
        private final OptionGroupUI groupUI;
        private final OptionGroupButton optionGroupButton;
        private final OptionGroupPanel optionGroupPanel;
        
        /**
         * Creates a new button.
         * 
         * @param name The title on the button.
         * @param groupUI The <code>OptionGroupUI</code> to be displayed when
         *      the button is clicked.
         */
        OptionGroupButton(final String name, final OptionGroupUI groupUI) {
            super(name);
            
            this.groupUI = groupUI;
            optionGroupButton = this;
            optionGroupPanel = new OptionGroupPanel();
            
            addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent e) {
                   optionGroupButton.setEnabled(false);
                   FreeCol.getFreeColClient().getCanvas().addAsFrame(optionGroupPanel);
               }                
            });
        }
        
        /**
         * Rollback to the original value.
         * 
         * This method gets called so that changes made to options with
         * {@link Option#isPreviewEnabled()} is rolled back
         * when an option dialoag has been cancelled.
         */
        public void rollback() {
            groupUI.rollback();
        }
        
        /**
         * Delegates the call to <code>groupUI</code>.
         */
        public void updateOption() {
            groupUI.updateOption();
        }
        
        /**
         * Reset with the value from the option.
         */
        public void reset() {
            groupUI.reset();
        }
        
        /**
         * Unregister <code>PropertyChangeListener</code>s and closes
         * the subpanel.
         */
        public void unregister() {
            groupUI.unregister();
            FreeCol.getFreeColClient().getCanvas().remove(optionGroupPanel);
        }
        
        /**
         * Panel for displaying the <code>groupUI</code>.
         */        
        private class OptionGroupPanel extends FreeColPanel {
            public OptionGroupPanel() {
                super(FreeCol.getFreeColClient().getCanvas(), new BorderLayout());
                
                JButton button = new JButton(Messages.message("ok"));
                button.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        FreeCol.getFreeColClient().getCanvas().remove(optionGroupPanel);
                        optionGroupButton.setEnabled(true);
                    }
                });
                
                add(groupUI, BorderLayout.CENTER);
                add(button, BorderLayout.SOUTH);
            }
            
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(400, 200);
            }
            
            @Override
            public Dimension getMinimumSize() {
                return getPreferredSize();
            }
        }
    }
}
