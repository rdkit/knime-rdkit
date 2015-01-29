def serialize(object_value):
	return str({"length":object_value.GetLength(),"bits":object_value.GetNonzeroElements()})
