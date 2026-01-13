import pandas as pd
import sys
import os
from datetime import datetime

def extract_paper_metrics(result_filename, flight_info_filename):
    try:
        base_dir = "log-analyze"
        input_dir = os.path.join(base_dir, "input")
        output_dir = os.path.join(base_dir, "output")
        
        result_xlsx_path = os.path.join(input_dir, result_filename)
        flight_info_path = os.path.join(input_dir, flight_info_filename)
        
        if not os.path.exists(output_dir): os.makedirs(output_dir)

        # 1. 데이터 로드
        flight_df = pd.read_excel(flight_info_path, sheet_name='User_Flight', skiprows=2)
        flight_data = flight_df.to_dict('records')
        result_df = pd.read_excel(result_xlsx_path, sheet_name='Data', skiprows=1, header=None)
        
        gp = 0
        dh_count = 0
        total_duration_minutes = 0
        active_legs = 0

        for idx, row in result_df.iterrows():
            pairing_flights = [int(f) for f in row[1:].dropna()]
            if not pairing_flights: continue
                
            gp += 1
            active_legs += len(pairing_flights)
            
            first_f = flight_data[pairing_flights[0]]
            last_f = flight_data[pairing_flights[-1]]
            
            if first_f['ORIGIN'] != last_f['DEST']:
                dh_count += 1
            
            start_t = pd.to_datetime(first_f['ORIGIN_DATE'])
            end_t = pd.to_datetime(last_f['DEST_DATE'])
            total_duration_minutes += (end_t - start_t).total_seconds() / 60

        # 결과 저장
        now_dt = datetime.now()
        output_filename = os.path.join(output_dir, f"result_summary_{now_dt.strftime('%Y%m%d_%H%M%S')}.txt")

        # [참고] CS는 실제 로그의 Soft Score를 절대값으로 기입
        summary = (
            f"\n{'='*60}\n"
            f"   [논문 TABLE 1 대응 데이터 분석 리포트]\n"
            f"{'='*60}\n"
            f"실험 일시 : {now_dt.strftime('%Y-%m-%d %H:%M:%S')}\n"
            f"결과 파일 : {result_filename}\n"
            f"입력 파일 : {flight_info_filename}\n"
            f"{'-'*60}\n"
            f" [핵심 지표 (Main Metrics)]\n"
            f"  1. GP (Generated Pairings) : {gp:,}\n"
            f"  2. DH (Deadheads)          : {dh_count:,}\n"
            f"  3. DH/GP (%)               : {(dh_count/gp*100):.2f}%\n"
            f"  4. CS (Cost Score)         : [로그의 Soft Score로 수기 입력 필요]\n"
            f"\n [비교 지표용 데이터 (For CRD, PRG, DRG)]\n"
            f"  * Total Mandays            : {total_duration_minutes / (24*60):.2f}\n"
            f"  * Active Legs              : {active_legs:,}\n"
            f"{'='*60}\n"
        )

        print(summary)
        with open(output_filename, "w", encoding="utf-8") as f: f.write(summary)
            
    except Exception as e:
        print(f"에러 발생: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3: print("사용법: python analyze.py [결과파일명] [입력파일명]")
    else: extract_paper_metrics(sys.argv[1], sys.argv[2])