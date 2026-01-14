#!/bin/bash
set -e

JAR=./crew-pairing.jar

DATA_DIR_PATH=data
DATA_DIR_NAME=crewpairing
FLIGHT_SIZE=17318
INPUT_XLSX=input_17318.xlsx

# ===== 2분 테스트용 설정 =====
ITER=5000000
TIME_MS=120000   # 2 minutes

mkdir -p logs

echo "=== OPIS + Hill Climbing (2min test) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-hc.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_hc_2min.log 2>&1 &

echo "=== OPIS + Great Deluge (2min test) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-gd.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_gd_2min.log 2>&1 &

echo "=== OPIS + Tabu Search (2min test) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-ts.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
opis \
${ITER} \
${TIME_MS} \
> logs/opis_ts_2min.log 2>&1 &

echo "=== KBRA + Hill Climbing (2min test) ==="
nohup java -jar ${JAR} \
${DATA_DIR_PATH} ${DATA_DIR_NAME} solverConfig-hc.xml \
${FLIGHT_SIZE} ${INPUT_XLSX} \
kbra \
${ITER} \
${TIME_MS} \
> logs/kbra_hc_2min.log 2>&1 &

echo "=== ALL JOBS SUBMITTED (2min test) ==="
