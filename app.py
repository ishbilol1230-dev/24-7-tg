import os
import logging
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from dataclasses import dataclass
from enum import Enum

from telegram import (
    Update, 
    InlineKeyboardButton, 
    InlineKeyboardMarkup, 
    InputMediaPhoto,
    PhotoSize
)
from telegram.ext import (
    Application, 
    CommandHandler, 
    CallbackQueryHandler, 
    MessageHandler, 
    filters, 
    ContextTypes
)

# Log konfiguratsiyasi
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO
)
logger = logging.getLogger(__name__)

# Konstanta qiymatlar
BOT_TOKEN = "8333145996:AAG2c5dEZ2YAXVSbw-aGO0f-EJaqDOCCRxQ"
ADMIN_ID = 7038296036
CHANNEL_USERNAME = "@arzon_mushukll"

# Viloyatlar ro'yxati
VILOYATLAR = [
    "Andijon", "Buxoro", "Farg'ona", "Jizzax", "Xorazm",
    "Namangan", "Navoiy", "Qashqadaryo", "Samarqand",
    "Sirdaryo", "Surxondaryo", "Toshkent", "Toshkent shahar"
]

# Mushuk zotlari
BREEDS = [
    "Scottish fol", "Scottish strayt", "Britiskiy",
    "Shinshila", "Uy mushuki"
]

# Yoshlar
AGES = [
    "1 oylik", "2 oylik", "3 oylik", "4 oylik", "5 oylik",
    "6 oylik", "7 oylik", "8 oylik", "9 oylik",
    "+1 yosh", "+1.5 yosh", "+2 yosh"
]

# State lar
class UserState(Enum):
    AWAIT_PHOTO = "await_photo"
    AWAIT_PHONE = "await_phone"
    AWAIT_PRICE = "await_price"
    WAIT_CHECK = "wait_check"
    WAITING_ADMIN = "waiting_admin"

@dataclass
class AdRecord:
    ad_id: int
    ad_type: str
    created_at: datetime
    approved: bool = False

