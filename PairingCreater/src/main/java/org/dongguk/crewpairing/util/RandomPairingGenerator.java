package org.dongguk.crewpairing.util;

import org.dongguk.crewpairing.domain.Flight;
import org.dongguk.crewpairing.domain.Pairing;

import java.util.*;
import java.util.stream.Collectors;

public class RandomPairingGenerator {

    public static List<Pairing> generate(
            List<Flight> flightList,
            int maxPairingSize,
            long seed
    ) {
        Random random = new Random(seed);

        // 시간 기준 정렬
        List<Flight> unassigned = flightList.stream()
                .sorted(Comparator.comparing(Flight::getOriginTime))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Pairing> pairingList = new ArrayList<>();
        long idCounter = 1L;

        while (!unassigned.isEmpty()) {

            Flight seedFlight = unassigned.remove(0);
            Pairing pairing = new Pairing(idCounter++);

            List<Flight> pair = new ArrayList<>();
            pair.add(seedFlight);

            while (pair.size() < maxPairingSize) {
                Flight last = pair.get(pair.size() - 1);

                List<Flight> candidates = unassigned.stream()
                        .filter(f ->
                                last.getDestAirport().equals(f.getOriginAirport())
                                && !last.getDestTime().isAfter(f.getOriginTime())
                        )
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) {
                    break;
                }

                boolean added = false;

                Collections.shuffle(candidates, random);
                for (Flight next : candidates) {

                    pair.add(next);
                    pairing.setPair(pair);

                    // Hard 제약 검사
                    if (
                            pairing.isImpossibleTime()
                            || pairing.isImpossibleAirport()
                            || pairing.isImpossibleContinuity()
                            || pairing.isLenghtPossible()
                            || pairing.isNotDepartBaseMoreThanTwo()
                    ) {
                        // 만족하지 않으면 되돌림
                        pair.remove(pair.size() - 1);
                        pairing.setPair(pair);
                        continue;
                    }

                    // 만족하면 확정
                    unassigned.remove(next);
                    added = true;
                    break;
                }

                // 더 이상 붙일 수 없으면 종료
                if (!added) {
                    break;
                }
            }

            pairing.setPair(pair);
            pairingList.add(pairing);
        }

        return pairingList;
    }
}
