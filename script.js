// Metro station data
const stations = [
    { id: 0, label: 'CENTRAL SECRETARIAT' },
    { id: 1, label: 'PATEL CHOWK' },
    { id: 2, label: 'RAJIV CHOWK' },
    { id: 3, label: 'MANDI HOUSE' },
    { id: 4, label: 'SUPREME COURT' },
    { id: 5, label: 'INDRAPRASTHA' },
    { id: 6, label: 'YAMUNA BANK' },
    { id: 7, label: 'AKSHARDHAM' },
    { id: 8, label: 'MAYUR VIHAR' },
    { id: 9, label: 'NIZAMUDDIN' },
    { id: 10, label: 'ASHRAM' },
    { id: 11, label: 'VINOBAPURI' },
    { id: 12, label: 'LAJPAT NAGAR' },
    { id: 13, label: 'SOUTH EXTENTION' },
    { id: 14, label: 'DILLI HAAT' },
    { id: 15, label: 'JOR BAGH' },
    { id: 16, label: 'LOK KALYAN MARG' },
    { id: 17, label: 'UDYOG BHAWAN' },
    { id: 18, label: 'KHAN MARKET' },
    { id: 19, label: 'JLN STADIUM' },
    { id: 20, label: 'JANGPURA' },
    { id: 21, label: 'JANPATH' }
];

const edges = [
    { from: 0, to: 1, length: 2 },
    { from: 0, to: 21, length: 1 },
    { from: 0, to: 17, length: 2 },
    { from: 0, to: 18, length: 4 },
    { from: 1, to: 2, length: 2 },
    { from: 2, to: 3, length: 4 },
    { from: 3, to: 4, length: 2 },
    { from: 3, to: 21, length: 3 },
    { from: 4, to: 5, length: 2 },
    { from: 5, to: 6, length: 3 },
    { from: 6, to: 7, length: 3 },
    { from: 7, to: 8, length: 3 },
    { from: 8, to: 9, length: 5 },
    { from: 9, to: 10, length: 3 },
    { from: 10, to: 11, length: 3 },
    { from: 11, to: 12, length: 3 },
    { from: 12, to: 13, length: 3 },
    { from: 12, to: 20, length: 3 },
    { from: 13, to: 14, length: 2 },
    { from: 14, to: 15, length: 2 },
    { from: 15, to: 16, length: 2 },
    { from: 16, to: 17, length: 2 },
    { from: 18, to: 19, length: 3 },
    { from: 19, to: 20, length: 2 }
];

// DOM Elements
const getStartedBtn = document.getElementById('getStarted');
const userModal = document.getElementById('userModal');
const userForm = document.getElementById('userForm');
let network = null;

// Event Listeners
getStartedBtn.addEventListener('click', () => {
    userModal.classList.add('active');
});

userForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const userData = {
        name: document.getElementById('name').value,
        age: document.getElementById('age').value,
        phone: document.getElementById('phone').value,
        isStudent: document.getElementById('isStudent').checked
    };
    
    // Store user data
    localStorage.setItem('userData', JSON.stringify(userData));
    
    // Hide modal and show graph
    userModal.classList.remove('active');
    document.querySelector('.landing-container').style.display = 'none';
    initializeGraph();
    showRoutePanel();
});

// Dijkstra's Algorithm implementation
function findShortestPath(start, end, graph) {
    const distances = {};
    const previous = {};
    const unvisited = new Set();
    
    // Initialize distances
    stations.forEach(station => {
        distances[station.id] = Infinity;
        previous[station.id] = null;
        unvisited.add(station.id);
    });
    distances[start] = 0;

    while (unvisited.size > 0) {
        // Find minimum distance node
        let current = Array.from(unvisited).reduce((min, node) => 
            distances[node] < distances[min] ? node : min
        );

        if (current === end) break;
        unvisited.delete(current);

        // Find neighbors
        const neighbors = edges.filter(edge => 
            edge.from === current || edge.to === current
        );

        for (let edge of neighbors) {
            const neighbor = edge.from === current ? edge.to : edge.from;
            if (!unvisited.has(neighbor)) continue;

            const alt = distances[current] + edge.length;
            if (alt < distances[neighbor]) {
                distances[neighbor] = alt;
                previous[neighbor] = current;
            }
        }
    }

    // Reconstruct path
    const path = [];
    let current = end;
    while (current !== null) {
        path.unshift(current);
        current = previous[current];
    }
    return path.length > 1 ? path : null;
}

