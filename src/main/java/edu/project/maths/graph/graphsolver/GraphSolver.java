package edu.project.maths.graph.graphsolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.DijkstraShortestPath;

public class GraphSolver {

    private static final double alpha = 0.5;

    public static void main(String[] argv) {

        System.out.println("I'm famous");

        NetworkGraph<String, NetworkLink> graph = createStringGraph();
        Transfer t5 = new Transfer("t5", "a", "c", 250, 5, 0);
        int bmin = t5.minimumSlotsRequired();
        System.out.println("bmin required: " + bmin);

        DijkstraShortestPath<String, NetworkLink> freeRoute = graph.getCapacitadedShortestPath("a", "c", bmin);

        if (freeRoute.getPath() != null) {
            System.out.println("Path for this transfer " + freeRoute.getPath());
            return;
        }

        List<GraphPath<String, NetworkLink>> paths = graph.getKShortestPaths("a", "c");
        HashMap<Transfer, Integer> solution = null;

        System.out.println("K Shortest Paths: " + paths);
        for (GraphPath<String, NetworkLink> path : paths) {
            System.out.println("==========================");
            System.out.println("Taking path: " + path);
            HashMap<Transfer, Integer> partialSolution = new HashMap<Transfer, Integer>();

            List<NetworkLink> edgeList = path.getEdgeList();
            List<NetworkLink> edgeListRemaining = new ArrayList(edgeList);
            //Collections.copy(edgeListRemaining, edgeList);
            boolean noSolutionForThisPath = false;

            for (NetworkLink link : edgeList) {
                int squeezedValue = 0;
                System.out.println("Edge List:  " + edgeListRemaining);
                if (link.getFreeSlots() < bmin) {
                    // 1. Generate RCL here 
                    // 2. then select a transfer at random
                    // 3. Check if transfer fulfills demand, if not select another at random
                    // 4. If transfers can be found which fulfils demand add it to the partial solution
                    // 5. Otherwise there is no solution in this path, try next path
                    HashMap<Transfer, Integer> candidateSet = getCandidateSetWithQuality(edgeListRemaining, partialSolution, bmin);
                    List<Transfer> rcl = getRCL(candidateSet);
                    List<Transfer> transferSetInThisLink = new ArrayList<Transfer>();
                    Random r = new Random();

                    while (!rcl.isEmpty() && squeezedValue < bmin) {

                        Transfer luckyTransfer = rcl.get(r.nextInt(rcl.size()));
                        squeezedValue += luckyTransfer.numberOfSqueezableSlots();
                        rcl.remove(luckyTransfer);
                        transferSetInThisLink.add(luckyTransfer);
                    }

                    if (squeezedValue >= bmin) {
                        for (Transfer t : transferSetInThisLink) {
                            partialSolution.put(t, t.numberOfSqueezableSlots());
                        }
                    } else {
                        noSolutionForThisPath = true;
                        break;
                    }

                }

                edgeListRemaining.remove(link);
            }

            if (!noSolutionForThisPath) {
                //found solution :D
                solution = partialSolution;
                break;
            }
        }

        if (solution != null) {
            System.out.print("Solution is: {");

            for (Map.Entry<Transfer, Integer> entrySet : solution.entrySet()) {
                Transfer key = entrySet.getKey();
                Integer value = entrySet.getValue();
                System.out.print(" (" + key.getName() + ", " + value + ")");
            }

            System.out.println("}");
        }
    }

