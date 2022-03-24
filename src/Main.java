import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * This program reads a 𝛿-table (NFA-λ), generates a t-table, DFA, inequivalences table/triangle and Minimized DFA and
 * outputs all four along with graphical views of DFA and Minimized DFA. It also repeatedly asks user for strings,
 * prints their computations along with whether they are accepted or rejected.
 *
 * @author Salman Khan
 */
public class Main {
    private static String[] sigma;

    private static Set<State> deltaStates;
    private static State deltaStartState;

    private static Set<State> tStates;

    private static Set<State> dFAStates;
    private static State dFAStartState;

    private static boolean[][] distinguishable;
    private static State[] xAxisStates;
    private static State[] yAxisStates;

    private static Set<State> minDFAStates;
    private static State minDFAStartState;

    public static void main(String[] args) {
        try {
            Scanner kbd = new Scanner(System.in);

            System.out.print("Enter path of a file containing NFA-λ info: ");
            String response = kbd.nextLine();

            System.out.println("Part I - NFA-λ to DFA");
            readTable(response);

            generateTTable();
            outputTable("TTable.tsv", tStates, getState(tStates, deltaStartState.name));
            System.out.println("'TTable.tsv' has been created.");

            generateDFA();
            outputTable("DFA.tsv", dFAStates, dFAStartState);
            System.out.println("'DFA.tsv' has been created.");

            outputGraphicalDFA("GraphicalDFA.txt", dFAStates, dFAStartState);
            System.out.println("'GraphicalDFA.txt' has been created.");

            System.out.println("\nPart II - DFA Minimization");
            renameDFAStates();
            generateDistinguishable();
            outputDistinguishable("Inequivalences.tsv");
            System.out.println("'Inequivalences.tsv' has been created.");

            generateMinDFA();
            outputTable("MinimizedDFA.tsv", minDFAStates, minDFAStartState);
            System.out.println("'MinimizedDFA.tsv' has been created.");

            outputGraphicalDFA("GraphicalMinimizedDFA.txt", minDFAStates, minDFAStartState);
            System.out.println("'GraphicalMinimizedDFA.txt' has been created.");

            System.out.println("\nPart III - String Computation");
            while (true) {
                System.out.print("Enter a string or -1 to end: ");
                response = kbd.nextLine();

                if (response.equals("-1")) break;

                checkStringAcceptance(response);

                System.out.print("\n");
            }
        } catch (Exception e) {
            System.out.println("\nAn error occurred.");
        }
    }

    /**
     * Reads a 𝛿-table (NFA-λ) and stores it into 'deltaStates' and 'deltaStartState'
     * @param inFilePath path of the .tsv that contains info about the NFA-λ
     * @throws FileNotFoundException thrown if file does not exist
     */
    private static void readTable(String inFilePath) throws FileNotFoundException {
        Scanner inFile = new Scanner(new File(inFilePath));
        String[] tokens = inFile.nextLine().split("\t");
        sigma = new String[tokens.length - 3];
        System.arraycopy(tokens, 1, sigma, 0, sigma.length);

        deltaStates = new HashSet<>();
        deltaStartState = null;

        while (inFile.hasNext()) {
            tokens = inFile.nextLine().split("\t");

            for (int i = 0; i < tokens.length - 1; i++) {
                tokens[i] = tokens[i].replace("{", "");
                tokens[i] = tokens[i].replace("}", "");
            }

            State thisState = getState(deltaStates, tokens[0]);

            if (thisState == null) {
                thisState = new State(tokens[0]);
                deltaStates.add(thisState);
                if (deltaStartState == null) deltaStartState = thisState;
            }

            thisState.accepting = tokens[tokens.length - 1].equals("1");

            String[] stringArcsTo;

            for (int i = 0; i < sigma.length + 1; i++) {
                stringArcsTo = tokens[i + 1].split(",");
                if (stringArcsTo[0].equals("")) continue;

                Set<State> stateArcsTo = new HashSet<>();

                for (int j = 0; j < stringArcsTo.length; j++) {
                    State state = getState(deltaStates, stringArcsTo[j]);

                    if (state == null) {
                        deltaStates.add(new State(stringArcsTo[j]));
                        state = getState(deltaStates, stringArcsTo[j]);
                    }

                    stateArcsTo.add(state);
                }

                if (i != sigma.length) {
                    thisState.outArcs.put(sigma[i], stateArcsTo);
                } else {
                    thisState.outArcs.put("L", stateArcsTo);
                }
            }
        }

        inFile.close();
    }

