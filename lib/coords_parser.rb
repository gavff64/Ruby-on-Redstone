def coords_parser(str)
  coords = {}

  coords[:proper_array] = str.scan(/[-]?\d+(?:\.\d+)?(?=[d])/).map do |element|
    element.to_f
  end

  coords[:proper_array_eye_level] = coords[:proper_array].dup.tap do |element|
    element[1] += 1
  end

  coords[:minecraft_string] = coords[:proper_array].map do |element|
    element
  end.join(" ")

  coords[:minecraft_string_eye_level] = coords[:proper_array_eye_level].map do |element|
    element
  end.join(" ")

  return coords
end
