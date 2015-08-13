// Copyright (c) 2015 K Team. All Rights Reserved.
package org.kframework.debugger;


import org.kframework.Rewriter;
import org.kframework.RewriterResult;
import org.kframework.definition.Rule;
import org.kframework.kore.K;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Kore Based Debugger Implementation.
 */
public class KoreKDebug implements KDebug {

    private final int DEFAULT_ID = 0;
    private List<DebuggerState> stateList;
    private int activeStateIndex;
    private Rewriter rewriter;
    private int checkpointInterval;

    /**
     * Start a Debugger Session. The initial Configuration becomes a part of the new and only state of the Debugger
     *
     * @param initialK The initial Configuration.
     * @param rewriter The Rewriter being used.
     */
    public KoreKDebug(K initialK, Rewriter rewriter, int checkpointInterval) {
        this.stateList = new ArrayList<>();
        this.rewriter = rewriter;
        this.checkpointInterval = checkpointInterval;
        NavigableMap<Integer, RewriterCheckpoint> checkpointMap = new TreeMap<>();
        checkpointMap.put(DEFAULT_ID, new RewriterCheckpoint(initialK));
        DebuggerState initialState = new DebuggerState(initialK, DEFAULT_ID, checkpointMap, false);
        stateList.add(initialState);
        activeStateIndex = DEFAULT_ID;
    }

    @Override
    public void setCheckpointInterval(int checkpointInterval) {
        this.checkpointInterval = checkpointInterval;
    }

    @Override
    public DebuggerState step(int currentStateIndex, int steps) {
        DebuggerState currentState = stateList.get(currentStateIndex);
        stateList.remove(currentStateIndex);
        K currentK = currentState.getCurrentK();
        int activeStateCheckpoint = currentState.getStepNum();
        RewriterResult result;
        NavigableMap<Integer, RewriterCheckpoint> checkpointMap = currentState.getCheckpointMap();
        while (steps > checkpointInterval) {
            result = rewriter.execute(currentK, Optional.of(checkpointInterval));
            if (isLeafState(checkpointInterval, result)) {
                return getDebuggerState(currentStateIndex, activeStateCheckpoint, result, checkpointMap);
            }
            steps -= checkpointInterval;
            activeStateCheckpoint += checkpointInterval;
            checkpointMap.put(activeStateCheckpoint, new RewriterCheckpoint(result.k()));
            currentK = result.k();
        }
        result = rewriter.execute(currentK, Optional.of(steps));
        if (isLeafState(steps, result)) {
            return getDebuggerState(currentStateIndex, activeStateCheckpoint, result, checkpointMap);
        }
        activeStateCheckpoint += steps;
        DebuggerState nextState = new DebuggerState(result.k(), activeStateCheckpoint, checkpointMap, false);
        stateList.add(nextState);
        return nextState;
    }

    private DebuggerState getDebuggerState(int currentStateIndex, int activeStateCheckpoint, RewriterResult result, NavigableMap<Integer, RewriterCheckpoint> checkpointMap) {
        activeStateCheckpoint += result.rewriteSteps().get();
        DebuggerState nextState = new DebuggerState(result.k(), activeStateCheckpoint, checkpointMap, true);
        stateList.add(currentStateIndex, nextState);
        return nextState;
    }

    private boolean isLeafState(int steps, RewriterResult result) {
        return result.rewriteSteps().isPresent() && result.rewriteSteps().get() < steps;
    }

    @Override
    public DebuggerState backStep(int initialStateNum, int steps) {
        DebuggerState currentState = stateList.get(initialStateNum);
        int currentCheckpoint = currentState.getStepNum();
        int target = currentCheckpoint - steps;
        NavigableMap<Integer, RewriterCheckpoint> currMap = currentState.getCheckpointMap();
        Map.Entry<Integer, RewriterCheckpoint> relevantEntry = currMap.floorEntry(target);
        if (relevantEntry == null) {
            /* Invalid Operation, no need to change the state */
            return null;
        }

        int floorKey = relevantEntry.getKey();
        currentState = new DebuggerState(relevantEntry.getValue().getCheckpointK(), floorKey, new TreeMap<>(currMap.headMap(floorKey, true)), false);
        stateList.remove(initialStateNum);
        stateList.add(initialStateNum, currentState);
        return step(initialStateNum, target - floorKey);
    }

    @Override
    public DebuggerState jumpTo(int initialStateNum, int configurationNum) {
        DebuggerState currentState = stateList.get(initialStateNum);
        NavigableMap<Integer, RewriterCheckpoint> checkpointMap = currentState.getCheckpointMap();
        int firstKey = checkpointMap.firstKey();
        if (configurationNum < firstKey) {
            return null;
        }
        int lastKey = currentState.getStepNum();
        stateList.remove(initialStateNum);
        stateList.add(currentState);
        if (configurationNum >= lastKey) {
            return step(initialStateNum, configurationNum - lastKey);
        }
        return backStep(initialStateNum, lastKey - configurationNum);
    }

    @Override
    public List<? extends Map<? extends K, ? extends K>> search(Optional<Integer> startStateId, Rule searchPattern, Optional<Integer> depth, Optional<Integer> bounds) {
        if (startStateId.isPresent()) {
            jumpTo(activeStateIndex, startStateId.get());
        }
        return rewriter.search(stateList.get(activeStateIndex).getCurrentK(), depth, bounds, searchPattern);
    }

    @Override
    public DebuggerState resume() {
        DebuggerState activeState = stateList.get(activeStateIndex);
        do {
            step(activeStateIndex, checkpointInterval);
        } while (!isFinalResult(activeState.getCurrentK()));
        return activeState;
    }

    @Override
    public List<DebuggerState> getStates() {
        return new ArrayList<>(stateList);
    }

    private boolean isFinalResult(K currK) {
        return currK.equals(rewriter.execute(currK, Optional.of(1)).k());
    }

    @Override
    public int setState(int stateNum, Optional<Integer> configurationNum) {
        DebuggerState newActiveState = stateList.get(stateNum);
        if (newActiveState == null) {
            return activeStateIndex;
        }
        activeStateIndex = stateNum;
        configurationNum.ifPresent(configNum -> jumpTo(stateNum, configNum));
        return stateNum;
    }

    @Override
    public int getActiveStateId() {
        return activeStateIndex;
    }

    @Override
    public DebuggerState createCopy(int stateNum) {
        DebuggerState newState = new DebuggerState(stateList.get(stateNum));
        stateList.add(newState);
        return newState;
    }

    @Override
    public DebuggerState peek(Optional<Integer> stateNum, Optional<Integer> configurationNum) {
        int concreteStateNum = stateNum.orElse(activeStateIndex);
        int concreteConfigurationNum = configurationNum.orElse(stateList.get(activeStateIndex).getStepNum());
        DebuggerState temp = stateList.get(concreteStateNum);
        DebuggerState peekState = jumpTo(concreteStateNum, concreteConfigurationNum);
        stateList.remove(concreteStateNum);
        stateList.add(concreteStateNum, temp);
        return peekState;
    }
}
