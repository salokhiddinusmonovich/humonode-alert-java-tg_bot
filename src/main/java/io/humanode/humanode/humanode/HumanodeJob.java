package io.humanode.humanode.humanode;

import io.humanode.humanode.bot.JarvisTelegramBotAPI;
import io.humanode.humanode.cache.StaticCache;
import io.humanode.humanode.dtos.BioAuthStatusDTO;
import io.humanode.humanode.exceptions.HumanodeException;
import io.humanode.humanode.utils.CustomSpringEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class HumanodeJob implements ApplicationListener<CustomSpringEvent> {

    private static final String bioAuthBody = """
            {
                "jsonrpc": "2.0",
                "id": 1,
                "method": "bioauth_status",
                "params": []
            }""";
    private final StaticCache staticCache;
    private final JarvisTelegramBotAPI jarvisTelegramBotAPI;
    private final HumanodeFeignClient client;
    @Value("${humanode.path.auth.cmd}")
    private String authCmd;
    @Value("${humanode.path.tunnel.cmd}")
    private String tunnelCmd;
    private static String url = null;

    @Scheduled(cron = "0 */1 * * * *")
    public void checkHumanodeHealthAndBioAuth() {
        log.info("Humanode sog'lig'ini va BioAuth holatini tekshirishga urunib ko'rayapman");

        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(getBioAuthTime() / 1000), staticCache.getTimeZoneId()
        );

        log.info("Humanode ishga tushdi");

        LocalDateTime now = LocalDateTime.now(staticCache.getTimeZoneId());

        long remaining = now.until(expiresAt, ChronoUnit.MINUTES);

        if (remaining > 1) {
            url = null;
        }

        if (remaining <= 5) {
            log.info("Sizning BioAuth yaqinlashmoqda. Sizda {} minut qolgan", remaining);
            jarvisTelegramBotAPI.sendMessage(
                    String.format("Sizning BioAuth yaqinlashmoqda. Sizda %s minut qolgan", remaining)
            );
        }
    }

    @Scheduled(cron = "0 0 12 * * *")
    public void bioAuthInformation() {
        if (!staticCache.isEnableNotification()) {
            return;
        }

        log.info("BioAuth ma'lumotlarini olishga urunib ko'rayapman");
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochSecond(getBioAuthTime() / 1000), staticCache.getTimeZoneId()
        );

        LocalDateTime now = LocalDateTime.now(staticCache.getTimeZoneId());

        String remainingTime = getString(expiresAt, now);

        jarvisTelegramBotAPI.sendMessage(
                String.format(
                        "Keyingi BioAuth %s da, qolgan vaqti %s", expiresAt.format(
                                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                        ),
                        remainingTime
                )
        );
        log.info("Keyingi BioAuth {}, qolgan vaqti {}", expiresAt, remainingTime);
    }

    private String getAuthUrl() {
        if (url != null) {
            return url;
        }
        new Thread(this::openTunnel).start();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/sh", "-c", authCmd);
        try {
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
                output.append(line);
            }

            process.waitFor();
            url = output.toString();
            return url;
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
            return "Autentifikatsiya uchun URL olinmadi, iltimos loglarni va application.properties faylini tekshiring";
        }
    }

    private void openTunnel() {
        log.info("Tunnelni ochishga urunib ko'rayapman");
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("/bin/sh", "-c", tunnelCmd);
        try {
            Process process = processBuilder.start();
            process.waitFor();
        } catch (IOException | InterruptedException e) {
            log.error(e.getMessage());
        }
    }

    @NotNull
    private String getString(LocalDateTime expiresAt, LocalDateTime now) {
        long days = now.until(expiresAt, ChronoUnit.DAYS);
        long hours = now.until(expiresAt.minusDays(days), ChronoUnit.HOURS);
        long min = now.until(expiresAt.minusDays(days).minusHours(hours), ChronoUnit.MINUTES);
        long sec = now.until(expiresAt.minusDays(days).minusHours(hours).minusMinutes(min), ChronoUnit.SECONDS);

        return String.format("%sd va %sh:%sm:%ss", days, hours, min, sec);
    }

    private Long getBioAuthTime() {
        try {
            BioAuthStatusDTO response = client.getBioAuthStatus(bioAuthBody);

            if (response == null || response.getResult() == null) {
                jarvisTelegramBotAPI.sendMessage("Muddati tugashini olishning imkoni yo'q");
                throw new HumanodeException("Muddati tugashini olishning imkoni yo'q");
            }

            if (response.getResult() instanceof String) {
                log.info("BioAuth muddati tugadi");
                jarvisTelegramBotAPI.sendMessage("BioAuth muddati tugadi");
                jarvisTelegramBotAPI.sendMessage(getAuthUrl());
                throw new HumanodeException("BioAuth muddati tugadi.");
            } else {
                if (response.getResult() instanceof LinkedHashMap<?, ?> result) {

                    if (!result.isEmpty() && result.get("Active") instanceof LinkedHashMap<?, ?> bioAuth) {

                        if (!bioAuth.isEmpty()) {
                            return (Long) bioAuth.get("expires_at");
                        }
                    }
                }
                throw generateExpireException();
            }
        } catch (Exception e) {
            if (!(e instanceof HumanodeException)) {
                jarvisTelegramBotAPI.sendMessage("Humanode ishlamayapti");
                throw new RuntimeException("Humanode ishlamayapti", e);
            } else {
                throw new RuntimeException(e);
            }
        }
    }

    private Exception generateExpireException() {
        jarvisTelegramBotAPI.sendMessage("Muddati tugashini olishning imkoni yo'q");
        throw new HumanodeException("Muddati tugashini olishning imkoni yo'q");
    }

    @Override
    public void onApplicationEvent(CustomSpringEvent event) {
        if (event.getMessage().equals("get_bioauth_link")) {
            url = null;
            jarvisTelegramBotAPI.sendMessage(getAuthUrl());
        }
    }
}
