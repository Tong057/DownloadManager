package com.example.androidtlo.DownloadUtils;

import android.os.AsyncTask;

import com.example.androidtlo.MainActivity.DownloadViewModel;

import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class DownloadInfoTask extends AsyncTask<String, Integer, Boolean> {
    private long mSize;
    private String mType;
    private final DownloadViewModel mViewModel;

    public DownloadInfoTask(DownloadViewModel viewModel) {
        this.mViewModel = viewModel;
    }

    @Override
        protected Boolean doInBackground(String... params) {
        if (params.length != 1) {
            return false;
        }

        String fileUrl = params[0];
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(fileUrl);
            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            mSize = connection.getContentLength();
            mType = connection.getContentType();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (connection != null)
                connection.disconnect();
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if (aBoolean) {
            mViewModel.updateInfo(mSize, mType);
        }
    }
}
