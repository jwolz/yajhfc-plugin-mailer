/**
 * 
 */
package yajhfc.send.email;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import yajhfc.SenderIdentity;
import yajhfc.Utils;
import yajhfc.file.MultiFileConvFormat;
import yajhfc.file.MultiFileConverter;
import yajhfc.phonebook.convrules.NameRule;
import yajhfc.send.SendController;
import yajhfc.send.SendControllerMailer;
import yajhfc.shutdown.ShutdownManager;

import com.sun.mail.smtp.SMTPTransport;

/**
 * @author jonas
 *
 */
public class EMailMailer extends SendControllerMailer {
	
	static void install() {
		SendControllerMailer.INSTANCE = new EMailMailer();
	}
	
    public static String getSenderName(SenderIdentity id) {
        String fromName = NameRule.GIVENNAME_NAME.applyRule(id);
        if (fromName.length() > 0) {
            if (id.FromCompany.length() > 0) {
                return fromName + " (" + id.FromCompany + ")";
            } else {
                return fromName;
            }
        } else if (id.FromCompany.length() > 0) {
            return id.FromCompany;
        } else {
            return "";
        }
    }
    
    protected EMailOptions theOptions = null;
	protected MessageFormat attachmentNameFormat = new MessageFormat("doc_{0,date,yyyy-MM-dd_HH-mm-ss}.pdf");
	
    public EMailMailer() {
        super();
    }
  
    public EMailMailer(EMailOptions options) {
        super();
        this.theOptions = options;
    }
  
    protected EMailOptions getOptions() {
        if (theOptions == null)
            return EntryPoint.getOptions();
        else
            return theOptions;
    }
    
    
    /* (non-Javadoc)
     * @see yajhfc.send.SendControllerMailer#mailToRecipients(yajhfc.send.SendController, java.util.Collection)
     */
    @Override
    public boolean mailToRecipients(SendController controller, Collection<String> mailAdresses) throws MailException {
        return mailToRecipients(controller, controller.getSubject(), controller.getComment(), mailAdresses);
    }

    @Override
    public boolean mailToRecipients(String subject, String body, Collection<String> mailAdresses, File attachment,
            String attachmentName, SenderIdentity fromIdentity) throws MailException {
        EMailOptions eo = getOptions();
        
        if (fromIdentity.FromEMail == null || fromIdentity.FromEMail.length() == 0) {
            throw new MailException("To send a mail, you have to specify a sender e-mail address in your sender identity under \"Cover Page & Identities\".");
        }
        try {
            Properties props = eo.toProperties();
            Session session = Session.getInstance(props, null);
            session.setDebug(Utils.debugMode);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromIdentity.FromEMail, EMailMailer.getSenderName(fromIdentity)));

            List<Address> recipients = new ArrayList<Address>();
            for (String a : mailAdresses) {
                for (Address adr : InternetAddress.parse(a, false)) {
                    recipients.add(adr);
                }
            }
            msg.setRecipients(Message.RecipientType.TO, recipients.toArray(new Address[recipients.size()]));

            msg.setSubject(subject);

            if (attachment != null) {
                // Attach the specified file.
                // We need a multipart message to hold the attachment.
                MimeBodyPart mbp1 = new MimeBodyPart();
                mbp1.setText(body);
                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.attachFile(attachment);
                if (attachmentName != null)
                    mbp2.setFileName(attachmentName);
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(mbp1);
                mp.addBodyPart(mbp2);
                msg.setContent(mp);
            } else {
                msg.setText(body);
            }

            msg.setSentDate(new Date());
            msg.setHeader("X-Mailer", Utils.AppShortName + " " + Utils.AppVersion);

            SMTPTransport t = (SMTPTransport)session.getTransport("smtp");
            try {
                if (eo.auth)
                    t.connect(eo.user, eo.password.getPassword());
                else
                    t.connect();
                t.sendMessage(msg, msg.getAllRecipients());
            } finally {
                t.close();
            }

            return true;
        } catch (Exception e) {
            throw new MailException(e);
        } 
    }

    @Override
    public MessageFormat getAttachmentNameFormat() {
        return attachmentNameFormat;
    }

    @Override
    public boolean mailToRecipients(SendController controller, String subject, String body, Collection<String> mailAdresses) throws MailException {
        SenderIdentity id = controller.getIdentity();
        MessageFormat fromFormat = getAttachmentNameFormat();
        final Date sendDate = new Date();
        
        File tempFile;
        try {
            tempFile = File.createTempFile("attachment", ".pdf");
            ShutdownManager.deleteOnExit(tempFile);
            MultiFileConverter.convertTFLItemsToSingleFile(controller.getFiles(), tempFile, MultiFileConvFormat.PDF, controller.getPaperSize());
        } catch (Exception e) {
            throw new MailException("Error creating PDF for fax", e);
        } 
        
        boolean rv = mailToRecipients(subject, body, mailAdresses, tempFile, fromFormat.format(new Object[] { sendDate }), id);
        tempFile.delete();
        return rv;
    }

}
