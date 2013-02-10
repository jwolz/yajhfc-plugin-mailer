package yajhfc.send.email;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.Utils;
import yajhfc.launch.Launcher2;
import yajhfc.options.PanelTreeNode;
import yajhfc.plugin.PluginManager;
import yajhfc.plugin.PluginUI;
import yajhfc.printerport.batch.BatchPortOptionsPanel;
import yajhfc.printerport.batch.BatchPrinterOptions;
import yajhfc.printerport.batch.TCPBatchPort;
import yajhfc.send.SendControllerMailer;

/**
 * Example initialization class for a YajHFC plugin.
 * 
 * The name of this class can be chosen freely, but must match the name
 * set in the YajHFC-Plugin-InitClass entry in the jar file.
 * @author jonas
 *
 */
public class EntryPoint {
    private static final Logger log = Logger.getLogger(EntryPoint.class.getName());
    public static TCPBatchPort BATCH_THREAD = null;
    
    public static String _(String key) {
        return key;
    }
    
    
	/**
	 * Plugin initialization method.
	 * The name and signature of this method must be exactly as follows 
	 * (i.e. it must always be "public static boolean init(int)" )
	 * @param startupMode the mode YajHFC is starting up in. The possible
	 *    values are one of the STARTUP_MODE_* constants defined in yajhfc.plugin.PluginManager
	 * @return true if the initialization was successful, false otherwise.
	 */
	public static boolean init(int startupMode) {

		PluginManager.pluginUIs.add(new PluginUI() {
			@Override
			public int getOptionsPanelParent() {
				return OPTION_PANEL_ADVANCED;
			}

			@Override
			public PanelTreeNode createOptionsPanel(PanelTreeNode parent) {
			    PanelTreeNode batchNode = new PanelTreeNode(
                        parent, // Always pass the parent as first parameter
                        new BatchPortOptionsPanel(), // The actual UI component that implements the options panel. 
                                                // This object *must* implement the OptionsPage interface.
                        _("Batch fax printer port"), // The text displayed in the tree view for this options page
                        Utils.loadIcon("general/Print"));            // The icon displayed in the tree view for this options page
			    batchNode.addChild(new PanelTreeNode(
						parent, // Always pass the parent as first parameter
						new EMailOptionsPanel(), // The actual UI component that implements the options panel. 
						                        // This object *must* implement the OptionsPage interface.
						_("SMTP settings"), // The text displayed in the tree view for this options page
						Utils.loadIcon("general/ComposeMail")));            // The icon displayed in the tree view for this options page
			    
			    return batchNode;
			}

			@Override
			public void saveOptions(Properties p) {
				getOptions().storeToProperties(p);
				getBatchPrintOptions().storeToProperties(p);
			}
		});

		SendControllerMailer.INSTANCE = new EMailMailer();
		
		reopenBatchThread();
		return true;
	}
	
	
	private static EMailOptions options;
	/**
	 * Lazily load some options (optional, only if you want to save settings)
	 * @return
	 */
    public static EMailOptions getOptions() {
        if (options == null) {
            options = new EMailOptions();
            options.loadFromProperties(Utils.getSettingsProperties());
        }
        return options;
    }
    
    private static BatchPrinterOptions batchPrintOptions;
    /**
     * Lazily load some options (optional, only if you want to save settings)
     * @return
     */
    public static BatchPrinterOptions getBatchPrintOptions() {
        if (batchPrintOptions == null) {
            batchPrintOptions = new BatchPrinterOptions();
            batchPrintOptions.loadFromProperties(Utils.getSettingsProperties());
        }
        return batchPrintOptions;
    }
    
    public static void reopenBatchThread() {
        if (BATCH_THREAD != null) {
            BATCH_THREAD.close();
            BATCH_THREAD = null;
        }
        final BatchPrinterOptions portOpts = getBatchPrintOptions();
        if (portOpts.enabled) {
            try {
                BATCH_THREAD = new TCPBatchPort(portOpts.bindAddress, portOpts.port);
                BATCH_THREAD.start();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Error creating server socket:", e);
            }
        }
    }
    
    /**
     * Launches YajHFC including this plugin (for debugging purposes)
     * @param args
     */
    public static void main(String[] args) {
		PluginManager.internalPlugins.add(EntryPoint.class);
		//Main.main(args);
		Launcher2.main(args);
	}
}
