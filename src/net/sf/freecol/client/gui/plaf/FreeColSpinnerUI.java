package net.sf.freecol.client.gui.plaf;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicSpinnerUI;

public class FreeColSpinnerUI extends BasicSpinnerUI {

    public static ComponentUI createUI(@SuppressWarnings("unused") JComponent c) {
        return new FreeColSpinnerUI();
    }
    
    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        
        final JSpinner spinner = (JSpinner) c;
        
        final boolean spinnerOpaque = spinner.isOpaque();
        spinner.getEditor().setOpaque(spinnerOpaque);
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setOpaque(spinnerOpaque);
        }
    }
}
