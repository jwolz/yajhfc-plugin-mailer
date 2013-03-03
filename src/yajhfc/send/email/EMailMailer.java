/**
 * 
 */
package yajhfc.send.email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import yajhfc.SenderIdentity;
import yajhfc.Utils;
import yajhfc.phonebook.convrules.NameRule;

import com.sun.mail.smtp.SMTPTransport;

/**
 * @author jonas
 *
 */
public class EMailMailer extends YajMailer {
	
	static void install() {
		YajMailer.IMPLEMENTATION = EMailMailer.class;
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
    protected Date lastSendTime = null;
	
    public EMailMailer() {
        super();
    }
  
    public EMailMailer(EMailOptions options) {
        super();
        this.theOptions = options;
    }
  
    @Override
    public Date getLastSendTime() {
        return lastSendTime;
    }
    
    protected EMailOptions getOptions() {
        if (theOptions == null)
            return EntryPoint.getOptions();
        else
            return theOptions;
    }
    
    private Address[] convertRecipients(Collection<String> mailAdresses) throws AddressException {
        List<Address> recipients = new ArrayList<Address>();
        for (String a : mailAdresses) {
            for (Address adr : InternetAddress.parse(a, false)) {
                recipients.add(adr);
            }
        }
        return recipients.toArray(new Address[recipients.size()]);
    }
    
    @Override
    public boolean sendMail() throws MailException {
        if (subject == null) {
            throw new MailException("Need a subject");
        }
        if (body == null) {
            throw new MailException("Need a body");
        }
        if (toAddresses == null || toAddresses.size()==0) {
            throw new MailException("Need a toAddress");
        }
        if (fromIdentity == null) {
            throw new MailException("Need a fromIdentity");
        }
        if (fromIdentity.FromEMail == null || fromIdentity.FromEMail.length() == 0) {
            throw new MailException("To send a mail, you have to specify a sender e-mail address in your sender identity under \"Cover Page & Identities\".");
        }
        
        final EMailOptions eo = getOptions();
        try {
            Properties props = eo.toProperties();
            Session session = Session.getInstance(props, null);
            session.setDebug(Utils.debugMode);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromIdentity.FromEMail, EMailMailer.getSenderName(fromIdentity)));

            msg.setRecipients(Message.RecipientType.TO, convertRecipients(toAddresses));
            if (ccAddresses!=null && ccAddresses.size() > 0) {
                msg.setRecipients(Message.RecipientType.CC, convertRecipients(ccAddresses));
            }
            if (bccAddresses!=null && bccAddresses.size() > 0) {
                msg.setRecipients(Message.RecipientType.BCC, convertRecipients(bccAddresses));
            }

            msg.setSubject(subject);

            if (attachments.size() > 0) {
                // Attaches the specified files.
                // We need a multipart message to hold the attachment.
                MimeBodyPart mbp1 = new MimeBodyPart();
                mbp1.setText(body);
                
                MimeMultipart mp = new MimeMultipart();
                mp.addBodyPart(mbp1);
                
                for (Attachment attachment : attachments) {
                    MimeBodyPart mbp2 = new MimeBodyPart();
                    if (attachment.file != null) {
                        mbp2.attachFile(attachment.file);
                    } else if (attachment.textContent != null) {
                        mbp2.setText(attachment.textContent);
                    }    
                    if (attachment.fileName != null) {
                        mbp2.setFileName(attachment.fileName);
                    }
                    //mbp2.setDisposition(Part.ATTACHMENT);
                    mp.addBodyPart(mbp2);
                }

                msg.setContent(mp);
            } else {
                msg.setText(body);
            }

            msg.setSentDate(lastSendTime = new Date());
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

}
