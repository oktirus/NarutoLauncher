package ua.konoha.tv.nativepreview.model;

import java.util.*;

public class CatalogData {

    public static int parseColor(String hex) {
        if (hex == null || !hex.startsWith("#")) return 0xFFFFFFFF;
        String clean = hex.substring(1);
        if (clean.length() == 6) {
            return 0xFF000000 | (int) Long.parseLong(clean, 16);
        } else if (clean.length() == 8) {
            return (int) Long.parseLong(clean, 16);
        }
        return 0xFFFFFFFF;
    }

    public static final List<Scene> SCENES = Arrays.asList(
        new Scene("dusk", "Вечірній міст", "Там, де чекають", "Тепло над каналом", "#ed943f",
            false, "Воля<br><em>вогню.</em>", "Після довгого дня шлях веде додому.<br>У тихому селі теж живе воля вогню.",
            "scenes/scene-dusk.jpg", "scenes/thumb-dusk.webp", "leaf", new int[]{parseColor("#e9af78"), parseColor("#b8b180")}),
        new Scene("team7", "Команда №7", "Перше спільне фото", "Наруто, Сакура, Саске й Какаші", "#b5cc99",
            false, "Разом<br><em>сильніші.</em>", "Класичне спільне фото команди №7.",
            "scenes/scene-team7.jpg", "scenes/thumb-team7.webp", "leaf", new int[]{parseColor("#b8cc8a"), parseColor("#91b1a0")}),
        new Scene("dawn", "Перед тренуванням", "Спробувати ще раз", "Ранок на полі", "#c7d99f",
            false, "Воля<br><em>вогню.</em>", "Нова спроба починається з рішення.<br>Попереду — ще один день тренувань.",
            "scenes/scene-dawn.jpg", "scenes/thumb-dawn.webp", "leaf", new int[]{parseColor("#d7c59d"), parseColor("#a3b89a")}),
        new Scene("ramen", "Рамен в Ічіраку", "Смак дому", "Рамен після довгого дня", "#eda653",
            false, "Смак<br><em>дому.</em>", "Наруто їсть рамен біля стійки Ічіраку.",
            "scenes/scene-ramen.jpg", "scenes/thumb-ramen.webp", "dust", new int[]{parseColor("#f0c88e"), parseColor("#e6b775")}),
        new Scene("kakashi", "Какаші", "Два дзвінки. Одна команда.", "Ранок на тренувальному полі", "#b6cfdb",
            true, "Сила<br><em>довіри.</em>", "Минуле навчило цінувати тих, хто поруч.<br>Майбутнє починається з довіри.",
            "scenes/scene-kakashi.jpg", "scenes/thumb-kakashi.webp", "leaf", new int[]{parseColor("#b0c3a1"), parseColor("#b4c5cc")}),
        new Scene("sakura", "Сакура", "Лікування чакрою", "Практика медичного ніндзюцу", "#f5b5c8",
            true, "Сила<br><em>зцілення.</em>", "Сакура відпрацьовує медичну техніку за столом.",
            "scenes/scene-sakura.jpg", "scenes/thumb-sakura.webp", "leaf", new int[]{parseColor("#f3bdce"), parseColor("#ecc3d4")}),
        new Scene("sasuke", "Саске", "Дорога крізь тишу", "Осінній перевал", "#b3b5e4",
            true, "Власний<br><em>шлях.</em>", "Відстань залишає місце для роздумів.<br>Серед багатьох доріг існує й дорога додому.",
            "scenes/scene-sasuke.jpg", "scenes/thumb-sasuke.webp", "leaf", new int[]{parseColor("#b87860"), parseColor("#be967b")}),
        new Scene("gaara", "Гаара", "Відповідальність за Суну", "Кабінет Казекаге", "#e9bc83",
            true, "П’ятий<br><em>Казекаге.</em>", "Сила знаходить сенс у турботі.<br>Пісок оберігає село, що стало домом.",
            "scenes/scene-gaara.jpg", "scenes/thumb-gaara.webp", "dust", new int[]{parseColor("#e9bc83"), parseColor("#bfa27a")}),
        new Scene("hinata", "Хіната", "Тиха рішучість", "Вечір біля тренувального паркану", "#c1a5d6",
            true, "Своя<br><em>хоробрість.</em>", "Хоробрість не завжди гучна.<br>Крок уперед починається зі щирого серця.",
            "scenes/scene-hinata.jpg", "scenes/thumb-hinata.webp", "leaf", new int[]{parseColor("#c1a5d6"), parseColor("#9b8bb8")}),
        new Scene("shikamaru", "Шикамару", "Хмари над дахом", "Спостереження за небом", "#b8d0a4",
            true, "Мудрість<br><em>спокою.</em>", "Не обов’язково поспішати за кожною думкою.<br>Найкращі рішення приходять у тиші.",
            "scenes/scene-shikamaru.jpg", "scenes/thumb-shikamaru.webp", "leaf", new int[]{parseColor("#b8d0a4"), parseColor("#8ea67a")}),
        new Scene("rocklee", "Рок Лі", "Ще один стрибок", "Ранкове тренування зі скакалкою", "#b8d68b",
            true, "Сила<br><em>зусиль.</em>", "Ритм, витривалість і щоденна праця.",
            "scenes/scene-rocklee.jpg", "scenes/thumb-rocklee.webp", "dust", new int[]{parseColor("#d4b68b"), parseColor("#b6c78d")}),
        new Scene("neji", "Неджи", "За межами визначеної долі", "Сніг і вільні птахи", "#cbdce6",
            true, "Вільний<br><em>обирати.</em>", "Клітка більше не визначає шлях.<br>Попереду — небо й право обирати.",
            "scenes/scene-neji.jpg", "scenes/thumb-neji.webp", "snow", new int[]{parseColor("#eef6fa"), parseColor("#bfdae4")}),
        new Scene("choji", "Чоджі", "Сила, якою діляться", "Тепло спільного вечора", "#edb38b",
            true, "Сила<br><em>доброти.</em>", "За спільним столом завжди знайдеться місце.<br>Добре серце теж уміє захищати.",
            "scenes/scene-choji.jpg", "scenes/thumb-choji.webp", "ember", new int[]{parseColor("#f3cd83"), parseColor("#e4ad73")}),
        new Scene("jiraiya", "Джирая", "Наступна сторінка", "Нотатки і три миски рамену", "#d2b18a",
            true, "Історія<br><em>триває.</em>", "Кожна подорож залишає історію.<br>Найважливіші слова ще попереду.",
            "scenes/scene-jiraiya.jpg", "scenes/thumb-jiraiya.webp", "leaf", new int[]{parseColor("#a8be9a"), parseColor("#cab69b")}),
        new Scene("itachi", "Ітачі", "Невимовлені слова", "Веранда після дощу", "#b4bcd0",
            true, "Ціна<br><em>миру.</em>", "Дощ стихає. Тиша залишається.<br>Деякі почуття не потребують слів.",
            "scenes/scene-itachi.jpg", "scenes/thumb-itachi.webp", "rain", new int[]{parseColor("#a9baca"), parseColor("#8b9ca7")}),
        new Scene("tsunade", "Цунаде", "Надія повертається", "Світлий ранок у лікарні", "#b9cfa1",
            true, "Сила<br><em>надії.</em>", "Відвага буває лагідною.<br>Дати сили жити — теж шлях шинобі.",
            "scenes/scene-tsunade.jpg", "scenes/thumb-tsunade.webp", "leaf", new int[]{parseColor("#b9cc9f"), parseColor("#d1c4a5")}),
        new Scene("minato", "Мінато й Кушіна", "Тихі миті разом", "Разом на лісовій галявині", "#e3d38e",
            true, "Мить<br><em>рішення.</em>", "Мінато та Кушіна відпочивають після тренування.",
            "scenes/scene-minato.jpg", "scenes/thumb-minato.webp", "leaf", new int[]{parseColor("#d1cd95"), parseColor("#a9ba92")}),
        new Scene("sai", "Сай", "Малюнок здіймається в небо", "Політ на чорнильному птахові", "#b9cddd",
            true, "Мова<br><em>почуттів.</em>", "Сай летить над лісом на птахові, створеному чорнильною технікою.",
            "scenes/scene-sai.jpg", "scenes/thumb-sai.webp", "leaf", new int[]{parseColor("#b1c6b8"), parseColor("#b4cadb")})
    );

