#!/usr/bin/env python3
"""
Taxonomy Expansion for Kuzmix AI Offline Knowledge Corpus.
Expands domains, categories, topics, and authoritative sources.
"""

import json
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_DIR = os.path.join(BASE_DIR, "data")

DOMAINS = [
    {
        "id": "sci_math",
        "name": "Science and Mathematics",
        "slug": "science-and-mathematics",
        "description": "Fundamental empirical laws governing nature and formal logical frameworks including physics, chemistry, biology, mathematics, and cosmology.",
        "icon": "Atom",
        "sort_order": 1
    },
    {
        "id": "eng_tech",
        "name": "Engineering and Technology",
        "slug": "engineering-and-technology",
        "description": "Application of scientific and mathematical principles to design, build, and optimize machines, structures, systems, and materials.",
        "icon": "Cpu",
        "sort_order": 2
    },
    {
        "id": "computing_ai",
        "name": "Computing and Artificial Intelligence",
        "slug": "computing-and-ai",
        "description": "Theoretical foundations of information processing, data structures, algorithms, operating systems, distributed architectures, and AI.",
        "icon": "Binary",
        "sort_order": 3
    },
    {
        "id": "business_econ",
        "name": "Business and Economics",
        "slug": "business-and-economics",
        "description": "Principles of resource allocation, market dynamics, corporate finance, entrepreneurship, organizational strategy, and trade.",
        "icon": "TrendingUp",
        "sort_order": 4
    },
    {
        "id": "humanities",
        "name": "Human Knowledge and Humanities",
        "slug": "human-knowledge-and-humanities",
        "description": "The study of human experience, philosophical inquiry, epistemology, history, cognitive psychology, language, and logic.",
        "icon": "BookOpen",
        "sort_order": 5
    },
    {
        "id": "society_culture",
        "name": "Society, Civics and Culture",
        "slug": "society-civics-and-culture",
        "description": "Institutions of human collective governance, legal principles, constitutional structures, international relations, and cultural customs.",
        "icon": "Landmark",
        "sort_order": 6
    },
    {
        "id": "health_life",
        "name": "Health and Life Sciences",
        "slug": "health-and-life-sciences",
        "description": "Foundational understanding of human anatomy, physiology, cellular metabolism, nutrition science, preventative wellness, and first aid.",
        "icon": "HeartPulse",
        "sort_order": 7
    },
    {
        "id": "creative_arts",
        "name": "Creative Fields and Arts",
        "slug": "creative-fields-and-arts",
        "description": "Formal principles of visual composition, architecture, design psychology, acoustic harmony, music theory, and narrative structure.",
        "icon": "Palette",
        "sort_order": 8
    },
    {
        "id": "practical_life",
        "name": "Practical and Applied Knowledge",
        "slug": "practical-and-applied-knowledge",
        "description": "Everyday scientific problem-solving, mechanical fasteners, food chemistry in cooking, domestic electrical safety, and tool taxonomy.",
        "icon": "Wrench",
        "sort_order": 9
    }
]

