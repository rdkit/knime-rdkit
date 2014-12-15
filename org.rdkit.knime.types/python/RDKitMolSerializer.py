from rdkit import Chem

def serialize(object_value):
	return object_value.ToBinary()
