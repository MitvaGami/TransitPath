import java.util.*;

class Passenger {
	private String name;
	private int age;
	private long phoneNo;
	private double billAmount;
	private boolean isStudent, isSenior;

	Passenger(String name, int age, long phoneNo) {
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Name cannot be empty");
		}
		if (age <= 0 || age > 120) {
			throw new IllegalArgumentException("Invalid age");
		}
		if (String.valueOf(phoneNo).length() != 10) {
			throw new IllegalArgumentException("Phone number must be 10 digits");
		}

		this.name = name;
		this.age = age;
		this.phoneNo = phoneNo;
		this.billAmount = 0;
		// More flexible student/senior criteria
		this.isStudent = false; // Will be set manually based on ID
		this.isSenior = age >= 60;
	}

	public void setBillAmount(double billAmount) {
		this.billAmount = billAmount;
	}

	public double getBillAmount() {
		return billAmount;
	}

	public boolean isStudent() {
		return isStudent;
	}

	public boolean isSenior() {
		return isSenior;
	}

	public String getName() {
		return name;
	}

	public long getPhoneNo() {
		return phoneNo;
	}

	public void setStudentStatus(boolean status) {
		this.isStudent = status;
	}
}

class MetroGraph {
	private int numStations;
	private List<List<Edge>> adjList;
	private Map<String, Integer> stationMap;
	private String[] stationNames;
	// Cache for frequently requested routes
	private Map<String, List<Integer>> routeCache;
	private static final int MAX_ALTERNATIVE_ROUTES = 3;
	private static final double MAX_ROUTE_DEVIATION = 1.5; // 50% longer than shortest

	public MetroGraph(int numStations) {
		this.numStations = numStations;
		adjList = new ArrayList<>(numStations);
		for (int i = 0; i < numStations; i++) {
			adjList.add(new ArrayList<>());
		}
		stationMap = new HashMap<>();
		stationNames = new String[numStations];
		routeCache = new HashMap<>();
	}

	public void addStation(String name, int id) {
		stationMap.put(name, id);
		stationNames[id] = name;
	}

	public void addEdge(int u, int v, int weight) {
		adjList.get(u).add(new Edge(v, weight));
		adjList.get(v).add(new Edge(u, weight)); // Bi-directional edges
	}

	// Helper method to create cache key
	private String createCacheKey(String start, String end) {
		return start + "|" + end;
	}

	// Modified findShortestPath with caching
	private List<Integer> findShortestPath(int start, int end, boolean timeOptimized) {
		String cacheKey = createCacheKey(stationNames[start], stationNames[end]);

		// Check cache first
		if (routeCache.containsKey(cacheKey)) {
			return new ArrayList<>(routeCache.get(cacheKey));
		}

		int[] distances = new int[numStations];
		int[] previous = new int[numStations];
		Arrays.fill(distances, Integer.MAX_VALUE);
		Arrays.fill(previous, -1);
		distances[start] = 0;

		PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(e -> e.weight));
		pq.offer(new Edge(start, 0));

		while (!pq.isEmpty()) {
			Edge current = pq.poll();
			int u = current.to;

			if (u == end)
				break;

			for (Edge edge : adjList.get(u)) {
				int v = edge.to;
				int weight = timeOptimized ? edge.weight : 1; // Use weight=1 for minimal transfers

				if (distances[u] + weight < distances[v]) {
					distances[v] = distances[u] + weight;
					previous[v] = u;
					pq.offer(new Edge(v, distances[v]));
				}
			}
		}

		List<Integer> path = new ArrayList<>();
		for (int at = end; at != -1; at = previous[at]) {
			path.add(at);
		}
		Collections.reverse(path);

		// Cache the result if valid
		if (path.get(0) == start) {
			routeCache.put(cacheKey, new ArrayList<>(path));
		}

