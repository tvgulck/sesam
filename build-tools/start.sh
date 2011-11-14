#! /bin/bash

# Copyright 2011 Vlaams Gewest
#
# This file is part of SESAM, the Service Endpoint Security And Monitoring framework.
#
# SESAM is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# SESAM is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with SESAM.  If not, see <http://www.gnu.org/licenses/>.

ps aux | grep bin/felix.jar | grep -v "grep bin/felix.jar"
if [ "$?" -ne "0" ]; then

	echo "Starting Sesam"
	export JAVA_OPTS="-server -Xms128m -Xmx512m -XX:MaxPermSize=128m"
	echo "JAVA_OPTS: $JAVA_OPTS"

	cd /opt/sesam
	java -Dfelix.config.properties=file:conf/config.properties -Dlogback.configurationFile=configurations/logback.xml -jar bin/felix.jar

else
	
	echo "Sesam is already running"
	exit 1
	
fi
