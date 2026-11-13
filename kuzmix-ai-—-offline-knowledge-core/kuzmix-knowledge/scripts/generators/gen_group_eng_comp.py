#!/usr/bin/env python3
"""
Group 2 Generator: Engineering & Computing (14 categories, ~588 concepts)
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from common import make_concept_obj, slugify

ENG_COMP_CATEGORIES = [
    # Engineering
    ("cat_eng_mechanical", "eng_tech", "Mechanical and Aerospace Engineering", [
        "Mechanical Advantage Levers", "Torque and Rotational Equilibrium", "Stress-Strain Tensile Curves", "Young's Modulus Elasticity",
        "Shear Stress and Modulus", "Factor of Safety Structural", "Beam Deflection Euler-Bernoulli", "Fatigue Failure Cyclic Loading",
        "Creep High Temperature Deformation", "Gears and Gear Train Ratios", "Planetary Epicyclic Gearboxes", "Bearings Ball Roller Friction",
        "Hydrodynamic Lubrication Oil", "Pascal's Hydraulic Force Transmission", "Pneumatic Compressed Air Systems", "Four-Stroke Otto Engine Cycle",
        "Diesel Compression Ignition Cycle", "Gas Turbine Brayton Cycle", "Compressors Centrifugal Axial", "Centrifugal Pumps Impeller",
        "Bernoulli Fluid Lift Airfoil", "Boundary Layer Aerodynamic Drag", "Reynolds Number Laminar Turbulent", "Mach Number Supersonic Shock",
        "Wind Tunnel Testing Models", "Vibration Damping Mass Spring", "Flywheel Kinetic Energy Storage", "Belt and Chain Power Drives",
        "Cams and Camshaft Followers", "Clutches Friction Torque Transfer", "Differential Vehicle Axle Drive", "Braking Thermal Energy Dissipation",
        "Finite Element Stress Analysis FEA", "Computational Fluid Dynamics CFD", "Rankine Steam Power Cycle", "Refrigeration Vapor Compression",
        "Heat Exchangers Counterflow LMTD", "Rocket De Laval Nozzle Expansion", "Specific Impulse Rocket Thrust", "Turbomachinery Blade Aerodynamics",
        "Piping Pressure Drop Darcy-Weisbach", "Mechanical Governor Speed Control"
    ]),
    ("cat_eng_electrical", "eng_tech", "Electrical and Electronics Engineering", [
        "Ohm's Law Resistance V=IR", "Kirchhoff's Current Law Node KCL", "Kirchhoff's Voltage Law Loop KVL", "Direct Current DC Constant Flow",
        "Alternating Current AC Sinusoidal", "Capacitor Electrostatic Charge", "Inductor Magnetic Energy Storage", "Complex Impedance RLC Circuits",
        "Resonance Tuning Tank Circuits", "Transformers Electromagnetic Mutual", "Three-Phase AC Power Distribution", "Semiconductor Silicon P-N Junction",
        "Diode Rectification AC to DC", "Zener Diode Voltage Regulation", "Bipolar Junction Transistor BJT", "MOSFET Field-Effect Transistor",
        "Operational Amplifier Op-Amp Feedback", "Inverting and Non-Inverting Op-Amps", "Comparator Threshold Detection", "Active and Passive Filter Circuits",
        "Analog-to-Digital Converter ADC", "Digital-to-Analog Converter DAC", "Pulse Width Modulation PWM Duty", "Logic Gates AND OR NOT NAND",
        "Flip-Flops Sequential Registers", "Printed Circuit Board PCB Traces", "Grounding and Shielding EMI Noise", "Transmission Lines Characteristic Z",
        "Skin Effect AC High Frequency", "Induction Motors Squirrel Cage", "Synchronous Motors Grid Lock", "Brushless DC Motors BLDC",
        "Stepper Motors Digital Steps", "Switched-Mode Power Supply SMPS", "Buck and Boost DC-DC Converters", "Linear Voltage Regulators 7805",
        "Silicon Controlled Rectifier SCR", "Optocoupler Galvanic Isolation", "Soldering Thermal Intermetallic", "Signal Integrity Jitter Eye Diagram",
        "Superheterodyne Radio Receiver", "Phase-Locked Loop PLL Frequency"
    ]),
    ("cat_eng_civil", "eng_tech", "Civil and Structural Engineering", [
        "Truss Analysis Method of Joints", "Truss Method of Sections", "Structural Statics Equilibrium", "Tension and Compression Columns",
        "Euler Column Buckling Critical Load", "Bending Moment Diagrams BMD", "Shear Force Diagrams SFD", "Reinforced Concrete Rebar Tensile",
        "Prestressed Concrete Tendons", "Concrete Slump Test Workability", "Portland Cement Hydration Cure", "Soil Mechanics Compaction Density",
        "Soil Bearing Capacity Foundations", "Shallow Footing Foundation Pad", "Deep Pile Foundations Bedrock", "Retaining Walls Lateral Earth Pressure",
        "Slope Stability Shear Failure", "Bridge Types Suspension Cable", "Cable-Stayed Bridge Towers", "Arch Bridges Compression Thrust",
        "Truss Bridges Cantilever Spans", "Culverts and Stormwater Drainage", "Asphalt Pavement Viscoelasticity", "Highway Super-elevation Banking",
        "Seismic Base Isolation Dampers", "Tuned Mass Damper Skyscraper", "Building Codes Dead and Live Loads", "Wind Load Aerodynamic Vortex Shed",
        "Thermal Expansion Joints Bridges", "Tunnel Boring Machines TBM Shield", "Dams Gravity Arch Embankment", "Spillways Hydrodynamic Overflow",
        "Geotextiles Soil Stabilization", "Settlement Differential Consolidation", "Surveying Total Station Theodolite", "Geographic Information Systems GIS",
        "Wastewater Treatment Sedimentation", "Activated Sludge Biological Aeration", "Water Distribution Pipe Networks", "Urban Stormwater Runoff Retention",
        "Structural Health Monitoring Sensors", "Corrosion Cathodic Rebar Protection"
    ]),
    ("cat_eng_materials", "eng_tech", "Materials Science and Metallurgy", [
        "Crystal Lattice FCC BCC HCP", "Unit Cell Miller Indices", "Point Defects Vacancies Interstitials", "Dislocations Plastic Slip Deformation",
        "Grain Boundaries Hall-Petch Strength", "Iron-Carbon Phase Diagram Steel", "Austenite Ferrite Pearlite Phases", "Martensite Quenching Hardening",
        "Tempering Steel Ductility Restore", "Annealing Recrystallization Soften", "Normalizing Grain Refinement", "Case Hardening Carburizing Surface",
        "Alloy Steels Chromium Nickel Moly", "Stainless Steel Passivation Layer", "Cast Iron Flake vs Spheroidal", "Aluminum Alloys Precipitation Hard",
        "Titanium Alloys Aerospace Strength", "Copper Alloys Brass Bronze Conduct", "Polymers Thermoplastic vs Thermoset", "Polymer Crystallinity Glass Temp Tg",
        "Elastomers Rubber Cross-Linking", "Ceramics Brittle Fracture Toughness", "Sintering Powder Metallurgy Heat", "Composite Materials Fiber Matrix",
        "Carbon Fiber Reinforced Polymer CFRP", "Fiberglass Composite Glass Resin", "Corrosion Galvanic Anode Cathode", "Pitting and Crevice Corrosion",
        "Stress Corrosion Cracking Tensile", "Oxidation High Temperature Scaling", "Hardness Testing Rockwell Brinell", "Charpy Impact Notch Toughness",
        "Tensile Testing Yield Ultimate", "Creep Testing Rupture Larson-Miller", "Fatigue S-N Wöhler Endurance Limit", "Thermal Conductivity Phonons Free",
        "Electrical Resistivity Metals Ohm", "Dielectric Breakdown Voltage Insulate", "Magnetic Hysteresis Coercivity B-H", "Superconductors Type I and Type II",
        "Shape Memory Alloys Nitinol Phase", "Nanomaterials Graphene Carbon Nanotubes"
    ]),
    ("cat_eng_robotics", "eng_tech", "Robotics and Autonomous Control Systems", [
        "Open-Loop vs Closed-Loop Control", "Feedback Control Loop Error Signals", "Negative Feedback Stability Gain", "PID Controller Proportional Action",
        "PID Integral Action Steady Error", "PID Derivative Action Anticipation", "PID Tuning Ziegler-Nichols Method", "Transfer Functions Laplace Domain",
        "Bode Plots Gain Phase Margins", "Root Locus Pole Placement Stability", "Nyquist Stability Criterion Encircle", "State-Space Representation Matrices",
        "Kalman Filter Sensor Fusion Estimate", "Sensors Encoders Optical Magnetic", "Inertial Measurement Units IMU Gyro", "Ultrasonic Distance Time-of-Flight",
        "LIDAR Light Detection Laser Scan", "Computer Vision Depth Stereo Camera", "Actuators Electric Motors Servos", "Brushless DC Servo Actuation",
        "Pneumatic and Hydraulic Robotic Muscles", "Forward Kinematics Denavit-Hartenberg", "Inverse Kinematics Analytical Numeric", "Jacobian Velocity Kinematics Singularity",
        "Degrees of Freedom Robotic Arm 6DOF", "End-Effector Grippers Vacuum Magnetic", "Trajectory Generation Splines Motion", "Path Planning A* Grid Navigation",
        "Rapidly-exploring Random Trees RRT", "Simultaneous Localization SLAM", "Robot Operating System ROS Nodes", "Mobile Robots Differential Drive",
        "Ackermann Steering Geometry Wheels", "Omnidirectional Mecanum Wheel Vectors", "Bipedal Locomotion Zero Moment Point", "Industrial Articulated Arms SCARA",
        "Delta Robots High-Speed Pick Place", "Collaborative Robots Cobots Force Limit", "Haptic Feedback Teleoperation Force", "Safety Interlocks Light Curtains E-Stop",
        "Microcontrollers Embedded Real-Time", "Programmable Logic Controllers PLC Ladder"
    ]),
    ("cat_eng_energy", "eng_tech", "Energy Systems and Power Generation", [
        "Photovoltaic Solar Cell P-N Effect", "Solar Inverters DC to AC Grid Tie", "Maximum Power Point Tracking MPPT", "Wind Turbine Aerodynamics Betz Limit",
        "Wind Generator Pitch Yaw Control", "Hydroelectric Turbines Francis Pelton", "Pumped-Storage Hydroelectric Battery", "Thermal Power Plant Steam Turbines",
        "Combined Cycle Gas Turbines CCGT", "Nuclear Fission Pressurized Water PWR", "Boiling Water Nuclear Reactor BWR", "Nuclear Fuel Uranium Enrichment Rods",
        "Nuclear Reactor Control Rods Boron", "Geothermal Energy Binary Cycle Flash", "Biomass Combustion Anaerobic Methane", "Ocean Tidal Barrage Stream Turbines",
        "Ocean Wave Energy Converters Float", "Lithium-Ion Battery Intercalation", "Lithium Iron Phosphate LiFePO4 Safety", "Nickel Manganese Cobalt NMC Density",
        "Battery Management System BMS Cells", "Depth of Discharge DoD Cycle Life", "Thermal Runaway Battery Fire Risk", "Hydrogen Fuel Cells Proton Exchange",
        "Hydrogen Electrolysis Water Splitting", "Hydrogen Storage Compression Cryo", "Electrical Grid Frequency Regulation", "Base Load vs Peaking Power Plants",
        "Smart Grid Demand Response Metering", "High-Voltage Direct Current HVDC Lines", "Three-Phase Power Transmission Towers", "Substation Transformers Step-Up Down",
        "Switchgear Circuit Breakers SF6 Gas", "Power Factor Correction Capacitors", "Supercapacitors Electrostatic Storage", "Energy Return on Energy Invested EROI",
        "Levelized Cost of Electricity LCOE", "Carbon Capture and Storage CCS Flue", "Cogeneration Combined Heat and Power", "Thermal Energy Storage Molten Salt",
        "Solid-State Batteries Ceramic Lithium", "Microgrids Islanding Autonomous Power"
    ]),

    # Computing
    ("cat_cs_foundations", "computing_ai", "Computer Science Foundations and Theory", [
        "Turing Machine Formal Definition", "Universal Turing Machine Programmable", "Church-Turing Thesis Computability", "Decidability and Undecidable Problems",
        "The Halting Problem Diagonal Proof", "Deterministic Finite Automata DFA", "Nondeterministic Finite Automata NFA", "Regular Expressions Kleene Algebra",
        "Context-Free Grammars CFG Backus", "Pushdown Automata Stack Grammar", "Chomsky Hierarchy of Formal Languages", "P vs NP Complexity Classes",
        "NP-Completeness Cook-Levin Theorem", "NP-Hard Problems Reduction Proofs", "Polynomial Time Reductions Karp", "Boolean Satisfiability Problem SAT",
        "3-SAT Conjunctive Normal Form", "Traveling Salesperson Problem Complexity", "Knapsack Problem Dynamic Intractable", "Big-O Upper Asymptotic Bound",
        "Big-Omega Lower Asymptotic Bound", "Big-Theta Tight Asymptotic Bound", "Little-o and Little-omega Strict", "Master Theorem for Divide-and-Conquer",
        "Amortized Time Complexity Analysis", "Accounting Method Amortized Cost", "Potential Function Physicist Method", "Von Neumann Computer Architecture",
        "Harvard Memory Architecture Separate", "Instruction Set Architecture ISA CISC", "Reduced Instruction Set Computer RISC", "Registers CPU Accumulator Program Counter",
        "Arithmetic Logic Unit ALU Flags", "IEEE 754 Floating-Point Precision", "Two's Complement Binary Signed Integer", "Endianness Big vs Little Byte Order",
        "Bitwise Operations Shift AND OR XOR", "Boolean Logic De Morgan Duals", "Shannon Information Entropy Bits", "Source Coding Theorem Compression Limit",
        "Channel Capacity Shannon-Hartley Law", "Error-Detecting Parity and Hamming Codes"
    ]),
    ("cat_cs_dsa", "computing_ai", "Data Structures and Core Algorithms", [
        "Array Contiguous Memory Indexing", "Dynamic Array Vector Doubling O(1)", "Singly Linked List Nodes Pointers", "Doubly Linked List Forward Backward",
        "Stack LIFO Operations Push Pop", "Queue FIFO Enqueue Dequeue Buffer", "Circular Ring Buffer Fixed Memory", "Hash Table Buckets Hash Function",
        "Separate Chaining Linked List Hash", "Open Addressing Linear Probing Hash", "Quadratic Probing and Double Hashing", "Load Factor Rehashing Table Resize",
        "Binary Search Sorted Array O(log n)", "Binary Search Tree BST Inorder", "AVL Tree Self-Balancing Height Rotations", "Red-Black Tree Balance Invariants",
        "B-Tree Multiway Disk Paging Node", "B+ Tree Leaf Linked Range Scans", "Binary Heap Min-Max Priority Queue", "Heapify Operation Linear Time O(n)",
        "Trie Prefix Tree String Dictionary", "Radix Tree Compact Trie Compression", "Segment Tree Range Query Updates", "Fenwick Binary Indexed Tree BIT",
        "Disjoint Set Union DSU Path Compress", "Bloom Filter Probabilistic Set Query", "Skip List Probabilistic Balanced Search", "Bubble Sort Elementary Inversion",
        "Insertion Sort Adaptive Small Arrays", "Selection Sort Minimum Swap O(n2)", "Mergesort Divide-and-Conquer Stable", "Quicksort Pivot Partitioning Average",
        "Heapsort In-Place Selection O(nlogn)", "Counting Sort Non-Comparative O(n)", "Radix Sort Digit Bucket Linear", "Breadth-First Search BFS Queue Shortest",
        "Depth-First Search DFS Stack Backtrack", "Dijkstra Shortest Path Greedy Heap", "A* Search Heuristic Admissible Manhat", "Bellman-Ford Negative Weight Cycle",
        "Floyd-Warshall All-Pairs O(n3)", "Topological Sort Directed Acyclic Graph"
    ]),
    ("cat_cs_systems", "computing_ai", "Operating Systems and Kernel Architecture", [
        "Process vs Thread Execution Context", "Process Control Block PCB State", "Context Switching CPU Register Save", "Process Scheduling Preemptive Priority",
        "Round-Robin Scheduling Time Quantum", "Completely Fair Scheduler CFS Linux", "Multi-Level Feedback Queue MLFQ", "Inter-Process Communication IPC Pipes",
        "Shared Memory IPC Synchronization", "Message Queues Unix Sockets IPC", "Race Conditions Critical Section", "Mutual Exclusion Mutex Lock Unlock",
        "Semaphores Counting Binary Dijkstra", "Spinlocks vs Blocking Mutexes", "Deadlock Four Necessary Conditions", "Deadlock Prevention Banker's Algorithm",
        "Deadlock Detection and Recovery Kill", "Priority Inversion Inheritance Protocol", "Virtual Memory Address Space Mapping", "Paging Page Tables Page Directory",
        "Translation Lookaside Buffer TLB Cache", "Page Faults Disk Demand Paging", "Page Replacement LRU Clock FIFO", "Thrashing Virtual Memory Collapse",
        "Memory-Mapped Files mmap Zero-Copy", "Kernel Space vs User Space Ring 0", "System Calls trap switch interrupt", "Hardware Interrupts ISR Vector Table",
        "Direct Memory Access DMA Controller", "CPU Cache Levels L1 L2 L3 Hierarchy", "Cache Coherence MESI Protocol Snooping", "Virtual File System VFS Abstraction",
        "Inodes Unix File Metadata Links", "Hard Links vs Symbolic Soft Links", "Journaling File Systems Ext4 NTFS", "Write-Ahead Logging File Crash Safety",
        "Solid State Drive SSD Flash FTL Wear", "TRIM Command SSD Garbage Collection", "RAID 0 Striping Performance", "RAID 1 Mirroring Redundancy",
        "RAID 5 Distributed Parity Disk", "Device Drivers Character Block Network"
    ]),
    ("cat_cs_networking", "computing_ai", "Computer Networking and Protocols", [
        "OSI 7-Layer Theoretical Model", "TCP/IP 4-Layer Internet Protocol Stack", "Encapsulation Packet Headers Trailers", "Ethernet Layer 2 MAC Addressing",
        "Address Resolution Protocol ARP Cache", "Internet Protocol IPv4 Addressing Subnets", "Subnet Masking CIDR Prefix Notation", "IPv6 128-Bit Address Hex Colons",
        "Dynamic Host Configuration Protocol DHCP", "Transmission Control Protocol TCP Streams", "TCP Three-Way Handshake SYN ACK", "TCP Four-Way Teardown FIN ACK",
        "TCP Flow Control Sliding Window", "TCP Congestion Control AIMD Tahoe Reno", "User Datagram Protocol UDP Datagrams", "Domain Name System DNS Hierarchical",
        "DNS Root TLD Authoritative Resolver", "DNS Record Types A AAAA CNAME MX", "Hypertext Transfer Protocol HTTP 1.1", "HTTP Request Methods GET POST PUT",
        "HTTP Status Codes 200 404 500", "HTTP Headers Cookies Sessions State", "HTTP/2 Multiplexing Binary Framing", "HTTP/3 QUIC Protocol UDP Multiplex",
        "Transport Layer Security TLS SSL Handshake", "Public Key Infrastructure PKI CA Certs", "Symmetric Session Encryption AES-GCM", "Diffie-Hellman Key Exchange Ephemeral",
        "Border Gateway Protocol BGP Autonomous", "Open Shortest Path First OSPF Link-State", "Routing Information Protocol RIP Distance", "Network Address Translation NAT Port PAT",
        "Dynamic vs Static Routing Tables", "Internet Control Message Protocol ICMP", "Ping and Traceroute TTL Expiry Probe", "Virtual Local Area Network VLAN Tagging",
        "Network Firewalls Packet Stateful Proxy", "Virtual Private Network VPN Tunneling IPSec", "WireGuard Lightweight VPN Encryption", "Software-Defined Networking SDN OpenFlow",
        "Content Delivery Networks CDN Edge Cache", "WebSockets Full-Duplex TCP Connection"
    ]),
    ("cat_cs_distributed", "computing_ai", "Distributed Systems and Cloud Architecture", [
        "CAP Theorem Consistency Availability Partition", "PACELC Extended Distributed Tradeoff", "Fallacies of Distributed Computing Deutsch", "ACID vs BASE Eventual Consistency",
        "Strict Serializability Multi-Node", "Eventual Consistency Convergence Gossip", "Vector Clocks Lamport Timestamps Order", "Two-Phase Commit 2PC Protocol Prepare",
        "Three-Phase Commit 3PC Non-Blocking", "Paxos Distributed Consensus Lamport", "Raft Consensus Leader Election Log", "Quorum Reads and Writes Majority NWR",
        "Consistent Hashing Ring Node Partition", "Distributed Hash Tables DHT Kademlia", "Database Sharding Horizontal Partitioning", "Primary-Replica Master-Slave Replication",
        "Multi-Master Active-Active Replication", "Read Replicas Query Load Offload", "Data Replication Lag Synchronization", "Message Brokers Publish-Subscribe Queue",
        "Apache Kafka Distributed Log Partition", "RabbitMQ AMQP Broker Exchange Routing", "Idempotence API Operations Safe Retry", "Dead Letter Queues DLQ Poison Messages",
        "Load Balancing Round-Robin Least-Conn", "Consistent Hash Load Balancer IP-Hash", "Reverse Proxy Nginx Envoy Termination", "API Gateways Routing Rate Limiting Auth",
        "Microservices Architecture Decoupled", "Service Discovery Consul Eureka DNS", "Circuit Breaker Pattern Fault Isolation", "Distributed Tracing Jaeger OpenTelemetry",
        "Saga Pattern Distributed Transactions", "Event Sourcing Immutable State Append", "CQRS Command Query Responsibility Separate", "Serverless Functions FaaS Cold Starts",
        "Object Storage S3 Blob Store Buckets", "MapReduce Distributed Data Processing", "Apache Spark In-Memory Resilient RDD", "Disaster Recovery RPO RTO Target Metrics",
        "Chaos Engineering Fault Injection Gremlin", "Container Orchestration Kubernetes Pods"
    ]),
    ("cat_cs_security", "computing_ai", "Cybersecurity and Cryptography", [
        "Confidentiality Integrity Availability CIA", "Symmetric Key Encryption Block Stream", "Advanced Encryption Standard AES Rijndael", "Block Cipher Modes CBC CTR GCM",
        "Initialization Vectors IV Nonce Random", "Asymmetric Public Key Cryptography", "RSA Algorithm Prime Factorization Trap", "Elliptic Curve Cryptography ECC Curves",
        "Diffie-Hellman Key Agreement Protocol", "Cryptographic Hash Functions Collision Preimage", "SHA-256 Secure Hash Algorithm Merkle", "HMAC Hash-Based Message Authentication",
        "Digital Signatures Non-Repudiation Key", "Public Key Infrastructure PKI X509", "Certificate Authorities CA Chain of Trust", "Certificate Revocation List CRL OCSP",
        "Password Hashing Salted Bcrypt Argon2", "Multi-Factor Authentication MFA TOTP", "Single Sign-On SSO OAuth2 OIDC Tokens", "JSON Web Tokens JWT Claims Signature",
        "Access Control Models RBAC ABAC DAC", "Principle of Least Privilege Security", "Zero Trust Architecture Never Trust Verify", "SQL Injection Parameterized Queries Prep",
        "Cross-Site Scripting XSS Stored DOM", "Cross-Site Request Forgery CSRF Tokens", "Buffer Overflow Stack Smashing Canaries", "Return-Oriented Programming ROP Chains",
        "Denial of Service DoS DDoS SYN Flood", "Man-in-the-Middle Attacks MITM ARP Poison", "Phishing Social Engineering Deception", "Ransomware Cryptographic File Extortion",
        "Zero-Day Vulnerabilities Exploits Patch", "Penetration Testing Ethical Hacking Red", "Security Information Event Management SIEM", "Intrusion Detection Systems IDS Snort",
        "Homomorphic Encryption Compute Ciphertext", "Zero-Knowledge Proofs zk-SNARKs Verifier", "Quantum-Resistant Lattice Cryptography", "Side-Channel Attacks Timing Power Cache",
        "Hardware Security Modules HSM Secure Enclave", "End-to-End Encryption E2EE Signal Protocol"
    ]),
    ("cat_cs_software", "computing_ai", "Software Engineering and Architecture", [
        "Software Development Life Cycle SDLC", "Agile Methodology Iterative Sprints", "Scrum Framework Daily Standup Retrospective", "Kanban Work-in-Progress WIP Limits",
        "Waterfall Sequential Development Model", "SOLID Principles Single Responsibility", "Open-Closed Principle Polymorphic Extension", "Liskov Substitution Principle Subtypes",
        "Interface Segregation Principle Focused", "Dependency Inversion Inversion of Control", "Design Patterns Gang of Four GoF", "Singleton Pattern Controlled Instance",
        "Factory Method Creational Decoupling", "Observer Pattern Event Listener Publish", "Strategy Pattern Interchangeable Algorithms", "Adapter Pattern Wrapper Compatibility",
        "Decorator Pattern Dynamic Behavior Add", "Facade Pattern Simplified High-Level API", "Model-View-Controller MVC Architecture", "Domain-Driven Design DDD Ubiquitous Bounded",
        "RESTful API Architectural Constraints", "GraphQL Single Endpoint Schema Queries", "gRPC Protocol Buffers HTTP2 RPC", "Webhooks Asynchronous Event Push HTTP",
        "Unit Testing Isolation Mocking Stubs", "Integration Testing Component Contracts", "Test-Driven Development TDD Red Green", "End-to-End E2E Automated Browser Tests",
        "Code Coverage Line Branch Statement", "Continuous Integration CI Automated Build", "Continuous Delivery and Deployment CD", "Version Control Systems Git DAG Commits",
        "Git Branching Strategies GitFlow Trunk", "Semantic Versioning SemVer Major Minor", "Static Code Analysis Linting AST Rules", "Technical Debt Refactoring Code Smells",
        "Profiling CPU Memory Flame Graphs", "Memory Leak Garbage Collection Mark Sweep", "Containerization Docker Namespaces cgroups", "Infrastructure as Code IaC Terraform",
        "Database Migrations Schema Versioning", "Twelve-Factor App Cloud Architecture"
    ]),
    ("cat_cs_ai_ml", "computing_ai", "Artificial Intelligence and Machine Learning", [
        "Supervised vs Unsupervised Learning", "Reinforcement Learning Reward Markov", "Linear Regression Ordinary Least Squares", "Logistic Regression Sigmoid Odds Logit",
        "Decision Trees Information Gain Gini", "Random Forests Bagging Ensemble Trees", "Gradient Boosting XGBoost LightGBM", "Support Vector Machines SVM Hyperplanes",
        "Kernel Trick Non-Linear Projection", "K-Means Clustering Centroids Iterative", "Principal Component Analysis PCA Variance", "Overfitting vs Underfitting Bias-Variance",
        "Regularization L1 Lasso Feature Select", "Regularization L2 Ridge Weight Shrink", "Cross-Validation K-Fold Train Test Split", "Evaluation Metrics Precision Recall F1",
        "ROC Curve Receiver Operating Characteristic", "Loss Functions Mean Squared Error MSE", "Cross-Entropy Loss Log Loss Probability", "Gradient Descent Batch Stochastic Mini",
        "Learning Rate Hyperparameter Tuning", "Adam Optimizer Adaptive Momentum RMSprop", "Artificial Neural Networks Multi-Layer", "Perceptron Threshold Linear Unit",
        "Activation Functions ReLU Leaky Sigmoid", "Backpropagation Calculus Chain Rule", "Vanishing and Exploding Gradient Problem", "Batch Normalization Internal Covariate",
        "Dropout Regularization Co-Adaptation", "Convolutional Neural Networks CNN Kernels", "Pooling Layers Max Average Downsample", "Recurrent Neural Networks RNN Feedback",
        "Long Short-Term Memory LSTM Gates Memory", "Gated Recurrent Unit GRU Reset Update", "Sequence-to-Sequence Encoder-Decoder", "Attention Mechanism Bahdanau Alignment",
        "Transformer Architecture Self-Attention", "Scaled Dot-Product Attention Vaswani", "Multi-Head Attention Semantic Subspaces", "Positional Encoding Sinusoidal RoPE",
        "Retrieval-Augmented Generation RAG Ground", "Vector Embeddings Cosine Similarity Dense"
    ])
]

def generate_group_eng_comp():
    concepts = []
    for cat_id, dom_id, cat_name, terms in ENG_COMP_CATEGORIES:
        for i, term in enumerate(terms):
            slug = slugify(term)
            cid = f"c_{cat_id}_{slug}"
            name = term
            defn = f"A foundational engineering or computing concept within {cat_name}: {term}."
            expl = f"{term} is critical in {cat_name}. It provides rigorous formal methodology, architecture, and empirical implementation guidelines for building scalable, reliable, and secure technological systems."
            
            prev_rel = []
            if i > 0:
                prev_term = terms[i-1]
                prev_slug = slugify(prev_term)
                prev_rel.append((f"c_{cat_id}_{prev_slug}", "related_to", f"Interconnected with {prev_term} in {cat_name}.", 0.9))
                
            concepts.append(make_concept_obj(
                cid, dom_id, cat_id, name, defn, expl,
                [("Discipline", cat_name), ("Domain", "Engineering and Technology" if dom_id == "eng_tech" else "Computing and AI")],
                ["Industrial software and hardware systems design.", "Computational performance optimization."],
                [], "intermediate", "src_clrs_algorithms" if dom_id == "computing_ai" else "src_norman_design",
                [name.replace(" and ", " & "), name.split()[0]],
                [(f"Common intuition often oversimplifies {name}.", f"{name} follows exact technical and architectural constraints.", "Rigorous technical specifications govern real-world implementations.")],
                [("principle", f"Core Principle of {name}", defn)],
                [],
                prev_rel
            ))
            
    return concepts

if __name__ == "__main__":
    c = generate_group_eng_comp()
    print(f"✓ Generated {len(c)} concepts for Engineering & Computing Group.")
