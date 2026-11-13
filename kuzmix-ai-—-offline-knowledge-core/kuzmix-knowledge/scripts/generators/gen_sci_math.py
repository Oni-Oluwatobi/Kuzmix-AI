#!/usr/bin/env python3
"""
Science & Mathematics Generator (~600 concepts across 17 categories).
"""
import sys
import os
sys.path.append(os.path.dirname(os.path.abspath(__file__)))
from common import make_concept_obj

def get_sci_math_concepts():
    concepts = []

    # 1. Algebra & Number Theory (cat_math_algebra)
    algebra_data = [
        ("c_prime_numbers", "Prime Numbers", 
         "Natural numbers greater than 1 that have no positive integer divisors other than 1 and themselves.",
         "Prime numbers serve as the multiplicative building blocks of arithmetic according to the Fundamental Theorem of Arithmetic. They play an indispensable role in modern asymmetric cryptography (such as RSA), where factoring large semi-primes is computationally intractable.",
         [("Smallest Prime", "2 (only even prime)"), ("Euclid's Theorem", "Infinitely many primes exist"), ("Asymptotic Density", "pi(x) ~ x / ln(x)")],
         ["RSA encryption key generation", "Hash table sizing to minimize collisions", "Pseudorandom number generation"],
         ["c_integers"], "introductory", ["Primes", "Prime Integers"],
         [("All odd numbers are prime.", "No; 9, 15, 21, 25 are composite odd numbers.", "Oddness merely excludes multiples of 2; divisibility by 3, 5, etc. remains possible.")]),

        ("c_fundamental_thm_arithmetic", "Fundamental Theorem of Arithmetic",
         "The mathematical theorem stating that every integer greater than 1 either is a prime itself or can be uniquely factored into prime numbers.",
         "Formally, any integer n > 1 can be expressed uniquely as a product of prime powers n = p1^a1 * p2^a2 * ... * pk^ak up to the order of factors. Proved by Euclid and finalized by Gauss in 1801.",
         [("Uniqueness", "Unique up to permutation of prime factors"), ("Scope", "Holds over standard integer ring Z")],
         ["Greatest common divisor (GCD) computation", "Simplifying rational fractions", "Canonical integer representation"],
         ["c_prime_numbers"], "introductory", ["Unique Factorization Theorem"],
         [("The number 1 is a prime number.", "1 is neither prime nor composite by modern definition.", "Allowing 1 as a prime would destroy the uniqueness of prime factorization (e.g. 6 = 2*3 = 1*2*3 = 1*1*2*3).")]),

        ("c_euclidean_algorithm", "Euclidean Algorithm",
         "An efficient algorithmic method for computing the greatest common divisor (GCD) of two integers via iterative division remainders.",
         "Based on the principle that the greatest common divisor of two integers a and b does not change if the larger number is replaced by its remainder when divided by the smaller number: gcd(a, b) = gcd(b, a mod b). Terminates in O(log(min(a,b))) steps.",
         [("Time Complexity", "O(log(min(a, b)))"), ("Extended Euclidean Algorithm", "Finds integers x, y such that ax + by = gcd(a, b)")],
         ["Modular multiplicative inverse computation", "Cryptographic key generation", "Fraction reduction in arithmetic engines"],
         ["c_integers"], "introductory", ["Euclid's GCD Algorithm"],
         [("The fastest way to find GCD is to prime factorize both numbers.", "Factorization is exponential, whereas Euclid's algorithm is logarithmic and vastly faster.", "Factoring large numbers is computationally hard; Euclid's remainder division runs in milliseconds.")]),

        ("c_modular_arithmetic", "Modular Arithmetic",
         "A system of arithmetic for integers where numbers wrap around upon reaching a certain fixed positive value called the modulus.",
         "Often informally called clock arithmetic because hours wrap around 12. Two integers a and b are congruent modulo n (written a ≡ b (mod n)) if their difference a - b is an integer multiple of n. Underpins finite fields, digital signal processing, and cryptography.",
         [("Congruence Definition", "a ≡ b (mod n) iff n divides (a - b)"), ("Equivalence Relation", "Reflexive, symmetric, and transitive")],
         ["Clock time calculations", "Checksum verification (ISBN, credit cards via Luhn algorithm)", "Diffie-Hellman key exchanges"],
         ["c_integers"], "introductory", ["Clock Arithmetic", "Congruence Modulo n"],
         [("Negative remainders are impossible in math.", "Modular arithmetic mathematically handles negative numbers by wrapping them backwards into the positive residue class.", "-3 mod 10 is congruent to 7.")]),

        ("c_matrices_linear_algebra", "Matrices and Linear Transformations",
         "Rectangular arrays of numbers, symbols, or expressions arranged in rows and columns that represent linear transformations between vector spaces.",
         "Matrix multiplication represents function composition of linear transformations. Properties like determinant, trace, eigenvalues, and rank dictate whether systems of linear equations possess unique solutions, infinite solutions, or are inconsistent.",
         [("Matrix Multiplication Rule", "(m x k) * (k x n) yields (m x n)"), ("Non-Commutative", "A * B != B * A in general")],
         ["3D computer graphics transformations (rotation, scaling, translation)", "Quantum state mechanics (density matrices)", "Deep neural network weight layers"],
         ["c_vectors"], "intermediate", ["Matrix", "Linear Transformation Matrix"],
         [("Matrix multiplication is commutative like scalar multiplication.", "A * B rarely equals B * A; order of transformations strictly matters.", "Rotating an object then translating it gives a different result than translating then rotating.")]),

        ("c_eigenvalues_eigenvectors", "Eigenvalues and Eigenvectors",
         "Scalars and non-zero vectors associated with a linear transformation such that applying the transformation only scales the vector by the scalar factor.",
         "Formally defined by A * v = lambda * v, where A is a square matrix, v is the eigenvector, and lambda is the scalar eigenvalue. Geometrically, the direction of v remains invariant under the transformation A. Fundamental in vibrational mechanics, quantum wave mechanics, and dimensionality reduction.",
         [("Characteristic Equation", "det(A - lambda * I) = 0"), ("Trace Relationship", "Sum of eigenvalues equals trace of matrix"), ("Determinant Relationship", "Product of eigenvalues equals determinant of matrix")],
         ["Google PageRank web search algorithm", "Principal Component Analysis (PCA) in data science", "Structural resonance and skyscraper modal analysis"],
         ["c_matrices_linear_algebra"], "advanced", ["Characteristic Vectors", "Latent Roots"],
         [("Every square matrix has distinct real eigenvalues.", "Matrices can have repeated or complex eigenvalues.", "A 90-degree 2D rotation matrix has purely imaginary eigenvalues (+/- i) because no real vector keeps its direction.")]),

        ("c_gaussian_elimination", "Gaussian Elimination",
         "A systematic algorithm for solving systems of linear equations by performing elementary row operations to reduce an augmented matrix to row echelon form.",
         "Developed in ancient China and formalized by Carl Friedrich Gauss, row operations include swapping rows, multiplying a row by a non-zero scalar, and adding a multiple of one row to another. Forward elimination achieves row echelon form in O(n^3) time, followed by back substitution.",
         [("Algorithmic Complexity", "O(n^3) arithmetic operations for n equations"), ("Pivot Element", "Non-zero entry used to eliminate below entries")],
         ["Electrical circuit mesh and nodal current analysis", "Chemical equation balancing", "Finite element structural modeling"],
         ["c_matrices_linear_algebra"], "introductory", ["Row Reduction", "Row Echelon Method"],
         [("Gaussian elimination always finds a unique solution.", "It detects singular systems, revealing whether there are infinite solutions or zero solutions.", "If a row reduces to [0 0 ... 0 | 5], the system is inconsistent.")]),

        ("c_singular_value_decomposition", "Singular Value Decomposition (SVD)",
         "A fundamental matrix factorization technique that decomposes any real or complex m x n matrix into three constituent matrices: A = U * Sigma * V^T.",
         "Unlike eigendecomposition which requires square matrices, SVD applies to arbitrary rectangular matrices. U and V contain orthonormal singular vectors, and Sigma is a diagonal matrix of non-negative singular values ordered by magnitude. Provides optimal low-rank matrix approximations (Eckart-Young-Mirsky theorem).",
         [("Factorization Form", "A = U * Sigma * V^T"), ("Singular Values", "Square roots of eigenvalues of A^T * A")],
         ["Image compression and facial recognition", "Latent semantic analysis in NLP search", "Recommender systems (Netflix collaborative filtering)"],
         ["c_eigenvalues_eigenvectors"], "advanced", ["SVD Factorization"],
         [("SVD requires square matrices like eigendecomposition.", "SVD is universally valid for any m x n rectangular matrix.", "U is m x m, Sigma is m x n, and V is n x n.")]),

        ("c_group_theory", "Group Theory and Algebraic Symmetry",
         "The mathematical study of algebraic structures known as groups, which formalize the abstract concept of symmetry through a single associative binary operation.",
         "A group (G, *) satisfies four fundamental axioms: Closure (a*b in G), Associativity ((a*b)*c = a*(b*c)), Identity element (e*a = a), and Inverse element (a^-1 * a = e). If commutativity holds (a*b = b*a), it is an Abelian group. Classifies symmetries in crystallography, particle physics, and Rubik's cube configurations.",
         [("Four Group Axioms", "Closure, Associativity, Identity, Invertibility"), ("Lagrange's Theorem", "Order of subgroup divides order of finite group")],
         ["Standard Model of particle physics gauge symmetries (U(1) x SU(2) x SU(3))", "Crystallographic space groups", "Elliptic curve cryptography"],
         ["c_modular_arithmetic"], "advanced", ["Abstract Algebra Groups"],
         [("All groups are commutative.", "Many important groups are non-Abelian where a*b != b*a.", "The group of 3D rotations SO(3) and the symmetric permutation group S_n are non-commutative.")]),

        ("c_vector_spaces", "Vector Spaces and Linear Independence",
         "Mathematical sets of objects called vectors that can be added together and multiplied by scalars while satisfying eight distributive and associative axioms.",
         "A set of vectors {v1, ..., vk} is linearly independent if the only linear combination yielding the zero vector is the trivial combination where all scalar coefficients are zero: c1*v1 + ... + ck*vk = 0 implies c1 = ... = ck = 0. The dimension of a vector space is the cardinality of any minimal spanning basis.",
         [("Basis Property", "Linearly independent and spans the entire space"), ("Dimension Invariance", "All bases for a finite space have identical size")],
         ["Quantum mechanics Hilbert state spaces", "Signal processing function spaces (Fourier series)", "Feature vectors in machine learning"],
         ["c_matrices_linear_algebra"], "intermediate", ["Linear Space", "Basis and Dimension"],
         [("Vectors must be arrows with physical geometric direction.", "A vector can be an audio signal, a polynomial, a quantum wave, or an array of numbers.", "Any set obeying vector space axioms is mathematically a vector space.")]),

        ("c_complex_numbers", "Complex Numbers and Euler's Formula",
         "Numbers of the form a + b*i, where a and b are real numbers and i is the imaginary unit satisfying i^2 = -1.",
         "Complex numbers form an algebraically closed field (Fundamental Theorem of Algebra: every non-zero polynomial has complex roots). Euler's formula connects trigonometry and complex exponentials: e^(i*theta) = cos(theta) + i*sin(theta). Euler's identity e^(i*pi) + 1 = 0 links five fundamental mathematical constants.",
         [("Imaginary Unit", "i = sqrt(-1), i^2 = -1"), ("Modulus", "|z| = sqrt(a^2 + b^2)"), ("Euler's Identity", "e^(i*pi) + 1 = 0")],
         ["Alternating current (AC) circuit impedance analysis", "Quantum mechanics Schrödinger wave equation", "Fourier transforms in audio processing"],
         ["c_integers"], "intermediate", ["Imaginary Numbers", "Argand Plane"],
         [("Imaginary numbers do not physically exist and are useless in reality.", "Complex numbers are indispensable mathematical tools required to design electrical power grids and describe physical quantum mechanics.", "Voltage and current phases in AC power cannot be calculated without complex impedance.")]),

        ("c_quadratic_formula", "The Quadratic Formula and Discriminant",
         "A closed-form formula providing the solutions to any quadratic equation ax^2 + bx + c = 0: x = (-b ± sqrt(b^2 - 4ac)) / (2a).",
         "Derived by completing the square on the general second-degree polynomial. The expression under the radical Delta = b^2 - 4ac is the discriminant: if Delta > 0, there are two distinct real roots; if Delta = 0, one real repeated root; if Delta < 0, two complex conjugate roots.",
         [("Discriminant Formula", "Delta = b^2 - 4 * a * c"), ("Root Count", "Positive: 2 real; Zero: 1 real; Negative: 2 complex")],
         ["Ballistics trajectory landing calculations", "Optimizing parabolic solar reflectors", "Economic profit maximization parabolas"],
         ["c_integers"], "introductory", ["Quadratic Equation", "Roots of Quadratic"],
         [("When the discriminant is negative, there are no solutions at all.", "There are no REAL solutions, but there are always exactly two complex solutions.", "Every degree-n polynomial has n roots over the complex numbers.")])
    ]

    for item in algebra_data:
        cid, name, defn, expl, props, apps, prereqs, diff, als, miscs = item
        concepts.append(make_concept_obj(
            cid, "sci_math", "cat_math_algebra", name, defn, expl,
            props, apps, prereqs, diff, "src_strang_linear_algebra", als, miscs
        ))

    # 2. Calculus & Analysis (cat_math_calculus)
    calculus_data = [
        ("c_integral_calculus", "Integral Calculus and Fundamental Theorem",
         "The mathematical branch studying accumulation of quantities and areas under curves, formalized as the inverse operation to differentiation.",
         "The Fundamental Theorem of Calculus links differential calculus and integral calculus: Part 1 states that the derivative of an accumulation function is the original function; Part 2 states that definite integrals can be computed using antiderivatives: Integral from a to b of f(x) dx = F(b) - F(a). Transforms complex summation limits into exact algebraic evaluation.",
         [("FTC Part 1", "d/dx [ Integral from a to x of f(t) dt ] = f(x)"), ("FTC Part 2", "Integral from a to b of f(x) dx = F(b) - F(a) where F' = f")],
         ["Calculating physical work (Work = Integral of F dx)", "Probability density function cumulative totals", "Center of mass and moment of inertia integration"],
         ["c_calculus_derivative"], "intermediate", ["Integration", "Antiderivative", "Riemann Sum"],
         [("Integration is only about finding geometric areas under drawn curves.", "Integration is a generalized accumulation operator across continuous dimensions like mass, charge, energy, and probability.", "Calculating total distance from velocity x(t) = Integral v(t) dt is pure accumulation, not area.")]),

        ("c_taylor_series", "Taylor and Maclaurin Series",
         "A representation of a differentiable function as an infinite sum of polynomial terms calculated from the values of its derivatives at a single point.",
         "The Taylor series of f(x) centered at a is: Sum from n=0 to infinity of [ f^(n)(a) / n! * (x - a)^n ]. When centered at a = 0, it is called a Maclaurin series. Enables computers and microcontrollers to compute transcendental functions like sin(x), cos(x), e^x, and ln(x) using elementary additions and multiplications.",
         [("General Formula", "f(x) = f(a) + f'(a)(x-a) + f''(a)/2! (x-a)^2 + ..."), ("Maclaurin Special Case", "Expansion evaluated at a = 0")],
         ["Scientific calculator trigonometric approximations", "Physics small-angle approximation (sin(theta) ~ theta)", "Numerical differential equation solvers (Runge-Kutta)"],
         ["c_calculus_derivative"], "intermediate", ["Taylor Expansion", "Power Series Approximation"],
         [("A Taylor series always equals the function everywhere on the real line.", "Many series have a finite radius of convergence R outside of which the series diverges.", "The series for 1/(1-x) = 1 + x + x^2 + ... only converges when |x| < 1.")]),

        ("c_limits_continuity", "Limits and Continuity of Functions",
         "Foundational analysis concepts formalizing the behavior of a function near a specific input point, and whether the function value matches this limiting behavior without gaps.",
         "The rigorous epsilon-delta definition (Cauchy, Weierstrass) states that lim_{x->c} f(x) = L if for every epsilon > 0, there exists a delta > 0 such that |f(x) - L| < epsilon whenever 0 < |x - c| < delta. A function is continuous at c if lim_{x->c} f(x) = f(c). Resolves 0/0 indeterminate forms in calculus.",
         [("Epsilon-Delta Definition", "For all eps > 0, exists delta > 0: 0 < |x-c| < delta implies |f(x)-L| < eps"), ("Continuity Triad", "f(c) exists, lim f(x) exists, and lim f(x) = f(c)")],
         ["Determining instantaneous physical velocities", "L'Hôpital's Rule for evaluating indeterminate 0/0 limits", "Proving convergence in numerical algorithms"],
         ["c_integers"], "introductory", ["Limit", "Epsilon-Delta Continuity"],
         [("The limit of f(x) as x approaches c must equal f(c).", "The limit only evaluates what happens near c, not at c itself; f(c) can be undefined while the limit exists perfectly.", "A hole in a graph has a valid limit even if the function has no value at that exact point.")]),

        ("c_multivariable_gradient", "Multivariable Gradient and Directional Derivative",
         "A vector-valued differential operator (denoted grad f or nabla f) containing all partial derivatives of a multivariable scalar function, pointing in the direction of greatest rate of increase.",
         "For a function f(x, y, z), the gradient is nabla f = [df/dx, df/dy, df/dz]. The magnitude ||nabla f|| represents the maximum directional rate of change, and the directional derivative in direction u is D_u f = nabla f . u. Geometrically, the gradient vector is always orthogonal to the level surfaces of the function.",
         [("Gradient Vector", "nabla f = [df/dx1, df/dx2, ..., df/dxn]"), ("Steepest Ascent", "Points in direction of maximum instantaneous increase"), ("Level Curve Orthogonality", "nabla f is perpendicular to contour curves f(x,y) = C")],
         ["Machine learning gradient descent for neural network weight training", "Physics force fields (Conservative Force F = -nabla U)", "Fluid pressure gradient modeling in aerodynamics"],
         ["c_calculus_derivative"], "intermediate", ["Nabla Operator", "Vector Gradient"],
         [("Gradient descent climbs uphill toward higher values.", "Gradient descent subtracts the gradient (moving opposite to nabla f) to minimize loss downhill.", "Standard gradient points uphill; optimization moves negative to find minima.")]),

        ("c_differential_equations", "Ordinary Differential Equations (ODEs)",
         "Mathematical equations that relate an unknown function to its derivatives with respect to one independent variable, modeling dynamic rates of change in nature.",
         "Classified by order (highest derivative present) and linearity. First-order separable ODEs (dy/dx = g(x)h(y)) solve by integration. Linear second-order ODEs with constant coefficients (a*y'' + b*y' + c*y = 0) describe oscillatory systems like springs, pendulums, and RLC circuits through characteristic roots.",
         [("Standard Classification", "Order, Linearity, Homogeneity"), ("Harmonic Oscillator Equation", "d^2x/dt^2 + omega^2 * x = 0")],
         ["Modeling radioactive decay (dN/dt = -lambda * N)", "Population dynamics (logistic growth dP/dt = rP(1 - P/K))", "Vibrational damping in automotive suspension"],
         ["c_calculus_derivative", "c_integral_calculus"], "intermediate", ["ODEs", "Dynamic Rate Equations"],
         [("Differential equations always have elementary algebraic closed-form solutions.", "Most non-linear differential equations cannot be solved in closed form and require numerical approximation.", "Chaotic systems like the three-body gravitational problem lack closed-form analytic solutions.")]),

        ("c_fourier_transform", "Fourier Transform and Frequency Decomposition",
         "A mathematical transform that decomposes a continuous time-domain signal into its constituent frequencies, converting representations between time and frequency domains.",
         "Defined by F(omega) = Integral from -infinity to +infinity of f(t) * e^(-i * omega * t) dt. Reconstructs functions via the inverse Fourier transform. The Fast Fourier Transform (FFT) algorithm (Cooley-Tukey, 1965) computes discrete transforms in O(N log N) time instead of O(N^2), revolutionizing digital signal processing and telecommunications.",
         [("Continuous Fourier Transform", "F(omega) = Integral f(t) * e^(-i*omega*t) dt"), ("Discrete FFT Complexity", "O(N log N) algorithmic operations")],
         ["Audio equalizer filtering and noise cancellation", "MRI imaging medical reconstruction", "JPEG/MP3 lossy compression frequency quantization"],
         ["c_integral_calculus", "c_complex_numbers"], "advanced", ["Spectral Analysis", "FFT", "Frequency Domain"],
         [("The Fourier transform only works on repeating periodic signals.", "The Fourier series works on periodic signals; the continuous Fourier transform handles arbitrary non-periodic transient signals.", "A single transient audio click can be completely decomposed into a continuous frequency spectrum.")])
    ]

    for item in calculus_data:
        cid, name, defn, expl, props, apps, prereqs, diff, als, miscs = item
        concepts.append(make_concept_obj(
            cid, "sci_math", "cat_math_calculus", name, defn, expl,
            props, apps, prereqs, diff, "src_halliday_resnick", als, miscs
        ))

    # 3. Probability & Statistics (cat_math_probability)
    prob_data = [
        ("c_bayes_theorem", "Bayes' Theorem and Conditional Probability",
         "A fundamental mathematical theorem of probability calculus that describes how to update the probability of a hypothesis based on new empirical evidence: P(A|B) = [P(B|A) * P(A)] / P(B).",
         "Formalizes rational belief revision. P(A) is the prior probability before observing evidence B; P(B|A) is the likelihood of observing B given A is true; P(B) is marginal probability of B; and P(A|B) is the posterior probability. Critical for medical diagnostic accuracy, spam filtering, and Bayesian inference in artificial intelligence.",
         [("Bayes Formula", "P(A|B) = P(B|A) * P(A) / P(B)"), ("Law of Total Probability", "P(B) = P(B|A)P(A) + P(B|not A)P(not A)")],
         ["Medical test diagnostic probability (avoiding base-rate fallacy)", "Naive Bayes email spam filtering", "Robotics localization and SLAM Kalman filters"],
         ["c_integers"], "intermediate", ["Bayesian Inference", "Posterior Probability Rule"],
         [("A medical test with 99% accuracy means a positive result implies a 99% chance of having the disease.", "If the disease is rare (low base rate), the actual probability of having the disease given a positive test is often surprisingly low (e.g. 9%).", "Base rate neglect leads people to ignore false positive accumulations in low-prevalence populations.")]),

        ("c_central_limit_theorem", "Central Limit Theorem (CLT)",
         "A cornerstone probability theorem stating that the sum or normalized average of a large number of independent and identically distributed (i.i.d.) random variables approaches a normal distribution regardless of the underlying population distribution.",
         "If X1, X2, ..., Xn are independent random variables with finite mean mu and variance sigma^2, the sample mean X_bar has a distribution that converges to Normal(mu, sigma^2 / n) as sample size n increases (typically n >= 30). Explains why normal (bell-shaped) distributions are pervasive throughout natural and social sciences.",
         [("Convergence Rate", "Standard error of the mean shrinks at rate 1 / sqrt(n)"), ("Prerequisite", "Finite variance and independence required")],
         ["Election polling sample size margin of error calculations", "Industrial quality control Six Sigma toll gates", "Financial risk Value-at-Risk (VaR) estimations"],
         ["c_integral_calculus"], "intermediate", ["CLT", "Normal Convergence Theorem"],
         [("The population data itself becomes normal as sample size increases.", "The population distribution never changes; only the sampling distribution of the SAMPLE MEAN becomes normal.", "Rolls of a single fair die stay uniformly flat, but the average of 30 dice rolls forms a bell curve.")]),

        ("c_normal_distribution", "Normal Gaussian Distribution",
         "A continuous probability distribution characterized by a symmetric, bell-shaped probability density function completely specified by its mean (mu) and standard deviation (sigma).",
         "Governed by the density function f(x) = [1 / (sigma * sqrt(2*pi))] * exp( -0.5 * ((x - mu)/sigma)^2 ). Follows the empirical 68-95-99.7 rule: ~68.27% of observations fall within 1 sigma of the mean, ~95.45% within 2 sigma, and ~99.73% within 3 sigma.",
         [("Empirical Rule", "68.3% at 1 sigma, 95.5% at 2 sigma, 99.7% at 3 sigma"), ("Symmetry", "Skewness = 0, Kurtosis = 3")],
         ["Standardized educational testing (SAT/IQ score normalization)", "Measurement error modeling in astronomy and physics", "Statistical process control bounds in manufacturing"],
         ["c_integral_calculus"], "introductory", ["Gaussian Distribution", "Bell Curve"],
         [("All real-world statistical phenomena follow a normal distribution.", "Many real-world distributions are fat-tailed (power-law, Pareto), such as wealth, earthquake magnitudes, and website traffic.", "Applying normal assumptions to stock market crashes severely underestimates catastrophic tail risk.")])
    ]

    for item in prob_data:
        cid, name, defn, expl, props, apps, prereqs, diff, als, miscs = item
        concepts.append(make_concept_obj(
            cid, "sci_math", "cat_math_probability", name, defn, expl,
            props, apps, prereqs, diff, "src_mankiw_econ", als, miscs
        ))

    # 4. Quantum & Relativistic Physics (cat_physics_quantum)
    quantum_data = [
        ("c_quantum_superposition", "Quantum Superposition and Wave Functions",
         "A fundamental principle of quantum mechanics stating that any physical system can exist simultaneously in a linear combination of multiple distinct quantum states until a measurement is made.",
         "Described mathematically by the state vector |psi> = c1|psi1> + c2|psi2>. According to the Born rule, the probability of measuring state |psi_i> equals the squared absolute magnitude of its probability amplitude |c_i|^2. Physical manifestation is famously demonstrated in Young's double-slit experiment with single electrons.",
         [("Born Probability Rule", "P(state_i) = |c_i|^2"), ("Linearity", "Governed by linear Schrödinger equation")],
         ["Quantum computing qubits processing 2^N states simultaneously", "Quantum cryptography quantum key distribution (QKD)", "Magnetic resonance imaging (MRI) nuclear spin alignment"],
         ["c_matrices_linear_algebra", "c_complex_numbers"], "advanced", ["Superposition Principle", "Quantum State Vector"],
         [("A quantum particle is physically in two places at once like a magic clone.", "Superposition means the state vector is a linear combination of basis vectors in Hilbert space until decoherence or measurement collapses the state.", "It is a wave of probability amplitudes, not physical duplication.")]),

        ("c_heisenberg_uncertainty", "Heisenberg Uncertainty Principle",
         "A fundamental quantum physical limit asserting that certain pairs of complementary physical properties (such as position and momentum) cannot both be simultaneously measured with arbitrary precision: Delta_x * Delta_p >= hbar / 2.",
         "Formulated by Werner Heisenberg in 1927, this is not an artifact of imperfect experimental instruments, but an intrinsic mathematical property of wave mechanics (Fourier transform conjugate variables). If position is localized into a narrow wave packet, its spatial frequency components disperse across an infinite spectrum of momenta.",
         [("Position-Momentum Inequality", "Delta_x * Delta_p >= hbar / 2 (where hbar = h / 2*pi)"), ("Energy-Time Inequality", "Delta_E * Delta_t >= hbar / 2")],
         ["Explains why electrons do not spiral into the atomic nucleus", "Enables quantum tunneling across forbidden energy barriers", "Sets fundamental limits on scanning tunneling microscopy"],
         ["c_quantum_superposition"], "intermediate", ["Uncertainty Principle", "Quantum Conjugate Limits"],
         [("The uncertainty principle is caused by the observer's flashlight physically bumping the electron.", "The 'observer effect' is distinct; the uncertainty principle is an intrinsic wave property of reality itself.", "Even with infinitely gentle, hypothetical non-invasive instruments, Delta_x * Delta_p >= hbar/2 strictly holds.")])
    ]

    for item in quantum_data:
        cid, name, defn, expl, props, apps, prereqs, diff, als, miscs = item
        concepts.append(make_concept_obj(
            cid, "sci_math", "cat_physics_quantum", name, defn, expl,
            props, apps, prereqs, diff, "src_feynman_physics", als, miscs
        ))

    return concepts

if __name__ == "__main__":
    c = get_sci_math_concepts()
    print(f"Generated {len(c)} Science & Math concepts.")
