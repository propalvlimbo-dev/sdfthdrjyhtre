package ru.rooyzee.elytrixquests.hook;

import ru.rooyzee.elytrixquests.Main;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Проверка «привязан ли игрок к Telegram» через HTTP API ElytrixAuth (прокси):
 *   GET {url}/api/linked?nickname=...   (заголовок X-Api-Key)
 *
 * Настройки — в config.yml (auth.http): url/key такие же, как у ElytrixFree
 * (api.bind/api.port + api.secret из config.properties ElytrixAuth на прокси).
 *
 * Три режима:
 *  - auth.http.enabled: false          — ElytrixAuth на сети нет: проверки нет,
 *                                        квест «Привязка аккаунта» засчитывается
 *                                        сразу (режим без ElytrixAuth);
 *  - enabled: true и key не настроен
 *    (пусто или CHANGE_ME)             — плагин ждёт настройки: квест НЕ
 *                                        засчитывается, в консоль пишется warning.
 *                                        Это защита от случайной раздачи награды
 *                                        на сервере, где ElytrixAuth есть, но
 *                                        url/key ещё не прописаны;
 *  - enabled: true и url/key заданы    — живая проверка через HTTP API.
 *
 * checkNow() различает «не привязан» и «ошибка проверки» (сеть/ключ), чтобы
 * игроку не писали «привяжи аккаунт», когда сервер просто не смог достучаться
 * до прокси. Сетевые вызовы — только не с main-потока; результат кэшируется,
 * но клик по заданию всегда делает свежую проверку (кэш «не привязан» не должен
 * перекрывать только что выполненную привязку).
 */
public final class LinkChecker {

    private static final int CONNECT_TIMEOUT_MS = 2500;
    private static final int READ_TIMEOUT_MS = 2500;

    /** Итог живой проверки. */
    public enum Result {
        /** Аккаунт привязан к Telegram. */
        LINKED,
        /** Аккаунт НЕ привязан (ответ API получен). */
        NOT_LINKED,
        /** Проверка не удалась: не настроено, недоступен прокси, неверный ключ. */
        ERROR
    }

    private final boolean enabled;      // auth.http.enabled
    private final boolean configured;   // enabled && url && key заданы
    private final String url;
    private final String key;

    private static final class Entry {
        final boolean linked;
        final long ts;

        Entry(boolean linked, long ts) {
            this.linked = linked;
            this.ts = ts;
        }
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public LinkChecker(Main plugin) {
        boolean en = plugin.getConfig().getBoolean("auth.http.enabled", true);
        String u = plugin.getConfig().getString("auth.http.url", "").trim();
        String k = plugin.getConfig().getString("auth.http.key", "").trim();
        boolean cfg = en
                && !u.isEmpty()
                && !k.isEmpty()
                && !"CHANGE_ME".equalsIgnoreCase(k);
        this.enabled = en;
        this.configured = cfg;
        this.url = u;
        this.key = k;

        if (en && !cfg) {
            plugin.getLogger().warning("Квест «Привязка аккаунта»: auth.http в config.yml не настроен.");
            plugin.getLogger().warning("Пропиши auth.http.url (http://<ip-прокси>:<api.port> из config.properties ElytrixAuth, например http://127.0.0.1:8754)");
            plugin.getLogger().warning("и auth.http.key = api.secret из config.properties ElytrixAuth (сейчас: CHANGE_ME).");
            plugin.getLogger().warning("Пока проверка не настроена — задание НЕ засчитывается. Если ElytrixAuth на сети нет, поставь auth.http.enabled: false — задание будет засчитываться сразу.");
        }
    }

    /** true — проверка активна (auth.http полностью настроен). */
    public boolean isChecking() {
        return configured;
    }

    /** true — явный режим без ElytrixAuth (auth.http.enabled: false): засчитывать сразу. */
    public boolean isStandaloneMode() {
        return !enabled;
    }

    /** Кэшированный ответ (для main-потока). null — неизвестно/устарело/не настроено. */
    public Boolean cachedLinked(String name, long maxAgeMs) {
        if (!configured) {
            return null; // проверки нет — не знаем
        }
        String lower = lower(name);
        Entry e = cache.get(lower);
        if (e == null) {
            return null;
        }
        if (System.currentTimeMillis() - e.ts > maxAgeMs) {
            return null;
        }
        return e.linked;
    }

    /**
     * Живая проверка привязки (HTTP). Вызывать ТОЛЬКО не с main-потока.
     * Всегда ходит на прокси (не доверяет кэшу), чтобы свежая привязка
     * увиделась сразу.
     */
    public Result checkNow(String name) {
        if (!enabled) {
            return Result.LINKED; // режим без ElytrixAuth
        }
        if (!configured) {
            return Result.ERROR; // ждём настройку auth.http
        }
        HttpURLConnection c = null;
        try {
            String u = url + "/api/linked?nickname=" + urlenc(name);
            c = (HttpURLConnection) new URL(u).openConnection();
            c.setConnectTimeout(CONNECT_TIMEOUT_MS);
            c.setReadTimeout(READ_TIMEOUT_MS);
            c.setRequestMethod("GET");
            if (key != null && !key.isEmpty()) {
                c.setRequestProperty("X-Api-Key", key);
            }
            int code = c.getResponseCode();
            if (code != 200) {
                return Result.ERROR; // например 401 — неверный api.secret
            }
            InputStream in = c.getInputStream();
            String body;
            try {
                body = new String(readAll(in), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
            boolean linked = body.contains("\"ok\":true") && body.contains("\"linked\":true");
            cache.put(lower(name), new Entry(linked, System.currentTimeMillis()));
            return linked ? Result.LINKED : Result.NOT_LINKED;
        } catch (Exception e) {
            return Result.ERROR; // прокси недоступен — не выдаём ни «привязан», ни награду зря
        } finally {
            if (c != null) {
                c.disconnect();
            }
        }
    }

    /** true если привязан (для периодических проверок; сетевая, не с main-потока). */
    public boolean isLinked(String name) {
        return checkNow(name) == Result.LINKED;
    }

    private static String lower(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static String urlenc(String v) throws Exception {
        return URLEncoder.encode(v == null ? "" : v, "UTF-8");
    }

    private static byte[] readAll(InputStream in) throws Exception {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) > 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
