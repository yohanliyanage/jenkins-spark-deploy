/*
 * Copyright (C) 2015 Yohan Liyanage
 *
 * Release under the MIT License (MIT). See LICENSE file for details.
 */

package com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer;

/**
 * Utility Methods.
 *
 * @author Yohan Liyanage
 */
public final class Utils {

    /**
     * Returns Actual Spark Master URL. For example, if a spark:// based URL is given, spark:// will be replaced with HTTP.
     * @param masterUrl Master URL that could be spark specific
     * @return actual master URL with HTTP / HTTPS scheme
     */
    public static String getActualSparkMasterUrl(String masterUrl) {
        if (masterUrl.startsWith("spark")) {
            return masterUrl.replaceFirst("spark", "http");
        }
        return masterUrl;
    }

    private Utils() {
        // No instantiation
    }
}
