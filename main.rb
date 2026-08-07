require_relative "player_bot"
require 'ripper'

# (player_name, player_skin_name)
player = Bot.new("Ruby", "Vkkz")

DISALLOWED = %w[
  File Dir IO Socket TCPSocket UDPSocket UNIXSocket Net Process
  ObjectSpace Marshal Kernel GC Fiber
  eval instance_eval class_eval module_eval binding
  send __send__ public_send method instance_variable_get instance_variable_set
  const_get const_set define_method remove_method undef_method
  system exec spawn fork open require require_relative load autoload
  at_exit caller trap alias_method
].freeze

def disallowed?(code)
  Ripper.lex(code).any? do |position, type, text, state|
    (type == :on_ident || type == :on_const) && DISALLOWED.include?(text)
  end

rescue StandardError
  true
end

buffer = ""
puts "Ruby on Redstone REPL."
puts "Type 'exit' to quit."

loop do
  print buffer.empty? ? "> " : "* "
  line = gets
  break if line.nil?
  break if buffer.empty? && line.chomp == "exit"
  buffer += line

  tree = Ripper.sexp(buffer)
  next if tree.nil?

  if disallowed?(buffer)
    puts "Blocked: '(text)' not allowed. Please stick to things like loops, basic data types, etc." # Fix this, get the text to display.
  else
    begin
      p eval(buffer, binding)
    rescue StandardError => e
      puts "Error: #{e.class}: #{e.message}"
    end
  end
  buffer = ""
end


# player.follow; player.stop; player.say(message)
