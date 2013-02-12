/**
 * 
 */
package yajhfc.printerport.batch;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

/**
 * @author jonas
 *
 */
public class StringHandler extends Handler {
    protected final StringBuffer buffer = new StringBuffer(1024);
    protected Formatter formatter;
    
    public StringHandler() {
        this(new SimpleFormatter());
    }
    
    public StringHandler(Formatter formatter) {
        this.formatter=formatter;
    }
    
    /* (non-Javadoc)
     * @see java.util.logging.Handler#publish(java.util.logging.LogRecord)
     */
    @Override
    public void publish(LogRecord record) {
        buffer.append(formatter.format(record));
    }

    /* (non-Javadoc)
     * @see java.util.logging.Handler#flush()
     */
    @Override
    public void flush() {
        // Do nothing
    }

    /* (non-Javadoc)
     * @see java.util.logging.Handler#close()
     */
    @Override
    public void close() throws SecurityException {
        // Do nothing
    }

    public StringBuffer getBuffer() {
        return buffer;
    }
    
    @Override
    public String toString() {
        return buffer.toString();
    }
}
