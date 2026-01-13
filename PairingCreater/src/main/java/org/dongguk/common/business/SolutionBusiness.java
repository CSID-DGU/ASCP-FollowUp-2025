package org.dongguk.common.business;

import lombok.Getter;
import lombok.Setter;
import org.dongguk.common.app.CommonApp;
import org.optaplanner.core.api.score.Score;
import org.optaplanner.core.api.solver.*;
import org.optaplanner.core.api.solver.change.ProblemChange;
import org.optaplanner.core.impl.score.director.InnerScoreDirector;
import org.optaplanner.core.impl.solver.DefaultSolverFactory;
import org.optaplanner.core.impl.solver.change.DefaultProblemChangeDirector;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Getter
@Setter
public final class SolutionBusiness<Solution_, Score_ extends Score<Score_>> implements AutoCloseable {
    public static String getBaseFileName(File file) {
        return getBaseFileName(file.getName());
    }

    public static String getBaseFileName(String name) {
        int indexOfLastDot = name.lastIndexOf('.');
        if (indexOfLastDot > 0) {
            return name.substring(0, indexOfLastDot);
        } else {
            return name;
        }
    }

    private static final Comparator<File> FILE_COMPARATOR = new ProblemFileComparator();

    private static final Logger LOGGER = LoggerFactory.getLogger(SolutionBusiness.class);

    private final CommonApp<Solution_> app;
    private final DefaultSolverFactory<Solution_> solverFactory;
    private final SolverManager<Solution_, Long> solverManager;
    private final SolutionManager<Solution_, Score_> solutionManager;

    /**
     * 멀티 쓰레드 환경에서 동시성 보장을 위해 AtomicReference 사용하지 않음 -
     * 왜냐하면 자바 -> 파이썬 -> 자바 -> ... 순으로 계속 돌린다. 따라서
     * 여러 개의 최적화 문제를 푸는 것이 아닌 하나의 최적화 문제만 풀 것이므로 동시성 문제를 생각할 필요가 없음
     * 하지만 사용해본다!
     */
    private final AtomicReference<SolverJob<Solution_, Long>> solverJobRef = new AtomicReference<>();
    private final AtomicReference<Solution_> workingSolutionRef = new AtomicReference<>();

    // 우리는 옵션들을 Import할 필요가 없으므로 필요 없음
    // private Set<AbstractSolutionImporter<Solution_>> importers;
    // private Set<AbstractSolutionExporter<Solution_>> exporters;

    private File dataDir;
    private SolutionFileIO<Solution_> solutionFileIO;
    private File inputDataDir;
    private File outputDataDir;

    private long methodStartTime;  // 초기해 생성 시작 시점
    private long solveStartTime;   // SolverJob이 시작된 시점
    private long initSolutionTime; // 초기해 생성 종료 시점 (첫 Best Score 계산 시점)
    private long bestScoreTime;    // 베스트 스코어 도출 시점
    private int bestScoreStep;     // 베스트 스코어 도출 Iteration
    private int currentStepCount;  // 전체 진행 Iteration 카운트

    public SolutionBusiness(CommonApp<Solution_> app, SolverFactory<Solution_> solverFactory) {
        this.app = app;
        this.solverFactory = (DefaultSolverFactory<Solution_>) solverFactory;
        this.solverManager = SolverManager.create(solverFactory);
        this.solutionManager = SolutionManager.create(solverFactory);
    }

    //pairingApp에서 시작 시점을 주입하기 위한 메소드
    public void setMethodStartTime(long startTime){
        this.methodStartTime = startTime;
    }

    public void updateDataDirs() {
        this.inputDataDir = new File(dataDir, "input");
        if (!inputDataDir.exists()) {
            throw new IllegalStateException(String.format("해당 Path [%s]에 Input Data Directory는 존재하지 않습니다.", inputDataDir.getAbsolutePath()));
        }

        this.outputDataDir = new File(dataDir, "output");
        if (!outputDataDir.exists()) {
            throw new IllegalStateException(String.format("해당 Path [%s]에 Output Data Directory는 존재하지 않습니다.", outputDataDir.getAbsolutePath()));
        }
    }

    public List<File> getInputFileList() {
        return getFileList(inputDataDir, solutionFileIO.getInputFileExtension());
    }

    public List<File> getOutputFileList() {
        return getFileList(outputDataDir, solutionFileIO.getInputFileExtension());
    }

