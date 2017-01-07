#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
java -cp "${DIR}/../bin:${DIR}/../lib/*" ui.CLI -protocol run -eddie_ip 35.164.214.188 -debbie_ip 35.167.17.206 charlie
