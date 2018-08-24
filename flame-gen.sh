#!/bin/sh

TMPDIR=./tmp_flame

rm -rf $TMPDIR 2> /dev/null
mkdir $TMPDIR 2> /dev/null

trap ctrl_c INT
profiling=""

function ctrl_c() {
  if [ "$profiling" != "" ]
  then
    sh ./flame-svg-gen.sh $TMPTRACE
  fi
  exit
}


TMPTRACE=./tmp_flame/trace.txt
rm $TMPTRACE 2> /dev/null
pid=""
if [ "$1" == "" ]
then
  echo "waiting for java process..."
  while [ "$pid" == "" ]
  do
      #For Windows with Cygwin use this
  	#pid=`ps -W | grep java | grep -o [0-9]* | head -1`
      pid=`ps | grep java | grep -v grep | grep -o [0-9]* | head -1`
  	if [ "$pid" != "" ]
  	then
  		break
  	fi
  done
else
  pid=$1;
fi
#For Windows with Cygwin use this
#processExists=`ps -pW $pid | grep $pid`
processExists=`ps -p $pid | grep $pid`
if [ "$processExists" != "" ]
then
  echo "process found pid: $pid"
  echo "profiling started, press ctrl+c to end and generate report"
  profiling="true"
  while true;
  do
  	jstack $pid >> $TMPTRACE && sleep 0.7;
  done
else
  echo "process with id $pid doesn't exists"
fi

#echo "Done! please check Flame.svg"
