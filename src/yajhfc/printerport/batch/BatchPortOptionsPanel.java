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
package yajhfc.printerport.batch;

import static yajhfc.send.email.i18n.Msgs._;
import info.clearthought.layout.TableLayout;

import java.awt.Font;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import yajhfc.FaxOptions;
import yajhfc.FileTextField;
import yajhfc.SenderIdentity;
import yajhfc.Utils;
import yajhfc.options.AbstractOptionsPanel;
import yajhfc.options.CoverPanel;
import yajhfc.options.OptionsPage;
import yajhfc.options.OptionsWin;
import yajhfc.options.PanelTreeNode;
import yajhfc.options.ServerSettingsPanel;
import yajhfc.send.email.EntryPoint;
import yajhfc.server.ServerOptions;
import yajhfc.util.ComponentEnabler;
import yajhfc.util.IntVerifier;
import yajhfc.util.WrapperComboBoxModel;

public class BatchPortOptionsPanel extends AbstractOptionsPanel<FaxOptions> {   

    public BatchPortOptionsPanel() {
        super(false);
    }

    JTextField textHost, textPort, textSubject, textFailMail;
    JTextArea textComment;
    FileTextField ftfSuccessDir, ftfFailDir;
    
    JCheckBox checkEnable, checkEnableSuccessDir, checkEnableFailDir, checkEnableMailer, checkEnableFailMail;
    
    JComboBox comboServer, comboIdentity;

    ServerOptions selServer;
    SenderIdentity selIdentity;
    boolean combosInitialized = false;
    
    @SuppressWarnings("unchecked")
    private static <T extends OptionsPage<FaxOptions>> T findOptionsPage(PanelTreeNode root, Class<T> panelClass) {
        if (panelClass.isInstance(root.getOptionsPage())) {
            return (T)root.getOptionsPage();
        }
        if (root.getChildren() != null) {
            for (PanelTreeNode c : root.getChildren()) {
                T res = findOptionsPage(c, panelClass);
                if (res != null)
                    return res;
            }
        }
        return null;
    }
    
    @Override
    protected void createOptionsUI() { 
        textHost = new JTextField();
        textPort = new JTextField();
        textPort.setInputVerifier(new IntVerifier(1,65535));
        textSubject = new JTextField();
        textComment = new JTextArea();
        textComment.setFont(new Font("DialogInput", Font.PLAIN, 12));
        textFailMail = new JTextField();
        
        ftfSuccessDir = new FileTextField();
        ftfSuccessDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ftfFailDir = new FileTextField();
        ftfFailDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        checkEnable = new JCheckBox(_("Enable batch printer port"));
        checkEnableSuccessDir = new JCheckBox(_("Save successfully submitted jobs to the following directory:"));
        checkEnableFailDir = new JCheckBox(_("Save unsuccessfully submitted jobs to the following directory:"));
        checkEnableMailer = new JCheckBox(_("Enable support for @@mailrecipient@@ tag"));
        checkEnableFailMail = new JCheckBox(_("Send unsuccessfully submitted jobs to the following e-mail address:"));
        
        comboServer = new JComboBox();
        comboIdentity = new JComboBox();

        double[][] dLay = {
                {OptionsWin.border, 0.25, OptionsWin.border, 0.25, OptionsWin.border, 0.25, OptionsWin.border, TableLayout.FILL, OptionsWin.border},
                {OptionsWin.border, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.FILL, OptionsWin.border}
        };
        setLayout(new TableLayout(dLay));

        add(checkEnable, "1,1,7,1,l,c");
        JLabel lblHost = Utils.addWithLabel(this, textHost, _("Bind address:"), "1,4,f,c");
        JLabel lblPort = Utils.addWithLabel(this, textPort, _("Port:"), "3,4,f,c");
        
        Utils.addWithLabel(this, comboServer, _("Server:"), "1,7,3,7,f,c");
        Utils.addWithLabel(this, comboIdentity, _("Identity:"), "5,7,7,7,f,c");
        
        add(checkEnableSuccessDir, "1,9,7,9,l,c");
        add(ftfSuccessDir, "1,10,7,10,f,f");
        
        add(checkEnableFailDir, "1,12,3,12,l,c");
        add(ftfFailDir, "1,13,3,13,f,f");
        add(checkEnableFailMail, "5,12,7,12,l,c");
        add(textFailMail, "5,13,7,13,f,f");
        
        
        add(checkEnableMailer, "1,15,7,15,l,c");
        JLabel lblSubject = Utils.addWithLabel(this, textSubject, _("Subject:"), "1,18,7,18,f,c");
        JLabel lblComment = Utils.addWithLabel(this, new JScrollPane(textComment), _("Mail text:"), "1,21,7,21,f,f");

        ComponentEnabler.installOn(checkEnable, true, lblHost, lblPort, textHost, textPort, comboServer, comboIdentity);
        ComponentEnabler.installOn(checkEnableMailer, true, lblSubject, lblComment, textSubject, textComment);
        ComponentEnabler.installOn(checkEnableFailDir, true, ftfFailDir);
        ComponentEnabler.installOn(checkEnableSuccessDir, true, ftfSuccessDir);
        ComponentEnabler.installOn(checkEnableFailMail, true, textFailMail);
    }

