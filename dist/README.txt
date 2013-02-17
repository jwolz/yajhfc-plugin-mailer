README for YajHFC Batch Printer and Mail plugin
===============================================

The YajHFC batch printer and mail plugin adds the option to add a batch printer port to YajHFC.
Additionally it offers the option to send e-mail messages if the fax contains a @@mailrecipient@@ tag.

If you want to use the email feature, you will have to configure your SMTP server settings in the Options dialog under "Batch printer port"->"SMTP settings".

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

6. By default, YajHFC will now listen to connections on TCP port 19101. 
   This port can be used as AppSocket port (socket: protocol) in CUPS or standard TCP/IP port in Windows to create a virtual fax printer.
   To do so, simply create a printer printing to port 19101 on localhost with a printer driver that outputs PostScript.
   