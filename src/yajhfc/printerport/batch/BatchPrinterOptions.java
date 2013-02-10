/**
 * 
 */
package yajhfc.printerport.batch;

import yajhfc.AbstractFaxOptions;
import yajhfc.IDAndNameOptions;
import yajhfc.SenderIdentity;
import yajhfc.Utils;
import yajhfc.server.Server;
import yajhfc.server.ServerManager;

/**
 * @author jonas
 *
 */
public class BatchPrinterOptions extends AbstractFaxOptions {
    public boolean enabled = true;
    public int port = 19101;
    public String bindAddress = "localhost";
    
    public boolean enableMailer = true;
    public String subject = "Mailed Fax";
    public String comment = "This document has been printed to the YajHFC batch fax printer.";
    
    public boolean enableSuccessDir = false;
    public String successDir = "";
    public boolean enableFailDir = false;
    public String failDir = "";
    
    public int serverID = -1;
    public int identityID = -1;
    
    
    public Server getServer() {
        Server server = ServerManager.getDefault().getServerByID(serverID);
        if (server == null)
            return ServerManager.getDefault().getCurrent();
        else
            return server;
    }
    
    public SenderIdentity getIdentity() {
        SenderIdentity id = IDAndNameOptions.getItemByID(Utils.getFaxOptions().identities, identityID);
        if (id==null)
            return getServer().getDefaultIdentity();
        else
            return id;
    }
    
    /**
     * @param propertyPrefix
     */
    public BatchPrinterOptions() {
        super("batchprinter");
    }

}
