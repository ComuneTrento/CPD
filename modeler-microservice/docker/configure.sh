#!/bin/bash

cd "$(dirname "$0")"

source environment.sh

#checking if oauth2providers file exists
if [ -f $OAUTH2PROVIDERSFILE ]; then
	echo "found an $OAUTH2PROVIDERSFILE file. Do you want to keep it? (Y/n)"
        read keepit
        if [ "$keepit" = "n" ]; then
                echo "generate a new $OAUTH2PROVIDERSFILE file (use docker.oauth2providers.json as a template) and re-run the script!"
		exit 1
        fi
else
	echo "generate an $OAUTH2PROVIDERSFILE file (use docker.oauth2providers.json as a template) and re-run the script!"
	exit 1
fi

#checking if keystore exists
if [ -f $KEYSTOREFILE ]; then
	echo "found a keystore file. Do you want to keep it? (Y/n)"
        read keepit
        if [ "$keepit" = "n" ]; then
                echo "generate a new keystore file and re-run the script!"
		exit 1
        fi
else
	echo "generate a keystore file and re-run the script!"
	exit 1
fi

#checking if PRODUCTIONFILE exists
if [ -f $PRODUCTIONFILE ]; then
	echo "found $PRODUCTIONFILE file. Do you want to keep it? (Y/n)"
	read keepit
	if [ "$keepit" = "" ] || [ "$keepit" = "y" ] || [ "$keepit" = "Y" ]; then 
		exit 0;
	else
		echo "Loading existing $PRODUCTIONFILE file. Creating json config files and language files from template..."
		cp $DOCKERJSONCONF1 $JSONCONF1	
		cp $DOCKERJSONCONF2 $JSONCONF2
		cp $DOCKERJSONCONF3 $JSONCONF3
		cp $DOCKERJSONCONF4 $JSONCONF4
		cp $DOCKERCLUSTERCONF $CLUSTERCONF
		cp $DOCKERINDEXHTMLES $INDEXHTMLES
		cp $DOCKERINDEXHTMLEN $INDEXHTMLEN
		cp $DOCKERINDEXHTMLIT $INDEXHTMLIT
		cp $DOCKERINDEXHTMLGL $INDEXHTMLGL
		#cp $DOCKERFILE $PRODUCTIONFILE
	fi
else 
	#creating a PRODUCTIONFILE file
	echo "$PRODUCTIONFILE file not found. Creating from template..."
	cp $DOCKERFILE $PRODUCTIONFILE
	echo "Creating json config files and languages files from template..."
	cp $DOCKERJSONCONF1 $JSONCONF1
	cp $DOCKERJSONCONF2 $JSONCONF2
	cp $DOCKERJSONCONF3 $JSONCONF3
	cp $DOCKERJSONCONF4 $JSONCONF4
	cp $DOCKERCLUSTERCONF $CLUSTERCONF
	cp $DOCKERINDEXHTMLES $INDEXHTMLES
	cp $DOCKERINDEXHTMLEN $INDEXHTMLEN
	cp $DOCKERINDEXHTMLIT $INDEXHTMLIT
	cp $DOCKERINDEXHTMLGL $INDEXHTMLGL
fi


#grep "^[^#\!].*$" example.properties | while read input; 

foundsslenabled=false
sslenabled=false

foundserverscheme=false
serverscheme=""

foundserverhost=false
serverhost=""

foundserverport=false
serverport=""

foundoauth2origin=false

foundoauth2providers=false

for input in $(grep "^[^#\!].*$" $PRODUCTIONFILE);
do 
#IFS='=' read -r -a array <<< "$input"
set -f
IFS='=' read -r key value <<< "$input"
array=($key $value)
set +
echo "read ${array[0]}=${array[1]}"
#if  [ ${array[0]} != "cpd.server.pub.scheme" ] || [ ${array[0]} != "cpd.oauth2.origin" ] ; then
if ! [[ ${array[0]} =~ ^cpd\.(server\.scheme|oauth2\.origin|oauth2\.providers)$ ]] ; then  
	echo "Insert value for property \"${array[0]}\" [${array[1]}]"
	read userinput
	echo "user input=$userinput"
	if [ "$userinput" != "" ]; then
		array[1]=$userinput
	fi
