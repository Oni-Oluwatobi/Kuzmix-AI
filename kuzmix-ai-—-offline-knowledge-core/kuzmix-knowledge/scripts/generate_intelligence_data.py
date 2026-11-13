#!/usr/bin/env python3
"""
Kuzmix AI Offline Knowledge Core - Intelligence & Depth Generator
Generates:
1. Disambiguations (Polysemous terms across domains)
2. Comparisons (Structured concept vs concept matrices)
3. Procedures (Step-by-step algorithms, troubleshooting & methods)
4. Concept Families (Semantic clusters & learning progressions)
5. Cross-Domain Knowledge Links (Inter-faculty bridges)
6. Embedding Metadata (Vector hybrid retrieval readiness)
"""

import os
import json
import glob

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")
CONCEPTS_DIR = os.path.join(DATA_DIR, "concepts")

# Load existing concepts to ensure 100% valid foreign keys
all_concepts = {}
for p in sorted(glob.glob(os.path.join(CONCEPTS_DIR, "*.json"))):
    with open(p, "r", encoding="utf-8") as f:
        for c in json.load(f):
            all_concepts[c["id"]] = c

def find_concept(keywords, category_sub=None, domain_id=None):
    best_id = None
    best_score = -1
    for cid, c in all_concepts.items():
        if domain_id and c["domain_id"] != domain_id:
            continue
        if category_sub and category_sub not in c["category_id"]:
            continue
        score = 0
        name_lower = c["name"].lower()
        cid_lower = cid.lower()
        for kw in keywords:
            kw_lower = kw.lower()
            if kw_lower == name_lower:
                score += 15
            elif kw_lower in name_lower:
                score += 5
            if kw_lower in cid_lower:
                score += 4
            if kw_lower in c.get("summary_definition", "").lower():
                score += 1
        if score > best_score and score > 0:
            best_score = score
            best_id = cid
    return best_id

print(f"Loaded {len(all_concepts)} concepts for intelligence data synthesis.")

