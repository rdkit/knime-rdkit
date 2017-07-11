from rdkit import Chem

def deserialize(inbytes):
	return Chem.Mol(inbytes)

