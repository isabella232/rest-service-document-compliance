package gov.nsf.psm.documentcompliance.compliance.common.utility;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DateUtils {

    private DateUtils() {
    }

    public static Date convertToDate(String input) {

        List<SimpleDateFormat> dateFormats = new ArrayList<SimpleDateFormat>();
        dateFormats.add(new SimpleDateFormat("M/dd/yyyy"));
        dateFormats.add(new SimpleDateFormat("dd.M.yyyy"));
        dateFormats.add(new SimpleDateFormat("M/dd/yyyy hh:mm:ss a"));
        dateFormats.add(new SimpleDateFormat("dd.M.yyyy hh:mm:ss a"));
        dateFormats.add(new SimpleDateFormat("dd.MMM.yyyy"));
        dateFormats.add(new SimpleDateFormat("dd-MMM-yyyy"));

        Date date = null;
        if (null == input) {
            return null;
        }
        for (SimpleDateFormat format : dateFormats) {
            try {
                format.setLenient(false);
                date = format.parse(input);
            } catch (ParseException e) {
                //
            }
            if (date != null) {
                break;
            }
        }

        return date;

    }

    public static String formatDate(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

}