    private static NetworkGraph<String, NetworkLink> createStringGraph() {
        NetworkGraph<String, NetworkLink> g
                = new NetworkGraph<String, NetworkLink>(NetworkLink.class);

        String a = "a";
        String b = "b";
        String c = "c";
        String d = "d";

        // add the vertices
        g.addVertex(a);
        g.addVertex(b);
        g.addVertex(c);
        g.addVertex(d);

        // add edges to create a circuit
        g.addEdge(a, b);
        g.addEdge(a, d);
        g.addEdge(d, b);
        g.addEdge(b, c);
        g.addEdge(c, d);

        //adding transfers to the demandlink
        Transfer t1 = new Transfer("t1", "a", "c", 40, 5, 5);

        Transfer t2 = new Transfer("t2", "c", "a", 40, 15, 3);

        Transfer t3 = new Transfer("t3", "a", "d", 40, 5, 6);

        Transfer t4 = new Transfer("t4", "b", "d", 40, 20, 2);

        ((NetworkLink) g.getEdge("a", "d")).addTransfer(t1);
        ((NetworkLink) g.getEdge("a", "d")).addTransfer(t2);

        ((NetworkLink) g.getEdge("a", "b")).addTransfer(t3);

        ((NetworkLink) g.getEdge("b", "c")).addTransfer(t4);

        ((NetworkLink) g.getEdge("c", "d")).addTransfer(t1);
        ((NetworkLink) g.getEdge("c", "d")).addTransfer(t2);
        ((NetworkLink) g.getEdge("c", "d")).addTransfer(t4);

        ((NetworkLink) g.getEdge("b", "d")).addTransfer(t3);

        return g;
    }

    private static List<Transfer> getRCL(HashMap<Transfer, Integer> tempRCL) {
        int qmax = 0, qmin = 0;

        List<Transfer> returnList = new ArrayList<Transfer>();

        for (Map.Entry<Transfer, Integer> entrySet : tempRCL.entrySet()) {

            Transfer key = entrySet.getKey();
            Integer value = entrySet.getValue();

            if (value > qmax) {
                qmax = value;
            }
            if (value < qmin) {
                qmin = value;
            }
        }

        double q = qmax - alpha * (qmax - qmin);

        for (Map.Entry<Transfer, Integer> entrySet : tempRCL.entrySet()) {
            Transfer key = entrySet.getKey();
            Integer value = entrySet.getValue();

            if (value > q) {
                returnList.add(key);
            }

        }

        return returnList;
    }

    private static HashMap<Transfer, Integer> getCandidateSetWithQuality(List<NetworkLink> edgeListRemaining, HashMap<Transfer, Integer> partialSolution, int bmin) {

        HashMap<Transfer, Integer> candidateSet = new HashMap<Transfer, Integer>();

        NetworkLink currentLink = edgeListRemaining.get(0);

        ArrayList<Transfer> transferList = currentLink.getTransferList();

        for (Transfer transfer : transferList) {

            if (partialSolution.containsKey(transfer)) {
                continue;
            }

            int numberOfAppearancesInTheRoute = getNumberOfAppearancesInTheRoute(edgeListRemaining, transfer);

            //HashMap<Transfer, Integer> transferEntry = new HashMap<Transfer, Integer>();
            candidateSet.put(transfer, transfer.numberOfSqueezableSlots() * numberOfAppearancesInTheRoute);

            //System.out.println("Select this transfer, we have only one chance.." + transfer.getName());
        }

        for (Map.Entry<Transfer, Integer> entrySet : candidateSet.entrySet()) {
            Transfer key = entrySet.getKey();
            Integer value = entrySet.getValue();
            System.out.println("Transfer is " + key.getName() + " Value is " + value);
        }

        System.out.print("Candidate Set with quality is: {");

        for (Map.Entry<Transfer, Integer> entrySet : candidateSet.entrySet()) {
            Transfer key = entrySet.getKey();
            Integer value = entrySet.getValue();
            System.out.print(" (" + key.getName() + ", " + value + ")");
        }

        System.out.println("}");

        return candidateSet;
    }

    private static int getNumberOfAppearancesInTheRoute(List<NetworkLink> edgeListRemaining, Transfer currentTransfer) {
        int appearances = 0;
        for (NetworkLink edge : edgeListRemaining) {
            for (Transfer transfer : edge.getTransferList()) {
                if (transfer.equals(currentTransfer)) {
                    appearances++;
                    break;
                }
            }
        }

        return appearances;
    }

}
