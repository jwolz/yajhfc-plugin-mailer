package yajhfc.send.email;

import java.util.Properties;

import yajhfc.AbstractFaxOptions;
import yajhfc.Password;


public class EMailOptions extends AbstractFaxOptions {
    public String hostname = "localhost";
    public int port = 25;
    public boolean ssl = false;
    public boolean tls = true;
    public boolean auth = false;
    public String user = "";
    public final Password password = new Password();
    public boolean trustAllHosts = false;
	
    public Properties toProperties() {
        Properties p = new Properties();
        p.put("mail.smtp.host", hostname);
        p.put("mail.smtp.port", port);
        p.put("mail.smtp.auth", auth);
        if (user != null && user.length() > 0)
            p.put("mail.smtp.user", user);
        
        p.put("mail.smtp.ssl.enable", ssl);
        p.put("mail.smtp.starttls.enable", tls);
        
        if (trustAllHosts)
            p.put("mail.smtp.ssl.trust", "*");
        
        return p;
    }
    
	/**
	 * Call the super constructor with the prefix that should be prepended
	 * to the options name.
	 */
	public EMailOptions() {
		super("mailsender");
	}
}
