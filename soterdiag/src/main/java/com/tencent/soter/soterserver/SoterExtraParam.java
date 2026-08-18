package com.tencent.soter.soterserver;

import android.os.Parcel;
import android.os.Parcelable;

public class SoterExtraParam implements Parcelable {
    public Object result;

    public SoterExtraParam() {}

    protected SoterExtraParam(Parcel in) {
        result = in.readValue(getClass().getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeValue(result);
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<SoterExtraParam> CREATOR = new Creator<SoterExtraParam>() {
        @Override
        public SoterExtraParam createFromParcel(Parcel in) { return new SoterExtraParam(in); }

        @Override
        public SoterExtraParam[] newArray(int size) { return new SoterExtraParam[size]; }
    };
}
