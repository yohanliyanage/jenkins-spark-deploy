/*
 * Copyright (C) 2015 Yohan Liyanage
 *
 * Release under the MIT License (MIT). See LICENSE file for details.
 */

package com.yohanliyanage.jenkins.plugins.sparkdeploy;

import com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer.DeploymentManager;
import com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer.DeploymentRequest;
import com.yohanliyanage.jenkins.plugins.sparkdeploy.deployer.Utils;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Spark Deployer is a Jenkins {@code Recorder} that does the following.
 * <ol>
 *    <li>Kills any previously deployed running Spark applications (configurable)</li>
 *    <li>Submits a new Spark application</li>
 *    <li>Ensures that the Spark application is running.</li>
 * </ol>
 *
 * @author Yohan Liyanage
 */
public class SparkDeployPublisher extends Recorder {

    /**
     * Constant SPARK_DEPLOY_SUBMISSION_FILE.
     */
    public static final String SPARK_DEPLOY_SUBMISSION_FILE = ".spark-deploy-submission";

    private String masterUrl;
    private String masterRestUrl;
    private String scalaVersion;
    private String appResource;
    private String mainClass;
    private String appArgs;
    private String sparkProperties;
    private boolean killBeforeSubmit;
    private boolean failBuildOnFailure;
    private boolean verbose;

    /**
     * Constructor for Spark Deploy Publisher.
     *
     * @param masterUrl Spark Master URL (ex. spark://localhost:6066)
     * @param masterRestUrl Custom Spark Master URL (HTTP / HTTPS REST URL) - Optional
     * @param scalaVersion Spark Scala Version
     * @param appResource Application Resource - JAR File URL (HTTP / HDFS URL or file path if available to executors).
     * @param mainClass Fully qualified main class name.
     * @param appArgs Space separated application command line arguments
     * @param sparkProperties Comma separated series name value pairs (ex. prop1=value1,prop2=value2,...)
     * @param killBeforeSubmit Kill previous submission before submitting new one
     * @param failBuildOnFailure Fail the build if deployment fails
     * @param verbose enables verbose mode which logs request / response of REST calls
     */
    @DataBoundConstructor
    public SparkDeployPublisher(String masterUrl, String masterRestUrl, String scalaVersion, String appResource,
                                String mainClass, String appArgs, String sparkProperties, boolean killBeforeSubmit,
                                boolean failBuildOnFailure, boolean verbose) {
        this.masterUrl = masterUrl.trim().toLowerCase();
        this.masterRestUrl = masterRestUrl.trim().isEmpty() ?
                Utils.getActualSparkMasterUrl(masterUrl) : masterRestUrl.trim().toLowerCase();
        this.scalaVersion = scalaVersion.trim().isEmpty() ? null : scalaVersion.trim();
        this.appResource = appResource.trim();
        this.mainClass = mainClass.trim();
        this.appArgs = appArgs.trim();
        this.sparkProperties = sparkProperties.trim();
        this.killBeforeSubmit = killBeforeSubmit;
        this.failBuildOnFailure = failBuildOnFailure;
        this.verbose = verbose;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        PrintStream logger = listener.getLogger();

        logger.println("[Spark-Deployer] Starting Spark Deployment on cluster: " + masterUrl);

        DeploymentManager deployer = new DeploymentManager(masterRestUrl, logger, verbose);

        if (killBeforeSubmit) {
            killPreviousSubmission(build, logger, deployer);
        }

        // Submit
        DeploymentRequest request = buildRequest(logger);
        String submissionId = deployer.submit(request);

        // Check State
        String driverState = getDriverState(deployer, submissionId);

        logger.println("[Spark-Deployer] Submitted Spark application under Submission ID " + submissionId + " - Driver State : " + driverState);

        // Handle Result
        if (! ("ERROR".equals(driverState) || "ATTEMPTS_EXCEEDED_NO_RESPONSE".endsWith(driverState))) {
            saveSubmissionId(submissionId, build);
        } else {
            // Failed
            if (failBuildOnFailure) {
                logger.println("[Spark-Deployer] Failing build since Spark Deployment was not successful. Enable verbose mode for more information.");
                build.setResult(Result.FAILURE);
                return false;
            }
        }

        return true;
    }

