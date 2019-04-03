/**
 *  Copyright (C) 2002-2019   The FreeCol Team
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

package net.sf.freecol.client.gui.dialog;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.InGameController;
import net.sf.freecol.client.gui.ChoiceItem;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.panel.*;
import net.sf.freecol.client.gui.plaf.FreeColOptionPaneUI;
import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;
import static net.sf.freecol.common.util.CollectionUtils.*;


/**
 * Superclass for all dialogs in FreeCol.
 */
public class FreeColDialog<T> extends JDialog implements PropertyChangeListener {

    private static final Logger logger = Logger.getLogger(FreeColDialog.class.getName());

    public static enum DialogType {
        PLAIN,
        QUESTION,
    };

    /** The enclosing client. */
    protected final FreeColClient freeColClient;

    /** Is this dialog modal? */
    protected boolean modal;

    /** The options to choose from. */
    protected List<ChoiceItem<T>> options;

    /** The JOptionPane to embed in this dialog. */
    private JOptionPane pane;


    /**
     * Protected constructor for the subclass panels.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     */
    protected FreeColDialog(FreeColClient freeColClient, JFrame frame) {
        super(frame);

        this.freeColClient = freeColClient;
    }

    /**
     * Full constructor for canvas to build a dialog in one hit (supplying
     * the getResponse() implementation).
     *
     * Much of this was extracted from the source for
     * JOptionPane.createDialog.  We needed a way to control modality.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     * @param type The {@code DialogType} to create.
     * @param modal Should this dialog be modal?
     * @param tmpl A {@code StringTemplate} to explains the choice.
     * @param icon An optional icon to display.
     * @param options A list of options to choose from.
     */
    public FreeColDialog(FreeColClient freeColClient, JFrame frame,
                         DialogType type, boolean modal, StringTemplate tmpl,
                         ImageIcon icon, List<ChoiceItem<T>> options) {
        this(freeColClient, frame);

        initializeDialog(frame, type, modal,
                         Utility.localizedTextArea(tmpl), icon, options);
    }


    /**
     * Select the default option from the supplied options.
     *
     * @param options A list of options to choose from.
     * @return The option to select initially.
     */
    private int selectDefault(List<ChoiceItem<T>> options) {
        int def = -1, can = -1, ok = -1, i = 0;
        for (ChoiceItem<T> ci : options) {
            if (ci.isDefault()) def = i;
            else if (ci.isCancel()) can = i;
            else if (ci.isOK()) ok = i;
            i++;
        }
        return (def >= 0) ? def : (can >= 0) ? can : (ok >= 0) ? ok
            : options.size() - 1;
    }

    /**
     * Collect the enabled options and return as an array so as to be able
     * to pass to the JOptionPane constructor.
     *
     * @return An array of enabled options.
     */
    private Object[] selectOptions() {
        return transform(this.options, ChoiceItem::isEnabled).toArray();
    }