// Find alternative routes using DFS
function findAlternativeRoutes(start, end) {
    const visited = new Set();
    const currentPath = [start];
    const allPaths = [];
    const MAX_PATHS = 3;
    const shortestPath = findShortestPath(start, end);
    const shortestDistance = calculatePathDistance(shortestPath);

    function dfs(current) {
        if (current === end) {
            const distance = calculatePathDistance(currentPath);
            if (distance <= shortestDistance * 1.5) { // 50% longer than shortest
                allPaths.push([...currentPath]);
            }
            return;
        }

        if (allPaths.length >= MAX_PATHS) return;

        const neighbors = edges.filter(edge => 
            edge.from === current || edge.to === current
        );

        for (let edge of neighbors) {
            const neighbor = edge.from === current ? edge.to : edge.from;
            if (!visited.has(neighbor)) {
                visited.add(neighbor);
                currentPath.push(neighbor);
                dfs(neighbor);
                currentPath.pop();
                visited.delete(neighbor);
            }
        }
    }

    visited.add(start);
    dfs(start);
    return allPaths;
}

function calculatePathDistance(path) {
    let distance = 0;
    for (let i = 0; i < path.length - 1; i++) {
        const edge = edges.find(e => 
            (e.from === path[i] && e.to === path[i + 1]) ||
            (e.to === path[i] && e.from === path[i + 1])
        );
        distance += edge.length;
    }
    return distance;
}

function calculateFare(distance, isStudent, isSenior) {
    let baseFare = 20;
    let zoneFare;
    
    if (distance <= 5) zoneFare = 1.0;
    else if (distance <= 12) zoneFare = 1.5;
    else if (distance <= 21) zoneFare = 2.0;
    else zoneFare = 2.5;

    let totalFare = baseFare + (distance * zoneFare);

    if (isStudent) totalFare *= 0.5;
    if (isSenior) totalFare *= 0.6;

    return Math.round(totalFare * 2) / 2.0;
}

function highlightRoute(path, color = '#2196F3') {
    if (!path || !window.network) return;

    // Create new nodes array with updated colors
    const nodes = stations.map(station => {
        const isInPath = path.includes(station.id);
        return {
            id: station.id,
            label: station.label,
            color: {
                background: isInPath ? color : '#ffffff',
                border: isInPath ? color : '#2196F3'
            },
            font: {
                size: 16,
                face: 'Arial',
                color: '#333333',
                bold: true,
                background: 'white'
            },
            size: 25,
            borderWidth: 3,
            shadow: true
        };
    });

    // Create new edges array with updated colors
    const visEdges = edges.map((edge, index) => {
        const isInPath = path.some((nodeId, i) => {
            if (i === path.length - 1) return false;
            const nextNodeId = path[i + 1];
            return (edge.from === nodeId && edge.to === nextNodeId) ||
                   (edge.from === nextNodeId && edge.to === nodeId);
        });

        return {
            id: index,
            from: edge.from,
            to: edge.to,
            label: edge.length + ' km',
            color: isInPath ? color : '#2196F3',
            width: isInPath ? 5 : 2,
            font: {
                size: 14,
                face: 'Arial',
                color: '#666666',
                background: 'white'
            },
            smooth: {
                type: 'curvedCW',
                roundness: 0.2
            }
        };
    });

    // Update the network with new data
    const data = {
        nodes: new vis.DataSet(nodes),
        edges: new vis.DataSet(visEdges)
    };

    window.network.setData(data);

    // Focus on the path
    window.network.fit({
        nodes: path,
        animation: {
            duration: 1000,
            easingFunction: 'easeInOutQuad'
        }
    });
}

function initializeGraph() {
    const graphDiv = document.createElement('div');
    graphDiv.id = 'graphContainer';
    document.body.appendChild(graphDiv);
    graphDiv.style.display = 'block';

    // Create nodes with improved styling
    window.nodesDataset = new vis.DataSet(
        stations.map(station => ({
            id: station.id,
            label: station.label,
            color: {
                background: '#ffffff',
                border: '#2196F3',
                highlight: { background: '#2196F3', border: '#1976D2' }
            },
            font: {
                size: 16,
                face: 'Arial',
                color: '#333333',
                bold: true,
                background: 'white'
            },
            size: 25,
            borderWidth: 3,
            shadow: true
        }))
    );

    // Create edges with improved styling
    window.edgesDataset = new vis.DataSet(
        edges.map((edge, index) => ({
            id: index,
            from: edge.from,
            to: edge.to,
            label: edge.length + ' km',
            color: '#2196F3',
            width: 2,
            font: {
                size: 14,
                face: 'Arial',
                color: '#666666',
                background: 'white'
            },
            smooth: {
                type: 'curvedCW',
                roundness: 0.2
            }
        }))
    );

    const container = document.getElementById('graphContainer');
    const data = { 
        nodes: window.nodesDataset, 
        edges: window.edgesDataset 
    };
    
    const options = {
        nodes: {
            shape: 'dot',
            scaling: {
                label: { enabled: true }
            }
        },
        edges: {
            selectionWidth: 3,
            arrows: {
                to: { enabled: false },
                from: { enabled: false }
            }
        },
        physics: {
            enabled: true,
            barnesHut: {
                gravitationalConstant: -10000,
                springLength: 200,
                springConstant: 0.04
            },
            stabilization: {
                iterations: 1000
            }
        },
        interaction: {
            hover: true,
            tooltipDelay: 200,
            zoomView: true,
            dragView: true
        }
    };

    window.network = new vis.Network(container, data, options);

    // Wait for physics stabilization
    window.network.on("stabilizationIterationsDone", function () {
        window.network.setOptions({ physics: false });
    });
    
    // Add result panel
    const resultPanel = document.createElement('div');
    resultPanel.id = 'resultPanel';
    resultPanel.className = 'result-panel';
    document.body.appendChild(resultPanel);
}