class CatBot:
    def __init__(self):
        # User ma'lumotlari
        self.state_map: Dict[int, UserState] = {}
        self.photos_map: Dict[int, List[str]] = {}
        self.manzil_map: Dict[int, str] = {}
        self.phone_map: Dict[int, str] = {}
        self.breed_map: Dict[int, str] = {}
        self.age_map: Dict[int, str] = {}
        self.health_map: Dict[int, str] = {}
        self.gender_map: Dict[int, str] = {}
        self.price_map: Dict[int, str] = {}
        self.check_map: Dict[int, str] = {}
        self.ad_type_map: Dict[int, str] = {}
        
        # Statistika
        self.approved_ads: List[AdRecord] = []
        self.ad_id_counter = 1000

    async def start(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Start komandasi"""
        chat_id = update.effective_chat.id
        await self.send_main_menu(chat_id, context)

    async def handle_message(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Xabarlarni qayta ishlash"""
        message = update.message
        chat_id = message.chat_id
        state = self.state_map.get(chat_id)

        if message.text:
            text = message.text.strip()

            if text == "/start":
                await self.send_main_menu(chat_id, context)
                return

            # Narx kiritish
            if state == UserState.AWAIT_PRICE:
                self.price_map[chat_id] = text
                await self.send_preview(chat_id, context)
                return

            # Telefon raqam kiritish
            if state == UserState.AWAIT_PHONE:
                self.phone_map[chat_id] = text
                ad_type = self.ad_type_map.get(chat_id)

                if ad_type == "hadiya":
                    await self.send_preview(chat_id, context)
                else:
                    await self.send_breed_selection(chat_id, context)
                return

            await context.bot.send_message(chat_id, "Iltimos, tugmalardan foydalaning yoki /start ni bosing.")
            return

        # Rasm qabul qilish
        if message.photo:
            if state == UserState.AWAIT_PHOTO:
                photos = message.photo
                file_id = photos[-1].file_id

                if chat_id not in self.photos_map:
                    self.photos_map[chat_id] = []

                self.photos_map[chat_id].append(file_id)

                # Agar 2 ta rasm yuborilgan bo'lsa
                if len(self.photos_map[chat_id]) >= 2:
                    await self.send_viloyat_selection(chat_id, context)
                else:
                    await context.bot.send_message(chat_id, "✅ 1-rasm qabul qilindi. Iltimos, 2-rasmni yuboring:")
                return

            # Chek qabul qilish
            if state == UserState.WAIT_CHECK:
                photos = message.photo
                file_id = photos[-1].file_id
                self.check_map[chat_id] = file_id
                await context.bot.send_message(chat_id, "✅ Chek qabul qilindi. Admin tekshiradi.")
                await self.notify_admin(chat_id, context)
                self.state_map[chat_id] = UserState.WAITING_ADMIN
                return

    async def handle_callback(self, update: Update, context: ContextTypes.DEFAULT_TYPE):
        """Callback query larni qayta ishlash"""
        query = update.callback_query
        await query.answer()
        
        chat_id = query.message.chat_id
        data = query.data
        user_id = query.from_user.id

        if data == "menu_reklama":
            await self.send_ad_type_selection(chat_id, context)
        elif data == "menu_admin":
            await context.bot.send_message(chat_id, "👤 Admin bilan bog'lanish:\n\n📶 @zayd_catlover\n\n📞 +998934938181")
        elif data == "menu_narx":
            await self.send_price_list(chat_id, context)
        elif data == "menu_haqida":
            await self.send_bot_info(chat_id, context)
        elif data == "menu_stats":
            if user_id == ADMIN_ID:
                await self.send_stats_menu(chat_id, context)

        # Reklama turi
        elif data == "adtype_sotish":
            self.ad_type_map[chat_id] = "sotish"
            await self.start_ad_process(chat_id, context)
        elif data == "adtype_hadiya":
            self.ad_type_map[chat_id] = "hadiya"
            await self.start_ad_process(chat_id, context)

        # Viloyat tanlash
        elif data.startswith("viloyat_"):
            viloyat = data.replace("viloyat_", "").replace("_", " ").title()
            self.manzil_map[chat_id] = viloyat
            self.state_map[chat_id] = UserState.AWAIT_PHONE
            await context.bot.send_message(chat_id, f"📍 Manzil: {viloyat}\n📞 Endi telefon raqamingizni yuboring:(masalan +998** *** ** **")

        # Breed tanlash
        elif data.startswith("breed_"):
            breed = data.replace("breed_", "").replace("_", " ").title()
            self.breed_map[chat_id] = breed
            await self.send_age_selection(chat_id, context)

        # Yosh tanlash
        elif data.startswith("age_"):
            age = data.replace("age_", "").replace("_", " ").replace("1 5", "1.5")
            self.age_map[chat_id] = age
            await self.send_health_selection(chat_id, context)

        # Sog'lik tanlash
        elif data == "health_soglom":
            self.health_map[chat_id] = "Sog'lom"
            await self.send_gender_selection(chat_id, context)
        elif data == "health_kasal":
            self.health_map[chat_id] = "Kasal"
            await self.send_gender_selection(chat_id, context)

        # Jins tanlash
        elif data == "gender_qiz":
            self.gender_map[chat_id] = "Qiz bola"
            if self.ad_type_map.get(chat_id) == "sotish":
                self.state_map[chat_id] = UserState.AWAIT_PRICE
                await context.bot.send_message(chat_id, "💰 Narxni kiriting (faqat raqamda):")
            else:
                await self.send_preview(chat_id, context)
        elif data == "gender_ogil":
            self.gender_map[chat_id] = "O'g'il bola"
            if self.ad_type_map.get(chat_id) == "sotish":
                self.state_map[chat_id] = UserState.AWAIT_PRICE
                await context.bot.send_message(chat_id, "💰 Narxni kiriting (faqat raqamda):")
            else:
                await self.send_preview(chat_id, context)

        # Preview tugmalari
        elif data == "preview_confirm":
            if self.ad_type_map.get(chat_id) == "sotish":
                await context.bot.send_message(chat_id, 
                    "💳 To'lov ma'lumotlari:\nKarta: **** **** **** ****\nMiqdor: 35,000 so'm\nTo'lov qilib, chekni rasmini yuboring.")
                self.state_map[chat_id] = UserState.WAIT_CHECK
            else:
                await context.bot.send_message(chat_id, 
                    "✅ Ma'lumotlaringiz qabul qilindi! Admin tekshirib kanalga joylaydi.")
                await self.notify_admin(chat_id, context)
                self.state_map[chat_id] = UserState.WAITING_ADMIN

        elif data == "preview_back":
            self.state_map[chat_id] = UserState.AWAIT_PHOTO
            if chat_id in self.photos_map:
                del self.photos_map[chat_id]
            await context.bot.send_message(chat_id, "↩️ Orqaga qaytildi. Iltimos, rasmlarni qayta yuboring.")

        # Statistika tugmalari
        elif data == "stats_today_sell":
            if user_id == ADMIN_ID:
                await self.show_today_sell_stats(chat_id, context)
        elif data == "stats_today_gift":
            if user_id == ADMIN_ID:
                await self.show_today_gift_stats(chat_id, context)
        elif data == "stats_weekly":
            if user_id == ADMIN_ID:
                await self.show_weekly_stats(chat_id, context)
        elif data == "stats_today_income":
            if user_id == ADMIN_ID:
                await self.show_today_income_stats(chat_id, context)

        # Admin tasdiqlash
        elif data.startswith("approve_"):
            if user_id == ADMIN_ID:
                uid = int(data.replace("approve_", ""))
                await self.post_to_channel(uid, context)
                await context.bot.send_message(uid, "✅ E'loningiz kanalga joylandi!")
                await context.bot.send_message(ADMIN_ID, "✅ E'lon admen tomonidan tasdiqlandi va joylandi.")
                self.add_to_stats(uid)

        elif data.startswith("decline_"):
            if user_id == ADMIN_ID:
                uid = int(data.replace("decline_", ""))
                await context.bot.send_message(uid, "❌ E'loningiz tasdiqlanmadi. Admin bilan bog'laning.")
                await context.bot.send_message(ADMIN_ID, "❌ E'lon rad etildi.")

    # ========== STATISTIKA METODLARI ==========

    async def send_stats_menu(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Statistika menyusini yuborish"""
        keyboard = [
            [InlineKeyboardButton("💰 Bugun sotishga", callback_data="stats_today_sell")],
            [InlineKeyboardButton("🎁 Bugun hadiyaga", callback_data="stats_today_gift")],
            [InlineKeyboardButton("📈 1 haftalik", callback_data="stats_weekly")],
            [InlineKeyboardButton("💵 Bugun daromad", callback_data="stats_today_income")]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "📊 Statistika menyusi:\n\nQuyidagilardan birini tanlang:", reply_markup=reply_markup)

    async def show_today_sell_stats(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Bugungi sotish statistikasi"""
        count = self.get_today_ads_count("sotish")
        message = f"💰 Bugun sotishga qo'yilgan mushuklar:\n\n📊 Soni: {count} ta\n⏰ Sana: {datetime.now().strftime('%d.%m.%Y')}"
        await self.send_stats_message(chat_id, message, context)

    async def show_today_gift_stats(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Bugungi hadiya statistikasi"""
        count = self.get_today_ads_count("hadiya")
        message = f"🎁 Bugun hadiyaga qo'yilgan mushuklar:\n\n📊 Soni: {count} ta\n⏰ Sana: {datetime.now().strftime('%d.%m.%Y')}"
        await self.send_stats_message(chat_id, message, context)

    async def show_weekly_stats(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Haftalik statistika"""
        weekly_sell = self.get_weekly_ads_count("sotish")
        weekly_gift = self.get_weekly_ads_count("hadiya")
        total = weekly_sell + weekly_gift
        
        message = f"📈 1 haftalik statistika:\n\n💰 Sotishga: {weekly_sell} ta\n🎁 Hadiyaga: {weekly_gift} ta\n📊 Jami: {total} ta\n📅 Davr: oxirgi 7 kun"
        await self.send_stats_message(chat_id, message, context)

    async def show_today_income_stats(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Bugungi daromad statistikasi"""
        today_sell = self.get_today_ads_count("sotish")
        today_gift = self.get_today_ads_count("hadiya")
        total_ads = today_sell + today_gift
        income = today_sell * 35000
        
        message = (
            f"💵 Bugungi statistika:\n\n"
            f"💰 Sotishga: {today_sell} ta\n"
            f"🎁 Hadiyaga: {today_gift} ta\n"
            f"📊 Jami mushuk: {total_ads} ta\n\n"
            f"💵 Daromad:\n"
            f"📊 Sotilgan reklama: {today_sell} ta\n"
            f"💰 Har bir reklama: 35,000 so'm\n"
            f"💵 Jami daromad: {income:,} so'm\n"
            f"⏰ Sana: {datetime.now().strftime('%d.%m.%Y')}"
        )
        await self.send_stats_message(chat_id, message, context)

    async def send_stats_message(self, chat_id: int, message: str, context: ContextTypes.DEFAULT_TYPE):
        """Statistika xabarini yuborish"""
        await context.bot.send_message(chat_id, message)
        await self.send_stats_menu(chat_id, context)

    def get_today_ads_count(self, ad_type: str) -> int:
        """Bugungi e'lonlar soni"""
        today = datetime.now().date()
        count = 0
        
        for ad in self.approved_ads:
            if ad.created_at.date() == today and ad.ad_type == ad_type:
                count += 1
        return count

    def get_weekly_ads_count(self, ad_type: str) -> int:
        """Haftalik e'lonlar soni"""
        week_ago = datetime.now() - timedelta(days=7)
        count = 0
        
        for ad in self.approved_ads:
            if ad.created_at > week_ago and ad.ad_type == ad_type:
                count += 1
        return count

    def add_to_stats(self, user_id: int):
        """Statistikaga qo'shish"""
        ad_type = self.ad_type_map.get(user_id)
        if ad_type:
            new_ad = AdRecord(
                ad_id=self.ad_id_counter,
                ad_type=ad_type,
                created_at=datetime.now(),
                approved=True
            )
            self.approved_ads.append(new_ad)
            self.ad_id_counter += 1
            logger.info(f"✅ Statistika yangilandi: {ad_type} - {datetime.now()}")

    # ========== YORDAMCHI METODLARI ==========

    async def send_main_menu(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Asosiy menyuni yuborish"""
        keyboard = [
            [InlineKeyboardButton("📢 Reklama joylash", callback_data="menu_reklama")],
            [InlineKeyboardButton("👤 Admin bilan bog'lanish", callback_data="menu_admin")],
            [InlineKeyboardButton("💰 Narxlar", callback_data="menu_narx")],
            [InlineKeyboardButton("ℹ️ Bot haqida", callback_data="menu_haqida")]
        ]
        
        # Admin uchun statistika tugmasi
        if chat_id == ADMIN_ID:
            keyboard.append([InlineKeyboardButton("📊 Statistika", callback_data="menu_stats")])
        
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(
            chat_id, 
            "🐱 Assalomu alaykum! UzbekCats botiga xush kelibsiz!\n\nQuyidagilardan birini tanlang:",
            reply_markup=reply_markup
        )

    async def send_ad_type_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Reklama turini tanlash"""
        keyboard = [
            [InlineKeyboardButton("💰 Sotiladigan", callback_data="adtype_sotish")],
            [InlineKeyboardButton("🎁 Hadiyaga", callback_data="adtype_hadiya")]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "🎯 Reklama turini tanlang:", reply_markup=reply_markup)

    async def start_ad_process(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Reklama jarayonini boshlash"""
        self.state_map[chat_id] = UserState.AWAIT_PHOTO
        self.photos_map[chat_id] = []
        await context.bot.send_message(chat_id, "📸 Iltimos, 2 ta rasm yuboring:")

    async def send_viloyat_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Viloyat tanlash"""
        keyboard = []
        for viloyat in VILOYATLAR:
            callback_data = f"viloyat_{viloyat.lower().replace(' ', '_').replace("'", '')}"
            keyboard.append([InlineKeyboardButton(viloyat, callback_data=callback_data)])
        
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "📍 Manzilingizni tanlang:", reply_markup=reply_markup)

    async def send_breed_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Mushuk zotini tanlash"""
        keyboard = []
        for breed in BREEDS:
            callback_data = f"breed_{breed.lower().replace(' ', '_')}"
            keyboard.append([InlineKeyboardButton(breed, callback_data=callback_data)])
        
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "🐱 Mushuk zotini tanlang:", reply_markup=reply_markup)

    async def send_age_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Yosh tanlash"""
        keyboard = []
        for i in range(0, len(AGES), 3):
            row = []
            for j in range(i, min(i + 3, len(AGES))):
                age = AGES[j]
                callback_data = f"age_{age.lower().replace(' ', '_').replace('+', '').replace('.', '_')}"
                row.append(InlineKeyboardButton(age, callback_data=callback_data))
            keyboard.append(row)
        
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "🎂 Yoshini tanlang:", reply_markup=reply_markup)

    async def send_health_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Sog'lik tanlash"""
        keyboard = [
            [
                InlineKeyboardButton("Sog'lom", callback_data="health_soglom"),
                InlineKeyboardButton("Kasal", callback_data="health_kasal")
            ]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "❤️ Sog'lig'ini tanlang:", reply_markup=reply_markup)

    async def send_gender_selection(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Jins tanlash"""
        keyboard = [
            [
                InlineKeyboardButton("Qiz bola", callback_data="gender_qiz"),
                InlineKeyboardButton("O'g'il bola", callback_data="gender_ogil")
            ]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)
        await context.bot.send_message(chat_id, "👤 Jinsini tanlang:", reply_markup=reply_markup)

    async def send_preview(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """E'lon preview ni yuborish"""
        user_photos = self.photos_map.get(chat_id)
        if not user_photos:
            await context.bot.send_message(chat_id, "Xatolik: Rasmlar topilmadi.")
            return

        # Preview caption yaratish
        caption = self.build_preview_caption(chat_id)

        # Tasdiqlash tugmalari
        keyboard = [
            [
                InlineKeyboardButton("✅ Tasdiqlash", callback_data="preview_confirm"),
                InlineKeyboardButton("↩️ Orqaga", callback_data="preview_back")
            ]
        ]
        reply_markup = InlineKeyboardMarkup(keyboard)

        # Rasmni yuborish
        await context.bot.send_photo(
            chat_id=chat_id,
            photo=user_photos[0],
            caption=caption,
            reply_markup=reply_markup
        )

    def build_preview_caption(self, chat_id: int) -> str:
        """Preview caption yaratish"""
        caption = "📋 E'lon ma'lumotlari:\n\n"
        caption += f"📍 Manzil: {self.manzil_map.get(chat_id, '—')}\n"
        caption += f"📞 Telefon: {self.phone_map.get(chat_id, '—')}\n"

        if self.ad_type_map.get(chat_id) == "sotish":
            caption += f"🐱 Zot: {self.breed_map.get(chat_id, '—')}\n"
            caption += f"🎂 Yosh: {self.age_map.get(chat_id, '—')}\n"
            caption += f"❤️ Sog'lig'i: {self.health_map.get(chat_id, '—')}\n"
            caption += f"👤 Jins: {self.gender_map.get(chat_id, '—')}\n"
            caption += f"💰 Narx: {self.price_map.get(chat_id, '—')}\n"

        caption += "\nMa'lumotlaringiz to'g'rimi?"
        return caption

    async def notify_admin(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Admin ga bildirish"""
        ad_id = self.ad_id_counter
        self.ad_id_counter += 1

        caption = f"🆕 Yangi e'lon! ID: {ad_id}\n\n"
        caption += f"Tur: {'Sotish' if self.ad_type_map.get(chat_id) == 'sotish' else 'Hadiya'}\n"
        caption += self.build_preview_caption(chat_id)

        # Admin ga ma'lumot
        await context.bot.send_message(ADMIN_ID, caption)

        # Admin ga rasm
        user_photos = self.photos_map.get(chat_id)
        if user_photos:
            # Admin tugmalari
            keyboard = [
                [InlineKeyboardButton("✅ Tasdiqlash", callback_data=f"approve_{chat_id}")],
                [InlineKeyboardButton("❌ Rad etish", callback_data=f"decline_{chat_id}")]
            ]
            reply_markup = InlineKeyboardMarkup(keyboard)

            await context.bot.send_photo(
                chat_id=ADMIN_ID,
                photo=user_photos[0],
                caption=f"E'lon rasmi - ID: {ad_id}",
                reply_markup=reply_markup
            )

        # Agar chek bo'lsa
        if chat_id in self.check_map:
            await context.bot.send_photo(
                chat_id=ADMIN_ID,
                photo=self.check_map[chat_id],
                caption=f"💳 To'lov cheki - ID: {ad_id}"
            )

    async def post_to_channel(self, user_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Kanalga e'lon joylash"""
        user_photos = self.photos_map.get(user_id)
        if not user_photos:
            return

        hashtag = "#SOTILADI" if self.ad_type_map.get(user_id) == "sotish" else "#HADIYAGA"
        
        caption = f"{hashtag}\n\n"
        caption += f"📍 Manzil: {self.manzil_map.get(user_id, '—')}\n"
        caption += f"📞 Telefon: {self.phone_map.get(user_id, '—')}\n"

        if self.ad_type_map.get(user_id) == "sotish":
            caption += f"🐱 Zot: {self.breed_map.get(user_id, '—')}\n"
            caption += f"🎂 Yosh: {self.age_map.get(user_id, '—')}\n"
            caption += f"❤️ Sog'lig'i: {self.health_map.get(user_id, '—')}\n"
            caption += f"👤 Jins: {self.gender_map.get(user_id, '—')}\n"
            caption += f"💰 Narx: {self.price_map.get(user_id, '—')}\n"

        caption += "\n📣 Biz bilan bog'laning!"

        await context.bot.send_photo(
            chat_id=CHANNEL_USERNAME,
            photo=user_photos[0],
            caption=caption
        )

    async def send_price_list(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Narxlar ro'yxati"""
        message = (
            "💰 Narxlar:\n\n"
            "📢 1 ta reklama - 35,000 so'm\n"
            "📢 5 ta reklama - 130,000 so'm\n"
            "🌐 Instagram/Telegram reklama - 200,000 so'm"
        )
        await context.bot.send_message(chat_id, message)

    async def send_bot_info(self, chat_id: int, context: ContextTypes.DEFAULT_TYPE):
        """Bot haqida ma'lumot"""
        message = (
            "🤖 Bot haqida:\n\n"
            "Ushbu bot orqali siz mushuklar haqida e'lon berishingiz mumkin.\n\n"
            "Taklif va shikoyatlar va sizgayam bot kerakbulsa shu numerga aloqaga chiqing: +998900512621"
        )
        await context.bot.send_message(chat_id, message)

def main():
    """Asosiy dastur"""
    # Bot yaratish
    bot = CatBot()
    
    # Application yaratish
    application = Application.builder().token(BOT_TOKEN).build()

    # Handler lar
    application.add_handler(CommandHandler("start", bot.start))
    application.add_handler(CallbackQueryHandler(bot.handle_callback))
    application.add_handler(MessageHandler(filters.TEXT | filters.PHOTO, bot.handle_message))

    # Botni ishga tushirish
    print("✅ Bot ishga tushdi!")
    application.run_polling()

if __name__ == "__main__":
    main()
