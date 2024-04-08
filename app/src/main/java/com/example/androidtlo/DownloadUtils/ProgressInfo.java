package com.example.androidtlo.DownloadUtils;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class ProgressInfo implements Parcelable {
    public int mProgressPercentage;
    public long mDownloadedBytes;
    public long mTotalSize;
    public String mStatus;
    protected ProgressInfo(Parcel in) {
        mProgressPercentage = in.readInt();
        mDownloadedBytes = in.readInt();
        mTotalSize = in.readInt();
        mStatus = in.readString();
    }

    public ProgressInfo(long downloadedBytes, long totalSize, String status) {
        mDownloadedBytes = downloadedBytes;
        mTotalSize = totalSize;
        mStatus = status;
        updateProgressPercentage();
    }

    public void updateProgressPercentage() {
        if (mTotalSize == 0) {
            mProgressPercentage = 0;
        } else {
            mProgressPercentage = (int) (mDownloadedBytes * 100 / mTotalSize);
        }
    }

    public static final Creator<ProgressInfo> CREATOR = new Creator<ProgressInfo>() {
        @Override
        public ProgressInfo createFromParcel(Parcel in) {
            return new ProgressInfo(in);
        }

        @Override
        public ProgressInfo[] newArray(int size) {
            return new ProgressInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel parcel, int i) {
        parcel.writeLong(mProgressPercentage);
        parcel.writeLong(mDownloadedBytes);
        parcel.writeLong(mTotalSize);
        parcel.writeString(mStatus);
    }
}