    /**
     * Complete the initialization.  Useful for subclasses that need
     * to construct a non-trivial object to display in the JOptionPane.
     *
     * @param frame The owner frame.
     * @param type The {@code DialogType} to create.
     * @param modal Should this dialog be modal?
     * @param jc The main object that explains the choice for the user.
     * @param icon An optional icon to display.
     * @param options A list of options to choose from.
     */
    protected final void initializeDialog(JFrame frame, DialogType type,
                                          boolean modal, JComponent jc,
                                          ImageIcon icon,
                                          List<ChoiceItem<T>> options) {
        this.modal = modal;
        this.options = options;
        int paneType = JOptionPane.QUESTION_MESSAGE;
        switch (type) {
        case PLAIN:    paneType = JOptionPane.PLAIN_MESSAGE; break;
        case QUESTION: paneType = JOptionPane.QUESTION_MESSAGE; break;
        }
        int def = selectDefault(options);
        ChoiceItem<T> ci = (def >= 0) ? options.get(def) : null;
        this.pane = new JOptionPane(jc, paneType, JOptionPane.DEFAULT_OPTION,
                                    icon, selectOptions(), ci);
        this.pane.setBorder(Utility.DIALOG_BORDER);
        this.pane.setOpaque(false);
        this.pane.setName("FreeColDialog");
        this.pane.setValue(JOptionPane.UNINITIALIZED_VALUE);
        this.pane.addPropertyChangeListener(this);
        this.pane.setSize(this.pane.getPreferredSize());
        setComponentOrientation(this.pane.getComponentOrientation());

        Container contentPane = getContentPane();
        contentPane.add(this.pane);
        setSize(getPreferredSize());
        setResizable(false);
        setUndecorated(true);
        setModal(modal);

        setSubcomponentsNotOpaque(this.pane);
        try { // Layout failures might not get logged.
            pack();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Packing failure", e);
        }
        setLocationRelativeTo(frame);

        WindowAdapter adapter = new WindowAdapter() {
                private boolean gotFocus = false;

                @Override
                public void windowClosing(WindowEvent we) {
                    if (!FreeColDialog.this.responded()) {
                        FreeColDialog.this.setValue(null);
                    }
                }
                @Override
                public void windowGainedFocus(WindowEvent we) {
                    if (!gotFocus) { // Once window gets focus, initialize.
                        FreeColDialog.this.pane.selectInitialValue();
                        gotFocus = true;
                    }
                }
            };
        addWindowListener(adapter);
        addWindowFocusListener(adapter);

        addComponentListener(new ComponentAdapter() {
                @Override
                public void componentShown(ComponentEvent ce) {
                    // Reset value to ensure closing works properly.
                    FreeColDialog.this.pane
                        .setValue(JOptionPane.UNINITIALIZED_VALUE);
                }
            });

        addMouseListener(new MouseAdapter() {
                private Point loc;

                //@Override
                //public void mouseDragged(MouseEvent e) {}

                //@Override
                //public void mouseMoved(MouseEvent e) {}

                @Override
                public void mousePressed(MouseEvent e) {
                    loc = SwingUtilities
                        .convertPoint((Component)e.getSource(),
                            e.getX(), e.getY(), null);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (loc == null) return;
                    Point now = SwingUtilities
                        .convertPoint((Component)e.getSource(),
                            e.getX(), e.getY(), null);
                    int dx = now.x - loc.x;
                    int dy = now.y - loc.y;
                    Point p = FreeColDialog.this.getLocation();
                    FreeColDialog.this.setLocation(p.x + dx, p.y + dy);
                    loc = null;
                }
            });
    }

    public static void setSubcomponentsNotOpaque(JComponent j) {
        synchronized(j.getTreeLock()) {
            iterateOverOpaqueLayersComponents(j);
        }
    }

    private static void iterateOverOpaqueLayersComponents(JComponent j){   
        if (j instanceof JPanel || j instanceof JOptionPane) {            
           Component[] componentes = j.getComponents();            
           for (Component componente : componentes) {
               setOpaqueLayerRecursive(componente);
           }
        }    
    }

    private static void setOpaqueLayerRecursive(Component opaqueComponent) {
        if (opaqueComponent instanceof JTextArea ||
            opaqueComponent instanceof JLabel) {
            if (opaqueComponent.isOpaque()) {
                ((JComponent) opaqueComponent).setOpaque(false);
            }
        } else if (opaqueComponent instanceof JPanel) {
            JComponent panel = (JComponent)opaqueComponent;
            if (panel.isOpaque()) {
                panel.setOpaque(false);
            }
            iterateOverOpaqueLayersComponents(panel);
        }
    }

    /**
     * Get the FreeColClient.
     *
     * @return The {@code FreeColClient}.
     */
    protected FreeColClient getFreeColClient() {
        return freeColClient;
    }

    /**
     * Get the GUI.
     *
     * @return The {@code GUI}.
     */
    protected GUI getGUI() {
        return freeColClient.getGUI();
    }

