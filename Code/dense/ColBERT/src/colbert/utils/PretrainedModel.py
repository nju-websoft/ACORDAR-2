from transformers import BertPreTrainedModel
import torch

model = BertPreTrainedModel.from_pretrained("/home/ttling/ColBERT/downloads/colbertv2.0")
path = '/home/ttling/ColBERT/downloads/colbertv2.0.dnn'
checkpoint = {}
checkpoint['epoch'] = 1
checkpoint['model_state_dict'] = model.state_dict()
# checkpoint['optimizer_state_dict'] = optimizer.state_dict()
# checkpoint['arguments'] = arguments

torch.save(checkpoint, path)