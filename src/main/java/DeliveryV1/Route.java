package DeliveryV1;

public class Route {
    private int[] order; // Represents the order of packages to be delivered
    private int totalDistance; // Represents the total distance of the route

    public Route(int[] order, int totalDistance)
    {
        this.order = order;
        this.totalDistance = totalDistance;
    }

    public int[] getOrder() {
        return order;
    }

    public int getTotalDistance() {
        return totalDistance;
    }

    // Calculate the total distance of the route
    public void calculateTotalDistance(int[][] distances) {
        int distance = 0;
        for (int i = 0; i < order.length - 1; i++) {
            distance += distances[order[i]][order[i + 1]];
        }
        totalDistance = distance;
    }
}