    public static final Map<String, Quote> QUOTES = new HashMap<>();
    static {
        QUOTES.put("dusk", new Quote("naruto", "Наруто Узумакі", "Я ніколи не відступаю від свого слова. Такий мій шлях ніндзя!"));
        QUOTES.put("team7", new Quote("naruto", "Наруто Узумакі", "Я ніколи не відступаю від свого слова. Такий мій шлях ніндзя!"));
        QUOTES.put("dawn", new Quote("naruto", "Наруто Узумакі", "Я ніколи не відступаю від свого слова. Такий мій шлях ніндзя!"));
        QUOTES.put("ramen", new Quote("naruto", "Наруто Узумакі", "Я ніколи не відступаю від свого слова. Такий мій шлях ніндзя!"));
        QUOTES.put("kakashi", new Quote("kakashi", "Какаші Хатаке", "Ті, хто кидає товаришів, гірші за покидьків."));
        QUOTES.put("sakura", new Quote("sakura", "Сакура Харуно", "Тепер моя черга вийти вперед."));
        QUOTES.put("sasuke", new Quote("sasuke", "Саске Учіха", "Це не мрія. Я втілю це в реальність."));
        QUOTES.put("gaara", new Quote("gaara", "Гаара", "Я хочу захистити цього друга і цей світ!"));
        QUOTES.put("hinata", new Quote("hinata", "Хіната Хюґа", "Я ніколи не відступаю від свого слова. Такий і мій шлях ніндзя!"));
        QUOTES.put("shikamaru", new Quote("shikamaru", "Шикамару Нара", "Іноді краще просто дивитися на хмари."));
        QUOTES.put("rocklee", new Quote("rocklee", "Рок Лі", "Старанна праця перемагає природний талант!"));
        QUOTES.put("neji", new Quote("neji", "Неджи Хюґа", "Небо вільне, і птах обирає, куди йому летіти."));
        QUOTES.put("choji", new Quote("choji", "Чоджі Акімічі", "Я не просто люблю поїсти. Я захищаю своїх друзів!"));
        QUOTES.put("jiraiya", new Quote("jiraiya", "Джирая", "Справжній шинобі — це той, хто ніколи не здається."));
        QUOTES.put("itachi", new Quote("itachi", "Ітачі Учіха", "Що б ти не робив далі, знай: я завжди любитиму тебе."));
        QUOTES.put("tsunade", new Quote("tsunade", "Цунаде", "Не недооцінюй Волю вогню!"));
        QUOTES.put("minato", new Quote("minato", "Мінато Намікадзе", "Повір у нього. Він же наш син!"));
        QUOTES.put("sai", new Quote("sai", "Сай", "Ось що я хотів показати братові. Малюнок наших мрій."));
    }

