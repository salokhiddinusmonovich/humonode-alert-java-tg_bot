package io.humanode.humanode.bot;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import io.humanode.humanode.cache.StaticCache;
import io.humanode.humanode.utils.CustomSpringEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.ZoneId;

import static io.humanode.humanode.utils.HumanUtils.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JarvisTelegramBot implements JarvisTelegramBotAPI {
    private static TelegramBot bot = null;
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    private final StaticCache staticCache;
    @Value("${bot.token}")
    private String token;

    @PostConstruct
    public void start() {
        bot = new TelegramBot(token);

        bot.setUpdatesListener(updates -> {
            log.info("Received updates from bot {}", updates);
            try {
                for (Update u : updates) {
                    executeCommand(u);
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        }, e -> {
            if (e.response() != null) {
                log.error(e.response().toString());
            } else {
                log.error(e.getMessage());
            }
        });

        log.info("Start bot with token: {}", token);
    }


    @Override
    public void sendMessage(String message) {
        if (bot == null) {
            throw new RuntimeException("Telegram bot initializatsiya qilinmagan");
        }

        Long id = staticCache.getChatId();

        if (id == null) {
            log.warn(MISSING_CHAT_ID);
            return;
        }

        SendMessage request = new SendMessage(id, message);

        bot.execute(request, new Callback<SendMessage, SendResponse>() {
            @Override
            public void onResponse(SendMessage request, SendResponse response) {
                log.info("Xabar muvaffaqiyatli jo'natildi {}", message);
            }

            @Override
            public void onFailure(SendMessage request, IOException e) {
                log.error("Xabarni jo'natishda xatolik {}, sabab {}", message, e.getMessage());
            }
        });
    }

    private void executeCommand(Update u) {
        String[] args = u.message().text().split(" ");
        String cmd = args[0];

        switch (cmd) {
            case "/register" -> {
                staticCache.setChatId(u.message().chat().id());
                sendMessage("Muvaffaqiyatli ro'yxatdan o'tdingiz");
            }
            case "/timezone" -> {
                if (args.length > 1 && isValidTimeZoneId(args[1])) {
                    staticCache.setTimeZoneId(args[1]);
                    sendMessage("Muvaffaqiyatli vaqt mintaqasini belgiladingiz");
                } else {
                    sendMessage("Noto'g'ri vaqt mintaqasi, misol: '/timezone Europe/Chisinau'");
                }
            }
            case "/enable_notification" -> {
                staticCache.enable();
                sendMessage("Bildirishnomalar yoqildi");
            }
            case "/disable_notification" -> {
                staticCache.disable();
                sendMessage("Bildirishnomalar o'chirildi");
            }
            case "/get_bioauth_link" -> {
                applicationEventPublisher.publishEvent(new CustomSpringEvent(this, "get_bioauth_link"));
            }
            case "/help" -> sendMessage("Yordam");
            default -> sendMessage("Noma'lum buyruq");
        }
    }

    private boolean isValidTimeZoneId(String timeZoneId) {
        try {
            ZoneId z = ZoneId.of(timeZoneId);
            log.info("Vaqt mintaqasi to'g'ri {}", z);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