function showRoutePanel() {
    // Create route selection panel
    const panel = document.createElement('div');
    panel.className = 'route-panel';
    panel.innerHTML = `
        <h3>Select Route</h3>
        <select id="startStation">
            <option value="">Select Start Station</option>
            ${stations.map(s => `<option value="${s.id}">${s.label}</option>`).join('')}
        </select>
        <select id="endStation">
            <option value="">Select End Station</option>
            ${stations.map(s => `<option value="${s.id}">${s.label}</option>`).join('')}
        </select>
        <button id="findRoute" class="btn-primary">Find Routes</button>
    `;
    document.body.appendChild(panel);
    panel.style.display = 'block';

    // Add event listener for route finding
    document.getElementById('findRoute').addEventListener('click', findRoutes);
}

function findRoutes() {
    const startId = parseInt(document.getElementById('startStation').value);
    const endId = parseInt(document.getElementById('endStation').value);
    const userData = JSON.parse(localStorage.getItem('userData'));

    if (!startId || !endId) {
        alert('Please select both start and end stations');
        return;
    }

    // Find shortest path
    const shortestPath = findShortestPath(startId, endId);
    if (!shortestPath) {
        alert('No route found between selected stations');
        return;
    }

    // Find alternative routes
    const alternativeRoutes = findAlternativeRoutes(startId, endId);

    // Display results
    const resultPanel = document.getElementById('resultPanel');
    resultPanel.innerHTML = '<h3>Available Routes</h3>';

    // Display shortest route
    const shortestDistance = calculatePathDistance(shortestPath);
    const shortestFare = calculateFare(shortestDistance, userData.isStudent, parseInt(userData.age) >= 60);
    
    resultPanel.innerHTML += `
        <div class="route-option" onclick="highlightRoute(${JSON.stringify(shortestPath)}, '#2196F3')">
            <h4>Shortest Route (${shortestDistance} km)</h4>
            <p>${shortestPath.map(id => stations[id].label).join(' → ')}</p>
            <p>Fare: ₹${shortestFare}</p>
        </div>
    `;

    // Display alternative routes
    alternativeRoutes.forEach((route, index) => {
        if (JSON.stringify(route) !== JSON.stringify(shortestPath)) {
            const distance = calculatePathDistance(route);
            const fare = calculateFare(distance, userData.isStudent, parseInt(userData.age) >= 60);
            const color = ['#FFC107', '#4CAF50', '#9C27B0'][index];
            
            resultPanel.innerHTML += `
                <div class="route-option" onclick="highlightRoute(${JSON.stringify(route)}, '${color}')">
                    <h4>Alternative Route ${index + 1} (${distance} km)</h4>
                    <p>${route.map(id => stations[id].label).join(' → ')}</p>
                    <p>Fare: ₹${fare}</p>
                </div>
            `;
        }
    });

    // Show first route by default
    highlightRoute(shortestPath);
}

// Load required scripts
function loadScript(url) {
    return new Promise((resolve, reject) => {
        const script = document.createElement('script');
        script.src = url;
        script.onload = resolve;
        script.onerror = reject;
        document.head.appendChild(script);
    });
}

// Load vis.js and its CSS
document.head.innerHTML += '<link href="https://unpkg.com/vis-network/styles/vis-network.min.css" rel="stylesheet" type="text/css" />';
loadScript('https://unpkg.com/vis-network/standalone/umd/vis-network.min.js')
    .then(() => {
        console.log('vis.js loaded successfully');
    })
    .catch(error => {
        console.error('Error loading vis.js:', error);
    }); 