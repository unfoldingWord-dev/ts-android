package com.door43.util.root;

import java.io.File;

/**
 * This class uses three different methods to determine if the device is rooted
 * @author Kevin Kowalewski
 * http://stackoverflow.com/questions/1101380/determine-if-running-on-a-rooted-device
 */
public class Root {

    private static String LOG_TAG = Root.class.getName();

    /**
     * Checks if the device is rooted
     * @return
     */
    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        try {
            File file = new File("/system/app/Superuser.apk");
            return file.exists();
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean checkRootMethod3() {
        return new ExecShell().executeCommand(ExecShell.SHELL_CMD.check_su_binary)!=null;
    }
}

