package com.satellite.routing;

import com.satellite.model.Satellite;

import java.util.*;

/**
 * Định tuyến truyền tin giữa các vệ tinh sử dụng thuật toán Dijkstra.
 * Chỉ cho phép truyền qua các vệ tinh relay có Line-of-Sight.
 */
public class RoutingEngine {

    /**
     * Tìm đường ngắn nhất giữa 2 vệ tinh.
     * Chỉ đi qua vệ tinh relay (is_relay=true), ngoại trừ src/dst có thể không relay.
     *
     * @param allSats danh sách tất cả vệ tinh (tọa độ x,y,z đã cập nhật)
     * @param srcId   ID vệ tinh nguồn
     * @param dstId   ID vệ tinh đích
     * @param planetRadiusScene bán kính hành tinh scene units
     * @return kết quả định tuyến
     */
    public static RoutingResult findRoute(List<Satellite> allSats,
                                           int srcId, int dstId,
                                           double planetRadiusScene) {
        // Lọc: chỉ lấy relay + src + dst
        List<Satellite> candidates = new ArrayList<>();
        Map<Integer, Integer> idToIdx = new HashMap<>();

        for (Satellite s : allSats) {
            if (s.isRelay() || s.getId() == srcId || s.getId() == dstId) {
                idToIdx.put(s.getId(), candidates.size());
                candidates.add(s);
            }
        }

        int n = candidates.size();
        Integer si = idToIdx.get(srcId);
        Integer di = idToIdx.get(dstId);
        if (si == null || di == null) {
            return new RoutingResult(Collections.emptyList(), -1);
        }

        // Xây adjacency matrix — O(n²)
        double[][] adj = new double[n][n];
        for (double[] row : adj) Arrays.fill(row, Double.MAX_VALUE);
        for (int i = 0; i < n; i++) {
            adj[i][i] = 0;
            for (int j = i + 1; j < n; j++) {
                if (LineOfSight.hasLOS(candidates.get(i), candidates.get(j), planetRadiusScene)) {
                    double d = LineOfSight.distance(candidates.get(i), candidates.get(j));
                    adj[i][j] = d;
                    adj[j][i] = d;
                }
            }
        }

        // Dijkstra
        double[] dist = new double[n];
        int[] prev = new int[n];
        Arrays.fill(dist, Double.MAX_VALUE);
        Arrays.fill(prev, -1);
        dist[si] = 0;

        PriorityQueue<double[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[0]));
        pq.offer(new double[]{0, si});
        boolean[] visited = new boolean[n];

        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int u = (int) cur[1];
            if (visited[u]) continue;
            visited[u] = true;
            if (u == di) break;

            for (int v = 0; v < n; v++) {
                if (!visited[v] && adj[u][v] < Double.MAX_VALUE) {
                    double nd = dist[u] + adj[u][v];
                    if (nd < dist[v]) {
                        dist[v] = nd;
                        prev[v] = u;
                        pq.offer(new double[]{nd, v});
                    }
                }
            }
        }

        // Không tìm được đường
        if (dist[di] == Double.MAX_VALUE) {
            return new RoutingResult(Collections.emptyList(), -1);
        }

        // Truy vết đường đi
        List<Satellite> path = new ArrayList<>();
        for (int at = di; at != -1; at = prev[at]) {
            path.add(candidates.get(at));
        }
        Collections.reverse(path);

        return new RoutingResult(path, dist[di]);
    }

    /**
     * Kết quả định tuyến: đường đi và tổng khoảng cách.
     */
    public record RoutingResult(List<Satellite> path, double totalDistance) {

        public boolean found() {
            return totalDistance >= 0 && !path.isEmpty();
        }

        public String summary() {
            if (!found()) return "Không tìm thấy đường đi!";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) sb.append(" → ");
                sb.append(path.get(i).getName());
            }
            sb.append(String.format("  (%.1f units, %d hop)", totalDistance, path.size() - 1));
            return sb.toString();
        }
    }
}