		return path.get(0) == start ? path : null;
	}

	// Path Information Display using Dijkstra's Algorithm
	public void displayRouteDetails(String startName, String endName, boolean timeOptimized) {
		// Check if station names are valid
		if (!stationMap.containsKey(startName) || !stationMap.containsKey(endName)) {
			System.out.println("Invalid station name(s): " + startName + ", " + endName);
			return;
		}

		int start = stationMap.get(startName);
		int end = stationMap.get(endName);

		List<Integer> path = findShortestPath(start, end, timeOptimized);

		if (path == null || path.isEmpty()) {
			System.out.println("No path found.");
			return;
		}

		System.out.print("Route: ");
		for (int station : path) {
			System.out.print(stationNames[station] + " -> ");
		}
		System.out.println("End");
	}

	public List<Integer> findShortestPath(String startName, String endName, boolean timeOptimized) {
		return findShortestPath(stationMap.get(startName), stationMap.get(endName), timeOptimized);
	}

	// Improved alternative routes finder
	public void findAlternativeRoutes(String startName, String endName) {
		int start = stationMap.get(startName);
		int end = stationMap.get(endName);

		// Get the shortest path first
		List<Integer> shortestPath = findShortestPath(start, end, true);
		if (shortestPath == null) {
			System.out.println("No route found between " + startName + " and " + endName);
			return;
		}
		int shortestDistance = calculatePathDistance(shortestPath);

		// Print the shortest route first
		System.out.println("\nShortest Route:");
		printRoute(shortestPath, shortestDistance);

		Set<List<Integer>> allRoutes = new HashSet<>();
		List<Integer> currentPath = new ArrayList<>();
		boolean[] visited = new boolean[numStations];

		// Find all possible paths
		findAllPathsDFS(start, end, visited, currentPath, allRoutes, shortestDistance);

		// Convert to list and sort by distance
		List<List<Integer>> sortedRoutes = new ArrayList<>(allRoutes);
		sortedRoutes.sort((a, b) -> calculatePathDistance(a) - calculatePathDistance(b));

		// Remove the shortest path as it's already displayed
		sortedRoutes.removeIf(route -> route.equals(shortestPath));

		// Display alternative routes
		int routesShown = 0;
		System.out.println("\nAlternative Routes:");
		for (List<Integer> route : sortedRoutes) {
			if (routesShown >= MAX_ALTERNATIVE_ROUTES - 1)
				break; // -1 because we already showed the shortest route

			int distance = calculatePathDistance(route);
			if (distance <= shortestDistance * MAX_ROUTE_DEVIATION) {
				routesShown++;
				printRoute(route, distance);
			}
		}

		if (routesShown == 0) {
			System.out.println("No alternative routes found within acceptable distance.");
		}
	}

	private void printRoute(List<Integer> route, int distance) {
		for (int station : route) {
			System.out.print(stationNames[station] + " -> ");
		}
		System.out.println("End");
		System.out.println("Distance: " + distance + " units");
		System.out.println("Price: " + calculateFareWithDiscounts(route, false, false));
		System.out.println();
	}

	// Helper method to calculate path distance
	private int calculatePathDistance(List<Integer> path) {
		int distance = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			for (Edge edge : adjList.get(path.get(i))) {
				if (edge.to == path.get(i + 1)) {
					distance += edge.weight;
					break;
				}
			}
		}
		return distance;
	}

	// Modified DFS to find all possible paths
	private void findAllPathsDFS(int current, int destination, boolean[] visited, List<Integer> currentPath,
			Set<List<Integer>> allRoutes, int shortestDistance) {
		if (currentPath.size() > numStations) {
			return; // Prevent cycles and extremely long paths
		}

		visited[current] = true;
		currentPath.add(current);

		if (current == destination) {
			int currentDistance = calculatePathDistance(currentPath);
			if (currentDistance <= shortestDistance * MAX_ROUTE_DEVIATION) {
				allRoutes.add(new ArrayList<>(currentPath));
			}
		} else {
			for (Edge edge : adjList.get(current)) {
				if (!visited[edge.to]) {
					findAllPathsDFS(edge.to, destination, visited, currentPath, allRoutes, shortestDistance);
				}
			}
		}

		currentPath.remove(currentPath.size() - 1);
		visited[current] = false;
	}

	// Transfer Minimization
	public void findMinimalTransferPath(Passenger passenger, String startName, String endName) {
		int start = stationMap.get(startName);
		int end = stationMap.get(endName);

		List<Integer> path = findShortestPath(start, end, false); // Simple Dijkstra, further customization needed
		System.out.print("Minimal Transfer Path: ");
		for (int station : path) {
			System.out.print(stationNames[station] + " -> ");
		}
		System.out.println("End");
		System.out.println("Price: " + 20 + path.size() * 1.5);

		generateBill(passenger, startName, endName);
	}

	public void generateBill(Passenger passenger, String startName, String endName) {
		double fare = calculateFareWithDiscounts(startName, endName, passenger.isStudent(), passenger.isSenior());
		passenger.setBillAmount(fare);
		System.out.println("Passenger: " + passenger.getName());
		System.out.println("Phone Number: " + passenger.getPhoneNo());
		System.out.println("Bill Amount: " + fare);
	}

	public void changeRoute(Passenger passenger, String startName, String midWayName, String newDestName) {
		if (!isOnPath(startName, midWayName, newDestName)) {
			System.out.println("Midway station is not on the path from start to destination.");

		}
		findMinimalTransferPath(passenger, midWayName, newDestName);
		passenger.setBillAmount(calculateFareWithDiscounts(startName, midWayName, passenger.isStudent(),
				passenger.isSenior())
				+ calculateFareWithDiscounts(midWayName, newDestName, passenger.isStudent(), passenger.isSenior()));
	}

	public boolean isOnPath(String startName, String midWayName, String endName) {
		List<Integer> originalPath = findShortestPath(startName, endName, false);
		int mid = stationMap.get(midWayName);
		for (int station : originalPath) {
			if (station == mid) {
				return true;
			}
		}
		return false;
	}

	// Improved fare calculation
	public double calculateFareWithDiscounts(String startName, String endName, boolean isStudent, boolean isSenior) {
		List<Integer> path = findShortestPath(stationMap.get(startName), stationMap.get(endName), true);
		return calculateFareWithDiscounts(path, isStudent, isSenior);
	}

	private double calculateFareWithDiscounts(List<Integer> path, boolean isStudent, boolean isSenior) {
		if (path == null || path.isEmpty())
			return 0;

		double baseFare = 20;
		int distance = calculatePathDistance(path);

		// Zone-based pricing
		double zoneFare;
		if (distance <= 5)
			zoneFare = 1.0;
		else if (distance <= 12)
			zoneFare = 1.5;
		else if (distance <= 21)
			zoneFare = 2.0;
		else
			zoneFare = 2.5;

		double totalFare = baseFare + (distance * zoneFare);

		// Apply discounts
		if (isStudent)
			totalFare *= 0.5; // 50% discount for students
		if (isSenior)
			totalFare *= 0.6; // 40% discount for seniors

		// Round to nearest 0.5
		return Math.round(totalFare * 2) / 2.0;
	}

	// Helper Class for Edges
	static class Edge {
		int to, weight;

		public Edge(int to, int weight) {
			this.to = to;
			this.weight = weight;
		}
	}

	// Add these new methods
	public String[] getStationNames() {
		return stationNames;
	}

	public boolean isValidStation(String stationName) {
		return stationMap.containsKey(stationName);
	}
}