    // 동시성 보장을 윈한 Atomic 객체를 사용했으므로 따르게 get, set 만들기
    public Solution_ getSolution() {
        return workingSolutionRef.get();
    }

    public void setSolution(Solution_ solution) {
        workingSolutionRef.set(solution);
    }

    public Score_ getScore() {
        return solutionManager.update(getSolution());
    }

    public boolean isSolving() {
        SolverJob<Solution_, Long> solverJob = solverJobRef.get();
        return solverJob != null && solverJob.getSolverStatus() == SolverStatus.SOLVING_ACTIVE;
    }

    public boolean isConstraintMatchEnabled() {
        return applyScoreDirector(InnerScoreDirector::isConstraintMatchEnabled);
    }

    // 해당 폴더에 어떤 File이 있는 불러오는 메소드
    private static List<File> getFileList(File dataDir, String extension) {
        try (Stream<Path> paths = Files.walk(dataDir.toPath(), FileVisitOption.FOLLOW_LINKS)) {
            return paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith("." + extension))
                    .map(Path::toFile)
                    .sorted(FILE_COMPARATOR)
                    .collect(toList());
        } catch (IOException e) {
            throw new IllegalStateException("Error while crawling data directory (" + dataDir + ").", e);
        }
    }

    public void doProblemChange(ProblemChange<Solution_> problemChange) {
        SolverJob<Solution_, Long> solverJob = solverJobRef.get();
        if (solverJob != null) {
            solverJob.addProblemChange(problemChange);
        } else {
            acceptScoreDirector(scoreDirector -> {
                DefaultProblemChangeDirector<Solution_> problemChangeDirector =
                        new DefaultProblemChangeDirector<>(scoreDirector);
                problemChangeDirector.doProblemChange(problemChange);
            });
        }
    }

    public Solution_ solve(Solution_ problem) {

        // 만약 pairingApp에서 시작 시점 안찍어줬으면 현재 시간으로 세팅
        if (this.methodStartTime == 0) this.methodStartTime = System.currentTimeMillis();

        solveStartTime = System.currentTimeMillis();
        initSolutionTime = 0;
        bestScoreTime = 0;
        bestScoreStep = 0;
        currentStepCount = 0;
        
        // 베스트 스코어 비교를 위한 내부 객체
        AtomicReference<Score_> lastBestScore = new AtomicReference<>(null);

        System.out.println("[SOLVER START]");

        SolverJob<Solution_, Long> solverJob = solverManager.solveAndListen(
                1L,
                id -> problem, 
                bestSolution -> {
                    long now = System.currentTimeMillis();
                    Score_ currentScore = solutionManager.update(bestSolution);
                    
                    // 1. 첫 번째 best solution 리스너 호출 시점을 초기해 종료 시점으로 기록
                    if (initSolutionTime == 0) {
                        initSolutionTime = now;
                    }

                    // 2. 베스트 스코어 갱신 트래킹
                    if (lastBestScore.get() == null || currentScore.compareTo(lastBestScore.get()) > 0) {
                        lastBestScore.set(currentScore);
                        bestScoreTime = now - methodStartTime; // 전체 흐름 기준 상대 시간
                        bestScoreStep = currentStepCount;
                    }
                    
                    // 진행 로그 출력
                    System.out.printf("[PROGRESS] %d,%s (Step: %d)%n", (now - methodStartTime), currentScore, currentStepCount);
                    
                    this.setSolution(bestSolution);
                    currentStepCount++; // Iteration 카운트 증가
                }
        );

        solverJobRef.set(solverJob);

        try {
            Solution_ finalSolution = solverJob.getFinalBestSolution();
            long solverEndPoint = System.currentTimeMillis();
            Score_ finalScore = solutionManager.update(finalSolution);
            
            // 계산 결과 리포트
            System.out.println("\n" + "=".repeat(60));
            System.out.println("            [ EXPERIMENT RESULT SUMMARY ]");
            System.out.println("=".repeat(60));
            System.out.println(" 1. Initial Solution Start (startPoint) : " + methodStartTime); //초기해 생성 시작 시점
            System.out.println(" 2. Initial Solution End (initEndPoint)  : " + initSolutionTime); //초기해 생성 종료 시점
            System.out.println(" 3. Initialization Time (initTime)       : " + (initSolutionTime - methodStartTime) + " ms"); //초기해 생성 시간
            System.out.println(" 4. Pure Solver Start (solverStart)      : " + initSolutionTime); //순수 solver 시작 시점
            System.out.println(" 5. Pure Solver Time (pureSolverTime)    : " + (solverEndPoint - initSolutionTime) + " ms"); //순수 solver 시간
            System.out.println(" 6. Total Solve Time (totalTime)         : " + (solverEndPoint - methodStartTime) + " ms"); //전체 solve 시간 (total time)
            System.out.println(" 7. Best Score Found At (relative)       : " + bestScoreTime + " ms"); //베스트 스코어 도출 시점
            System.out.println(" 8. Best Score Found At Step             : " + bestScoreStep); //베스트 스코어 도출 step
            System.out.println(" 9. Final Best Score                     : " + finalScore); //최종 베스트 스코어
            System.out.println("=".repeat(60) + "\n");
            
            return finalSolution;
            } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Solver thread was interrupted.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Solver failed.", e);
        } finally {
            solverJobRef.set(null); // Don't keep references to jobs that have finished solving.
        }
    }

    public void terminateSolvingEarly() {
        SolverJob<Solution_, Long> solverJob = solverJobRef.get();
        if (solverJob != null) {
            solverJob.terminateEarly();
        }
    }

    @Override
    public void close() {
        terminateSolvingEarly();
        solverManager.close();
    }

    /**
     * OptaPlanner는 Score Director Pattern이라는 패턴을 사용하여 최적화 문제를 해결하는데, InnerScoreDirector는 이 패턴의 핵심 역할을 담당합니다.
     * Score Director Pattern은 최적화 문제의 해결과정에서 현재 상태의 점수(Score)를 계산하고, 이 점수를 최적화하려는 방향으로 개선하는 작업을 수행하는 패턴입니다.
     * InnerScoreDirector는 이러한 Score Director Pattern의 내부적인 구현을 담당하며, 다음과 같은 역할을 합니다:
     * Score 계산: 최적화 문제의 현재 상태에 대한 점수를 계산합니다. 최적화 문제의 해법(Solution)과 해당 문제에 맞는 적합도 평가 기준(Score 객체)에 따라 Score를 계산합니다.
     * Score 변경: Score Director Pattern은 현재 해법을 변경하여 점수를 개선하는 방향으로 진행합니다.
     * InnerScoreDirector는 이러한 해법의 변경 작업을 수행하고, 변경된 해법의 점수를 다시 계산합니다.
     * 최적화 알고리즘과의 연동: InnerScoreDirector는 최적화 알고리즘과 상호 작용하여 최적화 문제를 해결합니다.
     * 알고리즘의 결정과정에 따라 다양한 해법을 탐색하고 개선하는데 사용됩니다.
     * 다중 스레드 환경에서의 동기화: OptaPlanner는 멀티 스레드 환경에서 동시에 최적화 알고리즘을 실행하는데, 이때 InnerScoreDirector는 다중 스레드 환경에서 안전하게 동작하도록 동기화 작업을 수행합니다.
     * InnerScoreDirector는 내부적으로 최적화 문제의 상태를 변경하고 Score를 계산하여 문제를 점진적으로 개선하는 중요한 역할을 수행합니다.
     * 이를 통해 OptaPlanner는 다양한 알고리즘과 휴리스틱을 사용하여 최적 또는 근사적인 해답을 찾습니다.
     */
    private <Result_> Result_ applyScoreDirector(Function<InnerScoreDirector<Solution_, Score_>, Result_> function) {
        try (InnerScoreDirector<Solution_, Score_> scoreDirector = (InnerScoreDirector<Solution_, Score_>) solverFactory.getScoreDirectorFactory()
                .buildScoreDirector(true,
                        true)) {
            scoreDirector.setWorkingSolution(getSolution());
            Result_ result = function.apply(scoreDirector);
            scoreDirector.triggerVariableListeners();
            scoreDirector.calculateScore();
            setSolution(scoreDirector.getWorkingSolution());
            return result;
        }
    }

    public void openSolution(File informationFile) {
        Solution_ solution = solutionFileIO.read(informationFile);
        workingSolutionRef.set(solution);
    }


    public void saveSolution(File file) {
        solutionFileIO.write(getSolution(), file);
        LOGGER.info("Saved: CSV File");
    }

    private void acceptScoreDirector(Consumer<InnerScoreDirector<Solution_, Score_>> consumer) {
        applyScoreDirector(s -> {
            consumer.accept(s);
            return null;
        });
    }
}
