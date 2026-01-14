#!/bin/bash
set -e

JAR=./crew-pairing.jar

DATA_DIR_PATH=data
DATA_DIR_NAME=crewpairing
FLIGHT_SIZE=17318
INPUT_XLSX=input_17318.xlsx

ITER=2147483640
TIME_MS=36000000   # 600 minutes

mkdir -p logs

echo "=== OPIS + Hill Climbing (600min) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-hc.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_hc_600min.log 2>&1 &

echo "=== OPIS + Great Deluge (600min) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-gd.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_gd_600min.log 2>&1 &

echo "=== OPIS + Tabu Search (600min) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-ts.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_ts_600min.log 2>&1 &

echo "=== KBRA + Hill Climbing (600min) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-hc.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
kbra \
${ITER} \
${TIME_MS} \
> logs/kbra_hc_600min.log 2>&1 &

# echo "=== KBRA + Great Deluge (600min) ==="
# nohup java -jar ${JAR} \
# ${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-gd.xml \
# ${FLIGHT_SIZE} ${INPUT_XLSX} \
# kbra \
# ${ITER} \
# ${TIME_MS} \
# > logs/kbra_gd_600min.log 2>&1 &

# echo "=== KBRA + Tabu Search (600min) ==="
# nohup java -jar ${JAR} \
# ${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-ts.xml \
# ${FLIGHT_SIZE} ${INPUT_XLSX} \
# kbra \
# ${ITER} \
# ${TIME_MS} \
# > logs/kbra_ts_600min.log 2>&1 &

echo "=== ALL JOBS SUBMITTED ==="
