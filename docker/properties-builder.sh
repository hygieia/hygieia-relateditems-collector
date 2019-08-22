#!/bin/bash

# if [ "$SKIP_PROPERTIES_BUILDER" = true ]; then
#   echo "Skipping properties builder"
#   exit 0
# fi

# if we are linked, use that info
# if [ "$MONGO_STARTED" != "" ]; then
#   # links now use hostnames
#   # todo: retrieve linked information such as hostname and port exposition
#   export SPRING_DATA_MONGODB_HOST=mongodb
#   export SPRING_DATA_MONGODB_PORT=27017
# fi

echo "SPRING_DATA_MONGODB_HOST: $SPRING_DATA_MONGODB_HOST"
echo "SPRING_DATA_MONGODB_PORT: $SPRING_DATA_MONGODB_PORT"


cat > $PROP_FILE <<EOF
#Database Name - default is test
dbname=${SPRING_DATA_MONGODB_DATABASE:-dashboarddb}

#Database HostName - default is localhost
dbhost=${SPRING_DATA_MONGODB_HOST:-db}

#Database Port - default is 27017
dbport=${SPRING_DATA_MONGODB_PORT:-27017}

#Database Username - default is blank
dbusername=${SPRING_DATA_MONGODB_USERNAME:-dashboarduser}

#Database Password - default is blank
dbpassword=${SPRING_DATA_MONGODB_PASSWORD:-dbpassword}


#This is ensure if you are keeping DB outside docker compose.
dbhostport=${SPRING_DATA_MONGODB_HOST}:${SPRING_DATA_MONGODB_PORT}

EOF
