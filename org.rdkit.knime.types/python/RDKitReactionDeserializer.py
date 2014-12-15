from rdkit.Chem.rdChemReactions import ChemicalReaction

def deserialize(bytes):
	return ChemicalReaction(bytes)

