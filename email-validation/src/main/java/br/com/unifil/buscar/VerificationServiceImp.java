package br.com.unifil.buscar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import net.bytebuddy.utility.RandomString;

/**
 * VerificationServiceImp is the main service layer class, it implements the
 * {@link VerificationService} interface, it offers all the implementation required
 * to verify an email.
 * 
 * @author Felipe Torres
 * @version 1.0
 * @since 1.0
 */
@Service(value = "VerificationServiceImp")
public class VerificationServiceImp implements VerificationService {

	@Value("${spring.mail.username}")
	private final String COMPANY_EMAIL_ADDRESS;
	@Value("${secrets.name}")
	private final String COMPANY_NAME;
	@Value("${secrets.baseUrl}")
	private final String BASE_URL;

	private final VerificationRepository repository;
	JavaMailSender mailSender;
	
	/**
	 * Class constructor specifying the dependencies to be injected.
	 * 
	 * @param repository A {@link VerificationRepository} implementation, as specified in the {@code @Qualifier} annotation. 
	 * @param mailSender A {@link JavaMailSender} implementation.
	 * @since 1.0
	 * */

	@Autowired
	public VerificationServiceImp(@Qualifier("RedisRepository") VerificationRepository repository, JavaMailSender mailSender) {
		this.COMPANY_EMAIL_ADDRESS = "";
		this.COMPANY_NAME = "";
		this.BASE_URL = "";
		this.repository = repository;
		this.mailSender = mailSender;
	}

	@Override
	public void sendVerificationEmail(EmailVerificationRecord record)
			throws DuplicatedRequestException, MessagingException {
		if (repository.isDuplicatedRequest(record.username()))
			throw new DuplicatedRequestException();

		repository.save(record);

		String verifyURL = BASE_URL + "/" + record.verificationCode() + ":" + record.username();
		String toAddress = record.email();
		String fromAddress = COMPANY_EMAIL_ADDRESS;
		String senderName = COMPANY_NAME;
		String subject = "Please verify your registration";
		String content = "Dear [[name]],<br>" + "Please click the link below to verify your registration:<br>"
				+ "<h3><a href=\"[[URL]]\" target=\"_self\">VERIFY</a></h3><br>[[URL]]<br>" + "Thank you,<br>" + "[[senderName]].";

		MimeMessage message = mailSender.createMimeMessage();
		MimeMessageHelper helper = new MimeMessageHelper(message);		
		
		helper.setFrom(fromAddress);
		helper.setTo(toAddress);
		helper.setSubject(subject);

		content = content.replace("[[URL]]", verifyURL).replace("[[senderName]]", senderName).replace("[[name]]",
				record.username());

		helper.setText(content, true);
		mailSender.send(message);
	}

	@Override
	public boolean processRequest(String code, String username) {
		try {

			EmailVerificationRecord userRequest = getByUsername(username);
			if (!code.equals(userRequest.verificationCode()))
				return false;
			repository.delete(username);
			System.out.println("Email was Validated");
			return true;

		} catch (IllegalArgumentException e) {
			return false;
		}
	}
	
	@Override
	public EmailVerificationRecord getByUsername(String username) throws IllegalArgumentException {

		return repository.getByUsername(username).orElseThrow(() -> new IllegalArgumentException());
	}
	
	@Override
	public String generateVerificationCode() {
		return RandomString.make(30);
	}
}
