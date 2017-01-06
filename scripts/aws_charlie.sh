#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol run -eddie_ip 35.162.230.27 -debbie_ip 35.163.125.170 charlie