    /**
     * Generates and stores t-table into 'tStates' from 'deltaStates'
     */
    private static void generateTTable() {
        tStates = new HashSet<>();

        for (State thisState : deltaStates) tStates.add(new State(thisState.name, thisState.accepting));

        State tState;
        Set<State> lClosureBeforeChar;

        for (State deltaState : deltaStates) {
            tState = getState(tStates, deltaState.name);

            lClosureBeforeChar = getLClosure(deltaState, new HashSet<>());

            for (int i = 0; i < sigma.length; i++) {
                Set<State> outArcs = new HashSet<>();

                for (State stateI : lClosureBeforeChar) {
                    if (stateI.outArcs.get(sigma[i]) == null) continue;

                    Set<State> statesThroughChar = new HashSet<>(stateI.outArcs.get(sigma[i]));

                    Set<State> lClosureAfterChar = new HashSet<>();
                    for (State stateJ : statesThroughChar)
                        lClosureAfterChar.addAll(getLClosure(stateJ, new HashSet<>()));

                    for (State stateJ : lClosureAfterChar) outArcs.add(getState(tStates, stateJ.name));
                }

                if (outArcs.size() > 0) tState.outArcs.put(sigma[i], outArcs);
            }
        }
    }

    /**
     * Generates and stores DFA into 'dFAStates' and 'dFAStartState' from 'tStates'
     */
    private static void generateDFA() { // Algorithm 5.6.3 - Page 172

        // 1. initialize Q' to λ-Closure(q0)
        Set<State> deltaStStateLClosure = getLClosure(deltaStartState, new HashSet<>());
        dFAStartState = new State(stateNamesToString(deltaStStateLClosure));

        // accepting state contains an element qi ∈ F
        for (State thisState : deltaStStateLClosure) {
            if (thisState.accepting) {
                dFAStartState.accepting = true;
                break;
            }
        }

        dFAStates = new HashSet<>();
        dFAStates.add(dFAStartState);

        // 2. repeat
        while (true) {

            //  2.1. if there is a node X ∈ Q' and a symbol a ∈ ∑ with no arc leaving X labeled a, then
            Set<State> incompleteStates = getIncompleteDFAStates();

            if (incompleteStates.size() == 0) break; //  else done := true

            for (State stateI : incompleteStates) { // StateI = X
                String[] nameParts = stateI.name.split(",");

                for (int i = 0; i < sigma.length; i++) { // i = a

                    //      2.1.1. let Y = U_(qj ∈ X) t(qi, a)
                    Set<State> destStates = new HashSet<>(); // destStates = Y

                    for (int j = 0; j < nameParts.length && !nameParts[0].equals("TrapState"); j++) {
                        Set<State> SetJ = getState(tStates, nameParts[j]).outArcs.get(sigma[i]);
                        if (SetJ != null) destStates.addAll(SetJ);
                    }

                    String destStateName;

                    if (destStates.size() > 0) {
                        destStateName = stateNamesToString(destStates);
                    } else {
                        destStateName = "TrapState";
                    }

                    State destState = getState(dFAStates, destStateName);

                    //      2.1.2. if Y ∉ Q', then set Q' := Q' U {Y}
                    if (destState == null) {
                        destState = new State(destStateName);
                        dFAStates.add(destState);
                    }

                    // 3. the set of accepting states of DM is F' = {X ∈ Q' |  contains an element qi ∈ F}
                    for (State stateJ : destStates) {
                        if (stateJ.accepting) {
                            destState.accepting = true;
                            break;
                        }
                    }

                    //      2.1.3. add an arc from X to Y labelled a
                    Set<State> wrappedDestState = new HashSet<>();
                    wrappedDestState.add(destState);
                    stateI.outArcs.put(sigma[i], wrappedDestState);
                }
            }
        } // until done
    }

