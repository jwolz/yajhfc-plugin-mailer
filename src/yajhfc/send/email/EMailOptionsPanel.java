/*
 * YajHFC - Yet another Java Hylafax client
 * Copyright (C) 2009 Jonas Wolz
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package yajhfc.send.email;

import static yajhfc.send.email.EntryPoint._;
import info.clearthought.layout.TableLayout;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import yajhfc.FaxOptions;
import yajhfc.SenderIdentity;
import yajhfc.Utils;
import yajhfc.options.AbstractOptionsPanel;
import yajhfc.options.OptionsWin;
import yajhfc.util.ExcDialogAbstractAction;
import yajhfc.util.IntVerifier;
import yajhfc.util.ProgressDialog;
import yajhfc.util.ProgressWorker;

import com.sun.mail.smtp.SMTPTransport;

/**
 * Implements a crude and simple UI to set the three example options.
 * 
 * @author jonas
 *
 */
public class EMailOptionsPanel extends AbstractOptionsPanel<FaxOptions> {   

    public EMailOptionsPanel() {
        super(false);
    }

    JTextField textHost, textPort, textUser;
    JCheckBox checkSSL, checkAuth, checkTLS;
    Action testAction;
    JPasswordField passwordField;


    @Override
    protected void createOptionsUI() { 
        testAction = new ExcDialogAbstractAction(_("Test connection")) {

            @Override
            protected void actualActionPerformed(ActionEvent e) {
                final EMailOptions eo = new EMailOptions();
                saveSettings(eo);

                final SenderIdentity id = Utils.getFaxOptions().getDefaultIdentity();
                if (id.FromEMail == null || id.FromEMail.length() == 0) {
                    JOptionPane.showMessageDialog(EMailOptionsPanel.this, _("To send a mail, you have to specify a sender e-mail address in your sender identity under \"Cover Page & Identities\"."), _("Error"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                final String recipient = JOptionPane.showInputDialog(EMailOptionsPanel.this, _("Please enter the test mail's recipient:"), id.FromEMail);
                if (recipient == null)
                    return;

                ProgressWorker testWorker = new ProgressWorker() {
                    boolean success = false;

                    @Override
                    public void doWork() {
                        try{
                            Properties props = eo.toProperties();
                            Session session = Session.getInstance(props, null);
                            Message msg = new MimeMessage(session);
                            msg.setFrom(new InternetAddress(id.FromEMail, EMailMailer.getSenderName(id)));

                            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient, false));

                            msg.setSubject(_("YajHFC test mail"));

                            String text = 
                                    "This is a test e-mail message sent from " + Utils.AppShortName + " " + Utils.AppVersion + "\n\n" +
                                            "Settings used:\n" +
                                            "Server:              " + eo.hostname + ":" + eo.port + "\n" +
                                            "Use SSL?:            " + eo.ssl + "\n" +
                                            "Allow TLS?:          " + eo.tls + "\n" +
                                            "Use authentication?: " + eo.auth + "\n" +
                                            "User name for auth:  " + eo.user + "\n";

                            msg.setText(text);

                            msg.setHeader("X-Mailer", Utils.AppShortName + " " + Utils.AppVersion);
                            msg.setSentDate(new Date());

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

                            success = true;
                        } catch (Exception e1) {
                            showExceptionDialog(_("Error sending the message:"), e1);
                        }
                    }
                    @Override
                    protected void done() {
                        if (success)
                            JOptionPane.showMessageDialog(EMailOptionsPanel.this, _("Message sent successfully."));
                    }
                };

                testWorker.setProgressMonitor(new ProgressDialog((Dialog)SwingUtilities.getWindowAncestor(EMailOptionsPanel.this), _("Test connection"), null).progressPanel);
                testWorker.startWork(SwingUtilities.getWindowAncestor(EMailOptionsPanel.this), _("Sending test mail..."));
            }
        };

        textHost = new JTextField();
        textPort = new JTextField();
        textPort.setInputVerifier(new IntVerifier(1,65535));
        textUser = new JTextField();

        passwordField = new JPasswordField();

        checkSSL = new JCheckBox(_("Use SSL"));
        checkTLS = new JCheckBox(_("Use TLS if available"));
        checkAuth = new JCheckBox(_("Use authentication"));

        double[][] dLay = {
                {OptionsWin.border, 0.25, OptionsWin.border, 0.125, OptionsWin.border, 0.125, TableLayout.FILL, OptionsWin.border},
                {OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.FILL, OptionsWin.border}
        };
        setLayout(new TableLayout(dLay));

        Utils.addWithLabel(this, textHost, _("Server host name:"), "1,2,f,c");
        Utils.addWithLabel(this, textPort, _("Port:"), "3,2,f,c");
        add(checkSSL, "5,2,l,c");

        add(checkAuth, "1,4,l,c");
        add(checkTLS, "3,4,5,4,l,c");
        Utils.addWithLabel(this, textUser, _("User name:"), "1,7,f,c");
        Utils.addWithLabel(this, passwordField, _("Password:"), "3,7,5,7,f,c");
        add(new JButton(testAction), "1,9,f,f");
    }


    void saveSettings(EMailOptions eo) {
        eo.hostname = textHost.getText();
        eo.port = getPort();
        eo.user = textUser.getText();

        eo.ssl = checkSSL.isSelected();
        eo.auth = checkAuth.isSelected();
        eo.tls = checkTLS.isSelected();
    }

    private int getPort() {
        try {
            return Integer.parseInt(textPort.getText());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#loadSettings(yajhfc.FaxOptions)
     */
    public void loadSettings(FaxOptions foEdit) {    	
        EMailOptions eo = EntryPoint.getOptions();
        textHost.setText(eo.hostname);
        textPort.setText(String.valueOf(eo.port));
        textUser.setText(eo.user);

        checkSSL.setSelected(eo.ssl);
        checkAuth.setSelected(eo.auth);
        checkTLS.setSelected(eo.tls);
    }

    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#saveSettings(yajhfc.FaxOptions)
     */
    public void saveSettings(FaxOptions foEdit) {
        EMailOptions eo = EntryPoint.getOptions();
        saveSettings(eo);
    }

    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#validateSettings(yajhfc.options.OptionsWin)
     */
    public boolean validateSettings(OptionsWin optionsWin) {
        int val = getPort();
        if (val < 1 || val > 65535) {
            JOptionPane.showMessageDialog(this, "Please enter a port between 1 and 65535!");
            return false;
        }

        return true;
    }

}