# ----------------------------------------------------------------------
# 1. DISAMBIGUATIONS
# ----------------------------------------------------------------------
disambiguations_raw = [
    {
        "term": "Cell",
        "meanings": [
            {
                "keywords": ["cell theory", "cellular"],
                "category": "cat_bio_cellular",
                "domain": "sci_math",
                "context": "Biology & Life Sciences: The fundamental microscopic structural and functional unit of all living organisms.",
                "hint": "Referencing biological membranes, mitochondria, DNA replication, or cytology."
            },
            {
                "keywords": ["battery", "galvanic", "electrochemical"],
                "category": "cat_eng_energy",
                "domain": "eng_tech",
                "context": "Electrical Engineering & Chemistry: An electrochemical power source consisting of anode, cathode, and electrolyte.",
                "hint": "Referencing voltage, galvanic potential, discharge rates, or battery chemistry."
            },
            {
                "keywords": ["photovoltaic", "solar cell"],
                "category": "cat_eng_energy",
                "domain": "eng_tech",
                "context": "Renewable Energy: A semiconductor p-n junction device converting sunlight directly into electricity via photovoltaic effect.",
                "hint": "Referencing solar panels, photon absorption, or clean energy generation."
            },
            {
                "keywords": ["unit cell", "crystal lattice"],
                "category": "cat_eng_materials",
                "domain": "eng_tech",
                "context": "Materials Science: The smallest repeating geometric unit in a crystal lattice.",
                "hint": "Referencing crystallography, Miller indices, or body-centered cubic structures."
            }
        ]
    },
    {
        "term": "Field",
        "meanings": [
            {
                "keywords": ["electric field", "gauss"],
                "category": "cat_physics_electromagnetism",
                "domain": "sci_math",
                "context": "Physics & Electromagnetism: A physical quantity with a vector or scalar value assigned to every point in spacetime.",
                "hint": "Referencing gravitational, electric, or magnetic force distributions."
            },
            {
                "keywords": ["galois fields", "finite fields"],
                "category": "cat_math_algebra",
                "domain": "sci_math",
                "context": "Abstract Algebra & Mathematics: An algebraic structure where addition, subtraction, multiplication, and division are defined.",
                "hint": "Referencing modular arithmetic, rings, Galois theory, or cryptography."
            }
        ]
    },
    {
        "term": "Current",
        "meanings": [
            {
                "keywords": ["electric current", "amperes"],
                "category": "cat_physics_electromagnetism",
                "domain": "sci_math",
                "context": "Physics & Electrical Circuits: The rate of flow of electric charge through a conductor (measured in Amperes).",
                "hint": "Referencing Ohm's law, voltage, electrons, or AC/DC power."
            },
            {
                "keywords": ["coriolis", "ocean", "atmosphere"],
                "category": "cat_earth_meteorology",
                "domain": "sci_math",
                "context": "Oceanography & Earth Science: Continuous, directed movement of seawater generated by wind, Coriolis effect, or temperature gradients.",
                "hint": "Referencing the Gulf Stream, oceanic conveyor belts, or thermohaline circulation."
            }
        ]
    },
    {
        "term": "Model",
        "meanings": [
            {
                "keywords": ["artificial intelligence", "transformer", "neural network"],
                "category": "cat_cs_ai_ml",
                "domain": "computing_ai",
                "context": "Machine Learning & AI: A mathematical function parameterized by weights trained to approximate patterns from data.",
                "hint": "Referencing weights, loss functions, training sets, transformers, or inference."
            },
            {
                "keywords": ["scientific method", "falsifiability"],
                "category": "cat_hum_philosophy",
                "domain": "humanities",
                "context": "Philosophy of Science: An idealized intellectual or conceptual representation used to explain and predict empirical phenomena.",
                "hint": "Referencing scientific theories, hypotheses, approximations, or predictive simulations."
            },
            {
                "keywords": ["business model", "value proposition"],
                "category": "cat_biz_strategy",
                "domain": "business_econ",
                "context": "Business Strategy: A plan for how a company creates, delivers, and captures economic value.",
                "hint": "Referencing revenue streams, unit economics, value propositions, or cost structures."
            }
        ]
    },
    {
        "term": "Kernel",
        "meanings": [
            {
                "keywords": ["kernel space", "operating systems"],
                "category": "cat_cs_systems",
                "domain": "computing_ai",
                "context": "Operating Systems: The foundational core program managing hardware resources, memory, and CPU processes.",
                "hint": "Referencing monolithic vs microkernels, system calls, interrupt handlers, or Ring 0."
            },
            {
                "keywords": ["linear algebra", "vector spaces", "rank nullity"],
                "category": "cat_math_algebra",
                "domain": "sci_math",
                "context": "Linear Algebra: The set of domain vectors mapped to the zero vector by a linear transformation (the null space).",
                "hint": "Referencing nullity, rank-nullity theorem, or matrix equations Av = 0."
            }
        ]
    },
    {
        "term": "Matrix",
        "meanings": [
            {
                "keywords": ["matrices", "linear transformations"],
                "category": "cat_math_algebra",
                "domain": "sci_math",
                "context": "Mathematics: A rectangular array of numbers or expressions arranged in rows and columns representing linear maps.",
                "hint": "Referencing matrix multiplication, determinants, eigenvalues, or Gaussian elimination."
            },
            {
                "keywords": ["extracellular matrix", "collagen"],
                "category": "cat_bio_cellular",
                "domain": "sci_math",
                "context": "Cell Biology: A three-dimensional network of extracellular macromolecules providing structural and biochemical support to surrounding cells.",
                "hint": "Referencing collagen, proteoglycans, fibroblasts, or tissue scaffold."
            }
        ]
    },
    {
        "term": "Stress",
        "meanings": [
            {
                "keywords": ["stress strain", "youngs modulus"],
                "category": "cat_eng_materials",
                "domain": "eng_tech",
                "context": "Materials Science & Mechanics: Internal restorative force per unit cross-sectional area within a solid body (measured in Pascals).",
                "hint": "Referencing tensile stress, shear stress, Hooke's law, or Yield strength."
            },
            {
                "keywords": ["stress", "psychological", "cortisol", "cognitive"],
                "category": "cat_hum_psychology",
                "domain": "humanities",
                "context": "Psychology & Neurobiology: Physiological and psychological response to perceived environmental threats or excessive demands.",
                "hint": "Referencing the sympathetic nervous system, fight-or-flight, cortisol, or burnout."
            }
        ]
    },
    {
        "term": "Power",
        "meanings": [
            {
                "keywords": ["work energy", "joule", "watt"],
                "category": "cat_physics_mechanics",
                "domain": "sci_math",
                "context": "Physics & Engineering: The rate at which mechanical, electrical, or thermal work is performed (measured in Watts: 1 W = 1 J/s).",
                "hint": "Referencing dW/dt, horsepower, kilowatt-hours, or energy transfer rate."
            },
            {
                "keywords": ["separation of powers", "checks and balances"],
                "category": "cat_soc_civics",
                "domain": "society_culture",
                "context": "Civics & Political Science: The institutional authority and capacity of a governmental branch or sovereign to enforce laws and govern.",
                "hint": "Referencing trias politica, executive branch, sovereign legitimacy, or legislation."
            },
            {
                "keywords": ["power", "exponentiation", "logarithm"],
                "category": "cat_math_algebra",
                "domain": "sci_math",
                "context": "Mathematics: The mathematical operation of raising a base quantity to an exponent (b^n).",
                "hint": "Referencing powers of two, exponential growth, or polynomial degrees."
            }
        ]
    },
    {
        "term": "Charge",
        "meanings": [
            {
                "keywords": ["coulomb", "electrostatic", "charge"],
                "category": "cat_physics_electromagnetism",
                "domain": "sci_math",
                "context": "Physics: The fundamental physical property of matter that causes it to experience a force when placed in an electromagnetic field.",
                "hint": "Referencing electrons, protons, Coulombs, or electrostatic attraction."
            },
            {
                "keywords": ["rule of law", "legal", "due process"],
                "category": "cat_soc_law",
                "domain": "society_culture",
                "context": "Law & Jurisprudence: A formal criminal indictment asserting that an individual committed a specified unlawful offense.",
                "hint": "Referencing court arraignment, prosecution, indictment, or penal law."
            }
        ]
    },
    {
        "term": "Network",
        "meanings": [
            {
                "keywords": ["tcp", "networking", "packet"],
                "category": "cat_cs_networking",
                "domain": "computing_ai",
                "context": "Computer Science: A group of interconnected computational nodes communicating via shared protocols (e.g., TCP/IP).",
                "hint": "Referencing routers, IP addresses, Ethernet, packets, or OSI model."
            },
            {
                "keywords": ["graph theory", "networks"],
                "category": "cat_math_discrete",
                "domain": "sci_math",
                "context": "Discrete Mathematics: A graph composed of vertices (nodes) connected by edges (links) modeling pairwise relationships.",
                "hint": "Referencing graph algorithms, adjacency matrices, minimum spanning trees, or shortest paths."
            },
            {
                "keywords": ["social", "institutions", "sociology"],
                "category": "cat_soc_sociology",
                "domain": "society_culture",
                "context": "Sociology: A social structure made up of social actors (individuals or organizations) connected by dyadic ties.",
                "hint": "Referencing social capital, human connections, organizational hierarchies, or community ties."
            }
        ]
    },
    {
        "term": "Cloud",
        "meanings": [
            {
                "keywords": ["cloud computing", "distributed"],
                "category": "cat_cs_distributed",
                "domain": "computing_ai",
                "context": "Computing & Infrastructure: On-demand delivery of computing power, database storage, and applications via the internet.",
                "hint": "Referencing virtualization, multi-tenancy, AWS/GCP, Kubernetes, or serverless architectures."
            },
            {
                "keywords": ["cumulus", "meteorology", "atmosphere"],
                "category": "cat_earth_meteorology",
                "domain": "sci_math",
                "context": "Atmospheric Science: Visible mass of condensed water droplets or ice crystals suspended in the atmosphere.",
                "hint": "Referencing rain, condensation nuclei, troposphere, or weather fronts."
            }
        ]
    },
    {
        "term": "Root",
        "meanings": [
            {
                "keywords": ["polynomial", "algebra", "fundamental theorem of algebra"],
                "category": "cat_math_algebra",
                "domain": "sci_math",
                "context": "Mathematics: A value of the independent variable that makes a polynomial or equation equal to zero (f(x) = 0).",
                "hint": "Referencing zeros of functions, square roots, or complex roots."
            },
            {
                "keywords": ["tree", "binary tree", "data structures"],
                "category": "cat_cs_dsa",
                "domain": "computing_ai",
                "context": "Data Structures: The topmost node in a tree data hierarchy, which has no parent nodes.",
                "hint": "Referencing binary search trees, tree traversal, or DOM root."
            }
        ]
    },
    {
        "term": "Node",
        "meanings": [
            {
                "keywords": ["linked list", "graph theory", "tree"],
                "category": "cat_cs_dsa",
                "domain": "computing_ai",
                "context": "Computer Science: A basic data unit containing a value and one or more pointers/references to adjacent nodes.",
                "hint": "Referencing linked lists, trees, graphs, or memory pointers."
            },
            {
                "keywords": ["standing waves", "nodes", "acoustics"],
                "category": "cat_physics_mechanics",
                "domain": "sci_math",
                "context": "Physics & Wave Mechanics: A point along a standing wave where the wave has minimal or zero amplitude of oscillation.",
                "hint": "Referencing standing waves, antinodes, wave harmonics, or resonance."
            }
        ]
    },
    {
        "term": "Class",
        "meanings": [
            {
                "keywords": ["object oriented", "software engineering"],
                "category": "cat_cs_software",
                "domain": "computing_ai",
                "context": "Software Engineering: An extensible program-code template for creating objects, defining initial state and behaviors.",
                "hint": "Referencing encapsulation, inheritance, polymorphism, methods, and instantiations."
            },
            {
                "keywords": ["social class", "sociology", "institutions"],
                "category": "cat_soc_sociology",
                "domain": "society_culture",
                "context": "Sociology: A group of people within a society who possess the same socioeconomic status or property ownership.",
                "hint": "Referencing socioeconomic stratification, labor, capital, or social mobility."
            }
        ]
    },
    {
        "term": "Agent",
        "meanings": [
            {
                "keywords": ["artificial intelligence", "agent", "reinforcement learning"],
                "category": "cat_cs_ai_ml",
                "domain": "computing_ai",
                "context": "Artificial Intelligence: An autonomous entity that observes an environment through sensors and takes actions to maximize goals.",
                "hint": "Referencing policies, actions, states, Markov decision processes, or LLM tool use."
            },
            {
                "keywords": ["microeconomics", "market equilibrium", "opportunity cost"],
                "category": "cat_econ_micro",
                "domain": "business_econ",
                "context": "Economics: An individual actor or decision-maker (consumer, firm, or worker) in an economic model.",
                "hint": "Referencing utility maximization, rational choice theory, or principal-agent problem."
            }
        ]
    }
]

