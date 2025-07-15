package emailtest;

import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;




@Service
@RequiredArgsConstructor
public class EmailService {

    private final EmailRepository repository;

    public void processMailFile(File emlFile) {
        try (InputStream is = new FileInputStream(emlFile)) {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage message = new MimeMessage(session, is);

            Email email = new Email();
            email.setFromAddress(message.getFrom()[0].toString());
            email.setSubject(message.getSubject());
            email.setReceivedAt(
                    LocalDateTime.ofInstant(
                            message.getReceivedDate().toInstant(),
                            ZoneId.of("Asia/Seoul"))
            );

            Object content = message.getContent();
            if (content instanceof String) {
                email.setTextBody((String) content);
            } else if (content instanceof Multipart) {
                Multipart mp = (Multipart) content;
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart bp = mp.getBodyPart(i);
                    String ct = bp.getContentType();
                    if (ct.startsWith("text/plain")) {
                        email.setTextBody((String) bp.getContent());
                    } else if (ct.startsWith("text/html")) {
                        email.setHtmlBody((String) bp.getContent());
                    }
                }
            }

            repository.save(email);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            emlFile.delete();
        }
    }
}
