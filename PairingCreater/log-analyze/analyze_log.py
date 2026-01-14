import sys
import os
from datetime import datetime

def analyze_log(log_filename):
    input_dir = "input"
    output_dir = "output"

    log_path = os.path.join(input_dir, log_filename)
    if not os.path.exists(log_path):
        raise FileNotFoundError(f"로그 파일이 존재하지 않습니다: {log_path}")

    hard_score = None
    soft_score = None
    total_time = None
    pure_solve = None
    init_time = None
    best_score_time = None
    step_total = None

    with open(log_path, encoding="utf-8", errors="ignore") as f:
        for raw_line in f:
            line = raw_line.strip()

            # --- 점수 ---
            if "Hard Score :" in line:
                hard_score = line.split("Hard Score :")[-1].strip()

            elif "Soft Score :" in line:
                soft_score = line.split("Soft Score :")[-1].strip()

            # --- 시간 ---
            elif "Total wall time(ms)" in line:
                total_time = line.split("=")[-1].strip()

            elif "Pure solve time(ms)" in line:
                pure_solve = line.split("=")[-1].strip()

            elif line.startswith("Initial solution time(ms)"):
                init_time = line.split("=")[-1].strip()

            elif "Best score first reached at(ms)" in line:
                best_score_time = line.split("=")[-1].strip()

            # --- iteration ---
            elif "Local Search phase" in line and "step total" in line:
                # 예: step total (24072275)
                step_total = line.split("step total (")[-1].split(")")[0]

    # 필수 지표 검증
    missing = []
    if hard_score is None: missing.append("Hard Score")
    if soft_score is None: missing.append("Soft Score")
    if total_time is None: missing.append("Total wall time")

    if missing:
        raise ValueError(f"필수 지표 누락: {', '.join(missing)}")

    now = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    summary = f"""
{'='*60}
   [논문 TABLE 1 대응 로그 기반 분석 리포트]
{'='*60}
실험 일시 : {now}
로그 파일 : {log_filename}
{'-'*60}
 [핵심 지표 (Main Metrics)]
  1. Hard Score            : {hard_score}
  2. Soft Score (CS)       : {soft_score}
  3. Iterations (LS steps) : {step_total}
{'-'*60}
 [시간 지표]
  * Initial solution time(ms) : {init_time}
  * Pure solve time(ms)       : {pure_solve}
  * Total wall time(ms)       : {total_time}
  * Best score reached at(ms) : {best_score_time}
{'='*60}
"""

    print(summary)

    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(
        output_dir,
        f"log_summary_{os.path.splitext(log_filename)[0]}.txt"
    )

    with open(out_path, "w", encoding="utf-8") as f:
        f.write(summary)

    print(f"[OK] Summary saved to {out_path}")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("사용법: python analyze_log.py <logfile>")
        sys.exit(1)

    analyze_log(sys.argv[1])
