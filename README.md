# Jenkins Spark Deployer

This Jenkins plugin enables you to deploy Spark Applications to Spark Standalone clusters as a post build action. Keeps track of previous deployments and you can use the 'kill' feature to kill the previous submission before deploying a new version of your application.

## Features
* Support for custom Spark Configuration options.
* Kill previous submissions option - Allows you to kill your last submission automatically before deploying the new version 
* Fail build on unsuccessful deployments feature.
* A verbose mode, which prints out requests / responses for easier debugging when things doesn't work as expected.

## Installation
TBD

## Screenshots
![Options](http://yohanliyanage.github.io/jenkins-spark-deploy/spark-deploy-options.png)

## Notes
* Spark on YARN and Mesos are not supported yet.