fi

if [ $foundsslenabled = false -a ${array[0]} = "cpd.ssl.enabled" ]; then
		
	foundsslenabled=true	
	sslenabled=${array[1]}

elif [ $foundserverscheme = false -a ${array[0]} = "cpd.server.pub.scheme" ]; then
	foundserverscheme=true
	if [ $sslenabled = true ]; then
		array[1]="https"
		 
	else
		array[1]="http"
	fi
	serverscheme=${array[1]}

elif [ $foundserverhost = false -a ${array[0]} = "cpd.server.pub.host" ]; then
	foundserverhost=true
	serverhost=${array[1]}

elif [ $foundserverport = false -a ${array[0]} = "cpd.server.pub.port" ]; then
	foundserverport=true
	serverport=${array[1]}

elif [ $foundoauth2origin = false -a ${array[0]} = "cpd.oauth2.origin" ]; then
	foundoauth2origin=true
	array[1]=$serverscheme"://"$serverhost":"$serverport

elif [ $foundoauth2providers = false -a ${array[0]} = "cpd.oauth2.providers" ]; then
	foundoauth2providers=true

	if [[ ${array[1]} != "[*" ]]; then
		#array[1]=${array[1]}	
	#else	
		array[1]=$(cat $OAUTH2PROVIDERSFILE | sed ':a;N;$!ba;s/\n//g' | tr -d '[:space:]')
		#buffer="$(sed ':a;N;$!ba;s/\n//g' <<<$buffer)"
		#trimmed_buffer=$(tr -d '[:space:]' $buffer)
		#$buffer="$(sed 'sE/[\n ]+//g' <<<$buffer)"
		#array[1]=$(tr -d '[:space:]' "$buffer")
		#tr '\n' ' ' "$buffer" | echo
	fi
	#echo "$OAUTH2PROVIDERSFILE json is: ${array[1]}"
fi

echo "writing ${array[0]}=${array[1]} to $PRODUCTIONFILE file"
sed -i "s|${array[0]}=.*|${array[0]}=${array[1]}|" $PRODUCTIONFILE

echo "writing ${array[1]} value to \${${array[0]}} property in json and language files"
sed -i "s#\${${array[0]}}#${array[1]}#" $JSONCONF1 $JSONCONF2 $JSONCONF3 $JSONCONF4 $CLUSTERCONF $INDEXHTMLES $INDEXHTMLEN $INDEXHTMLIT $INDEXHTMLGL

#echo "writing ${array[1]} value to \${${array[0]}} property in $JSONCONF2 file"
#sed -i "s#\${${array[0]}}#${array[1]}#" $JSONCONF2

#sed -E '/(#.*)/!s/${array[0]}=.*/${array[0]}=${array[1]}/g' $PRODUCTIONFILE
done
echo "copying configuration files to the backup directory..........."
	cp $PRODUCTIONFILE $PRODUCTIONFILE_BK
	cp $JSONCONF1 $JSONCONF1_BK	
	cp $JSONCONF2 $JSONCONF2_BK
	cp $JSONCONF3 $JSONCONF3_BK
	cp $JSONCONF4 $JSONCONF4_BK
	cp $CLUSTERCONF $CLUSTERCONF_BK
	cp $INDEXHTMLEN $INDEXHTMLEN_BK
	cp $INDEXHTMLES $INDEXHTMLES_BK
	cp $INDEXHTMLIT $INDEXHTMLIT_BK
	cp $INDEXHTMLGL $INDEXHTMLGL_BK
	cp $KEYSTOREFILE $KEYSTOREFILE_BK
	cp $OAUTH2PROVIDERSFILE $OAUTH2PROVIDERSFILE_BK