    private String getDriverState(DeploymentManager deployer, String submissionId) throws IOException, InterruptedException {
        String driverState = deployer.getDriverState(submissionId);
        int attempts = 0;

        while ("LOADING".equals(driverState)) {
            attempts++;
            driverState = deployer.getDriverState(submissionId);
            Thread.sleep(2000 * attempts);

            if (attempts > 4) {
                driverState = "ATTEMPTS_EXCEEDED_NO_RESPONSE";
            }
        }
        return driverState;
    }

    private void killPreviousSubmission(AbstractBuild<?, ?> build, PrintStream logger, DeploymentManager deployer) throws IOException {
        String previousSubmission = getPreviousSubmissionId(build);
        if (previousSubmission == null) {
            logger.println("[Spark-Deployer] Kill before submit is enabled, but no previous submission data found. Skipping kill step");
        } else {
            deployer.kill(previousSubmission);
        }
    }

    /**
     * Builds a Deployment Request using the configuration of the plugin.
     * @param logger logger
     * @return request
     */
    private DeploymentRequest buildRequest(PrintStream logger) {
        DeploymentRequest request = new DeploymentRequest();
        request.setAppArgs(appArgs != null ? appArgs.trim().split("\\s+") : new String[] {});
        request.setAppResource(appResource);
        request.setMainClass(mainClass);

        // Default Properties
        request.getSparkProperties().put("spark.jars", request.getAppResource());
        request.getSparkProperties().put("spark.driver.supervise", "false");
        request.getSparkProperties().put("spark.app.name", request.getMainClass());
        request.getSparkProperties().put("spark.master", masterUrl);

        // Environment Variables
        request.getEnvironmentVariables().put("SPARK_SCALA_VERSION", scalaVersion);
        request.getEnvironmentVariables().put("SPARK_ENV_LOADED", "1");

        // Add spark properties
        String[] props = sparkProperties.split(",");
        for (String prop : props) {
            String[] pair = prop.split("=");
            if (request.getSparkProperties().containsKey(pair[0])) {
                // Log about overriding properties
                logger.println("[Spark-Deployer] Overriding Spark Property '" + pair[0] + "' : Previous Value = " +
                        request.getSparkProperties().get(pair[0]) + ", New Value = " + pair[1] + System.lineSeparator());
            }
            request.getSparkProperties().put(pair[0].trim(), pair[1].trim());
        }
        return request;
    }

