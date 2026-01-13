package org.dongguk.common.app;

import lombok.Getter;
import lombok.Setter;
import org.dongguk.common.business.SolutionBusiness;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig; // 추가
import org.optaplanner.core.config.phase.PhaseConfig; // 추가
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

import java.awt.*;
import java.io.File;
import java.util.Scanner;
import java.util.List;

@Getter
@Setter
public abstract class CommonApp<Solution_> extends LoggingMain {
    public static final String DATA_DIR_SYSTEM_PROPERTY = "org.dongguk.dataDir";

    protected final String name;
    protected final String description;
    protected final String solverConfigResource;
    protected final String dataDirPath;
    protected final String dataDirName;
    protected final String informationFileName;

    protected SolutionBusiness<Solution_, ?> solutionBusiness;
    protected SolverConfig solverConfig;

    public File determineDataDir() {
        File dataDir = new File(dataDirPath, dataDirName);
        if (!dataDir.exists()) {
            throw new IllegalStateException(String.format("해당 Path [%s]에 Data Directory는 존재하지 않습니다", dataDir.getAbsolutePath()));
        }
        return dataDir;
    }

    protected CommonApp(String name, String description, String solverConfigResource,
                        String dataDirPath, String dataDirName, String informationFileName) {
        this.name = name;
        this.description = description;
        this.solverConfigResource = solverConfigResource;
        this.dataDirPath = dataDirPath;
        this.dataDirName = dataDirName;
        this.informationFileName = informationFileName;
    }

    public CommonApp<Solution_> init(
            Integer flightSize,
            int stepLimit,
            long timeLimitMs
    ) {
        init(null, true, flightSize, stepLimit, timeLimitMs);
        return this;
    }


    public void init(Component centerForComponent,
                    boolean exitOnClose,
                    Integer flightSize,
                    int stepLimit,
                    long timeLimitMs) {
        solutionBusiness = createSolutionBusiness(flightSize, stepLimit, timeLimitMs);
    }


    private SolutionBusiness<Solution_, ?> createSolutionBusiness(
            Integer flightSize,
            int stepLimit,
            long timeLimitMs
    ){
        // Scanner scanner = new Scanner(System.in);
        // System.out.print("Enter the iteration limit: ");
        // int stepLimit = scanner.nextInt();

        SolverConfig solverConfig = SolverConfig.createFromXmlResource(solverConfigResource);
        solverConfig.withMoveThreadCount("1");

        List<PhaseConfig> phaseConfigList = solverConfig.getPhaseConfigList();
        if (phaseConfigList != null) {
            for (PhaseConfig phaseConfig : phaseConfigList) {
                if (phaseConfig instanceof LocalSearchPhaseConfig) {
                    LocalSearchPhaseConfig lsConfig = (LocalSearchPhaseConfig) phaseConfig;
                    
                    TerminationConfig terminationConfig = new TerminationConfig()
                            .withStepCountLimit(stepLimit)
                            .withMillisecondsSpentLimit(timeLimitMs);

                    lsConfig.setTerminationConfig(terminationConfig);

                }
            }
        }

        SolutionBusiness<Solution_, ?> solutionBusiness = new SolutionBusiness<>(this,
                SolverFactory.create(solverConfig));
        solutionBusiness.setDataDir(determineDataDir());
        solutionBusiness.setSolutionFileIO(createSolutionFileIO());
        solutionBusiness.updateDataDirs();
        return solutionBusiness;
    }

    public abstract SolutionFileIO<Solution_> createSolutionFileIO();
}