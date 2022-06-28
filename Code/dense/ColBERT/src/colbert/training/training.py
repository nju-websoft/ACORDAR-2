import os
import random
import time
import torch
import torch.nn as nn
import numpy as np

from transformers import AdamW
from colbert.utils.runs import Run
from colbert.utils.amp import MixedPrecisionManager

from colbert.training.lazy_batcher import LazyBatcher
from colbert.training.eager_batcher import EagerBatcher
from colbert.training.valid_reader import valid_reader
from colbert.parameters import DEVICE

from colbert.modeling.colbert import ColBERT
from colbert.utils.utils import print_message
from colbert.training.utils import print_progress, manage_checkpoints


def train1(args):
    random.seed(12345)
    np.random.seed(12345)
    torch.manual_seed(12345)
    if args.distributed:
        torch.cuda.manual_seed_all(12345)

    if args.distributed:
        assert args.bsize % args.nranks == 0, (args.bsize, args.nranks)
        assert args.accumsteps == 1
        args.bsize = args.bsize // args.nranks

        print("Using args.bsize =", args.bsize, "(per process) and args.accumsteps =", args.accumsteps)

    if args.lazy:
        reader = LazyBatcher(args, (0 if args.rank == -1 else args.rank), args.nranks)
        valider = LazyBatcher(args, (0 if args.rank == -1 else args.rank), args.nranks, isTrain=False)
    else:
        reader = EagerBatcher(args, (0 if args.rank == -1 else args.rank), args.nranks)
        valider = LazyBatcher(args, (0 if args.rank == -1 else args.rank), args.nranks, isTrain=False)

    if args.rank not in [-1, 0]:
        torch.distributed.barrier()

    colbert = ColBERT.from_pretrained('bert-base-uncased',
                                      query_maxlen=args.query_maxlen,
                                      doc_maxlen=args.doc_maxlen,
                                      dim=args.dim,
                                      similarity_metric=args.similarity,
                                      mask_punctuation=args.mask_punctuation)

    if args.checkpoint is not None:
        assert args.resume_optimizer is False, "TODO: This would mean reload optimizer too."
        print_message(f"#> Starting from checkpoint {args.checkpoint} -- but NOT the optimizer!")

        checkpoint = torch.load(args.checkpoint, map_location='cpu')

        try:
            colbert.load_state_dict(checkpoint['model_state_dict'])
        except:
            print_message("[WARNING] Loading checkpoint with strict=False")
            colbert.load_state_dict(checkpoint['model_state_dict'], strict=False)

    if args.rank == 0:
        torch.distributed.barrier()

    colbert = colbert.to(DEVICE)
    # print("#> Start training...")
    colbert.train()

    if args.distributed:
        colbert = torch.nn.parallel.DistributedDataParallel(colbert, device_ids=[args.rank],
                                                            output_device=args.rank,
                                                            find_unused_parameters=True)

    optimizer = AdamW(filter(lambda p: p.requires_grad, colbert.parameters()), lr=args.lr, eps=1e-8)
    optimizer.zero_grad()
    # manage_checkpoints(args,0,colbert,optimizer,0)


    amp = MixedPrecisionManager(args.amp)
    criterion = nn.CrossEntropyLoss()
    labels = torch.zeros(args.bsize, dtype=torch.long, device=DEVICE)

    start_time = time.time()
    # train_loss = 0.0

    start_batch_idx = 0

    if args.resume:
        assert args.checkpoint is not None
        start_batch_idx = checkpoint['batch']

        reader.skip_to_batch(start_batch_idx, checkpoint['arguments']['bsize'])

    best_loss = float('inf')

    # cnt=0
    # last_loss= float('inf')
    for epoch in range(args.epoch):
        # print("epoch {}".format(epoch))
        # print("training……")

        colbert.train()
        reader.position = 0
        train_loss = 0.0

        for batch_idx, BatchSteps in zip(range(start_batch_idx, args.maxsteps), reader):
            this_batch_loss = 0.0

            for queries, passages in BatchSteps:

                with amp.context():
                    scores = colbert(queries, passages).view(2, -1).permute(1, 0)
                    loss = criterion(scores, labels[:scores.size(0)])
                    loss = loss / args.accumsteps



                # if args.rank < 1:
                #     print_progress(scores)

                amp.backward(loss)

                train_loss += loss.item()
                this_batch_loss += loss.item()

            amp.step(colbert, optimizer)

            if args.rank < 1:
                avg_loss = train_loss / (batch_idx + 1)

                # 已经train了多少示例
                num_examples_seen = (batch_idx - start_batch_idx) * args.bsize * args.nranks
                elapsed = float(time.time() - start_time)

                log_to_mlflow = (batch_idx % 20 == 0)
                Run.log_metric('train/avg_loss', avg_loss, step=batch_idx, log_to_mlflow=log_to_mlflow)
                Run.log_metric('train/batch_loss', this_batch_loss, step=batch_idx, log_to_mlflow=log_to_mlflow)
                Run.log_metric('train/examples', num_examples_seen, step=batch_idx, log_to_mlflow=log_to_mlflow)
                Run.log_metric('train/throughput', num_examples_seen / elapsed, step=batch_idx,
                               log_to_mlflow=log_to_mlflow)

                with open(args.root + args.experiment +"/train.py/"+args.run+ "/tr_loss.txt", 'a') as f:
                    f.write("epoch: "+str(epoch)+"\t"+"batch_idx: " + str(batch_idx) +"\ttrain_loss: "+str(avg_loss)+'\n')



        # print("eval……")
        colbert.eval()
        eval_loss = 0
        valider.position = 0
        for evalBatch in valider:

            for queries, passages in evalBatch:
                # print(queries)
                with amp.context():
                    scores = colbert(queries, passages).view(2, -1).permute(1, 0)
                    loss = criterion(scores, labels[:scores.size(0)])
                    eval_loss += loss.item()
                    # print(eval_loss)

        with open(args.root+args.experiment+"/train.py/"+args.run+"/eval_loss.txt",'a') as f:
            f.write(str(epoch)+'\t'+str(eval_loss)+'\n')
        if best_loss > eval_loss:
            manage_checkpoints(args, epoch, colbert, optimizer, batch_idx=None)
            best_loss = eval_loss

        # if eval_loss>=last_loss:
        #     cnt=cnt+1
        #     print_message("cnt: "+str(cnt))
        #     if cnt>=3: return
        # else:
        #     cnt=0
        last_loss = eval_loss

