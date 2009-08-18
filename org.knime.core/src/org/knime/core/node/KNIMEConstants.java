/*
 * -------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2009
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * -------------------------------------------------------------------
 *
 * History
 *   10.08.2005 (bernd): created
 */
package org.knime.core.node;

import java.io.File;
import java.util.Locale;

import javax.swing.ImageIcon;

import org.knime.core.internal.KNIMEPath;
import org.knime.core.util.ThreadPool;

/**
 * Class that hold static values about the knime platform. This includes,
 * among others, the welcome message and an icon.
 *
 * @author Bernd Wiswedel, University of Konstanz
 */
public final class KNIMEConstants {
    /** KNIME's major release number. */
    public static final int MAJOR = 2;
    /** KNIME's minor release number. */
    public static final int MINOR = 1;
    /** KNIME's revision number. */
    public static final int REV = 0;
    /** KNIME's build id. */
    public static final String BUILD = ".0021700";

    // IMPORTANT: Remember to also update the NodeLogger welcome screen with
    // the current version and the prerequisites
    /** Workflow file version. */
    public static final String VERSION = MAJOR + "." + MINOR + "." + REV
        + BUILD;

    /** The build date, is set automatically by the build scripts. */
    public static final String BUILD_DATE = "June 20, 2008";

    /** Java property name that is used to identify whether KNIME is started 
     * in expert mode or not (e.g. whether to show loop nodes or not).
     * This field is also used for the preference pages.
     * <p>Values of this field must be either "true" or "false". */
    public static final String PROPERTY_EXPERT_MODE = "knime.expert.mode";

    /** Java property name to specify the default max thread count variable
     * (can be set via preference page). */
    public static final String PROPERTY_MAX_THREAD_COUNT = 
        "org.knime.core.maxThreads";

    /** Java property name to specify the default temp directory for 
     * KNIME temp files (such as data files). This can be changed in the 
     * preference pages and is by default the same as the java.io.tmpdir */
    public static final String PROPERTY_TEMP_DIR = "knime.tmpdir";
    
    /**
     * The name of the system property whose value is - if set - used as knime
     * home directory. If no (or an invalid) value is set, ~user/knime will be
     * used instead. To set the knime home dir from the command line, use
     * -Dknime.home=&lt;absolutePathToNewHomeDir&gt;.
     */
    public static final String KNIME_HOME_PROPERTYNAME = "knime.home";

    /** KNIME home directory. */
    private static File knimeHomeDir;
    
    /** KNIME temp directory. */
    private static File knimeTempDir;

    /**
     * <i>Welcome to KNIME Konstanz Information Miner</i>.
     */
    public static final String WELCOME_MESSAGE;


    static {
        String line1 =
                "***  Welcome to KNIME v" + VERSION
                        + " - the Konstanz Information Miner  ***";
        String line2 =
                "Copyright, 2003 - 2009, Uni Konstanz and "
                        + "KNIME GmbH, Germany";
        line2 =
                "***"
                        + "        ".substring(0, (int)Math.floor((line1
                                .length()
                                - line2.length() - 6) / 2))
                        + line2
                        + "        ".substring(0, (int)Math.ceil((line1
                                .length()
                                - line2.length() - 6) / 2)) + "***";

        String stars =
                "***************************************************"
                        + "*******************************************";

        String s =
                stars.substring(0, line1.length()) + "\n" + line1 + "\n"
                        + line2 + "\n" + stars.substring(0, line1.length())
                        + "\n";
        WELCOME_MESSAGE = s;
    }

    /** Path to the <i>knime.png</i> icon. */
    private static final String KNIME_ICON_PATH =
            KNIMEConstants.class.getPackage().getName().replace('.', '/')
                    + "/knime.png";

    /** Icon 16 times 16 pixel. */
    public static final ImageIcon KNIME16X16;