    public static final List<Technique> TECHNIQUES = Arrays.asList(
        new Technique("rasengan", "Расенґан", "Наруто", parseColor("#85e5ff"), "rasengan", "Щільні спіралі чакри й обертовий вихор."),
        new Technique("chidori", "Чідорі", "Саске / Какаші", parseColor("#b8dfff"), "chidori", "Рвані розгалуження блискавки навколо іконки."),
        new Technique("sharingan", "Шарінґан", "Клан Учіха", parseColor("#ff6265"), "sharingan", "Три томое на червоній райдужці."),
        new Technique("kurama", "Чакра Курами", "Наруто", parseColor("#ff9a46"), "kurama", "Дев’ять живих хвостів і теплий покрив чакри."),
        new Technique("summoning", "Виклик жаби", "Джирая / Наруто", parseColor("#d7c08b"), "summoning", "Печатка призову, дим і маленька жаба."),
        new Technique("hiraishin", "Хірайшин", "Мінато · Четвертий хокаге", parseColor("#ffe59b"), "hiraishin", "Трипроменевий кунай і золотий слід телепорту."),
        new Technique("fireball", "Вогняна куля", "Саске", parseColor("#ffb65c"), "fireball", "Вогняна корона з язиками полум’я."),
        new Technique("amaterasu", "Аматерасу", "Ітачі / Саске", parseColor("#c3a0ed"), "amaterasu", "Чорне полум’я з холодним фіолетовим краєм."),
        new Technique("rasenshuriken", "Расенсюрикен", "Наруто · стихія вітру", parseColor("#b7f1e5"), "rasenshuriken", "Чотири леза вітру навколо спірального ядра."),
        new Technique("gamabunta", "Стрибок Гамабунти", "Володар жаб", parseColor("#e7b789"), "gamabunta", "Гамабунта стрибає вперед і здіймає пил при приземленні.")
    );

