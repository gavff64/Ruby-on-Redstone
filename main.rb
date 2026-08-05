require_relative "player_bot"
require_relative "chat_parser" # Consider moving this to player_bot

player = nil
# (player_name, player_skin_name)
loop do
  break if player
  player = Bot.new("Ruby", "Vkkz") if /spawn/.match?(player_messages(1).join) && player.nil?
  sleep 0.5
end

Thread.new do
  loop do
    (player.say("Teleporting!"); player.teleport) if /teleport|tp/.match?(player_messages(1).join)
  end
end

Thread.new do
  loop do
    (player.say("Hello! :D"); player.look_at; 6.times {player.sneak; sleep 0.1}) if /hi|hey|hello/.match?(player_messages(1).join) && (player.stopped || player.idle)
  end
end

Thread.new do
  loop do
    (player.say("Stopping!"); player.stop) if /stop/.match?(player_messages(1).join) && !player.stopped
    (player.say("I'm already stopped!")) if /stop/.match?(player_messages(1).join) && player.stopped
  end
end

Thread.new do
  loop do
    (player.say("Following!"); player.follow) if /follow/.match?(player_messages(1).join) && player.stopped
  end
end

Thread.new do
  loop do
    (player.say("I'm already following!")) if /follow/.match?(player_messages(1).join) && !player.stopped
  end
end.join

# player.follow; player.stop; player.say(message)
