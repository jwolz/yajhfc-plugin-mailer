README for YajHFC Batch Printer and Mail plugin
===============================================

The YajHFC batch printer and mail plugin adds the option to add a batch printer port to YajHFC.

It also offers the option to send e-mail messages instead of a fax if the print job contains a @@mailrecipient@@ tag.

Differences to the "normal" TCP/IP printer port:
- The batch printer port will never show a send dialog
- Thus print jobs must include a @@recipient@@ tag
- When an error occurs the fax is silently dropped by default (there is also an option to save such faxes or send a mail in that case)
- Generally the batch printer port is designed to run without any user interaction
- Support for the @@mailrecipient@@ tag


INSTALLATION
-------------

If you use the Windows setup:
After installing YajHFC, install this plugin using the yajhfc-X_Y_Z-plugin-mail-setup.exe.
This automatically creates a new printer using the batch printer port for you.

Else:
1. Unpack this ZIP file somewhere.

2. Start YajHFC, go to Options->Plugins&JDBC and click "Add plugin".

3. Select yajhfc-plugin-mail.jar 

4. Restart YajHFC

5. If it worked, you should now have a new panel in the Options dialog called "Batch printer port". 

By default, YajHFC will now listen to connections on TCP port 19101. 
This port can be used as AppSocket port (socket: protocol) in CUPS or standard TCP/IP port in Windows to create a virtual fax printer.
To do so, simply create a printer printing to port 19101 on localhost with a printer driver that outputs PostScript.


CONFIGURATION
-------------

To configure the batch printer port go to the panel "Batch printer port" in the Options dialog,

In this panel you can do the following things:
- configure the port YajHFC listens on
- set directories to save successful/failed faxes in
- set a email address to send failed faxes to

- enable/disable the @@mailrecipient@@ tag, i.e. enable the email feature
  If you want to use the email feature, you will have to configure your SMTP server settings first 
  under "Batch printer port"->"SMTP settings".
  
- If you have enabled the email feature, you can also set the subject and body (i.e. text) of the mails sent.
- You can also set an email address that receives a copy of the mails sent (either an exact copy as BCC or a mail including a log) 


 
   