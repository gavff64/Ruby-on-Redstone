require_relative "lib/player_bot"
require "ripper"

# (player_name, player_skin_name)
player = Bot.new("Ruby", "Vkkz")

DISALLOWED = %w[
  File Dir IO Socket TCPSocket UDPSocket UNIXSocket Net Process
  ObjectSpace Marshal Kernel GC Fiber Ractor
  eval instance_eval class_eval module_eval binding
  send __send__ public_send method instance_variable_get instance_variable_set
  const_get const_set define_method remove_method undef_method
  system exec spawn fork open require require_relative load autoload
  at_exit caller trap alias_method
].freeze

def disallowed_token(code)
  match = Ripper.lex(code).find do |position, type, text, state|
    (type == :on_ident || type == :on_const) && DISALLOWED.include?(text)
  end
  match && match[2]

rescue StandardError
  :parse_error
end

repl_binding = binding
buffer = ""
multiline = false

puts "Ruby on Redstone REPL."
puts "Type 'exit' to quit."

loop do
  print buffer.empty? ? "> " : "* "
  line = gets
  break if line.nil?
  break if buffer.empty? && line.chomp == "exit"

  buffer += line
  multiline = true if buffer.count("\n") > 1 unless multiline

  tree = Ripper.sexp(buffer)
  next if tree.nil?
  next if multiline && line.chomp != ""

  dangerous = disallowed_token(buffer)

  if dangerous
    msg = dangerous == :parse_error ? "could not be parsed" : "'#{dangerous}' not allowed"
    puts "Blocked: #{msg}. Please stick to things like loops, basic data types, etc."
  else
    begin
      p eval(buffer, repl_binding)
    rescue StandardError => e
      puts "Error: #{e.class}: #{e.message}"
    end
  end

  buffer = ""
  multiline = false
end


# player.follow; player.stop; player.say(message)