    public static final List<MusicTrack> MUSIC_TRACKS = Arrays.asList(
        new MusicTrack("1584791975", "The Rising Fighting Spirit", "audio/1584791975.m4a"),
        new MusicTrack("1584792236", "Strong and Strike", "audio/1584792236.m4a"),
        new MusicTrack("1584792240", "Naruto Main Theme", "audio/1584792240.m4a")
    );

    public static final List<HighlightSound> HIGHLIGHT_SOUNDS = Arrays.asList(
        new HighlightSound("jutsu", "Завершення печатки", "audio/v7-seal-complete-focus.wav"),
        new HighlightSound("amaterasu", "Аматерасу", "audio/v5-amaterasu-focus.wav"),
        new HighlightSound("rasenshuriken", "Расенсюрикен", "audio/v5-rasenshuriken-focus.wav"),
        new HighlightSound("hiraishin", "Хірайшин", "audio/v5-hiraishin-focus.wav"),
        new HighlightSound("summoning", "Виклик жаби", "audio/v5-summoning-focus.wav"),
        new HighlightSound("sharingan", "Шарінґан", "audio/v5-sharingan-focus.wav")
    );

    public static class AppDefinition {
        public final String pkg;
        public final String name;
        public final String kind;
        public final String description;
        public final List<String> aliases;

        public AppDefinition(String pkg, String name, String kind, String description, String... aliases) {
            this.pkg = pkg;
            this.name = name;
            this.kind = kind;
            this.description = description;
            this.aliases = Arrays.asList(aliases);
        }
    }

    public static final List<AppDefinition> KNOWN_APPS = Arrays.asList(
        new AppDefinition("preview.smarttube", "SmartTube", "smarttube", "Відео без поспіху"),
        new AppDefinition("preview.lampa", "Lampa", "lampa", "Кіно на цей вечір"),
        new AppDefinition("preview.uakino", "UAKino", "uakino", "Кіно українською"),
        new AppDefinition("preview.netflix", "Netflix", "netflix", "Ще одна серія"),
        new AppDefinition("preview.kodi", "Kodi", "kodi", "Медіатека"),
        new AppDefinition("preview.spotify", "Spotify", "spotify", "Музика та подкасти"),
        new AppDefinition("preview.crunchyroll", "Crunchyroll", "crunchyroll", "Світ аніме"),
        new AppDefinition("preview.vlc", "VLC", "vlc", "Відеоплеєр"),
        new AppDefinition("preview.plex", "Plex", "plex", "Домашнє кіно"),
        new AppDefinition("preview.youtube", "YouTube", "youtube", "Відео та трансляції", "com.google.android.youtube.tv"),
        new AppDefinition("preview.tvbro", "TV Bro", "tvbro", "Браузер для телевізора", "com.phlox.tvwebbrowser", "browser"),
        new AppDefinition("preview.kyivstar", "Київстар ТБ", "kyivstar", "Телебачення та кіно", "kyivstar", "київстар", "ua.kyivstar.tv"),
        new AppDefinition("preview.megogo", "MEGOGO", "megogo", "Кіно, канали та спорт"),
        new AppDefinition("preview.sweettv", "SWEET.TV", "sweettv", "Канали та кіно українською", "sweet.tv", "sweet tv", "tv.sweet"),
        new AppDefinition("preview.primevideo", "Prime Video", "primevideo", "Фільми та серіали", "prime video", "com.amazon.amazonvideo.livingroom"),
        new AppDefinition("preview.disneyplus", "Disney+", "disneyplus", "Історії та анімація", "disney", "com.disney.disneyplus"),
        new AppDefinition("preview.jellyfin", "Jellyfin", "jellyfin", "Особиста медіатека"),
        new AppDefinition("preview.sendfiles", "Send Files to TV", "sendfiles", "Передавання файлів", "send files", "com.yablio.sendfilestotv"),
        new AppDefinition("preview.xplore", "X-plore", "xplore", "Файловий менеджер", "x-plore", "com.lonelycatgames.xplore")
    );

    public static String matchKind(String pkg, String label) {
        String query = (label + " " + pkg).toLowerCase(Locale.ROOT);
        for (int i = KNOWN_APPS.size() - 1; i >= 0; i--) {
            AppDefinition def = KNOWN_APPS.get(i);
            if (query.contains(def.kind.toLowerCase(Locale.ROOT))) return def.kind;
            for (String alias : def.aliases) {
                if (query.contains(alias.toLowerCase(Locale.ROOT))) return def.kind;
            }
        }
        return "";
    }
}
