package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMediaGroup;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.media.InputMedia;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new MyBot());
            System.out.println("✅ Bot ishga tushdi!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class MyBot extends TelegramLongPollingBot {

        private static final String BOT_USERNAME = "uz_bek_cats_bot";
        private static final String BOT_TOKEN = "8333145996:AAG2c5dEZ2YAXVSbw-aGO0f-EJaqDOCCRxQ";
        private final long ADMIN_ID = 7038296036L;
        private final String CHANNEL_USERNAME = "@arzon_mushukll";

        // State va ma'lumotlar
        private final Map<Long, String> stateMap = new HashMap<>();
        private final Map<Long, List<String>> photosMap = new HashMap<>();
        private final Map<Long, String> manzilMap = new HashMap<>();
        private final Map<Long, String> phoneMap = new HashMap<>();
        private final Map<Long, String> breedMap = new HashMap<>();
        private final Map<Long, String> ageMap = new HashMap<>();
        private final Map<Long, String> healthMap = new HashMap<>();
        private final Map<Long, String> genderMap = new HashMap<>();
        private final Map<Long, String> priceMap = new HashMap<>();
        private final Map<Long, String> checkMap = new HashMap<>();
        private final Map<Long, String> adTypeMap = new HashMap<>();
        private final AtomicLong adIdCounter = new AtomicLong(1000);

        // Admin o'zgartirish uchun
        private final Map<Long, Long> editUserMap = new HashMap<>(); // adminId -> userId

        // Viloyatlar ro'yxati
        private final List<String> viloyatlar = Arrays.asList(
                "Andijon", "Buxoro", "Farg'ona", "Jizzax", "Xorazm",
                "Namangan", "Navoiy", "Qashqadaryo", "Samarqand",
                "Sirdaryo", "Surxondaryo", "Toshkent", "Toshkent shahar"
        );

        // Mushuk zotlari
        private final List<String> breeds = Arrays.asList(
                "Scottish Fold", "Scottish Strayt", "Britiskiy",
                "Shinshila", "Uy mushuki"
        );

        // Yoshlar
        private final List<String> ages = Arrays.asList(
                "1 oylik", "2 oylik", "3 oylik", "4 oylik", "5 oylik",
                "6 oylik", "7 oylik", "8 oylik", "9 oylik",
                "+1 yosh", "+1.5 yosh", "+2 yosh"
        );

        // Logo URL - YANGI RASM
        private final String LOGO_URL = "https://i.postimg.cc/PCGRfS7g/image.png";

        @Override
        public String getBotUsername() { return BOT_USERNAME; }
        @Override
        public String getBotToken() { return BOT_TOKEN; }

        @Override
        public void onUpdateReceived(Update update) {
            try {
                if (update.hasMessage()) handleMessage(update.getMessage());
                else if (update.hasCallbackQuery()) handleCallback(update.getCallbackQuery());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void handleMessage(Message msg) throws Exception {
            long chatId = msg.getChatId();
            String state = stateMap.getOrDefault(chatId, "");

            if (msg.hasText()) {
                String text = msg.getText().trim();

                if (text.equals("/start")) {
                    sendMainMenu(chatId);
                    return;
                }

                // Narx kiritish
                if ("await_price".equals(state)) {
                    priceMap.put(chatId, text);
                    sendPreview(chatId);
                    return;
                }

                // Yordam uchun ma'lumot qabul qilish
                if ("yordam_await_info".equals(state)) {
                    String userInfo = text;
                    manzilMap.put(chatId, userInfo);
                    sendYordamPreview(chatId, userInfo);
                    return;
                }

                // Telefon raqam kiritish
                if ("await_phone".equals(state)) {
                    phoneMap.put(chatId, text);

                    if ("hadiya".equals(adTypeMap.get(chatId))) {
                        sendPreview(chatId);
                    } else {
                        sendBreedSelection(chatId);
                    }
                    return;
                }

                sendText(chatId, "Iltimos, tugmalardan foydalaning yoki /start ni bosing.");
                return;
            }

            // Rasm qabul qilish
            if (msg.hasPhoto()) {
                if ("await_photo".equals(state)) {
                    List<PhotoSize> photos = msg.getPhoto();
                    String fileId = photos.get(photos.size()-1).getFileId();

                    if (!photosMap.containsKey(chatId)) {
                        photosMap.put(chatId, new ArrayList<>());
                    }
                    photosMap.get(chatId).add(fileId);

                    if (photosMap.get(chatId).size() >= 2) {
                        sendViloyatSelection(chatId);
                    } else {
                        sendText(chatId, "✅ 1-rasm qabul qilindi. Iltimos, 2-rasmni yuboring:");
                    }
                    return;
                }

                // Kasal mushuk uchun rasm qabul qilish
                if ("yordam_kasal_photo".equals(state)) {
                    List<PhotoSize> photos = msg.getPhoto();
                    String fileId = photos.get(photos.size()-1).getFileId();

                    if (!photosMap.containsKey(chatId)) {
                        photosMap.put(chatId, new ArrayList<>());
                    }
                    photosMap.get(chatId).add(fileId);

                    if (photosMap.get(chatId).size() >= 2) {
                        notifyAdminForYordam(chatId, "kasal");
                        sendText(chatId, "✅ Rasmlaringiz qabul qilindi! Admin tekshiradi.");
                        stateMap.put(chatId, "waiting_admin");
                    } else {
                        sendText(chatId, "✅ 1-rasm qabul qilindi. Iltimos, 2-rasmni yuboring:");
                    }
                    return;
                }

                // Yordam uchun rasm qabul qilish
                if (state.startsWith("yordam_") && !state.equals("yordam_kasal_photo")) {
                    List<PhotoSize> photos = msg.getPhoto();
                    String fileId = photos.get(photos.size()-1).getFileId();

                    if (!photosMap.containsKey(chatId)) {
                        photosMap.put(chatId, new ArrayList<>());
                    }
                    photosMap.get(chatId).add(fileId);

                    String userInfo = manzilMap.getOrDefault(chatId, "");
                    sendYordamPreview(chatId, userInfo);
                    return;
                }

                // Chek qabul qilish
                if ("wait_check".equals(state)) {
                    List<PhotoSize> photos = msg.getPhoto();
                    String fileId = photos.get(photos.size()-1).getFileId();
                    checkMap.put(chatId, fileId);
                    sendText(chatId, "✅ Chek qabul qilindi. Admin tekshiradi.");
                    notifyAdmin(chatId);
                    stateMap.put(chatId, "waiting_admin");
                    return;
                }
            }
        }

        private void handleCallback(CallbackQuery cb) throws Exception {
            long chatId = cb.getMessage().getChatId();
            String data = cb.getData();
            long fromId = cb.getFrom().getId();

            execute(new AnswerCallbackQuery(cb.getId()));

            // Admin o'zgartirish callbacks
            if (data.startsWith("admin_set_breed_")) {
                handleAdminSetBreed(chatId, data);
                return;
            }

            switch (data) {
                case "menu_reklama":
                    sendAdTypeSelection(chatId);
                    break;
                case "menu_admin":
                    sendText(chatId, "👤 Admin bilan bog'lanish:\n\n📶 @zayd_catlover\n\n📞 +998934938181");
                    break;
                case "menu_narx":
                    sendPriceList(chatId);
                    break;
                case "menu_haqida":
                    sendBotAboutMenu(chatId);
                    break;
                case "menu_yordam":
                    sendYordamMenu(chatId);
                    break;

                // Bot haqida menyusi
                case "about_back":
                    sendMainMenu(chatId);
                    break;
                case "about_what_can":
                    sendText(chatId, "Ushbu bot orqali siz mushuklar haqida e'lon berishingiz mumkin.");
                    sendMainMenu(chatId);
                    break;
                case "about_need_bot":
                    sendText(chatId, "Taklif va shikoyatlar va sizga ham bot kerak bo'lsa shu raqamgaga aloqaga chiqing: +998900512621");
                    sendMainMenu(chatId);
                    break;

                // Reklama turi
                case "adtype_sotish":
                    adTypeMap.put(chatId, "sotish");
                    startAdProcess(chatId);
                    break;
                case "adtype_hadiya":
                    adTypeMap.put(chatId, "hadiya");
                    startAdProcess(chatId);
                    break;
                case "adtype_vyazka":
                    adTypeMap.put(chatId, "vyazka");
                    startAdProcess(chatId);
                    break;
                case "adtype_back":
                    sendMainMenu(chatId);
                    break;

                // Yordam menyusi
                case "yordam_back":
                    sendMainMenu(chatId);
                    break;
                case "yordam_onasiz":
                    handleYordamOnasiz(chatId);
                    break;
                case "yordam_kasal":
                    handleKasalMushuk(chatId);
                    break;
                case "yordam_kasal_hadiya":
                    handleYordamKasalHadiya(chatId);
                    break;
                case "yordam_confirm":
                    handleYordamConfirm(chatId);
                    break;
                case "yordam_cancel":
                    sendYordamMenu(chatId);
                    break;
                case "yordam_final_confirm":
                    String yordamType = stateMap.get(chatId);
                    notifyAdminForYordam(chatId, yordamType);
                    sendText(chatId, "✅ So'rovingiz qabul qilindi! Admin tekshiradi.");
                    stateMap.put(chatId, "waiting_admin");
                    break;

                // Viloyat tanlash (reklama uchun)
                case "viloyat_andijon": case "viloyat_buxoro": case "viloyat_fargona":
                case "viloyat_jizzax": case "viloyat_xorazm": case "viloyat_namangan":
                case "viloyat_navoiy": case "viloyat_qashqadaryo": case "viloyat_samarqand":
                case "viloyat_sirdaryo": case "viloyat_surxondaryo": case "viloyat_toshkent":
                case "viloyat_toshkent_shahar":
                    String viloyat = data.replace("viloyat_", "").replace("_", " ");
                    manzilMap.put(chatId, viloyat);
                    stateMap.put(chatId, "await_phone");
                    sendText(chatId, "📍 Manzil: " + viloyat + "\n📞 Endi telefon raqamingizni yuboring:(masalan +998** *** ** **)");
                    break;

                // Yordam uchun viloyat tanlash
                case "yordam_viloyat_andijon": case "yordam_viloyat_buxoro": case "yordam_viloyat_fargona":
                case "yordam_viloyat_jizzax": case "yordam_viloyat_xorazm": case "yordam_viloyat_namangan":
                case "yordam_viloyat_navoiy": case "yordam_viloyat_qashqadaryo": case "yordam_viloyat_samarqand":
                case "yordam_viloyat_sirdaryo": case "yordam_viloyat_surxondaryo": case "yordam_viloyat_toshkent":
                case "yordam_viloyat_toshkent_shahar":
                    String yordamViloyat = data.replace("yordam_viloyat_", "").replace("_", " ");
                    manzilMap.put(chatId, yordamViloyat);
                    stateMap.put(chatId, "yordam_await_phone");
                    sendText(chatId, "📍 Manzil: " + yordamViloyat + "\n📞 Endi telefon raqamingizni yuboring:(masalan +998** *** ** **)");
                    break;

                // Yordam telefon raqam qabul qilish
                case "yordam_phone_confirm":
                    String currentState = stateMap.get(chatId);
                    if (currentState.startsWith("yordam_")) {
                        stateMap.put(chatId, currentState + "_photo");
                        sendText(chatId, "📸 Endi rasm yuboring:");
                    }
                    break;

                // Breed tanlash
                case "breed_scottish_fol": case "breed_scottish_strayt":
                case "breed_britiskiy": case "breed_shinshila": case "breed_uy_mushuki":
                    String breed = data.replace("breed_", "").replace("_", " ");
                    breedMap.put(chatId, breed);
                    sendAgeSelection(chatId);
                    break;

                // Yosh tanlash
                case "age_1_oylik": case "age_2_oylik": case "age_3_oylik": case "age_4_oylik":
                case "age_5_oylik": case "age_6_oylik": case "age_7_oylik": case "age_8_oylik":
                case "age_9_oylik": case "age_1_yosh": case "age_1_5_yosh": case "age_2_yosh":
                    String age = data.replace("age_", "").replace("_", " ");
                    ageMap.put(chatId, age);

                    if ("vyazka".equals(adTypeMap.get(chatId))) {
                        sendGenderSelection(chatId);
                    } else {
                        sendHealthSelection(chatId);
                    }
                    break;

                // Sog'lik tanlash
                case "health_soglom":
                    healthMap.put(chatId, "Sog'lom");
                    sendGenderSelection(chatId);
                    break;
                case "health_kasal":
                    healthMap.put(chatId, "Kasal");
                    sendGenderSelection(chatId);
                    break;

                // Jins tanlash
                case "gender_qiz":
                    genderMap.put(chatId, "Qiz bola");
                    if ("sotish".equals(adTypeMap.get(chatId)) || "vyazka".equals(adTypeMap.get(chatId))) {
                        stateMap.put(chatId, "await_price");
                        sendText(chatId, "💰 Narxni kiriting (faqat raqamda):");
                    } else {
                        sendPreview(chatId);
                    }
                    break;
                case "gender_ogil":
                    genderMap.put(chatId, "O'g'il bola");
                    if ("sotish".equals(adTypeMap.get(chatId)) || "vyazka".equals(adTypeMap.get(chatId))) {
                        stateMap.put(chatId, "await_price");
                        sendText(chatId, "💰 Narxni kiriting (faqat raqamda):");
                    } else {
                        sendPreview(chatId);
                    }
                    break;

                // Preview tugmalari
                case "preview_confirm":
                    if ("sotish".equals(adTypeMap.get(chatId)) || "vyazka".equals(adTypeMap.get(chatId))) {
                        sendText(chatId, "💳 To'lov ma'lumotlari:\nKarta: 5614681626280956\n Xalilov.A\nMiqdor: 35,000 so'm minimym reklama narxlardan kurib tulov qiling\nTo'lov qilib, chekni rasmini yuboring.");
                        stateMap.put(chatId, "wait_check");
                    } else {
                        sendText(chatId, "✅ Ma'lumotlaringiz qabul qilindi! Admin tekshirib kanalga joylaydi.");
                        notifyAdmin(chatId);
                        stateMap.put(chatId, "waiting_admin");
                    }
                    break;
                case "preview_back":
                    stateMap.put(chatId, "await_photo");
                    photosMap.remove(chatId);
                    sendText(chatId, "↩️ Orqaga qaytildi. Iltimos, rasmlarni qayta yuboring yoki /start ni bosing.");
                    break;

                // Admin tasdiqlash va o'zgartirish
                case "admin_edit_breed":
                    handleAdminEditBreed(chatId);
                    break;
                case "admin_edit_confirm":
                    handleAdminEditConfirm(chatId);
                    break;
                case "admin_edit_cancel":
                    sendAdminPanel(chatId);
                    break;

                // DEFAULT qismi - Admin tasdiqlash
                default:
                    if (data.startsWith("approve_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("approve_".length());
                            long uid = Long.parseLong(uidStr);
                            postToChannel(uid);
                            sendText(uid, "✅ E'loningiz kanalga joylandi!");
                            sendText(ADMIN_ID, "✅ E'lon admen tomonidan tasdiqlandi va joylandi.");
                        }
                    } else if (data.startsWith("decline_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("decline_".length());
                            long uid = Long.parseLong(uidStr);
                            sendText(uid, "❌ E'loningiz tasdiqlanmadi. Admin bilan bog'laning.");
                            sendText(ADMIN_ID, "❌ E'lon rad etildi.");
                        }
                    } else if (data.startsWith("edit_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("edit_".length());
                            long uid = Long.parseLong(uidStr);
                            sendAdminEditMenu(chatId, uid);
                        }
                    } else if (data.startsWith("yordam_approve_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("yordam_approve_".length());
                            long uid = Long.parseLong(uidStr);
                            postYordamToChannel(uid);
                            sendText(uid, "✅ So'rovingiz tasdiqlandi va kanalga joylandi!");
                            sendText(ADMIN_ID, "✅ Yordam so'rovi tasdiqlandi.");
                        }
                    } else if (data.startsWith("yordam_decline_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("yordam_decline_".length());
                            long uid = Long.parseLong(uidStr);
                            sendText(uid, "❌ So'rovingiz tasdiqlanmadi. Admin bilan bog'laning.");
                            sendText(ADMIN_ID, "❌ Yordam so'rovi rad etildi.");
                        }
                    }
                    break;
            }
        }

        // ========== YANGI: DUMALOQ LOGO BILAN WATERMARK QO'SHISH ==========
        private String addWatermarkToImage(String fileId) {
            try {
                // Asosiy rasmni yuklash
                String fileUrl = getFileUrl(fileId);
                URL url = new URL(fileUrl);
                BufferedImage originalImage = ImageIO.read(url);

                // Logoni yuklash
                URL logoUrl = new URL(LOGO_URL);
                BufferedImage originalLogo = ImageIO.read(logoUrl);

                // Yangi rasm yaratish
                BufferedImage watermarkedImage = new BufferedImage(
                        originalImage.getWidth(),
                        originalImage.getHeight(),
                        BufferedImage.TYPE_INT_RGB
                );

                Graphics2D g2d = watermarkedImage.createGraphics();

                // Asosiy rasmini chizish
                g2d.drawImage(originalImage, 0, 0, null);

                // Sifat sozlamalari
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                // DUMALOQ LOGO YARATISH
                int logoSize = originalImage.getWidth() / 8; // Kichik o'lcham
                BufferedImage circularLogo = createCircularImage(originalLogo, logoSize);

                // Logo joylashuvi (chap yuqori burchak)
                int logoX = 20;
                int logoY = 20;

                // Logoni chizish
                g2d.drawImage(circularLogo, logoX, logoY, null);

                // UzbekCats yozuvi (logo dan keyin)
                g2d.setFont(new Font("Arial", Font.BOLD, 28));
                g2d.setColor(Color.WHITE);

                String watermarkText = "UzbekCats";
                FontMetrics metrics = g2d.getFontMetrics();
                int textX = logoX + logoSize + 10; // Logodan keyin
                int textY = logoY + logoSize / 2 + metrics.getAscent() / 3;

                // Matn fonini chizish (qora shaffof)
                g2d.setColor(new Color(0, 0, 0, 150));
                int padding = 10;
                g2d.fillRoundRect(textX - padding, textY - metrics.getAscent() + padding/2,
                        metrics.stringWidth(watermarkText) + padding*2,
                        metrics.getHeight(), 15, 15);

                // Matnni chizish
                g2d.setColor(Color.WHITE);
                g2d.drawString(watermarkText, textX, textY);

                g2d.dispose();

                // Yangi faylga saqlash
                File outputFile = new File("watermarked_" + System.currentTimeMillis() + ".jpg");
                ImageIO.write(watermarkedImage, "jpg", outputFile);

                // Telegramga yuklash
                String newFileId = uploadPhotoToTelegram(outputFile);
                outputFile.delete();

                return newFileId;

            } catch (Exception e) {
                System.out.println("❌ Watermark qo'shishda xatolik: " + e.getMessage());
                e.printStackTrace();
                return fileId; // Xatolik yuz bersa, original rasmni qaytarish
            }
        }

        // DUMALOQ RASM YARATISH METODI
        private BufferedImage createCircularImage(BufferedImage originalImage, int size) {
            BufferedImage circularImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = circularImage.createGraphics();

            // Sifat sozlamalari
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Dumaloq shakl yaratish
            java.awt.geom.Ellipse2D.Double circle = new java.awt.geom.Ellipse2D.Double(0, 0, size, size);
            g2d.setClip(circle);

            // Rasmini chizish (o'lchamini moslashtirish)
            g2d.drawImage(originalImage, 0, 0, size, size, null);

            // Oq chet qo'shish
            g2d.setClip(null);
            g2d.setStroke(new BasicStroke(3));
            g2d.setColor(Color.WHITE);
            g2d.drawOval(1, 1, size-2, size-2);

            g2d.dispose();
            return circularImage;
        } 

        // Dumaloq shakl klass
        static class Ellipse2D extends java.awt.geom.Ellipse2D.Double {
            public Ellipse2D(double x, double y, double w, double h) {
                super(x, y, w, h);
            }
        }

        private String getFileUrl(String fileId) throws TelegramApiException {
            org.telegram.telegrambots.meta.api.objects.File file = execute(
                    new org.telegram.telegrambots.meta.api.methods.GetFile(fileId)
            );
            return "https://api.telegram.org/file/bot" + BOT_TOKEN + "/" + file.getFilePath();
        }

        private String uploadPhotoToTelegram(File file) throws TelegramApiException {
            SendPhoto sendPhoto = new SendPhoto();
            sendPhoto.setChatId(String.valueOf(ADMIN_ID));
            sendPhoto.setPhoto(new InputFile(file));

            Message message = execute(sendPhoto);
            if (message.hasPhoto()) {
                List<PhotoSize> photos = message.getPhoto();
                return photos.get(photos.size()-1).getFileId();
            }
            return null;
        }

        // ========== BOT HAQIDA MENYUSI ==========
        private void sendBotAboutMenu(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🤖 Bot haqida ma'lumot:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("🤔 Bot nima qiloladi?");
            b1.setCallbackData("about_what_can");
            rows.add(Collections.singletonList(b1));

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("💼 Sizga ham bot kerakmi?");
            b2.setCallbackData("about_need_bot");
            rows.add(Collections.singletonList(b2));

            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("↩️ Orqaga");
            b3.setCallbackData("about_back");
            rows.add(Collections.singletonList(b3));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        // ========== YORDAM MENYUSI ==========
        private void sendYordamMenu(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🆘 Qanday yordam kerak?");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("🐱 Onasiz mushuk");
            b1.setCallbackData("yordam_onasiz");
            rows.add(Collections.singletonList(b1));

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("🏥 Kasal mushuk");
            b2.setCallbackData("yordam_kasal");
            rows.add(Collections.singletonList(b2));

            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("🎁 Kasal Mushuk Hadiyaga");
            b3.setCallbackData("yordam_kasal_hadiya");
            rows.add(Collections.singletonList(b3));

            InlineKeyboardButton b4 = new InlineKeyboardButton();
            b4.setText("↩️ Orqaga");
            b4.setCallbackData("yordam_back");
            rows.add(Collections.singletonList(b4));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        // ========== KANALGA JOYLASH ==========
        private void postToChannel(long userId) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(userId);
            if (userPhotos == null || userPhotos.isEmpty()) {
                System.out.println("❌ Rasmlar topilmadi!");
                return;
            }

            // 2 TA RASMGA HAM WATERMARK QO'SHISH
            List<String> watermarkedPhotos = new ArrayList<>();

            for (String photoId : userPhotos) {
                if (watermarkedPhotos.size() < 2) {
                    String watermarkedFileId = addWatermarkToImage(photoId);
                    if (watermarkedFileId != null) {
                        watermarkedPhotos.add(watermarkedFileId);
                        System.out.println("✅ Rasmga DUMALOQ logo watermark qo'shildi");
                    } else {
                        watermarkedPhotos.add(photoId); // Xatolikda original rasm
                    }
                }
            }

            String adType = adTypeMap.getOrDefault(userId, "");
            String manzil = manzilMap.getOrDefault(userId, "");
            String phone = phoneMap.getOrDefault(userId, "");

            String caption = buildChannelCaption(userId, adType, manzil, phone);

            System.out.println("📝 Kanalga joylash boshlandi...");
            System.out.println("📸 Rasmlar soni: " + watermarkedPhotos.size());
            System.out.println("📋 Format: " + adType);

            // 2 TA RASMNI BIRGA JOYLASH
            try {
                if (watermarkedPhotos.size() >= 2) {
                    SendMediaGroup mediaGroup = new SendMediaGroup();
                    mediaGroup.setChatId(CHANNEL_USERNAME);

                    List<InputMedia> mediaList = new ArrayList<>();

                    // Birinchi rasm - caption bilan
                    InputMediaPhoto media1 = new InputMediaPhoto();
                    media1.setMedia(watermarkedPhotos.get(0));
                    media1.setCaption(caption);
                    media1.setParseMode("Markdown");
                    mediaList.add(media1);

                    // Ikkinchi rasm - captionsiz
                    InputMediaPhoto media2 = new InputMediaPhoto();
                    media2.setMedia(watermarkedPhotos.get(1));
                    mediaList.add(media2);

                    mediaGroup.setMedias(mediaList);
                    execute(mediaGroup);
                    System.out.println("✅ 2 ta rasm kanalga joylandi!");
                } else if (watermarkedPhotos.size() == 1) {
                    // Agar faqat 1 ta rasm bo'lsa
                    SendPhoto post = new SendPhoto();
                    post.setChatId(CHANNEL_USERNAME);
                    post.setPhoto(new InputFile(watermarkedPhotos.get(0)));
                    post.setCaption(caption);
                    post.setParseMode("Markdown");
                    execute(post);
                    System.out.println("✅ 1 ta rasm kanalga joylandi!");
                }
            } catch (Exception e) {
                System.out.println("❌ Kanalga joylashda xatolik: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private String buildChannelCaption(long userId, String adType, String manzil, String phone) {
            StringBuilder caption = new StringBuilder();

            if ("hadiya".equals(adType)) {
                // HADIYA UCHUN MAXSUS FORMAT
                caption.append("#HADIYAGA 🎁\n\n");
                caption.append("📝 Mushukcha yaxshi insonlarga tekinga sovg'a qilinadi. Iltimos mushukni sotadigan yoki chidolmay ko'chaga tashlab ketadigan bo'lsangiz olmang! Allohdan qo'rqing\n\n");
                caption.append("🏠 Manzil: ").append(manzil).append("\n");
                caption.append("📞 Nomer: ").append(phone).append("\n\n");

            } else if ("vyazka".equals(adType)) {
                // VYAZKA UCHUN MAXSUS FORMAT
                String breed = breedMap.getOrDefault(userId, "");
                String age = ageMap.getOrDefault(userId, "");
                String gender = genderMap.getOrDefault(userId, "");
                String price = priceMap.getOrDefault(userId, "");

                caption.append("#VYAZKAGA 💝\n\n");
                caption.append("📝 ").append(breed).append(" ").append(age).append(" ").append(gender).append("\n\n");
                caption.append("🏠 Manzil: ").append(manzil).append("\n");
                caption.append("💵 Narxi: ").append(price).append(" so'm\n");
                caption.append("📞 Tel: ").append(phone).append("\n\n");

            } else {
                // SOTISH UCHUN FORMAT
                String breed = breedMap.getOrDefault(userId, "");
                String age = ageMap.getOrDefault(userId, "");
                String health = healthMap.getOrDefault(userId, "");
                String gender = genderMap.getOrDefault(userId, "");
                String price = priceMap.getOrDefault(userId, "");

                caption.append("#SOTILADI 💰\n\n");
                caption.append("📝 ").append(breed).append(" ").append(age).append(" ").append(gender).append(" ").append(health.toLowerCase()).append("\n\n");
                caption.append("🏠 Manzil: ").append(manzil).append("\n");
                caption.append("💵 Narxi: ").append(price).append(" so'm\n");
                caption.append("📞 Tel: ").append(phone).append("\n\n");
            }

            // LINKLAR - Markdown formatida
            caption.append("[🤩 Admin](https://t.me/zayd_catlover) | ");
            caption.append("[📹 YouTube](https://youtu.be/vdwgSB7_amw) | ");
            caption.append("[📷 Instagram](https://www.instagram.com/p/C-cZkgstVGK/) | ");
            caption.append("[💬 Telegram](https://t.me/uzbek_cats)");

            return caption.toString();
        }

        // ========== QOLGAN METODLAR ==========
        // ... (qolgan metodlar o'zgarmagan)

        // Asosiy menyu
        private void sendMainMenu(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🐱 Assalomu alaykum! UzbekCats botiga xush kelibsiz!\n\nQuyidagilardan birini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("📢 Reklama joylash");
            b1.setCallbackData("menu_reklama");
            rows.add(Collections.singletonList(b1));

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("👤 Admin bilan bog'lanish");
            b2.setCallbackData("menu_admin");
            rows.add(Collections.singletonList(b2));

            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("💰 Narxlar");
            b3.setCallbackData("menu_narx");
            rows.add(Collections.singletonList(b3));

            InlineKeyboardButton b4 = new InlineKeyboardButton();
            b4.setText("🆘 Yordam");
            b4.setCallbackData("menu_yordam");
            rows.add(Collections.singletonList(b4));

            InlineKeyboardButton b5 = new InlineKeyboardButton();
            b5.setText("ℹ️ Bot haqida");
            b5.setCallbackData("menu_haqida");
            rows.add(Collections.singletonList(b5));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        // Yordam bo'limi metodlari
        private void handleYordamOnasiz(long chatId) throws TelegramApiException {
            stateMap.put(chatId, "yordam_onasiz");
            photosMap.put(chatId, new ArrayList<>());

            String message = "Assalomu alaykum sizni mushukchanggizni onasi bo'lmasa va uni siz hadiyaga bermoqchi bo'lsaez reklama qiling ";

            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(message);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("✅ Ko'rib chiqdim");
            b1.setCallbackData("yordam_confirm");

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("↩️ Orqaga");
            b2.setCallbackData("yordam_cancel");

            rows.add(Collections.singletonList(b1));
            rows.add(Collections.singletonList(b2));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void handleYordamKasalHadiya(long chatId) throws TelegramApiException {
            stateMap.put(chatId, "yordam_kasal_hadiya");
            photosMap.put(chatId, new ArrayList<>());

            String message = "Assalomu alaykum agar sizda kasal mushuk bo'lsa va siz uni boqolmasanggiz hadiyaga bermoqchi bo'lsanggiz iltimos uni 2 ta rasmini yuboring ";

            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(message);

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("✅ Ko'rib chiqdim");
            b1.setCallbackData("yordam_confirm");

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("↩️ Orqaga");
            b2.setCallbackData("yordam_cancel");

            rows.add(Collections.singletonList(b1));
            rows.add(Collections.singletonList(b2));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void handleKasalMushuk(long chatId) throws TelegramApiException {
            stateMap.put(chatId, "yordam_kasal_photo");
            photosMap.put(chatId, new ArrayList<>());
            sendText(chatId, "🏥 Kasal mushukni 2 ta rasmni yuboring iltimos:");
        }

        private void handleYordamConfirm(long chatId) throws TelegramApiException {
            String state = stateMap.get(chatId);

            if (state.equals("yordam_onasiz") || state.equals("yordam_kasal_hadiya")) {
                sendYordamViloyatSelection(chatId);
            }
        }

        private void sendYordamViloyatSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("📍 Manzilingizni tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String viloyat : viloyatlar) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(viloyat);
                btn.setCallbackData("yordam_viloyat_" + viloyat.toLowerCase().replace(" ", "_").replace("'", ""));
                rows.add(Collections.singletonList(btn));
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendYordamPreview(long chatId, String userInfo) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(chatId);
            String state = stateMap.get(chatId);

            String caption = "";
            String finalText = "";

            if (state.equals("yordam_onasiz")) {
                caption = "🐱 ONA SIZ MUSHUK\n\n";
                finalText = "mushukcha yaxshi insonlarga tekinga sovg'a qilinadi. Iltimos mushukni sotadigan yoki chidolmay ko'chaga tashlab ketadigan bo'lsangiz olmang! Allohdan qo'rqing";
            } else if (state.equals("yordam_kasal_hadiya")) {
                caption = "🎁 KASAL MUSHUK HADIYAGA\n\n";
                finalText = "mushukcha yaxshi insonlarga tekinga sovg'a qilinadi. Iltimos mushukni sotadigan yoki chidolmay ko'chaga tashlab ketadigan bo'lsangiz olmang! Allohdan qo'rqing";
            }

            caption += "📍 Ma'lumot: " + userInfo + "\n\n";
            caption += finalText + "\n\nMa'lumotlaringiz to'g'rimi?";

            if (userPhotos != null && !userPhotos.isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(String.valueOf(chatId));
                photo.setPhoto(new InputFile(userPhotos.get(0)));
                photo.setCaption(caption);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton confirm = new InlineKeyboardButton();
                confirm.setText("✅ Tasdiqlash");
                confirm.setCallbackData("yordam_final_confirm");

                InlineKeyboardButton back = new InlineKeyboardButton();
                back.setText("↩️ Orqaga");
                back.setCallbackData("yordam_cancel");

                markup.setKeyboard(Arrays.asList(
                        Collections.singletonList(confirm),
                        Collections.singletonList(back)
                ));
                photo.setReplyMarkup(markup);

                execute(photo);
            } else {
                SendMessage msg = new SendMessage();
                msg.setChatId(String.valueOf(chatId));
                msg.setText(caption);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton confirm = new InlineKeyboardButton();
                confirm.setText("✅ Tasdiqlash");
                confirm.setCallbackData("yordam_final_confirm");

                InlineKeyboardButton back = new InlineKeyboardButton();
                back.setText("↩️ Orqaga");
                back.setCallbackData("yordam_cancel");

                markup.setKeyboard(Arrays.asList(
                        Collections.singletonList(confirm),
                        Collections.singletonList(back)
                ));
                msg.setReplyMarkup(markup);

                execute(msg);
            }
        }

        private void notifyAdminForYordam(long chatId, String type) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(chatId);

            String adminText = "🆘 YANGI YORDAM SO'ROVI\n\n";
            adminText += "Turi: " + type + "\n";
            adminText += "User ID: " + chatId + "\n";
            adminText += "Manzil: " + manzilMap.getOrDefault(chatId, "—") + "\n";
            adminText += "Telefon: " + phoneMap.getOrDefault(chatId, "—") + "\n\n";
            adminText += "Tasdiqlaysizmi?";

            if (userPhotos != null && !userPhotos.isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(String.valueOf(ADMIN_ID));
                photo.setPhoto(new InputFile(userPhotos.get(0)));
                photo.setCaption(adminText);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton approve = new InlineKeyboardButton();
                approve.setText("✅ Tasdiqlash");
                approve.setCallbackData("yordam_approve_" + chatId);

                InlineKeyboardButton decline = new InlineKeyboardButton();
                decline.setText("❌ Rad etish");
                decline.setCallbackData("yordam_decline_" + chatId);

                markup.setKeyboard(Arrays.asList(
                        Collections.singletonList(approve),
                        Collections.singletonList(decline)
                ));
                photo.setReplyMarkup(markup);

                execute(photo);
            } else {
                SendMessage msg = new SendMessage();
                msg.setChatId(String.valueOf(ADMIN_ID));
                msg.setText(adminText);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton approve = new InlineKeyboardButton();
                approve.setText("✅ Tasdiqlash");
                approve.setCallbackData("yordam_approve_" + chatId);

                InlineKeyboardButton decline = new InlineKeyboardButton();
                decline.setText("❌ Rad etish");
                decline.setCallbackData("yordam_decline_" + chatId);

                markup.setKeyboard(Arrays.asList(
                        Collections.singletonList(approve),
                        Collections.singletonList(decline)
                ));
                msg.setReplyMarkup(markup);

                execute(msg);
            }
        }

        private void postYordamToChannel(long userId) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(userId);
            String state = stateMap.get(userId);

            String caption = "#YORDAM\n\n";

            if (state.equals("yordam_onasiz")) {
                caption += "🐱 ONA SIZ MUSHUK\n\n";
                caption += "mushukcha yaxshi insonlarga tekinga sovg'a qilinadi. Iltimos mushukni sotadigan yoki chidolmay ko'chaga tashlab ketadigan bo'lsangiz olmang! Allohdan qo'rqing\n\n";
                caption += "📍 Manzil: " + manzilMap.getOrDefault(userId, "—") + "\n";
                caption += "📞 Telefon: " + phoneMap.getOrDefault(userId, "—") + "\n\n";
            } else if (state.equals("yordam_kasal")) {
                caption += "#KASAL 🏥\n\n";
                caption += "Yordam Qo'lini kestirib olib tashlash kerak iltimos qo'ldan kegancha yordam bering pastagi nomerga qiling";
            } else if (state.equals("yordam_kasal_hadiya")) {
                caption += "🎁 KASAL MUSHUK HADIYAGA\n\n";
                caption += "Mushukcha yaxshi insonlarga tekinga sovg'a qilinadi. Iltimos mushukni sotadigan yoki chidolmay ko'chaga tashlab ketadigan bo'lsangiz olmang! Allohdan qo'rqing";
                caption += "📍 Manzil: " + manzilMap.getOrDefault(userId, "—") + "\n";
                caption += "📞 Telefon: " + phoneMap.getOrDefault(userId, "—") + "\n\n";
            }

            // LINKLAR QO'SHISH
            caption += "[🤩 Admin](https://t.me/zayd_catlover) | ";
            caption += "[📹 YouTube](https://youtu.be/vdwgSB7_amw) | ";
            caption += "[📷 Instagram](https://www.instagram.com/p/C-cZkgstVGK/) | ";
            caption += "[💬 Telegram](https://t.me/uzbek_cats)";

            if (userPhotos != null && !userPhotos.isEmpty()) {
                SendPhoto post = new SendPhoto();
                post.setChatId(CHANNEL_USERNAME);
                post.setPhoto(new InputFile(userPhotos.get(0)));
                post.setCaption(caption);
                post.setParseMode("Markdown");
                execute(post);
            } else {
                SendMessage post = new SendMessage();
                post.setChatId(CHANNEL_USERNAME);
                post.setText(caption);
                post.setParseMode("Markdown");
                execute(post);
            }
        }

        // Qolgan metodlar...
        private void sendAdTypeSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🎯 Reklama turini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("💰 Sotiladigan");
            b1.setCallbackData("adtype_sotish");
            rows.add(Collections.singletonList(b1));

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("🎁 Hadiyaga");
            b2.setCallbackData("adtype_hadiya");
            rows.add(Collections.singletonList(b2));

            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("💝 Vyazkaga");
            b3.setCallbackData("adtype_vyazka");
            rows.add(Collections.singletonList(b3));

            InlineKeyboardButton b4 = new InlineKeyboardButton();
            b4.setText("↩️ Orqaga");
            b4.setCallbackData("adtype_back");
            rows.add(Collections.singletonList(b4));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void startAdProcess(long chatId) throws TelegramApiException {
            stateMap.put(chatId, "await_photo");
            photosMap.put(chatId, new ArrayList<>());
            sendText(chatId, "📸 Iltimos, 2 ta rasm yuboring:");
        }

        private void sendViloyatSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("📍 Manzilingizni tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String viloyat : viloyatlar) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(viloyat);
                btn.setCallbackData("viloyat_" + viloyat.toLowerCase().replace(" ", "_").replace("'", ""));
                rows.add(Collections.singletonList(btn));
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendBreedSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🐱 Mushuk zotini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String breed : breeds) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(breed);
                btn.setCallbackData("breed_" + breed.toLowerCase().replace(" ", "_"));
                rows.add(Collections.singletonList(btn));
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendAgeSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🎂 Yoshini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (int i = 0; i < ages.size(); i += 3) {
                List<InlineKeyboardButton> row = new ArrayList<>();
                for (int j = i; j < Math.min(i + 3, ages.size()); j++) {
                    InlineKeyboardButton btn = new InlineKeyboardButton();
                    btn.setText(ages.get(j));
                    btn.setCallbackData("age_" + ages.get(j).toLowerCase().replace(" ", "_").replace("+", "").replace(".", "_"));
                    row.add(btn);
                }
                rows.add(row);
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendHealthSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("❤️ Sog'lig'ini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("Sog'lom");
            b1.setCallbackData("health_soglom");

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("Kasal");
            b2.setCallbackData("health_kasal");

            rows.add(Arrays.asList(b1, b2));
            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendGenderSelection(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("👤 Jinsini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("Qiz bola");
            b1.setCallbackData("gender_qiz");

            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("O'g'il bola");
            b2.setCallbackData("gender_ogil");

            rows.add(Arrays.asList(b1, b2));
            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void sendPreview(long chatId) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(chatId);
            if (userPhotos == null || userPhotos.isEmpty()) {
                sendText(chatId, "Xatolik: Rasmlar topilmadi.");
                return;
            }

            SendPhoto photo = new SendPhoto();
            photo.setChatId(String.valueOf(chatId));
            photo.setPhoto(new InputFile(userPhotos.get(0)));
            photo.setCaption(buildPreviewCaption(chatId));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            InlineKeyboardButton confirm = new InlineKeyboardButton();
            confirm.setText("✅ Tasdiqlash");
            confirm.setCallbackData("preview_confirm");

            InlineKeyboardButton back = new InlineKeyboardButton();
            back.setText("↩️ Orqaga");
            back.setCallbackData("preview_back");

            markup.setKeyboard(Collections.singletonList(Arrays.asList(confirm, back)));
            photo.setReplyMarkup(markup);

            execute(photo);
        }

        private String buildPreviewCaption(long chatId) {
            StringBuilder sb = new StringBuilder();
            String adType = adTypeMap.getOrDefault(chatId, "");

            if ("vyazka".equals(adType)) {
                sb.append("💝 VYAZKA - E'lon ma'lumotlari:\n\n");
            } else if ("sotish".equals(adType)) {
                sb.append("📋 SOTISH - E'lon ma'lumotlari:\n\n");
            } else {
                sb.append("🎁 HADIYA - E'lon ma'lumotlari:\n\n");
            }

            sb.append("📍 Manzil: ").append(manzilMap.getOrDefault(chatId, "—")).append("\n");
            sb.append("📞 Telefon: ").append(phoneMap.getOrDefault(chatId, "—")).append("\n");

            if (!"hadiya".equals(adType)) {
                sb.append("🐱 Zot: ").append(breedMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("🎂 Yosh: ").append(ageMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("👤 Jins: ").append(genderMap.getOrDefault(chatId, "—")).append("\n");

                if ("sotish".equals(adType)) {
                    sb.append("❤️ Sog'lig'i: ").append(healthMap.getOrDefault(chatId, "—")).append("\n");
                }

                sb.append("💰 Narx: ").append(priceMap.getOrDefault(chatId, "—")).append("\n");
            }

            sb.append("\nMa'lumotlaringiz to'g'rimi?");
            return sb.toString();
        }

        private void sendAdminPanel(long chatId) throws TelegramApiException {
            if (chatId != ADMIN_ID) {
                sendText(chatId, "❌ Siz admin emassiz!");
                return;
            }

            sendText(chatId, "👨‍💼 Admin paneliga xush kelibsiz!");
        }

        private void sendPriceList(long chatId) throws TelegramApiException {
            sendText(chatId, "\uD83D\uDC08\u200D⬛ reklama Narxlar:\n\n" +
                    "❕Iltimos oxirigacha diqqat bilan o'qib tanishib chiqing.\n\n" +
                    "\uD83D\uDCAC Telegram (https://t.me/uzbek_cats) post 35 ming so'm mushukcha sotilguncha turadi\n"+
                    "\uD83D\uDCF9 Instagram (https://instagram.com/zayd.catlover) istoriya 40 ming so'm 24 so'atlik istoriya qo'yiladi\n" +
                    "\uD83D\uDC08\u200D⬛\uFE0F shayxsiy telegramim (https://t.me/zayd_catlover) istoriyasi 15 ming so'm 24 so'at turadi\n"+
                    "Yuqorida aytib o'tilgan reklama narxi faqatgina 1 dona mushuk reklamasi uchun hissoblanadi ❗\uFE0F\n"+
                    "⚠\uFE0F Iltimos mushukni to'liq malumoti va chiroyli rasmi , telefon raqamingiz va manzilni yozing\n"+
                    "Karta raqam \uD83D\uDC47\n"+
                    "\uD83D\uDCB3 5614681626280956\n"+
                    "\uD83D\uDD0D Xalilov A\n\n\n\n\n\n\n\n"
            );

            sendText(chatId, "\uD83D\uDC08\u200D⬛ reklama Narxlar:\n\n" +
                    "\uD83D\uDC08\u200D⬛️ Vyazka reklama narxi\n\n"+
                    "\uD83D\uDCAC Telegram (https://t.me/uzbek_cats) telegram 100 ming so'm o'chib ketmaydi doim turadi\n"+
                    "\uD83D\uDCF9 Instagram (https://instagram.com/zayd.catlover) istoriya 50 ming so'm aktualniyaga qo'yamiz umurbod turadi 50$\n"+
                    "⚠️\uFE0F Iltimos mushukni to'liq malumoti va chiroyli rasmi , telefon raqamingiz va manzilni yozing\uD83D\uDC08\u200D⬛️\uFE0F shayxsiy telegramim (https://t.me/zayd_catlover) istoriyasi 20 ming so'm 24 so'at turadi\n"+
                    "Karta raqam \uD83D\uDC47\n"+
                    "\uD83D\uDCB3 5614681626280956\n"+
                    "\uD83D\uDD0D Xalilov A\n");
        }

        private void sendText(long chatId, String text) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(text);
            execute(msg);
        }

        // Admin metodlari
        private void sendAdminEditMenu(long adminId, long userId) throws TelegramApiException {
            editUserMap.put(adminId, userId);

            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(adminId));
            msg.setText("✏️ Faqat zotni o'zgartirishingiz mumkin:\n\n" + buildAdminPreviewCaption(userId));

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            InlineKeyboardButton breedBtn = new InlineKeyboardButton();
            breedBtn.setText("✏️ Zotni o'zgartirish");
            breedBtn.setCallbackData("admin_edit_breed");
            rows.add(Collections.singletonList(breedBtn));

            InlineKeyboardButton confirmBtn = new InlineKeyboardButton();
            confirmBtn.setText("✅ Tasdiqlash");
            confirmBtn.setCallbackData("admin_edit_confirm");

            InlineKeyboardButton cancelBtn = new InlineKeyboardButton();
            cancelBtn.setText("❌ Bekor qilish");
            cancelBtn.setCallbackData("admin_edit_cancel");

            rows.add(Arrays.asList(confirmBtn, cancelBtn));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void handleAdminEditBreed(long adminId) throws TelegramApiException {
            sendAdminBreedSelection(adminId);
        }

        private void handleAdminEditConfirm(long adminId) throws TelegramApiException {
            Long userId = editUserMap.get(adminId);
            if (userId == null) return;

            postToChannel(userId);
            sendText(userId, "✅ E'loningiz kanalga joylandi!");
            sendText(adminId, "✅ E'lon o'zgartirildi va kanalga joylandi!");

            editUserMap.remove(adminId);
        }

        private void sendAdminBreedSelection(long adminId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(adminId));
            msg.setText("🐱 Yangi zotni tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (String breed : breeds) {
                InlineKeyboardButton btn = new InlineKeyboardButton();
                btn.setText(breed);
                btn.setCallbackData("admin_set_breed_" + breed.toLowerCase().replace(" ", "_"));
                rows.add(Collections.singletonList(btn));
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void handleAdminSetBreed(long adminId, String data) throws TelegramApiException {
            Long userId = editUserMap.get(adminId);
            if (userId == null) return;

            String breed = data.replace("admin_set_breed_", "").replace("_", " ");
            breedMap.put(userId, breed);

            sendText(adminId, "✅ Zot o'zgartirildi: " + breed);
            sendAdminEditMenu(adminId, userId);
        }

        private String buildAdminPreviewCaption(long userId) {
            return "📋 Joriy ma'lumotlar:\n\n" +
                    "🐱 Zot: " + breedMap.getOrDefault(userId, "—") + "\n" +
                    "🎂 Yosh: " + ageMap.getOrDefault(userId, "—") + "\n" +
                    "❤️ Sog'lig'i: " + healthMap.getOrDefault(userId, "—") + "\n" +
                    "👤 Jins: " + genderMap.getOrDefault(userId, "—") + "\n" +
                    "📍 Manzil: " + manzilMap.getOrDefault(userId, "—") + "\n" +
                    "📞 Telefon: " + phoneMap.getOrDefault(userId, "—") + "\n" +
                    "💰 Narx: " + priceMap.getOrDefault(userId, "—");
        }

        private void notifyAdmin(long chatId) throws TelegramApiException {
            long adId = adIdCounter.incrementAndGet();

            String caption = "🆕 Yangi e'lon! ID: " + adId + "\n\n" +
                    "Tur: " + adTypeMap.getOrDefault(chatId, "") + "\n" +
                    buildPreviewCaption(chatId);

            SendMessage info = new SendMessage();
            info.setChatId(String.valueOf(ADMIN_ID));
            info.setText(caption);
            execute(info);

            List<String> userPhotos = photosMap.get(chatId);
            if (userPhotos != null && !userPhotos.isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(String.valueOf(ADMIN_ID));
                photo.setPhoto(new InputFile(userPhotos.get(0)));
                photo.setCaption("E'lon rasmi - ID: " + adId);

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton approve = new InlineKeyboardButton();
                approve.setText("✅ Tasdiqlash");
                approve.setCallbackData("approve_" + chatId);

                InlineKeyboardButton edit = new InlineKeyboardButton();
                edit.setText("✏️ O'zgartirish");
                edit.setCallbackData("edit_" + chatId);

                InlineKeyboardButton decline = new InlineKeyboardButton();
                decline.setText("❌ Rad etish");
                decline.setCallbackData("decline_" + chatId);

                markup.setKeyboard(Arrays.asList(
                        Arrays.asList(approve, edit),
                        Collections.singletonList(decline)
                ));
                photo.setReplyMarkup(markup);

                execute(photo);
            }

            if (checkMap.containsKey(chatId)) {
                SendPhoto check = new SendPhoto();
                check.setChatId(String.valueOf(ADMIN_ID));
                check.setPhoto(new InputFile(checkMap.get(chatId)));
                check.setCaption("💳 To'lov cheki - ID: " + adId);
                execute(check);
            }
        }
    }
}
// YANGI: DUMALOQ LOGO BILAN WATERMARK QO'SHISH