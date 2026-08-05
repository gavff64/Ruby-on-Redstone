# Maybe consider putting the following logic in a separate file as that'll be complicated, not sure.
# Create a gemfile to bundle things like rcon

require "rcon"
require_relative "coords_parser"
require_relative "chat_parser"

class Bot
  attr_reader :stopped, :idle
  attr_writer :name, :skin_name, :message

  def initialize(name = "Steve", skin_name = "WoofAI")
    client = Rcon::Client.new(host: "0.0.0.0", port: 25575, password: "1234")
    @client = client
    @name = name
    @host_ign = last_player_name.freeze
    @stopped = true
    @idle = true
    client.authenticate!(ignore_first_packet: false)
    client.execute("fp spawn #{name}", wait: "0.25")
    client.execute("fp skin #{skin_name}", wait: "0.25")
    client.execute("op #{name}", wait: "0.25")
  end

  def stop
    @stopped = true
  end

  def say(message)
    @client.execute("fp cmd #{@name} say #{message}", wait: "0.25")
  end

  def teleport
    @client.execute("fp cmd #{@name} tp #{@name} #{@host_ign}", wait: "0.25")
  end

  def sneak
    @client.execute("fp sneak", wait: "0.25")
  end

  def look_at
    parsed_target_coords = coords_parser(@client.execute("data get entity #{@host_ign} Pos", wait: "0.25").body)
    @client.execute("fp look at #{parsed_target_coords[:minecraft_string_eye_level]}", wait: "0.25")
  end

  def follow
    @stopped = false
    @idle = false
    sprinting = false
    toggle = false
    loop do
      break if @stopped
      parsed_target_coords = coords_parser(@client.execute("data get entity #{@host_ign} Pos", wait: "0.25").body)
      parsed_bot_coords = coords_parser(@client.execute("data get entity #{@name} Pos", wait: "0.25").body)
      distance_from_target = (parsed_bot_coords[:proper_array].sum - parsed_target_coords[:proper_array].sum).abs
      target_higher_than_bot = parsed_bot_coords[:proper_array][1] < parsed_target_coords[:proper_array][1]
      @idle = true if distance_from_target < 2
      @idle = false if distance_from_target >= 2

      if distance_from_target >= 10 && toggle == false
        @client.execute("fp sprint", wait: "0.25")
        toggle = true
      elsif distance_from_target <= 9 && (toggle == true || @stopped == true)
        @client.execute("fp sprint", wait: "0.25")
        toggle = false
      end

      @client.execute("fp look at #{parsed_target_coords[:minecraft_string_eye_level]}", wait: "0.25")
      @client.execute("fp move forward", wait: "0.25") if @idle == false
      @client.execute("fp jump", wait: "0.25") if target_higher_than_bot
      @client.execute("fp stop", wait: "0.25") if @idle == true
      sleep 0.75
    end
    @idle = true
  end
end

