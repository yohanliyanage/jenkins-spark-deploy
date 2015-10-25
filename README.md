# Jenkins Spark Deployer

This Jenkins plugin enables you to deploy Apache Spark Applications to Spark Standalone clusters as a post build action. Keeps track of previous deployments and you can use the 'kill' feature to kill the previous submission before deploying a new version of your application.

## Features
* Support for custom Spark Configuration options.
* Kill previous submissions - Allows you to kill your last submission automatically before deploying the new version, if desired.
* Fail build on unsuccessful deployments (configurable).
* A verbose mode, which prints out requests / responses for easier debugging when things doesn't work as expected.

## Installation
TBD

## Screenshots
![Options](http://yohanliyanage.github.io/jenkins-spark-deploy/spark-deploy-options.png)

# Configuration Options

<table>
    <tr>
        <td>
            Spark Master URL (*)
        </td>
        <td>
            This is passed as a configuration to executors. Example: <i>spark://master.spark.cluster.com:6066</i>
        </td>
    </tr>
    <tr>
        <td>
            Spark Master REST HTTP URL
        </td>
        <td>
            Optional. If your Spark Masters are behind a load balancer / proxy, you can use this to tell Jenkins which URL to use for communicating over REST. Also, if you have HTTPS enabled, you will have to use this option. If this is not provided, the Spark Master URL will be used to determine the default (that is spark:// will become http://). Example: <i>http://master.spark.cluster.com:6066</i>
        </td>
    </tr>
    <tr>
        <td>
            Spark Scala Version
        </td>
        <td>
            Optional. Spark Scala Version. Default: <i>2.10</i>.
        </td>
    </tr>
    <tr>
        <td>
            Application Resource URL (*)
        </td>
        <td>
            Application JAR URL. If the JAR file is in HDFS, this should be the HDFS URL. If it is available over HTTP or HTTPS, this should be the relevant URL. File URLs are also supported, but those URLs should be valid within executors for that to work. Example: <i>http://some.file.server/spark-app.jar</i>
        </td>
    </tr>
    <tr>
        <td>
            Main Class (*)
        </td>
        <td>
            The fully qualified main class name. For example, <i>org.spark.demo.app.Main</i>
        </td>
    </tr>
    <tr>
        <td>
            Application Arguments
        </td>
        <td>
            These are the command line arguments that will passed to the Spark application. Multiple arguments should be separated by a space. For example: <i>arg1 arg2 arg3</i>
        </td>
    </tr>
    <tr>
        <td>
            Spark Configuration Properties
        </td>
        <td>
            Additional properties for Spark. These are typically passed in as --conf parameters for Spark Submit. The settings should be specified as key value pairs separated by commas. Example: <i>spark.driver.supervise=false,spark.executor.memory=2G</i>
        </td>
    </tr>
    <tr>
        <td>
            Kill previous submissions before deploying
        </td>
        <td>
            If enabled, Jenkins will try to kill the last submission done for this job before deploying latest version.
        </td>
    </tr>
    <tr>
        <td>
            Fail the build if deployment fails
        </td>
        <td>
            It does exactly what it says :)
        </td>
    </tr>
    <tr>
        <td>
            Verbose
        </td>
        <td>
            Prints the REST request / response in build output for debugging purposes.
        </td>
    </tr>
</table>
(*) - Required Field

## Notes
* Spark on YARN and Mesos are not supported yet.
