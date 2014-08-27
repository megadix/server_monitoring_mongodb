package monitoring;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class DateFormatUtils {
    public static final DateFormat dayFormat = new SimpleDateFormat("yyyyMMdd");
    public static final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
}