    @Override
    public void pageIsShown(OptionsWin optionsWin) {
        if (!combosInitialized) {
            OptionsWin ow = (OptionsWin)SwingUtilities.getWindowAncestor(this);

            ServerSettingsPanel serverPanel = findOptionsPage(ow.getRootNode(), ServerSettingsPanel.class);
            comboServer.setModel(new WrapperComboBoxModel(serverPanel.getServers()));
            comboServer.setSelectedItem(selServer);

            CoverPanel covPanel = findOptionsPage(ow.getRootNode(), CoverPanel.class);
            comboIdentity.setModel(new WrapperComboBoxModel(covPanel.getListModel()));
            comboIdentity.setSelectedItem(selIdentity);
            
            combosInitialized = true;
        }
    }

    private int getPort() {
        try {
            return Integer.parseInt(textPort.getText());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private SenderIdentity getSelIdentity() {
        if (combosInitialized) {
            return (SenderIdentity)comboIdentity.getSelectedItem();
        } else {
            return selIdentity;
        }
    }
    
    private ServerOptions getSelServer() {
        if (combosInitialized) {
            return (ServerOptions)comboServer.getSelectedItem();
        } else {
            return selServer;
        }
    }
    
    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#loadSettings(yajhfc.FaxOptions)
     */
    public void loadSettings(FaxOptions foEdit) {    	
        BatchPrinterOptions bpo = EntryPoint.getBatchPrintOptions();
        
        textHost.setText(bpo.bindAddress);
        textPort.setText(String.valueOf(bpo.port));
        textSubject.setText(bpo.subject);
        textComment.setText(bpo.comment);
        textFailMail.setText(bpo.failRecipient);
        
        ftfFailDir.setText(bpo.failDir);
        ftfSuccessDir.setText(bpo.successDir);
        
        checkEnable.setSelected(bpo.enabled);
        checkEnableFailDir.setSelected(bpo.enableFailDir);
        checkEnableMailer.setSelected(bpo.enableMailer);
        checkEnableSuccessDir.setSelected(bpo.enableSuccessDir);
        checkEnableFailMail.setSelected(bpo.enableFailMail);
        
        selServer = bpo.getServer().getOptions();
        selIdentity = bpo.getIdentity();
    }

    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#saveSettings(yajhfc.FaxOptions)
     */
    public void saveSettings(FaxOptions foEdit) {
        BatchPrinterOptions bpo = EntryPoint.getBatchPrintOptions();
        
        bpo.bindAddress = textHost.getText();
        bpo.port = getPort();
        bpo.subject = textSubject.getText();
        bpo.comment = textComment.getText();
        bpo.failRecipient = textFailMail.getText();
        
        bpo.failDir = ftfFailDir.getText();
        bpo.successDir = ftfSuccessDir.getText();
        
        bpo.enabled = checkEnable.isSelected();
        bpo.enableFailDir = checkEnableFailDir.isSelected();
        bpo.enableMailer = checkEnableMailer.isSelected();
        bpo.enableSuccessDir = checkEnableSuccessDir.isSelected();
        bpo.enableFailMail = checkEnableFailMail.isSelected();
        
        if (combosInitialized) {
            bpo.identityID = getSelIdentity().id;
            bpo.serverID = getSelServer().id;
        }
        
        EntryPoint.reopenBatchThread();
    }

    /* (non-Javadoc)
     * @see yajhfc.options.OptionsPage#validateSettings(yajhfc.options.OptionsWin)
     */
    public boolean validateSettings(OptionsWin optionsWin) {
        int val = getPort();
        if (val < 1 || val > 65535) {
            JOptionPane.showMessageDialog(this, _("Please enter a port between 1 and 65535!"));
            return false;
        }

        if (checkEnableFailDir.isSelected()) {
            if (!new File(ftfFailDir.getText()).isDirectory()) {
                JOptionPane.showMessageDialog(this, _("Please enter an existing directory!"));
                optionsWin.focusComponent(ftfFailDir.getJTextField());
                return false;
            }
        }
        
        if (checkEnableSuccessDir.isSelected()) {
            if (!new File(ftfSuccessDir.getText()).isDirectory()) {
                JOptionPane.showMessageDialog(this, _("Please enter an existing directory!"));
                optionsWin.focusComponent(ftfSuccessDir.getJTextField());
                return false;
            }
        }
        
        if (checkEnableFailMail.isSelected()) {
            if (!textFailMail.getText().contains("@")) {
                JOptionPane.showMessageDialog(this, _("Please enter a e-mail address!"));
                optionsWin.focusComponent(textFailMail);
                return false;
            }
        }
        
        return true;
    }

}
