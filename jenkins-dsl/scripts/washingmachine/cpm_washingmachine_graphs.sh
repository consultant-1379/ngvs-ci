#!/bin/ksh -e

BASEDIR="/proj/kacx/artifacts/rrdtool/cpm/performance"
LOGFILE2="$(ls -1 $BASEDIR/data/jmeterPerformance2.log* | tail -1)"
STATFILE2="$(ls -1 $BASEDIR/data/vmware_statistics2.log* | tail -1)"
PATH=/proj/eta-tools/rrdtool/1.3.8/Linux_x86_64/bin/:${PATH}
export PATH

RRDDB1="$BASEDIR/latest.rrdb"
RRDDB1B="$BASEDIR/latest2.rrdb"
RRDDB2="$BASEDIR/history.rrdb"
RRDDB2B="$BASEDIR/history2.rrdb"
RRDDB3="$BASEDIR/latest_stat.rrdb"
RRDDB3B="$BASEDIR/latest2_stat.rrdb"


# Create datapoints/graphs for the last run

rm -f "$RRDDB1B"
grep "jmeter.reporters.Summariser: summary +" $LOGFILE2 | awk '{print $1 " " $2 " " $8 " " $12 " " $14 " " $16 " " $18 " " $20}' | sed -e 's,/s,,' | while read DAY TIME REQ THROUGH AVG MIN MAX ERR; do
    TIMEVAL="$(date -d "$DAY $TIME" "+%s")"
    if [[ ! -r $RRDDB1B ]]; then
        rrdtool create $RRDDB1B --step 10 --start "$((${TIMEVAL} - 1))" \
            "DS:requests:GAUGE:15:0:U" "DS:errors:GAUGE:15:0:U" "DS:throughput:GAUGE:15:0:U" "DS:min:GAUGE:15:0:U" "DS:avg:GAUGE:15:0:U" "DS:max:GAUGE:15:0:U" \
            "RRA:AVERAGE:0.5:1:720"
        START="${TIMEVAL}"
    fi
    rrdtool update $RRDDB1B "${TIMEVAL}@${REQ}:${ERR}:${THROUGH}:${MIN}:${AVG}:${MAX}" || true
done
rrdtool graph $BASEDIR/latest2-throughput.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "Requests/s" "DEF:throughput=${RRDDB1B}:throughput:AVERAGE" "LINE:throughput#00aa00:Requests"
rrdtool graph $BASEDIR/latest2-min-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "Response Times ms (minimum)" "DEF:min=${RRDDB1B}:min:AVERAGE" "LINE:min#00aa00:Min"
rrdtool graph $BASEDIR/latest2-avg-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "Response Times ms (average)" "DEF:avg=${RRDDB1B}:avg:AVERAGE" "LINE:avg#00aa00:Avg"
rrdtool graph $BASEDIR/latest2-max-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "Response Times ms (maximum)" "DEF:max=${RRDDB1B}:max:AVERAGE" "LINE:max#00aa00:Max"

# Add datapoint/create graph for the history
grep "jmeter.reporters.Summariser: summary =" $LOGFILE2 | tail -1 | awk '{print $1 " " $2 " " $8 " " $12 " " $14 " " $16 " " $18 " " $20}' | sed -e 's,/s,,' | while read DAY TIME REQ THROUGH AVG MIN MAX ERR; do
    TIMEVAL="$(date -d "$DAY $TIME" "+%s")"
    if [[ ! -r $RRDDB2B ]]; then
        rrdtool create $RRDDB2B --step 3600 --start "$((${TIMEVAL} - 1))" \
            "DS:requests:GAUGE:7200:0:U" "DS:errors:GAUGE:7200:0:U" "DS:throughput:GAUGE:7200:0:U" "DS:min:GAUGE:7200:0:U" "DS:avg:GAUGE:7200:0:U" "DS:max:GAUGE:7200:0:U" \
            "RRA:LAST:0:1:4032"
    fi
    rrdtool update $RRDDB2B "${TIMEVAL}@${REQ}:${ERR}:${THROUGH}:${MIN}:${AVG}:${MAX}" || true
done
START="$(($TIMEVAL - 2592000))"
rrdtool graph $BASEDIR/history-throughput.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 --title "Requests/s" \
    "DEF:throughput=${RRDDB2}:throughput:LAST" "LINE:throughput#00aa00:Requests" "DEF:throughput2=${RRDDB2B}:throughput:LAST" "LINE:throughput2#0000aa:Requests"
