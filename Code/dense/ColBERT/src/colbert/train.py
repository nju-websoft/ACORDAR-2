import os
import sys
import random
# import torch
import copy

curPath = os.path.abspath(os.path.dirname(__file__))
rootPath = os.path.split(curPath)[0]
sys.path.append(rootPath)

import colbert.utils.distributed as distributed

from colbert.utils.parser import Arguments
from colbert.utils.runs import Run
from colbert.training.training import train1


def main():
    parser = Arguments(description='Training ColBERT with <query, positive passage, negative passage> triples.')

    parser.add_model_parameters()
    parser.add_model_training_parameters()
    parser.add_training_input()

    args = parser.parse()

    assert args.bsize % args.accumsteps == 0, ((args.bsize, args.accumsteps),
                                               "The batch size must be divisible by the number of gradient accumulation steps.")
    assert args.query_maxlen <= 512
    assert args.doc_maxlen <= 512

    args.lazy = args.collection is not None

    with Run.context(consider_failed_if_interrupted=False):
        train1(args)


if __name__ == "__main__":

    main()

