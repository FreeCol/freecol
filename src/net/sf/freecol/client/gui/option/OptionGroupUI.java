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

package net.sf.freecol.client.gui.option;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.gui.action.FreeColAction;
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
import net.sf.freecol.common.option.StringOption;

/**
 * This class provides visualization for an {@link OptionGroup}. In order to
 * enable values to be both seen and changed.
 */
public final class OptionGroupUI extends JPanel implements OptionUpdater {

    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    private final ArrayList<OptionUpdater> optionUpdaters = new ArrayList<OptionUpdater>();

    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionGroup</code>.
     * 
     * @param option The <code>OptionGroup</code> to make a user interface
     *            for.
     * @param editable a <code>boolean</code> value
     * @param level an <code>int</code> value
     */
    public OptionGroupUI(OptionGroup option, boolean editable, int level, Map<String, JComponent> optionUIs) {

        setLayout(new MigLayout("wrap 4", "[fill]related[fill]unrelated[fill]related[fill]"));

        Iterator<Option> it = option.iterator();
        while (it.hasNext()) {
            Option o = it.next();
            addOptionUI(o, editable, level, optionUIs);
        }

        setOpaque(false);
    }


    private void addOptionUI(Option o, boolean editable, int level, Map<String, JComponent> optionUIs) {
        if (o instanceof OptionGroup) {
            add(new JLabel(o.getName()), "span, split 2");
            add(new JSeparator(), "growx");
            Iterator<Option> it = ((OptionGroup) o).iterator();
            while (it.hasNext()) {
                Option option = it.next();
                addOptionUI(option, editable, level, optionUIs);
            }
        } else if (o instanceof BooleanOption) {                
            final BooleanOptionUI boi = new BooleanOptionUI((BooleanOption) o, editable);
            optionUpdaters.add(boi);
            if (boi.getText().length() > 40) {
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
            optionUpdaters.add(soi);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), soi);
            }
        } else if (o instanceof ListOption<?>) {
            @SuppressWarnings("unchecked")
            final ListOptionUI soi = new ListOptionUI((ListOption) o, editable);
            add(soi);
            optionUpdaters.add(soi);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), soi);
            }
        } else if (o instanceof RangeOption) {
            final RangeOptionUI soi = new RangeOptionUI((RangeOption) o, editable);
            add(soi, "newline, span");
            optionUpdaters.add(soi);
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
            optionUpdaters.add(soi);
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
            optionUpdaters.add(iou);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), iou);
            }
        } else if (o instanceof FileOption) {
            final FileOptionUI iou = new FileOptionUI((FileOption) o, editable);
            add(iou, "newline, span");
            optionUpdaters.add(iou);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), iou);
            }
        } else if (o instanceof StringOption) {
            final StringOptionUI soi = new StringOptionUI((StringOption) o, editable);
            if (soi.getLabel().getText().length() > 30) {
                add(soi.getLabel(), "newline, span 3, right");
            } else {
                add(soi.getLabel(), "right");
            }
            add(soi);
            optionUpdaters.add(soi);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), soi);
            }
        } else if (o instanceof LanguageOption) {
            final LanguageOptionUI soi = new LanguageOptionUI((LanguageOption) o, editable);
            if (soi.getLabel().getText().length() > 30) {
                add(soi.getLabel(), "newline, span 3");
            } else {
                add(soi.getLabel());
            }
            add(soi);
            optionUpdaters.add(soi);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), soi);
            }
        } else if (o instanceof AudioMixerOption) {
            final AudioMixerOptionUI soi = new AudioMixerOptionUI((AudioMixerOption) o, editable);
            if (soi.getLabel().getText().length() > 30) {
                add(soi.getLabel(), "newline, span 3");
            } else {
                add(soi.getLabel());
            }
            add(soi);
            optionUpdaters.add(soi);
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), soi);
            }
        } else if (o instanceof FreeColAction) {
            final FreeColActionUI fau = new FreeColActionUI((FreeColAction) o, this);
            optionUpdaters.add(fau);
            add(fau, "newline, span");
            if (!o.getId().equals(Option.NO_ID)) {
                optionUIs.put(o.getId(), fau);
            }
        } else {
            logger.warning("Unknown option.");
        }
    }


    /**
     * Rollback to the original value.
     * 
     * This method gets called so that changes made to options with
     * {@link Option#isPreviewEnabled()} is rolled back
     * when an option dialoag has been cancelled.
     */
    public void rollback() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.rollback();
        }
    }
    
    /**
     * Unregister <code>PropertyChangeListener</code>s.
     */
    public void unregister() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.unregister();
        }
    }

    /**
     * Updates the value of the {@link Option} this object keeps.
     */
    public void updateOption() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.updateOption();
        }
    }
    
    /**
     * Reset with the value from the option.
     */
    public void reset() {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            optionUpdater.reset();
        }
    }

    /**
     * Removes the given <code>KeyStroke</code> from all of this
     * <code>OptionGroupUI</code>'s children.
     * 
     * @param keyStroke The <code>KeyStroke</code> to be removed.
     */
    public void removeKeyStroke(KeyStroke keyStroke) {
        for (OptionUpdater optionUpdater : optionUpdaters) {
            if (optionUpdater instanceof FreeColActionUI) {
                ((FreeColActionUI) optionUpdater).removeKeyStroke(keyStroke);
            }
        }
    }

