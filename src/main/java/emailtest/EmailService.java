package emailtest;

import emailtest.article.Article;
import emailtest.article.ArticleRepository;
import emailtest.member.Member;
import emailtest.member.MemberRepository;
import emailtest.newsletter.Newsletter;
import emailtest.newsletter.NewsletterRepository;
import jakarta.mail.Address;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.BreakIterator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private static final int WORDS_PER_MINUTE = 200;

    private final MemberRepository memberRepository;
    private final ArticleRepository articleRepository;
    private final NewsletterRepository newsletterRepository;

    /**
     * Maildir에서 읽어온 파일을 파싱 후 DB에 저장하고,
     * 성공 시 해당 파일을 삭제한다.
     */
    public void processMailFile(File emlFile) {
        try (InputStream is = new FileInputStream(emlFile)) {
            Session session = Session.getDefaultInstance(new Properties());
            MimeMessage msg = new MimeMessage(session, is);

            // Email 엔티티에 값 매핑
            Email email = new Email();
            Address[] fromAddrs = msg.getFrom();
            if (fromAddrs == null && fromAddrs.length == 0) {
                log.debug("From 주소가 없어 메일을 무시합니다: " + msg.getSubject());
                return;
            }
            Address toAddress = Arrays.stream(msg.getRecipients(RecipientType.TO)).findFirst().get();
            Member member = memberRepository.findByEmail(toAddress.getType())
                    .orElse(null);
            Newsletter newsletter = newsletterRepository.findByEmail(String.valueOf(Arrays.stream(fromAddrs).findFirst()))
                    .orElse(null);

            // 본문 파싱: text/plain 과 text/html 처리
            String contents = extractContents(msg);

            final Article article = Article.builder()
                    .title(msg.getSubject())
                    .contents(contents)
                    .expectedReadTime(calculateReadingTimeFromText(contents))
                    .contentsSummary(sliceContents(contents))
                    .memberId(0L)
                    .newsletterId(0L)
                    .arrivedDateTime(LocalDateTime.now(ZoneId.of("Asia/Seoul")))
                    .build();

            articleRepository.save(article);

            log.info("Saved email from={} subject={}", email.getFromAddress(), email.getSubject());

        } catch (Exception e) {
            // 파싱/저장 중 오류 발생 시 로그만 남기고 파일은 보존
            log.error("Failed to process mail file: {}", emlFile.getName(), e);
            return;
        }

        deleteEmailFile(emlFile);
    }

    private String extractContents(MimeMessage msg) {
        try {
            Object content = msg.getContent();

            if (content instanceof String) {
                return (String) content;
            }

            if (content instanceof Multipart multipart) {
                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/html")) {
                        return (String) part.getContent();
                    }
                }

                for (int i = 0; i < multipart.getCount(); i++) {
                    BodyPart part = multipart.getBodyPart(i);
                    if (part.isMimeType("text/plain")) {
                        return (String) part.getContent();
                    }
                }
            }
        } catch (IOException | MessagingException e) {
            log.warn("본문 추출 중 오류 발생: {}", e.getMessage());
        }

        return "";
    }

    private void deleteEmailFile(File emailFile) {
        try {
            Files.delete(emailFile.toPath());
            log.debug("Deleted processed mail file: {}", emailFile.getName());
        } catch (IOException e) {
            log.warn("Failed to delete processed mail file: {} - exists: {}, canWrite: {}, isFile: {}, Reason: {}",
                    emailFile.getAbsolutePath(),
                    emailFile.exists(),
                    emailFile.canWrite(),
                    emailFile.isFile(),
                    e.getMessage());
        }
    }

    private int calculateReadingTimeFromText(String fullText) {
        if (fullText == null || fullText.trim().isEmpty()) {
            return 0;
        }
        // HTML 태그 제거 및 단어 수 세기
        String textOnly = Jsoup.parse(fullText).text().trim();
        int wordCount = textOnly.split("\\s+").length;
        // 올림 계산
        int minutes = (int) Math.ceil((double) wordCount / WORDS_PER_MINUTE);
        return Math.max(minutes, 1);
    }

    private String sliceContents(String contents) {
        if (contents == null || contents.isBlank()) {
            return "";
        }

        String textOnly = Jsoup.parse(contents).text(); // HTML 제거
        return textOnly.length() <= 100 ? textOnly : textOnly.substring(0, 100) + "...";
    }
}
