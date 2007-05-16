package net.sf.freecol.client.gui.option;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import net.sf.freecol.FreeCol;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.client.gui.i18n.Messages;
import net.sf.freecol.client.gui.panel.FreeColPanel;
import net.sf.freecol.common.option.BooleanOption;
import net.sf.freecol.common.option.FileOption;
import net.sf.freecol.common.option.IntegerOption;
import net.sf.freecol.common.option.Option;
import net.sf.freecol.common.option.OptionGroup;
import net.sf.freecol.common.option.SelectOption;

/**
 * This class provides visualization for an {@link OptionGroup}. In order to
 * enable values to be both seen and changed.
 */
public final class OptionGroupUI extends JPanel implements OptionUpdater {
    private static final Logger logger = Logger.getLogger(OptionGroupUI.class.getName());

    public static final String COPYRIGHT = "Copyright (C) 2003-2005 The FreeCol Team";

    public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";

    public static final String REVISION = "$Revision$";

    /** The horisontal gap between components in this <code>OptionGroupUI</code>. */
    public static final int H_GAP = 10;

    private final OptionUpdater[] optionUpdaters;
    
    private final int level;


    /**
     * Creates a new <code>OptionGroupUI</code> for the given
     * <code>OptionGroup</code>.
     * 
     * @param option The <code>OptionGroup</code> to make a user interface
     *            for.
     */
    public OptionGroupUI(OptionGroup option, boolean editable, int level) {
        this.level = level;
        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        JPanel horizontalPanel = null;
        boolean buttonAdded = false;
        
        ArrayList<OptionUpdater> ou = new ArrayList<OptionUpdater>();
        Iterator<Option> it = option.iterator();
        while (it.hasNext()) {
            Option o = it.next();

            if (o instanceof OptionGroup) {
                if (level == 2) {
                    final OptionGroupUI groupUI = new OptionGroupUI((OptionGroup) o, editable, 1);
                    final OptionGroupButton ogb = new OptionGroupButton(o.getName(), groupUI);
                    ou.add(ogb);
                    if ((horizontalPanel == null) || !buttonAdded) {
                        horizontalPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                        horizontalPanel.setOpaque(false);
                    }
                    horizontalPanel.add(ogb);
                    add(horizontalPanel);
                    buttonAdded = true;
                } else {
                    final OptionGroupUI groupUI = new OptionGroupUI((OptionGroup) o, editable, level+1);
                    add(groupUI);
                    ou.add(groupUI);
                    buttonAdded = false;
                }
            } else if (o instanceof BooleanOption) {                
                final BooleanOptionUI boi = new BooleanOptionUI((BooleanOption) o, editable);
                ou.add(boi);
                final boolean alreadyAdded = (horizontalPanel != null && !buttonAdded);
                if (!alreadyAdded || buttonAdded) {
                    horizontalPanel = new JPanel(new GridLayout(1, 2, H_GAP, 5));
                    horizontalPanel.setOpaque(false);
                }
                horizontalPanel.add(boi);
                add(horizontalPanel);
                if (alreadyAdded) {
                    horizontalPanel = null;
                }
                buttonAdded = false;
            } else if (o instanceof IntegerOption) {
                final IntegerOptionUI iou = new IntegerOptionUI((IntegerOption) o, editable);
                add(iou);
                ou.add(iou);
                buttonAdded = false;
            } else if (o instanceof FileOption) {
                final FileOptionUI iou = new FileOptionUI((FileOption) o, editable);
                add(iou);
                ou.add(iou);
                buttonAdded = false;                
            } else if (o instanceof SelectOption) {
                final SelectOptionUI soi = new SelectOptionUI((SelectOption) o, editable);
                add(soi);
                ou.add(soi);
                buttonAdded = false;
            } else if (o instanceof FreeColAction) {
                final FreeColActionUI fau = new FreeColActionUI((FreeColAction) o, this);
                ou.add(fau);
                final boolean alreadyAdded = (horizontalPanel != null && !buttonAdded);
                if (!alreadyAdded || buttonAdded) {
                    horizontalPanel = new JPanel(new GridLayout(1, 2, H_GAP, 5));
                    horizontalPanel.setOpaque(false);
                }
                horizontalPanel.add(fau);
                add(horizontalPanel);
                if (alreadyAdded) {
                    horizontalPanel = null;
                }
                buttonAdded = false;
            } else {
                logger.warning("Unknown option.");
            }
        }
        optionUpdaters = ou.toArray(new OptionUpdater[0]);

        setBorder(BorderFactory.createTitledBorder(option.getName()));
        setOpaque(false);
    }
    
    private void addToHorizontalPanel(JComponent c) {
        
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
        private boolean displayed;
        
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
            this.displayed = false;
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
         * Delegates the call to <code>groupUI</code>.
         */
        public void updateOption() {
            groupUI.updateOption();
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
                super(new BorderLayout());
                
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