disambiguations_final = []
dis_id_counter = 1

for d in disambiguations_raw:
    term = d["term"]
    term_norm = term.lower().strip()
    for m in d["meanings"]:
        cid = find_concept(m["keywords"], category_sub=m["category"], domain_id=m["domain"])
        if not cid:
            # fallback find
            cid = find_concept(m["keywords"])
        if cid:
            concept = all_concepts[cid]
            disambiguations_final.append({
                "id": f"dis_{term_norm}_{dis_id_counter}",
                "term": term,
                "term_normalized": term_norm,
                "concept_id": cid,
                "domain_id": concept["domain_id"],
                "category_id": concept["category_id"],
                "distinguishing_context": m["context"],
                "disambiguation_hint": m["hint"],
                "sort_priority": dis_id_counter
            })
            dis_id_counter += 1

print(f"Generated {len(disambiguations_final)} disambiguation entries.")

with open(os.path.join(DATA_DIR, "disambiguations.json"), "w", encoding="utf-8") as f:
    json.dump(disambiguations_final, f, indent=2)

# ----------------------------------------------------------------------
# 2. COMPARISONS (Phase 14)
# ----------------------------------------------------------------------
comparisons_raw = [
    {
        "title": "Speed vs Velocity",
        "shared_aspect": "Kinematics: Quantifying the rate of positional motion over time.",
        "concept_a_kw": ["kinematics", "mechanics"],
        "concept_b_kw": ["orbital velocity", "mechanics"],
        "criteria": [
            {"criterion": "Mathematical Nature", "concept_a_val": "Scalar quantity (magnitude only, e.g. 50 m/s)", "concept_b_val": "Vector quantity (magnitude + direction, e.g. 50 m/s North)"},
            {"criterion": "Sign & Negative Values", "concept_a_val": "Strictly non-negative (>= 0)", "concept_b_val": "Can be negative depending on coordinate axis definition"},
            {"criterion": "Calculation Formula", "concept_a_val": "Total Distance / Elapsed Time", "concept_b_val": "Displacement Vector / Elapsed Time"},
            {"criterion": "Circular Motion Example", "concept_a_val": "Car on circular track at 60 km/h has constant speed", "concept_b_val": "Car on circular track has continuously changing velocity due to directional turning"}
        ],
        "common_confusion": "Everyday language uses them interchangeably, causing students to forget that turning at constant speed still involves non-zero acceleration.",
        "rule_of_thumb": "If direction matters (or can cancel out over a round trip returning to start), it is Velocity. If only magnitude matters, it is Speed."
    },
    {
        "title": "Mass vs Weight",
        "shared_aspect": "Classical Mechanics: Quantities associated with gravitational and inertial properties of matter.",
        "concept_a_kw": ["gravity and gravitation", "universal gravitation"],
        "concept_b_kw": ["newtons second law", "f=ma"],
        "criteria": [
            {"criterion": "Physical Definition", "concept_a_val": "Intrinsic measure of matter and inertia; resistance to acceleration", "concept_b_val": "Gravitational downward force exerted on that mass (W = m*g)"},
            {"criterion": "SI Units", "concept_a_val": "Kilograms (kg)", "concept_b_val": "Newtons (N)"},
            {"criterion": "Location Dependency", "concept_a_val": "Invariant across the universe (same on Earth, Moon, or in deep space)", "concept_b_val": "Varies directly with local gravitational acceleration (Moon weight is 1/6th of Earth)"},
            {"criterion": "Measurement Tool", "concept_a_val": "Balance scale (comparing unknown mass against reference masses)", "concept_b_val": "Spring scale or strain gauge (measuring gravitational pull)"}
        ],
        "common_confusion": "Scales in everyday commerce display kilograms for weight, confounding the force of gravity with intrinsic mass.",
        "rule_of_thumb": "Mass is what you are made of (does not change on the Moon); Weight is how hard gravity pulls on that mass."
    },
    {
        "title": "TCP vs UDP",
        "shared_aspect": "Computer Networking: Transport Layer (Layer 4) host-to-host data delivery protocols.",
        "concept_a_kw": ["transmission control protocol", "tcp"],
        "concept_b_kw": ["user datagram", "networking"],
        "criteria": [
            {"criterion": "Connection Paradigm", "concept_a_val": "Connection-oriented (3-way handshake SYN, SYN-ACK, ACK before transmission)", "concept_b_val": "Connectionless (fire-and-forget datagrams without pre-handshake)"},
            {"criterion": "Reliability & Ordering", "concept_a_val": "Guaranteed delivery, automatic packet retransmission, and in-order sequence assembly", "concept_b_val": "No guarantee of packet delivery, ordering, or deduplication"},
            {"criterion": "Overhead & Latency", "concept_a_val": "Higher latency (20-byte header, ACKs, flow control, congestion backoff)", "concept_b_val": "Minimal latency and low overhead (8-byte header, zero ACKs)"},
            {"criterion": "Ideal Use Cases", "concept_a_val": "Web browsing (HTTP/HTTPS), file transfer (FTP), email (SMTP), financial transactions", "concept_b_val": "Real-time gaming, live video streaming, DNS lookups, VoIP audio"}
        ],
        "common_confusion": "Assuming TCP is always better because it is 'reliable'—in real-time audio, waiting for retransmitted old packets causes unacceptable stutter.",
        "rule_of_thumb": "If every byte must arrive intact without corruption: use TCP. If speed matters more than losing an occasional frame: use UDP."
    },
    {
        "title": "Compiler vs Interpreter",
        "shared_aspect": "Computer Science: Translating high-level human-readable code into executable machine instructions.",
        "concept_a_kw": ["software engineering", "computer science"],
        "concept_b_kw": ["algorithms", "foundations"],
        "criteria": [
            {"criterion": "Translation Timing", "concept_a_val": "Ahead-Of-Time (AOT): Translates entire program once before runtime", "concept_b_val": "Just-In-Time (JIT) or line-by-line: Translates instructions dynamically as code executes"},
            {"criterion": "Output Artifact", "concept_a_val": "Standalone native binary executable (.exe, ELF, Mach-O)", "concept_b_val": "No standalone binary; requires interpreter runtime environment to execute source/bytecode"},
            {"criterion": "Execution Speed", "concept_a_val": "Very fast execution speed; deep compiler optimization passes (LLVM, GCC)", "concept_b_val": "Slower execution speed due to runtime interpretation overhead"},
            {"criterion": "Error Detection", "concept_a_val": "Catches syntax and static type errors completely before any code runs", "concept_b_val": "Syntax or runtime type errors encountered only when execution reaches that line"}
        ],
        "common_confusion": "Classifying programming languages as strictly 'compiled' or 'interpreted'—modern environments (Java JVM, Python PyPy, JavaScript V8) combine bytecode compilation with JIT interpretation.",
        "rule_of_thumb": "Compilers translate before you run; Interpreters translate as you run."
    },
    {
        "title": "Correlation vs Causation",
        "shared_aspect": "Statistics & Epistemology: Evaluating statistical relationships between observed variables.",
        "concept_a_kw": ["correlation", "statistics"],
        "concept_b_kw": ["scientific falsifiability", "falsifiability"],
        "criteria": [
            {"criterion": "Definition", "concept_a_val": "Statistical association where two variables move together predictably (e.g. Pearson r)", "concept_b_val": "Direct causal mechanism where altering variable X directly forces an outcome in variable Y"},
            {"criterion": "Confounding Factors", "concept_a_val": "Vulnerable to lurking third variables (e.g. ice cream sales and drowning both rise in summer)", "concept_b_val": "Controls for confounding variables via randomized controlled trials (RCTs)"},
            {"criterion": "Directionality", "concept_a_val": "Symmetric: corr(A, B) equals corr(B, A)", "concept_b_val": "Asymmetric: X causes Y does not imply Y causes X"},
            {"criterion": "Experimental Proof", "concept_a_val": "Can be demonstrated with observational data alone", "concept_b_val": "Requires experimental intervention, counterfactual analysis, or causal graphs"}
        ],
        "common_confusion": "Assuming that because two trends align graphically or statistically, one must be causing the other.",
        "rule_of_thumb": "Correlation shows that two things happened together; Causation proves that one made the other happen."
    },
    {
        "title": "Mitosis vs Meiosis",
        "shared_aspect": "Cellular Biology & Genetics: Nuclear division in eukaryotic cells.",
        "concept_a_kw": ["mitosis", "cellular division"],
        "concept_b_kw": ["meiosis", "haploid"],
        "criteria": [
            {"criterion": "Purpose & Function", "concept_a_val": "Growth, tissue repair, and asexual reproduction", "concept_b_val": "Production of gametes (sperm and egg cells) for sexual reproduction"},
            {"criterion": "Daughter Cells Produced", "concept_a_val": "2 identical diploid (2n) daughter cells", "concept_b_val": "4 genetically distinct haploid (n) daughter cells"},
            {"criterion": "Rounds of Division", "concept_a_val": "1 division cycle (Prophase, Metaphase, Anaphase, Telophase)", "concept_b_val": "2 sequential division cycles (Meiosis I and Meiosis II)"},
            {"criterion": "Genetic Recombination", "concept_a_val": "No crossing over; produces exact genetic clones", "concept_b_val": "Homologous crossing over and independent assortment generate high diversity"}
        ],
        "common_confusion": "Mixing up chromosome count outcomes, forgetting that meiosis halves the genetic complement so fertilization restores the diploid count.",
        "rule_of_thumb": "Mitosis makes identical copies for MY body (somatic); Meiosis makes unique cells for reproduction."
    },
    {
        "title": "Hypothesis vs Scientific Theory",
        "shared_aspect": "Philosophy of Science: Explanatory scientific propositions.",
        "concept_a_kw": ["hypothesis testing", "probability"],
        "concept_b_kw": ["scientific falsifiability", "demarcation"],
        "criteria": [
            {"criterion": "Scope & Maturity", "concept_a_val": "A tentative, testable proposed explanation for an empirical observation", "concept_b_val": "A comprehensive, extensively tested, and corroborated framework explaining nature"},
            {"criterion": "Evidentiary Backing", "concept_a_val": "May have minimal initial empirical data; awaiting testing", "concept_b_val": "Supported by vast bodies of independent empirical evidence across decades"},
            {"criterion": "Colloquial Confusion", "concept_a_val": "Matches the everyday word 'guess'", "concept_b_val": "NOT a guess; represents the highest tier of scientific certainty (e.g. Germ Theory, Plate Tectonics)"},
            {"criterion": "Falsifiability", "concept_a_val": "Must propose concrete predictions that could be proven false", "concept_b_val": "Continuously refined as new precise instruments challenge edge cases"}
        ],
        "common_confusion": "Saying 'it is only a theory' dismisses a well-tested scientific framework by confusing it with an unsubstantiated hypothesis.",
        "rule_of_thumb": "A hypothesis is a question waiting to be tested; a theory is an overarching explanation backed by mountains of evidence."
    },
    {
        "title": "RAM vs Secondary Storage",
        "shared_aspect": "Computer Architecture: Digital hardware for storing binary program data and instructions.",
        "concept_a_kw": ["array contiguous memory", "data structures"],
        "concept_b_kw": ["operating systems", "systems"],
        "criteria": [
            {"criterion": "Volatility", "concept_a_val": "Volatile: contents vanish instantly when electric power is lost", "concept_b_val": "Non-volatile: data persists indefinitely without electric power (NAND Flash / HDD)"},
            {"criterion": "Access Latency", "concept_a_val": "Ultra-fast direct CPU access: ~10 to 80 nanoseconds", "concept_b_val": "Slower block I/O access: ~10 to 100 microseconds (NVMe SSD) or milliseconds (HDD)"},
            {"criterion": "System Role", "concept_a_val": "Workspace holding actively executing OS code, open applications, and live runtime memory", "concept_b_val": "Archive holding installed software, user files, media, and dormant data"},
            {"criterion": "Cost per Gigabyte", "concept_a_val": "Significantly higher cost per gigabyte", "concept_b_val": "Low cost per gigabyte; orders of magnitude larger capacities"}
        ],
        "common_confusion": "Users saying their computer has '512 GB of memory' when they mean 512 GB of SSD storage and only 16 GB of RAM.",
        "rule_of_thumb": "RAM is your desk where you work right now; Storage is the filing cabinet where you keep files when you are done."
    }
]