    /** Load icon. */
    static {
        File knimeHome = KNIMEPath.getKNIMEHomeDirPath();
        knimeHomeDir = knimeHome;
        ImageIcon icon;
        try {
            ClassLoader loader = KNIMEConstants.class.getClassLoader();
            icon = new ImageIcon(loader.getResource(KNIME_ICON_PATH));
        } catch (Exception e) {
            icon = null;
        }
        KNIME16X16 = icon;
        // we prefer to have all gui-related locales being set to us-standard,
        try {
            Locale.setDefault(Locale.US);
        } catch (Exception e) {
            // do nothing.
        }

        int maxThreads = Runtime.getRuntime().availableProcessors() + 2;
        String maxThreadsString =
            System.getProperty("org.knime.core.maxThreads");
        try {
            if (maxThreadsString != null && maxThreadsString.length() > 0) {
                int val = Integer.parseInt(maxThreadsString);
                if (val <= 0) {
                    throw new NumberFormatException("Not positive");
                }
                maxThreads = val;
            }
        } catch (NumberFormatException nfe) {
            // no NodeLogger available yet!
            System.err.println("Unable to parse system property "
                    + "\"org.knime.core.maxThreads\" (\"" + maxThreadsString
                    + "\") as number: " + nfe.getMessage());
        }
        GLOBAL_THREAD_POOL = new ThreadPool(maxThreads);
        boolean flag;
        try {
            assert false;
            flag = false;
        } catch (AssertionError ae) {
            flag = true;
        }
        ASSERTIONS_ENABLED = flag;
        String tempDirPath = System.getProperty(PROPERTY_TEMP_DIR);
        if (tempDirPath != null) {
            File f = new File(tempDirPath);
            if (!(f.isDirectory() && f.canWrite())) {
                String error = "Unable to set temp path to \"" 
                        + tempDirPath + "\": no directory or not writable";
                System.err.println(error);
                throw new InternalError(error);
            } else {
                setKNIMETempDir(f);
            }
        } else {
            knimeTempDir = new File(System.getProperty("java.io.tmpdir"));
        }
    }

    /** The global thread pool from which all threads should be taken. */
    public static final ThreadPool GLOBAL_THREAD_POOL;

    /** Global flag indicating whether assertions are enabled or disabled. */
    public static final boolean ASSERTIONS_ENABLED;

    /**
     * The directory where knime will put log files and configuration files. If
     * started in eclipse, this is usually ${workspace_path}/.metadata/knime.
     * Otherwise it's in the current working directory. This variable does not
     * have a trailing file separator character.
     *
     * @return The KNIME home dir.
     */
    public static final String getKNIMEHomeDir() {
        return knimeHomeDir.getAbsolutePath();
    }
    
    /** Location for KNIME related temp files such as data container files. This
     * is by default System.getProperty("java.io.tmpdir") but can be overwritten
     * in the command line or the preference page. The 
     * @return The path to the temp directory (trailing slashes omitted). 
     */
    public static final String getKNIMETempDir() {
        return knimeTempDir.getAbsolutePath();
    }
    
    /** Set a new location for the KNIME temp directory. Client should not
     * be required to use this method. It has public scope so that bootstrap
     * classes can initialize this properly.
     * @param dir the new location to set
     * @throws NullPointerException If the argument is null
     * @throws IllegalArgumentException If the argument is not a directory
     * or not writable.
     */
    public static final void setKNIMETempDir(final File dir) {
        if (dir == null) {
            throw new NullPointerException("Directory must not be null");
        }
        if (!(dir.isDirectory() && dir.canWrite())) {
            throw new IllegalArgumentException("Can't set temp directory to \""
                    + dir.getAbsolutePath() 
                    + "\": not a directory or not writable");
        }
        System.setProperty("java.io.tmpdir", dir.getAbsolutePath());
        knimeTempDir = dir;
    }
    
    /**
     * Hides public constructor.
     */
    private KNIMEConstants() {
    }
}
