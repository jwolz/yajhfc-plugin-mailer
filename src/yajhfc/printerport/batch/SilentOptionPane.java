package yajhfc.printerport.batch;

import java.awt.Window;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.send.email.i18n.Msgs;
import yajhfc.ui.swing.SwingYajOptionPane;

/**
 * A option pane with minimal user interaction.
 * If user interaction is necessary, it is done graphically.
 * 
 * @author jonas
 *
 */
public class SilentOptionPane extends SwingYajOptionPane {
    private static final Logger log = Logger.getLogger(SilentOptionPane.class.getName());

    public SilentOptionPane(Window parent) {
        super(parent);

    }

    @Override
    public void showExceptionDialog(String title, String message, Exception exc) {
        log.log(Level.SEVERE, title + ": " + message, exc);
    }

    @Override
    public void showExceptionDialog(String message, Exception exc) {
        showExceptionDialog(Msgs._("Error"), message, exc);
    }

    @Override
    public void showExceptionDialog(String title, String message,
            Exception exc, int timeout) {
        showExceptionDialog(title, message, exc);
    }

    @Override
    public void showExceptionDialog(String message, Exception exc, int timeout) {
        showExceptionDialog(message, exc);
    }

    @Override
    public void showMessageDialog(String message, String title, int messageType) {
        log.log(Level.INFO, title + ": " + message);
    }

    
}
