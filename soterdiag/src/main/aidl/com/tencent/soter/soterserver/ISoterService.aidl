package com.tencent.soter.soterserver;

import com.tencent.soter.soterserver.SoterExportResult;
import com.tencent.soter.soterserver.SoterSessionResult;
import com.tencent.soter.soterserver.SoterSignResult;
import com.tencent.soter.soterserver.SoterDeviceResult;
import com.tencent.soter.soterserver.SoterExtraParam;

interface ISoterService {
    int generateAppSecureKey(int uid);
    SoterExportResult getAppSecureKey(int uid);
    boolean hasAskAlready(int uid);
    int generateAuthKey(int uid, String kname);
    int removeAuthKey(int uid, String kname);
    SoterExportResult getAuthKey(int uid, String kname);
    int removeAllAuthKey(int uid);
    boolean hasAuthKey(int uid, String kname);
    SoterSessionResult initSigh(int uid, String kname, String challenge);
    SoterSignResult finishSign(long signSession);
    SoterDeviceResult getDeviceId();
    int getVersion();
    SoterExtraParam getExtraParam(String key);
}
