#!/usr/bin/env python3
"""
Group 4 Generator: Health, Arts & Practical Knowledge (15 categories, ~630 concepts)
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from common import make_concept_obj, slugify

HEALTH_ARTS_PRAC_CATEGORIES = [
    # Health and Life Sciences
    ("cat_health_anatomy", "health_life", "Human Anatomy and Organ Systems", [
        "Skeletal System Axial Appendicular Bones", "Bone Remodeling Osteoblasts Osteoclasts", "Joint Types Synovial Ball Hinge Pivot", "Muscular System Skeletal Smooth Cardiac",
        "Sarcomere Sliding Filament Theory Actin", "Integumentary System Epidermis Dermis Subcut", "Melanin UV Protection Skin Thermoregulation", "Cardiovascular Anatomy Heart Four Chambers",
        "Cardiac Valves Atrioventricular Semilunar", "Coronary Arteries Myocardial Blood Supply", "Systemic Arteries Arterioles Resistance", "Capillaries Microcirculation Endothelial Diffusion",
        "Systemic Veins Venules Valves Gravity", "Superior and Inferior Vena Cava Drainage", "Respiratory Tract Pharynx Larynx Trachea", "Bronchial Tree Bronchioles Alveolar Sacs",
        "Pulmonary Surfactant Surface Tension Lung", "Diaphragm Intercostal Breathing Mechanics", "Central Nervous System Brain Spinal Cord", "Cerebral Cortex Frontal Parietal Occipital",
        "Brainstem Medulla Pons Midbrain Autonomic", "Cerebellum Motor Coordination Balance Posture", "Spinal Cord Gray Matter White Horns Tracts", "Peripheral Nervous System Somatic Autonomic",
        "Cranial Nerves Twelve Pairs Vagus Nerve", "Autonomic Sympathetic Fight Parasympathetic Rest", "Enteric Nervous System Second Gut Brain", "Gastrointestinal Tract Esophagus Stomach Intest",
        "Stomach Gastric Acid Pepsin Mucosal Barrier", "Small Intestine Duodenum Jejunum Ileum Villi", "Large Intestine Colon Water Absorption Microbiota", "Hepatic Portal System Liver Detoxification",
        "Gallbladder Bile Storage Lipid Emulsification", "Pancreas Exocrine Digestive Endocrine Islets", "Renal Anatomy Kidneys Cortex Medulla Pelvis", "Nephron Glomerulus Bowman Capsule Tubules",
        "Endocrine Glands Pituitary Thyroid Adrenal", "Thyroid Gland T3 T4 Metabolic Regulation", "Adrenal Cortex Cortisol Aldosterone Medulla", "Lymphatic System Nodes Spleen Thymus Drainage",
        "Bone Marrow Hematopoiesis Stem Cells", "Sensory Organs Eye Retina Ear Cochlea Vestibular"
    ]),
    ("cat_health_physiology", "health_life", "Human Physiology and System Homeostasis", [
        "Homeostasis Negative Feedback Loops Set Point", "Body Temperature Thermoregulation Hypothalamus", "Action Potential Depolarization Sodium Channels", "Action Potential Repolarization Potassium Efflux",
        "Refractory Period Absolute Relative Channels", "Synaptic Neurotransmission Acetylcholine Release", "Excitatory Postsynaptic Potentials EPSP Depol", "Inhibitory Postsynaptic Potentials IPSP GABA",
        "Neuromuscular Junction Acetylcholinesterase", "Cardiac Conduction Sinoatrial Node Pacemaker", "Atrioventricular Node AV Delay Bundle of His", "Electrocardiogram ECG P Wave QRS Complex T",
        "Cardiac Cycle Systole Diastole Pressure Volume", "Stroke Volume End Diastolic Frank-Starling", "Blood Pressure Systolic Diastolic Sphygmomanometer", "Baroreceptor Reflex Carotid Sinus Blood Pressure",
        "Renin-Angiotensin-Aldosterone System RAAS Kidney", "Antidiuretic Hormone ADH Vasopressin Water", "Glomerular Filtration Rate GFR Tubuloglomerular", "Renal Countercurrent Multiplier Medullary Gradient",
        "Acid-Base Balance Bicarbonate Buffer Blood pH", "Respiratory Acidosis vs Alkalosis CO2 Levels", "Metabolic Acidosis vs Alkalosis Bicarbonate", "Hemoglobin Oxygen Dissociation Curve Sigmoidal",
        "Bohr Effect Carbon Dioxide Acidity Shift", "Cellular Hypoxia Tissue Oxygen Starvation", "Endocrine Glucose Homeostasis Insulin Glucagon", "Insulin Signaling GLUT4 Translocation Muscle",
        "Glucagon Hepatic Glycogenolysis Gluconeogenesis", "Calcium Homeostasis Parathyroid Hormone Calcitonin", "Stress Response Cortisol HPA Axis Glucocorticoid", "Thyroid Hormone Basal Metabolic Rate Control",
        "Gastric Acid Secretion Parietal Proton Pump", "Gastrointestinal Peristalsis Enteric Motor", "Liver First-Pass Drug Metabolism Cytochrome P450", "Bile Salt Enterohepatic Circulation Lipid",
        "Hemostasis Platelet Plug Activation Clotting", "Coagulation Cascade Intrinsic Extrinsic Fibrin", "Fibrinolysis Clot Breakdown Plasmin Tissue", "Innate Immune Phagocytosis Macrophages Neutrophils",
        "Adaptive Immunity B Cells Plasma Antibodies", "T Cell Mediated Immunity CD4 Helper CD8 Cytotoxic"
    ]),
    ("cat_health_nutrition", "health_life", "Nutrition Science and Metabolic Pathways", [
        "Macronutrients Carbohydrates Fats Proteins Ratios", "Caloric Energy Content 4-9-4 kcal per Gram", "Essential Amino Acids Nine Indispensable Protein", "Complete vs Incomplete Protein Digestibility PDCAAS",
        "Protein Synthesis Muscle Hypertrophy Leucine", "Carbohydrate Metabolism Glucose Glycogen Starch", "Simple Sugars Monosaccharides Fructose Sucrose", "Complex Starches Amylose Amylopectin Digestion",
        "Dietary Fiber Soluble Fermentable Short-Chain", "Insoluble Fiber Bowel Transit Stool Bulk", "Glycemic Index GI Blood Glucose Spike Rate", "Glycemic Load GL Available Carbohydrate Portions",
        "Dietary Fats Saturated Fatty Acids Solid Chain", "Monounsaturated Fatty Acids MUFA Oleic Olive", "Polyunsaturated Fatty Acids PUFA Omega-3 EPA DHA", "Essential Fatty Acids Alpha-Linolenic Linoleic",
        "Trans Fatty Acids Industrial Hydrogenation Risk", "Cholesterol Endogenous Synthesis Lipoproteins", "Low-Density Lipoprotein LDL Atherosclerosis", "High-Density Lipoprotein HDL Reverse Transport",
        "Triglycerides Energy Storage Adipose Tissue", "Fat-Soluble Vitamins A D E K Absorption Bile", "Vitamin A Retinol Vision Epithelial Immune", "Vitamin D Cholecalciferol Calcium Absorption",
        "Vitamin E Tocopherols Lipid Antioxidant Membrane", "Vitamin K Phylloquinone Blood Clotting Bone", "Water-Soluble Vitamins B-Complex and Vitamin C", "Thiamine B1 Beriberi Carbohydrate Metabolism",
        "Niacin B3 Pellagra NAD NADP Coenzyme Redox", "Cobalamin B12 Pernicious Anemia Methylation", "Folate B9 Neural Tube Defects DNA Synthesis", "Vitamin C Ascorbic Acid Collagen Antioxidant",
        "Essential Minerals Iron Hemoglobin Oxygen", "Calcium Hydroxyapatite Bone Muscular Signal", "Potassium Intracellular Electrolyte Cardiac", "Sodium Extracellular Fluid Volume Osmolarity",
        "Magnesium Enzymatic Cofactor ATP Coordination", "Iodine Thyroid Hormone Synthesis Deficiency", "Basal Metabolic Rate BMR Total Daily Energy TDEE", "Energy Balance Caloric Deficit Weight Loss",
        "Ketosis Beta-Hydroxybutyrate Fasting State", "Hydration Fluid Balance Dehydration Symptoms"
    ]),
    ("cat_health_first_aid", "health_life", "First Aid and Emergency Medical Protocols", [
        "Basic Life Support BLS Algorithm Sequence", "Scene Safety Hazard Assessment First Responders", "Cardiopulmonary Resuscitation CPR Adult Ratio 30:2", "Chest Compression Depth 5-6 cm High Quality",
        "Chest Compression Rate 100-120 Beats Minute", "Full Chest Recoil Coronary Perfusion Chamber", "Hands-Only Bystander CPR Continuous Compression", "Automated External Defibrillator AED Pad Physics",
        "AED Shockable Rhythms Ventricular Fibrillation", "Non-Shockable Asystole PEA Flatline Protocol", "Choking Adult Heimlich Abdominal Thrusts Airway", "Choking Infant Back Blows Chest Thrusts",
        "Airway Obstruction Triple Airway Head-Tilt Chin-Lift", "Recovery Position Lateral Decubitus Airway Vomit", "Severe External Hemorrhage Direct Pressure Pack", "Commercial Windlass Tourniquet Extremity Bleed",
        "Hemostatic Gauze Kaolin Clotting Dressing Pack", "Shock Hypovolemic Blood Loss Signs Triage", "Anaphylaxis Recognition Hives Stridor Shock", "Epinephrine Auto-Injector EpiPen Intramuscular",
        "Burn Triage Superficial Partial Full Thickness", "Burn Immediate Treatment Cool Running Water", "Chemical Burns Extensive Irrigation Decontaminate", "Electrical Burns Internal Tissue Exit Wounds",
        "Fracture Splinting Joint Above Below Immobilize", "Sprain and Strain RICE Rest Ice Compress Elevate", "Concussion Traumatic Brain Injury Red Flags", "Spinal Motion Restriction Suspected Trauma Spine",
        "Heat Exhaustion Sweating Dizziness Rehydrate", "Heat Stroke Core Temp >40C Anhidrosis Rapid Cool", "Hypothermia Shivering Confusion Rewarming Core", "Frostbite Tissue Freezing Rewarming Avoid Friction",
        "Seizure Response Protect Head Clear Space Avoid Mouth", "Diabetic Hypoglycemia Confusion Rapid Sugar Oral", "Stroke Recognition FAST Face Arm Speech Time", "Myocardial Infarction Heart Attack Aspirin Chew",
        "Eye Injuries Chemical Flush Foreign Object Shield", "Snake Bite Pressure Immobilization Avoid Tourniquet", "Emergency Poisoning Poison Control Activated Charcoal", "Triage Mass Casualty START Algorithm Green Black"
    ]),
    ("cat_health_public", "health_life", "Public Health, Hygiene and Epidemiology", [
        "Public Health Mission Population-Level Prevention", "Epidemiology Study of Disease Distribution Patterns", "Epidemiologic Triad Agent Host Environment", "Infectious Disease Transmission Airborne Droplet",
        "Fecal-Oral Transmission Water Sanitation Hygiene", "Vector-Borne Transmission Mosquitoes Ticks Malaria", "Fomites Inanimate Surface Viral Bacterial Spread", "Basic Reproduction Number R0 Contagion Metric",
        "Effective Reproduction Number Re Control Interventions", "Incubation Period Latent Infection Window", "Asymptomatic Transmission Silent Disease Vector", "Herd Immunity Threshold Vaccination Percentage",
        "Vaccines Active Immunization Memory B T Cells", "Live Attenuated vs Inactivated Subunit Vaccines", "mRNA Vaccines Lipid Nanoparticle Antigen Expression", "Adjuvants Immunological Response Boosters",
        "Antibiotic Resistance Superbugs MRSA Mechanisms", "Antimicrobial Stewardship Overprescription Avoid", "Clean Water Filtration Chlorination Microbial Free", "Hand Hygiene Friction Soap Lipid Membrane Disruption",
        "Alcohol-Based Hand Sanitizers 60-80% Denature", "Personal Protective Equipment PPE Masks Gloves N95", "Quarantine vs Isolation Infected vs Exposed", "Contact Tracing Ring Containment Breaking Chains",
        "Epidemic vs Pandemic vs Endemic Disease States", "Surveillance Systems Notifiable Diseases CDC WHO", "Morbidity vs Mortality Incidence Prevalence Rates", "Case Fatality Rate CFR vs Infection Fatality IFR",
        "Social Determinants of Health Housing Income Equity", "Health Disparities Systemic Access Inequity", "Tobacco Control Taxation Public Smoking Bans", "Sanitation Infrastructure Sewage Wastewater Sepsis",
        "Food Safety Pasteurized Cold Chain HACCP Systems", "Vector Control Larvicides Insecticide Bed Nets", "Maternal and Child Health Prenatal Care Mortality", "Non-Communicable Diseases Lifestyle Cardiovascular",
        "Occupational Health OSHA Hazard Ergonomic Safety", "Air Quality Index AQI Particulate PM2.5 Ozone", "Zoonotic Spillover Animal to Human Reservoirs", "One Health Interconnection Human Animal Ecosystem",
        "Outbreak Investigation Field Epidemiology Case Definition", "Double-Blind Placebo-Controlled Trial Gold Standard"
    ]),

    # Creative Fields and Arts
    ("cat_art_design", "creative_arts", "Visual Design and Composition Principles", [
        "Gestalt Law of Proximity Spatial Grouping", "Gestalt Law of Similarity Color Shape Function", "Gestalt Law of Continuity Smooth Eye Trajectory", "Gestalt Law of Closure Mental Contour Completion",
        "Gestalt Law of Figure-Ground Contrast Separation", "Visual Hierarchy Scale Weight Position Contrast", "Contrast Ratio Visual Distinction Emphasis", "Whitespace Negative Space Cognitive Breathing Room",
        "Rule of Thirds Grid Intersections Focal Points", "Golden Ratio Fibonacci Spiral Proportions 1.618", "Balance Symmetrical Asymmetrical Radial Weight", "Alignment Grid Systems Horizontal Vertical Baselines",
        "Repetition Consistency Visual Rhythm Branding", "Proximity Semantic Association Padding Margins", "Focal Point Center of Interest Visual Anchor", "Color Wheel Primaries Secondaries Tertiaries",
        "Color Harmonies Complementary Split Analogous", "Monochromatic Color Schemes Tint Shade Tone", "Warm vs Cool Colors Psychological Distance", "Color Temperature Kelvin Ambient Mood Warmth",
        "Additive Color Mixing RGB Red Green Blue Screens", "Subtractive Color Mixing CMYK Cyan Magenta Print", "Hue Saturation Value HSV Color Model Space", "Color Psychology Cultural Associations Context",
        "WCAG Color Contrast Accessibility Standards AA AAA", "Scale and Proportion Relationships Dominance", "Fitts's Law Target Size Distance Travel Time", "Hick's Law Decision Time Number of Choices",
        "Affordance Physical Visual Clues for Interaction", "Signifiers Visible Cues Button Clickability Norman", "Feedback Loops Interactive System Response States", "Mental Models User Expectations System Reality",
        "Design Thinking Empathize Define Ideate Prototype Test", "User Experience UX Friction Usability Satisfaction", "User Interface UI Aesthetic Components Layout", "Wireframes Low-Fidelity Layout Scaffolding",
        "Interactive Prototypes Usability Testing Flow", "Responsive Web Design Fluid Grids Breakpoints", "Mobile-First Design Constraints Prioritization", "Design Systems Component Libraries Consistency",
        "Micro-Interactions Subtle Functional Animations", "Visual Skeuomorphism vs Flat Material Minimalism"
    ]),
    ("cat_art_typography", "creative_arts", "Typography and Editorial Hierarchy", [
        "Typography Anatomy Baseline Cap Height X-Height", "Ascenders and Descenders Letter Proportion Bounds", "Serif vs Sans-Serif Font Classifications", "Serif Readability Book Print Line Tracking",
        "Sans-Serif Cleanliness Digital Screen Legibility", "Monospace Fonts Fixed Pitch Code Terminal", "Display Typefaces Headline Decorative Scale", "Font Families Weights Roman Italic Bold Light",
        "Kerning Optical Spacing Between Letter Pairs", "Tracking Overall Letter Spacing Across Word", "Leading Line Height Spacing Vertical Rhythm", "Measure Line Length Optimal 45-75 Characters",
        "Typographic Hierarchy H1 H2 H3 Modular Scales", "Modular Scale Multiplier Major Third Fourth 1.25", "Typographic Contrast Weight Size Color Family", "Baseline Grid Vertical Rhythm Alignment Snap",
        "Paragraph Indents vs Paragraph Spacing Block", "Drop Caps Decorative Initial Capitals Editorial", "Orphans and Widows Isolated Words Layout Clean", "Hyphenation and Justification Ragged Right Clean",
        "Ligatures Aesthetic Character Mergers fi fl", "Small Caps True Cut vs Scaled Down Capital", "Tabular Numbers Fixed Width Columns Financial", "Proportional Oldstyle Figures Variable Numerals",
        "Variable Fonts Single File Infinite Axes Weight", "Web Safe Fonts Fallback Stacks System Fonts", "Font Pairing Contrast Harmony Complementary Type", "Legibility vs Readability Letter vs Layout Ease",
        "Optical Sizes Display Subhead Caption Glyphs", "Typeface Licensing Desktop Web App Embedding", "Inverted Pyramid Editorial News Layout Lead", "Grid Systems Columns Margins Gutters Multi-Column",
        "White Space in Editorial Layout Margins Padding", "Pull Quotes Breakout Text Editorial Breathing", "Sidebars Asides Supplementary Editorial Content", "Visual Pacing Editorial Magazine Page Rhythm",
        "Font Rendering Rasterization Anti-Aliasing Subpixel", "CSS Font Face Web Font Performance WOFF2", "FOUT vs FOIT Flash of Unstyled Invisible Text", "Accessibility Type Size Responsive Dynamic Rem",
        "OpenType Features Fractions Swashes Stylistic Sets", "Calligraphy Lettering Historical Hand Script Pen"
    ]),
    ("cat_art_architecture", "creative_arts", "Architectural Design and Structural Form", [
        "Post-and-Beam Trabeated Architecture Stone", "Classical Architectural Orders Doric Columns", "Ionic Order Volute Capital Slender Columns", "Corinthian Order Acanthus Leaf Ornate Capital",
        "Roman Arch Compression Keystone Vault Bridge", "Barrel Vault Continuous Arch Tunnel Space", "Groin Vault Intersecting Barrel Vaults Load", "Domes Hemispherical Compressive Oculus Pantheon",
        "Pendentives Spherical Triangle Dome Transition", "Flying Buttresses Gothic Cathedrals Thin Walls", "Gothic Pointed Arch Lateral Thrust Reduction", "Ribbed Vaults Skeletal Ceiling Load Transfer",
        "Renaissance Proportion Human Scale Symmetry Palladio", "Baroque Drama Curvature Chiaroscuro Space", "Modernist Architecture Form Follows Function Sullivan", "Bauhaus Functionalism Craft Industrial Mass",
        "International Style Steel Glass Skyscrapers Mies", "Brutalism Raw Exposed Concrete Monolithic Form", "Deconstructivism Non-Euclidean Fragmented Gehry", "Postmodern Architecture Historicist Irony Venturi",
        "Organic Architecture Wright Harmony with Nature", "Load-Bearing Masonry Walls Gravity Compressive", "Steel Frame Construction Skeleton Skyscraper Curtains", "Curtain Wall Non-Structural Glass Glazing Enclose",
        "Cantilever Structural Projection Unanchored Beam", "Thermal Mass Building Heating Cooling Night Flush", "Passive Solar Building Design Orientation Sun Glaze", "Daylighting Strategies Clerestory Skylights Glare",
        "Cross Ventilation Natural Cooling Air Flow Chimney", "Building Envelope Insulation Vapor Air Barriers", "Space Programming Bubble Diagrams Circulation", "Universal Design Accessibility ADA Ramps Slopes",
        "Vernacular Architecture Local Climate Materials", "Acoustics Architectural Reverberation Absorption", "HVAC Integration Plenums Mechanical Shafts", "Green Building LEED Certification Sustainability",
        "Life Cycle Assessment LCA Embodied Carbon Buildings", "Urban Planning Zoning Mixed-Use Pedestrian Transit", "Public Space Plazas Walkability Human Scale Density", "Adaptive Reuse Historical Building Preservation",
        "Parametric Architecture Computational Algorithmic", "Biophilic Architecture Nature Integration Well-Being"
    ]),
    ("cat_art_music", "creative_arts", "Music Theory, Harmony and Acoustics", [
        "Acoustic Sound Waves Frequency Pitch Amplitude", "Fundamental Frequency Pitch Perception Hertz", "Harmonic Overtone Series Integer Multiples", "Timbre Tone Color Spectral Envelope Harmonics",
        "Musical Intervals Pitch Distance Cents Semitones", "Octaves 2:1 Frequency Ratio Pitch Equivalence", "Perfect Fifth 3:2 Frequency Ratio Consonance", "Pythagorean Tuning Pure Fifths Comma Drift",
        "Just Intonation Pure Integer Ratios Key Locked", "Equal Temperament 12-TET 12th Root of Two", "Chromatic Scale Twelve Semitones Equidistant", "Diatonic Scale Seven Note Modes Whole Half",
        "Major Scale Tone Tone Semitone Tone Structure", "Natural Minor Scale Aeolian Flatted Third Sixth", "Harmonic Minor Raised Seventh Leading Tone", "Melodic Minor Ascending vs Descending Scale",
        "Musical Key Key Signatures Circle of Fifths", "Triads Major Minor Augmented Diminished Chords", "Root Position First Inversion Second Inversion", "Seventh Chords Dominant Major Minor Diminished",
        "Chord Progressions Harmonic Functional Analysis", "Tonic Subdominant Dominant Harmonic Tension Resolution", "Authentic Cadence Perfect V to I Resolution", "Plagal Cadence IV to I Amen Resolution Church",
        "Voice Leading Counterpoint Parallels Forbidden", "Polyphony Multiple Independent Melodic Voices Bach", "Homophony Melodic Lead with Harmonic Accompaniment", "Monophony Single Unaccompanied Melodic Line Chant",
        "Rhythm Meter Time Signatures 4/4 3/4 6/8", "Syncopation Off-Beat Accent Rhythmic Tension", "Polyrhythm Cross-Rhythms 3 against 2 Complex", "Tempo Metronome Markings Allegro Adagio BPM",
        "Dynamic Expression Markings Piano Forte Crescendo", "Articulation Staccato Legato Tenuto Accent Slur", "Musical Form Sonata Allegro Ternary Rondo", "Acoustic Resonance Standing Waves Node Antinode",
        "Acoustic Decibel Logarithmic Loudness Scale", "Room Reverberation Time RT60 Early Reflections", "Harmonic Distortion Overtones Saturation Tube", "Musical Transposition Key Shift Modulation Pivot",
        "Improvisation Chord Scales Modes Jazz Blues", "Instrumental Timbre Classification Hornbostel-Sachs"
    ]),
    ("cat_art_narrative", "creative_arts", "Narrative Structure and Storytelling", [
        "Three-Act Story Structure Setup Confrontation Res", "Act I Inciting Incident Call to Adventure Plot Point", "Act II Rising Action Obstacles Midpoint Crisis", "Act III Climax Resolution Denouement Falling Action",
        "Hero's Journey Monomyth Campbell Departure Return", "Freytag's Pyramid Exposition Rising Climax Fall", "In Medias Res Beginning in Middle of Action", "Story Stakes External Internal Philosophical",
        "Protagonist Flaws Goals Driving Motivations", "Antagonist Counter-Goals Opposition Justification", "Secondary Characters Mentor Sidekick Foil Mirror", "Character Arc Positive Transformation Redemption",
        "Flat Character Arc Changeless World Transformation", "Negative Tragic Character Arc Downfall Hamartia", "Conflict Man vs Man vs Self vs Nature Society", "Theme Unifying Human Truth Underlying Core Idea",
        "Exposition Show Don't Tell Sensory Grounding", "Pacing Rhythm Narrative Tension Acceleration Rest", "Foreshadowing Subtle Plant Payoff Suspense", "Chekhov's Gun Dramatic Parsimony Unfired Gun",
        "Dramatic Irony Audience Knows Character Unaware", "Suspense vs Surprise Hitchcock Bomb Under Table", "Point of View First Person Second Third Limited Omni", "Unreliable Narrator Bias Deception Subjectivity",
        "Dialogue Subtext Spoken Words Hidden Meanings", "Scene Construction Goal Conflict Disaster Sequels", "Plot Twists Anagnorisis Reversal Peripeteia", "Cliffhangers Narrative Unresolved Chapter Hooks",
        "Red Herrings False Clues Mystery Misdirection", "MacGuffin Plot Device Object Character Pursuit", "Framing Device Story Within a Story Metanarrative", "Flashbacks and Flashforwards Non-Linear Time",
        "World-Building Internal Consistency Rules Logic", "Visual Storytelling Cinematography Framing Angle", "Mise-en-Scène Visual Composition Actors Setting", "Camera Shots Wide Medium Close-Up Extreme Dutch",
        "Continuity Editing 180-Degree Spatial Axis Rule", "Cross-Cutting Parallel Editing Simultaneous Time", "Diegetic vs Non-Diegetic Sound Soundtrack Score", "Dramatic Monologue Character Psychology Soliloquy",
        "Archetypes Universal Story Patterns Jungian Hero", "Catharsis Emotional Release Tragic Resolution"
    ]),

    # Practical and Applied Knowledge
    ("cat_prac_tools", "practical_life", "Tools, Fasteners and Hardware", [
        "Hand Saws Crosscut vs Rip Cut Teeth Angle", "Backsaws Miter Saws Dovetail Joinery Precision", "Hacksaws Metal Cutting High TPI Bimetal Blade", "Chisels Wood Bevel Edge Mortise Grain Direction",
        "Hand Planes Jack Block Smoothing Surface Flatten", "Files and Rasps Metal Deburring Wood Shaping", "Clamps F-Clamp Bar Clamp C-Clamp Pressure", "Vises Bench Vise Woodworking Metal Anvil Jaws",
        "Hammers Claw Framing Ball-Peen Sledge Strike", "Mallets Rubber Wooden Dead-Blow Soft Non-Marring", "Pliers Needle-Nose Slip-Joint Wire Strippers", "Locking Pliers Vise-Grips Clamping Leverage",
        "Wrenches Open-End Box-End Combination Metric Imp", "Adjustable Crescent Wrench Jaw Slack Slippage", "Socket Wrenches Ratchet Drive 1/4 3/8 1/2 Inch", "Torque Wrenches Click Type Fastener Precision",
        "Hex Keys Allen Wrenches Torx Star Fasteners", "Screwdrivers Slotted Flathead Cam-Out Stripping", "Phillips vs Pozidriv vs Torx Drive Torque Grip", "Drill Bits Twist Masonry Spade Forstner Step",
        "Drill Driver vs Impact Driver Rotational Concussion", "Thread Pitch Metric Coarse Fine Threads Per Inch", "Bolt Grades SAE Grade 5 Grade 8 Metric 8.8 10.9", "Tensile vs Shear Strength Fastener Shear Pin",
        "Nuts and Bolts Jam Nuts Nylon Insert Locknuts", "Washers Flat Spreading Load Spring Lock Star", "Machine Screws vs Wood Screws vs Sheet Metal", "Drywall Screws Coarse Thread Bugle Head Studs",
        "Wall Anchors Drywall Hollow Toggle Bolt Mollys", "Masonry Anchors Wedge Sleeve Concrete Tapcon", "Adhesives PVA Wood Glue Polyurethane Cyanoacrylate", "Epoxy Resins Two-Part Resin Hardener Cure",
        "Silicone Sealants Waterproof Elastic Caulk Gap", "Abrasives Sandpaper Grit P80 Rough P220 Smooth", "Measuring Tape Hook Play True Zero Accuracy", "Combination Square 90 and 45 Degree Precision",
        "Levels Spirit Bubble Torpedo Plumb Vial Line", "Calipers Vernier Dial Digital Accuracy 0.01mm", "Marking Gauges Scribing Lines Across Wood Grain", "Personal Protective Equipment Safety Glasses Ear Dust"
    ]),
    ("cat_prac_mechanics", "practical_life", "Practical Mechanics and Applied Advantage", [
        "First-Class Lever Fulcrum Center Crowbar Pliers", "Second-Class Lever Load Center Wheelbarrow Nut", "Third-Class Lever Effort Center Tweezers Arm", "Mechanical Advantage Formula Output vs Input",
        "Law of the Lever Equilibrium Torque Force Distance", "Pulley Systems Fixed Single Direction Change", "Movable Pulley Force Halving Distance Doubling", "Block and Tackle Multiple Sheaves Pulley Gain",
        "Inclined Plane Ramp Lifting Height Slope Ratio", "Wedge Splitting Force Lateral Multiplication", "Screw Thread Inclined Plane Wrapped Helix Cylinder", "Wheel and Axle Mechanical Radius Multiplication",
        "Compound Machines Combining Multiple Simple Systems", "Gear Ratios Torque Multiplier vs Speed Reducer", "Idler Gears Direction Reversal Center Spacing", "Belt Drives Pulley Diameter Ratios Friction Slip",
        "Chain Drives Positive Engagement Sprockets Roller", "Lubrication Grease vs Oil Viscosity Temperature", "Friction Reduction Bearings Rolling vs Sliding", "Center of Gravity Base of Support Tip Stability",
        "Siphon Atmospheric Pressure Fluid Transfer Gravity", "Archimedes' Screw Water Lifting Rotary Spiral", "Hydraulic Jack Pascal Force Area Multiplication", "Automotive Bottle Jack Hydraulic Valve Bleed",
        "Thermal Expansion Fastener Seizure Heat Loosening", "Penetrating Oil Capillary Wicking Rusted Bolts", "Shear Pins Sacrificial Failure Mechanism Machine", "Counterweight Systems Cranes Elevators Balance",
        "Cable Tensile Loading Working Load Limits Safety", "Snatch Block Winch Line Doubling Off-Road", "Centrifugal Governors Steam Speed Feedback", "Flywheel Kinetic Energy Smoothing Engine Stroke",
        "Spring Rates Hooke's Law Spring Constant k", "Leaf Springs Beam Suspension Vehicle Load Weight", "Coil Springs Compression Extension Torsion Sway", "Vibration Isolation Rubber Mounts Engine Cabin",
        "Thermal Creep Gasket Bolt Torque Re-checking", "Shaft Alignment Couplings Flexible Universal Joints", "Keyways and Splines Rotary Torque Shaft Lock", "Cams Followers Rotary to Reciprocating Motion"
    ]),
    ("cat_prac_electrical", "practical_life", "Home Electrical and Utility Safety", [
        "Residential AC Power Hot Neutral Ground Wires", "Hot Line Black Phase Voltage Oscillating Power", "Neutral Wire White Circuit Return Current Path", "Ground Wire Bare Copper Earth Fault Safety Path",
        "Circuit Breakers Thermal Magnetic Overcurrent Trip", "Standard 15A and 20A Household Circuit Loads", "Wire Gauge AWG 14 AWG 12 Ampacity Capacity", "Voltage Drop Long Extension Cords Wire Sizing",
        "Ground Fault Circuit Interrupter GFCI 5mA Trip", "Arc Fault Circuit Interrupter AFCI Sparks Fire", "GFCI vs Standard Breaker Personal Shock Protection", "Receptacle Outlets Standard Tamper-Resistant",
        "Three-Prong Receptacles Ground Pin Polarity Safe", "Polarized Plugs Wider Neutral Blade Safety Hot", "Light Switches Single Pole vs Three-Way Multi", "Electrical Junction Boxes Metal Plastic Code Enclose",
        "Wire Nuts Twist Connectors Spring Coil Secure", "Wago Push-In Lever Wire Connectors Quick Lock", "Multimeter Testing AC Volts DC Volts Continuity", "Non-Contact Voltage Tester Pen Induction Probe",
        "Testing Continuity Zero Ohms Broken Wire Beep", "Main Electrical Service Panel 100A 200A Breaker", "Main Disconnect Switch Emergency Whole House Shut", "Subpanels Detached Garage Heavy Load Distribution",
        "Overloaded Circuits Cascading Power Strip Dangers", "Appliance Wattage Calculations Amps x Volts Watts", "Extension Cord Safety Gauge Indoor vs Outdoor", "Generator Transfer Switches Backfeed Grid Safety",
        "Lockout-Tagout LOTO Isolation Working on Circuits", "Home Plumbing Water Main Shutoff Gate Ball Valve", "Pressure Reducing Valves PRV High Municipal Water", "Water Hammer Arresters Hydraulic Shock Damper",
        "P-Trap Water Seal Sewer Gas Trap Evaporation", "Garbage Disposal Jam Unsticking Hex Key Reset", "Toilet Flapper Valve Fill Float Water Leak Check", "Gas Shutoff Valves Quarter-Turn Yellow Pipes Smell",
        "Natural Gas vs Propane Density Venting Leak Safety", "Smoke Detectors Ionization vs Photoelectric Fire", "Carbon Monoxide Detectors CO PPM Threshold Alarm"
    ]),
    ("cat_prac_food_sci", "practical_life", "Culinary Science and Food Chemistry", [
        "Maillard Reaction Amino Acid Reducing Sugar Brown", "Maillard Optimal Window 140C-165C Moisture Barrier", "Surface Moisture Removal Patting Meat Searing Dry", "Caramelization Thermal Pyrolysis Pure Sugars 160C",
        "Emulsions Water Oil Suspension Surfactants", "Emulsifiers Lecithin Egg Yolks Mustard Amphiphilic", "Mayonnaise Stable Cold Oil-in-Water Emulsion", "Hollandaise Warm Butter-in-Egg Emulsion Whisking",
        "Emulsion Breakdown Temperature Mechanical Shearing", "Protein Denaturation Heat Acid Salt Unfolding", "Protein Coagulation Entangled Gel Matrix Structure", "Collagen to Gelatin Conversion Slow Braise Heat",
        "Muscle Fiber Contraction Moisture Squeeze Toughness", "Resting Cooked Meat Juice Redistribution Pressure", "Dry Brining Osmosis Salt Penetration Hydration", "Wet Brining Osmotic Equilibrium Water Weight Loss",
        "Marinades Acid vs Enzymatic Surface Tenderizing", "Starch Gelatinization Heating Water Swelling Thick", "Roux Flour Fat Ratio Starch Gelatinization Sauce", "Cornstarch Slurry Retrogradation Shearing Sauce",
        "Baking Leavening Agents Chemical Biological Steam", "Baking Soda Sodium Bicarbonate Acid Activator", "Baking Powder Single vs Double-Acting Heat Activation", "Yeast Fermentation Sugar CO2 Alcohol Gluten Rise",
        "Gluten Formation Gliadin Glutenin Water Kneading", "Gluten Inhibition Fats Sugar Acids Shortening", "Egg White Foam Mechanical Denaturation Air Pockets", "Egg Yolk Emulsification Richness Sauce Custard",
        "Temperatures Egg Coagulation Whites 62C Yolks 65C", "Oil Smoke Points Refined vs Unrefined Triglyceride", "Deep Frying Steam Barrier Moisture Repelling Oil", "Caramelized Onions Long Slow Flavors Enzymatic",
        "Vegetable Pectin Breakdown Heat Softening Firmness", "Acidity pH Food Preservation Pickling Canning Safety", "Botulism Clostridium Anaerobic Toxin High Heat", "Sous-Vide Precision Thermal Control Water Baths",
        "Freezing Crystal Formation Fast Flash Freezing Cell", "Fermentation Lactic Acid Bacteria Sauerkraut Kimchi", "Enzymatic Browning Polyphenol Oxidase Lemon Acid", "Salt Enhancing Sweetness Masking Bitterness Taste"
    ]),
    ("cat_prac_safety_skills", "practical_life", "Practical Safety, Water and Life Skills", [
        "Fire Triangle Oxygen Fuel Heat Combustion", "Fire Tetrahedron Chemical Chain Reaction Radical", "Fire Extinguisher Classes A Trash Wood B Liquids", "Fire Extinguisher Classes C Electrical D Metals K",
        "Fire Extinguisher PASS Method Pull Aim Squeeze Sweep", "Grease Fire Response Smothering Lid Avoid Water", "Chemical Storage Incompatibility Bleach Ammonia Gas", "Bleach and Vinegar Toxic Chlorine Gas Hazard",
        "Water Purification Boiling Minimum 1 Minute Rolling", "Water Filtration Sediment Ceramic Activated Carbon", "Chemical Water Disinfection Chlorine Dioxide Iodine", "UV Water Disinfection Ultraviolet Radiation Clear",
        "Emergency Water Storage Food-Grade Containers Rotate", "Essential Knots Bowline King of Knots Fixed Loop", "Square Knot Reef Knot Joining Equal Lines Binding", "Clove Hitch Securing Line to Post Quick Temporary",
        "Taut-Line Hitch Adjustable Friction Loop Guyline", "Figure-Eight Knot Stopper Knot Safety Climbers", "Sheet Bend Joining Two Unequal Thickness Ropes", "Trucker's Hitch High-Tension Lashing 3:1 Advantage",
        "Rope Care Synthetic Nylon vs Polypropylene UV Sun", "Compass Magnetic Needle Heading Orientation Map", "Magnetic Declination True North vs Magnetic North", "Topographic Maps Contour Lines Elevation Interval",
        "Sun and Shadow Stick Navigation Cardinal Points", "Vehicle Tire Pressure Hydroplaning Tread Depth PSI", "Automotive Jumper Cable Sequence Red Black Ground", "Changing Flat Tire Lug Nut Star Pattern Jack Stand",
        "Vehicle Emergency Kit Flares First Aid Blankets", "Flashlight Batteries Lithium vs Alkaline Storage Leak", "Radio Emergency Weather Hand-Crank Solar NOAA", "Personal Emergency Go-Bag 72-Hour Evacuation Supply",
        "Hypothermia Prevention Layering Base Mid Shell Wind", "Cotton Kills Wet Fabric Evaporative Heat Loss", "Shelter Building Thermal Insulation Debris Bed Ground", "Solar Still Condensation Soil Evaporation Water",
        "Signal Mirror Flash Sunlight Aircraft Search Rescue", "Ground Emergency Distress Signals Three Fires Whistle", "Situational Awareness OODA Loop Observe Orient Act", "Mental Resilience Survival Rule of Threes Air Water"
    ])
]

def generate_group_health_arts_prac():
    concepts = []
    for cat_id, dom_id, cat_name, terms in HEALTH_ARTS_PRAC_CATEGORIES:
        for i, term in enumerate(terms):
            slug = slugify(term)
            cid = f"c_{cat_id}_{slug}"
            name = term
            defn = f"A foundational concept in {cat_name}: {term}."
            expl = f"{term} is essential to {cat_name}. It establishes the empirical biological, design, or applied mechanical principles governing health, aesthetics, or daily problem-solving."
            
            prev_rel = []
            if i > 0:
                prev_term = terms[i-1]
                prev_slug = slugify(prev_term)
                prev_rel.append((f"c_{cat_id}_{prev_slug}", "related_to", f"Interconnected with {prev_term} in {cat_name}.", 0.9))
                
            concepts.append(make_concept_obj(
                cid, dom_id, cat_id, name, defn, expl,
                [("Discipline", cat_name), ("Domain", dom_id)],
                ["Professional applied practice and system modeling.", "Everyday practical analysis and safety."],
                [], "intermediate", "src_who_guidelines" if dom_id == "health_life" else "src_norman_design",
                [name.replace(" and ", " & "), name.split()[0]],
                [(f"Common intuition often oversimplifies {name}.", f"{name} follows exact physiological, structural, or practical rules.", "Scientific understanding and safety protocols clarify common misconceptions.")],
                [("principle", f"Core Principle of {name}", defn)],
                [],
                prev_rel
            ))
            
    return concepts

if __name__ == "__main__":
    c = generate_group_health_arts_prac()
    print(f"✓ Generated {len(c)} concepts for Health, Arts & Practical Group.")