public class MetroRouteOptimization {
	public static void main(String[] args) {
		Scanner scanner = new Scanner(System.in);
		MetroGraph graph = new MetroGraph(22);

		// Add stations
		graph.addStation("CENTRAL SECRETARIAT", 0);
		graph.addStation("PATEL CHOWK", 1);
		graph.addStation("RAJIV CHOWK", 2);
		graph.addStation("MANDI HOUSE", 3);
		graph.addStation("SUPREME COURT", 4);
		graph.addStation("INDRAPRASTHA", 5);
		graph.addStation("YAMUNA BANK", 6);
		graph.addStation("AKSHARDHAM", 7);
		graph.addStation("MAYUR VIHAR", 8);
		graph.addStation("NIZAMUDDIN", 9);
		graph.addStation("ASHRAM", 10);
		graph.addStation("VINOBAPURI", 11);
		graph.addStation("LAJPAT NAGAR", 12);
		graph.addStation("SOUTH EXTENTION", 13);
		graph.addStation("DILLI HAAT", 14);
		graph.addStation("JOR BAGH", 15);
		graph.addStation("LOK KALYAN MARG", 16);
		graph.addStation("UDYOG BHAWAN", 17);
		graph.addStation("KHAN MARKET", 18);
		graph.addStation("JLN STADIUM", 19);
		graph.addStation("JANGPURA", 20);
		graph.addStation("JANPATH", 21);

		// Add edges between metro stations with their respective distances
		graph.addEdge(0, 1, 2);
		graph.addEdge(0, 21, 1);
		graph.addEdge(0, 17, 2);
		graph.addEdge(0, 18, 4);
		graph.addEdge(1, 2, 2);
		graph.addEdge(2, 3, 4);
		graph.addEdge(3, 4, 2);
		graph.addEdge(3, 21, 3);
		graph.addEdge(4, 5, 2);
		graph.addEdge(5, 6, 3);
		graph.addEdge(6, 7, 3);
		graph.addEdge(7, 8, 3);
		graph.addEdge(8, 9, 5);
		graph.addEdge(9, 10, 3);
		graph.addEdge(10, 11, 3);
		graph.addEdge(11, 12, 3);
		graph.addEdge(12, 13, 3);
		graph.addEdge(12, 20, 3);
		graph.addEdge(13, 14, 2);
		graph.addEdge(14, 15, 2);
		graph.addEdge(15, 16, 2);
		graph.addEdge(16, 17, 2);
		graph.addEdge(18, 19, 3);
		graph.addEdge(19, 20, 2);

		Passenger passenger = null;
		while (passenger == null) {
			try {
				System.out.println("Enter name of passenger:");
				String name = scanner.nextLine().trim();

				System.out.println("Enter age:");
				int age = Integer.parseInt(scanner.nextLine());

				System.out.println("Enter phone number (10 digits):");
				long phoneNo = Long.parseLong(scanner.nextLine());

				passenger = new Passenger(name, age, phoneNo);

				System.out.println("Are you a student? (yes/no):");
				String studentResponse = scanner.nextLine().toLowerCase();
				passenger.setStudentStatus(studentResponse.startsWith("y"));

			} catch (IllegalArgumentException e) {
				System.out.println("Error: " + e.getMessage());
				System.out.println("Please try again.\n");
			}
		}

		// Display available stations
		System.out.println("\nAvailable Stations:");
		for (int i = 0; i < 22; i++) {
			System.out.printf("%2d. %s%n", (i + 1), graph.getStationNames()[i]);
		}

		String start = null, end = null;
		while (start == null || end == null) {
			try {
				System.out.println("\nEnter start station name:");
				start = scanner.nextLine().toUpperCase();
				if (!graph.isValidStation(start)) {
					System.out.println("Invalid station name. Please try again.");
					start = null;
					continue;
				}

				System.out.println("Enter end station name:");
				end = scanner.nextLine().toUpperCase();
				if (!graph.isValidStation(end)) {
					System.out.println("Invalid station name. Please try again.");
					end = null;
					continue;
				}

				if (start.equals(end)) {
					System.out.println("Start and end stations cannot be the same. Please try again.");
					start = null;
					end = null;
				}
			} catch (Exception e) {
				System.out.println("Error occurred. Please try again.");
				start = null;
				end = null;
			}
		}

		while (true) {
			try {
				System.out.println("\nChoose an option:");
				System.out.println("1. Display Route Details");
				System.out.println("2. Find Alternative Routes");
				System.out.println("3. Find Minimal Transfer Path");
				System.out.println("4. Change Route");
				System.out.println("5. Exit");

				String choice = scanner.nextLine();

				switch (choice) {
					case "1":
						graph.displayRouteDetails(start, end, true);
						break;
					case "2":
						graph.findAlternativeRoutes(start, end);
						break;
					case "3":
						graph.findMinimalTransferPath(passenger, start, end);
						break;
					case "4":
						System.out.println("Enter station that you want to get off at:");
						String midWayChange = scanner.nextLine().toUpperCase();
						if (!graph.isValidStation(midWayChange)) {
							System.out.println("Invalid station name.");
							break;
						}
						if (!graph.isOnPath(start, midWayChange, end)) {
							System.out.println("Station " + midWayChange + " does not lie on your path");
						} else {
							System.out.println("Enter new destination station:");
							String newDestChange = scanner.nextLine().toUpperCase();
							if (!graph.isValidStation(newDestChange)) {
								System.out.println("Invalid station name.");
								break;
							}
							graph.changeRoute(passenger, start, midWayChange, newDestChange);
						}
						break;
					case "5":
						System.out.println("Thank you for using Metro Route Optimization!");
						scanner.close();
						System.exit(0);
						break;
					default:
						System.out.println("Invalid choice. Please try again.");
				}
			} catch (Exception e) {
				System.out.println("An error occurred: " + e.getMessage());
				System.out.println("Please try again.");
			}
		}
	}
}