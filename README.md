# Tiger Compiler for NIOS II Processor
A compiler for the Tiger programming language targeting the NIOS II Processor architecture, developed in Java, closely following the principles outlined in *Modern Compiler Implementation in Java* by Andrew W. Appel.

### Introduction
This project aims to implement a compiler for the Tiger programming language, following the principles outlined in *Modern Compiler Implementation in Java* by Andrew W. Appel. The compiler consists of front-end components including lexical analysis, syntax analysis, type checking, intermediate representation (IR) generation, and code generation.

### Tiger Programming Language
Tiger is an imperative and statically typed programming language designed for educational purposes. It supports integer and string variables, arrays, records, strong static types, limited recursion, and nested functions. Its syntax is similar to some functional languages.

### Project Structure Progression
The development of this compiler project mirrors the progression outlined in *Modern Compiler Implementation in Java*. As the project evolves through commits, each phase of the compiler corresponds to a core stage in the book's methodology:

1. **Lexical Analysis:**
   Building a lexical analyzer using the JFex tool.

2. **Parsing:**
   Constructing a parse tree using the CUP2 parser generator.

3. **Type Checking:**
   Adding type checking according to the Tiger Language Specification.

4. **Stack Allocation and Parameter Passing:**
   Handling stack allocation, escape analysis, and parameter passing.

5. **Intermediate Representation Generation:**
   Implementing translation methods for generating the intermediate representation.

6. **Code Generation:**
   Performing instruction selection to translate the IR to NIOS assembly language using the Maximal Munch technique.

### Technologies
- Java: A programming language for compiler development
- [JFex] (https://jflex.de/): Tool for building lexical analyzers
- [CUP] (http://www2.cs.tum.edu/projects/cup/index.php): Parser generator for constructing parse trees 
- NIOS II Processor: Target architecture for code generation. For further details, refer to the [Intel Nios II Reference Manual](https://cdrdv2-public.intel.com/666887/n2cpu-nii5v1-683836-666887.pdf)
- Modern Compiler Implementation in Java (Textbook): Reference material for compiler design and implementation

### Usage
The compiler project can be cloned and built using standard Java development tools. Each lab assignment corresponds to a specific phase of the compiler development process, and instructions for completing each lab can be found in the respective lab documents.

### TODO
The project progress aligns with the stages of compiler construction, with each phase representing a milestone in the development of the Tiger compiler.

- [x] Phase 1: Lexical Analysis
- [x] Phase 2: Parsing (Constructing Parse Trees)
- [x] Phase 3: Type Checking
- [x] Phase 4: Stack Allocation and Parameter Passing
- [ ] Phase 5: Intermediate Representation Generation
- [ ] Phase 6: Code Generation (NIOS II Assembly)


### Deviations
In the process of implementing the Tiger compiler, certain features or aspects from the second edition of *Modern Compiler Implementation in Java* may be incorporated. These deviations are aimed at leveraging updated techniques, algorithms, or methodologies introduced to enhance the functionality, efficiency, or correctness of the compiler.

You are welcome to follow along using the second edition, but please note that the Tiger language specifications are exclusively found in the first edition. For alternative reference, you can consult the [Columbia University Tiger Language Specification](https://www.cs.columbia.edu/~sedwards/classes/2002/w4115/tiger.pdf).

### Acknowledgment
I would like to acknowledge the guidance and instruction provided by Dr. Brian Larkins during COMP 362 at Rhodes College. The experience gained from the course has been invaluable in shaping my understanding and approach to compiler construction.
