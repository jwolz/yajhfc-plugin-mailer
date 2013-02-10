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

import static yajhfc.send.email.EntryPoint._;
import info.clearthought.layout.TableLayout;

import java.awt.Font;
import java.io.File;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import yajhfc.FaxOptions;
import yajhfc.FileTextField;
import yajhfc.Utils;
import yajhfc.options.AbstractOptionsPanel;
import yajhfc.options.OptionsWin;
import yajhfc.send.email.EntryPoint;
import yajhfc.util.ComponentEnabler;
import yajhfc.util.IntVerifier;

public class BatchPortOptionsPanel extends AbstractOptionsPanel<FaxOptions> {   

    public BatchPortOptionsPanel() {
        super(false);
    }

    JTextField textHost, textPort, textSubject;
    JTextArea textComment;
    FileTextField ftfSuccessDir, ftfFailDir;
    
    JCheckBox checkEnable, checkEnableSuccessDir, checkEnableFailDir, checkEnableMailer;

    @Override
    protected void createOptionsUI() { 
       

        textHost = new JTextField();
        textPort = new JTextField();
        textPort.setInputVerifier(new IntVerifier(1,65535));
        textSubject = new JTextField();
        textComment = new JTextArea();
        textComment.setFont(new Font("DialogInput", Font.PLAIN, 12));
        
        ftfSuccessDir = new FileTextField();
        ftfSuccessDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        ftfFailDir = new FileTextField();
        ftfFailDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        checkEnable = new JCheckBox(_("Enable batch printer port"));
        checkEnableSuccessDir = new JCheckBox(_("Save successfully submitted jobs to the following directory:"));
        checkEnableFailDir = new JCheckBox(_("Save unsuccessfully submitted jobs to the following directory:"));
        checkEnableMailer = new JCheckBox(_("Enable support for @@mailrecipient@@ tag"));

        double[][] dLay = {
                {OptionsWin.border, 0.25, OptionsWin.border, 0.25, OptionsWin.border, 0.25, OptionsWin.border, TableLayout.FILL, OptionsWin.border},
                {OptionsWin.border, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border,
                    TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.PREFERRED, OptionsWin.border, TableLayout.PREFERRED, TableLayout.FILL, OptionsWin.border}
        };
        setLayout(new TableLayout(dLay));

        add(checkEnable, "1,1,7,1,l,c");
        JLabel lblHost = Utils.addWithLabel(this, textHost, _("Listen address:"), "1,4,f,c");
        JLabel lblPort = Utils.addWithLabel(this, textPort, _("Port:"), "3,4,f,c");
        
        add(checkEnableSuccessDir, "1,6,7,6,l,c");
        add(ftfSuccessDir, "1,7,7,7,f,f");
        
        add(checkEnableFailDir, "1,9,7,9,l,c");
        add(ftfFailDir, "1,10,7,10,f,f");
        
        add(checkEnableMailer, "1,12,7,12,l,c");
        JLabel lblSubject = Utils.addWithLabel(this, textSubject, _("Subject:"), "1,15,7,15,f,c");
        JLabel lblComment = Utils.addWithLabel(this, new JScrollPane(textComment), _("Mail text:"), "1,18,7,18,f,f");

        ComponentEnabler.installOn(checkEnable, true, lblHost, lblPort, textHost, textPort);
        ComponentEnabler.installOn(checkEnableMailer, true, lblSubject, lblComment, textSubject, textComment);
        ComponentEnabler.installOn(checkEnableFailDir, true, ftfFailDir);
        ComponentEnabler.installOn(checkEnableSuccessDir, true, ftfSuccessDir);
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
        BatchPrinterOptions bpo = EntryPoint.getBatchPrintOptions();
        
        textHost.setText(bpo.bindAddress);
        textPort.setText(String.valueOf(bpo.port));
        textSubject.setText(bpo.subject);
        textComment.setText(bpo.comment);
        
        ftfFailDir.setText(bpo.failDir);
        ftfSuccessDir.setText(bpo.successDir);
        
        checkEnable.setSelected(bpo.enabled);
        checkEnableFailDir.setSelected(bpo.enableFailDir);
        checkEnableMailer.setSelected(bpo.enableMailer);
        checkEnableSuccessDir.setSelected(bpo.enableSuccessDir);
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
        
        bpo.failDir = ftfFailDir.getText();
        bpo.successDir = ftfSuccessDir.getText();
        
        bpo.enabled = checkEnable.isSelected();
        bpo.enableFailDir = checkEnableFailDir.isSelected();
        bpo.enableMailer = checkEnableMailer.isSelected();
        bpo.enableSuccessDir = checkEnableSuccessDir.isSelected();
        
        EntryPoint.reopenBatchThread();
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

        if (checkEnableFailDir.isSelected()) {
            if (!new File(ftfFailDir.getText()).isDirectory()) {
                JOptionPane.showMessageDialog(this, "Please enter an existing directory!");
                optionsWin.focusComponent(ftfFailDir.getJTextField());
                return false;
            }
        }
        
        if (checkEnableSuccessDir.isSelected()) {
            if (!new File(ftfSuccessDir.getText()).isDirectory()) {
                JOptionPane.showMessageDialog(this, "Please enter an existing directory!");
                optionsWin.focusComponent(ftfSuccessDir.getJTextField());
                return false;
            }
        }
        
        return true;
    }

}
