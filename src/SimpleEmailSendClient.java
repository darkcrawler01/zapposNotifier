
import java.io.FileInputStream;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;


public class SimpleEmailSendClient
{
	private static Session session;
	private static boolean setup() {		
		try
		{
			Properties props = new Properties();
			props.load(new FileInputStream("config.properties"));
			final String username = props.getProperty("username");
			final String password = props.getProperty("password");

			props.put("mail.smtp.auth", "true");
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.host", props.getProperty("smtp.host"));
			props.put("mail.smtp.port", props.getProperty("smtp.port"));

			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(username, password);
				}
			});
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean sendMail(String from, String subject,
			String message , String recipient){
		
		if (session == null)
		{
			setup();
		}
		try {

			MimeMessage msg = new MimeMessage(session);

			msg.setFrom(new InternetAddress(from));
			msg.setRecipients(Message.RecipientType.TO,
					InternetAddress.parse(recipient));
			msg.setSubject(subject);

			msg.setContent(message, "text/html; charset=utf-8");

			Transport.send(msg);

			System.out.println(new Date() +":Mail sent to " + recipient);
			return true;

		} catch (MessagingException e) {
			e.printStackTrace();
		}
		return false;
	}

}