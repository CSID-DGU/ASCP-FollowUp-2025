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
        
        if (args.length < 6) {
            throw new IllegalArgumentException(
                "Usage: java -jar crew-pairing.jar " +
                "<dataDirPath> <dataDirName> <flightSize> <input.xlsx> " +
                "<opis|kbra|dqn> [pairing.xlsx] <iteration>"
            );
        }

        String dataDirPath = args[0];
        String dataDirName = args[1];
        Integer flightSize = Integer.valueOf(args[2]);
        String informationXlsxFile = args[3];

        String mode = args[4];   // opis | kbra | dqn
        String pairingXlsxFile = null;
        int stepLimit;

        switch (mode) {
            case "opis":
                stepLimit = Integer.parseInt(args[5]);
                break;

            case "kbra":
                stepLimit = Integer.parseInt(args[5]);
                break;

            case "dqn":
                pairingXlsxFile = args[5];
                stepLimit = Integer.parseInt(args[6]);
                break;

            default:
                throw new IllegalArgumentException(
                    "mode must be one of: opis | kbra | dqn"
                );
        }

        System.out.println("================================");
        System.out.println("Mode       = " + mode);
        System.out.println("Iteration  = " + stepLimit);
        if (pairingXlsxFile != null) {
            System.out.println("Init file  = " + pairingXlsxFile);
        }
        System.out.println("================================");


        assert flightSize != null;
        SolutionBusiness<PairingSolution, ?> business = new PairingApp(dataDirPath, dataDirName, informationXlsxFile)
                .init(flightSize, stepLimit).getSolutionBusiness();

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



        //[추가] 시작 시간 기록 
        long startTime = System.currentTimeMillis();

        // Solve By SolverJob
        business.solve(business.getSolution());

        //[추가] 종료 시간 기록 및 출력
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // Solution 출력
        PairingSolution solution = business.getSolution();
        System.out.println(solution);

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

        //[추가] 결과 출력
        System.out.println("\n" + "=".repeat(40));
        System.out.println("Performance result");
        System.out.println("total solve time (): " + totalTime + " ms");
        System.out.println("(* Note: Divide this by the iteration limit ");
        System.out.println("  to get the average time per iteration)");
        System.out.println("=".repeat(40));

        System.exit(0);
    }

    public PairingApp(String dataDirPath, String dataDirName, String informationFileName) {
        super("CrewPairing",
                "Airline Scheduling Crew Pairing",
                SOLVER_CONFIG,
                dataDirPath,
                dataDirName,
                informationFileName);
    }

    @Override
    public SolutionFileIO<PairingSolution> createSolutionFileIO() {
        return new FlightCrewPairingXlsxFileIO();
    }
}
