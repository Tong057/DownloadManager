package com.example.androidtlo.MainActivity;

import android.content.Context;
import android.os.AsyncTask;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.androidtlo.DownloadUtils.DownloadFileService;
import com.example.androidtlo.DownloadUtils.DownloadInfoTask;

public class DownloadViewModel extends ViewModel {
    private DownloadInfoTask mDownloadInfoTask;
    private final MutableLiveData<Long> fileSizeLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> fileTypeLiveData = new MutableLiveData<>();

    public void downloadInfo(String fileUrl) {
        if (!isDownloadTaskRunning()) {
            mDownloadInfoTask = new DownloadInfoTask(this);
            mDownloadInfoTask.execute(fileUrl);
        }
    }

    public boolean isDownloadTaskRunning() {
        return mDownloadInfoTask != null && mDownloadInfoTask.getStatus() == AsyncTask.Status.RUNNING;
    }

    public void downloadFile(Context context, String address, String fileType, long fileSize) {
        DownloadFileService.startService(context, address, fileType, fileSize);
    }

    public LiveData<Long> getFileSizeLiveData() {
        return fileSizeLiveData;
    }

    public LiveData<String> getFileTypeLiveData() {
        return fileTypeLiveData;
    }

    public void updateInfo(long fileSize, String fileType) {
        fileSizeLiveData.setValue(fileSize);
        fileTypeLiveData.setValue(fileType);
    }

}