import os
import torch

from colbert.utils.runs import Run
from colbert.utils.utils import print_message, save_checkpoint
from colbert.parameters import SAVED_CHECKPOINTS


def print_progress(scores):
    positive_avg, negative_avg = round(scores[:, 0].mean().item(), 2), round(scores[:, 1].mean().item(), 2)
    print("#>>>   ", positive_avg, negative_avg, '\t\t|\t\t', positive_avg - negative_avg)

# def manage_log(args,  epoch, eval_loss):
#     path = os.path.join(args.root, 'loss.txt')
#     with open(path,'w+') as f:
#         f.write(str(args.lr)+'\t'+str(args.bsize)+'\t'+str(epoch)+'\t'+args.similarity+'\t'+str(eval_loss)+'\n')

def manage_checkpoints(args, epoch, colbert, optimizer, batch_idx):
    arguments = args.input_arguments.__dict__

    path = os.path.join(Run.path, 'checkpoints')

    if not os.path.exists(path):
        os.mkdir(path)

    if batch_idx is None:
        name = os.path.join(path, "colbert-{}.dnn".format(epoch))
        save_checkpoint(name, epoch, batch_idx, colbert, optimizer, arguments)
        return
    if batch_idx % 2000 == 0:
        name = os.path.join(path, "colbert.dnn")
        save_checkpoint(name, epoch, batch_idx, colbert, optimizer, arguments)

    if batch_idx in SAVED_CHECKPOINTS:
        name = os.path.join(path, "colbert-{}-{}.dnn".format(epoch,batch_idx))
        save_checkpoint(name, epoch, batch_idx, colbert, optimizer, arguments)