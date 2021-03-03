
if type -p java; then
    echo found java executable in PATH
    _java=java
elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
    echo found java executable in JAVA_HOME     
    _java="$JAVA_HOME/bin/java"
else
    echo "no java"
fi

if [[ "$_java" ]]; then
    version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo version "$version"
    if [[ "$version" > "1.5" ]]; then
        echo version is more than 1.5
        RED='\033[0;31m'
        NC='\033[0m' # No Color
        printf "${RED}ERROR! YOU NEED TO USE JAVA 8${NC}\n"
    else         
        echo version is less than 1.5
    fi
fi