    /**
     * Get the client controller.
     *
     * @return The client {@code InGameController}.
     */
    protected InGameController igc() {
        return freeColClient.getInGameController();
    }

    /**
     * Get the Image library.
     *
     * @return The {@code ImageLibrary}.
     */
    protected ImageLibrary getImageLibrary() {
        return getGUI().getImageLibrary();
    }

    /**
     * Get the game.
     *
     * @return The {@code Game}.
     */
    protected Game getGame() {
        return freeColClient.getGame();
    }

    /**
     * Gets the specification.
     *
     * @return The specification from the game.
     */
    protected Specification getSpecification() {
        final Game game = getGame();
        return (game == null) ? null : game.getSpecification();
    }

    /**
     * Get the player.
     *
     * @return The current {@code Player}.
     */
    protected Player getMyPlayer() {
        return freeColClient.getMyPlayer();
    }

    /**
     * Get the response that was set by {@link JOptionPane#setValue} and
     * clean up the dialog.  Used by implementors of getResponse().
     *
     * @return The pane value.
     */
    protected Object getValue() {
        Object value = pane.getValue();
        dispose(); // Pane will now be null following removeNotify().
        return value;
    }

    /**
     * Set the value of this dialog.
     *
     * @param value The new value.
     */
    protected synchronized void setValue(Object value) {
        this.pane.setValue(value);
    }

    /**
     * Has this dialog been given a response.
     *
     * @return True if the dialog has a response.
     */
    public synchronized boolean responded() {
        return this.pane != null
            && this.pane.getValue() != JOptionPane.UNINITIALIZED_VALUE;
    }

    /**
     * Get the response from this dialog.
     *
     * @return The response from this dialog.
     */
    public T getResponse() {
        if (responded()) {
            Object value = getValue();
            for (ChoiceItem<T> ci : this.options) {
                if (ci.equals(value)) return ci.getObject();
            }
        }
        return null;
    }

    /**
     * Is this a modal dialog?
     *
     * @return True if this is a modal dialog.
     */
    @Override
    public boolean isModal() {
        return modal;
    }

    /**
     * Create a list of choices.
     *
     * @param <T> The choice type.
     * @return An empty list of choices.
     */
    public static <T> List<ChoiceItem<T>> choices() {
        return new ArrayList<>();
    }

    /**
     * Helper to get a small single abstract unit image.
     *
     * @param au The {@code AbstractUnit} to examine.
     * @return A suitable {@code BufferedImage}.
     */
    public BufferedImage getSmallAbstractUnitImage(AbstractUnit au) {
        final Specification spec = getSpecification();
        return getImageLibrary().getSmallUnitTypeImage(au.getType(spec),
                                                       au.getRoleId(), false);
    }

        
    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void propertyChange(PropertyChangeEvent e) {
        // Let the defaultCloseOperation handle the closing if the
        // user closed the window without selecting a button (in which
        // case the new value will be null).  Otherwise, close the dialog.
        if (this.isVisible()
            && e.getSource() == pane
            && (JOptionPane.VALUE_PROPERTY.equals(e.getPropertyName())
                || JOptionPane.INPUT_VALUE_PROPERTY.equals(e.getPropertyName()))
            && e.getNewValue() != null
            && e.getNewValue() != JOptionPane.UNINITIALIZED_VALUE) {
            this.setVisible(false);
        }
    }


    // Override Dialog

    /**
     * {@inheritDoc}
     */
    @Override
    public void setVisible(boolean val) {
        if (val) this.pane.selectInitialValue();
        super.setVisible(val); // This is where the thread blocks when modal.
    }


    // Override Component

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        super.removeNotify();

        getGUI().removeDialog(FreeColDialog.this);

        removeAll();
        if (this.pane != null) {
            this.pane.removePropertyChangeListener(this);
            this.pane = null;
        }

        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void requestFocus() {
        if (this.pane != null
            && this.pane.getUI() instanceof FreeColOptionPaneUI) {
            this.pane.getUI().selectInitialValue(null);
        }
    }
}
