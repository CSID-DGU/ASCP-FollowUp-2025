import pandas as pd
import re

SRC = "201801flightdata_파란셀포함.csv"   # 원본
OUT = "flight_201801_o.csv"                     # 출력

df = pd.read_csv(SRC)

# -------------------------
# 1) 결항 제거 (CANCELLED 값이 1/True/'1'/'true' 등 어떤 형태여도 제거)
# -------------------------
if "CANCELLED" in df.columns:
    cancelled = df["CANCELLED"].astype(str).str.strip().str.lower()
    df = df[~cancelled.isin(["1", "true", "t", "yes", "y"])].copy()

# -------------------------
# 2) 시간 파싱 + 파싱 실패 제거
# -------------------------
df["DEP_TIME"] = pd.to_datetime(df["DEP_TIME"], errors="coerce")
df["ARR_TIME"] = pd.to_datetime(df["ARR_TIME"], errors="coerce")
df = df.dropna(subset=["DEP_TIME", "ARR_TIME"]).copy()

# -------------------------
# 3) 필수 컬럼 비어있는 행 제거 (공항/기종)
#    ※ 빈 문자열도 제거
# -------------------------
for col in ["ORIGIN", "DEST", "AIRCRAFT_MODEL"]:
    df[col] = df[col].astype(str)
    df = df[df[col].str.strip() != ""].copy()

# -------------------------
# 4) 기종명 표준화 (필요 기종만)
# -------------------------
def std_aircraft(x):
    s = str(x).strip().replace(" ", "")
    s = re.sub(r"^B767-200$", "B767-2", s, flags=re.IGNORECASE)
    s = re.sub(r"^B777-300$", "B777-3", s, flags=re.IGNORECASE)
    return s.upper()

df["AIRCRAFT_TYPE"] = df["AIRCRAFT_MODEL"].apply(std_aircraft)

# -------------------------
# 5) flight_201801.csv 포맷으로 생성
# -------------------------
out = pd.DataFrame({
    "INDEX": [f"F{i:06d}" for i in range(1, len(df) + 1)],
    "origin": df["DEP_TIME"].dt.strftime("%Y-%m-%d %H:%M"),
    "dest":   df["ARR_TIME"].dt.strftime("%Y-%m-%d %H:%M"),
    "ORIGIN": df["ORIGIN"].str.strip(),
    "DEST":   df["DEST"].str.strip(),
    "AIRCRAFT_TYPE": df["AIRCRAFT_TYPE"],
})

# 혹시라도 값이 비는 행이 남아있으면 마지막 안전장치로 제거
out = out.dropna()
out = out[(out["origin"] != "") & (out["dest"] != "") & (out["ORIGIN"] != "") & (out["DEST"] != "") & (out["AIRCRAFT_TYPE"] != "")]

# 엑셀에서 깨짐 줄이려고 utf-8-sig 추천
out.to_csv(OUT, index=False, encoding="utf-8-sig")
print("saved:", OUT, "rows:", len(out))
print(out.head(3))