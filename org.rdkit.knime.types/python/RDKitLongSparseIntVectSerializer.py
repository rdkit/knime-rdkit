def serialize(object_value):
    s = str(
        {"length": object_value.GetLength(), "bits": object_value.GetNonzeroElements()}
    )

    import sys

    if sys.version_info.major > 2:
        s = s.encode()

    return s