    /**
     * Returns the last submission ID if exists, or null.
     * @param build build reference
     * @return last submission ID or null
     * @throws IOException
     */
    private String getPreviousSubmissionId(AbstractBuild<?, ?> build) throws IOException {

        String submissionId = null;
        File root = build.getProject().getRootDir();
        File file = new File(root, SPARK_DEPLOY_SUBMISSION_FILE);

        if (file.exists()) {
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                String text = reader.readLine();
                if (text != null && ! text.trim().isEmpty()) {
                    submissionId =  text;
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        return submissionId;
    }

    /**
     * Saves the given submission ID so that it can be fetched later on. This is stored in JOB ROOT.
     * @param submissionid submission ID to save
     * @param build build reference
     * @throws IOException
     */
    private void saveSubmissionId(String submissionid, AbstractBuild<?, ?> build) throws IOException {
        File root =  build.getProject().getRootDir();
        File file = new File(root, SPARK_DEPLOY_SUBMISSION_FILE);

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file)));
            writer.write(submissionid);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    /**
     * Plugin Descriptor.
     */
    @Extension
    @SuppressWarnings("unused")
    public static class Descriptor extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            // Supports all project types
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Deploy to Apache Spark";
        }

        /**
         * Validate Master URL.
         * @param value value
         * @return validation result
         */
        public FormValidation doCheckMasterUrl(@QueryParameter String value) {
            String master = value.trim().toLowerCase();
            if (master.isEmpty()) {
                return FormValidation.error("Master URL is required");
            }

            if (master.startsWith("mesos") || master.startsWith("yarn")) {
                return FormValidation.error("This plugin currently supports only Spark Standalone clusters. " +
                        "Mesos / YARN support is not available yet.");
            }
            master = Utils.getActualSparkMasterUrl(master);

            try {
                new URL(master);
            } catch (MalformedURLException e) {
                return FormValidation.error("Master URL is not a valid URL", e);
            }

            return FormValidation.ok();
        }

        /**
         * Validate Master REST URL if specified.
         * @param value value
         * @return validation result
         */
        public FormValidation doCheckMasterRestUrl(@QueryParameter String value) {
            String masterRest = value.trim();
            if (masterRest.isEmpty()) {
                // Optional Field
                return FormValidation.ok();
            }

            try {
                new URL(masterRest);
            } catch (MalformedURLException e) {
                return FormValidation.error("Master REST URL is not a valid URL", e);
            }

            return FormValidation.ok();
        }

        /**
         * Validate Spark Properties if specified.
         *
         * @param value value
         * @return validation result
         */
        public FormValidation doCheckSparkProperties(@QueryParameter String value) {
            String sparkProps = value.trim();
            if (sparkProps.isEmpty()) {
                // Optional Field
                return FormValidation.ok();
            }

            String[] tokens = sparkProps.split(",");

            for (String token :  tokens) {
                String[] pair = token.split("=");
                if (pair.length != 2) {
                    return FormValidation.error("Invalid Spark Properties. It should be = separated key value pairs " +
                            "combined using a comma. Example: spark.driver.supervise=false,spark.executor.memory=2G");
                }
            }

            return FormValidation.ok();
        }

        /**
         * Validates App Resource.
         * @param value value
         * @return validation result
         */
        public FormValidation doCheckAppResource(@QueryParameter String value) {
            String appResource = value.trim();
            if (appResource.isEmpty()) {
                return FormValidation.error("Application Resource is required");
            }
            return FormValidation.ok();
        }

        /**
         * Validates Main Class.
         * @param value value
         * @return validation result
         */
        public FormValidation doCheckMainClass(@QueryParameter String value) {
            String mainClass = value.trim();
            if (mainClass.isEmpty()) {
                return FormValidation.error("Main Class is required");
            }
            return FormValidation.ok();
        }
    }

    /**
     * Returns master url.
     *
     * @return master url
     */
    @SuppressWarnings("unused")
    public String getMasterUrl() {
        return masterUrl;
    }

    /**
     * Returns master rest url.
     *
     * @return master rest url
     */
    @SuppressWarnings("unused")
    public String getMasterRestUrl() {
        return masterRestUrl;
    }

    /**
     * Returns scala version.
     *
     * @return scala version
     */
    @SuppressWarnings("unused")
    public String getScalaVersion() {
        return scalaVersion;
    }

    /**
     * Returns app resource.
     *
     * @return app resource
     */
    @SuppressWarnings("unused")
    public String getAppResource() {
        return appResource;
    }

    /**
     * Returns main class.
     *
     * @return main class
     */
    @SuppressWarnings("unused")
    public String getMainClass() {
        return mainClass;
    }

    /**
     * Returns app args.
     *
     * @return app args
     */
    @SuppressWarnings("unused")
    public String getAppArgs() {
        return appArgs;
    }

    /**
     * Returns spark properties.
     *
     * @return spark properties
     */
    public String getSparkProperties() {
        return sparkProperties;
    }

    /**
     * Is kill before submit.
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isKillBeforeSubmit() {
        return killBeforeSubmit;
    }

    /**
     * Is fail build on failure.
     *
     * @return boolean
     */
    @SuppressWarnings("unused")
    public boolean isFailBuildOnFailure() {
        return failBuildOnFailure;
    }
}