comparisons_final = []
for idx, c in enumerate(comparisons_raw):
    cid_a = find_concept(c["concept_a_kw"])
    cid_b = find_concept(c["concept_b_kw"])
    if cid_a and cid_b and cid_a != cid_b:
        comparisons_final.append({
            "id": f"comp_{idx + 1:03d}",
            "concept_a_id": cid_a,
            "concept_b_id": cid_b,
            "title": c["title"],
            "shared_aspect": c["shared_aspect"],
            "key_differences_json": json.dumps(c["criteria"]),
            "common_confusion": c["common_confusion"],
            "rule_of_thumb": c["rule_of_thumb"]
        })

print(f"Generated {len(comparisons_final)} structured comparisons.")
with open(os.path.join(DATA_DIR, "comparisons.json"), "w", encoding="utf-8") as f:
    json.dump(comparisons_final, f, indent=2)

# ----------------------------------------------------------------------
# 3. PROCEDURES (Phase 15)
# ----------------------------------------------------------------------
procedures_raw = [
    {
        "concept_kw": ["cpr", "first aid", "cardiopulmonary"],
        "title": "Adult Cardiopulmonary Resuscitation (CPR) & AED Protocol",
        "goal": "Maintain vital cerebral and myocardial perfusion in an unresponsive victim of sudden cardiac arrest until defibrillation or EMS arrives.",
        "prereqs": ["c_cardiovascular_sys"],
        "steps": [
            {"step_number": 1, "title": "Scene Safety & Responsiveness Check", "instruction": "Verify the scene is safe for rescuers. Tap victim's shoulders firmly and shout 'Are you okay?'. Check for normal breathing for no more than 10 seconds.", "expected_outcome": "Determination of unresponsiveness and absent/agonal breathing."},
            {"step_number": 2, "title": "Activate Emergency Response & Retrieve AED", "instruction": "Direct a specific bystander to call local emergency services (911/112) and retrieve an Automated External Defibrillator (AED).", "expected_outcome": "EMS dispatch activated and AED en route."},
            {"step_number": 3, "title": "Position Hands on Center of Chest", "instruction": "Place heel of one hand on lower half of breastbone (sternum); interlock fingers of other hand on top. Lock elbows straight directly above victim's chest.", "expected_outcome": "Mechanical alignment for efficient bodyweight compressions."},
            {"step_number": 4, "title": "High-Quality Chest Compressions", "instruction": "Compress chest at rate of 100-120 beats per minute (to tempo of 'Stayin' Alive') to depth of at least 2 inches (5 cm) but no more than 2.4 inches (6 cm). Allow full chest recoil between compressions without leaning.", "expected_outcome": "Artificially generates ~25-30% of normal cardiac output, maintaining brain oxygenation."},
            {"step_number": 5, "title": "Deploy AED Immediately Upon Arrival", "instruction": "Power on AED, follow voice prompts, adhere pads to bare chest (upper right, lower left side), stand clear during rhythm analysis, and deliver shock if prompted before immediately resuming compressions.", "expected_outcome": "Terminates ventricular fibrillation (VF) through synchronous electrical depolarization."}
        ],
        "verification": "Check for return of spontaneous circulation (ROSC), coughing, movement, or arrival of advanced emergency medical personnel.",
        "troubleshooting": "If the chest fails to rise during rescue breaths, reposition the airway (head-tilt, chin-lift) to clear tongue obstruction; if AED pads are wet, dry chest vigorously before pad placement."
    },
    {
        "concept_kw": ["binary search", "dsa", "algorithms"],
        "title": "Binary Search Algorithm Execution",
        "goal": "Locate target value within a sorted array in O(log n) logarithmic time complexity.",
        "prereqs": ["c_big_o_notation"],
        "steps": [
            {"step_number": 1, "title": "Verify Sorted Precondition", "instruction": "Ensure array elements are in monotonically increasing order.", "expected_outcome": "Guarantees divide-and-conquer elimination property holds."},
            {"step_number": 2, "title": "Initialize Boundary Pointers", "instruction": "Set low pointer = 0 and high pointer = length - 1.", "expected_outcome": "Defines active search window covering entire array."},
            {"step_number": 3, "title": "Calculate Safe Midpoint", "instruction": "Compute mid = low + ((high - low) // 2) to avoid potential 32-bit integer overflow.", "expected_outcome": "Index of middle element of current subarray."},
            {"step_number": 4, "title": "Evaluate Target Match", "instruction": "Compare array[mid] with target: if equal, return mid index.", "expected_outcome": "Target found immediately."},
            {"step_number": 5, "title": "Halve Search Interval", "instruction": "If array[mid] < target, set low = mid + 1; else set high = mid - 1. Repeat from Step 3 while low <= high.", "expected_outcome": "Discards half of remaining search space per step."},
            {"step_number": 6, "title": "Handle Target Absence", "instruction": "If loop terminates with low > high without match, return -1 (not found).", "expected_outcome": "Definitive conclusion target is absent."}
        ],
        "verification": "Confirm returned index holds target value: array[index] === target.",
        "troubleshooting": "Using `mid = (low + high) / 2` can cause integer overflow in languages with fixed-width integers; off-by-one errors occur if `<` is used instead of `<=` in while condition."
    },
    {
        "concept_kw": ["scientific method", "falsifiability"],
        "title": "The Empirical Scientific Method",
        "goal": "Systematically formulate, test, and validate an objective explanation for natural phenomena.",
        "prereqs": ["c_scientific_falsifiability"],
        "steps": [
            {"step_number": 1, "title": "Systematic Observation", "instruction": "Identify an unexplained phenomenon and gather existing quantitative measurements and literature.", "expected_outcome": "Clear, measurable research question."},
            {"step_number": 2, "title": "Formulate Falsifiable Hypothesis", "instruction": "Propose a testable mechanism that predicts specific observable consequences that could be proven wrong.", "expected_outcome": "Empirically testable proposition with defined null and alternative hypotheses."},
            {"step_number": 3, "title": "Design Controlled Experiment", "instruction": "Isolate the independent variable, establish control groups, and keep all extraneous variables constant.", "expected_outcome": "Experimental setup minimizing confounding factors and bias."},
            {"step_number": 4, "title": "Data Collection & Replication", "instruction": "Conduct trials with sufficient sample size; record measurements blind to avoid experimenter expectancy bias.", "expected_outcome": "Statistically robust raw dataset."},
            {"step_number": 5, "title": "Statistical Analysis & Conclusion", "instruction": "Perform statistical significance tests (p-values, effect sizes). Accept or reject null hypothesis.", "expected_outcome": "Corroboration or falsification of hypothesis leading to iterative refinement."}
        ],
        "verification": "Peer-reviewed independent replication of findings by external laboratories.",
        "troubleshooting": "Beware of p-hacking (running multiple statistical tests until one produces p < 0.05 by chance) and confirmation bias (ignoring contradictory data)."
    },
    {
        "concept_kw": ["domestic electrical", "electrical safety", "circuit"],
        "title": "Domestic Branch Circuit Troubleshooting & Safety Isolation",
        "goal": "Safely locate and remedy a tripped circuit breaker or ground fault in domestic electrical wiring.",
        "prereqs": ["c_domestic_electrical", "c_ohms_law"],
        "steps": [
            {"step_number": 1, "title": "Verify Zero Voltage (Safety First)", "instruction": "Wear insulated footwear and use an approved Non-Contact Voltage Tester (NCVT) to confirm circuit is fully de-energized before touching wires.", "expected_outcome": "Verification that conductor carries 0 Volts AC."},
            {"step_number": 2, "title": "Inspect Main Electrical Panel", "instruction": "Locate tripped breaker (handle rests in middle position between ON and OFF).", "expected_outcome": "Identifies affected branch circuit."},
            {"step_number": 3, "title": "Unplug All Loads on Circuit", "instruction": "Disconnect all appliances, chargers, and lamps from all receptacles served by that breaker.", "expected_outcome": "Distinguishes external device fault from permanent behind-the-wall wiring short."},
            {"step_number": 4, "title": "Reset Breaker", "instruction": "Push breaker handle firmly to the full OFF position until it clicks, then push firmly to the full ON position.", "expected_outcome": "Resets the internal bimetallic/magnetic trip latch mechanism."},
            {"step_number": 5, "title": "Isolate Fault Source", "instruction": "If breaker trips immediately with nothing plugged in: wiring short circuit exists. If it holds, plug in devices one by one until breaker trips to identify faulty appliance.", "expected_outcome": "Pinpoints the exact source of overcurrent or ground fault."}
        ],
        "verification": "Circuit breaker remains energized under normal load; receptacle tester confirms correct hot, neutral, and ground polarity.",
        "troubleshooting": "Never replace a 15-Amp breaker with a 20-Amp breaker to stop tripping—this can cause 14 AWG wire behind walls to overheat and ignite house fires."
    }
]

