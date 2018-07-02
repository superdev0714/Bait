package com.detect.bait;

/**
 * Created by hello on 7/2/18.
 */

public class Constant {

    public static String AppName = "JakeTV";

    public static boolean isTracking = false;
    public static boolean isBaitMode = false;

    public static boolean isPowerOff = false;

    public static class SHARED_PR {
        public static final String SHARE_PREF = AppName + "_preferences";

        public static final String KEY_ActivityName = "activityName";
    }
}
