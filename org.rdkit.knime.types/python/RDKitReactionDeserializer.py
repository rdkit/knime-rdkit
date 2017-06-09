from rdkit.Chem.rdChemReactions import ChemicalReaction

def deserialize(inbytes):
	return ChemicalReaction(inbytes)

