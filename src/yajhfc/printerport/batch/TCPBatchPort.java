/**
 * 
 */
package yajhfc.printerport.batch;

import static yajhfc.send.email.EntryPoint._;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.file.textextract.FaxnumberExtractor;
import yajhfc.launch.Launcher2;
import yajhfc.phonebook.convrules.DefaultPBEntryFieldContainer;
import yajhfc.printerport.ListenThread;
import yajhfc.send.SendController;
import yajhfc.send.SendControllerListener;
import yajhfc.send.SendControllerMailer;
import yajhfc.send.SendControllerMailer.MailException;
import yajhfc.send.SendFaxArchiver;
import yajhfc.send.StreamTFLItem;
import yajhfc.send.email.EntryPoint;

/**
 * @author jonas
 *
 */
public class TCPBatchPort extends ListenThread implements SendControllerListener {
    private static final Logger log = Logger.getLogger(TCPBatchPort.class.getName());

    protected final Semaphore faxLock = new Semaphore(0);

    public TCPBatchPort(String listenAddress, int listenPort)
            throws UnknownHostException, IOException {
        super(listenAddress, listenPort);
    }

    @Override
    public void sendOperationComplete(boolean success) {
        if (success)
            log.info("Fax job successfully submitted to HylaFAX.");
        else
            log.info("Fax job could not be successfully submitted to HylaFAX.");
        
        faxLock.release();
    }

    @Override
    protected void submitFax(Socket sock) throws IOException {
        Handler memHandler = new StringHandler();
        memHandler.setLevel(Level.INFO);
        final Logger[] batchLoggers = {
                Logger.getLogger("yajhfc.printerport"),
                Logger.getLogger("yajhfc.send"),
                Logger.getLogger("yajhfc.file")
        };
        for (Logger l : batchLoggers)
            l.addHandler(memHandler);
        try {
            log.info("Connection to socket " + sock + ": Sending a fax in batch mode...");

            BatchPrinterOptions bpo = EntryPoint.getBatchPrintOptions();
            SilentOptionPane dialogs = new SilentOptionPane(Launcher2.application.getFrame());

            SendController sendController = new SendController(bpo.getServer(), dialogs, false, null);
            sendController.setIdentity(bpo.getIdentity());
            sendController.setSubject(bpo.subject);
            sendController.setComment(bpo.comment);
            sendController.getFiles().add(new StreamTFLItem(sock.getInputStream(), getPrinterSockText()));
            sendController.addSendControllerListener(this);

            SendFaxArchiver archiver = null;
            if (bpo.enableSuccessDir || bpo.enableFailDir) {
                String successDir = (bpo.enableSuccessDir ? bpo.successDir : null);
                String errorDir = (bpo.enableFailDir ? bpo.failDir : null);

                archiver = new SendFaxArchiver(sendController, dialogs, successDir, errorDir, memHandler);
            } 

            // n.b.: All documents should have been added at this point
            List<String> mailRecipients = null;
            List<String> faxRecipients = new ArrayList<String>();
            try {
                int num;
                if (bpo.enableMailer) {
                    mailRecipients = new ArrayList<String>();
                    FaxnumberExtractor extractor = new FaxnumberExtractor(FaxnumberExtractor.getDefaultPattern(), SendControllerMailer.getDefaultMailPattern());
                    num = extractor.extractFromMultipleDocuments(sendController.getFiles(), faxRecipients, mailRecipients);
                } else {
                    FaxnumberExtractor extractor = new FaxnumberExtractor();
                    num = extractor.extractFromMultipleDocuments(sendController.getFiles(), faxRecipients);
                }
                if (num == 0) {
                    log.warning(_("Error: No recipients could be found in the specified documents."));
                }
            } catch (Exception e) {
                dialogs.showExceptionDialog("Error extracting recipients", e);
            }


            if (faxRecipients.size() == 0 && (!bpo.enableMailer || bpo.enableMailer && mailRecipients.size()==0)) {
                log.severe(_("Fatal: No recipients specified for fax, discarding print job."));
                if (archiver != null)
                    archiver.saveFaxAsError();
            }

            boolean mailSuccess = false;
            if (bpo.enableMailer && mailRecipients.size()>0) {
                if (SendControllerMailer.INSTANCE != null)
                    try {
                        log.info("Sending mail to: " + mailRecipients);
                        SendControllerMailer.INSTANCE.mailToRecipients(sendController, mailRecipients);
                        mailSuccess = true;
                        log.info("Mail sent successfully.");
                    } catch (MailException e) {
                        dialogs.showExceptionDialog("Error sending mail to " + mailRecipients, e);
                    }
                else
                    log.severe("Error: Cannot send mail: SendControllerMailer not available!");
            }
            if (faxRecipients.size() > 0) {
                DefaultPBEntryFieldContainer.parseCmdLineStrings(sendController.getNumbers(), faxRecipients);

                if (sendController.validateEntries()) {
                    log.info("Sending fax to: " + sendController.getNumbers());
                    sendController.sendFax();

                    try {
                        faxLock.acquire();
                    } catch (InterruptedException e) {
                        log.log(Level.SEVERE, "Error waiting for fax to be sent", e);
                    }
                } else {
                    log.warning(_("Error: Invalid data specified for fax, discarding print job."));
                    if (archiver != null)
                        archiver.saveFaxAsError();
                }
            } else {
                if (mailSuccess) {
                    if (archiver != null)
                        archiver.saveFaxAsSuccess();
                }
            }
            log.info("Batch print job finished.");
        } finally {
            for (Logger l : batchLoggers)
                l.removeHandler(memHandler);
        }
    }
}

