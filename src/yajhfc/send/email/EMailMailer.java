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

    
    /* (non-Javadoc)
     * @see yajhfc.send.SendControllerMailer#mailToRecipients(yajhfc.send.SendController, java.util.Collection)
     */
    @Override
    public boolean mailToRecipients(SendController controller, Collection<String> mailAdresses) throws MailException {
        try {
            EMailOptions eo = EntryPoint.getOptions();
            SenderIdentity id = controller.getIdentity();
            MessageFormat fromFormat = new MessageFormat("doc_{0,date,yyyy-MM-dd_HH-mm-ss}.pdf");
            final Date sendDate = new Date();
            
            File tempFile = File.createTempFile("attachment", ".pdf");
            MultiFileConverter.convertTFLItemsToSingleFile(controller.getFiles(), tempFile, MultiFileConvFormat.PDF, controller.getPaperSize());
            ShutdownManager.deleteOnExit(tempFile);
            
            Properties props = eo.toProperties();
            Session session = Session.getInstance(props, null);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(id.FromEMail, EMailMailer.getSenderName(id)));

            List<Address> recipients = new ArrayList<Address>();
            for (String a : mailAdresses) {
                for (Address adr : InternetAddress.parse(a, false)) {
                    recipients.add(adr);
                }
            }
            msg.setRecipients(Message.RecipientType.TO, recipients.toArray(new Address[recipients.size()]));

            msg.setSubject(controller.getSubject());

            // Attach the specified file.
            // We need a multipart message to hold the attachment.
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(controller.getComment());
            MimeBodyPart mbp2 = new MimeBodyPart();
            mbp2.attachFile(tempFile);
            mbp2.setFileName(fromFormat.format(new Object[] { sendDate }));
            MimeMultipart mp = new MimeMultipart();
            mp.addBodyPart(mbp1);
            mp.addBodyPart(mbp2);
            msg.setContent(mp);


            msg.setHeader("X-Mailer", Utils.AppShortName + " " + Utils.AppVersion);
            msg.setSentDate(sendDate);

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
            tempFile.delete();
            return true;
        } catch (Exception e) {
            throw new MailException(e);
        } 
    }

}
