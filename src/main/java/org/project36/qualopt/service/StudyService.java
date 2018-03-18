package org.project36.qualopt.service;

import org.apache.commons.lang3.CharEncoding;
import org.project36.qualopt.domain.Participant;
import org.project36.qualopt.domain.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Objects;
import java.util.Properties;

@Service
public class StudyService {

    private static final Logger log = LoggerFactory.getLogger(StudyService.class);
    public static final String CUSTOM_LAST_NAME = "--lastName";
    public static final String CUSTOM_FIRST_NAME = "--firstName";
    public static final String CUSTOM_LOCATION = "--location";
    public static final String CUSTOM_OCCUPATION = "--occupation";
    public static final String CUSTOM_PROGRAMMING_LANGUAGE = "--programmingLanguage";
    public static final String CUSTOM_NUMBER_OF_CONTRIBUTIONS = "--numberOfContributions";
    public static final String CUSTOM_NUMBER_OF_REPOSITORIES = "--numberOfRepositories";

    private final JavaMailSenderImpl javaMailSender;

    public StudyService(JavaMailSenderImpl javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    /**
     * This method sends invitation emails to study participants asynchronously.
     * @param study
     */
    @Async
    public void sendInvitationEmail(Study study){
        log.debug("Sending invitation email for study '{}'", study);

        String subject = study.getEmailSubject();
        String content = Objects.isNull(study.getEmailBody()) ? "" : study.getEmailBody();
        String userEmail = study.getUser().getEmail();
        try {
            javaMailSender.setSession(getUserEmailSession());
            MimeMessage message = javaMailSender.createMimeMessage();
            message.setFrom(new InternetAddress(userEmail));
            for (Participant participant : study.getParticipants()) {
                InternetAddress participantEmailAddress;
                try {
                     participantEmailAddress = new InternetAddress(participant.getEmail());
                } catch (AddressException e) {
                    log.error("Failed to create internet address from participant email", e);
                    throw new RuntimeException(e);
                }
                message.addRecipient(Message.RecipientType.TO, participantEmailAddress);
                String customisedContent = customiseEmailText(participant, content);
                String customisedSubject = customiseEmailText(participant, subject);
                message.setSubject(customisedSubject, CharEncoding.UTF_8);
                message.setText(customisedContent, CharEncoding.UTF_8);
                javaMailSender.send(message);
                log.debug("Sent invitation email for study '{}'", study);
            }

        } catch (MessagingException e) {
            log.error("Failed to send invitation email", e);
        }
    }

    /**
     * This method goes through text and customises it with the participants personal details.
     *
     * @param participant
     * @param content
     * @return customisedContent
     */
    private String customiseEmailText(Participant participant, String content) {
        String customisedContent = content.replaceAll(CUSTOM_FIRST_NAME, participant.getFirstName());
        customisedContent = customisedContent.replaceAll(CUSTOM_LAST_NAME, participant.getLastName());
        customisedContent = customisedContent.replaceAll(CUSTOM_LOCATION, participant.getLocation());
        customisedContent = customisedContent.replaceAll(CUSTOM_OCCUPATION, participant.getOccupation());
        customisedContent = customisedContent.replaceAll(CUSTOM_PROGRAMMING_LANGUAGE, participant.getProgrammingLanguage());
        customisedContent = customisedContent.replaceAll(CUSTOM_NUMBER_OF_CONTRIBUTIONS, participant.getNumberOfContributions() + "");
        customisedContent = customisedContent.replaceAll(CUSTOM_NUMBER_OF_REPOSITORIES, participant.getNumberOfRepositories() + "");
        return customisedContent;
    }

    private Session getUserEmailSession(){
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        // trust the host gmail; prevents antivirus from blocking emails being sent
        props.put("mail.smtp.ssl.trust", "smtp.gmail.com");
        return Session.getInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    // dev test gmail account for proof of concept
                    // TODO authenticate user email and use that instead. i.e. study.getUser().getEmail()
                    return new PasswordAuthentication("tt7199425@gmail.com", "testemail123");
                }
            });
    }
}
