package co.com.taxisverdes.taxisverdes.utils;

import android.util.Log;
import android.widget.Toast;

import co.com.taxisverdes.taxisverdes.R;

import static co.com.taxisverdes.taxisverdes.Application.getAppContext;

public class NotificationUtils {

    public static void showNotification(int messageId) {
        Toast.makeText(getAppContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    public static void showGeneralError() {
        Toast.makeText(getAppContext(), R.string.genericError, Toast.LENGTH_SHORT).show();
    }

    public static void showGeneralError(Exception e) {
        Log.e(getAppContext().getClass().getName(), getAppContext().getString(R.string.genericError), e);
        Toast.makeText(getAppContext(), R.string.genericError, Toast.LENGTH_SHORT).show();
    }
}