procedures_final = []
for idx, p in enumerate(procedures_raw):
    cid = find_concept(p["concept_kw"])
    if cid:
        procedures_final.append({
            "id": f"proc_{idx + 1:03d}",
            "concept_id": cid,
            "title": p["title"],
            "goal": p["goal"],
            "prerequisites_json": json.dumps(p["prereqs"]),
            "steps_json": json.dumps(p["steps"]),
            "verification_step": p["verification"],
            "troubleshooting_notes": p["troubleshooting"]
        })

print(f"Generated {len(procedures_final)} structured procedures.")
with open(os.path.join(DATA_DIR, "procedures.json"), "w", encoding="utf-8") as f:
    json.dump(procedures_final, f, indent=2)

# ----------------------------------------------------------------------
# 4. CROSS-DOMAIN RELATIONSHIPS (Phase 11)
# ----------------------------------------------------------------------
cross_domain_bridges = [
    # Math -> Physics -> Engineering
    (["calculus", "derivative"], ["velocity", "kinematics"], "governed_by", "Calculus derivatives formally define instantaneous velocity as dx/dt.", 0.95),
    (["eigenvalues", "linear algebra"], ["quantum", "superposition"], "governed_by", "Observable physical states in quantum mechanics correspond to eigenvalues of Hermitian operators.", 0.95),
    (["singular value decomposition"], ["neural network", "transformer"], "used_for", "SVD enables low-rank matrix factorization (LoRA) for efficient LLM parameter adaptation.", 0.90),
    (["ohms law"], ["semiconductor", "electrical"], "prerequisite_of", "Ohm's law forms baseline linear relationship against which non-linear semiconductor diodes are modeled.", 0.95),
    # Chemistry -> Materials Science -> Energy
    (["covalent", "bonding"], ["polymerization", "materials"], "prerequisite_of", "Covalent electron sharing binds repeating monomer chains into structural polymers.", 0.90),
    (["electrochemical", "galvanic"], ["battery", "energy"], "application_of", "Galvanic cell redox reactions supply direct electrical current in lithium-ion and alkaline batteries.", 0.95),
    # Biology -> Medicine / Psychology
    (["action potential", "neuron"], ["synaptic", "neuroplasticity"], "causes", "Axonal action potentials trigger neurotransmitter exocytosis across synaptic junctions.", 0.95),
    (["working memory"], ["cognitive load", "psychology"], "prerequisite_of", "Working memory limits (7±2 chunks) dictate cognitive load boundaries in instructional design.", 0.90),
    # Statistics -> Machine Learning / Data Science
    (["bayes theorem"], ["machine learning", "ai"], "application_of", "Bayesian updating provides probabilistic foundation for naive Bayes classifiers and Bayesian neural networks.", 0.95),
    (["central limit theorem"], ["hypothesis testing", "statistics"], "prerequisite_of", "CLT guarantees sampling distributions approach normality, validating z-tests and t-tests.", 0.95),
    # Economics -> Psychology / Civics
    (["opportunity cost"], ["cognitive biases", "decision"], "influenced_by", "Sunk cost fallacy causes economic actors to miscalculate opportunity costs by honoring past unrecoverable expenses.", 0.90),
    (["supply and demand"], ["deadweight loss", "tax"], "governed_by", "Market clearing price equilibrium shifts under taxes, creating deadweight welfare loss.", 0.95),
    # Design -> Visual Perception
    (["gestalt", "design"], ["typography hierarchy", "readability"], "application_of", "Gestalt proximity and similarity dictate optimal typographic leading and visual header hierarchy.", 0.95)
]