    /**
     * Renames dFAStates to q0',q1',q2'...
     */
    private static void renameDFAStates() {
        dFAStartState.name = "q0'";

        int index = 1;
        for (State thisState : dFAStates) {
            if (thisState.equals(dFAStartState)) continue;

            thisState.name = "q" + index++ + "'";
        }
    }

    /**
     * Determines distinguishability of 'dFAStates' and stores results in 'distinguishable', 'xAxisStates' and
     * 'yAxisStates'
     */
    private static void generateDistinguishable() {
        distinguishable = new boolean[dFAStates.size() - 1][];
        for (int i = 0; i < distinguishable.length; i++) {
            distinguishable[i] = new boolean[distinguishable.length - i];
            for (int j = 0; j < distinguishable.length - i; j++) distinguishable[i][j] = false;
        }

        xAxisStates = new State[dFAStates.size() - 1];
        yAxisStates = new State[dFAStates.size() - 1];

        int index = 0;
        for (State thisState : dFAStates) {
            if (index != xAxisStates.length) xAxisStates[index] = thisState;
            if (index != 0) yAxisStates[index - 1] = thisState;

            index++;
        }

        for (int i = 0; i < xAxisStates.length; i++)
            for (int j = 0; j < distinguishable[i].length; j++)
                if ((xAxisStates[i].accepting && !yAxisStates[j + i].accepting) ||
                        (!xAxisStates[i].accepting && yAxisStates[j + i].accepting))
                    distinguishable[i][j] = true;

        boolean changesMade = true;

        while (changesMade) {
            changesMade = false;

            for (int i = 0; i < xAxisStates.length; i++) {
                for (int j = 0; j < distinguishable[i].length; j++) {
                    if (distinguishable[i][j]) continue;

                    for (String thisChar : sigma) {
                        State stateOne = xAxisStates[i].outArcs.get(thisChar).iterator().next();
                        State stateTwo = yAxisStates[j + i].outArcs.get(thisChar).iterator().next();

                        if (stateOne.equals(stateTwo)) continue;

                        int xIndex;
                        int yIndex;

                        if (getIndex(yAxisStates, stateOne) - getIndex(xAxisStates, stateTwo) >= 0 &&
                                getIndex(xAxisStates, stateTwo) >= 0) {
                            xIndex = getIndex(xAxisStates, stateTwo);
                            yIndex = getIndex(yAxisStates, stateOne);
                        } else {
                            xIndex = getIndex(xAxisStates, stateOne);
                            yIndex = getIndex(yAxisStates, stateTwo);
                        }

                        if (distinguishable[xIndex][yIndex - xIndex]) {
                            distinguishable[i][j] = true;

                            changesMade = true;
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * Outputs distinguishability table/triangle stored in 'distinguishable', 'xAxisStates' and 'yAxisStates' to a .tsv
     * file
     * @param outFilePath path/name of the file to output to
     * @throws IOException thrown if an error occurs while outputting
     */
    private static void outputDistinguishable(String outFilePath) throws IOException {
        FileWriter outFile = new FileWriter(outFilePath);

        for (int i = 0; i < distinguishable.length; i++) {
            outFile.write(yAxisStates[i].name);

            for (int j = 0; j <= i; j++) {
                outFile.write("\t" + (distinguishable[j][i - j] ? "x" : " "));
            }

            outFile.write("\n");
        }

        for (int i = 0; i < xAxisStates.length; i++) outFile.write("\t" + xAxisStates[i].name);

        outFile.close();
    }

    /**
     * Generates and stores Minimized DFA into 'minDFAStates' and 'minDFAStartState' from 'dFAStates' and
     * 'dFAStartState'
     */
    private static void generateMinDFA() {
        minDFAStates = new HashSet<>();
        Set<State> addedStates = new HashSet<>();
        Set<State> thisSet;
        boolean accepting;

        for (int i = 0; i < xAxisStates.length; i++) {
            thisSet = new HashSet<>();
            accepting = false;
            boolean startState = false;

            if (!addedStates.contains(xAxisStates[i])) {
                thisSet.add(xAxisStates[i]);
                if (xAxisStates[i].accepting) accepting = true;
                if (xAxisStates[i].equals(dFAStartState)) startState = true;

                addedStates.add(xAxisStates[i]);
            }

            for (int j = 0; j < distinguishable[i].length; j++) {
                if (!addedStates.contains(yAxisStates[j + i]) && !distinguishable[i][j]) {
                    thisSet.add(yAxisStates[j + i]);
                    if (yAxisStates[j + i].accepting) accepting = true;
                    if (yAxisStates[j + i].equals(dFAStartState)) startState = true;

                    addedStates.add(yAxisStates[j + i]);
                }
            }

            if (startState) {
                minDFAStartState = new State(stateNamesToString(thisSet), accepting);
                minDFAStates.add(minDFAStartState);
            } else if (thisSet.size() > 0) {
                minDFAStates.add(new State(stateNamesToString(thisSet), accepting));
            }
        }

        if (!addedStates.contains(yAxisStates[yAxisStates.length - 1])) {
            thisSet = new HashSet<>();
            thisSet.add(yAxisStates[yAxisStates.length - 1]);

            accepting = yAxisStates[yAxisStates.length - 1].accepting;

            if (yAxisStates[yAxisStates.length - 1].equals(dFAStartState)) {
                minDFAStartState = new State(stateNamesToString(thisSet), accepting);
                minDFAStates.add(minDFAStartState);
            } else {
                minDFAStates.add(new State(stateNamesToString(thisSet), accepting));
            }

            addedStates.add(yAxisStates[yAxisStates.length - 1]);
        }

        for (State stateA : minDFAStates) {
            State aSubState = getState(dFAStates, stateA.name.split(",")[0]);

            for (int i = 0; i < sigma.length; i++) {
                for (State stateB : minDFAStates) {
                    String[] bSubStateNames = stateB.name.split(",");
                    boolean added = false;

                    for (int j = 0; j < bSubStateNames.length; j++) {
                        if (aSubState.outArcs.get(sigma[i]).iterator().next().name.equals(bSubStateNames[j])) {
                            Set<State> sigmaIOutArc = new HashSet<>();
                            sigmaIOutArc.add(stateB);
                            stateA.outArcs.put(sigma[i], sigmaIOutArc);
                            added = true;
                            break;
                        }
                    }

                    if (added) break;
                }
            }
        }
    }

    /**
     * Prints computation of a string as well as whether it is accepted or rejected by the Minimized DFA stored in
     * 'minDFAStates'
     * @param toCheck the string to compute and check
     */
    private static void checkStringAcceptance(String toCheck) {
        State currentState = minDFAStartState;

        boolean firstIteration = true;

        while (true) {
            if (firstIteration) {
                System.out.print("   ");
                firstIteration = false;
            } else {
                System.out.print("|- ");
            }

            System.out.printf("[{%s}, %s]\n", currentState.name, toCheck.length() == 0 ? "λ" : toCheck);

            if (toCheck.length() == 0) break;

            if (currentState.outArcs.get(toCheck.substring(0, 1)) == null) {
                System.out.printf("%c is not in Σ. Therefore, Rejected.\n", toCheck.charAt(0));
                return;
            }

            currentState = currentState.outArcs.get(toCheck.substring(0, 1)).iterator().next();

            toCheck = toCheck.substring(1);
        }

        if (currentState.accepting) {
            System.out.printf("{%s} is an accepting state. Therefore, Accepted.\n", currentState.name);
        } else {
            System.out.printf("{%s} is not an accepting state. Therefore, Rejected.\n", currentState.name);
        }
    }

    /**
     * Outputs a table representing the information stored in received 'states' and 'startState' to a .tsv file
     * @param outFilePath path/name of the file to output to
     * @param states contains information about the states to output
     * @param startState contains start state in received 'states'
     * @throws IOException thrown if an error occurs while outputting
     */
    private static void outputTable(String outFilePath, Set<State> states, State startState) throws IOException {
        FileWriter outFile = new FileWriter(outFilePath);

        for (int i = 0; i < sigma.length; i++) outFile.write("\t" + sigma[i]);
        outFile.write("\tAcceptingState");

        writeStateLine(outFile, startState);

        ArrayList<State> statesList = new ArrayList<>(states);
        Collections.sort(statesList);

        for (State thisState : statesList) {
            if (thisState.equals(startState)) continue;

            writeStateLine(outFile, thisState);
        }

        outFile.close();
    }

    /**
     * Outputs states of a DFA arranged vertically with characters from sigma next to them. From front of these
     * characters, lines go out and connect with top of other states. Accepting states are enclosed in double pipes
     * ‘||’, while non-accepting ones are enclosed in single pipes ‘|’.
     * @param outFilePath path/name of the file to output to
     * @param statesSet contains information about the states to output
     * @param startState contains start state in received 'statesSet'
     * @throws IOException thrown if an error occurs while outputting
     */
    private static void outputGraphicalDFA(String outFilePath, Set<State> statesSet, State startState)
            throws IOException {
        State[] states = new State[statesSet.size()];
        states[0] = startState;

        int maxNameLen = -1;
        int index = 1;
        for(State thisState : statesSet) {
            if (!thisState.equals(startState)) states[index++] = thisState;

            if (thisState.name.length() > maxNameLen) maxNameLen = thisState.name.length();
        }
        maxNameLen += 2;

        StringBuilder[] lines = new StringBuilder[states.length * (sigma.length + 2) - 1];
        for (int i = 0; i < lines.length; i++) lines[i] = new StringBuilder("");

        int lineIndex = 0;
        for (int i = 0; i < states.length; i++) {
            for (int j = 0; j < sigma.length; j++) {
                if (j == 0) {
                    lines[lineIndex].append(" ");
                    for (int k = 0; k < maxNameLen + 2; k++) lines[lineIndex].append("_");
                    lines[lineIndex].append(" <__ ");
                    lineIndex++;

                    lines[lineIndex].append("|").append(states[i].accepting ? "|" : " ").append("{")
                            .append(states[i].name.equals("TrapState") ? "}         " : (states[i].name + "}"));
                    for (int k = 0; k < maxNameLen - states[i].name.length() - 2; k++) lines[lineIndex].append(" ");
                    lines[lineIndex].append(states[i].accepting ? "|" : " ").append("| ");
                } else if (j == sigma.length - 1) {
                    lines[lineIndex].append(states[i].accepting ? "||" : "|_");
                    for (int k = 0; k < maxNameLen; k++) lines[lineIndex].append("_");
                    lines[lineIndex].append(states[i].accepting ? "||" : "_|").append(" ");
                } else {
                    lines[lineIndex].append(states[i].accepting ? "||" : "| ");
                    for (int k = 0; k < maxNameLen; k++) lines[lineIndex].append(" ");
                    lines[lineIndex].append(states[i].accepting ? "||" : " |").append(" ");
                }

                lines[lineIndex].append(sigma[j]);
                for (int k = 0; k < i * sigma.length + j + 1; k++) lines[lineIndex].append("_ ");
                lineIndex++;
            }

            lineIndex++;
        }

        int[] stateStartLines = new int[states.length];
        stateStartLines[0] = 0;
        for (int i = 1; i < stateStartLines.length; i++) stateStartLines[i] = stateStartLines[i - 1] + sigma.length + 2;

        for (int i = 0; i < states.length; i++) {
            for (int j = 0; j < sigma.length; j++) {
                int modifyIndex = 7 + maxNameLen + 2 * (i * sigma.length + j);

                int destStateIndex = -1;
                State destState = states[i].outArcs.get(sigma[j]).iterator().next();
                for (int k = 0; k < states.length; k++) {
                    if (states[k].equals(destState)) {
                        destStateIndex = k;
                        break;
                    }
                }

                if (stateStartLines[i] < stateStartLines[destStateIndex]) {
                    for (int k = stateStartLines[i] + 2 + j; k <= stateStartLines[destStateIndex]; k++) {
                        for (int l = lines[k].length(); l <= modifyIndex; l++) lines[k].append(" ");

                        lines[k].setCharAt(modifyIndex, '|');
                    }
                } else {
                    for (int k = stateStartLines[i] + 1 + j; k > stateStartLines[destStateIndex]; k--) {
                        for (int l = lines[k].length(); l <= modifyIndex; l++) lines[k].append(" ");

                        lines[k].setCharAt(modifyIndex, '|');
                    }
                }

                for (int l = maxNameLen + 8; l < modifyIndex; l += 2) {
                    if (lines[stateStartLines[destStateIndex]].length() > l) {
                        lines[stateStartLines[destStateIndex]].setCharAt(l, '_');
                    } else {
                        lines[stateStartLines[destStateIndex]].append("_ ");
                    }
                }
            }
        }

        FileWriter outFile = new FileWriter(outFilePath);

        for (int i = 0; i < lines.length; i++) {
            outFile.write((i == 0 ? "-->" : "   ") + lines[i].toString());

            if (i != lines.length - 1) outFile.write("\n");
        }

        outFile.close();
    }

    /**
     * Gets state with received 'name' from received 'states'
     * @param states contains the states to search in
     * @param name name of the state to search for
     * @return the matching state or null if not found
     */
    private static State getState(Set<State> states, String name) {
        for (State thisState : states) if (thisState.name.equals(name)) return thisState;

        return null;
    }

    /**
     * Gets λ-closure of received 'state'
     * @param state the state to get λ-closure of
     * @param addTo the set to add λ-closure states to
     * @return set containing λ-closure
     */
    private static Set<State> getLClosure(State state, Set<State> addTo) {
        addTo.add(state);

        if (state.outArcs.get("L") != null)
            for (State thisState : state.outArcs.get("L"))
                if (!addTo.contains(thisState)) addTo.addAll(getLClosure(thisState, addTo));

        return addTo;
    }

    /**
     * Combines names of states in received 'states' with ','s in between
     * @param states the states whose names are to be combined
     * @return the combined string
     */
    private static String stateNamesToString(Set<State> states) {
        List<String> names = new ArrayList<>();
        for (State thisState : states) names.add(thisState.name);
        Collections.sort(names);

        StringBuilder sBuilder = new StringBuilder();
        for (String name : names) sBuilder.append(name).append(",");
        sBuilder.deleteCharAt(sBuilder.length() - 1);

        return sBuilder.toString();
    }

    /**
     * Gets States from dFAStates that do not have an out arc for at least one character in 'Sigma'
     * @return the set of incomplete states
     */
    private static Set<State> getIncompleteDFAStates() {
        Set<State> toReturn = new HashSet<>();

        for (State thisState : dFAStates) if (thisState.outArcs.size() != sigma.length) toReturn.add(thisState);

        return toReturn;
    }

    /**
     * Gets index of the received 'toFind' state in 'states'
     * @param states the array to find 'toFind' state in
     * @param toFind the state to find
     * @return the index at which 'toFind' exists in 'states' or -1 if not found
     */
    private static int getIndex(State[] states, State toFind) {
        for (int i = 0; i < states.length; i++) if (states[i].equals(toFind)) return i;

        return -1;
    }

    /**
     * Writes a line (a single state) to 'outFile'
     * @param outFile the file to write to
     * @param state the state to write (converted into a line/string)
     * @throws IOException thrown if an error occurs while outputting
     */
    private static void writeStateLine(FileWriter outFile, State state) throws IOException {
        outFile.write("\n{" + (state.name.equals("TrapState")  ? "" : state.name) + "}");

        for (int i = 0; i < sigma.length; i++) {
            if (state.outArcs.get(sigma[i]) != null) {
                String stateName = stateNamesToString(state.outArcs.get(sigma[i]));
                outFile.write("\t{" + (stateName.equals("TrapState") ? "" : stateName) + "}");
            } else {
                outFile.write("\t{}");
            }
        }

        if (state.accepting) {
            outFile.write("\t1");
        } else {
            outFile.write("\t0");
        }
    }
}