CATEGORIES = [
    # Science and Mathematics
    {"id": "cat_math_algebra", "domain_id": "sci_math", "name": "Algebra & Number Theory", "slug": "algebra-number-theory", "description": "Equations, linear algebra, matrices, prime numbers, and abstract algebraic structures.", "sort_order": 1},
    {"id": "cat_math_calculus", "domain_id": "sci_math", "name": "Calculus & Analysis", "slug": "calculus-analysis", "description": "Differential and integral calculus, series, limits, and multivariable analysis.", "sort_order": 2},
    {"id": "cat_math_probability", "domain_id": "sci_math", "name": "Probability & Statistics", "slug": "probability-statistics", "description": "Distributions, Bayesian inference, hypothesis testing, and stochastic processes.", "sort_order": 3},
    {"id": "cat_math_discrete", "domain_id": "sci_math", "name": "Discrete Mathematics & Logic", "slug": "discrete-math-logic", "description": "Combinatorics, graph theory, propositional logic, and formal proof systems.", "sort_order": 4},
    {"id": "cat_physics_mechanics", "domain_id": "sci_math", "name": "Classical & Continuum Mechanics", "slug": "classical-mechanics", "description": "Kinematics, Newton's laws, gravitation, momentum, and rotational dynamics.", "sort_order": 5},
    {"id": "cat_physics_thermo", "domain_id": "sci_math", "name": "Thermodynamics & Statistical Physics", "slug": "thermodynamics-statistical", "description": "Heat transfer, entropy, thermodynamic cycles, and kinetic theory of gases.", "sort_order": 6},
    {"id": "cat_physics_electromagnetism", "domain_id": "sci_math", "name": "Electromagnetism & Wave Optics", "slug": "electromagnetism-optics", "description": "Maxwell's equations, electrostatic fields, wave propagation, and refraction.", "sort_order": 7},
    {"id": "cat_physics_quantum", "domain_id": "sci_math", "name": "Quantum & Relativistic Physics", "slug": "quantum-relativity", "description": "Wave mechanics, spacetime geometry, general relativity, and quantum states.", "sort_order": 8},
    {"id": "cat_chem_general", "domain_id": "sci_math", "name": "General & Inorganic Chemistry", "slug": "general-inorganic-chem", "description": "Periodic trends, chemical bonding, stoichiometry, and coordination complexes.", "sort_order": 9},
    {"id": "cat_chem_organic", "domain_id": "sci_math", "name": "Organic & Biological Chemistry", "slug": "organic-biochem", "description": "Functional groups, reaction mechanisms, biomolecules, and organic synthesis.", "sort_order": 10},
    {"id": "cat_chem_physical", "domain_id": "sci_math", "name": "Physical Chemistry & Electrochemistry", "slug": "physical-electrochem", "description": "Reaction kinetics, chemical equilibrium, thermodynamics, and redox systems.", "sort_order": 11},
    {"id": "cat_bio_cellular", "domain_id": "sci_math", "name": "Cellular & Molecular Biology", "slug": "cellular-molecular-bio", "description": "Cell organelles, DNA replication, gene expression, and ATP bioenergetics.", "sort_order": 12},
    {"id": "cat_bio_genetics", "domain_id": "sci_math", "name": "Genetics & Evolutionary Biology", "slug": "genetics-evolution", "description": "Heredity, natural selection, phylogenetics, and population genetics.", "sort_order": 13},
    {"id": "cat_bio_ecology", "domain_id": "sci_math", "name": "Ecology & Environmental Biology", "slug": "ecology-environmental", "description": "Ecosystems, biogeochemical cycles, food webs, and biodiversity conservation.", "sort_order": 14},
    {"id": "cat_earth_geology", "domain_id": "sci_math", "name": "Geology & Geophysics", "slug": "geology-geophysics", "description": "Plate tectonics, mineralogy, rock cycles, volcanism, and seismic wave dynamics.", "sort_order": 15},
    {"id": "cat_earth_meteorology", "domain_id": "sci_math", "name": "Atmospheric Science & Meteorology", "slug": "meteorology-climate", "description": "Atmospheric circulation, weather systems, Coriolis dynamics, and climatology.", "sort_order": 16},
    {"id": "cat_space_astronomy", "domain_id": "sci_math", "name": "Astronomy & Astrophysics", "slug": "astronomy-astrophysics", "description": "Stellar evolution, orbital mechanics, cosmological expansion, and galaxies.", "sort_order": 17},

    # Engineering and Technology
    {"id": "cat_eng_mechanical", "domain_id": "eng_tech", "name": "Mechanical & Aerospace Engineering", "slug": "mechanical-aerospace", "description": "Fluid dynamics, aerodynamics, machine components, gears, and thermal engines.", "sort_order": 18},
    {"id": "cat_eng_electrical", "domain_id": "eng_tech", "name": "Electrical & Electronics Engineering", "slug": "electrical-electronics", "description": "Circuit analysis, semiconductors, power systems, analog circuits, and signals.", "sort_order": 19},
    {"id": "cat_eng_civil", "domain_id": "eng_tech", "name": "Civil & Structural Engineering", "slug": "civil-structural", "description": "Load distribution, concrete technology, structural mechanics, and bridges.", "sort_order": 20},
    {"id": "cat_eng_materials", "domain_id": "eng_tech", "name": "Materials Science & Metallurgy", "slug": "materials-science", "description": "Crystal structures, tensile stress, polymers, ceramics, and corrosion.", "sort_order": 21},
    {"id": "cat_eng_robotics", "domain_id": "eng_tech", "name": "Robotics & Control Systems", "slug": "robotics-control", "description": "Feedback control, PID loops, sensors, actuators, and kinematic chains.", "sort_order": 22},
    {"id": "cat_eng_energy", "domain_id": "eng_tech", "name": "Energy Systems & Power Generation", "slug": "energy-power-systems", "description": "Renewables, battery storage, nuclear power, electrical grids, and efficiency.", "sort_order": 23},

    # Computing and Artificial Intelligence
    {"id": "cat_cs_foundations", "domain_id": "computing_ai", "name": "Computer Science Foundations", "slug": "cs-foundations", "description": "Automata, Turing completeness, computational complexity, and information theory.", "sort_order": 24},
    {"id": "cat_cs_dsa", "domain_id": "computing_ai", "name": "Data Structures & Algorithms", "slug": "data-structures-algorithms", "description": "Trees, graphs, hashing, sorting, dynamic programming, and search algorithms.", "sort_order": 25},
    {"id": "cat_cs_systems", "domain_id": "computing_ai", "name": "Operating Systems & Hardware Architecture", "slug": "os-architecture", "description": "Virtual memory, CPU scheduling, concurrency, caches, and file systems.", "sort_order": 26},
    {"id": "cat_cs_networking", "domain_id": "computing_ai", "name": "Computer Networking & Protocols", "slug": "networking-protocols", "description": "TCP/IP stack, routing, DNS, HTTP, socket communication, and bandwidth.", "sort_order": 27},
    {"id": "cat_cs_distributed", "domain_id": "computing_ai", "name": "Distributed Systems & Cloud Computing", "slug": "distributed-systems", "description": "Consensus protocols, CAP theorem, replication, microservices, and sharding.", "sort_order": 28},
    {"id": "cat_cs_security", "domain_id": "computing_ai", "name": "Cybersecurity & Cryptography", "slug": "cybersecurity-cryptography", "description": "Ciphers, public-key encryption, authentication, hashes, and threat models.", "sort_order": 29},
    {"id": "cat_cs_software", "domain_id": "computing_ai", "name": "Software Engineering & Architecture", "slug": "software-engineering", "description": "Design patterns, testing methodologies, API design, and database systems.", "sort_order": 30},
    {"id": "cat_cs_ai_ml", "domain_id": "computing_ai", "name": "Artificial Intelligence & Machine Learning", "slug": "ai-machine-learning", "description": "Neural networks, optimization, gradient descent, transformers, and RAG.", "sort_order": 31},

    # Business and Economics
    {"id": "cat_econ_micro", "domain_id": "business_econ", "name": "Microeconomics & Market Structures", "slug": "microeconomics-markets", "description": "Supply and demand, price elasticity, consumer theory, and market failures.", "sort_order": 32},
    {"id": "cat_econ_macro", "domain_id": "business_econ", "name": "Macroeconomics & Monetary Policy", "slug": "macroeconomics-policy", "description": "GDP, inflation, interest rates, central banking, and international trade.", "sort_order": 33},
    {"id": "cat_biz_finance", "domain_id": "business_econ", "name": "Corporate Finance & Capital Markets", "slug": "finance-capital-markets", "description": "Valuation, DCF, portfolio theory, capital structure, and asset pricing.", "sort_order": 34},
    {"id": "cat_biz_accounting", "domain_id": "business_econ", "name": "Financial & Managerial Accounting", "slug": "accounting-financial", "description": "Double-entry bookkeeping, balance sheets, cash flow, and cost accounting.", "sort_order": 35},
    {"id": "cat_biz_strategy", "domain_id": "business_econ", "name": "Strategic Management & Operations", "slug": "strategy-operations", "description": "Competitive advantage, value chain, unit economics, and operational efficiency.", "sort_order": 36},
    {"id": "cat_biz_marketing", "domain_id": "business_econ", "name": "Marketing & Customer Dynamics", "slug": "marketing-customer", "description": "Market segmentation, positioning, pricing strategies, and customer lifetime value.", "sort_order": 37},

    # Human Knowledge and Humanities
    {"id": "cat_hum_philosophy", "domain_id": "humanities", "name": "Epistemology & Metaphysics", "slug": "philosophy-epistemology", "description": "Theories of knowledge, truth criteria, ontology, and philosophy of science.", "sort_order": 38},
    {"id": "cat_hum_ethics", "domain_id": "humanities", "name": "Ethics & Moral Philosophy", "slug": "ethics-moral-philosophy", "description": "Utilitarianism, deontology, virtue ethics, and applied moral dilemmas.", "sort_order": 39},
    {"id": "cat_hum_history", "domain_id": "humanities", "name": "World History & Historiography", "slug": "world-history-eras", "description": "Historical eras, civilizational turning points, trade networks, and revolutions.", "sort_order": 40},
    {"id": "cat_hum_psychology", "domain_id": "humanities", "name": "Cognitive & Behavioral Psychology", "slug": "psychology-cognitive", "description": "Memory systems, biases, heuristics, perception, and learning models.", "sort_order": 41},
    {"id": "cat_hum_linguistics", "domain_id": "humanities", "name": "Linguistics & Semantics", "slug": "linguistics-semantics", "description": "Phonology, morphology, syntax, semantics, and language evolution.", "sort_order": 42},

    # Society, Civics and Culture
    {"id": "cat_soc_civics", "domain_id": "society_culture", "name": "Civics & Governance Structures", "slug": "civics-governance", "description": "Constitutional separation of powers, electoral systems, and executive branches.", "sort_order": 43},
    {"id": "cat_soc_law", "domain_id": "society_culture", "name": "Jurisprudence & Legal Principles", "slug": "law-jurisprudence", "description": "Rule of law, procedural due process, contracts, and legal evidence standards.", "sort_order": 44},
    {"id": "cat_soc_international", "domain_id": "society_culture", "name": "International Relations & Treaties", "slug": "international-relations", "description": "Sovereignty, diplomacy, international law, treaties, and global institutions.", "sort_order": 45},
    {"id": "cat_soc_sociology", "domain_id": "society_culture", "name": "Sociology & Social Institutions", "slug": "sociology-institutions", "description": "Social stratification, demographic transitions, cultural norms, and collective action.", "sort_order": 46},

    # Health and Life Sciences
    {"id": "cat_health_anatomy", "domain_id": "health_life", "name": "Human Anatomy & Organ Systems", "slug": "human-anatomy-systems", "description": "Skeletal, muscular, circulatory, nervous, respiratory, and renal anatomy.", "sort_order": 47},
    {"id": "cat_health_physiology", "domain_id": "health_life", "name": "Human Physiology & Neurobiology", "slug": "human-physiology", "description": "Endocrine regulation, synaptic signaling, cardiac hemodynamics, and homeostasis.", "sort_order": 48},
    {"id": "cat_health_nutrition", "domain_id": "health_life", "name": "Nutrition Science & Metabolism", "slug": "nutrition-metabolic-science", "description": "Macronutrients, micronutrients, glycemic response, and metabolic pathways.", "sort_order": 49},
    {"id": "cat_health_first_aid", "domain_id": "health_life", "name": "First Aid & Emergency Protocols", "slug": "first-aid-emergency", "description": "CPR, hemorrhage control, burn triage, shock response, and airway management.", "sort_order": 50},
    {"id": "cat_health_public", "domain_id": "health_life", "name": "Public Health & Epidemiology", "slug": "public-health-epidemiology", "description": "Disease transmission models, herd immunity, hygiene standards, and prevention.", "sort_order": 51},

    # Creative Fields and Arts
    {"id": "cat_art_design", "domain_id": "creative_arts", "name": "Visual Design & Composition", "slug": "visual-design-composition", "description": "Gestalt laws, visual hierarchy, color theory, contrast ratios, and whitespace.", "sort_order": 52},
    {"id": "cat_art_typography", "domain_id": "creative_arts", "name": "Typography & Editorial Layout", "slug": "typography-layout", "description": "Type scales, optical weights, leading, line lengths, and font pairings.", "sort_order": 53},
    {"id": "cat_art_architecture", "domain_id": "creative_arts", "name": "Architectural Design & Space", "slug": "architectural-design", "description": "Structural form, daylighting, passive solar heating, and spatial proportions.", "sort_order": 54},
    {"id": "cat_art_music", "domain_id": "creative_arts", "name": "Music Theory & Acoustics", "slug": "music-theory-acoustics", "description": "Harmonic series, equal temperament, intervals, scales, and acoustic resonance.", "sort_order": 55},
    {"id": "cat_art_narrative", "domain_id": "creative_arts", "name": "Narrative Structure & Storytelling", "slug": "narrative-storytelling", "description": "Three-act structure, pacing, character arcs, and dramatic tension.", "sort_order": 56},

    # Practical and Applied Knowledge
    {"id": "cat_prac_tools", "domain_id": "practical_life", "name": "Tools, Hardware & Fasteners", "slug": "tools-hardware-fasteners", "description": "Screw thread pitch, bolt grades, saw types, wrenches, and tool taxonomy.", "sort_order": 57},
    {"id": "cat_prac_mechanics", "domain_id": "practical_life", "name": "Everyday Mechanical Advantage", "slug": "everyday-mechanics", "description": "Levers, pulleys, torque leverage, inclined planes, and lubrication.", "sort_order": 58},
    {"id": "cat_prac_electrical", "domain_id": "practical_life", "name": "Domestic Electrical & Utility Safety", "slug": "domestic-electrical-utilities", "description": "Circuit breakers, GFCI, wire gauge ratings, grounding, and multimeters.", "sort_order": 59},
    {"id": "cat_prac_food_sci", "domain_id": "practical_life", "name": "Culinary Science & Cooking Principles", "slug": "culinary-food-science", "description": "Maillard reaction, emulsions, gelatinization, caramelization, and brining.", "sort_order": 60},
    {"id": "cat_prac_safety_skills", "domain_id": "practical_life", "name": "Safety, Water & Life Skills", "slug": "practical-safety-skills", "description": "Fire triangle, water filtration, essential knots, navigation, and storage.", "sort_order": 61}
]

# Write out updated domains and categories
with open(os.path.join(DATA_DIR, "domains.json"), "w", encoding="utf-8") as f:
    json.dump(DOMAINS, f, indent=2)

with open(os.path.join(DATA_DIR, "categories.json"), "w", encoding="utf-8") as f:
    json.dump(CATEGORIES, f, indent=2)

print(f"✓ Updated {len(DOMAINS)} domains and {len(CATEGORIES)} categories successfully.")
