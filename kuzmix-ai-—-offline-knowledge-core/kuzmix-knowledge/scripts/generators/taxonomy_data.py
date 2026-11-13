# Comprehensive Taxonomy Data for 61 Categories (2,500+ Concepts)

TAXONOMY_MAP = {
    # 1. cat_math_algebra
    "cat_math_algebra": [
        ("prime_numbers", "Prime Numbers", "Natural numbers > 1 with no positive divisors other than 1 and itself.", "Multiplicative building blocks of integers."),
        ("euclidean_algorithm", "Euclidean Algorithm", "Algorithm for computing greatest common divisor via remainders.", "Iterative division terminates in logarithmic time."),
        ("matrices_transformations", "Matrices and Linear Transformations", "Rectangular numerical arrays representing linear maps between vector spaces.", "Matrix multiplication corresponds to function composition."),
        ("eigenvalues_eigenvectors", "Eigenvalues and Eigenvectors", "Scalars and vectors where linear transformation acts purely by scaling: Av = lambda*v.", "Identifies invariant directions and vibrational normal modes."),
        ("gaussian_elimination", "Gaussian Elimination", "Algorithm for solving systems of linear equations using row operations.", "Reduces augmented matrices systematically in O(n^3) time."),
        ("singular_value_decomposition", "Singular Value Decomposition (SVD)", "Matrix factorization decomposing any m x n matrix: A = U*Sigma*V^T.", "Applies to rectangular matrices, providing optimal low-rank approximations."),
        ("group_theory", "Group Theory and Symmetry", "Algebraic study of groups formalizing symmetry through an invertible operation.", "Satisfies closure, associativity, identity, and invertibility axioms."),
        ("vector_spaces", "Vector Spaces and Basis", "Sets of vectors closed under addition and scalar multiplication.", "Linearly independent spanning sets form a basis whose cardinality defines dimension."),
        ("complex_numbers", "Complex Numbers and Euler's Formula", "Numbers of form a + bi where i^2 = -1, represented in the complex plane.", "Euler's formula e^(ix) = cos(x) + i*sin(x) connects exponentials to trigonometry."),
        ("quadratic_formula", "Quadratic Formula and Discriminant", "Closed-form formula solving ax^2 + bx + c = 0: x = (-b ± sqrt(b^2 - 4ac)) / 2a.", "The discriminant Delta = b^2 - 4ac dictates whether roots are real or complex."),
        ("determinants", "Matrix Determinants", "Scalar value of a square matrix indicating geometric volume scaling and invertibility.", "A matrix is invertible if and only if its determinant is non-zero."),
        ("chinese_remainder_theorem", "Chinese Remainder Theorem", "Number theory theorem solving simultaneous modular congruences with coprime moduli.", "Allows large integer calculations to be split into parallel smaller modular rings."),
        ("quaternions", "Quaternions for 3D Rotations", "Four-dimensional number system q = a + bi + cj + dk avoiding gimbal lock.", "Multiplication is non-commutative; unit quaternions smoothly interpolate 3D rotations."),
        ("fermats_little_theorem", "Fermat's Little Theorem", "Number theory theorem stating a^(p-1) ≡ 1 (mod p) for prime p and coprime a.", "Foundational to modular exponentiation and primality verification."),
        ("diophantine_equations", "Diophantine Equations", "Polynomial equations seeking integer-only solutions.", "Bézout's identity ax + by = gcd(a, b) guarantees integer solutions for linear cases."),
        ("rank_nullity_theorem", "Rank-Nullity Theorem", "Linear algebra theorem: dim(V) = rank(T) + nullity(T).", "Relates the dimension of a linear transformation's domain to its image and kernel."),
        ("dot_cross_products", "Dot and Cross Products", "Vector multiplication: dot product yields scalar alignment, cross product yields orthogonal vector.", "Dot product measures work; cross product measures torque."),
        ("modular_inverses", "Modular Multiplicative Inverse", "An integer x such that a*x ≡ 1 (mod m), existing iff gcd(a, m) = 1.", "Computed using the Extended Euclidean Algorithm to solve modular division."),
        ("gram_schmidt_process", "Gram-Schmidt Orthonormalization", "Algorithm that converts a set of linearly independent vectors into an orthonormal basis.", "Iteratively subtracts vector projections to guarantee mutual orthogonality."),
        ("trace_of_matrix", "Trace of a Square Matrix", "The sum of elements along the main diagonal of a square matrix.", "Invariant under cyclic permutations and equal to the sum of eigenvalues."),
        ("polynomial_roots", "Fundamental Theorem of Algebra", "Every non-zero single-variable polynomial of degree n has exactly n complex roots.", "Proves that the field of complex numbers is algebraically closed."),
        ("cauchy_schwarz_inequality", "Cauchy-Schwarz Inequality", "Fundamental vector inequality: |u . v|^2 <= (u . u) * (v . v).", "Inner product squared cannot exceed the product of individual norms."),
        ("linear_independence", "Linear Independence of Vectors", "Property where no vector in a set can be written as a linear combination of others.", "Essential condition for a vector collection to form a coordinate basis."),
        ("ring_and_field_theory", "Rings and Fields in Abstract Algebra", "Algebraic structures with two binary operations (addition and multiplication).", "A field requires non-zero elements to have multiplicative inverses."),
        ("cayley_hamilton_theorem", "Cayley-Hamilton Theorem", "Theorem stating that every square matrix satisfies its own characteristic polynomial.", "Allows high matrix powers and matrix exponentials to be calculated from lower degrees."),
        ("spectral_theorem", "The Spectral Theorem", "Real symmetric matrices can be diagonalized by an orthogonal matrix.", "Guarantees an orthonormal basis of eigenvectors with purely real eigenvalues."),
        ("fibonacci_sequence", "Fibonacci Sequence and Golden Ratio", "Integer sequence where each number is the sum of two preceding ones: F_n = F_{n-1} + F_{n-2}.", "Ratio of successive terms converges to golden ratio phi ≈ 1.618."),
        ("pascal_triangle", "Pascal's Triangle and Binomial Coefficients", "Triangular array of binomial coefficients encoding expansions of (x + y)^n.", "Follows Pascal's identity (n choose k) = (n-1 choose k-1) + (n-1 choose k)."),
        ("modular_exponentiation", "Modular Exponentiation", "Exponentiation over a modulus: c = b^e mod m computed in O(log e) time.", "Computed efficiently via repeated squaring to avoid intermediate overflow."),
        ("sieve_of_eratosthenes", "Sieve of Eratosthenes", "Optimal algorithm for finding all prime numbers up to limit n in O(n log log n) time.", "Iteratively marks multiples of each prime as composite."),
        ("gcd_and_lcm", "Greatest Common Divisor and Least Common Multiple", "Arithmetic concepts connected by gcd(a, b) * lcm(a, b) = |a * b|.", "Essential for simplifying fractions and synchronizing cyclical periods."),
        ("lucas_numbers", "Lucas Numbers", "Integer sequence satisfying L_n = L_{n-1} + L_{n-2} starting with L_0 = 2, L_1 = 1.", "Closely related to Fibonacci numbers through identities like L_n = F_{n-1} + F_{n+1}."),
        ("dihedral_groups", "Dihedral Groups of Regular Polygons", "Symmetry group of a regular n-gon including rotations and reflections (order 2n).", "Non-Abelian for n >= 3, describing rotational and reflectional symmetries."),
        ("galois_fields", "Galois Fields (Finite Fields)", "Algebraic fields with finite elements GF(p^n) where p is prime.", "Arithmetic follows modular operations; foundation of AES cryptography and error codes."),
        ("ideal_theory", "Ideals in Abstract Algebra", "Subsets of a ring absorbing multiplication by arbitrary ring elements.", "Enables construction of quotient rings, foundational to algebraic geometry."),
        ("abelian_groups", "Abelian Groups", "Groups where group operation is commutative (a * b = b * a for all elements).", "Fundamental building blocks classified by the structure theorem for finitely generated groups."),
        ("inner_product_spaces", "Inner Product Spaces", "Vector spaces equipped with an inner product defining angles, lengths, and orthogonality.", "Generalizes Euclidean space to infinite-dimensional Hilbert spaces."),
        ("nilpotent_matrices", "Nilpotent Matrices", "Square matrices where N^k = 0 for some positive integer k.", "All eigenvalues of a nilpotent matrix are identically zero."),
        ("kronecker_product", "Kronecker Product of Matrices", "Operation on two matrices of arbitrary size resulting in a block matrix.", "Preserves matrix properties; essential in quantum computing multi-qubit systems."),
        ("homomorphisms_isomorphisms", "Homomorphisms and Isomorphisms", "Structure-preserving maps between algebraic structures.", "An isomorphism is a bijective homomorphism establishing identical algebraic structure.")
    ]
}

print(f"Loaded taxonomy sample with {len(TAXONOMY_MAP['cat_math_algebra'])} concepts in Algebra.")
