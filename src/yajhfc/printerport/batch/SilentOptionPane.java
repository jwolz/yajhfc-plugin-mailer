package yajhfc.printerport.batch;

import java.awt.Window;
import java.io.PrintWriter;
import java.io.StringWriter;

import yajhfc.send.email.EntryPoint;
import yajhfc.ui.swing.SwingYajOptionPane;

/**
 * A option pane with minimal user interaction.
 * If user interaction is necessary, it is done graphically.
 * 
 * @author jonas
 *
 */
public class SilentOptionPane extends SwingYajOptionPane {

    protected final StringWriter messageLog;
    protected final PrintWriter messageWriter;
    
    public StringWriter getMessageLog() {
        return messageLog;
    }
    
    public PrintWriter getMessageWriter() {
        return messageWriter;
    }
    
    public SilentOptionPane(Window parent) {
        super(parent);
        messageLog = new StringWriter();
        messageWriter = new PrintWriter(messageLog);
    }

    @Override
    public void showExceptionDialog(String title, String message, Exception exc) {
        showMessageDialog(message, title, 0);
        exc.printStackTrace(messageWriter);
        messageWriter.println();
    }

    @Override
    public void showExceptionDialog(String message, Exception exc) {
        showExceptionDialog(EntryPoint._("Error"), message, exc);
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
        messageWriter.append(title).append(": ").append(message);
        messageWriter.println();
    }

    
}