//    /**
//     * A button for displaying an <code>OptionGroupUI</code>. The 
//     * <code>OptionGroupUI</code> is displayed inside a panel when
//     * the button is clicked.
//     */
//    private class OptionGroupButton extends JButton implements OptionUpdater {
//        
//        private final OptionGroupUI groupUI;
//        private final OptionGroupButton optionGroupButton;
//        private final OptionGroupPanel optionGroupPanel;
//        
//        /**
//         * Creates a new button.
//         * 
//         * @param name The title on the button.
//         * @param groupUI The <code>OptionGroupUI</code> to be displayed when
//         *      the button is clicked.
//         */
//        OptionGroupButton(final String name, final OptionGroupUI groupUI) {
//            super(name);
//            
//            this.groupUI = groupUI;
//            optionGroupButton = this;
//            optionGroupPanel = new OptionGroupPanel();
//            
//            addActionListener(new ActionListener() {
//               public void actionPerformed(ActionEvent e) {
//                   optionGroupButton.setEnabled(false);
//                   FreeCol.getFreeColClient().getCanvas().addAsFrame(optionGroupPanel);
//               }                
//            });
//        }
//        
//        /**
//         * Rollback to the original value.
//         * 
//         * This method gets called so that changes made to options with
//         * {@link Option#isPreviewEnabled()} is rolled back
//         * when an option dialoag has been cancelled.
//         */
//        public void rollback() {
//            groupUI.rollback();
//        }
//        
//        /**
//         * Delegates the call to <code>groupUI</code>.
//         */
//        public void updateOption() {
//            groupUI.updateOption();
//        }
//        
//        /**
//         * Reset with the value from the option.
//         */
//        public void reset() {
//            groupUI.reset();
//        }
//        
//        /**
//         * Unregister <code>PropertyChangeListener</code>s and closes
//         * the subpanel.
//         */
//        public void unregister() {
//            groupUI.unregister();
//            FreeCol.getFreeColClient().getCanvas().remove(optionGroupPanel);
//        }
//        
//        /**
//         * Panel for displaying the <code>groupUI</code>.
//         */        
//        private class OptionGroupPanel extends FreeColPanel {
//            public OptionGroupPanel() {
//                super(FreeCol.getFreeColClient().getCanvas(), new BorderLayout());
//                
//                JButton button = new JButton(Messages.message("ok"));
//                button.addActionListener(new ActionListener() {
//                    public void actionPerformed(ActionEvent e) {
//                        FreeCol.getFreeColClient().getCanvas().remove(optionGroupPanel);
//                        optionGroupButton.setEnabled(true);
//                    }
//                });
//                
//                add(groupUI, BorderLayout.CENTER);
//                add(button, BorderLayout.SOUTH);
//            }
//            
//            @Override
//            public Dimension getPreferredSize() {
//                return new Dimension(400, 200);
//            }
//            
//            @Override
//            public Dimension getMinimumSize() {
//                return getPreferredSize();
//            }
//        }
//    }

    @Override
    public String getUIClassID() {
        return "OptionGroupUI";
    }
}
