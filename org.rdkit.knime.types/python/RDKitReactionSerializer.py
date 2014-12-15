from rdkit.Chem.rdChemReactions import ChemicalReaction

def serialize(object_value):
	return object_value.ToBinary()
