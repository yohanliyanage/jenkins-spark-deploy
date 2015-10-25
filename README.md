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
* Spark Master URL - This is passed as a configuration to executors. Example: <i>spark://master.spark.cluster.com:6066</i>
* Spark Master REST HTTP URL - Optional. If your Spark Masters are behind a load balancer / proxy, you can use this to tell Jenkins which URL to use for communicating over REST. Also, if you have HTTPS enabled, you will have to use this option. If this is not provided, the Spark Master URL will be used to determine the default (that is spark:// will become http://). Example: <i>http://master.spark.cluster.com:6066</i>
* Spark Scala Version - Spark Scala Version. Default: <i>2.10</i>.
* Application Resource URL - Application JAR URL. If the JAR file is in HDFS, this should be the HDFS URL. If it is available over HTTP or HTTPS, this should be the relevant URL. File URLs are also supported, but those URLs should be valid within executors for that to work. Example: <i>http://some.file.server/spark-app.jar</i>
* Main Class - The fully qualified main class name. For example, <i>org.spark.demo.app.Main</i>
* Application Arguments - These are the command line arguments that will passed to the Spark application. Multiple arguments should be separated by a space. For example: <i>arg1 arg2 arg3</i>
* Spark Configuration Properties - Additional properties for Spark. These are typically passed in as --conf parameters for Spark Submit. The settings should be specified as key value pairs separated by commas. Example: <i>spark.driver.supervise=false,spark.executor.memory=2G</i>
* Kill previous submissions before deploying - If enabled, Jenkins will try to kill the last submission done for this job before deploying latest version.
* Fail the build if deployment fails - It does exactly what it says :)
* Verbose - Prints the REST request / response in build output for debugging purposes.

## Notes
* Spark on YARN and Mesos are not supported yet.
