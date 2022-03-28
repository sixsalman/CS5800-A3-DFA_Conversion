Assignment Prompt:
Part I. Implement Algorithm 5.6.3 from Sudkamp to convert an NFA-lambda M = (Q,Sigma,delta,q0,F) into a DFA M' = DM.
Part II. Implement the DFA minimization algorithm from the HMU text, to convert DFA M' to a DFA M'' with minimal number of states. Read HMU handout: Ch. 4 Sections 4.4.1-3.
Part III. Write a DFA simulator to generate the computation of the DFA on a given string.
The program should work for a general (user-specified) NFA-lambda as input (not just for specific examples), which should be read from a file.
Parts I, II and III have to be coded into one program (not three separate programs).
The input (string) for Part III may be given interactively.

Include the "t-table" in the output from part I, as well as the transition table and full specification of the obtained DFA (M').
Include the triangular table (of distinguishabilities) in your output from part II.

Input Specifications (NFA-λ):
• Entries must be separated by tabs
• First line should contain a tab followed by elements of sigma separated by tabs followed by ‘L’ as heading for lambda column followed by heading for column identifying accepting states
• Following lines should start with name of state followed by the set of states that first element of sigma in heading (first line) leads to ({_,_,_...}), followed by the set of states that the next element of sigma leads to ... followed by the set of states that lambda leads to, followed by 1 if this state is an accepting state, 0 otherwise. State in the first of these lines should be the starting state.