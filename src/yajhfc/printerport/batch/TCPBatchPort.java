/**
 * 
 */
package yajhfc.printerport.batch;

import static yajhfc.send.email.i18n.Msgs._;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import yajhfc.DateKind;
import yajhfc.Utils;
import yajhfc.file.FileFormat;
import yajhfc.file.FileUtils;
import yajhfc.file.FormattedFile;
import yajhfc.file.textextract.FaxnumberExtractor;
import yajhfc.launch.Launcher2;
import yajhfc.phonebook.convrules.DefaultPBEntryFieldContainer;
import yajhfc.printerport.ListenThread;
import yajhfc.send.SendController;
import yajhfc.send.SendControllerListener;
import yajhfc.send.SendFaxArchiver;
import yajhfc.send.StreamTFLItem;
import yajhfc.send.email.EntryPoint;
import yajhfc.send.email.MailException;
import yajhfc.send.email.YajMailer;

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
            //sendController.setSubject(bpo.subject);
            //sendController.setComment(bpo.comment);
            StreamTFLItem document = new StreamTFLItem(sock.getInputStream(), getPrinterSockText());
            sendController.getFiles().add(document);
            sendController.addSendControllerListener(this);
            
            try {
                final FormattedFile docFile = document.getPreviewFilename();
                if (docFile.getFormat() == FileFormat.PostScript) {
                    String subject = FileUtils.extractTitleFromPSFile(docFile.file);
                    if (subject != null) {
                        sendController.setSubject(subject);
                    }
                }
            } catch (Exception e1) {
                log.log(Level.WARNING, "Error extracting title from document.", e1);
            }
            

            SendFaxArchiver archiver = null;
            if (bpo.enableSuccessDir || bpo.enableFailDir || bpo.enableFailMail) {
                String successDir = (bpo.enableSuccessDir ? bpo.successDir : null);
                String errorDir = (bpo.enableFailDir ? bpo.failDir : null);
                String errorMail = (bpo.enableFailMail ? bpo.failRecipient : null);

                archiver = new SendFaxArchiver(sendController, dialogs, successDir, errorDir, errorMail, memHandler);
            } 

            // n.b.: All documents should have been added at this point
            List<String> mailRecipients = null;
            List<String> faxRecipients = new ArrayList<String>();
            List<String> subjects = null;
            try {
                int num;
                if (bpo.enableMailer) {
                    mailRecipients = new ArrayList<String>();
                    subjects = new ArrayList<String>();
                    FaxnumberExtractor extractor = new FaxnumberExtractor(FaxnumberExtractor.getDefaultPattern(), FaxnumberExtractor.getDefaultMailPattern(), FaxnumberExtractor.getDefaultSubjectPattern());
                    num = extractor.extractFromMultipleDocuments(sendController.getFiles(), faxRecipients, mailRecipients, subjects);
                    num = num - subjects.size(); // Subjects are no recipients...
                } else {
                    FaxnumberExtractor extractor = new FaxnumberExtractor();
                    num = extractor.extractFromMultipleDocuments(sendController.getFiles(), faxRecipients);
                }
                if (num <= 0) {
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

            boolean mailUseSendControllerSubject = false;
            if (subjects.size() > 0) {
                mailUseSendControllerSubject = true;
                
                // Set subject to the last subject found
                String subject = subjects.get(subjects.size()-1);
                if (! FaxnumberExtractor.SUBJECT_DOCTITLE.equalsIgnoreCase(subject.trim())) {
                    sendController.setSubject(subject);
                }
            }
            
            boolean mailSuccess = false;
            if (bpo.enableMailer && mailRecipients.size()>0) {
                if (YajMailer.isAvailable())
                    try {
                        log.info("Sending mail to: " + mailRecipients);
                        YajMailer mailer = YajMailer.getInstance();
                        mailer.initializeFromSendController(sendController);
                        mailer.setToAddresses(mailRecipients);
                        mailer.setSubject(mailUseSendControllerSubject ? sendController.getSubject() : bpo.subject);
                        mailer.setBody(bpo.comment);
                        if (bpo.enableBCC && bpo.bccExactCopy) {
                            mailer.setBccAddresses(bpo.bccAddress);
                        }
                        mailSuccess = mailer.sendMail();
                        if (mailSuccess)
                            log.info("Mail sent successfully.");
                        else
                            log.info("Mail sent unsuccessfully.");
                        
                        if (bpo.enableBCC && !bpo.bccExactCopy) {
                            final String faxSubject = sendController.getSubject();
                            
                            StringBuilder newBody = new StringBuilder(_("The attached PDF has been mailed to the following recipient(s):")).append('\n');
                            for (String recipient : mailer.getToAddresses()) {
                                newBody.append("  ").append(recipient).append('\n');
                            }
                            newBody.append('\n');
                            newBody.append(_("Subject:")).append(' ').append(mailer.getSubject()).append('\n');
                            if (faxSubject != null && faxSubject.length() > 0) {
                                newBody.append(_("Document title:")).append(' ').append(faxSubject).append('\n');
                            }
                            newBody.append(_("Time:")).append(' ').append(DateKind.DATE_AND_TIME.getFormat().format(mailer.getLastSendTime())).append('\n');
                            newBody.append('\n');
                            newBody.append(_("The original mail's body can be found in the attachment message.txt")).append('\n');
                            newBody.append('\n');
                            newBody.append(Utils._("Send log:")).append("\n")
                                .append("-------------------------------------------------\n")
                                .append(memHandler)
                                .append('\n');

                            String newSubject = _("Sent Mail");
                            if (faxSubject != null && faxSubject.length() > 0) {
                                newSubject = newSubject + " [" + faxSubject + "]";
                            }
                            
                            mailer.addAttachment(mailer.getBody(), "message.txt");
                            mailer.setSubject(newSubject);
                            mailer.setBody(newBody.toString());
                            mailer.setToAddresses(bpo.bccAddress);
                            mailer.sendMail();
                        }
                    } catch (MailException e) {
                        dialogs.showExceptionDialog("Error sending mail to " + mailRecipients, e);
                        if (archiver != null)
                            archiver.saveFaxAsError();
                    }
                else {
                    log.severe("Error: Cannot send mail: YajMailer not available!");
                }
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

