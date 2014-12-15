from rdkit import Chem

def deserialize(bytes):
	return Chem.Mol(bytes)

