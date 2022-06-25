import torch
import numpy as np

a = torch.tensor([[[1,2,3],[4,5,6],[1,2,2],[1,2,1]]])
b = a.view(2,-1)
print(b)