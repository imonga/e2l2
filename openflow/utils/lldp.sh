#!/bin/bash

echo "Starting Capture for lldp packets"

# int - interface on which you want to capture the lldp packets
int="eth2"

tcpdump -vvv -n -e -S -c 1 -s 1514 ether proto 0x88cc -i $int > log.txt

cnt=1;
dpid=""
 
for i in $(fold -b log.txt);
	do
		cnt=$(expr $cnt + 1 )
		if [ "$cnt" -eq 19 ]
		then
  		echo "Port Id is 0x$i"
		fi
		if [ "$cnt" -eq 28 ] || [ "$cnt" -eq 29 ] || [ "$cnt" -eq 30 ] || [ "$cnt" -eq 31 ]
		then 
		dpid="$dpid $i"
		fi
done

echo "Datapath id is $dpid"
