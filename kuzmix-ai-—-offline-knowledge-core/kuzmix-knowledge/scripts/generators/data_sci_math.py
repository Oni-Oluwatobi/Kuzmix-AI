# Science and Mathematics Data Bank (~600 concepts)
# Categories: 17 categories, ~35 concepts each

SCI_MATH_DEFINITIONS = [
    # ALGEBRA & NUMBER THEORY
    ("c_prime_numbers", "cat_math_algebra", "Prime Numbers",
     "Natural numbers greater than 1 that have no positive integer divisors other than 1 and themselves.",
     "Prime numbers serve as the multiplicative building blocks of arithmetic. They are fundamental in modern cryptography (such as RSA), where factoring large semi-primes is computationally intractable.",
     [("Smallest Prime", "2 (only even prime)"), ("Euclid's Theorem", "Infinitely many primes exist")],
     ["RSA encryption key generation", "Hash table sizing to minimize collisions"]),

    ("c_fundamental_thm_arithmetic", "cat_math_algebra", "Fundamental Theorem of Arithmetic",
     "The mathematical theorem stating that every integer greater than 1 either is a prime itself or can be uniquely factored into prime numbers.",
     "Formally, any integer n > 1 can be expressed uniquely as a product of prime powers n = p1^a1 * p2^a2 * ... * pk^ak up to the order of factors. Proved by Euclid and finalized by Gauss in 1801.",
     [("Uniqueness", "Unique up to permutation of prime factors"), ("Scope", "Holds over standard integer ring Z")],
     ["Greatest common divisor (GCD) computation", "Simplifying rational fractions"]),

    ("c_euclidean_algorithm", "cat_math_algebra", "Euclidean Algorithm",
     "An efficient algorithmic method for computing the greatest common divisor (GCD) of two integers via iterative division remainders.",
     "Based on the principle that the greatest common divisor of two integers a and b does not change if the larger number is replaced by its remainder when divided by the smaller number: gcd(a, b) = gcd(b, a mod b). Terminates in O(log(min(a,b))) steps.",
     [("Time Complexity", "O(log(min(a, b)))"), ("Extended Euclidean Algorithm", "Finds integers x, y such that ax + by = gcd(a, b)")],
     ["Modular multiplicative inverse computation", "Cryptographic key generation"]),

    ("c_modular_arithmetic", "cat_math_algebra", "Modular Arithmetic",
     "A system of arithmetic for integers where numbers wrap around upon reaching a certain fixed positive value called the modulus.",
     "Often informally called clock arithmetic because hours wrap around 12. Two integers a and b are congruent modulo n (written a ≡ b (mod n)) if their difference a - b is an integer multiple of n. Underpins finite fields, digital signal processing, and cryptography.",
     [("Congruence Definition", "a ≡ b (mod n) iff n divides (a - b)"), ("Equivalence Relation", "Reflexive, symmetric, and transitive")],
     ["Clock time calculations", "Checksum verification (ISBN, credit cards via Luhn algorithm)"]),

    ("c_matrices_linear_algebra", "cat_math_algebra", "Matrices and Linear Transformations",
     "Rectangular arrays of numbers, symbols, or expressions arranged in rows and columns that represent linear transformations between vector spaces.",
     "Matrix multiplication represents function composition of linear transformations. Properties like determinant, trace, eigenvalues, and rank dictate whether systems of linear equations possess unique solutions, infinite solutions, or are inconsistent.",
     [("Matrix Multiplication Rule", "(m x k) * (k x n) yields (m x n)"), ("Non-Commutative", "A * B != B * A in general")],
     ["3D computer graphics transformations (rotation, scaling, translation)", "Quantum state mechanics"]),

    ("c_eigenvalues_eigenvectors", "cat_math_algebra", "Eigenvalues and Eigenvectors",
     "Scalars and non-zero vectors associated with a linear transformation such that applying the transformation only scales the vector by the scalar factor.",
     "Formally defined by A * v = lambda * v, where A is a square matrix, v is the eigenvector, and lambda is the scalar eigenvalue. Geometrically, the direction of v remains invariant under the transformation A. Fundamental in vibrational mechanics, quantum wave mechanics, and dimensionality reduction.",
     [("Characteristic Equation", "det(A - lambda * I) = 0"), ("Trace Relationship", "Sum of eigenvalues equals trace of matrix")],
     ["Google PageRank web search algorithm", "Principal Component Analysis (PCA) in data science"]),

    ("c_gaussian_elimination", "cat_math_algebra", "Gaussian Elimination",
     "A systematic algorithm for solving systems of linear equations by performing elementary row operations to reduce an augmented matrix to row echelon form.",
     "Developed in ancient China and formalized by Carl Friedrich Gauss, row operations include swapping rows, multiplying a row by a non-zero scalar, and adding a multiple of one row to another. Forward elimination achieves row echelon form in O(n^3) time, followed by back substitution.",
     [("Algorithmic Complexity", "O(n^3) arithmetic operations for n equations"), ("Pivot Element", "Non-zero entry used to eliminate below entries")],
     ["Electrical circuit mesh and nodal current analysis", "Chemical equation balancing"]),

    ("c_singular_value_decomposition", "cat_math_algebra", "Singular Value Decomposition (SVD)",
     "A fundamental matrix factorization technique that decomposes any real or complex m x n matrix into three constituent matrices: A = U * Sigma * V^T.",
     "Unlike eigendecomposition which requires square matrices, SVD applies to arbitrary rectangular matrices. U and V contain orthonormal singular vectors, and Sigma is a diagonal matrix of non-negative singular values ordered by magnitude. Provides optimal low-rank matrix approximations (Eckart-Young-Mirsky theorem).",
     [("Factorization Form", "A = U * Sigma * V^T"), ("Singular Values", "Square roots of eigenvalues of A^T * A")],
     ["Image compression and facial recognition", "Latent semantic analysis in NLP search"]),

    ("c_group_theory", "cat_math_algebra", "Group Theory and Algebraic Symmetry",
     "The mathematical study of algebraic structures known as groups, which formalize the abstract concept of symmetry through a single associative binary operation.",
     "A group (G, *) satisfies four fundamental axioms: Closure, Associativity, Identity element, and Inverse element. If commutativity holds, it is an Abelian group. Classifies symmetries in crystallography, particle physics, and Rubik's cube configurations.",
     [("Four Group Axioms", "Closure, Associativity, Identity, Invertibility"), ("Lagrange's Theorem", "Order of subgroup divides order of finite group")],
     ["Standard Model of particle physics gauge symmetries", "Crystallographic space groups"]),

    ("c_vector_spaces", "cat_math_algebra", "Vector Spaces and Linear Independence",
     "Mathematical sets of objects called vectors that can be added together and multiplied by scalars while satisfying eight distributive and associative axioms.",
     "A set of vectors is linearly independent if the only linear combination yielding the zero vector is the trivial combination where all scalar coefficients are zero. The dimension of a vector space is the cardinality of any minimal spanning basis.",
     [("Basis Property", "Linearly independent and spans the entire space"), ("Dimension Invariance", "All bases for a finite space have identical size")],
     ["Quantum mechanics Hilbert state spaces", "Signal processing function spaces"]),

    ("c_complex_numbers", "cat_math_algebra", "Complex Numbers and Euler's Formula",
     "Numbers of the form a + b*i, where a and b are real numbers and i is the imaginary unit satisfying i^2 = -1.",
     "Complex numbers form an algebraically closed field. Euler's formula connects trigonometry and complex exponentials: e^(i*theta) = cos(theta) + i*sin(theta). Euler's identity e^(i*pi) + 1 = 0 links five fundamental mathematical constants.",
     [("Imaginary Unit", "i = sqrt(-1), i^2 = -1"), ("Euler's Identity", "e^(i*pi) + 1 = 0")],
     ["Alternating current (AC) circuit impedance analysis", "Quantum mechanics Schrödinger wave equation"]),

    ("c_quadratic_formula", "cat_math_algebra", "The Quadratic Formula and Discriminant",
     "A closed-form formula providing the solutions to any quadratic equation ax^2 + bx + c = 0: x = (-b ± sqrt(b^2 - 4ac)) / (2a).",
     "Derived by completing the square on the general second-degree polynomial. The expression under the radical Delta = b^2 - 4ac is the discriminant: if Delta > 0, there are two distinct real roots; if Delta = 0, one real repeated root; if Delta < 0, two complex conjugate roots.",
     [("Discriminant Formula", "Delta = b^2 - 4 * a * c"), ("Root Count", "Positive: 2 real; Zero: 1 real; Negative: 2 complex")],
     ["Ballistics trajectory landing calculations", "Optimizing parabolic solar reflectors"]),

    ("c_determinants", "cat_math_algebra", "Determinants of Square Matrices",
     "A scalar value computed from the elements of a square matrix that encodes scaling factor of the transformation and whether the matrix is invertible.",
     "A matrix is invertible if and only if its determinant is non-zero (det(A) != 0). Geometrically, in 2D it represents the signed area of a transformed unit square; in 3D, the signed volume of a transformed unit cube.",
     [("Invertibility Criterion", "det(A) != 0 iff A is invertible"), ("Multiplicative Property", "det(A * B) = det(A) * det(B)")],
     ["Checking system invertibility", "Wronskian in differential equations"]),

    ("c_polynomial_rings", "cat_math_algebra", "Polynomial Rings and Factorization",
     "An algebraic ring formed from the set of polynomials in one or more indeterminates with coefficients in another ring or field.",
     "Polynomial division algorithms, irreducible polynomials, and ideals in polynomial rings form the cornerstone of algebraic geometry, coding theory (Reed-Solomon error correction codes), and cryptography.",
     [("Degree Rule", "deg(f * g) = deg(f) + deg(g) over an integral domain"), ("Irreducibility", "Analogue of prime numbers in polynomial rings")],
     ["Reed-Solomon error correction in CDs/QR codes", "Cyclic redundancy checks (CRC)"]),

    ("c_chinese_remainder_thm", "cat_math_algebra", "Chinese Remainder Theorem",
     "A theorem of number theory stating that if one knows the remainders of the Euclidean division of an integer by several pairwise coprime integers, then one can determine uniquely the remainder of the division of the integer by the product of these integers.",
     "Discovered by Sunzi in the 3rd century AD, it enables large multi-precision integer arithmetic to be split into parallel independent small modular computations.",
     [("Coprimality Condition", "Moduli m1, m2, ..., mk must be pairwise coprime"), ("Isomorphism", "Z/mZ isomorphic to product of Z/miZ")],
     ["Accelerating RSA decryption", "Fast multi-precision integer multiplication"]),

    ("c_fermats_little_thm", "cat_math_algebra", "Fermat's Little Theorem",
     "A foundational theorem of number theory stating that if p is a prime number, then for any integer a, the number a^p - a is an integer multiple of p: a^p ≡ a (mod p).",
     "If a is not divisible by p, this is equivalent to a^(p-1) ≡ 1 (mod p). It is the theoretical foundation for the Miller-Rabin primality test and RSA encryption exponentiation.",
     [("Congruence Formulation", "a^(p-1) ≡ 1 (mod p) for gcd(a, p) = 1"), ("Primality Testing", "Underpins Fermat and Miller-Rabin tests")],
     ["Primality testing for cryptographic keys", "Computing modular inverses"]),

    ("c_diophantine_equations", "cat_math_algebra", "Diophantine Equations",
     "Polynomial equations, usually in two or more unknowns, such that only the integer solutions are studied and sought.",
     "Named after Diophantus of Alexandria, famous examples include linear Diophantine equations ax + by = c (solvable iff gcd(a, b) divides c) and Fermat's Last Theorem x^n + y^n = z^n, proved by Andrew Wiles in 1994 to have no non-zero integer solutions for n > 2.",
     [("Bézout's Identity", "ax + by = gcd(a, b) always has integer solutions"), ("Fermat's Last Theorem", "No integer solutions for n > 2")],
     ["Resource allocation integer scheduling", "Lattice cryptography"]),

    ("c_rank_nullity_thm", "cat_math_algebra", "Rank-Nullity Theorem",
     "A fundamental theorem of linear algebra stating that for any linear map T: V -> W, the dimension of the domain V equals the rank of T plus the nullity of T: dim(V) = rank(T) + nullity(T).",
     "Rank is the dimension of the image/column space (dim(im(T))), and nullity is the dimension of the kernel/null space (dim(ker(T))). Proves that a square matrix is injective if and only if it is surjective.",
     [("Formula", "dim(V) = rank(T) + nullity(T)"), ("Fundamental Subspaces", "Four fundamental subspaces of a matrix")],
     ["Determining solution space dimensions of linear differential systems", "Control system controllability"]),

    ("c_quaternions", "cat_math_algebra", "Quaternions and 3D Rotations",
     "A non-commutative number system extending complex numbers to four dimensions (q = a + bi + cj + dk) where i^2 = j^2 = k^2 = ijk = -1.",
     "Invented by William Rowan Hamilton in 1843, unit quaternions provide an elegant, singularity-free representation of 3D orientations, completely avoiding the catastrophic gimbal lock problem encountered in Euler angles.",
     [("Hamilton Multiplication", "ij = k, jk = i, ki = j; ji = -k"), ("Unit Norm", "||q|| = sqrt(a^2 + b^2 + c^2 + d^2) = 1 for rotations")],
     ["Spacecraft and drone flight orientation", "3D video game character animation and robotics kinematics"]),

    ("c_dot_cross_products", "cat_math_algebra", "Dot Product and Cross Product of Vectors",
     "Two fundamental vector multiplication operations: the dot product yields a scalar measuring directional alignment (u . v = ||u|| ||v|| cos(theta)), while the cross product in 3D yields a perpendicular vector measuring rotational area (||u x v|| = ||u|| ||v|| sin(theta)).",
     "The dot product calculates physical work (W = F . d) and projections. The cross product calculates torque (tau = r x F), angular momentum, and magnetic forces on moving charges (F = q(v x B)).",
     [("Dot Product", "u . v = u1v1 + u2v2 + u3v3 = scalar"), ("Cross Product", "u x v = vector orthogonal to both u and v")],
     ["Calculating mechanical work and power", "Calculating rotational torque and angular momentum in mechanics"])
]

# We will write out functions to generate full items across all domains programmatically.
print(f"Loaded {len(SCI_MATH_DEFINITIONS)} core Math/Algebra definitions.")