rrdtool graph $BASEDIR/history-min-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 --title "Response Times ms (minimum)" \
    "DEF:min=${RRDDB2}:min:LAST" "LINE:min#00aa00:Min" "DEF:min2=${RRDDB2B}:min:LAST" "LINE:min2#0000aa:Min"
rrdtool graph $BASEDIR/history-avg-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 --title "Response Times ms (average)" \
    "DEF:avg=${RRDDB2}:avg:LAST" "LINE:avg#00aa00:Avg" "DEF:avg2=${RRDDB2B}:avg:LAST" "LINE:avg2#0000aa:Avg"
rrdtool graph $BASEDIR/history-max-response.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 --title "Response Times ms (maximum)" \
    "DEF:max=${RRDDB2}:max:LAST" "LINE:max#00aa00:Max" "DEF:max2=${RRDDB2B}:max:LAST" "LINE:max2#0000aa:Max"

# Add CPU/Mem info for the last run
rm -f ${RRDDB3B}
tr "," " " < $STATFILE2 | while read DAY TIME PTIME PSTOLE VMMHZ HOSTMHZ T1 T2 T3 MEMACTIVE T4 MEMMAP MEMOVH T5 T6 T7 MEMTARG MEMUSE T8 T9 MEMTOT; do
    if [[ "$DAY" != "Timestamp" ]]; then
        TIMEVAL="$(date -d "$DAY $TIME" "+%s")"
        if [[ ! -r ${RRDDB3B} ]]; then
            rrdtool create ${RRDDB3B} --step 10 --start "$((${TIMEVAL} - 1))" \
                "DS:ptime:GAUGE:15:0:U" "DS:pstole:GAUGE:15:0:U" "DS:vmmhz:GAUGE:15:0:U" "DS:hostmhz:GAUGE:15:0:U" \
                "DS:memactive:GAUGE:15:0:U" "DS:memmaped:GAUGE:15:0:U" "DS:memoverhead:GAUGE:15:0:U" "DS:memtarget:GAUGE:15:0:U" "DS:memuse:GAUGE:15:0:U" "DS:memtot:GAUGE:15:0:U" \
                "RRA:AVERAGE:0.5:1:720" "RRA:AVERAGE:0.5:6:1460" "RRA:AVERAGE:0.5:30:2016"
            START="${TIMEVAL}"
        fi
        rrdtool update ${RRDDB3B} "${TIMEVAL}@${PTIME}:${PSTOLE}:${VMMHZ}:${HOSTMHZ}:${MEMACTIVE}:${MEMMAP}:${MEMOVH}:${MEMTARG}:${MEMUSE}:$((${MEMTOT} / 10))"
    fi
done
rrdtool graph $BASEDIR/latest2-vm-cpu1.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "VM CPU utilization (%)" "DEF:ptime=${RRDDB3B}:ptime:AVERAGE" "DEF:pstole=${RRDDB3B}:pstole:AVERAGE" \
    "LINE:ptime#00aa00:CPU%" "LINE:pstole#aa0000:CPU% stolen"
rrdtool graph $BASEDIR/latest2-vm-cpu2.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "VM CPU speed (MHz)" "DEF:vmmhz=${RRDDB3B}:vmmhz:AVERAGE" "DEF:hostmhz=${RRDDB3B}:hostmhz:AVERAGE" \
    "LINE:vmmhz#00aa00:VM MHz" "LINE:hostmhz#666666:Host MHz"
rrdtool graph $BASEDIR/latest2-vm-mem.png --imgformat PNG -w 600 -h 300 --start "${START}" --end "${TIMEVAL}" --lower-limit 0 \
    --title "VM Memory statistics (Mb)" "DEF:memactive=${RRDDB3B}:memactive:AVERAGE" "DEF:memtarget=${RRDDB3B}:memtarget:AVERAGE" \
    "DEF:memuse=${RRDDB3B}:memuse:AVERAGE" "DEF:memtot=${RRDDB3B}:memtot:AVERAGE" \
    "AREA:memactive#0000aa:Active memory" "LINE:memuse#00aa00:Used memory" "LINE:memtarget#666666:Total memory" "LINE:memtot#666666:Max memory"

# Clean up older files than approx 4 days
ls -1 $BASEDIR/data/jmeterPerformance2.log* | head -n -96 | xargs rm -f
ls -1 $BASEDIR/data/vmware_statistics2.log* | head -n -96 | xargs rm -f
