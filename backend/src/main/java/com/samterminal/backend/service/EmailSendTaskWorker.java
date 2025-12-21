package com.samterminal.backend.service;

import com.samterminal.backend.config.EmailVerificationProperties;
import com.samterminal.backend.entity.EmailSendLog;
import com.samterminal.backend.entity.EmailSendLogStatus;
import com.samterminal.backend.entity.EmailSendTask;
import com.samterminal.backend.entity.EmailSendTaskStatus;
import com.samterminal.backend.repository.EmailSendLogRepository;
import com.samterminal.backend.repository.EmailSendTaskRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailSendTaskWorker {
    private final EmailSendTaskRepository taskRepository;
    private final EmailSendLogRepository logRepository;
    private final SmtpPoolService smtpPoolService;
    private final EmailVerificationProperties properties;
    private final EmailCryptoService cryptoService;
    private final Clock clock;

    public EmailSendTaskWorker(EmailSendTaskRepository taskRepository,
                               EmailSendLogRepository logRepository,
                               SmtpPoolService smtpPoolService,
                               EmailVerificationProperties properties,
                               EmailCryptoService cryptoService,
                               Clock clock) {
        this.taskRepository = taskRepository;
        this.logRepository = logRepository;
        this.smtpPoolService = smtpPoolService;
        this.properties = properties;
        this.cryptoService = cryptoService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${email.send-task.worker-delay-seconds:2}000")
    @Transactional
    public void processTasks() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<EmailSendTask> tasks = taskRepository.findReadyTasks(
                List.of(EmailSendTaskStatus.PENDING, EmailSendTaskStatus.FAILED), now);
        int processed = 0;
        for (EmailSendTask task : tasks) {
            if (processed >= properties.getSendTask().getBatchSize()) {
                break;
            }
            processed++;
            task.setStatus(EmailSendTaskStatus.SENDING);
            task.setUpdatedAt(now);
            taskRepository.save(task);

            EmailSendLog log = logRepository.findTopByRequestIdOrderByCreatedAtDesc(task.getRequestId());
            try {
                String code = task.getCodeEncrypted() != null ? cryptoService.decrypt(task.getCodeEncrypted()) : null;
                var smtpConfig = smtpPoolService.sendWithFailover(task.getEmail(),
                        "S.A.M. 注册验证码",
                        buildEmailBody(task.getUsername(), task.getEmail(), task.getRequestId(), code));
                task.setStatus(EmailSendTaskStatus.SENT);
                task.setUpdatedAt(LocalDateTime.now(clock));
                taskRepository.save(task);

                if (log != null) {
                    log.setStatus(EmailSendLogStatus.SENT);
                    log.setSmtpId(smtpConfig.getId());
                    if (log.getSentAt() == null) {
                        log.setSentAt(LocalDateTime.now(clock));
                    }
                    log.setErrorMessage(null);
                    log.setUpdatedAt(LocalDateTime.now(clock));
                    logRepository.save(log);
                }
            } catch (Exception ex) {
                handleFailure(task, log, ex.getMessage());
            }
        }
    }

    private void handleFailure(EmailSendTask task, EmailSendLog log, String message) {
        int attempts = task.getAttemptCount() == null ? 0 : task.getAttemptCount();
        attempts++;
        task.setAttemptCount(attempts);
        task.setLastError(message);
        LocalDateTime now = LocalDateTime.now(clock);
        if (attempts >= properties.getSendTask().getMaxAttempts()) {
            task.setStatus(EmailSendTaskStatus.FAILED);
        } else {
            task.setStatus(EmailSendTaskStatus.FAILED);
            task.setNextAttemptAt(now.plusSeconds(nextBackoffSeconds(attempts)));
        }
        task.setUpdatedAt(now);
        taskRepository.save(task);

        if (log != null) {
            log.setStatus(EmailSendLogStatus.FAILED);
            log.setErrorMessage(message);
            if (log.getSentAt() == null) {
                log.setSentAt(now);
            }
            log.setUpdatedAt(now);
            logRepository.save(log);
        }
    }

    private long nextBackoffSeconds(int attempts) {
        long base = properties.getSendTask().getInitialBackoffSeconds();
        long max = properties.getSendTask().getMaxBackoffSeconds();
        long delay = (long) (base * Math.pow(2, Math.max(0, attempts - 1)));
        return Math.min(delay, max);
    }

    private String buildEmailBody(String username, String email, String requestId, String code) {
        return "您好 " + username + "，\n\n" +
                "您正在注册 S.A.M. Terminal。请在 5 分钟内完成验证码校验。" + "\n" +
                "验证码: " + (code != null ? code : "******") + "\n" +
                "验证码请在页面输入框中填写。" + "\n\n" +
                "若非本人操作，请忽略该邮件。\n" +
                "请求编号: " + requestId;
    }
}
