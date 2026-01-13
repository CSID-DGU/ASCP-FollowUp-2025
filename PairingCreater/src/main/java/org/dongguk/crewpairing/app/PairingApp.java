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

    public static void main(String[] args) {
        
        if (args.length < 8) {
            throw new IllegalArgumentException(
                "Usage: java -jar crew-pairing.jar "
            + "<dataDirPath> <dataDirName> <solverConfig.xml> "
            + "<flightSize> <input.xlsx> "
            + "<opis|kbra|dqn> [pairing.xlsx] <iteration> <timeLimitMs>"
            );
        }

        String mode = args[5]; // opis | kbra | dqn

        if ("dqn".equals(mode) && args.length < 9) {
            throw new IllegalArgumentException(
                "DQN mode requires: [pairing.xlsx] <iteration> <timeLimitMs>"
            );
        }


        String dataDirPath = args[0];
        String dataDirName = args[1];
        String solverConfigResource = args[2];
        Integer flightSize = Integer.valueOf(args[3]);
        String informationXlsxFile = args[4];

        String pairingXlsxFile = null;
        int stepLimit;
        long timeLimitMs;

        switch (mode) {
            case "opis":
                stepLimit = Integer.parseInt(args[6]);
                timeLimitMs = Long.parseLong(args[7]);
                break;

            case "kbra":
                stepLimit = Integer.parseInt(args[6]);
                timeLimitMs = Long.parseLong(args[7]);
                break;

            case "dqn":
                pairingXlsxFile = args[6];
                stepLimit = Integer.parseInt(args[7]);
                timeLimitMs = Long.parseLong(args[8]);
                break;

            default:
                throw new IllegalArgumentException(
                    "mode must be one of: opis | kbra | dqn"
                );
        }

        System.out.println("================================");
        System.out.println("Mode       = " + mode);
        System.out.println("Iteration  = " + stepLimit);
        System.out.println("Time limit = " + timeLimitMs + " ms");

        if (pairingXlsxFile != null) {
            System.out.println("Init file  = " + pairingXlsxFile);
        }
        System.out.println("================================");


        assert flightSize != null;
        SolutionBusiness<PairingSolution, ?> business = new PairingApp(dataDirPath, dataDirName, solverConfigResource, informationXlsxFile)
                .init(flightSize, stepLimit, timeLimitMs).getSolutionBusiness();

        business.setTimeLimitMs(timeLimitMs);
        business.setOpisMode("opis".equals(mode));

        // Input Information Xlsx File
        business.openSolution(
                business.getInputFileList()
                        .stream()
                        .filter(inputFile -> inputFile.getName().equals(informationXlsxFile))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException("파일이 존재하지 않습니다.")));

        // ===== Initial solution handling by mode =====

        if ("opis".equals(mode)) {
            System.out.println("[OPIS] No initial solution injected.");
        }

        if ("kbra".equals(mode)) {
            List<Flight> flightList = business.getSolution().getFlightList();

            long kbraInitStart = System.currentTimeMillis();
            business.setExternalInitialStartTimeMs(kbraInitStart);

            List<Pairing> randomPairings =
                    RandomPairingGenerator.generate(
                            flightList,
                            4,
                            42L
                    );


            business.getSolution().setPairingList(randomPairings);

            System.out.println("[KBRA] Random initial pairing injected.");
        }


        if ("dqn".equals(mode)) {
            long xlsxReadStart = System.currentTimeMillis();
            business.setExternalInitialStartTimeMs(xlsxReadStart);

            FlightCrewPairingXlsxFileIO xlsxFileIO = new FlightCrewPairingXlsxFileIO();
            List<Flight> flightList = business.getSolution().getFlightList();

            final String initXlsx = pairingXlsxFile;

            List<Pairing> pairingList =
                xlsxFileIO.readPairingList(
                    flightList,
                    business.getOutputFileList()
                        .stream()
                        .filter(f -> f.getName().equals(initXlsx))
                        .findFirst()
                        .orElseThrow(() ->
                            new IllegalArgumentException("pairing xlsx not found"))
                );

            business.getSolution().setPairingList(pairingList);
            System.out.println("[DQN] XLSX initial pairing injected.");
        }

        // solver 실행
        business.solve(business.getSolution());

        // Solution 출력
        PairingSolution solution = business.getSolution();

        solution.calculateMandays();

        // Check score detail
        SolutionManager<PairingSolution, HardSoftLongScore> scoreManager = SolutionManager.create(business.getSolverFactory());
        ScoreExplanation<PairingSolution, HardSoftLongScore> explain = scoreManager.explain(solution);
        Map<String, ConstraintMatchTotal<HardSoftLongScore>> constraintMatchTotalMap = explain.getConstraintMatchTotalMap();
        ViewAllConstraint.viewAll(constraintMatchTotalMap, solution);
        //ViewAllConstraint.pairingScore(explain);

        // Output Excel File
        System.out.println("save...");
        business.saveSolution(null);
        System.out.println("done");

        System.exit(0);
    }

    public PairingApp(String dataDirPath, String dataDirName, String solverConfigResource, String informationFileName) {
        super("CrewPairing",
                "Airline Scheduling Crew Pairing",
                solverConfigResource,
                dataDirPath,
                dataDirName,
                informationFileName);
    }

    @Override
    public SolutionFileIO<PairingSolution> createSolutionFileIO() {
        return new FlightCrewPairingXlsxFileIO();
    }
}