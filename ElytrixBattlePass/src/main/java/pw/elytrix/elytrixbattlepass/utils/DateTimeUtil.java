package pw.elytrix.elytrixbattlepass.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class DateTimeUtil {
   private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMMM, HH:mm", new Locale("ru"));

   public static String getFormattedDate(long timestamp) {
      Instant instant = Instant.ofEpochMilli(timestamp);
      ZonedDateTime zonedDateTime = instant.atZone(ZoneId.of("Europe/Moscow"));
      return zonedDateTime.format(formatter);
   }

   public static String getFormattedTime(long totalSeconds) {
      long minutes = totalSeconds / 60L;
      long seconds = totalSeconds % 60L;
      StringBuilder formattedTime = new StringBuilder();
      if (minutes > 0L) {
         if (!formattedTime.isEmpty()) {
            formattedTime.append(" ");
         }

         formattedTime.append(minutes).append(" мин.");
      }

      if (formattedTime.isEmpty() || seconds > 0L) {
         if (!formattedTime.isEmpty()) {
            formattedTime.append(" ");
         }

         formattedTime.append(seconds).append(" сек.");
      }

      return formattedTime.toString();
   }
}
