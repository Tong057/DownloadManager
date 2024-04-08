package com.example.androidtlo.DownloadUtils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.androidtlo.MainActivity.MainActivity;
import com.example.androidtlo.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DownloadFileService extends IntentService {
    public static final String NOTIFICATION = "com.example.androidtlo.intent_service.download_progress";
    private static final String TASK = "com.example.androidtlo.intent_service.action.download_file";
    public static final String PROGRESS_INFO = "progress_info";
    private static final String ID_NOTIFICATION_CHANNEL = "channel_id_01";
    private static final int ID_NOTIFICATION = 1;
    private NotificationManager mNotificationManager;
    private boolean isDownloading;
    private ProgressInfo mProgressInfo;

    public DownloadFileService() {
        super("DownloadFileService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mProgressInfo = new ProgressInfo(0, 0, getString(R.string.waiting));
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        prepareNotificationChannel();
        if (intent != null) {
            final String address = intent.getStringExtra("address");
            final String fileType = intent.getStringExtra("file_type");
            final long fileSize = intent.getLongExtra("file_size", 0);

            final String action = intent.getAction();
            startForeground(ID_NOTIFICATION, createNotification(address, fileType, fileSize));
            tryCompleteTask(action, address, fileType, fileSize);
        }
        Log.e("downloading_service", "service did task");
    }

    private void tryCompleteTask(String action, String address, String fileType, long fileSize) {
        if (TASK.equals(action)) {
            isDownloading = true;
            completeTask(address, fileType, fileSize);
            isDownloading = false;
        } else {
            Log.e("downloading_service", "unknown action");
        }
    }

    public static void startService(Context context, String address, String fileType, long fileSize) {
        Intent intent = new Intent(context, DownloadFileService.class);
        intent.setAction(TASK);
        intent.putExtra("address", address);
        intent.putExtra("file_type", fileType);
        intent.putExtra("file_size", fileSize);
        context.startService(intent);
    }

    private void completeTask(String address, String fileType, long fileSize) {
        try {
            URL url = new URL(address);
            HttpURLConnection urlConnection = createHttpGetConnection(url);
            String fileName = new File(url.getPath()).getName();
            String mimeType = urlConnection.getContentType();
            mProgressInfo.mTotalSize = urlConnection.getContentLength();

            ContentValues values = new ContentValues();
            putContentValues(values, fileName, mimeType);
            Uri uri = generateDownloadUri(values);

            downloadFile(address, fileType, fileSize, urlConnection, uri);
        } catch (Exception e) {
            e.printStackTrace();
            mProgressInfo.mStatus = getString(R.string.error);
        }
        sendProgressBroadcast(mProgressInfo);
    }

    private void downloadFile(String address, String fileType, long fileSize, HttpURLConnection urlConnection, Uri uri) {
        try (InputStream inputStream = urlConnection.getInputStream();
             OutputStream outputStream = getContentResolver().openOutputStream(uri)) {
            byte[] buffer = new byte[4096];
            int bytesRead;

            mProgressInfo.mStatus = getString(R.string.downloading);
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                updateDownloadedBytes(bytesRead);

                Log.e("downloading_service", "Downloaded: " + mProgressInfo.mDownloadedBytes);
                sendProgressBroadcast(mProgressInfo);
                mNotificationManager.notify(ID_NOTIFICATION, createNotification(address, fileType, fileSize));
            }
            mProgressInfo.mStatus = getString(R.string.finished);
        } catch (IOException e) {
            e.printStackTrace();
            mProgressInfo.mStatus = getString(R.string.error);
        } finally {
            urlConnection.disconnect();
        }
    }

    private void updateDownloadedBytes(int bytesRead) {
        mProgressInfo.mDownloadedBytes += bytesRead;
        mProgressInfo.updateProgressPercentage();
    }

    private HttpURLConnection createHttpGetConnection(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        return urlConnection;
    }

    private Uri generateDownloadUri(ContentValues values) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }
        return null;
    }

    private void putContentValues(ContentValues values, String fileName, String mimeType) {
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
    }

    private void prepareNotificationChannel() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel channel = new NotificationChannel(ID_NOTIFICATION_CHANNEL, name, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(String address, String fileType, long fileSize) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        putExtraFileData(notificationIntent, address, fileType, fileSize);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(notificationIntent);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this, ID_NOTIFICATION_CHANNEL);
        builder.setContentTitle(getString(R.string.notification_title))
                .setProgress(100, mProgressInfo.mProgressPercentage, false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH);

        builder.setOngoing(!isDownloading);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(ID_NOTIFICATION_CHANNEL);
        }
        mNotificationManager.notify(ID_NOTIFICATION, builder.build());

        return builder.build();
    }

    private void putExtraFileData(Intent intent, String address, String fileType, long fileSize) {
        intent.putExtra("address", address);
        intent.putExtra("file_type", fileType);
        intent.putExtra("file_size", fileSize);
    }

    private void sendProgressBroadcast(ProgressInfo progressInfo) {
        Intent intent = new Intent(NOTIFICATION);
        intent.putExtra(PROGRESS_INFO, progressInfo);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
