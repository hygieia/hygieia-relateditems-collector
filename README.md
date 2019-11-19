# hygieia-relateditems-collector
[![Total alerts](https://img.shields.io/lgtm/alerts/g/Hygieia/hygieia-relateditems-collector.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/Hygieia/hygieia-relateditems-collector/alerts/)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/Hygieia/hygieia-relateditems-collector.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/Hygieia/hygieia-relateditems-collector/context:java)
[![License](https://img.shields.io/badge/license-Apache%202-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Gitter Chat](https://badges.gitter.im/Join%20Chat.svg)](https://www.apache.org/licenses/LICENSE-2.0)
<br>This project repository is a scheduled data collection from relatedItems collection in Hygieia and post it to subscriber end point.

### Setup Instructions - Build & Deploy

To configure the Related Items collector Job, execute the following steps:

*   **Step 1: Change Directory**

Change the current working directory to the `hygieia-relatedItems-collector` directory of your source code installation.

For example, in the Mac terminal, run the following command:

```
cd /Users/<user>/git/forks/hygieia-relateditems-collector
```

*   **Step 2: Run Maven Build**

Run the maven build to package the project into an executable jar file:

```bash
 mvn install
```

The output file `hygieia-relateditems-collector.jar` is generated in the `hygieia-relateditems-collector/target` folder.

*   **Step 3: Set Parameters in Application Properties File**

Set the configurable parameters in the `application.properties` file to connect to the Dashboard MongoDB database instance, including properties required by the relateditems-collector Job.

For information about sourcing the application properties file, refer to the [Spring Boot Documentation](http://docs.spring.io/spring-boot/docs/current-SNAPSHOT/reference/htmlsingle/#boot-features-external-config-application-property-files).

To configure parameters for the relateditems-collector Job, refer to the sample [application.properties](#sample-application-properties-file) file.

*   **Step 4: Deploy the Executable File**

To deploy the `hygieia-relateditems-collector.jar` file, change directory to `hygieia-relateditems-collector/target`, and then execute the following from the command prompt:

```bash
java -jar hygieia-relateditems-collector.jar --spring.config.name=subversion --spring.config.location=[path to application.properties file]
```

### Sample Application Properties File

The sample `application.properties` file lists parameter values to configure the relateditems-collector Job. Set the parameters based on your environment setup.

```properties
		# Database Name
		dbname=dashboarddb

		# Database HostName - default is localhost
		dbhost=localhost

		# Database Port - default is 27017
		dbport=27017

		# MongoDB replicaset
		dbreplicaset=[false if you are not using MongoDB replicaset]
		dbhostport=[host1:port1,host2:port2,host3:port3]

		# Database Username - default is blank
		dbusername=dashboarduser

		# Database Password - default is blank
		dbpassword=dbpassword

		# Logging File location
		logging.file=./logs/hygieia-relateditems-collector.log

		# Collector schedule (required)
		relateditems.cron=0 0/30 * * * *

		# server address
		relateditems.subscribers[0]=<server address>

		# Optional timestamp value to configure test results by specific time
		relateditems.lastExecutedTimeStamp=
```

# Developing.

Before making any contribvutions, we suggest that you read the [CONTRIBUTING.md](CONTRIBUTING.md) and the 
[Development Docs](./src) (housed in `./src/docs/README.md`). But for the most part, running:

```
mvn clean test install site
``` 

will test and generate the project metrics (navigable from the locally built `./target/site/index.html` 
-- see "Project Reports" in the left nav for all of the available reports). We generally want these 
reports to look good.

