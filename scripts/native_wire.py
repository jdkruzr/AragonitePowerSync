"""Small bounded protobuf wire codec for synthetic lab fixtures.

Preserves unknown scalar fields and their wire types. This is not a vendor
schema implementation or a production note conversion library.
"""
def read_varint(data, pos):
    value = 0
    for shift in range(0, 64, 7):
        if pos >= len(data):
            raise ValueError('Truncated varint')
        byte = data[pos]
        pos += 1
        if shift == 63 and byte & 254:
            raise ValueError('Overflowing varint')
        value |= (byte & 127) << shift
        if byte < 128:
            return value, pos
    raise ValueError('Oversized varint')

def fields(data):
    if len(data) > 1048576:
        raise ValueError('Message exceeds lab limit')
    pos, result = 0, []
    while pos < len(data):
        key, pos = read_varint(data, pos)
        number, wire = key >> 3, key & 7
        if not 0 < number <= 536870911:
            raise ValueError('Invalid field number')
        if wire == 0:
            value, pos = read_varint(data, pos)
        else:
            if wire == 2:
                size, pos = read_varint(data, pos)
            elif wire in (1, 5):
                size = {1: 8, 5: 4}[wire]
            else:
                raise ValueError('Unsupported wire type')
            if size > len(data) - pos:
                raise ValueError('Truncated field')
            value = data[pos:pos+size]
            pos += size
        result.append((number, wire, value))
    return result

def varint(value):
    if not 0 <= value < 1 << 64:
        raise ValueError('uint64 range')
    out = bytearray()
    while value >= 128:
        out.append((value & 127) | 128)
        value >>= 7
    return bytes(out) + bytes([value])

def encode(items):
    out = bytearray()
    for number, wire, value in items:
        if not 0 < number <= 536870911 or wire not in (0, 1, 2, 5):
            raise ValueError('Invalid field')
        out.extend(varint(number << 3 | wire))
        if wire == 0:
            out.extend(varint(value))
        else:
            if wire == 2:
                out.extend(varint(len(value)))
            elif len(value) != {1: 8, 5: 4}[wire]:
                raise ValueError('Fixed field size')
            out.extend(value)
    return bytes(out)

def replace(items, updates):
    """Replace scalar fields while retaining unknown fields unchanged."""
    numbers = [number for number, _, _ in items]
    if len(set(numbers)) != len(numbers):
        raise ValueError('Duplicate scalar field')
    result = [(number, *updates[number]) if number in updates else (number, wire, value)
              for number, wire, value in items]
    result.extend((number, wire, value) for number, (wire, value) in updates.items() if number not in numbers)
    return result
