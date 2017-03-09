package com.vnetpublishing.java.suapp;

import java.lang.management.*;
import java.util.logging.*;

import com.vnetpublishing.java.suapp.linux.*;
import com.vnetpublishing.java.suapp.mac.*;
import com.vnetpublishing.java.suapp.posix.*;
import com.vnetpublishing.java.suapp.win.*;

/**
 * Usage:
 * 
 * <pre>
 * <code>
 * public class ExampleApp implements SuperUserApplication {
 *
 *     public static void main(String[] args) {
 *         try {
 *             SU.run(new ExampleApp(), args);
 *         } catch (SudoNotSuccessfulException ex) {
 *             System.out.println("Couldn't get Sudo :(");
 *             ex.printStackTrace();
 *         }
 *     }
 *     
 *     {@literal @}Override
 *     public int run(String[] args) {
 *         System.out.println("We have sudo here! YAAY! :)");
 *         return 0;
 *     }
 *
 * }
 * </code>
 * </pre>
 */
public class SU {

    private final static Logger logger = Logger.getLogger(SU.class.getName());

    private static final int SUDO_NOT_SUCCESSFUL = -1111;
    private static final String TRY_WITH_SUDO = "##tryWithSudo##";

    public static boolean daemon;
    public static boolean prefer_stdio;
    public static boolean debug;

    /**
     * Runs a SuperUserApplication as a SuperUser.
     * 
     * @param app to run
     * @param args of the main method, can be null to automatically get them via MXBeans
     * @return error code
     * @throws SudoNotSuccessfulException if there was an attempt to get sudo but the
     *         system denied that request (e.g. disabled by system admin or admin login
     *         unsuccessfull)
     */
    public static int run(SuperUserApplication app, String[] args)
            throws SudoNotSuccessfulException {
        int result = -1;

        if (isSuperUser()) {
            result = app.run(args);
        } else if (isTryAdminRun(args)) {
            // still isnt super user => abort, dont go into endless loop
            System.err.println("Application still isn't a super user, "
                    + "exiting with errorcode " + SUDO_NOT_SUCCESSFUL);
            System.exit(SUDO_NOT_SUCCESSFUL);
        } else {
            result = sudo(args, new String[] { TRY_WITH_SUDO });
        }

        if (result == SUDO_NOT_SUCCESSFUL) {
            throw new SudoNotSuccessfulException();
        }

        if (!daemon) {
            System.err.println(
                    "Application isn't a daemon, " + "exiting with errorcode " + result);
            System.exit(result);
        }

        return result;
    }

    private static boolean isTryAdminRun(String[] args) {
        String[] inputArgs = null;
        try {
            inputArgs = ManagementFactory.getRuntimeMXBean().getInputArguments()
                    .toArray(new String[0]);
        } catch (Exception e) {
            inputArgs = args;
        }

        if (inputArgs != null) {
            for (String string : args) {
                if (string.equals(TRY_WITH_SUDO)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static final int sudo() {
        return sudo(null);
    }

    public static final int sudo(String[] args) {
        return sudo(args, null);
    }

    public static final int sudo(String[] args, String[] additionArgs) {
        final OS os = OS.get();
        switch (os) {
        case WINDOWS:
            return new WinSudo().sudo(args, additionArgs);
        case LINUX:
            return new LinuxSudo().sudo(args, additionArgs);
        case MAC:
            return new MacSudo().sudo(args, additionArgs);
        case POSIX:
            return new PosixSudo().sudo(args, additionArgs);
        default:
            logger.warning(String
                    .format("Unsupported platform '%s, falling back to posix'", os));
            return new PosixSudo().sudo(args, additionArgs);
        }
    }

    public static final boolean isSuperUser() {
        final OS os = OS.get();
        switch (os) {
        case WINDOWS:
            return new WinSuperUserDetector().isSuperUser();
        case LINUX:
            return new LinuxSuperUserDetector().isSuperUser();
        case MAC:
            return new MacSuperUserDetector().isSuperUser();
        case POSIX:
            return new PosixSuperUserDetector().isSuperUser();
        default:
            throw new IllegalStateException(
                    String.format("Unsupported operating system: %s", os));

        }
    }

}
