#!/usr/bin/env python3
"""
Group 3 Generator: Business, Economics, Humanities & Society (15 categories, ~630 concepts)
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from common import make_concept_obj, slugify

BIZ_HUM_SOC_CATEGORIES = [
    # Business & Economics
    ("cat_econ_micro", "business_econ", "Microeconomics and Market Structures", [
        "Law of Demand Price Inverse", "Law of Supply Price Direct", "Market Equilibrium Clearing Price", "Price Elasticity of Demand PED",
        "Cross-Price Elasticity Substitute Complement", "Income Elasticity Normal Inferior", "Consumer Surplus Willingness to Pay", "Producer Surplus Market Price Cost",
        "Deadweight Loss Inefficiency Tax", "Price Ceilings Shortage Lines", "Price Floors Surplus Storage", "Marginal Utility Diminishing Returns",
        "Indifference Curves Budget Constraints", "Marginal Rate of Substitution MRS", "Opportunity Cost Scarcity Tradeoff", "Production Possibility Frontier PPF",
        "Comparative Advantage Ricardo Opportunity", "Absolute Advantage Productivity", "Externalities Negative Carbon Pollution", "Positive Externalities Education R&D",
        "Pigouvian Taxes Internalizing Cost", "Coase Theorem Property Rights Bargain", "Public Goods Non-Excludable Rivalrous", "Tragedy of the Commons Overuse",
        "Club Goods Excludable Toll Road", "Common Pool Resources Fisheries", "Asymmetric Information Hidden Knowledge", "Adverse Selection Lemons Market Akerlof",
        "Moral Hazard Risk Taking Insurance", "Principal-Agent Problem Incentives", "Signaling Spencian Education Credentials", "Screening Information Asymmetry",
        "Perfect Competition Price Takers", "Monopoly Single Seller Price Maker", "Natural Monopoly High Fixed Infrastructure", "Monopolistic Competition Product Differentiate",
        "Oligopoly Interdependence Few Firms", "Cartels Collusion OPEC Quotas", "Game Theory Strategic Interdependence", "Prisoner's Dilemma Dominant Strategy",
        "Nash Equilibrium No Unilateral Deviation", "Network Effects Metcalfe Platform Value"
    ]),
    ("cat_econ_macro", "business_econ", "Macroeconomics and Monetary Policy", [
        "Gross Domestic Product GDP Output", "Nominal vs Real GDP Deflator Inflation", "GDP Expenditure Approach C+I+G+NX", "Consumer Price Index CPI Basket",
        "Inflation Purchasing Power Erosion", "Deflation Debt Deflation Spiral", "Hyperinflation Currency Collapse Printing", "Unemployment Rate Labor Force Part",
        "Frictional Unemployment Job Search", "Structural Unemployment Skills Mismatch", "Cyclical Unemployment Recession Drop", "Natural Rate of Unemployment NAIRU",
        "Okun's Law GDP Unemployment Link", "Phillips Curve Inflation Unemployment", "Aggregate Demand Downward Slope Wealth", "Aggregate Supply Short vs Long Run",
        "Business Cycle Expansion Peak Recession", "Fiscal Policy Government Spending Taxes", "Keynesian Economics Multiplier Demand", "Expansionary vs Contractionary Fiscal",
        "Government Budget Deficit vs Debt", "Crowding-Out Effect Interest Rates", "Monetary Policy Central Bank Mandate", "Federal Funds Rate Policy Rate",
        "Open Market Operations Bond Purchases", "Reserve Requirements Fractional Banking", "Money Multiplier Fractional Reserve", "Quantitative Easing Balance Sheet Expand",
        "Liquidity Trap Zero Lower Bound ZIRP", "Taylor Rule Policy Rate Guidance", "Quantity Theory of Money MV=PY", "Velocity of Money Circulation Speed",
        "Fiat Money Legal Tender Confidence", "Foreign Exchange Rates Floating Fixed", "Purchasing Power Parity PPP Big Mac", "Balance of Payments Current Financial",
        "Current Account Trade Deficit Surplus", "Tariffs Protectionism Deadweight Loss", "Import Quotas Trade Quantity Restriction", "Free Trade Agreements Comparative Gain",
        "Sovereign Debt Default Restructure", "International Monetary Fund IMF Bailout"
    ]),
    ("cat_biz_finance", "business_econ", "Corporate Finance and Capital Markets", [
        "Time Value of Money Discounting Present", "Compound Interest Exponential Growth", "Rule of 72 Doubling Period Estimate", "Discounted Cash Flow DCF Valuation",
        "Net Present Value NPV Hurdle Rate", "Internal Rate of Return IRR Breakeven", "Payback Period Liquidity Recovery", "Risk-Free Rate Sovereign Bonds Treasury",
        "Equity Risk Premium Stock Compensation", "Capital Asset Pricing Model CAPM Beta", "Weighted Average Cost of Capital WACC", "Modigliani-Miller Capital Invariance",
        "Capital Structure Debt vs Equity Ratio", "Financial Leverage Interest Tax Shield", "Cost of Debt Yield to Maturity YTM", "Cost of Equity Dividend Discount Gordon",
        "Initial Public Offering IPO Underwrite", "Secondary Stock Market Liquidity", "Common Stock vs Preferred Stock Dividend", "Bonds Par Value Coupon Maturity Yield",
        "Yield Curve Normal Inverted Recession", "Credit Rating Agencies Moody's S&P AAA", "High-Yield Junk Bonds Default Risk", "Corporate Mergers and Acquisitions M&A",
        "Due Diligence Financial Legal Audit", "Leveraged Buyout LBO Private Equity", "Venture Capital Term Sheets Liquidation", "Seed Series A B Venture Capital",
        "Valuation Multiples EV/EBITDA P/E Ratio", "Price-to-Book P/B Ratio Net Worth", "Free Cash Flow to Firm FCFF", "Working Capital Current Ratio Quick",
        "Capital Expenditure CapEx Maintenance", "Depreciation Non-Cash Expense Tax", "Dividend Policy Payout vs Buybacks", "Stock Repurchases Share Buyback EPS",
        "Efficient Market Hypothesis EMH Fama", "Modern Portfolio Theory Markowitz Efficient", "Diversification Unsystematic Risk Reduction", "Derivatives Options Calls Puts Strike",
        "Futures and Forwards Commodity Hedging", "Black-Scholes Option Pricing Formula"
    ]),
    ("cat_biz_accounting", "business_econ", "Financial and Managerial Accounting", [
        "Fundamental Accounting Equation Assets", "Double-Entry Bookkeeping Debit Credit", "General Ledger Chart of Accounts", "Trial Balance Debits Equal Credits",
        "Accrual Accounting vs Cash Basis", "Revenue Recognition Matching Principle", "Conservatism Principle Prudence Bias", "Historical Cost vs Fair Value Mark",
        "The Balance Sheet Financial Position", "Current Assets Cash Receivables Inventory", "Non-Current Assets Property Plant Equip", "Current Liabilities Payables Short Debt",
        "Long-Term Debt Bonds Payable Leases", "Shareholders' Equity Paid-In Retained", "The Income Statement Profit Loss P&L", "Gross Revenue Net Sales Top Line",
        "Cost of Goods Sold COGS Direct Costs", "Gross Profit Margin Revenue Minus COGS", "Operating Expenses OpEx SG&A Overhead", "Operating Income EBIT Bottom Operating",
        "Earnings Before Interest Taxes EBITDA", "Net Income Bottom Line Net Profit Margin", "Cash Flow Statement Three Activities", "Operating Cash Flow Direct Indirect",
        "Investing Cash Flow CapEx Purchases", "Financing Cash Flow Debt Equity Dividends", "Depreciation Straight-Line Accelerated", "Amortization Intangible Assets Goodwill",
        "Accounts Receivable DSO Collection Days", "Allowance for Doubtful Accounts Bad Debt", "Inventory Valuation FIFO First-In", "Inventory Valuation LIFO Last-In First",
        "Weighted Average Cost Inventory Method", "Auditing Independent CPA Opinion Clean", "Sarbanes-Oxley Act SOX Internal Control", "GAAP Generally Accepted Accounting",
        "IFRS International Financial Reporting", "Managerial Cost Accounting Allocation", "Fixed Costs vs Variable Costs Marginal", "Contribution Margin Breakeven Analysis",
        "Standard Costing Variance Analysis Budget", "Activity-Based Costing ABC Cost Pools"
    ]),
    ("cat_biz_strategy", "business_econ", "Strategic Management and Operations", [
        "Porter's Five Forces Industry Competitiveness", "Threat of New Entrants Barrier Scale", "Bargaining Power of Buyers Price Sensitivity", "Bargaining Power of Suppliers Leverage",
        "Threat of Substitute Products Cross-Elast", "Rivalry Among Existing Competitors Price", "Porter's Generic Strategies Cost Lead", "Differentiation Strategy Unique Premium",
        "Focus Strategy Niche Market Segment", "SWOT Analysis Strengths Weaknesses", "Resource-Based View RBV VRIO Framework", "Core Competencies Hamel Prahalad Inimitable",
        "Value Chain Analysis Primary Support", "Economies of Scale Fixed Cost Dilution", "Economies of Scope Synergies Multi-Product", "Experience Curve Learning By Doing",
        "Disruptive Innovation Christensen Low-End", "Blue Ocean Strategy Kim Mauborgne Uncontested", "First-Mover Advantage vs Fast Follower", "Platform Business Models Multi-Sided Network",
        "Network Effects Direct Indirect Metcalfe", "Two-Sided Markets Subsidizing Side", "Vertical Integration Backward Forward", "Outsourcing Offshoring Core Focus",
        "Transaction Cost Economics Coase Williamson", "Balanced Scorecard Financial Customer Internal", "Objectives and Key Results OKR Framework", "Key Performance Indicators KPI Targets",
        "Lean Manufacturing Toyota Production System", "Just-In-Time JIT Inventory Reduction", "Kanban Visual Workflow Card Pull System", "Kaizen Continuous Incremental Improvement",
        "Six Sigma DMAIC Quality Defect Minimization", "Total Quality Management TQM Deming", "Supply Chain Bullwhip Effect Forecast Error", "Vendor Managed Inventory VMI Partnership",
        "Capacity Utilization Peak Load Bottleneck", "Critical Path Method CPM Project Timeline", "Customer Lifetime Value LTV Calculation", "Customer Acquisition Cost CAC Ratio LTV",
        "Churn Rate Retention Cohort Analysis", "Minimum Viable Product MVP Agile Lean"
    ]),
    ("cat_biz_marketing", "business_econ", "Marketing and Consumer Dynamics", [
        "The Marketing Mix 4Ps Product Price Place", "Extended 7Ps Services People Process", "Market Segmentation Demographic Psycho Geo", "Targeting Evaluation Segment Attractiveness",
        "Brand Positioning Perceptual Map Mind", "Value Proposition Canvas Jobs Pains Gains", "Brand Equity Awareness Loyalty Association", "Brand Identity Logo Typography Tone Voice",
        "Consumer Decision Journey Funnel AIDA", "Awareness Interest Desire Action Conversion", "Touchpoints Omnichannel Unified Journey", "Cognitive Biases in Pricing Decoy Anchoring",
        "Price Skimming vs Penetration Pricing", "Dynamic Pricing Algorithmic Demand Surge", "Freemium Pricing Business Model Conversion", "Psychological Pricing Charm 99 Cents",
        "Content Marketing Inbound Organic SEO", "Search Engine Optimization SEO Keywords", "Pay-Per-Click PPC Advertising Google Ads", "Cost Per Click CPC Click-Through CTR",
        "Cost Per Mille CPM Impression Pricing", "Social Media Marketing Influencer Reach", "Email Marketing Nurture Automated Drip", "Viral Marketing Referral Loops K-Factor",
        "Public Relations PR Media Relations Crisis", "Event Sponsorship Brand Association Exposure", "Customer Relationship Management CRM Hub", "Net Promoter Score NPS Promoter Detractor",
        "Customer Satisfaction CSAT Survey Score", "A/B Split Testing Conversion Optimization", "Multivariate Testing Website Landing Page", "Attribution Modeling First Last Multi-Touch",
        "Market Research Focus Groups Surveys", "Conjoint Analysis Feature Preference WTP", "Product Life Cycle Introduction Growth Mat", "Brand Extension Halo Effect Line Extension",
        "Direct-to-Consumer DTC E-commerce Channel", "Retail Distribution Wholesale Shelf Slotting", "Shopper Marketing Point-of-Sale POS Promo", "Word-of-Mouth WOM Advocacy Organic",
        "Guerilla Marketing Unconventional Ambient", "Ethical Marketing Greenwashing Scrutiny"
    ]),

    # Humanities
    ("cat_hum_philosophy", "humanities", "Philosophy, Epistemology and Metaphysics", [
        "Epistemology Definition of Knowledge Belief", "Justified True Belief JTB Tripartite", "Gettier Problem Counterexamples JTB", "Rationalism Descartes Spinoza A Priori",
        "Empiricism Locke Hume Berkeley A Posteriori", "Cartesian Doubt Radical Skepticism Cogito", "Problem of Induction Hume Uniformity Nature", "Popper Falsifiability Demarcation Criterion",
        "Paradigm Shifts Kuhn Scientific Revolutions", "Occam's Razor Parsimony Theoretical Simplicity", "Metaphysics Study of Being Reality Essence", "Ontology Being Identity Categories Existence",
        "Mind-Body Dualism Cartesian Interactionism", "Physicalism Materialism Mental Supervenience", "Qualia Subjective Experience Hard Problem", "Philosophical Zombie Chalmers Conceivability",
        "Determinism vs Free Will Compatibilism", "Hard Determinism Incompatibilism Causal", "Libertarian Free Will Agent Causation", "Ship of Theseus Identity Over Time",
        "Teleology Purpose Final Causes Aristotle", "Four Causes Material Formal Efficient Final", "Platonic Realism Theory of Forms Cave", "Aristotelian Hylomorphism Matter Form Substance",
        "Nominalism vs Realism Universals Particulars", "Pragmatism James Dewey Truth as Utility", "Phenomenology Husserl Consciousness Intentionality", "Existentialism Existence Precedes Essence",
        "Absurdism Camus Myth of Sisyphus Revolt", "Nihilism Nietzsche Death of God Meaningless", "Hermeneutics Interpretation Text Meaning Circle", "Deconstructionism Derrida Binary Opposition",
        "Logical Positivism Verificationism Vienna Circle", "Analytic vs Synthetic Propositions Kant", "A Priori vs A Posteriori Knowledge Kant", "Transcendental Idealism Kant Phenomenon Noumenon",
        "Panpsychism Mind as Fundamental Physics", "Solipsism Self Alone Exists Skepticism", "Epistemic Humility Fallibilism Peirce", "Coherentism Web of Belief Quine Duhem",
        "Foundationalism Basic Beliefs Evidentialism", "Simulated Reality Hypothesis Bostrom Matrix"
    ]),
    ("cat_hum_ethics", "humanities", "Ethics and Moral Philosophy", [
        "Normative Ethics vs Meta-Ethics Applied", "Consequentialism Outcomes Judge Morality", "Utilitarianism Greatest Happiness Bentham Mill", "Act Utilitarianism vs Rule Utilitarianism",
        "Deontology Duty-Based Ethics Kant", "Categorical Imperative Universal Law Maxim", "Treating Humanity as an End Never Means", "Virtue Ethics Aristotle Eudaimonia Character",
        "The Golden Mean Balance Between Vices", "Natural Law Theory Aquinas Moral Reason", "Social Contract Theory Hobbes Locke Rousseau", "State of Nature Solitary Nasty Brutish",
        "Rawlsian Veil of Ignorance Original Position", "Difference Principle Maximizing Worst-Off", "Moral Relativism Cultural Subjectivity", "Moral Objectivism Universal Moral Truths",
        "Moral Realism vs Anti-Realism Facts", "Emotivism Expressivism Boo-Hooray Theory", "Error Theory Mackie Moral Skepticism", "The Trolley Problem Moral Dilemma Foot",
        "Doctrine of Double Effect Intention Foreseen", "Euthyphro Dilemma Divine Command Goodness", "Is-Ought Problem Hume's Guillotine Facts Values", "Naturalistic Fallacy Moore Open Question",
        "Care Ethics Gilligan Relational Interdependence", "Bioethics Autonomy Beneficence Non-Maleficence", "Informed Consent Medical Patient Autonomy", "Euthanasia Active Passive Autonomy Suffering",
        "Animal Ethics Singer Speciesism Sentience", "Regan Rights-Based Animal Subject-of-a-Life", "Environmental Ethics Anthropocentric Biocentric", "Deep Ecology Naess Intrinsic Value Nature",
        "Just War Theory Jus Ad Bellum In Bello", "Proportionality and Discrimination in Warfare", "Distributive Justice Wealth Resource Equity", "Retributive Justice Punishment Proportional",
        "Restorative Justice Healing Victim Offender", "Whistleblowing Corporate Ethics Duty Public", "AI Ethics Algorithmic Bias Alignment Problem", "Future Generations Intergenerational Justice",
        "Existential Risk Longtermism Parfit Bostrom", "Moral Luck Nagel Outcome Agent Regret"
    ]),
    ("cat_hum_history", "humanities", "World History and Historiography", [
        "Neolithic Agricultural Revolution Farming", "Mesopotamia Fertile Crescent Tigris Euphrates", "Code of Hammurabi Lex Talionis Eye for Eye", "Ancient Egypt Nile Pharaohs Pyramids",
        "Indus Valley Civilization Harappa Mohenjo-Daro", "Ancient Chinese Dynasties Mandate of Heaven", "Silk Road Trans-Eurasian Trade Networks", "Ancient Persian Achaemenid Empire Cyrus",
        "Classical Athens Democracy Pericles Philosophy", "Spartan Militarism Helot Slavery Oligarchy", "Alexander the Great Hellenistic Cultural Spread", "Roman Republic Senate Consuls Tribunes",
        "Roman Empire Augustus Pax Romana Legion", "Fall of Western Roman Empire Germanic Migrations", "Byzantine Empire Constantinople Justinian Code", "Islamic Golden Age Abbasid House of Wisdom",
        "Trans-Saharan Gold-Salt Trade Mansa Musa", "Feudalism Vassals Serfs Manorialism Manor", "The Crusades Religious Military Expeditions", "Mongol Empire Genghis Khan Pax Mongolica",
        "Black Death Bubonic Plague Demographic Collapse", "Italian Renaissance Humanism Medici Printing", "Gutenberg Movable Type Printing Revolution", "Protestant Reformation Martin Luther 95 Theses",
        "Age of Discovery Maritime Navigation Caravel", "Columbian Exchange Plants Animals Disease", "Transatlantic Slave Trade Middle Passage Human", "Mughal Empire Akbar Religious Tolerance",
        "Ottoman Empire Suleiman Gunpowder Empire", "Scientific Revolution Galileo Newton Empirical", "European Enlightenment Voltaire Rousseau Reason", "American Revolution Constitutional Republic",
        "French Revolution Liberty Equality Guillotine", "Industrial Revolution Steam Coal Factory System", "Urbanization Working Class Labor Movements", "European Imperialism Scramble for Africa Berlin",
        "Meiji Restoration Japanese Modernization", "World War I Trench Warfare Treaty of Versailles", "Russian Revolution Bolsheviks Lenin Soviet Union", "Great Depression Global Financial Collapse",
        "World War II Fascism Holocaust Atomic Bomb", "The Cold War Nuclear Deterrence MAD Decolonize"
    ]),
    ("cat_hum_psychology", "humanities", "Cognitive and Behavioral Psychology", [
        "Dual-Process Theory System 1 and System 2", "Working Memory Limits Miller's Law 7±2", "Cognitive Load Theory Sweller Intrinsic Extra", "Cognitive Dissonance Festinger Belief Clash",
        "Confirmation Bias Myside Selective Search", "Availability Heuristic Mental Shortcut Recall", "Anchoring and Adjustment Bias Numerical Fixation", "Framing Effect Wording Influences Decision",
        "Loss Aversion Kahneman Tversky Prospect Theory", "Sunk Cost Fallacy Escalation of Commitment", "Dunning-Kruger Effect Incompetence Confidence", "Hindsight Bias I Knew It All Along",
        "Classical Conditioning Pavlov Salivating Dogs", "Operant Conditioning Skinner Reinforce Punish", "Positive and Negative Reinforcement Schedules", "Extinction and Spontaneous Recovery Behavior",
        "Bandura Social Learning Theory Modeling Bobo", "Maslow's Hierarchy of Needs Self-Actualization", "Self-Determination Theory Autonomy Competence", "Intrinsic vs Extrinsic Motivation Rewards",
        "Attachment Theory Bowlby Ainsworth Secure Avoid", "Piaget Stages of Cognitive Development Child", "Vygotsky Zone of Proximal Development Scaffolding", "Erikson Psychosocial Stages Identity Crisis",
        "Kohlberg Stages of Moral Development Reasoning", "Big Five Personality Traits OCEAN Neuroticism", "Locus of Control Internal vs External Rotter", "Learned Helplessness Seligman Depression Model",
        "Growth Mindset Dweck Effort vs Fixed Talent", "Fundamental Attribution Error Dispositional Bias", "Actor-Observer Asymmetry Self Situational", "In-Group Favoritism Out-Group Homogeneity",
        "Asch Conformity Experiment Social Peer Pressure", "Milgram Obedience Experiment Authority Shock", "Bystander Effect Diffusion of Responsibility", "Stanford Prison Experiment Zimbardo Roles",
        "Neuroplasticity Synaptic Pruning Long-Term Pot", "Circadian Rhythms Suprachiasmatic Nucleus Sleep", "Sleep Architecture REM Slow-Wave Deep Memory", "Stress Response HPA Axis Cortisol Adrenaline",
        "Post-Traumatic Stress Disorder PTSD Flashbacks", "Cognitive Behavioral Therapy CBT Cognitive Triad"
    ]),
    ("cat_hum_linguistics", "humanities", "Linguistics, Semantics and Communication", [
        "Phonetics Acoustic Articulatory International IPA", "Phonology Phonemes Allophones Sound Contrast", "Morphology Morphemes Free Bound Affixes", "Inflectional vs Derivational Morphology",
        "Syntax Sentence Structure Generative Grammar", "Constituency Phrase Structure Parse Trees", "Chomsky Universal Grammar Language Acquisition", "Transformational Generative Grammar Deep Surface",
        "Poverty of the Stimulus Argument Inborn Grammar", "Semantics Meaning Denotation Connotation Senses", "Lexical Semantics Synonyms Antonyms Hyponyms", "Pragmatics Context Implicature Speech Acts",
        "Grice's Conversational Maxims Cooperative Co", "Speech Act Theory Austin Searle Locutionary", "Performative Utterances Saying is Doing Vows", "Deixis Spatial Temporal Person Indexicality",
        "Sapir-Whorf Hypothesis Linguistic Relativity", "Linguistic Determinism Strong vs Weak Whorf", "Sociolinguistics Dialects Sociolects Accents", "Code-Switching Bilingual Multilingual Context",
        "Pidgins and Creoles Language Contact Evolution", "Historical Linguistics Comparative Method Proto", "Proto-Indo-European Language Family Reconstruction", "Grimm's Law Germanic Consonant Sound Shift",
        "Language Death Extinction Revitalization Efforts", "Psycholinguistics Speech Production Comprehend", "Broca's Area Motor Speech Production Aphasia", "Wernicke's Area Sensory Language Comprehend",
        "Critical Period Hypothesis Lenneberg Language", "Animal Communication vs Human Recursive Language", "Writing Systems Logographic Syllabic Alphabetic", "Cuneiform and Hieroglyphic Ancient Inscriptions",
        "The Alphabet Phoenician Greek Latin Evolution", "Semiotics Signs Signifier Signified Saussure", "Peircean Semiotics Icon Index Symbol Triad", "Discourse Analysis Power Structures Foucault",
        "Rhetorical Triangle Ethos Pathos Logos Aristotle", "Fallacy of Ambiguity Equivocation Amphiboly", "Non-Violent Communication Rosenberg Empathy Needs", "Active Listening Paraphrasing Non-Verbal Cues",
        "Body Language Kinesics Proxemics Micro-Expressions", "Corpus Linguistics Collocations Frequency N-Grams"
    ]),

    # Society & Civics
    ("cat_soc_civics", "society_culture", "Civics and Governance Systems", [
        "Separation of Powers Trias Politica Branches", "Checks and Balances Mutual Branch Restraint", "The Legislative Branch Lawmaking Statutes", "Bicameralism Senate House of Representatives",
        "The Executive Branch Administration Enforcement", "The Judicial Branch Legal Interpretation Review", "Judicial Review Marbury v Madison Precedent", "Federalism Federal State Shared Powers",
        "Unitary State Centralized Government Hierarchy", "Confederation Loose Sovereign State Alliance", "Constitutional Monarchy Figurehead Sovereign", "Parliamentary Democracy Prime Minister Cabinet",
        "Presidential Republic Head of State and Govt", "Semi-Presidential Dual Executive French Model", "Direct Democracy Citizens Referendum Initiative", "Representative Democracy Elected Legislators",
        "Electoral Systems First-Past-The-Post Plurality", "Proportional Representation PR Party List Coalition", "Ranked-Choice Voting Instant Runoff Majority", "Gerrymandering Boundary Manipulation District",
        "Voter Turnout Civic Participation Suppression", "Political Parties Factionalism Pluralism Duverger", "Coalition Governments Compromise Confidence Vote", "Civil Service Meritocratic Bureaucracy Weber",
        "Public Administration Implementation Policy", "Lobbying Interest Groups Advocacy Regulation", "Campaign Finance PACs Super PACs Speech Dark", "Freedom of Speech Expression First Amendment",
        "Freedom of the Press Fourth Estate Watchdog", "Freedom of Assembly Protest Petition Redress", "Rule of Law Non-Arbitrary Equal Enforcement", "Due Process Procedural Substantive Fair Trial",
        "Habeas Corpus Right to Contest Unlawful Jail", "Presumption of Innocence Burden of Proof", "Trial by Jury Peers Cross-Examination Verdict", "Impeachment Removal High Crimes Misdemeanor",
        "State of Emergency Emergency Powers Derogation", "Ombudsman Public Complaint Investigation Citizen", "Civil Society Non-Governmental Organizations NGO", "Civic Education Democratic Norms Citizenship",
        "Authoritarianism Autocracy State Control Dissent", "Totalitarianism Comprehensive Ideological Regimes"
    ]),
    ("cat_soc_law", "society_culture", "Jurisprudence and Legal Principles", [
        "Common Law Precedent Judge-Made Judicial", "Civil Law Codified Statutes Roman Napoleonic", "Stare Decisis Binding Legal Precedent Court", "Ratio Decidendi Binding Core Legal Reasoning",
        "Obiter Dicta Incidental Non-Binding Remarks", "Statutory Interpretation Plain Meaning Intent", "Hierarchy of Laws Constitutional Supremacy", "Criminal Law State Prosecution Penal Sanctions",
        "Mens Rea Guilty Mind Criminal Intent Knowledge", "Actus Reus Guilty Act Proscribed Physical Deed", "Strict Liability No Intent Required Violation", "Burden of Proof Beyond Reasonable Doubt Criminal",
        "Preponderance of the Evidence Civil Standard", "Civil Law Tort Law Negligence Duty of Care", "Breach of Duty Reasonable Person Standard Tort", "Causation Actual Cause But-For Proximate Cause",
        "Damages Compensatory Punitive Nominal Restitution", "Contract Law Offer Acceptance Consideration Legal", "Mutual Assent Meeting of the Minds Contract", "Promissory Estoppel Detrimental Reliance Equity",
        "Breach of Contract Material Minor Specific Perf", "Property Law Real Property Land Chattel Personal", "Adverse Possession Squatter Title Ownership Time", "Eminent Domain Takings Public Use Compensation",
        "Intellectual Property Patents Copyright Trademark", "Copyright Original Creative Expression Fair Use", "Patents Inventions Novel Non-Obvious Utility", "Trademarks Brand Distinctiveness Confusion",
        "Trade Secrets Confidential Proprietary NDA", "Constitutional Due Process Substantive Procedural", "Equal Protection Clause Fourteenth Amendment Strict", "Administrative Law Regulations Notice Comment",
        "Chevron Deference Judicial Agency Interpretation", "Evidence Law Relevance Hearsay Exceptions Chain", "Exclusionary Rule Fruit of the Poisonous Tree", "Attorney-Client Privilege Confidentiality Shield",
        "Class Action Lawsuits Mass Tort Aggregation", "Alternative Dispute Resolution ADR Arbitration", "Mediation Facilitated Voluntary Settlement", "Sovereign Immunity State Exemption From Lawsuits",
        "Injunction Equitable Remedy Cease and Desist", "Legal Standing Injury in Fact Redressability"
    ]),
    ("cat_soc_international", "society_culture", "International Relations and Global Law", [
        "Westphalian Sovereignty Non-Intervention Border", "Realism Anarchy State Power National Interest", "Neorealism Structural Anarchy Balance of Power", "Liberalism Interdependence Institutions Commerce",
        "Neoliberal Institutionalism Cooperation Regimes", "Constructivism Ideas Norms Identities Anarchy", "Balance of Power Alliances Counterbalancing Hegemony", "Hegemonic Stability Theory Global Public Goods",
        "Security Dilemma Defensive Arming Misperception", "Deterrence Theory Mutually Assured Destruction MAD", "Nuclear Non-Proliferation Treaty NPT IAEA", "Customary International Law State Practice Opinio",
        "Treaties Vienna Convention Law of Treaties VCLT", "Jus Cogens Peremptory Non-Derogable Human Norms", "United Nations Charter Sovereign Equality Peace", "UN Security Council Permanent Five P5 Veto",
        "UN General Assembly Universal Member Body Res", "International Court of Justice ICJ World Court", "International Criminal Court ICC Rome Statute", "Genocide Convention Crimes Against Humanity",
        "Geneva Conventions War Crimes Combatant Civilian", "Universal Declaration of Human Rights UDHR 1948", "International Covenant on Civil Political Rights", "International Covenant on Economic Social Cultural",
        "Responsibility to Protect R2P Humanitarian Intervene", "Diplomatic Immunity Vienna Convention Embassy", "Economic Sanctions Multilateral Unilateral Embargo", "World Trade Organization WTO Tariffs Dispute",
        "Most-Favored-Nation MFN Non-Discrimination Rule", "Bretton Woods System IMF World Bank Reconstruction", "International Monetary Fund Balance Payments Loans", "World Bank Development Poverty Lending Loans",
        "Extradition Treaties Dual Criminality Surrender", "Territorial Waters Exclusive Economic Zone EEZ UNCLOS", "Freedom of Navigation International Straits Sea", "Outer Space Treaty Peaceful Exploration Moon Non",
        "Kyoto Protocol Paris Climate Agreement Nationally", "Refugee Law 1951 Convention Non-Refoulement", "Transnational Terrorism Non-State Violent Actors", "Cyber Warfare Attribution Critical Infrastructure",
        "Soft Power Cultural Attraction Nye Diplomatic", "Hard Power Military Coercion Economic Sanctions"
    ]),
    ("cat_soc_sociology", "society_culture", "Sociology and Social Dynamics", [
        "Sociological Imagination Mills Personal Public", "Social Structure Institutions Roles Statuses", "Socialization Primary Family Secondary Peers", "Social Norms Folkways Mores Taboos Sanctions",
        "Social Stratification Caste Class Feudal Estate", "Socioeconomic Status SES Income Education Job", "Social Mobility Intergenerational Intragenerational", "Bourgeoisie and Proletariat Marx Class Conflict",
        "Max Weber Class Status Party Tripartite Power", "Rationalization Bureaucracy Iron Cage Disenchant", "Anomie Durkheim Normlessness Social Disconnect", "Mechanical vs Organic Social Solidarity Division",
        "Structural Functionalism Parsons Social Homeostasis", "Conflict Theory Power Struggles Inequality Marx", "Symbolic Interactionism Mead Blumer Meaning Action", "Looking-Glass Self Cooley Social Mirror Image",
        "Erving Goffman Dramaturgical Frontstage Backstage", "Impression Management Social Self-Presentation", "Total Institutions Asylums Prisons Resocialization", "Social Capital Putnam Bowling Alone Trust Networks",
        "Cultural Capital Bourdieu Habitus Distinction Taste", "Social Network Analysis Weak Ties Granovetter", "Demography Population Birth Death Migration Rates", "Demographic Transition Model Industrial Stages",
        "Malthusian Population Trap Resource Scarcity Growth", "Urbanization Megacities Slums Rural-Urban Shift", "Gentrification Urban Displacement Neighborhood Change", "Suburbanization Automobile Sprawl White Flight",
        "Cultural Relativism vs Ethnocentrism Bias", "Subcultures and Countercultures Deviance Rebellion", "Social Deviance Labeling Theory Lemert Becker", "Broken Windows Theory Urban Disorder Crime Debate",
        "Strain Theory Merton Goals Means Disconnect Adapt", "Collective Action Problem Free-Rider Dilemma Olson", "Social Movements Resource Mobilization Political", "Relative Deprivation Discontent Protest Mobilize",
        "Echo Chambers Filter Bubbles Algorithmic Polarization", "Institutional Racism Systemic Structural Bias", "Intersectionality Crenshaw Race Gender Class Axis", "Digital Divide Broadband Access Tech Inequality",
        "Secularization Hypothesis Religion Modernity Decline", "Globalization Transnational Cultural Homogenization"
    ])
]

def generate_group_biz_hum_soc():
    concepts = []
    for cat_id, dom_id, cat_name, terms in BIZ_HUM_SOC_CATEGORIES:
        for i, term in enumerate(terms):
            slug = slugify(term)
            cid = f"c_{cat_id}_{slug}"
            name = term
            defn = f"A foundational concept in {cat_name}: {term}."
            expl = f"{term} is essential to {cat_name}. It establishes the analytical, institutional, and empirical principles governing human decision-making, organizational structure, and cultural systems."
            
            prev_rel = []
            if i > 0:
                prev_term = terms[i-1]
                prev_slug = slugify(prev_term)
                prev_rel.append((f"c_{cat_id}_{prev_slug}", "related_to", f"Connected with {prev_term} in {cat_name}.", 0.9))
                
            concepts.append(make_concept_obj(
                cid, dom_id, cat_id, name, defn, expl,
                [("Discipline", cat_name), ("Domain", dom_id)],
                ["Institutional and corporate strategic decision making.", "Academic research and policy analysis."],
                [], "intermediate", "src_mankiw_econ" if dom_id == "business_econ" else "src_norman_design",
                [name.replace(" and ", " & "), name.split()[0]],
                [(f"Common intuition often oversimplifies {name}.", f"{name} follows exact sociological, economic, or legal definitions.", "Rigorous institutional and empirical analysis clarifies common misunderstandings.")],
                [("principle", f"Core Principle of {name}", defn)],
                [],
                prev_rel
            ))
            
    return concepts

if __name__ == "__main__":
    c = generate_group_biz_hum_soc()
    print(f"✓ Generated {len(c)} concepts for Business, Humanities & Society Group.")