cross_links_final = []
for idx, (kw_a, kw_b, rel_type, desc, wt) in enumerate(cross_domain_bridges):
    cid_a = find_concept(kw_a)
    cid_b = find_concept(kw_b)
    if cid_a and cid_b and cid_a != cid_b:
        cross_links_final.append({
            "source_concept_id": cid_a,
            "target_concept_id": cid_b,
            "relationship_type": rel_type,
            "description": desc,
            "weight": wt
        })

print(f"Generated {len(cross_links_final)} cross-domain relationship links.")
with open(os.path.join(DATA_DIR, "cross_domain_links.json"), "w", encoding="utf-8") as f:
    json.dump(cross_links_final, f, indent=2)

# ----------------------------------------------------------------------
# 5. CONCEPT FAMILIES / CLUSTERS (Phase 3)
# ----------------------------------------------------------------------
concept_families = [
    {
        "family_id": "fam_classical_mechanics",
        "name": "Newtonian Classical Mechanics Family",
        "domain_id": "sci_math",
        "description": "Foundational physical laws governing motion, forces, momentum, energy conservation, and gravitation.",
        "concepts": ["c_gravity", "c_newtons_laws", "c_conservation_energy"]
    },
    {
        "family_id": "fam_electrical_systems",
        "name": "Circuit Theory & Electrical Systems",
        "domain_id": "eng_tech",
        "description": "Electromotive force, resistance, Ohm's law, domestic wiring, and electrical utility safety.",
        "concepts": ["c_ohms_law", "c_domestic_electrical"]
    },
    {
        "family_id": "fam_algorithms_data_structures",
        "name": "Computational Complexity & Data Structures",
        "domain_id": "computing_ai",
        "description": "Asymptotic analysis (Big-O), associative arrays (Hash Tables), and search algorithms.",
        "concepts": ["c_big_o_notation", "c_hash_table", "c_rag", "c_transformer_arch"]
    },
    {
        "family_id": "fam_market_economics",
        "name": "Microeconomic Foundations & Valuation",
        "domain_id": "business_econ",
        "description": "Supply/demand equilibrium, opportunity cost, and compound interest calculations.",
        "concepts": ["c_supply_demand", "c_opportunity_cost", "c_compound_interest"]
    },
    {
        "family_id": "fam_cognitive_science",
        "name": "Epistemology & Cognitive Architecture",
        "domain_id": "humanities",
        "description": "Empirical falsifiability, working memory constraints, and heuristic decision biases.",
        "concepts": ["c_scientific_falsifiability", "c_working_memory", "c_cognitive_biases"]
    },
    {
        "family_id": "fam_constitutional_governance",
        "name": "Constitutional Law & Governance Structures",
        "domain_id": "society_culture",
        "description": "Separation of powers, checks and balances, and the procedural rule of law.",
        "concepts": ["c_separation_of_powers", "c_rule_of_law"]
    }
]

with open(os.path.join(DATA_DIR, "concept_families.json"), "w", encoding="utf-8") as f:
    json.dump(concept_families, f, indent=2)
print(f"Generated {len(concept_families)} concept families.")

# ----------------------------------------------------------------------
# 6. VECTOR EMBEDDING METADATA (Phase 10)
# ----------------------------------------------------------------------
embedding_records = []
target_sample_concepts = ["c_gravity", "c_newtons_laws", "c_ohms_law", "c_big_o_notation", "c_hash_table", "c_rag", "c_transformer_arch", "c_supply_demand"]

for cid in target_sample_concepts:
    if cid in all_concepts:
        embedding_records.append({
            "id": f"emb_concept_{cid}",
            "target_type": "concept",
            "target_id": cid,
            "model_name": "text-embedding-004",
            "dimensions": 768,
            "quantization": "float32",
            "vector_json": None, # Lightweight metadata ready for vector indexing
            "version": "1.0.0"
        })

with open(os.path.join(DATA_DIR, "embedding_metadata.json"), "w", encoding="utf-8") as f:
    json.dump(embedding_records, f, indent=2)
print(f"Generated {len(embedding_records)} embedding metadata scaffolds.")

print("\n✓ Intelligence data generation completed successfully!")
