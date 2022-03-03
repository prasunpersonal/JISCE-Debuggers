package com.prasunpersonal.jiscedebuggers;

import android.app.Activity;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.view.inputmethod.InputMethodManager;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import com.prasunpersonal.jiscedebuggers.Models.User;


public class App extends Application {
    public static final String NOTIFICATION_CHANNEL_ID = "ProgressNotification";
    public static final String GOOGLE = "GOOGLE";
    public static final String FACEBOOK = "FACEBOOK";
    public static final String GITHUB = "GITHUB";

    public static int AUTHENTICATION_TYPE;
    public static User ME;

    public static final HashMap<String, String> EXTENSION_TO_LANGUAGE_MAP = new HashMap<>();
    public static final HashMap<String, String> LANGUAGE_TO_EXTENSION_MAP = new HashMap<>();
    public static final ArrayList<String> PROGRAMING_LANGUAGES = new ArrayList<>();
    public static final ArrayList<Integer> RATING_COLOR = new ArrayList<>();
    public static final ArrayList<Integer> RATING_STARS = new ArrayList<>();

    static {
        RATING_STARS.add(R.drawable.ic_star);
        RATING_STARS.add(R.drawable.ic_star_1);
        RATING_STARS.add(R.drawable.ic_star_2);
        RATING_STARS.add(R.drawable.ic_star_3);
        RATING_STARS.add(R.drawable.ic_star_4);
        RATING_STARS.add(R.drawable.ic_star_5);
    }

    static {
        RATING_COLOR.add(R.color.gray_lite);
        RATING_COLOR.add(R.color.red);
        RATING_COLOR.add(R.color.orange);
        RATING_COLOR.add(R.color.yellow);
        RATING_COLOR.add(R.color.green_yellow);
        RATING_COLOR.add(R.color.green);
    }

    static {
        EXTENSION_TO_LANGUAGE_MAP.put(".c", "C");
        EXTENSION_TO_LANGUAGE_MAP.put(".css", "CSS");
        EXTENSION_TO_LANGUAGE_MAP.put(".cpp", "C++");
        EXTENSION_TO_LANGUAGE_MAP.put(".cxx", "C++");
        EXTENSION_TO_LANGUAGE_MAP.put(".c++", "C++");
        EXTENSION_TO_LANGUAGE_MAP.put(".cs", "C#");
        EXTENSION_TO_LANGUAGE_MAP.put(".dart", "DART");
        EXTENSION_TO_LANGUAGE_MAP.put(".go", "GO");
        EXTENSION_TO_LANGUAGE_MAP.put(".html", "HTML");
        EXTENSION_TO_LANGUAGE_MAP.put(".htm", "HTML");
        EXTENSION_TO_LANGUAGE_MAP.put(".java", "JAVA");
        EXTENSION_TO_LANGUAGE_MAP.put(".js", "JAVASCRIPT");
        EXTENSION_TO_LANGUAGE_MAP.put(".json", "JSON");
        EXTENSION_TO_LANGUAGE_MAP.put(".mm", "OBJECTIVE-C");
        EXTENSION_TO_LANGUAGE_MAP.put(".pl", "PERL");
        EXTENSION_TO_LANGUAGE_MAP.put(".php", "PHP");
        EXTENSION_TO_LANGUAGE_MAP.put(".py", "PYTHON");
        EXTENSION_TO_LANGUAGE_MAP.put(".python", "PYTHON");
        EXTENSION_TO_LANGUAGE_MAP.put(".r", "R");
        EXTENSION_TO_LANGUAGE_MAP.put(".txt", "SIMPLE TEXT");
        EXTENSION_TO_LANGUAGE_MAP.put(".swift", "SWIFT");
        EXTENSION_TO_LANGUAGE_MAP.put(".xml", "XML");
    }

    static {
        LANGUAGE_TO_EXTENSION_MAP.put("C", ".c");
        LANGUAGE_TO_EXTENSION_MAP.put("CSS", ".css");
        LANGUAGE_TO_EXTENSION_MAP.put("C++", ".cpp");
        LANGUAGE_TO_EXTENSION_MAP.put("C#", ".cs");
        LANGUAGE_TO_EXTENSION_MAP.put("DART", ".dart");
        LANGUAGE_TO_EXTENSION_MAP.put("GO", ".go");
        LANGUAGE_TO_EXTENSION_MAP.put("HTML", ".html");
        LANGUAGE_TO_EXTENSION_MAP.put("JAVA", ".java");
        LANGUAGE_TO_EXTENSION_MAP.put("JAVASCRIPT", ".js");
        LANGUAGE_TO_EXTENSION_MAP.put("JSON", ".json");
        LANGUAGE_TO_EXTENSION_MAP.put("OBJECTIVE-C", ".mm");
        LANGUAGE_TO_EXTENSION_MAP.put("PERL", ".pl");
        LANGUAGE_TO_EXTENSION_MAP.put("PHP", ".php");
        LANGUAGE_TO_EXTENSION_MAP.put("PYTHON", ".py");
        LANGUAGE_TO_EXTENSION_MAP.put("R", ".r");
        LANGUAGE_TO_EXTENSION_MAP.put("SIMPLE TEXT", ".txt");
        LANGUAGE_TO_EXTENSION_MAP.put("SWIFT", ".swift");
        LANGUAGE_TO_EXTENSION_MAP.put("XML", ".xml");
    }

    static {
        PROGRAMING_LANGUAGES.add("C");
        PROGRAMING_LANGUAGES.add("CSS");
        PROGRAMING_LANGUAGES.add("C++");
        PROGRAMING_LANGUAGES.add("C#");
        PROGRAMING_LANGUAGES.add("DART");
        PROGRAMING_LANGUAGES.add("GO");
        PROGRAMING_LANGUAGES.add("HTML");
        PROGRAMING_LANGUAGES.add("JAVA");
        PROGRAMING_LANGUAGES.add("JAVASCRIPT");
        PROGRAMING_LANGUAGES.add("JSON");
        PROGRAMING_LANGUAGES.add("OBJECTIVE-C");
        PROGRAMING_LANGUAGES.add("PERL");
        PROGRAMING_LANGUAGES.add("PHP");
        PROGRAMING_LANGUAGES.add("PYTHON");
        PROGRAMING_LANGUAGES.add("R");
        PROGRAMING_LANGUAGES.add("SIMPLE TEXT");
        PROGRAMING_LANGUAGES.add("SWIFT");
        PROGRAMING_LANGUAGES.add("XML");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        getSystemService(NotificationManager.class).createNotificationChannel(notificationChannel);
    }

    public static String timeStamp(long ms) {
        if (TimeUnit.MILLISECONDS.toHours(ms) > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(ms), TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ms)), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)));
        } else {
            return String.format(Locale.getDefault(), "%d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(ms)), TimeUnit.MILLISECONDS.toSeconds(ms) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(ms)));
        }
    }

    public static void closeKeyboard(Activity activity) {
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getCurrentFocus().getWindowToken(), 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getFileSize(long size) {
        if (size <= 0)
            return "0";

        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}
