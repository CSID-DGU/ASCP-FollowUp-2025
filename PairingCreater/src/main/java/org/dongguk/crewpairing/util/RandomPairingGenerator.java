package org.dongguk.crewpairing.util;

import org.dongguk.crewpairing.domain.Flight;
import org.dongguk.crewpairing.domain.Pairing;

import java.util.*;
import java.util.stream.Collectors;

public class RandomPairingGenerator {

    public static List<Pairing> generate(List<Flight> flightList, int maxPairingSize, long seed) {
        Random random = new Random(seed);
        
        // 1. 모든 비행을 시간순으로 정렬
        List<Flight> unassigned = flightList.stream()
                .sorted(Comparator.comparing(Flight::getOriginTime))
                .collect(Collectors.toCollection(ArrayList::new));

        List<Pairing> pairingList = new ArrayList<>();
        long idCounter = 1L;


        while (!unassigned.isEmpty()) {
            Flight seedFlight = unassigned.get(0);
            unassigned.remove(seedFlight);
            
            Pairing pairing = new Pairing(idCounter++, new ArrayList<>(), 0);
            pairing.getPair().add(seedFlight);

            // 처음 출발 공항을 기억함
            String homeBase = seedFlight.getOriginAirport().getName();

            while (pairing.getPair().size() < maxPairingSize) {
                Flight last = pairing.getPair().get(pairing.getPair().size() - 1);

                // 이미 집으로 돌아왔다면 더 이상 붙이지 않음 (DQN: V_p[4] == V_p[3])
                if (last.getDestAirport().getName().equals(homeBase)) break;

                List<Flight> candidates = unassigned.stream()
                        .filter(f -> last.getDestAirport().getName().equals(f.getOriginAirport().getName()) 
                                && !last.getDestTime().isAfter(f.getOriginTime()))
                        .filter(f -> isFeasible(pairing, f)) 
                        .collect(Collectors.toList());

                if (candidates.isEmpty()) break;

                Flight next = candidates.get(random.nextInt(candidates.size()));
                pairing.getPair().add(next);
                unassigned.remove(next);
            }
            pairingList.add(pairing);
        }
                
        System.out.println(">>> [KBRA DEBUG] Generated " + pairingList.size() + " pairings.");
        return pairingList;
    }

    private static boolean isFeasible(Pairing pairing, Flight f) {
        pairing.getPair().add(f);
        
        // Pairing.java에 정의된 하드 제약들을 모두 통과해야 함
        // 하나라도 true(위반)이면 false 반환
        boolean isPossible = 
               !pairing.isImpossibleTime()       
            && !pairing.isImpossibleAirport()    
            && !pairing.isDifferentAircraft()    
            && !pairing.isImpossibleContinuity() 
            && !pairing.isLenghtPossible();      
        
        // 체크를 위해 넣었던 비행을 다시 제거
        pairing.getPair().remove(pairing.getPair().size() - 1);
        return isPossible;
    }
}