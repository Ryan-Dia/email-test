package emailtest;

import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final EmailRepository repository;

    public EmailService(EmailRepository repository) {
        this.repository = repository;
    }

    /**
     * Maildir에서 읽어온 파일을 파싱 후 DB에 저장하고,
     * 성공 시 해당 파일을 삭제합니다.
     */
    public void processMailFile(File emlFile) {
        try (InputStream is = new FileInputStream(emlFile)) {
            // JavaMail 세션 생성
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage msg = new MimeMessage(session, is);

            // Email 엔티티에 값 매핑
            Email email = new Email();
            Address[] fromAddrs = msg.getFrom();
            if (fromAddrs != null && fromAddrs.length > 0) {
                email.setFromAddress(((InternetAddress) fromAddrs[0]).getAddress());
            }
            email.setSubject(msg.getSubject());
            Date receivedDate = msg.getReceivedDate();
            if (receivedDate != null) {
                email.setReceivedAt(
                        receivedDate.toInstant()
                                .atZone(ZoneId.of("Asia/Seoul"))
                                .toLocalDateTime()
                );
            } else {
                log.warn("메일에 ReceivedDate가 없어 현재 시간으로 대체합니다: {}", msg.getSubject());
                email.setReceivedAt(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
            }

            // 본문 파싱: text/plain 과 text/html 처리
            Object content = msg.getContent();
            if (content instanceof String) {
                email.setTextBody((String) content);
            } else if (content instanceof Multipart) {
                Multipart mp = (Multipart) content;
                for (int i = 0; i < mp.getCount(); i++) {
                    BodyPart part = mp.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        email.setTextBody((String) part.getContent());
                    } else if (part.isMimeType("text/html")) {
                        email.setHtmlBody((String) part.getContent());
                    }
                }
            }

            // DB 저장
            repository.save(email);
            log.info("Saved email from={} subject={}", email.getFromAddress(), email.getSubject());

        } catch (Exception e) {
            // 파싱/저장 중 오류 발생 시 로그만 남기고 파일은 보존
            log.error("Failed to process mail file: {}", emlFile.getName(), e);
            return;
        }

        // 파싱·저장 성공 시 원본 파일 삭제
        boolean deleted = emlFile.delete();
        if (!deleted) {
            log.warn("Failed to delete processed mail file: {}", emlFile.getAbsolutePath());
        } else {
            log.debug("Deleted processed mail file: {}", emlFile.getName());
        }
    }
}
