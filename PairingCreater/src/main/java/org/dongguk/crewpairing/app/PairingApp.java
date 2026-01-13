package org.dongguk.crewpairing.app;

import lombok.extern.slf4j.Slf4j;
import org.dongguk.common.app.CommonApp;
import org.dongguk.common.business.SolutionBusiness;
import org.dongguk.crewpairing.domain.*;
import org.dongguk.crewpairing.persistence.FlightCrewPairingXlsxFileIO;
import org.dongguk.crewpairing.util.ViewAllConstraint;
import org.optaplanner.core.api.score.ScoreExplanation;
import org.optaplanner.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatchTotal;
import org.optaplanner.core.api.solver.SolutionManager;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;
import org.dongguk.crewpairing.util.RandomPairingGenerator;

import java.util.*;

@Slf4j
public class PairingApp extends CommonApp<PairingSolution> {
    public static final String SOLVER_CONFIG = "solverConfig.xml";

    public static void main(String[] args) {
        System.setIn(new java.io.ByteArrayInputStream("p\n".getBytes()));
        
        // 인자 개수 체크 (최소 7개 필요: dqn 모드 대비)
        if (args.length < 6) {
            System.out.println("Usage: java -jar app.jar <dirPath> <dirName> <flightSize> <inputXlsx> <mode> <iter> [pairingXlsx] <timeLimit>");
            return;
        }

        //실험 전체 시작 시점 기록
        long experimentStart = System.currentTimeMillis();

        String dataDirPath = args[0];
        String dataDirName = args[1];
        Integer flightSize = Integer.valueOf(args[2]);
        String informationXlsxFile = args[3];
        String mode = args[4]; // opis, kbra, dqn
        
        int stepLimit;
        long timeLimitMs;
        String pairingXlsxFile = null;

        // 인자 파싱 (모드별로 인자 위치가 다를 수 있음을 고려)
        if ("dqn".equals(mode)) {
            pairingXlsxFile = args[5];
            stepLimit = Integer.parseInt(args[6]);
            timeLimitMs = Long.parseLong(args[7]);
        } else {
            stepLimit = Integer.parseInt(args[5]);
            timeLimitMs = Long.parseLong(args[6]);
        }

        // 비즈니스 로직 초기화 (Step 및 Time 제한 전달)
        SolutionBusiness<PairingSolution, ?> business = new PairingApp(dataDirPath, dataDirName, informationXlsxFile)
                .init(flightSize, stepLimit, timeLimitMs).getSolutionBusiness();

        
        //비즈니스 로직에 시작 시점 전달
        business.setMethodStartTime(experimentStart);       

        // 엑셀 데이터 로드
        business.openSolution(business.getInputFileList().stream()
                .filter(f -> f.getName().equals(informationXlsxFile))
                .findFirst().orElseThrow());

        // [모드별 초기해 처리 및 생성 시작]
        if ("kbra".equals(mode)) {
            List<Flight> flightList = business.getSolution().getFlightList();
            List<Pairing> randomPairings = RandomPairingGenerator.generate(flightList, 4, 42L);
            business.getSolution().setPairingList(randomPairings);
            System.out.println("[KBRA] Random initial solution generated.");
        } else if ("dqn".equals(mode) && pairingXlsxFile != null) {
            FlightCrewPairingXlsxFileIO xlsxIO = new FlightCrewPairingXlsxFileIO();
            List<Pairing> pairings = xlsxIO.readPairingList(business.getSolution().getFlightList(), 
                business.getOutputFileList().stream().filter(f -> f.getName().equals(args[5])).findFirst().orElse(null));
            business.getSolution().setPairingList(pairings);
            System.out.println("[DQN] Initial solution loaded from: " + pairingXlsxFile);
        }

        // 최적화 시작
        business.solve(business.getSolution());


        // 결과 저장
        business.saveSolution(null);
        System.exit(0);
    }

    public PairingApp(String dataDirPath, String dataDirName, String informationFileName) {
        super("CrewPairing", "Airline Scheduling", SOLVER_CONFIG, dataDirPath, dataDirName, informationFileName);
    }

    @Override public SolutionFileIO<PairingSolution> createSolutionFileIO() { return new FlightCrewPairingXlsxFileIO(); }
}