package io.humanode.humanode.utils;

public class HumanUtils {
    public static final String SUCCESSFULLY_REGISTERED_MESSAGE = "Siz muvaffaqiyatli ro'yxatdan o'tdingiz, har kuni (agar xabarnomalarni yoqsangiz) autentifikatsiya uchun qoldiqli vaqtni va muddat tugashidan 5 daqiqa oldin xabarnoma eslatmasini olish uchun xabarlar olasiz. Xabarnomalarni yoqish/o'chirish uchun buyruqlarni ko'rish uchun /help yozing.";
    public static final String UNKNOWN_COMMAND = "Noma'lum buyruq, tugmani '/register' bosing va nodeniz haqida xabarnoma olish uchun ro'yxatdan o'ting, vaqt mintaqasini o'rnatish uchun '/timezone vaqt_mintaqa_id' bosing.";
    public static final String MISSING_CHAT_ID = "Chat ID topilmadi, ro'yxatdan o'tishingiz kerak, bot bilan chatda '/register' yozing.";
    public static final String SUCCESSFULLY_REGISTERED_TIME_ZONE_ID = "Siz muvaffaqiyatli vaqt mintaqa ID'sini o'rnatdingiz.";
    public static final String ENABLE_NOTIF = "Xabarnomalar yoqildi.";
    public static final String DISABLED_NOTIF = "Xabarnomalar o'chirildi.";

    public static final String HELP = """
                        /register -> nodeniz uchun ro'yxatdan o'ting
/timezone -> vaqt mintaqasini o'rnating, misol '/timezone Europe/Chisinau'
/enable_notification -> har kuni tushdan keyin nodeniz haqida xabarnoma olishni yoqing
/disable_notification -> har kuni tushdan keyin nodeniz haqida xabarnoma olishni o'chiring
/get_bioauth_link -> yangi autentifikatsiya havolasini oling
/help -> buyruqlar ro'yxati
            """;
}
