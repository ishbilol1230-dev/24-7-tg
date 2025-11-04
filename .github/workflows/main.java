package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.text.SimpleDateFormat;

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

        // Soddaroq statistika
        private final List<AdRecord> approvedAds = new ArrayList<>();

        // Viloyatlar ro'yxati
        private final List<String> viloyatlar = Arrays.asList(
                "Andijon", "Buxoro", "Farg'ona", "Jizzax", "Xorazm",
                "Namangan", "Navoiy", "Qashqadaryo", "Samarqand",
                "Sirdaryo", "Surxondaryo", "Toshkent", "Toshkent shahar"
        );

        // Mushuk zotlari
        private final List<String> breeds = Arrays.asList(
                "Scottish fol", "Scottish strayt", "Britiskiy",
                "Shinshila", "Uy mushuki"
        );

        // Yoshlar
        private final List<String> ages = Arrays.asList(
                "1 oylik", "2 oylik", "3 oylik", "4 oylik", "5 oylik",
                "6 oylik", "7 oylik", "8 oylik", "9 oylik",
                "+1 yosh", "+1.5 yosh", "+2 yosh"
        );

        // AdRecord klassi
        private static class AdRecord {
            private Long adId;
            private String adType;
            private Date createdAt;
            private boolean approved;

            public AdRecord(Long adId, String adType, Date createdAt, boolean approved) {
                this.adId = adId;
                this.adType = adType;
                this.createdAt = createdAt;
                this.approved = approved;
            }

            public Long getAdId() { return adId; }
            public String getAdType() { return adType; }
            public Date getCreatedAt() { return createdAt; }
            public boolean isApproved() { return approved; }
            public void setApproved(boolean approved) { this.approved = approved; }
        }

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

                    // Agar 2 ta rasm yuborilgan bo'lsa
                    if (photosMap.get(chatId).size() >= 2) {
                        sendViloyatSelection(chatId);
                    } else {
                        sendText(chatId, "✅ 1-rasm qabul qilindi. Iltimos, 2-rasmni yuboring:");
                    }
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
                    sendBotInfo(chatId);
                    break;
                case "menu_stats":
                    if (fromId == ADMIN_ID) {
                        sendStatsMenu(chatId);
                    }
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

                // Viloyat tanlash
                case "viloyat_andijon": case "viloyat_buxoro": case "viloyat_fargona":
                case "viloyat_jizzax": case "viloyat_xorazm": case "viloyat_namangan":
                case "viloyat_navoiy": case "viloyat_qashqadaryo": case "viloyat_samarqand":
                case "viloyat_sirdaryo": case "viloyat_surxondaryo": case "viloyat_toshkent":
                case "viloyat_toshkent_shahar":
                    String viloyat = data.replace("viloyat_", "").replace("_", " ");
                    manzilMap.put(chatId, viloyat);
                    stateMap.put(chatId, "await_phone");
                    sendText(chatId, "📍 Manzil: " + viloyat + "\n📞 Endi telefon raqamingizni yuboring:(masalan +998** *** ** **");
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
                    sendHealthSelection(chatId);
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
                    if ("sotish".equals(adTypeMap.get(chatId))) {
                        stateMap.put(chatId, "await_price");
                        sendText(chatId, "💰 Narxni kiriting (faqat raqamda):");
                    } else {
                        sendPreview(chatId);
                    }
                    break;
                case "gender_ogil":
                    genderMap.put(chatId, "O'g'il bola");
                    if ("sotish".equals(adTypeMap.get(chatId))) {
                        stateMap.put(chatId, "await_price");
                        sendText(chatId, "💰 Narxni kiriting (faqat raqamda):");
                    } else {
                        sendPreview(chatId);
                    }
                    break;

                // Preview tugmalari
                case "preview_confirm":
                    if ("sotish".equals(adTypeMap.get(chatId))) {
                        sendText(chatId, "💳 To'lov ma'lumotlari:\nKarta: **** **** **** ****\nMiqdor: 35,000 so'm\nTo'lov qilib, chekni rasmini yuboring.");
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
                    sendText(chatId, "↩️ Orqaga qaytildi. Iltimos, rasmlarni qayta yuboring.");
                    break;

                // Statistika tugmalari
                case "stats_today_sell":
                    if (fromId == ADMIN_ID) {
                        showTodaySellStats(chatId);
                    }
                    break;
                case "stats_today_gift":
                    if (fromId == ADMIN_ID) {
                        showTodayGiftStats(chatId);
                    }
                    break;
                case "stats_weekly":
                    if (fromId == ADMIN_ID) {
                        showWeeklyStats(chatId);
                    }
                    break;
                case "stats_today_income":
                    if (fromId == ADMIN_ID) {
                        showTodayIncomeStats(chatId);
                    }
                    break;

                // Admin tasdiqlash
                default:
                    if (data.startsWith("approve_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("approve_".length());
                            long uid = Long.parseLong(uidStr);
                            postToChannel(uid);
                            sendText(uid, "✅ E'loningiz kanalga joylandi!");
                            sendText(ADMIN_ID, "✅ E'lon admen tomonidan tasdiqlandi va joylandi.");

                            // Statistikaga qo'shish
                            addToStats(uid);
                        }
                    } else if (data.startsWith("decline_")) {
                        if (fromId == ADMIN_ID) {
                            String uidStr = data.substring("decline_".length());
                            long uid = Long.parseLong(uidStr);
                            sendText(uid, "❌ E'loningiz tasdiqlanmadi. Admin bilan bog'laning.");
                            sendText(ADMIN_ID, "❌ E'lon rad etildi.");
                        }
                    }
                    break;
            }
        }

        // ========== STATISTIKA METODLARI ==========

        private void sendStatsMenu(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("📊 Statistika menyusi:\n\nQuyidagilardan birini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // 1-qator
            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("💰 Bugun sotishga");
            b1.setCallbackData("stats_today_sell");
            rows.add(Collections.singletonList(b1));

            // 2-qator
            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("🎁 Bugun hadiyaga");
            b2.setCallbackData("stats_today_gift");
            rows.add(Collections.singletonList(b2));

            // 3-qator
            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("📈 1 haftalik");
            b3.setCallbackData("stats_weekly");
            rows.add(Collections.singletonList(b3));

            // 4-qator
            InlineKeyboardButton b4 = new InlineKeyboardButton();
            b4.setText("💵 Bugun daromad");
            b4.setCallbackData("stats_today_income");
            rows.add(Collections.singletonList(b4));

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

        private void showTodaySellStats(long chatId) throws TelegramApiException {
            int count = getTodayAdsCount("sotish");
            String message = "💰 Bugun sotishga qo'yilgan mushuklar:\n\n" +
                    "📊 Soni: " + count + " ta\n" +
                    "⏰ Sana: " + new SimpleDateFormat("dd.MM.yyyy").format(new Date());

            sendStatsMessage(chatId, message);
        }

        private void showTodayGiftStats(long chatId) throws TelegramApiException {
            int count = getTodayAdsCount("hadiya");
            String message = "🎁 Bugun hadiyaga qo'yilgan mushuklar:\n\n" +
                    "📊 Soni: " + count + " ta\n" +
                    "⏰ Sana: " + new SimpleDateFormat("dd.MM.yyyy").format(new Date());

            sendStatsMessage(chatId, message);
        }

        private void showWeeklyStats(long chatId) throws TelegramApiException {
            int weeklySell = getWeeklyAdsCount("sotish");
            int weeklyGift = getWeeklyAdsCount("hadiya");
            int total = weeklySell + weeklyGift;

            String message = "📈 1 haftalik statistika:\n\n" +
                    "💰 Sotishga: " + weeklySell + " ta\n" +
                    "🎁 Hadiyaga: " + weeklyGift + " ta\n" +
                    "📊 Jami: " + total + " ta\n" +
                    "📅 Davr: oxirgi 7 kun";

            sendStatsMessage(chatId, message);
        }

        private void showTodayIncomeStats(long chatId) throws TelegramApiException {
            int todaySell = getTodayAdsCount("sotish");
            int todayGift = getTodayAdsCount("hadiya");
            int totalAds = todaySell + todayGift;
            long income = todaySell * 35000;

            String message = "💵 Bugungi statistika:\n\n" +
                    "💰 Sotishga: " + todaySell + " ta\n" +
                    "🎁 Hadiyaga: " + todayGift + " ta\n" +
                    "📊 Jami mushuk: " + totalAds + " ta\n\n" +
                    "💵 Daromad:\n" +
                    "📊 Sotilgan reklama: " + todaySell + " ta\n" +
                    "💰 Har bir reklama: 35,000 so'm\n" +
                    "💵 Jami daromad: " + String.format("%,d", income) + " so'm\n" +
                    "⏰ Sana: " + new SimpleDateFormat("dd.MM.yyyy").format(new Date());

            sendStatsMessage(chatId, message);
        }

        private void sendStatsMessage(long chatId, String message) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(message);
            execute(msg);

            // Statistika menyusiga qaytish
            sendStatsMenu(chatId);
        }

        private int getTodayAdsCount(String adType) {
            String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
            int count = 0;

            for (AdRecord ad : approvedAds) {
                String adDate = new SimpleDateFormat("yyyy-MM-dd").format(ad.getCreatedAt());
                if (adDate.equals(today) && ad.getAdType().equals(adType)) {
                    count++;
                }
            }
            return count;
        }

        private int getWeeklyAdsCount(String adType) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            Date weekAgo = cal.getTime();
            int count = 0;

            for (AdRecord ad : approvedAds) {
                if (ad.getCreatedAt().after(weekAgo) && ad.getAdType().equals(adType)) {
                    count++;
                }
            }
            return count;
        }

        private void addToStats(long userId) {
            // Userning eng so'nggi e'lonini topamiz
            String adType = adTypeMap.get(userId);
            if (adType != null) {
                AdRecord newAd = new AdRecord(
                        adIdCounter.get(),
                        adType,
                        new Date(),
                        true
                );
                approvedAds.add(newAd);
                System.out.println("✅ Statistika yangilandi: " + adType + " - " + new Date());
            }
        }

        // ========== YORDAMCHI METODLAR ==========

        private void sendMainMenu(long chatId) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText("🐱 Assalomu alaykum! UzbekCats botiga xush kelibsiz!\n\nQuyidagilardan birini tanlang:");

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            // 1-qator
            InlineKeyboardButton b1 = new InlineKeyboardButton();
            b1.setText("📢 Reklama joylash");
            b1.setCallbackData("menu_reklama");
            rows.add(Collections.singletonList(b1));

            // 2-qator
            InlineKeyboardButton b2 = new InlineKeyboardButton();
            b2.setText("👤 Admin bilan bog'lanish");
            b2.setCallbackData("menu_admin");
            rows.add(Collections.singletonList(b2));

            // 3-qator
            InlineKeyboardButton b3 = new InlineKeyboardButton();
            b3.setText("💰 Narxlar");
            b3.setCallbackData("menu_narx");
            rows.add(Collections.singletonList(b3));

            // 4-qator
            InlineKeyboardButton b4 = new InlineKeyboardButton();
            b4.setText("ℹ️ Bot haqida");
            b4.setCallbackData("menu_haqida");
            rows.add(Collections.singletonList(b4));

            // Admin uchun statistika tugmasi
            if (chatId == ADMIN_ID) {
                InlineKeyboardButton statsBtn = new InlineKeyboardButton();
                statsBtn.setText("📊 Statistika");
                statsBtn.setCallbackData("menu_stats");
                rows.add(Collections.singletonList(statsBtn));
            }

            markup.setKeyboard(rows);
            msg.setReplyMarkup(markup);
            execute(msg);
        }

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

            // Har bir viloyat uchun tugma
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

            // 4 ta qatorga bo'lamiz (har qatorda 3 ta)
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

            // Birinchi rasmni yuboramiz
            SendPhoto photo = new SendPhoto();
            photo.setChatId(String.valueOf(chatId));
            photo.setPhoto(new InputFile(userPhotos.get(0)));
            photo.setCaption(buildPreviewCaption(chatId));

            // Tasdiqlash tugmalari
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
            sb.append("📋 E'lon ma'lumotlari:\n\n");

            sb.append("📍 Manzil: ").append(manzilMap.getOrDefault(chatId, "—")).append("\n");
            sb.append("📞 Telefon: ").append(phoneMap.getOrDefault(chatId, "—")).append("\n");

            if ("sotish".equals(adTypeMap.get(chatId))) {
                sb.append("🐱 Zot: ").append(breedMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("🎂 Yosh: ").append(ageMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("❤️ Sog'lig'i: ").append(healthMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("👤 Jins: ").append(genderMap.getOrDefault(chatId, "—")).append("\n");
                sb.append("💰 Narx: ").append(priceMap.getOrDefault(chatId, "—")).append("\n");
            }

            sb.append("\nMa'lumotlaringiz to'g'rimi?");
            return sb.toString();
        }

        private void notifyAdmin(long chatId) throws TelegramApiException {
            long adId = adIdCounter.incrementAndGet();

            String caption = "🆕 Yangi e'lon! ID: " + adId + "\n\n" +
                    "Tur: " + ("sotish".equals(adTypeMap.get(chatId)) ? "Sotish" : "Hadiya") + "\n" +
                    buildPreviewCaption(chatId);

            // Admin ga ma'lumot
            SendMessage info = new SendMessage();
            info.setChatId(String.valueOf(ADMIN_ID));
            info.setText(caption);
            execute(info);

            // Admin ga rasm
            List<String> userPhotos = photosMap.get(chatId);
            if (userPhotos != null && !userPhotos.isEmpty()) {
                SendPhoto photo = new SendPhoto();
                photo.setChatId(String.valueOf(ADMIN_ID));
                photo.setPhoto(new InputFile(userPhotos.get(0)));
                photo.setCaption("E'lon rasmi - ID: " + adId);

                // Admin tugmalari
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                InlineKeyboardButton approve = new InlineKeyboardButton();
                approve.setText("✅ Tasdiqlash");
                approve.setCallbackData("approve_" + chatId);

                InlineKeyboardButton decline = new InlineKeyboardButton();
                decline.setText("❌ Rad etish");
                decline.setCallbackData("decline_" + chatId);

                markup.setKeyboard(Arrays.asList(
                        Collections.singletonList(approve),
                        Collections.singletonList(decline)
                ));
                photo.setReplyMarkup(markup);

                execute(photo);
            }

            // Agar chek bo'lsa
            if (checkMap.containsKey(chatId)) {
                SendPhoto check = new SendPhoto();
                check.setChatId(String.valueOf(ADMIN_ID));
                check.setPhoto(new InputFile(checkMap.get(chatId)));
                check.setCaption("💳 To'lov cheki - ID: " + adId);
                execute(check);
            }
        }

        private void postToChannel(long userId) throws TelegramApiException {
            List<String> userPhotos = photosMap.get(userId);
            if (userPhotos == null || userPhotos.isEmpty()) return;

            String hashtag = "sotish".equals(adTypeMap.get(userId)) ? "#SOTILADI" : "#HADIYAGA";

            StringBuilder caption = new StringBuilder();
            caption.append(hashtag).append("\n\n");

            caption.append("📍 Manzil: ").append(manzilMap.getOrDefault(userId, "—")).append("\n");
            caption.append("📞 Telefon: ").append(phoneMap.getOrDefault(userId, "—")).append("\n");

            if ("sotish".equals(adTypeMap.get(userId))) {
                caption.append("🐱 Zot: ").append(breedMap.getOrDefault(userId, "—")).append("\n");
                caption.append("🎂 Yosh: ").append(ageMap.getOrDefault(userId, "—")).append("\n");
                caption.append("❤️ Sog'lig'i: ").append(healthMap.getOrDefault(userId, "—")).append("\n");
                caption.append("👤 Jins: ").append(genderMap.getOrDefault(userId, "—")).append("\n");
                caption.append("💰 Narx: ").append(priceMap.getOrDefault(userId, "—")).append("\n");
            }

            caption.append("\n📣 Biz bilan bog'laning!");

            SendPhoto post = new SendPhoto();
            post.setChatId(CHANNEL_USERNAME);
            post.setPhoto(new InputFile(userPhotos.get(0)));
            post.setCaption(caption.toString());
            execute(post);
        }

        private void sendPriceList(long chatId) throws TelegramApiException {
            sendText(chatId, "💰 Narxlar:\n\n" +
                    "📢 1 ta reklama - 35,000 so'm\n" +
                    "📢 5 ta reklama - 130,000 so'm\n" +
                    "🌐 Instagram/Telegram reklama - 200,000 so'm");
        }

        private void sendBotInfo(long chatId) throws TelegramApiException {
            sendText(chatId, "🤖 Bot haqida:\n\n" +
                    "Ushbu bot orqali siz mushuklar haqida e'lon berishingiz mumkin.\n\n" +
                    "Taklif va shikoyatlar va sizgayam bot kerakbulsa shu numerga aloqaga chiqing: +998900512621");
        }

        private void sendText(long chatId, String text) throws TelegramApiException {
            SendMessage msg = new SendMessage();
            msg.setChatId(String.valueOf(chatId));
            msg.setText(text);
            execute(msg);
        }
    }
}
