* Licenses for dependencies are posted in the lib/ directory *

This is a Java implementation of the original C&C normal-form model for CCG parsing. It is intended for use with the original CCGbank format, and relies on AUTO files for input.

SUPERTAGGING:

The parser does perform better (and is much faster) when using a supertagger to propose a set of candidate lexical categories for each word. This codebase also contains a ntive Java implementation of the feed-forward neural network supertagger from EasyCCG (http://homepages.inf.ed.ac.uk/s1049478/easyccg.html). While it may take a while to train, this supertagger is much faster than the trigram CRF-based supertagger included in the original C&C package, and nearly as accurate.

The supertagging code is spread across a couple of packages here, including 'supertagger', 'multitagger', and with dependencies on 'neuralnet'. For running experiments on (WSJ) CCGbank, the entry point for training a cross-validated supertagger suitable for generating multitags for training the parser is: multitagger.runnables.TrainAndUseCrossValidatedSupertagger.java. This file assumes that CCGbank is available at 'data/CCGbank', that there are Turian embeddings at 'embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt'*, and takes two arguments: an output directory, and a 'categories' file listing the set of possible lexical categories -- this file is included in this repository.

*Sample Turian embeddings are available at: http://metaoptimize.s3.amazonaws.com/cw-embeddings-ACL2010/embeddings-scaled.EMBEDDING_SIZE=50.txt.gz

PARSING:

The main entry point for training and evaluating the C&C normal-form parser is: perceptron.RunPerceptronParser.java. This class breaks the training pipeline insto several steps: splitting the training data into several sections (called 'folds', though no cross-validation is done here); processing each section by parsing each sentence with a basic CCG grammar and extracting the features that occur frequently enough in the training data to be included in the trained parsing model; and finally, learning the weights on the features by doing "coarse to fine" parsing on the cleaned forests using the full model, caching these forests, and iterating over them using the perceptron update on the Viterbi parse to adjust model weights.

Once a parser is trained, perceptron.EvaluatePerceptronParser.java is used to parse a set of sentences and print out the predicated parses and their dependencies. Then, eval.RunEvaluation.java can be used to compare the predicted AUTO file with the gold AUTO file and generate statistics.

NOTES ON THE CODE:

This codebase grew out of Yonatan Bisks's grammar induction code, and was originally designed for generative models that would reproduce the results of http://www.aclweb.org/anthology/P02-1043.pdf (see also Julia's thesis: http://juliahmr.cs.illinois.edu/Dissertation/).

Most of the backbone code -- along with the implementation of the generative models -- is in the illinoisParser package.

The util and training packages contain mostly reference/debugging code.
