/*
 * Copyright (C) 2015 Yohan Liyanage
 *
 * Release under the MIT License (MIT). See LICENSE file for details.
 */

package com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a request that is generated to deploy a Spark Application.
 *
 * @author Yohan Liyanage
 */
public class DeploymentRequest {

    private static final String ACTION = "CreateSubmissionRequest";
    private static final String CLIENT_SPARK_VERSION = "1.4.1";

    private String[] appArgs;
    private String appResource;
    private String mainClass;
    private Map<String, String> environmentVariables = new HashMap<String, String>();
    private Map<String, String> sparkProperties = new HashMap<String, String>();

    /**
     * Instantiates a new Deployment request.
     */
    public DeploymentRequest() {
        environmentVariables.put("SPARK_ENV_LOADED", "1");
    }

    /**
     * Get app args.
     *
     * @return string [ ]
     */
    public String[] getAppArgs() {
        return appArgs;
    }

    /**
     * Sets app args.
     *
     * @param appArgs app args
     */
    public void setAppArgs(String[] appArgs) {
        this.appArgs = appArgs;
    }

    /**
     * Returns app resource.
     *
     * @return app resource
     */
    public String getAppResource() {
        return appResource;
    }

    /**
     * Sets app resource.
     *
     * @param appResource app resource
     */
    public void setAppResource(String appResource) {
        this.appResource = appResource;
    }

    /**
     * Returns main class.
     *
     * @return main class
     */
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Sets main class.
     *
     * @param mainClass main class
     */
    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    /**
     * Returns spark properties.
     *
     * @return spark properties
     */
    public Map<String, String> getSparkProperties() {
        return sparkProperties;
    }

    /**
     * Sets spark properties.
     *
     * @param sparkProperties spark properties
     */
    public void setSparkProperties(Map<String, String> sparkProperties) {
        this.sparkProperties = sparkProperties;
    }

    /**
     * Returns action.
     *
     * @return action
     */
    public String getAction() {
        return ACTION;
    }

    /**
     * Returns client spark version.
     *
     * @return client spark version
     */
    public String getClientSparkVersion() {
        return CLIENT_SPARK_VERSION;
    }

    /**
     * Returns environment variables.
     *
     * @return environment variables
     */
    public Map<String, String> getEnvironmentVariables() {
        return environmentVariables;
    }
}
