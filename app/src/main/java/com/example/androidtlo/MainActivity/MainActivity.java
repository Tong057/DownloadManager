package com.example.androidtlo.MainActivity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.Manifest;

import com.example.androidtlo.DownloadUtils.DownloadFileService;
import com.example.androidtlo.DownloadUtils.ProgressInfo;
import com.example.androidtlo.R;
import com.google.android.material.progressindicator.LinearProgressIndicator;

public class MainActivity extends AppCompatActivity {
    final int PERMISSION_REQUEST_CODE = 112;
    Button downloadInfoButton, downloadFileButton;
    EditText addressEditText;
    TextView fileSizeTextView, fileTypeTextView, downloadedBytesTextView, downloadingStatusTextView;
    LinearProgressIndicator downloadingProgressInd;
    DownloadViewModel mDownloadViewModel;
    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // getting message
            Bundle bundle = intent.getExtras();
            ProgressInfo progressInfo = bundle.getParcelable(DownloadFileService.PROGRESS_INFO);

            downloadedBytesTextView.setText(String.valueOf(progressInfo.mDownloadedBytes));
            downloadingStatusTextView.setText(progressInfo.mStatus);
            downloadingProgressInd.setProgress(progressInfo.mProgressPercentage);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mBroadcastReceiver, new IntentFilter(DownloadFileService.NOTIFICATION));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        String downloadingStatus = downloadingStatusTextView.getText().toString();
        outState.putString("downloading_status", downloadingStatus);

        String downloadedBytes = downloadedBytesTextView.getText().toString();
        outState.putString("downloaded_bytes", downloadedBytes);

        int downloadingProgress = downloadingProgressInd.getProgress();
        outState.putInt("downloading_progress", downloadingProgress);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String downloadingStatus = savedInstanceState.getString("downloading_status");
        if (downloadingStatus != null) {
            downloadingStatusTextView.setText(downloadingStatus);
        }

        String downloadedBytes = savedInstanceState.getString("downloaded_bytes");
        if (downloadedBytes != null) {
            downloadedBytesTextView.setText(downloadedBytes);
        }

        int downloadingProgress = savedInstanceState.getInt("downloading_progress", 0);
        downloadingProgressInd.setProgress(downloadingProgress);
    }

    private void restoreIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            String address = intent.getStringExtra("address");
            if (address != null) {
                addressEditText.setText(address);
            }

            String fileType = intent.getStringExtra("file_type");
            long fileSize = intent.getLongExtra("file_size", 0);
            if (fileType != null && fileSize != 0) {
                mDownloadViewModel.updateInfo(fileSize, fileType);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeUI();
        initializeViewModel();
        checkNotificationPermission();
        restoreIntentData();
    }

    private void initializeUI() {
        initializeDownloadInfoButton();
        initializeDownloadFileButton();
        initializeAddressEditText();
        initializeTextViews();
        initializeProgressIndicator();
    }

    private void initializeViewModel() {
        mDownloadViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        mDownloadViewModel.getFileSizeLiveData().observe(this, fileSize -> fileSizeTextView.setText(String.valueOf(fileSize)));
        mDownloadViewModel.getFileTypeLiveData().observe(this, fileType -> fileTypeTextView.setText(fileType));
    }

    private void initializeDownloadFileButton() {
        downloadFileButton = findViewById(R.id.btn_download_file);
        downloadFileButton.setOnClickListener(view -> {
            checkNotificationPermission();
            String address = addressEditText.getText().toString();
            String fileType = fileTypeTextView.getText().toString();
            long fileSize = Long.parseLong(fileSizeTextView.getText().toString());
            mDownloadViewModel.downloadFile(this, address, fileType, fileSize);
        });
    }

    private void initializeDownloadInfoButton() {
        downloadInfoButton = findViewById(R.id.btn_download_info);
        downloadInfoButton.setOnClickListener(view -> mDownloadViewModel.downloadInfo(addressEditText.getText().toString()));
    }

    private void initializeProgressIndicator() {
        downloadingProgressInd = findViewById(R.id.lpi_downloading_progress);
    }

    private void initializeTextViews() {
        fileSizeTextView = findViewById(R.id.tv_file_size);
        fileTypeTextView = findViewById(R.id.tv_file_type);
        downloadedBytesTextView = findViewById(R.id.tv_downloading_progress);
        downloadingStatusTextView = findViewById(R.id.tv_downloading_status);
    }
    private void initializeAddressEditText() {
        addressEditText = findViewById(R.id.et_address);
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT > 32) {
            if (!shouldShowRequestPermissionRationale("112")){
                getNotificationPermission();
            }
        }
    }

    public void getNotificationPermission(){
        try {
            if (Build.VERSION.SDK_INT > 32) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}